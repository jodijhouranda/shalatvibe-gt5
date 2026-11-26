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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File

/**
 * Mengekstrak teks Arab berharakat, terjemahan Indonesia, dan bedah leksikal
 * secara otomatis dari file PDF modul baru (ketika file JSON pre-built belum ada di GitHub)
 * menggunakan Firebase AI Logic (Agent Platform / Vertex AI backend).
 */
class ArabicPdfContentExtractor(context: Context) {

    private val appContext = context.applicationContext

    private val lessonContentSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "transcriptSegments" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "label" to Schema.string(description = "Rentang waktu atau judul bagian, mis. 00:01 – 00:54"),
                            "startSec" to Schema.integer(description = "Detik mulai segmen"),
                            "endSec" to Schema.integer(description = "Detik selesai segmen"),
                            "arabic" to Schema.string(description = "Teks Arab lengkap berharakat (tasykil) dari PDF"),
                            "indonesian" to Schema.string(description = "Terjemahan Bahasa Indonesia yang akurat dari PDF")
                        )
                    ),
                    description = "Daftar segmen transkrip Arab berharakat & terjemahan Indonesia"
                ),
                "semanticShifts" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "arabic" to Schema.string(description = "Kata/istilah Arab berharakat"),
                            "transliteration" to Schema.string(description = "Transliterasi Latin"),
                            "turatsMeaning" to Schema.string(description = "Makna klasik / turats"),
                            "modernMeaning" to Schema.string(description = "Makna kontemporer / modern")
                        ),
                        optionalProperties = listOf("transliteration", "turatsMeaning")
                    ),
                    description = "Pergeseran makna turats ke modern jika ada di PDF"
                ),
                "idioms" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "arabic" to Schema.string(description = "Ungkapan / idiom Arab berharakat"),
                            "transliteration" to Schema.string(description = "Transliterasi Latin"),
                            "literalMeaning" to Schema.string(description = "Arti harfiah"),
                            "idiomaticMeaning" to Schema.string(description = "Arti idiomatik Indonesia"),
                            "exampleArabic" to Schema.string(description = "Contoh kalimat Arab berharakat"),
                            "exampleIndonesian" to Schema.string(description = "Arti contoh kalimat")
                        ),
                        optionalProperties = listOf("transliteration", "literalMeaning", "exampleArabic", "exampleIndonesian")
                    ),
                    description = "Idiom atau ungkapan penting dari PDF"
                ),
                "synonymGroups" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "words" to Schema.array(items = Schema.string(), description = "Daftar kata Arab bersinonim"),
                            "distinction" to Schema.string(description = "Perbedaan nuansa makna antar sinonim"),
                            "collocations" to Schema.array(items = Schema.string(), description = "Contoh kolokasi singkat")
                        ),
                        optionalProperties = listOf("collocations")
                    ),
                    description = "Kelompok sinonim dan beda nuansa jika ada di PDF"
                ),
                "thematicVocab" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "arabic" to Schema.string(description = "Kosakata Arab berharakat"),
                            "indonesian" to Schema.string(description = "Arti Bahasa Indonesia"),
                            "clusterTitle" to Schema.string(description = "Tema/klaster kosakata")
                        ),
                        optionalProperties = listOf("clusterTitle")
                    ),
                    description = "Daftar kosakata tematik penting dari PDF"
                )
            ),
            optionalProperties = listOf("semanticShifts", "idioms", "synonymGroups", "thematicVocab")
        )
    }

    private val sharedGenerationConfig by lazy {
        generationConfig {
            temperature = 0.15f
            responseMimeType = "application/json"
            responseSchema = lessonContentSchema
        }
    }

    private val sharedSystemInstruction by lazy {
        content {
            text(
                "Anda adalah sistem ekstraksi modul pembelajaran Bahasa Arab (TAFL) presisi tinggi. " +
                    "Ekstrak teks Arab lengkap DENGAN HARAKAT (تَشْكِيل) yang benar, terjemahan Bahasa Indonesia, " +
                    "serta daftar bedah leksikal (pergeseran semantik turats-modern, idiom, sinonim, kosakata tematik) " +
                    "secara setia dari dokumen PDF terlampir ke format JSON terstruktur."
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

    suspend fun extractFromPdf(lesson: ArabicLesson, pdfFile: File): Pair<ArabicLessonContent, String> {
        if (!pdfFile.exists() || pdfFile.length() <= MIN_VALID_PDF_BYTES) {
            throw IllegalStateException("File PDF kosong atau belum lengkap sehingga tidak dapat diekstrak.")
        }
        if (pdfFile.length() > MAX_PDF_BYTES) {
            throw IllegalStateException("Ukuran file PDF terlalu besar (> 15 MB) untuk ekstraksi otomatis.")
        }
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            throw IllegalStateException("Firebase AI belum aktif pada aplikasi ini.")
        }

        val pdfBytes = pdfFile.readBytes()
        val prompt = buildExtractionPrompt(lesson)
        val requestContent = content {
            inlineData(pdfBytes, "application/pdf")
            text(prompt)
        }

        val response = withTimeout(TIMEOUT_MILLIS) {
            generateWithFallback(requestContent)
        }

        val rawText = response.text?.trim()
        if (rawText.isNullOrBlank()) {
            throw IllegalStateException("Gemini AI mengembalikan respons kosong saat mengekstrak PDF.")
        }

        // Inject authoritative lessonId & pdfFileName so parser & cache stay 100% consistent
        val rootObj = JSONObject(rawText).apply {
            put("lessonId", lesson.id)
            put("pdfFileName", lesson.pdfFileName)
        }
        val normalizedJson = rootObj.toString()

        val parsed = ArabicContentParser.parseContentJson(normalizedJson)
            ?: throw IllegalStateException("Format JSON hasil ekstraksi PDF tidak valid.")
        if (parsed.transcriptSegments.isEmpty() && parsed.totalGlossaryItems == 0) {
            throw IllegalStateException("PDF tidak memuat teks Arab atau kosakata yang dapat diekstrak.")
        }
        return parsed to normalizedJson
    }

    private suspend fun generateWithFallback(requestContent: com.google.firebase.ai.type.Content) = try {
        primaryModel.generateContent(requestContent)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Primary model ($PRIMARY_MODEL_NAME) gagal mengekstrak PDF, mencoba $FALLBACK_MODEL_NAME", e)
        try {
            fallbackModel.generateContent(requestContent)
        } catch (e2: CancellationException) {
            throw e2
        } catch (e2: Exception) {
            Log.e(TAG, "Fallback model ($FALLBACK_MODEL_NAME) juga gagal mengekstrak PDF", e2)
            throw e2
        }
    }

    private fun buildExtractionPrompt(lesson: ArabicLesson): String = buildString {
        appendLine("Ekstrak seluruh isi materi pembelajaran Bahasa Arab dari PDF terlampir menjadi JSON.")
        appendLine("Judul Modul: ${lesson.title}")
        if (lesson.sourceName.isNotBlank()) {
            appendLine("Sumber: ${lesson.sourceName}")
        }
        if (lesson.segments.isNotEmpty()) {
            appendLine("Gunakan pembagian segmen waktu berikut untuk transcriptSegments:")
            lesson.segments.forEach { seg ->
                appendLine("- label: \"${seg.label}\", startSec: ${seg.startSec}, endSec: ${seg.endSec}")
            }
        } else {
            appendLine("Rentang waktu video: startSec=${lesson.startSeconds}, endSec=${lesson.endSeconds}. Pecah teks menjadi beberapa segmen logis.")
        }
        appendLine("Ketentuan penting:")
        appendLine("1. Pastikan setiap teks Arab pada transcriptSegments memiliki harakat lengkap (tasykil) dan akurat.")
        appendLine("2. Sertakan terjemahan Bahasa Indonesia pada setiap segmen sesuai isi PDF.")
        appendLine("3. Ekstrak tabel/bagian Pergeseran Semantik (semanticShifts), Idiom (idioms), Sinonim (synonymGroups), dan Kosakata Tematik (thematicVocab) bila tersedia di PDF.")
    }

    companion object {
        private const val TAG = "ArabicPdfExtractor"
        private const val PRIMARY_MODEL_NAME = "gemini-3.8-flash"
        private const val FALLBACK_MODEL_NAME = "gemini-3.5-flash-lite"
        private const val MIN_VALID_PDF_BYTES = 100L
        private const val MAX_PDF_BYTES = 15L * 1024L * 1024L
        private const val TIMEOUT_MILLIS = 60_000L
    }
}
