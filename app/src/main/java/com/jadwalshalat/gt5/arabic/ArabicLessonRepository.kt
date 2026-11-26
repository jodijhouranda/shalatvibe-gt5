package com.jadwalshalat.gt5.arabic

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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

    fun hasBundledOrCachedPdf(lesson: ArabicLesson): Boolean {
        if (lesson.pdfFileName.isBlank()) return false
        val cached = File(pdfCacheDir, lesson.pdfFileName)
        if (cached.exists() && cached.length() > 1024L) return true
        return runCatching {
            appContext.assets.open("$ASSET_DIR/${lesson.pdfFileName}").use { true }
        }.getOrDefault(false)
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

        // 2. Jika modul lama masih punya pdfUrl atau Google Drive link, unduh hanya saat diminta
        val downloadUrl = when {
            lesson.pdfUrl.isNotBlank() -> ArabicCatalogParser.normalizeDownloadUrl(lesson.pdfUrl)
            lesson.driveFileId.isNotBlank() -> "https://drive.google.com/uc?export=download&id=${lesson.driveFileId}"
            else -> ""
        }
        if (downloadUrl.isBlank()) return@withContext null

        val downloaded = downloadFile(downloadUrl, cached)
        if (downloaded && cached.length() > 1024L) cached else null
    }

    suspend fun syncFromRemote(
        urlOverride: String? = null,
        refreshContentJson: Boolean = false
    ): SyncResult = withContext(Dispatchers.IO) {
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

        val mergedJsonText = mergeAutoDiscoveredGitHubJsonLessons(normalizedUrl, jsonText, parsed)
        val finalParsed = runCatching { ArabicCatalogParser.parseCatalogJson(mergedJsonText) }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: parsed

        syncedCatalogFile.writeText(mergedJsonText)

        // Pre-cache konten JSON interaktif dari GitHub agar langsung siap dibuka offline (tanpa unduh PDF)
        val contentRepo = ArabicContentRepository(appContext)
        var newlyCachedJson = 0
        for (lesson in finalParsed) {
            val updated = contentRepo.prefetchLessonContent(
                lesson = lesson,
                refreshExisting = refreshContentJson
            )
            if (updated) newlyCachedJson++
        }

        val stamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale("id", "ID")))
        prefs.edit().putString(KEY_LAST_SYNC_LABEL, stamp).apply()

        val all = loadLessons()
        SyncResult(
            success = true,
            totalLessons = all.size,
            newlyCachedPdfs = newlyCachedJson,
            message = if (newlyCachedJson > 0) {
                "Sinkronisasi selesai: ${all.size} modul ($newlyCachedJson konten JSON baru tersimpan offline)."
            } else {
                "Katalog mutakhir: ${all.size} modul JSON siap dipelajari."
            }
        )
    }

    /**
     * Jika URL sinkronisasi menunjuk ke `raw.githubusercontent.com/<owner>/<repo>/<branch>/<dir>/catalog.json`,
     * periksa juga isi folder `<dir>/arabic_content` melalui GitHub Contents API agar file `.json` baru yang
     * belum sempat dicantumkan di `catalog.json` tetap otomatis muncul di aplikasi.
     */
    private fun mergeAutoDiscoveredGitHubJsonLessons(
        catalogUrl: String,
        originalCatalogJson: String,
        existingLessons: List<ArabicLesson>
    ): String {
        return runCatching {
            val match = Regex(
                "^https://raw\\.githubusercontent\\.com/([^/]+)/([^/]+)/([^/]+)/(.+)/[^/]+\\.json(?:\\?.*)?$"
            ).find(catalogUrl) ?: return originalCatalogJson

            val owner = match.groupValues[1]
            val repo = match.groupValues[2]
            val branch = match.groupValues[3]
            val baseDir = match.groupValues[4].trim('/')
            val contentDir = "$baseDir/arabic_content"
            val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$contentDir?ref=$branch"

            val listingRaw = fetchText(apiUrl) ?: return originalCatalogJson
            val filesArray = JSONArray(listingRaw)
            if (filesArray.length() == 0) return originalCatalogJson

            val knownJsonNames = existingLessons
                .map { ArabicContentParser.contentFileNameForLesson(it).trim().lowercase(Locale.US) }
                .filter { it.isNotEmpty() }
                .toHashSet()

            val discoveredEntries = mutableListOf<JSONObject>()
            for (i in 0 until filesArray.length()) {
                val item = filesArray.optJSONObject(i) ?: continue
                if (!item.optString("type").equals("file", ignoreCase = true)) continue
                val name = item.optString("name").trim()
                if (!name.endsWith(".json", ignoreCase = true)) continue
                val sizeBytes = item.optLong("size", 0L)
                if (sizeBytes <= 30L) continue
                if (knownJsonNames.contains(name.lowercase(Locale.US))) continue

                val downloadUrl = item.optString("download_url").trim().ifBlank {
                    val encodedName = URLEncoder.encode(name, "UTF-8").replace("+", "%20")
                    "https://raw.githubusercontent.com/$owner/$repo/$branch/$contentDir/$encodedName"
                }

                val stem = name.removeSuffix(".json").removeSuffix(".JSON")
                val safeId = "auto_json_" + stem.lowercase(Locale.US)
                    .replace(Regex("[^a-z0-9]+"), "_")
                    .trim('_')
                    .take(64)

                val prettyTitle = stem
                    .replace("Bahasa_Arab_Analisis_Video_", "", ignoreCase = true)
                    .replace("_TAFL", "", ignoreCase = true)
                    .replace('_', ' ')
                    .replace('-', ' ')
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .ifBlank { name }

                val category = if (stem.contains("video", ignoreCase = true)) {
                    "VIDEO_ANALYSIS"
                } else {
                    "DAILY_LEXICAL"
                }

                discoveredEntries.add(
                    JSONObject().apply {
                        put("id", safeId)
                        put("category", category)
                        put("dayLabel", "Materi Baru")
                        put("dateLabel", "Auto GitHub")
                        put("title", prettyTitle)
                        put("subtitle", "Konten JSON interaktif terbaru dari GitHub.")
                        put("sourceName", "GitHub JSON")
                        put("level", "CEFR B2–C1")
                        put("youtubeVideoId", "")
                        put("startSeconds", 0)
                        put("endSeconds", 0)
                        put("pdfFileName", "")
                        put("pdfUrl", "")
                        put("driveFileId", "")
                        put("jsonFileName", name)
                        put("jsonUrl", downloadUrl)
                        put("segments", JSONArray())
                    }
                )
            }

            if (discoveredEntries.isEmpty()) return originalCatalogJson

            val root = JSONObject(originalCatalogJson)
            val existingArr = root.optJSONArray("lessons") ?: JSONArray()
            val combinedArr = JSONArray()
            // Taruh materi JSON baru paling atas agar langsung terlihat
            for (entry in discoveredEntries) {
                combinedArr.put(entry)
            }
            for (i in 0 until existingArr.length()) {
                combinedArr.put(existingArr.get(i))
            }
            root.put("lessons", combinedArr)
            root.toString()
        }.getOrDefault(originalCatalogJson)
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

    fun mergeProgressFromCloud(
        cloudCompleted: Set<String>,
        cloudFavorites: Set<String>
    ): Pair<Set<String>, Set<String>> {
        val mergedCompleted = (getCompletedIds() + cloudCompleted).filter { it.isNotBlank() }.toSet()
        val mergedFavorites = (getFavoriteIds() + cloudFavorites).filter { it.isNotBlank() }.toSet()
        prefs.edit()
            .putStringSet(KEY_COMPLETED_IDS, mergedCompleted)
            .putStringSet(KEY_FAVORITE_IDS, mergedFavorites)
            .apply()
        return mergedCompleted to mergedFavorites
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
