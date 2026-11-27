package com.jadwalshalat.gt5.tafsir

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class TafsirRepository(context: Context) {

    private val appContext = context.applicationContext
    private val cacheRoot = File(appContext.filesDir, "tafsir_cache").apply { mkdirs() }
    private val surahCacheDir = File(cacheRoot, "surahs").apply { mkdirs() }
    private val bookCacheDir = File(cacheRoot, "books").apply { mkdirs() }

    private val surahMemoryCache = ConcurrentHashMap<Int, Map<Int, String>>()
    private val tafsirMemoryCache: MutableMap<String, ParsedTafsirContent> =
        Collections.synchronizedMap(
            object : LinkedHashMap<String, ParsedTafsirContent>(80, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ParsedTafsirContent>?): Boolean {
                    return size > 96
                }
            }
        )

    /**
     * Memuat seluruh ayat Utsmani untuk 1 surah (cache memori -> cache disk -> 3 sumber API cepat).
     */
    suspend fun loadSurahVerses(surah: Int): Result<Map<Int, String>> = withContext(Dispatchers.IO) {
        val safeSurah = surah.coerceIn(1, 114)

        surahMemoryCache[safeSurah]?.let { cached ->
            if (cached.isNotEmpty()) return@withContext Result.success(cached)
        }

        val diskFile = File(surahCacheDir, "surah_$safeSurah.json")
        readSurahCacheFile(diskFile)?.let { fromDisk ->
            if (fromDisk.isNotEmpty()) {
                surahMemoryCache[safeSurah] = fromDisk
                return@withContext Result.success(fromDisk)
            }
        }

        // 1. Utama: jsDelivr quran-json@3.1.2 (~70ms)
        val primaryUrl = "https://cdn.jsdelivr.net/npm/quran-json@3.1.2/dist/chapters/$safeSurah.json"
        val primaryResult = runCatching {
            val raw = httpGet(primaryUrl).body
            parseQuranJsonChapter(raw)
        }.getOrNull()
        if (!primaryResult.isNullOrEmpty()) {
            surahMemoryCache[safeSurah] = primaryResult
            writeSurahCacheFile(diskFile, primaryResult)
            return@withContext Result.success(primaryResult)
        }

        // 2. Cadangan 1: api.quran.com v4 uthmani
        val backup1Url = "https://api.quran.com/api/v4/quran/verses/uthmani?chapter_number=$safeSurah"
        val backup1Result = runCatching {
            val raw = httpGet(backup1Url).body
            parseQuranComUthmani(raw)
        }.getOrNull()
        if (!backup1Result.isNullOrEmpty()) {
            surahMemoryCache[safeSurah] = backup1Result
            writeSurahCacheFile(diskFile, backup1Result)
            return@withContext Result.success(backup1Result)
        }

        // 3. Cadangan 2: api.alquran.cloud
        val backup2Url = "https://api.alquran.cloud/v1/surah/$safeSurah/quran-uthmani"
        val backup2Result = runCatching {
            val raw = httpGet(backup2Url).body
            parseAlQuranCloud(raw)
        }.getOrNull()
        if (!backup2Result.isNullOrEmpty()) {
            surahMemoryCache[safeSurah] = backup2Result
            writeSurahCacheFile(diskFile, backup2Result)
            return@withContext Result.success(backup2Result)
        }

        Result.failure(IOException("Gagal memuat teks ayat surah $safeSurah. Periksa koneksi internet Anda."))
    }

    /**
     * Memuat 1 kitab tafsir aktif untuk (surah, ayah) dengan cache lokal & fallback cepat.
     */
    suspend fun loadTafsir(
        book: TafsirBook,
        surah: Int,
        ayah: Int,
        forceRefresh: Boolean = false
    ): TafsirLoadState = withContext(Dispatchers.IO) {
        val safeSurah = surah.coerceIn(1, 114)
        val maxAyah = TafsirCatalog.surahByNumber(safeSurah).ayahCount
        val safeAyah = ayah.coerceIn(1, maxAyah)
        val externalUrl = book.tafsirAppUrl(safeSurah, safeAyah)

        if (book.isScannedOnly) {
            return@withContext TafsirLoadState.ScannedOnly(
                book = book,
                surah = safeSurah,
                ayah = safeAyah,
                externalUrl = externalUrl
            )
        }

        val cacheKey = "${book.id}:$safeSurah:$safeAyah"
        if (!forceRefresh) {
            tafsirMemoryCache[cacheKey]?.let { mem ->
                return@withContext TafsirLoadState.Ready(mem.copy(fromCache = true))
            }
            readTafsirCacheFile(book.id, safeSurah, safeAyah)?.let { disk ->
                tafsirMemoryCache[cacheKey] = disk
                return@withContext TafsirLoadState.Ready(disk.copy(fromCache = true))
            }
        }

        var hadNetworkFailure = false
        var lastErrorMessage: String? = null

        // 1. Utama: jsDelivr spa5k/tafsir_api (kecuali ibn-uthaymeen yang langsung ke tafsir.app)
        val cdnSlug = book.cdnSlug
        if (!cdnSlug.isNullOrBlank()) {
            val cdnUrl = "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/$cdnSlug/$safeSurah/$safeAyah.json"
            when (val res = fetchCdnTafsir(cdnUrl)) {
                is FetchAttempt.Success -> {
                    val parsed = TafsirTextProcessor.parseTafsirRawText(res.rawText)
                    if (parsed.isNotEmpty()) {
                        val content = ParsedTafsirContent(
                            paragraphs = parsed,
                            sourceLabel = "CDN Tafsir",
                            fromCache = false
                        )
                        tafsirMemoryCache[cacheKey] = content
                        writeTafsirCacheFile(book.id, safeSurah, safeAyah, res.rawText, "CDN Tafsir")
                        return@withContext TafsirLoadState.Ready(content)
                    }
                }
                is FetchAttempt.Empty -> Unit
                is FetchAttempt.NetworkError -> {
                    hadNetworkFailure = true
                    lastErrorMessage = res.message
                }
            }
        }

        // 2. Fallback (dan jalur langsung untuk Ibnu Utsaimin): tafsir.app/get.php
        val tafsirAppApiUrl =
            "https://tafsir.app/get.php?src=${book.tafsirAppSlug}&s=$safeSurah&a=$safeAyah&ver=1"
        when (val res = fetchTafsirApp(tafsirAppApiUrl)) {
            is FetchAttempt.Success -> {
                val parsed = TafsirTextProcessor.parseTafsirRawText(res.rawText)
                if (parsed.isNotEmpty()) {
                    val content = ParsedTafsirContent(
                        paragraphs = parsed,
                        sourceLabel = "tafsir.app",
                        fromCache = false
                    )
                    tafsirMemoryCache[cacheKey] = content
                    writeTafsirCacheFile(book.id, safeSurah, safeAyah, res.rawText, "tafsir.app")
                    return@withContext TafsirLoadState.Ready(content)
                }
            }
            is FetchAttempt.Empty -> {
                // Server merespons normal namun ayat memang kosong di kitab ini
                return@withContext TafsirLoadState.EmptyForAyah(
                    book = book,
                    surah = safeSurah,
                    ayah = safeAyah,
                    externalUrl = externalUrl
                )
            }
            is FetchAttempt.NetworkError -> {
                hadNetworkFailure = true
                lastErrorMessage = res.message
            }
        }

        if (hadNetworkFailure) {
            TafsirLoadState.Error(
                message = lastErrorMessage ?: "Gagal memuat tafsir. Periksa koneksi internet Anda.",
                externalUrl = externalUrl
            )
        } else {
            TafsirLoadState.EmptyForAyah(
                book = book,
                surah = safeSurah,
                ayah = safeAyah,
                externalUrl = externalUrl
            )
        }
    }

    /**
     * Prefetch diam-diam ayat berikutnya untuk kitab yang sedang aktif agar navigasi instan.
     */
    suspend fun prefetchNextAyah(book: TafsirBook, currentSurah: Int, currentAyah: Int) {
        if (book.isScannedOnly) return
        val next = computeNextReference(currentSurah, currentAyah) ?: return
        runCatching {
            loadSurahVerses(next.first)
            loadTafsir(book, next.first, next.second, forceRefresh = false)
        }
    }

    fun computeNextReference(surah: Int, ayah: Int): Pair<Int, Int>? {
        val meta = TafsirCatalog.surahByNumber(surah)
        return when {
            ayah < meta.ayahCount -> surah to (ayah + 1)
            surah < 114 -> (surah + 1) to 1
            else -> null
        }
    }

    fun computePreviousReference(surah: Int, ayah: Int): Pair<Int, Int>? {
        return when {
            ayah > 1 -> surah to (ayah - 1)
            surah > 1 -> {
                val prevSurah = surah - 1
                prevSurah to TafsirCatalog.surahByNumber(prevSurah).ayahCount
            }
            else -> null
        }
    }

    private sealed interface FetchAttempt {
        data class Success(val rawText: String) : FetchAttempt
        data object Empty : FetchAttempt
        data class NetworkError(val message: String) : FetchAttempt
    }

    private fun fetchCdnTafsir(url: String): FetchAttempt {
        return try {
            val resp = httpGet(url)
            if (resp.code == 404 || resp.code == 403) {
                FetchAttempt.Empty
            } else if (resp.code !in 200..299) {
                FetchAttempt.NetworkError("CDN mengembalikan status ${resp.code}")
            } else {
                val json = JSONObject(resp.body)
                val text = json.optString("text", "").trim()
                if (text.isBlank()) FetchAttempt.Empty else FetchAttempt.Success(text)
            }
        } catch (e: Exception) {
            FetchAttempt.NetworkError(e.message ?: "Koneksi ke CDN tafsir terganggu.")
        }
    }

    private fun fetchTafsirApp(url: String): FetchAttempt {
        return try {
            val resp = httpGet(url)
            if (resp.code == 404) {
                FetchAttempt.Empty
            } else if (resp.code !in 200..299) {
                FetchAttempt.NetworkError("Server tafsir.app mengembalikan status ${resp.code}")
            } else {
                val trimmed = resp.body.trim()
                if (trimmed.isBlank()) return FetchAttempt.Empty
                val dataText = if (trimmed.startsWith("{")) {
                    val json = JSONObject(trimmed)
                    if (json.isNull("data")) "" else json.optString("data", "").trim()
                } else {
                    trimmed
                }
                if (dataText.isBlank() || dataText.equals("false", ignoreCase = true) || dataText.equals("null", ignoreCase = true)) {
                    FetchAttempt.Empty
                } else {
                    FetchAttempt.Success(dataText)
                }
            }
        } catch (e: Exception) {
            FetchAttempt.NetworkError(e.message ?: "Koneksi ke tafsir.app terganggu.")
        }
    }

    private data class HttpResponse(val code: Int, val body: String)

    private fun httpGet(urlStr: String): HttpResponse {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection)
        return try {
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json, text/plain, */*")
            conn.setRequestProperty("User-Agent", "NidaBelajarAndroid/2.5")
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            HttpResponse(code, body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseQuranJsonChapter(raw: String): Map<Int, String> {
        val root = JSONObject(raw)
        val verses = root.optJSONArray("verses") ?: return emptyMap()
        val map = LinkedHashMap<Int, String>(verses.length())
        for (i in 0 until verses.length()) {
            val v = verses.optJSONObject(i) ?: continue
            val id = v.optInt("id", -1)
            val text = v.optString("text", "").trim()
            if (id > 0 && text.isNotBlank()) {
                map[id] = text
            }
        }
        return map
    }

    private fun parseQuranComUthmani(raw: String): Map<Int, String> {
        val root = JSONObject(raw)
        val verses = root.optJSONArray("verses") ?: return emptyMap()
        val map = LinkedHashMap<Int, String>(verses.length())
        for (i in 0 until verses.length()) {
            val v = verses.optJSONObject(i) ?: continue
            val key = v.optString("verse_key", "")
            val ayahNum = key.substringAfter(':', "").toIntOrNull() ?: (i + 1)
            val text = v.optString("text_uthmani", "").trim()
            if (ayahNum > 0 && text.isNotBlank()) {
                map[ayahNum] = text
            }
        }
        return map
    }

    private fun parseAlQuranCloud(raw: String): Map<Int, String> {
        val root = JSONObject(raw)
        val data = root.optJSONObject("data") ?: return emptyMap()
        val ayahs = data.optJSONArray("ayahs") ?: return emptyMap()
        val map = LinkedHashMap<Int, String>(ayahs.length())
        for (i in 0 until ayahs.length()) {
            val a = ayahs.optJSONObject(i) ?: continue
            val num = a.optInt("numberInSurah", i + 1)
            val text = a.optString("text", "").trim()
            if (num > 0 && text.isNotBlank()) {
                map[num] = text
            }
        }
        return map
    }

    private fun readSurahCacheFile(file: File): Map<Int, String>? {
        if (!file.exists() || file.length() < 8L) return null
        return runCatching {
            val obj = JSONObject(file.readText(Charsets.UTF_8))
            val map = LinkedHashMap<Int, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val num = k.toIntOrNull() ?: continue
                val txt = obj.optString(k, "").trim()
                if (txt.isNotBlank()) map[num] = txt
            }
            map
        }.getOrNull()
    }

    private fun writeSurahCacheFile(file: File, verses: Map<Int, String>) {
        runCatching {
            surahCacheDir.mkdirs()
            val obj = JSONObject()
            verses.forEach { (num, text) -> obj.put(num.toString(), text) }
            writeAtomically(file, obj.toString())
        }
    }

    private fun tafsirDiskFile(bookId: String, surah: Int, ayah: Int): File {
        val dir = File(bookCacheDir, bookId).apply { mkdirs() }
        return File(dir, "${surah}_${ayah}.json")
    }

    private fun readTafsirCacheFile(bookId: String, surah: Int, ayah: Int): ParsedTafsirContent? {
        val file = tafsirDiskFile(bookId, surah, ayah)
        if (!file.exists() || file.length() < 10L) return null
        return runCatching {
            val obj = JSONObject(file.readText(Charsets.UTF_8))
            val rawText = obj.optString("text", "")
            val source = obj.optString("source", "Cache Lokal")
            val parsed = TafsirTextProcessor.parseTafsirRawText(rawText)
            if (parsed.isEmpty()) null else ParsedTafsirContent(parsed, source, fromCache = true)
        }.getOrNull()
    }

    private fun writeTafsirCacheFile(bookId: String, surah: Int, ayah: Int, rawText: String, source: String) {
        runCatching {
            val file = tafsirDiskFile(bookId, surah, ayah)
            val obj = JSONObject().apply {
                put("text", rawText)
                put("source", source)
            }
            writeAtomically(file, obj.toString())
        }
    }

    private fun writeAtomically(target: File, content: String) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            target.writeText(content, Charsets.UTF_8)
            tmp.delete()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 6500
        private const val READ_TIMEOUT_MS = 9000
    }
}
