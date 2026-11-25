package com.jadwalshalat.gt5.arabic

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Pembantu Text-to-Speech Bahasa Arab offline untuk pelafalan kata, idiom, maupun kalimat penuh.
 * Menangani inisialisasi aman serta fallback jika suara Bahasa Arab belum terpasang pada mesin TTS perangkat.
 */
class ArabicTtsHelper(context: Context) {

    private var tts: TextToSpeech? = null
    @Volatile
    private var isReady: Boolean = false
    @Volatile
    private var hasArabicLocale: Boolean = false

    init {
        val appContext = context.applicationContext
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts
                    if (engine != null) {
                        val arabicLocales = listOf(
                            Locale("ar", "SA"),
                            Locale("ar", "EG"),
                            Locale("ar")
                        )
                        var configured = false
                        for (loc in arabicLocales) {
                            val res = engine.setLanguage(loc)
                            if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                                configured = true
                                break
                            }
                        }
                        if (!configured) {
                            engine.language = Locale("ar")
                        }
                        engine.setSpeechRate(0.88f)
                        hasArabicLocale = configured
                        isReady = true
                    }
                }
            }
        } catch (_: Throwable) {
            isReady = false
        }
    }

    /**
     * Memutar pelafalan teks Arab. Mengembalikan pesan diagnostik singkat jika TTS Arab belum tersedia.
     */
    fun speak(arabicText: String): String? {
        val clean = arabicText.replace(Regex("[«»()\\[\\]…]"), " ").trim()
        if (clean.isBlank()) return null
        val engine = tts ?: return "Mesin suara perangkat belum siap."
        if (!isReady) {
            return "Menyiapkan suara Bahasa Arab… ketuk sekali lagi."
        }
        return try {
            val status = engine.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "gt5_ar_${System.currentTimeMillis()}")
            if (status == TextToSpeech.ERROR) {
                "Paket suara Arab belum aktif. Aktifkan di Pengaturan Bahasa/TTS HP atau putar klip video."
            } else if (!hasArabicLocale) {
                "Memutar dengan profil TTS bawaan (suara Arab khusus belum terunduh)."
            } else {
                null
            }
        } catch (_: Throwable) {
            "Tidak dapat memutar suara di perangkat ini."
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {
        } finally {
            tts = null
            isReady = false
        }
    }
}
