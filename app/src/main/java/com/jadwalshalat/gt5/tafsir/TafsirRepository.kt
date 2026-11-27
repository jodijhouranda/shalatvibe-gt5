package com.jadwalshalat.gt5.tafsir

import android.content.Context
import com.jadwalshalat.gt5.arabic.ArabicTextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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

    /**
     * Pencarian autocomplete potongan ayat Al-Qur'an (mendukung:
     * 1) Referensi langsung seperti "2:255" atau "baqarah 255"
     * 2) Pencarian cepat pada surah yang sudah tersimpan di cache lokal (Arab tanpa harakat)
     * 3) Autocomplete daring cepat via api.quran.com/api/v4/search (Arab & arti Indonesia)
     *    dengan cadangan api.alquran.cloud/v1/search).
     */
    suspend fun searchAyahSnippets(query: String): Result<List<TafsirAyahSearchHit>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext Result.success(emptyList())

        val hits = LinkedHashMap<String, TafsirAyahSearchHit>()

        // 1. Referensi langsung mis. "2:255" atau "kahfi 10"
        parseDirectReference(trimmed)?.let { (surahNum, ayahNum) ->
            val meta = TafsirCatalog.surahByNumber(surahNum)
            val cachedText = surahMemoryCache[surahNum]?.get(ayahNum)
                ?: readSurahCacheFile(File(surahCacheDir, "surah_$surahNum.json"))?.get(ayahNum)
                ?: ""
            hits["$surahNum:$ayahNum"] = TafsirAyahSearchHit(
                surah = surahNum,
                ayah = ayahNum,
                surahNameLatin = meta.nameLatin,
                surahNameArabic = meta.nameArabic,
                arabicSnippet = cachedText,
                secondarySnippet = "Lompat langsung ke QS ${meta.nameLatin} $surahNum:$ayahNum",
                sourceLabel = "Langsung"
            )
            return@withContext Result.success(hits.values.toList())
        }

        val isArabicQuery = ArabicTextUtils.hasArabicLetters(trimmed)
        val normArabicQuery = if (isArabicQuery) ArabicTextUtils.normalizeForLookup(trimmed) else ""

        // 2. Pencarian instan pada cache lokal (memori + disk) bila query berbahasa Arab >= 3 huruf
        if (isArabicQuery && normArabicQuery.length >= 3) {
            searchLocalCachedSurahs(normArabicQuery, limit = 8).forEach { hit ->
                hits["${hit.surah}:${hit.ayah}"] = hit
            }
        }

        // 3. Pencarian daring autocomplete jika query cukup spesifik (>= 3 karakter)
        if (trimmed.length >= 3) {
            val encoded = runCatching { URLEncoder.encode(trimmed, "UTF-8") }.getOrDefault(trimmed)
            var remoteError: Exception? = null

            // Utama: Quran.com v4 search (mendukung potongan ayat Arab tanpa harakat & terjemahan ID)
            val quranComUrl = "https://api.quran.com/api/v4/search?q=$encoded&size=12&language=id"
            val quranComHits = runCatching {
                val resp = httpGet(quranComUrl)
                if (resp.code in 200..299) parseQuranComSearch(resp.body) else emptyList()
            }.onFailure { remoteError = it as? Exception }.getOrNull().orEmpty()

            quranComHits.forEach { hit ->
                hits.putIfAbsent("${hit.surah}:${hit.ayah}", hit)
            }

            // Cadangan: AlQuran.cloud search (jika Quran.com kosong/gagal)
            if (hits.isEmpty()) {
                val edition = if (isArabicQuery) "quran-simple-clean" else "id.indonesian"
                val alQuranQuery = if (isArabicQuery) {
                    TafsirTextProcessor.formatArabicForDisplay(trimmed, showHarakat = false)
                } else {
                    trimmed
                }
                val encodedFallback = runCatching { URLEncoder.encode(alQuranQuery, "UTF-8") }.getOrDefault(alQuranQuery)
                val fallbackUrl = "https://api.alquran.cloud/v1/search/$encodedFallback/all/$edition"
                val fallbackHits = runCatching {
                    val resp = httpGet(fallbackUrl)
                    if (resp.code in 200..299) parseAlQuranCloudSearch(resp.body, isArabicQuery) else emptyList()
                }.onFailure { if (remoteError == null) remoteError = it as? Exception }.getOrNull().orEmpty()

                fallbackHits.forEach { hit ->
                    hits.putIfAbsent("${hit.surah}:${hit.ayah}", hit)
                }
            }

            if (hits.isEmpty() && remoteError != null) {
                return@withContext Result.failure(remoteError!!)
            }
        }

        Result.success(hits.values.take(15))
    }

    private fun searchLocalCachedSurahs(normQuery: String, limit: Int): List<TafsirAyahSearchHit> {
        val out = ArrayList<TafsirAyahSearchHit>()
        val checkedSurahs = HashSet<Int>()

        fun scanMap(surahNum: Int, verses: Map<Int, String>) {
            if (!checkedSurahs.add(surahNum)) return
            val meta = TafsirCatalog.surahByNumber(surahNum)
            for ((ayahNum, text) in verses) {
                if (ArabicTextUtils.normalizeForLookup(text).contains(normQuery)) {
                    out += TafsirAyahSearchHit(
                        surah = surahNum,
                        ayah = ayahNum,
                        surahNameLatin = meta.nameLatin,
                        surahNameArabic = meta.nameArabic,
                        arabicSnippet = text,
                        sourceLabel = "Cache Lokal"
                    )
                    if (out.size >= limit) return
                }
            }
        }

        for ((surahNum, verses) in surahMemoryCache) {
            scanMap(surahNum, verses)
            if (out.size >= limit) return out
        }

        val files = surahCacheDir.listFiles().orEmpty()
        for (file in files) {
            val surahNum = file.name.removePrefix("surah_").removeSuffix(".json").toIntOrNull() ?: continue
            if (surahNum in checkedSurahs) continue
            val verses = readSurahCacheFile(file) ?: continue
            scanMap(surahNum, verses)
            if (out.size >= limit) return out
        }
        return out
    }

    private fun parseQuranComSearch(raw: String): List<TafsirAyahSearchHit> {
        val root = JSONObject(raw)
        val searchObj = root.optJSONObject("search") ?: return emptyList()
        val results = searchObj.optJSONArray("results") ?: return emptyList()
        val list = ArrayList<TafsirAyahSearchHit>(results.length())
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val key = item.optString("verse_key", "")
            val parts = key.split(":")
            if (parts.size != 2) continue
            val surah = parts[0].toIntOrNull()?.takeIf { it in 1..114 } ?: continue
            val meta = TafsirCatalog.surahByNumber(surah)
            val ayah = parts[1].toIntOrNull()?.takeIf { it in 1..meta.ayahCount } ?: continue
            val arabicText = TafsirTextProcessor.cleanHtmlToPlainText(item.optString("text", ""))
            val translations = item.optJSONArray("translations")
            val transText = if (translations != null && translations.length() > 0) {
                TafsirTextProcessor.cleanHtmlToPlainText(translations.optJSONObject(0)?.optString("text", "").orEmpty())
            } else ""
            list += TafsirAyahSearchHit(
                surah = surah,
                ayah = ayah,
                surahNameLatin = meta.nameLatin,
                surahNameArabic = meta.nameArabic,
                arabicSnippet = arabicText,
                secondarySnippet = transText,
                sourceLabel = "Quran.com"
            )
        }
        return list
    }

    private fun parseAlQuranCloudSearch(raw: String, isArabicEdition: Boolean): List<TafsirAyahSearchHit> {
        val root = JSONObject(raw)
        val data = root.optJSONObject("data") ?: return emptyList()
        val matches = data.optJSONArray("matches") ?: return emptyList()
        val list = ArrayList<TafsirAyahSearchHit>(minOf(matches.length(), 15))
        for (i in 0 until minOf(matches.length(), 15)) {
            val item = matches.optJSONObject(i) ?: continue
            val surahObj = item.optJSONObject("surah") ?: continue
            val surah = surahObj.optInt("number", -1).takeIf { it in 1..114 } ?: continue
            val meta = TafsirCatalog.surahByNumber(surah)
            val ayah = item.optInt("numberInSurah", -1).takeIf { it in 1..meta.ayahCount } ?: continue
            val text = TafsirTextProcessor.cleanHtmlToPlainText(item.optString("text", ""))
            list += TafsirAyahSearchHit(
                surah = surah,
                ayah = ayah,
                surahNameLatin = meta.nameLatin,
                surahNameArabic = meta.nameArabic,
                arabicSnippet = if (isArabicEdition) text else "",
                secondarySnippet = if (isArabicEdition) "" else text,
                sourceLabel = "AlQuran"
            )
        }
        return list
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

        private val COLON_REF_REGEX = Regex("^(\\d{1,3})\\s*[:\\-/.\\s]\\s*(\\d{1,3})$")
        private val NAME_AND_AYAH_REGEX = Regex("^(.+?)\\s+(\\d{1,3})$")

        /**
         * Mengenali input referensi langsung seperti "2:255", "18 10", atau "baqarah 255".
         */
        fun parseDirectReference(input: String): Pair<Int, Int>? {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return null

            COLON_REF_REGEX.matchEntire(trimmed)?.let { m ->
                val s = m.groupValues[1].toIntOrNull() ?: return null
                val a = m.groupValues[2].toIntOrNull() ?: return null
                if (s in 1..114) {
                    val max = TafsirCatalog.surahByNumber(s).ayahCount
                    if (a in 1..max) return s to a
                }
            }

            NAME_AND_AYAH_REGEX.matchEntire(trimmed)?.let { m ->
                val surahPart = m.groupValues[1].trim()
                val ayah = m.groupValues[2].toIntOrNull() ?: return null
                if (surahPart.all { it.isDigit() }) return null
                val cleanPart = surahPart.lowercase().replace("-", "").replace("'", "").replace(" ", "")
                val matchedSurah = TafsirCatalog.surahs.firstOrNull { s ->
                    val latinNorm = s.nameLatin.lowercase().replace("-", "").replace("'", "").replace(" ", "")
                    latinNorm.contains(cleanPart) || s.nameArabic.contains(surahPart)
                }
                if (matchedSurah != null && ayah in 1..matchedSurah.ayahCount) {
                    return matchedSurah.number to ayah
                }
            }

            return null
        }
    }
}
