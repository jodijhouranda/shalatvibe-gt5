package com.jadwalshalat.gt5.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.jadwalshalat.gt5.data.PreferencesManager

enum class PhoneVibrationPattern(
    val key: String,
    val label: String,
    val description: String,
    val timings: LongArray,
    val amplitudes: IntArray
) {
    SHORT(
        key = "SHORT",
        label = "Singkat",
        description = "2 denyut ringan",
        timings = longArrayOf(0L, 220L, 140L, 220L),
        amplitudes = intArrayOf(0, 180, 0, 210)
    ),
    MEDIUM(
        key = "MEDIUM",
        label = "Sedang",
        description = "3 denyut seimbang",
        timings = longArrayOf(0L, 380L, 180L, 380L, 180L, 420L),
        amplitudes = intArrayOf(0, 220, 0, 235, 0, 255)
    ),
    STRONG(
        key = "STRONG",
        label = "Kuat",
        description = "4 denyut tegas Adzan",
        timings = longArrayOf(0L, 600L, 180L, 600L, 180L, 600L, 200L, 750L),
        amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)
    );

    companion object {
        @JvmStatic
        fun fromKey(key: String?): PhoneVibrationPattern {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class PhoneRingerState(val label: String) {
    NORMAL("Normal"),
    VIBRATE("Mode Getar"),
    SILENT("Mode Silent")
}

/**
 * Mengendalikan motor getar fisik HP secara independen dari saluran notifikasi standar.
 * Menggunakan atribut USAGE_ALARM saat opsi "Tembus Silent" aktif agar ponsel tetap bergetar
 * meskipun ringer HP sedang dalam mode Silent / Senyap.
 */
object PhoneVibrationHelper {

    private const val TAG = "PhoneVibrationHelper"

    @JvmStatic
    fun getRingerState(context: Context): PhoneRingerState {
        val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
            ?: return PhoneRingerState.NORMAL
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> PhoneRingerState.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> PhoneRingerState.VIBRATE
            else -> PhoneRingerState.NORMAL
        }
    }

    @JvmStatic
    fun hasVibrator(context: Context): Boolean {
        val vibrator = resolveVibrator(context) ?: return false
        return runCatching { vibrator.hasVibrator() }.getOrDefault(false)
    }

    @JvmStatic
    fun vibrateForPrayer(context: Context, isPreReminder: Boolean): Boolean {
        val appContext = context.applicationContext
        val prefs = PreferencesManager(appContext)
        if (!prefs.isPhoneVibrateEnabled) {
            return false
        }
        if (isPreReminder && !prefs.isPhoneVibrateOnPreReminder) {
            return false
        }
        val pattern = PhoneVibrationPattern.fromKey(prefs.phoneVibratePatternKey)
        return vibrate(
            context = appContext,
            pattern = pattern,
            bypassSilent = prefs.isPhoneVibrateOnSilent
        )
    }

    @JvmStatic
    fun vibrateTestFromPrefs(context: Context): Boolean {
        val appContext = context.applicationContext
        val prefs = PreferencesManager(appContext)
        val pattern = PhoneVibrationPattern.fromKey(prefs.phoneVibratePatternKey)
        return vibrate(
            context = appContext,
            pattern = pattern,
            bypassSilent = prefs.isPhoneVibrateOnSilent
        )
    }

    @JvmStatic
    fun vibratePreview(context: Context, pattern: PhoneVibrationPattern, bypassSilent: Boolean): Boolean {
        return vibrate(
            context = context.applicationContext,
            pattern = pattern,
            bypassSilent = bypassSilent
        )
    }

    @JvmStatic
    fun vibrate(
        context: Context,
        pattern: PhoneVibrationPattern,
        bypassSilent: Boolean
    ): Boolean {
        val appContext = context.applicationContext
        val ringerState = getRingerState(appContext)
        if (!bypassSilent && ringerState == PhoneRingerState.SILENT) {
            return false
        }

        val vibrator = resolveVibrator(appContext) ?: return false
        if (!runCatching { vibrator.hasVibrator() }.getOrDefault(false)) {
            return false
        }

        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(pattern.timings, -1)
        }

        return runCatching {
            vibrator.cancel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val usage = if (bypassSilent) {
                    VibrationAttributes.USAGE_ALARM
                } else {
                    VibrationAttributes.USAGE_NOTIFICATION
                }
                val attrs = VibrationAttributes.Builder()
                    .setUsage(usage)
                    .build()
                vibrator.vibrate(effect, attrs)
            } else {
                val usage = if (bypassSilent) {
                    AudioAttributes.USAGE_ALARM
                } else {
                    AudioAttributes.USAGE_NOTIFICATION_EVENT
                }
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, audioAttrs)
            }
            true
        }.onFailure { err ->
            Log.e(TAG, "Gagal memicu getar HP", err)
        }.getOrDefault(false)
    }

    private fun resolveVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
