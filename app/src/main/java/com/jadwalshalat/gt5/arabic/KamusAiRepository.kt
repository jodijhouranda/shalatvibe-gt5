package com.jadwalshalat.gt5.arabic

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.jadwalshalat.gt5.ai.AppCheckDiagnostics
import com.jadwalshalat.gt5.ai.AppCheckInitStatus
import com.jadwalshalat.gt5.tafsir.TafsirTextProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Repositori AI Kamus menggunakan Firebase AI Logic (Agent Platform / Vertex AI backend)
 * agar tagihan langsung memakai Google Cloud Billing & kredit developer,
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
                "vocalizedArabic" to Schema.string(description = "Arab berharakat lengkap sesuai konteks"),
                "indonesianMeaning" to Schema.string(description = "Arti Indonesia kontekstual singkat (1 baris)"),
                "transliteration" to Schema.string(description = "Transliterasi Latin baku"),
                "turatsOrigin" to Schema.string(description = "Akar kata & makna turats ke modern singkat"),
                "synonymNote" to Schema.string(description = "1-2 sinonim & beda nuansa singkat, atau kosong"),
                "entryKind" to Schema.enumeration(
                    values = listOf("word", "phrase", "idiom"),
                    description = "word, phrase, atau idiom"
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
                "Pakar leksikografi Arab-Indonesia (turats & modern). " +
                    "Analisis kata/frase Arab sesuai konteks kalimat secara ringkas dan presisi dalam Bahasa Indonesia, " +
                    "dengan harakat lengkap pada vocalizedArabic."
            )
        }
    }

    private val aiClient by lazy {
        Firebase.ai(backend = GenerativeBackend.agentPlatform(location = "global"))
    }

    private val primaryModel: GenerativeModel by lazy {
        aiClient.generativeModel(
            modelName = PRIMARY_MODEL_NAME,
            generationConfig = sharedGenerationConfig,
            systemInstruction = sharedSystemInstruction
        )
    }

    private val fallbackModel: GenerativeModel by lazy {
        aiClient.generativeModel(
            modelName = FALLBACK_MODEL_NAME,
            generationConfig = sharedGenerationConfig,
            systemInstruction = sharedSystemInstruction
        )
    }

    fun getCached(arabic: String, contextSentence: String): KamusAiAnalysis? {
        val exactKey = buildCacheKey(arabic, contextSentence)
        val normArabic = ArabicTextUtils.normalizeForLookup(arabic)
        if (normArabic.isBlank()) return null
        return synchronized(cacheLock) {
            val map = ensureCacheLoadedLocked()
            val exact = if (exactKey.isNotBlank()) map[exactKey] else null
            val hit = exact ?: map.values.lastOrNull { it.key.startsWith("$normArabic|") }
            hit?.analysis?.copy(fromCache = true)
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

        val trimmedContext = trimContextForAi(contextSentence, cleanArabic)

        if (!forceRefresh) {
            getCached(cleanArabic, trimmedContext)?.let { return it }
        }

        if (FirebaseApp.getApps(appContext).isEmpty()) {
            throw IllegalStateException(
                "Firebase AI belum aktif pada APK ini karena google-services.json tidak disertakan saat build."
            )
        }

        val prompt = buildPrompt(
            arabic = cleanArabic,
            contextSentence = trimmedContext,
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
        saveToCache(cleanArabic, trimmedContext, analysis)
        return analysis
    }

    private suspend fun generateWithFallback(prompt: String) = try {
        primaryModel.generateContent(prompt)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val isQuota = isQuotaError(e)
        val isDemand = isHighDemandError(e)
        if (!isQuota && !isDemand) throw mapActionableError(e)
        Log.w(TAG, "Primary model ($PRIMARY_MODEL_NAME) unavailable/quota, trying $FALLBACK_MODEL_NAME once", e)
        try {
            fallbackModel.generateContent(prompt)
        } catch (e2: CancellationException) {
            throw e2
        } catch (e2: Exception) {
            Log.e(TAG, "Fallback model ($FALLBACK_MODEL_NAME) also failed", e2)
            when {
                isQuotaError(e2) || isQuota -> throw IllegalStateException(
                    "Kuota Vertex AI Gemini sedang padat / batas per menit tercapai (429). Tunggu sebentar lalu coba lagi.",
                    e2
                )
                isHighDemandError(e2) || isDemand -> throw IllegalStateException(
                    "Server Gemini sedang padat sementara. Tunggu beberapa detik lalu tekan Coba Lagi.",
                    e2
                )
                else -> throw mapActionableError(e2)
            }
        }
    }

    private fun mapActionableError(error: Exception): Exception {
        val msg = errorMessageLower(error)
        return when {
            msg.contains("has not been used") ||
                msg.contains("api is not enabled") ||
                msg.contains("aiplatform.googleapis.com") ||
                msg.contains("firebasevertexai.googleapis.com") -> IllegalStateException(
                "Vertex AI / Agent Platform Gemini API belum aktif di Firebase Console. Buka Firebase Console → AI Logic → aktifkan Vertex AI / Agent Platform API untuk project ini.",
                error
            )
            msg.contains("invalid token") ||
                msg.contains("app check") ||
                msg.contains("attestation") ||
                msg.contains("firebase app") -> {
                val hint = when (AppCheckDiagnostics.status) {
                    AppCheckInitStatus.RELEASE_KEY_MISSING ->
                        "Token App Check tidak tersedia (recaptcha_site_key tidak ditemukan pada APK ini)."
                    AppCheckInitStatus.DEBUG_NO_TOKEN ->
                        "Token App Check ditolak karena APK Debug dijalankan di luar preview AI Studio tanpa debug token terdaftar. Gunakan APK Release atau daftarkan debug token perangkat di Firebase Console → App Check."
                    AppCheckInitStatus.RELEASE_RECAPTCHA_INSTALLED ->
                        "Token App Check ditolak Firebase (invalid token). Untuk APK rilis GitHub, pastikan sidik jari SHA-256 keystore rilis sudah didaftarkan di Firebase Console → Project Settings → com.jadwalshalat.gt5, serta cek status reCAPTCHA Enterprise / Enforcement di Firebase Console → App Check → Firebase AI Logic."
                    else ->
                        "Token Firebase App Check tidak valid. Pastikan SHA-256 keystore rilis & konfigurasi App Check di Firebase Console sesuai dengan APK yang terpasang."
                }
                IllegalStateException(hint, error)
            }
            else -> error
        }
    }

    private fun isQuotaError(error: Throwable): Boolean {
        val msg = errorMessageLower(error)
        return msg.contains("quota") ||
            msg.contains("resource_exhausted") ||
            msg.contains("429") ||
            msg.contains("rate limit")
    }

    private fun isHighDemandError(error: Throwable): Boolean {
        val msg = errorMessageLower(error)
        return msg.contains("high demand") ||
            msg.contains("overloaded") ||
            msg.contains("503") ||
            msg.contains("unavailable") ||
            msg.contains("try again later")
    }

    private fun errorMessageLower(error: Throwable): String = buildString {
        append(error.message.orEmpty())
        error.cause?.message?.let { append(' ').append(it) }
    }.lowercase()

    private fun trimContextForAi(contextSentence: String, targetArabic: String): String {
        val normalized = contextSentence.replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank()) return ""
        return if (normalized.length <= MAX_CONTEXT_CHARS) {
            normalized
        } else {
            TafsirTextProcessor.extractContextSnippet(
                fullParagraph = normalized,
                tappedWord = targetArabic,
                maxChars = MAX_CONTEXT_CHARS
            )
        }
    }

    private fun buildPrompt(
        arabic: String,
        contextSentence: String,
        lessonTitle: String
    ): String = buildString {
        append("Target: ").append(arabic)
        val safeContext = trimContextForAi(contextSentence, arabic)
        if (safeContext.isNotBlank()) {
            append("\nKonteks: ").append(safeContext)
        }
        if (lessonTitle.isNotBlank()) {
            append("\nTopik: ").append(lessonTitle.take(MAX_TOPIC_CHARS))
        }
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
        private const val PRIMARY_MODEL_NAME = "gemini-3.5-flash-lite"
        private const val FALLBACK_MODEL_NAME = "gemini-3.8-flash"
        private const val CACHE_FILE_NAME = "kamus_ai_cache_v1.json"
        private const val MAX_CACHE_ENTRIES = 300
        private const val MAX_CONTEXT_CHARS = 200
        private const val MAX_TOPIC_CHARS = 80
        private const val TIMEOUT_MILLIS = 45_000L
    }
}
