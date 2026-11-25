package com.jadwalshalat.gt5.arabic

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Repositori AI Kamus menggunakan Firebase AI Logic (Gemini 3.8 Flash)
 * dengan structured JSON output + cache lokal atomik di penyimpanan internal.
 */
class KamusAiRepository(context: Context) {

    private val appContext = context.applicationContext
    private val cacheFile = File(appContext.filesDir, CACHE_FILE_NAME)
    private val tempFile = File(appContext.filesDir, "$CACHE_FILE_NAME.tmp")
    private val cacheLock = Any()

    @Volatile
    private var memoryCache: LinkedHashMap<String, CachedAiRecord>? = null

    private val analysisSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "vocalizedArabic" to Schema.string(
                    description = "Kata atau frase Arab dengan harakat (tasykil) lengkap yang akurat sesuai kedudukan i'rab dalam konteks kalimat."
                ),
                "indonesianMeaning" to Schema.string(
                    description = "Arti Bahasa Indonesia yang akurat, alami, dan ringkas sesuai konteks kalimat transkrip (1-2 kalimat singkat)."
                ),
                "transliteration" to Schema.string(
                    description = "Transliterasi Latin baku yang mudah dibaca, misalnya al-ḥaḍārah atau fī ẓilli."
                ),
                "turatsOrigin" to Schema.string(
                    description = "Akar kata 3 huruf (jadzr) dan makna dasar/klasik turats hingga pergeseran makna kontemporernya secara singkat."
                ),
                "synonymNote" to Schema.string(
                    description = "Sinonim Arab yang dekat beserta pembeda nuansa maknanya secara singkat, atau string kosong jika tidak relevan."
                ),
                "entryKind" to Schema.enumeration(
                    values = listOf("word", "phrase", "idiom"),
                    description = "Kategori entri: word untuk kata tunggal, phrase untuk frase atau kolokasi, idiom untuk ungkapan idiomatik."
                )
            )
        )
    }

    private val sharedGenerationConfig by lazy {
        generationConfig {
            temperature = 0.2f
            responseMimeType = "application/json"
            responseSchema = analysisSchema
        }
    }

    private val sharedSystemInstruction by lazy {
        content {
            text(
                "Kamu adalah pakar leksikografi Bahasa Arab (klasik turats dan Arab modern/MSA) " +
                    "dan penerjemah Arab-Indonesia yang presisi. Tugasmu menganalisis kata, frase, atau idiom Arab " +
                    "dari transkrip pelajaran untuk dicatat di Kamus Pribadi pengguna.\n" +
                    "Aturan ketat:\n" +
                    "1. Selalu utamakan makna kontekstual yang tepat sesuai kalimat transkrip yang diberikan.\n" +
                    "2. Berikan harakat (tasykil) lengkap pada vocalizedArabic yang selaras dengan teks sumber.\n" +
                    "3. Tulis indonesianMeaning, turatsOrigin, dan synonymNote dalam Bahasa Indonesia yang ringkas, jelas, dan langsung pada inti.\n" +
                    "4. Untuk turatsOrigin, sebutkan akar kata (misalnya: akar ح-ض-ر) dan hubungan makna klasik ke makna modern.\n" +
                    "5. Untuk synonymNote, sebutkan 1-2 sinonim Arab beserta nuansa bedanya jika bermanfaat."
            )
        }
    }

    private val primaryModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL_NAME,
            generationConfig = sharedGenerationConfig,
            systemInstruction = sharedSystemInstruction
        )
    }

    private val fallbackModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL_NAME,
            generationConfig = sharedGenerationConfig,
            systemInstruction = sharedSystemInstruction
        )
    }

    fun getCached(arabic: String, contextSentence: String): KamusAiAnalysis? {
        val key = buildCacheKey(arabic, contextSentence)
        if (key.isBlank()) return null
        return synchronized(cacheLock) {
            val map = ensureCacheLoadedLocked()
            map[key]?.analysis?.copy(fromCache = true)
        }
    }

    suspend fun analyze(
        arabic: String,
        contextSentence: String,
        lessonTitle: String = "",
        forceRefresh: Boolean = false
    ): KamusAiAnalysis {
        val cleanArabic = ArabicTextUtils.stripOuterPunctuation(arabic).ifBlank { arabic.trim() }
        require(cleanArabic.isNotBlank()) { "Kata atau frase Arab masih kosong." }

        if (!forceRefresh) {
            getCached(cleanArabic, contextSentence)?.let { return it }
        }

        if (FirebaseApp.getApps(appContext).isEmpty()) {
            throw IllegalStateException(
                "Firebase AI belum aktif pada APK ini karena google-services.json tidak disertakan saat build."
            )
        }

        val prompt = buildPrompt(
            arabic = cleanArabic,
            contextSentence = contextSentence.trim(),
            lessonTitle = lessonTitle.trim()
        )

        val response = withTimeout(TIMEOUT_MILLIS) {
            generateWithFallback(prompt)
        }

        val rawJson = response.text?.trim()
        if (rawJson.isNullOrBlank()) {
            throw IllegalStateException("Gemini AI mengembalikan respons kosong.")
        }

        val analysis = parseAnalysisJson(rawJson, fallbackArabic = cleanArabic)
        saveToCache(cleanArabic, contextSentence, analysis)
        return analysis
    }

    private suspend fun generateWithFallback(prompt: String) = try {
        primaryModel.generateContent(prompt)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (!isTransientDemandError(e)) throw e
        Log.w(TAG, "Primary model ($PRIMARY_MODEL_NAME) busy, falling back to $FALLBACK_MODEL_NAME", e)
        try {
            fallbackModel.generateContent(prompt)
        } catch (e2: CancellationException) {
            throw e2
        } catch (e2: Exception) {
            if (!isTransientDemandError(e2)) throw e2
            Log.w(TAG, "Fallback model ($FALLBACK_MODEL_NAME) also busy, retrying once after delay", e2)
            delay(RETRY_DELAY_MILLIS)
            try {
                fallbackModel.generateContent(prompt)
            } catch (e3: CancellationException) {
                throw e3
            } catch (e3: Exception) {
                Log.e(TAG, "All Gemini models busy after retry", e3)
                if (isTransientDemandError(e3)) {
                    throw IllegalStateException(
                        "Server Gemini sedang sangat padat sementara. Tunggu beberapa detik lalu tekan Coba Lagi.",
                        e3
                    )
                }
                throw e3
            }
        }
    }

    private fun isTransientDemandError(error: Throwable): Boolean {
        val msg = buildString {
            append(error.message.orEmpty())
            error.cause?.message?.let { append(' ').append(it) }
        }.lowercase()
        return msg.contains("high demand") ||
            msg.contains("overloaded") ||
            msg.contains("503") ||
            msg.contains("unavailable") ||
            msg.contains("resource_exhausted") ||
            msg.contains("429") ||
            msg.contains("try again later")
    }

    private fun buildPrompt(
        arabic: String,
        contextSentence: String,
        lessonTitle: String
    ): String = buildString {
        appendLine("Analisis entri kamus Bahasa Arab berikut:")
        appendLine("- Kata/Frase target: $arabic")
        if (contextSentence.isNotBlank()) {
            appendLine("- Kalimat konteks dari transkrip: $contextSentence")
        }
        if (lessonTitle.isNotBlank()) {
            appendLine("- Topik/Judul pelajaran: $lessonTitle")
        }
        append("Kembalikan JSON sesuai skema dengan arti kontekstual Indonesia, harakat lengkap, transliterasi, akar kata/turats, nuansa sinonim, dan jenis entri.")
    }

    private fun parseAnalysisJson(rawJson: String, fallbackArabic: String): KamusAiAnalysis {
        val json = JSONObject(rawJson)
        val vocalized = json.optString("vocalizedArabic", "").trim().ifBlank { fallbackArabic }
        val meaning = json.optString("indonesianMeaning", "").trim()
        if (meaning.isBlank()) {
            throw IllegalStateException("Respons AI tidak memuat arti Bahasa Indonesia.")
        }
        val transliteration = json.optString("transliteration", "").trim()
        val turatsOrigin = json.optString("turatsOrigin", "").trim()
        val synonymNote = json.optString("synonymNote", "").trim()
        val kindRaw = json.optString("entryKind", "").trim()
        val inferredDefault = if (fallbackArabic.contains(' ')) KamusEntryKind.PHRASE else KamusEntryKind.WORD
        val kind = if (kindRaw.isBlank()) inferredDefault else KamusEntryKind.fromRaw(kindRaw)

        return KamusAiAnalysis(
            vocalizedArabic = vocalized,
            indonesianMeaning = meaning,
            transliteration = transliteration,
            turatsOrigin = turatsOrigin,
            synonymNote = synonymNote,
            suggestedKind = kind,
            fromCache = false
        )
    }

    private fun buildCacheKey(arabic: String, contextSentence: String): String {
        val normArabic = ArabicTextUtils.normalizeForLookup(arabic)
        if (normArabic.isBlank()) return ""
        val ctxHash = ArabicTextUtils.normalizeForLookup(contextSentence).hashCode()
        return "$normArabic|$ctxHash"
    }

    private fun saveToCache(arabic: String, contextSentence: String, analysis: KamusAiAnalysis) {
        val key = buildCacheKey(arabic, contextSentence)
        if (key.isBlank()) return
        synchronized(cacheLock) {
            val map = ensureCacheLoadedLocked()
            map.remove(key)
            map[key] = CachedAiRecord(
                key = key,
                analysis = analysis.copy(fromCache = true),
                updatedAtMillis = System.currentTimeMillis()
            )
            while (map.size > MAX_CACHE_ENTRIES) {
                val oldestKey = map.entries.firstOrNull()?.key ?: break
                map.remove(oldestKey)
            }
            writeCacheLocked(map)
        }
    }

    private fun ensureCacheLoadedLocked(): LinkedHashMap<String, CachedAiRecord> {
        memoryCache?.let { return it }
        val loaded = LinkedHashMap<String, CachedAiRecord>()
        if (cacheFile.exists()) {
            runCatching {
                val root = JSONObject(cacheFile.readText(Charsets.UTF_8))
                val arr = root.optJSONArray("items") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val key = obj.optString("key", "").trim()
                    val meaning = obj.optString("indonesianMeaning", "").trim()
                    if (key.isBlank() || meaning.isBlank()) continue
                    loaded[key] = CachedAiRecord(
                        key = key,
                        analysis = KamusAiAnalysis(
                            vocalizedArabic = obj.optString("vocalizedArabic", ""),
                            indonesianMeaning = meaning,
                            transliteration = obj.optString("transliteration", ""),
                            turatsOrigin = obj.optString("turatsOrigin", ""),
                            synonymNote = obj.optString("synonymNote", ""),
                            suggestedKind = KamusEntryKind.fromRaw(obj.optString("entryKind", "word")),
                            fromCache = true
                        ),
                        updatedAtMillis = obj.optLong("updatedAtMillis", 0L)
                    )
                }
            }.onFailure { err ->
                Log.w(TAG, "Gagal membaca cache AI Kamus, reset cache lokal.", err)
            }
        }
        memoryCache = loaded
        return loaded
    }

    private fun writeCacheLocked(map: LinkedHashMap<String, CachedAiRecord>) {
        val arr = JSONArray()
        map.values.forEach { rec ->
            arr.put(
                JSONObject().apply {
                    put("key", rec.key)
                    put("vocalizedArabic", rec.analysis.vocalizedArabic)
                    put("indonesianMeaning", rec.analysis.indonesianMeaning)
                    put("transliteration", rec.analysis.transliteration)
                    put("turatsOrigin", rec.analysis.turatsOrigin)
                    put("synonymNote", rec.analysis.synonymNote)
                    put("entryKind", rec.analysis.suggestedKind.raw)
                    put("updatedAtMillis", rec.updatedAtMillis)
                }
            )
        }
        val payload = JSONObject().apply {
            put("version", 1)
            put("items", arr)
        }.toString()

        runCatching {
            tempFile.parentFile?.mkdirs()
            tempFile.writeText(payload, Charsets.UTF_8)
            if (cacheFile.exists()) cacheFile.delete()
            if (!tempFile.renameTo(cacheFile)) {
                cacheFile.writeText(payload, Charsets.UTF_8)
                tempFile.delete()
            }
        }.onFailure { err ->
            Log.w(TAG, "Gagal menulis cache AI Kamus", err)
            runCatching { cacheFile.writeText(payload, Charsets.UTF_8) }
        }
    }

    private data class CachedAiRecord(
        val key: String,
        val analysis: KamusAiAnalysis,
        val updatedAtMillis: Long
    )

    companion object {
        private const val TAG = "KamusAiRepository"
        private const val PRIMARY_MODEL_NAME = "gemini-3.8-flash"
        private const val FALLBACK_MODEL_NAME = "gemini-3.5-flash-lite"
        private const val CACHE_FILE_NAME = "kamus_ai_cache_v1.json"
        private const val MAX_CACHE_ENTRIES = 300
        private const val TIMEOUT_MILLIS = 45_000L
        private const val RETRY_DELAY_MILLIS = 1_200L
    }
}
