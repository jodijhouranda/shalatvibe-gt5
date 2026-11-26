package com.jadwalshalat.gt5.arabic

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Repositori hibrid untuk konten Teks Interaktif modul Bahasa Arab (.json):
 * 1. Cek cache lokal terunduh / hasil ekstraksi AI (`filesDir/arabic_content/<stem>.json`)
 * 2. Cek aset bawaan APK (`assets/arabic_lessons/content/<stem>.json`)
 * 3. Unduh otomatis dari GitHub/sumber jika modul baru masuk dari sinkronisasi
 * 4. Jika JSON belum tersedia di GitHub namun PDF resminya ada, ekstrak otomatis
 *    dengan Firebase AI Logic (Gemini) lalu simpan permanen ke cache lokal.
 */
class ArabicContentRepository(context: Context) {

    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.filesDir, "arabic_content").apply { mkdirs() }
    private val pdfExtractor by lazy { ArabicPdfContentExtractor(appContext) }

    suspend fun loadLessonContent(lesson: ArabicLesson): ArabicLessonContent? = withContext(Dispatchers.IO) {
        val fileName = ArabicContentParser.contentFileNameForLesson(lesson)
        val cached = File(cacheDir, fileName)

        if (cached.exists() && cached.length() > 30L) {
            val raw = runCatching { cached.readText(Charsets.UTF_8) }.getOrNull()
            val parsed = raw?.let { ArabicContentParser.parseContentJson(it) }
            if (parsed != null) return@withContext parsed
        }

        // Fallback ke aset bundled di APK
        val assetText = runCatching {
            appContext.assets.open("arabic_lessons/content/$fileName").bufferedReader(Charsets.UTF_8).use {
                it.readText()
            }
        }.getOrNull()
        if (!assetText.isNullOrBlank()) {
            val parsed = ArabicContentParser.parseContentJson(assetText)
            if (parsed != null) return@withContext parsed
        }

        // Modul baru dari GitHub sync -> unduh file JSON interaktifnya langsung dari GitHub
        val remoteUrl = ArabicContentParser.inferContentUrl(lesson)
        val fetched = runCatching { fetchText(remoteUrl) }.getOrNull()
        if (!fetched.isNullOrBlank()) {
            val parsed = ArabicContentParser.parseContentJson(fetched)
            if (parsed != null) {
                writeCacheAtomically(cached, fileName, fetched)
                return@withContext parsed
            }
        }
        null
    }

    /**
     * Mengunduh & menyimpan cache lokal JSON konten dari GitHub saat sinkronisasi katalog
     * agar modul baru maupun pembaruan JSON langsung tersedia offline.
     * Mengembalikan `true` jika file JSON baru/terbaru berhasil diunduh dan disimpan.
     */
    suspend fun prefetchLessonContent(
        lesson: ArabicLesson,
        refreshExisting: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val fileName = ArabicContentParser.contentFileNameForLesson(lesson)
        val cached = File(cacheDir, fileName)
        val hasCached = cached.exists() && cached.length() > 30L
        val hasBundled = !hasCached && runCatching {
            appContext.assets.open("arabic_lessons/content/$fileName").use { true }
        }.getOrDefault(false)

        if (!refreshExisting && (hasCached || hasBundled)) {
            return@withContext false
        }

        val remoteUrl = ArabicContentParser.inferContentUrl(lesson)
        val fetched = runCatching { fetchText(remoteUrl) }.getOrNull() ?: return@withContext false
        val parsed = ArabicContentParser.parseContentJson(fetched) ?: return@withContext false
        if (parsed.transcriptSegments.isEmpty() && parsed.totalGlossaryItems == 0) {
            return@withContext false
        }

        val existingText = when {
            hasCached -> runCatching { cached.readText(Charsets.UTF_8) }.getOrNull()
            hasBundled -> runCatching {
                appContext.assets.open("arabic_lessons/content/$fileName").bufferedReader(Charsets.UTF_8).use { it.readText() }
            }.getOrNull()
            else -> null
        }
        if (existingText != null && existingText.trim() == fetched.trim()) {
            return@withContext false
        }

        writeCacheAtomically(cached, fileName, fetched)
        true
    }

    suspend fun extractAndCacheFromPdf(
        lesson: ArabicLesson,
        pdfFile: File
    ): ArabicLessonContent = withContext(Dispatchers.IO) {
        val fileName = ArabicContentParser.contentFileNameForLesson(lesson)
        val cached = File(cacheDir, fileName)
        if (cached.exists() && cached.length() > 30L) {
            val raw = runCatching { cached.readText(Charsets.UTF_8) }.getOrNull()
            val parsed = raw?.let { ArabicContentParser.parseContentJson(it) }
            if (parsed != null) return@withContext parsed
        }

        val (parsed, rawJson) = pdfExtractor.extractFromPdf(lesson, pdfFile)
        writeCacheAtomically(cached, fileName, rawJson)
        parsed
    }

    private fun writeCacheAtomically(targetFile: File, fileName: String, content: String) {
        runCatching {
            cacheDir.mkdirs()
            val tmp = File(cacheDir, "$fileName.tmp")
            tmp.writeText(content, Charsets.UTF_8)
            if (targetFile.exists()) targetFile.delete()
            if (!tmp.renameTo(targetFile)) {
                targetFile.writeText(content, Charsets.UTF_8)
                tmp.delete()
            }
        }
    }

    private fun fetchText(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as? HttpURLConnection) ?: return null
        return try {
            conn.connectTimeout = 6000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }
}
