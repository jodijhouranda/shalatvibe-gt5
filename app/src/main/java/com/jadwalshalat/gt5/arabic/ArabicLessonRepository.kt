package com.jadwalshalat.gt5.arabic

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SyncResult(
    val success: Boolean,
    val totalLessons: Int,
    val newlyCachedPdfs: Int,
    val message: String
)

/**
 * Repositori Hybrid Offline + Sync Manifest (Opsi 1) untuk modul Bahasa Arab Harian.
 * - Modul bundled di assets/arabic_lessons/ langsung siap offline tanpa internet.
 * - Modul baru bisa disinkronkan lewat manifest catalog.json (GitHub Raw atau Google Drive public link)
 *   lalu di-cache lokal di filesDir/arabic_pdfs/ agar terbuka cepat secara offline.
 */
class ArabicLessonRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val pdfCacheDir: File = File(appContext.filesDir, "arabic_pdfs").apply { mkdirs() }
    private val syncedCatalogFile: File = File(appContext.filesDir, "arabic_synced_catalog.json")

    fun loadLessons(): List<ArabicLesson> {
        val bundled = runCatching {
            val json = appContext.assets.open(ASSET_CATALOG_PATH).bufferedReader().use { it.readText() }
            ArabicCatalogParser.parseCatalogJson(json)
        }.getOrDefault(emptyList())

        val synced = if (syncedCatalogFile.exists()) {
            runCatching {
                ArabicCatalogParser.parseCatalogJson(syncedCatalogFile.readText())
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        if (synced.isEmpty()) return bundled

        // Gabungkan: daftar hasil sync di atas (terbaru), diikuti modul bundled yang belum ada di sync
        val merged = LinkedHashMap<String, ArabicLesson>()
        for (lesson in synced) {
            merged[lesson.id] = lesson
        }
        for (lesson in bundled) {
            if (!merged.containsKey(lesson.id)) {
                merged[lesson.id] = lesson
            }
        }
        return merged.values.toList()
    }

    suspend fun resolvePdfFile(lesson: ArabicLesson): File? = withContext(Dispatchers.IO) {
        if (lesson.pdfFileName.isBlank()) return@withContext null

        val cached = File(pdfCacheDir, lesson.pdfFileName)
        if (cached.exists() && cached.length() > 1024L) {
            return@withContext cached
        }

        // 1. Coba salin dari bundled assets
        val copiedFromAsset = runCatching {
            appContext.assets.open("$ASSET_DIR/${lesson.pdfFileName}").use { input ->
                FileOutputStream(cached).use { output ->
                    input.copyTo(output)
                }
            }
            cached.exists() && cached.length() > 1024L
        }.getOrDefault(false)

        if (copiedFromAsset) {
            return@withContext cached
        }

        // 2. Jika modul baru dari Sync, unduh dari pdfUrl atau Google Drive direct link
        val downloadUrl = when {
            lesson.pdfUrl.isNotBlank() -> ArabicCatalogParser.normalizeDownloadUrl(lesson.pdfUrl)
            lesson.driveFileId.isNotBlank() -> "https://drive.google.com/uc?export=download&id=${lesson.driveFileId}"
            else -> ""
        }
        if (downloadUrl.isBlank()) return@withContext null

        val downloaded = downloadFile(downloadUrl, cached)
        if (downloaded && cached.length() > 1024L) cached else null
    }

    suspend fun syncFromRemote(urlOverride: String? = null): SyncResult = withContext(Dispatchers.IO) {
        val rawUrl = (urlOverride ?: getSyncManifestUrl()).trim()
        if (rawUrl.isBlank()) {
            return@withContext SyncResult(false, loadLessons().size, 0, "URL manifest sinkronisasi masih kosong.")
        }

        val normalizedUrl = ArabicCatalogParser.normalizeDownloadUrl(rawUrl)
        val jsonText = fetchText(normalizedUrl)
            ?: return@withContext SyncResult(
                false,
                loadLessons().size,
                0,
                "Gagal mengunduh katalog. Pastikan internet aktif dan URL publik dapat diakses."
            )

        val parsed = runCatching { ArabicCatalogParser.parseCatalogJson(jsonText) }.getOrNull()
        if (parsed.isNullOrEmpty()) {
            return@withContext SyncResult(
                false,
                loadLessons().size,
                0,
                "Format catalog.json tidak valid atau daftar modul kosong."
            )
        }

        syncedCatalogFile.writeText(jsonText)

        var newlyCached = 0
        for (lesson in parsed) {
            if (lesson.pdfFileName.isBlank()) continue
            val target = File(pdfCacheDir, lesson.pdfFileName)
            if (!target.exists() || target.length() < 1024L) {
                val resolved = resolvePdfFile(lesson)
                if (resolved != null) newlyCached++
            }
        }

        val stamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale("id", "ID")))
        prefs.edit().putString(KEY_LAST_SYNC_LABEL, stamp).apply()

        val all = loadLessons()
        SyncResult(
            success = true,
            totalLessons = all.size,
            newlyCachedPdfs = newlyCached,
            message = if (newlyCached > 0) {
                "Sinkronisasi selesai: ${all.size} modul ($newlyCached PDF baru tersimpan offline)."
            } else {
                "Katalog mutakhir: ${all.size} modul siap dipelajari."
            }
        )
    }

    fun getCompletedIds(): Set<String> =
        prefs.getStringSet(KEY_COMPLETED_IDS, emptySet())?.toSet() ?: emptySet()

    fun toggleCompleted(lessonId: String): Set<String> {
        val next = getCompletedIds().toMutableSet()
        if (!next.add(lessonId)) next.remove(lessonId)
        prefs.edit().putStringSet(KEY_COMPLETED_IDS, next).apply()
        return next
    }

    fun markCompleted(lessonId: String, completed: Boolean): Set<String> {
        val next = getCompletedIds().toMutableSet()
        if (completed) next.add(lessonId) else next.remove(lessonId)
        prefs.edit().putStringSet(KEY_COMPLETED_IDS, next).apply()
        return next
    }

    fun getFavoriteIds(): Set<String> =
        prefs.getStringSet(KEY_FAVORITE_IDS, emptySet())?.toSet() ?: emptySet()

    fun toggleFavorite(lessonId: String): Set<String> {
        val next = getFavoriteIds().toMutableSet()
        if (!next.add(lessonId)) next.remove(lessonId)
        prefs.edit().putStringSet(KEY_FAVORITE_IDS, next).apply()
        return next
    }

    fun getLastOpenedLessonId(): String? = prefs.getString(KEY_LAST_OPENED_ID, null)

    fun setLastOpenedLessonId(lessonId: String) {
        prefs.edit().putString(KEY_LAST_OPENED_ID, lessonId).apply()
    }

    fun getReadingMode(): PdfReadingMode =
        PdfReadingMode.fromName(prefs.getString(KEY_READING_MODE, PdfReadingMode.ORIGINAL.name))

    fun setReadingMode(mode: PdfReadingMode) {
        prefs.edit().putString(KEY_READING_MODE, mode.name).apply()
    }

    fun getSyncManifestUrl(): String =
        prefs.getString(KEY_SYNC_URL, DEFAULT_SYNC_URL) ?: DEFAULT_SYNC_URL

    fun setSyncManifestUrl(url: String) {
        val clean = url.trim().ifBlank { DEFAULT_SYNC_URL }
        prefs.edit().putString(KEY_SYNC_URL, clean).apply()
    }

    fun resetSyncManifestUrl(): String {
        prefs.edit().putString(KEY_SYNC_URL, DEFAULT_SYNC_URL).apply()
        return DEFAULT_SYNC_URL
    }

    fun getLastSyncLabel(): String =
        prefs.getString(KEY_LAST_SYNC_LABEL, "Bundled offline") ?: "Bundled offline"

    private fun fetchText(urlString: String): String? {
        return runCatching {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "ShalatVibeGT5/2.1")
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun downloadFile(urlString: String, target: File): Boolean {
        return runCatching {
            val tmp = File(target.parentFile, "${target.name}.tmp")
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "ShalatVibeGT5/2.1")
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching false
                conn.inputStream.use { input ->
                    FileOutputStream(tmp).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tmp.length() > 1024L) {
                    if (target.exists()) target.delete()
                    tmp.renameTo(target)
                } else {
                    tmp.delete()
                    false
                }
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(false)
    }

    companion object {
        private const val PREF_NAME = "shalat_gt5_arabic_prefs"
        private const val ASSET_DIR = "arabic_lessons"
        private const val ASSET_CATALOG_PATH = "arabic_lessons/catalog.json"

        private const val KEY_COMPLETED_IDS = "completed_lesson_ids"
        private const val KEY_FAVORITE_IDS = "favorite_lesson_ids"
        private const val KEY_LAST_OPENED_ID = "last_opened_lesson_id"
        private const val KEY_READING_MODE = "pdf_reading_mode"
        private const val KEY_SYNC_URL = "sync_manifest_url"
        private const val KEY_LAST_SYNC_LABEL = "last_sync_label"

        const val DEFAULT_SYNC_URL =
            "https://raw.githubusercontent.com/jodijhouranda/shalatvibe-gt5/main/lessons/catalog.json"
    }
}
