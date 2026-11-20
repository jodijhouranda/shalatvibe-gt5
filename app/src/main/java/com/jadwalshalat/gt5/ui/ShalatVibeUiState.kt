package com.jadwalshalat.gt5.ui

import com.jadwalshalat.gt5.model.CalculationMethod
import com.jadwalshalat.gt5.model.PrayerSchedule
import com.jadwalshalat.gt5.model.PrayerType
import com.jadwalshalat.gt5.prayer.PrayerEventPlanner
import com.jadwalshalat.gt5.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

enum class MainTab(val label: String) {
    SCHEDULE("Jadwal"),
    ARABIC("Belajar"),
    QIBLA("Kiblat"),
    GT5_HUB("GT 5")
}

data class PrayerRowUi(
    val type: PrayerType,
    val time: ZonedDateTime?,
    val formattedTime: String,
    val isEnabled: Boolean,
    val isNext: Boolean,
    val isPassed: Boolean,
    val effectivePreMinutes: Int,
    val hasCustomOverride: Boolean,
    val reminderSubtitle: String
)

data class MethodPreviewUi(
    val method: CalculationMethod,
    val subuhTime: String,
    val isyaTime: String
)

data class ShalatVibeUiState(
    val now: ZonedDateTime = ZonedDateTime.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val isViewingToday: Boolean = true,
    val gregorianDateLabel: String = "",
    val selectedDateHeaderLabel: String = "",
    val hijriDateLabel: String = "",
    val cityName: String = "Jakarta Pusat",
    val latitude: Double = -6.1754,
    val longitude: Double = 106.8272,
    val zoneId: ZoneId = ZoneId.of("Asia/Jakarta"),
    val calculationMethod: CalculationMethod = CalculationMethod.KEMENAG_RI,
    val useIhtiyat: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    // Hero & Solar Arc
    val currentPhase: PrayerType = PrayerType.DZUHUR,
    val nextPrayer: PrayerType = PrayerType.ASHAR,
    val nextPrayerTimeFormatted: String = "--:--",
    val countdownFormatted: String = "--:--:--",
    val solarProgress: Float = 0f,
    val nextGt5TriggerSummary: String = "",
    val nextScheduledEvent: PrayerEventPlanner.ScheduledEvent? = null,

    // Rows & Previews
    val prayerRows: List<PrayerRowUi> = emptyList(),
    val methodPreviews: List<MethodPreviewUi> = emptyList(),
    val todaySchedule: PrayerSchedule? = null,

    // Reminder settings
    val reminderActive: Boolean = true,
    val globalPreReminderMinutes: Int = 10,
    val notifyOnTime: Boolean = true,
    val doublePing: Boolean = false,
    val useAlarmClockMode: Boolean = true,
    val compactGt5Format: Boolean = true,

    // Readiness & Health Check
    val hasNotificationPermission: Boolean = true,
    val hasExactAlarmPermission: Boolean = true,
    val isIgnoringBatteryOptimizations: Boolean = true,
    val huaweiChecklistStates: List<Boolean> = List(5) { false },
    val lastAlarmLog: String = "",
    val delayedTestCountdown: Int = 0
) {
    val missingCriticalPermissionCount: Int
        get() = listOf(
            !hasNotificationPermission,
            !hasExactAlarmPermission,
            !isIgnoringBatteryOptimizations
        ).count { it }

    val allSystemsReady: Boolean
        get() = hasNotificationPermission && hasExactAlarmPermission && isIgnoringBatteryOptimizations && useAlarmClockMode

    val completedHuaweiSteps: Int
        get() = huaweiChecklistStates.count { it }
}
