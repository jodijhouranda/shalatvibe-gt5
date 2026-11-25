package com.jadwalshalat.gt5.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jadwalshalat.gt5.alarm.AlarmScheduler
import com.jadwalshalat.gt5.data.PreferencesManager
import com.jadwalshalat.gt5.location.LocationHelper
import com.jadwalshalat.gt5.model.CalculationMethod
import com.jadwalshalat.gt5.model.CityPreset
import com.jadwalshalat.gt5.model.PrayerType
import com.jadwalshalat.gt5.notification.NotificationHelper
import com.jadwalshalat.gt5.notification.PhoneRingerState
import com.jadwalshalat.gt5.notification.PhoneVibrationHelper
import com.jadwalshalat.gt5.notification.PhoneVibrationPattern
import com.jadwalshalat.gt5.prayer.HijriCalendarHelper
import com.jadwalshalat.gt5.prayer.PrayerEventPlanner
import com.jadwalshalat.gt5.prayer.PrayerTimeCalculator
import com.jadwalshalat.gt5.ui.theme.ThemeMode
import com.jadwalshalat.gt5.work.PrayerWorkScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ShalatVibeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = PreferencesManager(appContext)

    private val idLocale = Locale("id", "ID")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val shortDateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", idLocale)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", idLocale)

    private val _uiState = MutableStateFlow(ShalatVibeUiState())
    val uiState: StateFlow<ShalatVibeUiState> = _uiState.asStateFlow()

    private var selectedDate: LocalDate = LocalDate.now(prefs.zoneId)
    private var tickerJob: Job? = null
    private var delayedTestJob: Job? = null
    private var immediateBurstJob: Job? = null

    init {
        NotificationHelper.ensureChannel(appContext)
        refreshAll(rescheduleAlarm = true)
        startClockTicker()
    }

    private fun startClockTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                refreshAll(rescheduleAlarm = false)
            }
        }
    }

    fun onAppResumed() {
        refreshAll(rescheduleAlarm = true)
    }

    fun refreshAll(rescheduleAlarm: Boolean = false) {
        if (rescheduleAlarm) {
            AlarmScheduler.scheduleNext(appContext)
            PrayerWorkScheduler.ensureDailySyncScheduled(appContext)
        }

        val zoneId = prefs.zoneId
        val now = ZonedDateTime.now(zoneId)
        val today = now.toLocalDate()

        // Pastikan jika tanggal sistem berganti tengah malam saat melihat hari ini, ikut berpindah
        val wasToday = _uiState.value.isViewingToday
        if (wasToday && selectedDate != today) {
            selectedDate = today
        }

        val lat = prefs.latitude
        val lon = prefs.longitude
        val method = prefs.calculationMethod
        val useIhtiyat = prefs.isUseIhtiyat
        val reminderActive = prefs.isReminderActive
        val globalPre = prefs.preReminderMinutes
        val notifyOnTime = prefs.isNotifyOnTime
        val adzanTripleBurst = prefs.isAdzanTripleBurst
        val doublePing = prefs.isDoublePing
        val alarmClockMode = prefs.isUseAlarmClockMode
        val compactGt5 = prefs.isCompactGt5Format
        val themeMode = ThemeMode.fromName(prefs.themeModeName)

        val yesterdaySchedule = PrayerTimeCalculator.calculate(
            today.minusDays(1), lat, lon, zoneId, method, useIhtiyat
        )
        val todaySchedule = PrayerTimeCalculator.calculate(
            today, lat, lon, zoneId, method, useIhtiyat
        )
        val tomorrowSchedule = PrayerTimeCalculator.calculate(
            today.plusDays(1), lat, lon, zoneId, method, useIhtiyat
        )
        val displayedSchedule = if (selectedDate == today) {
            todaySchedule
        } else {
            PrayerTimeCalculator.calculate(selectedDate, lat, lon, zoneId, method, useIhtiyat)
        }

        val solar = PrayerEventPlanner.computeSolarWindow(
            yesterdaySchedule, todaySchedule, tomorrowSchedule, now
        )

        val remainingDuration = if (solar.nextPrayerTime != null) {
            Duration.between(now, solar.nextPrayerTime)
        } else {
            Duration.ZERO
        }
        val totalSeconds = remainingDuration.seconds.coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val countdownFormatted = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

        val nextEvent = if (reminderActive) {
            PrayerEventPlanner.findNextEvent(
                now, lat, lon, zoneId, method, useIhtiyat,
                prefs.enabledPrayers,
                prefs.effectivePreReminderMap,
                notifyOnTime
            )
        } else {
            null
        }

        val nextGt5Summary = when {
            !reminderActive -> "Pengingat otomatis GT 5 nonaktif"
            nextEvent != null -> {
                val triggerStr = nextEvent.triggerTime.format(timeFormatter)
                val kind = if (nextEvent.isPreReminder) {
                    "${nextEvent.minutesBefore} mnt sebelum ${nextEvent.prayerType.displayName}"
                } else {
                    val burstSuffix = if (adzanTripleBurst) " (3x getar)" else ""
                    "Tepat waktu ${nextEvent.prayerType.displayName}$burstSuffix"
                }
                "Getar GT 5 pukul $triggerStr • $kind"
            }
            else -> "Belum ada waktu shalat yang diaktifkan"
        }

        val isViewingToday = (selectedDate == today)
        val prayerRows = PrayerType.entries.map { type ->
            val time = displayedSchedule.getTime(type)
            val enabled = prefs.isPrayerEnabled(type)
            val overrideVal = prefs.getPrayerPreReminderOverride(type)
            val effectivePre = if (overrideVal >= 0) overrideVal else globalPre
            val hasCustom = overrideVal >= 0
            val isNext = isViewingToday && type == solar.nextPrayer && time != null && time.isAfter(now)
            val isPassed = isViewingToday && time != null && !time.isAfter(now)

            val subtitle = when {
                !reminderActive -> "Pengingat utama nonaktif"
                !enabled -> "Getar nonaktif"
                time != null && effectivePre > 0 -> {
                    val preTimeStr = time.minusMinutes(effectivePre.toLong()).format(timeFormatter)
                    val customTag = if (hasCustom) " • Khusus" else ""
                    "Getar $effectivePre mnt sebelum ($preTimeStr)$customTag"
                }
                notifyOnTime -> {
                    val burstTag = if (adzanTripleBurst) " 3x" else ""
                    "Getar$burstTag tepat saat Adzan" + if (hasCustom) " • Khusus" else ""
                }
                else -> "Hanya jadwal tampil"
            }

            PrayerRowUi(
                type = type,
                time = time,
                formattedTime = time?.format(timeFormatter) ?: "--:--",
                isEnabled = enabled,
                isNext = isNext,
                isPassed = isPassed,
                effectivePreMinutes = effectivePre,
                hasCustomOverride = hasCustom,
                reminderSubtitle = subtitle
            )
        }

        val methodPreviews = CalculationMethod.entries.map { m ->
            val previewSched = PrayerTimeCalculator.calculate(today, lat, lon, zoneId, m, useIhtiyat)
            MethodPreviewUi(
                method = m,
                subuhTime = previewSched.getTime(PrayerType.SUBUH)?.format(timeFormatter) ?: "--:--",
                isyaTime = previewSched.getTime(PrayerType.ISYA)?.format(timeFormatter) ?: "--:--"
            )
        }

        val selectedHeader = when (selectedDate) {
            today -> "Hari Ini • ${selectedDate.format(fullDateFormatter)}"
            today.plusDays(1) -> "Besok • ${selectedDate.format(fullDateFormatter)}"
            today.minusDays(1) -> "Kemarin • ${selectedDate.format(fullDateFormatter)}"
            else -> selectedDate.format(fullDateFormatter)
        }

        val checklist = List(5) { idx -> prefs.isHuaweiStepChecked(idx) }

        _uiState.update { current ->
            current.copy(
                now = now,
                selectedDate = selectedDate,
                isViewingToday = isViewingToday,
                gregorianDateLabel = today.format(shortDateFormatter),
                selectedDateHeaderLabel = selectedHeader,
                hijriDateLabel = HijriCalendarHelper.formatIndonesianHijriDate(selectedDate),
                cityName = prefs.cityName,
                latitude = lat,
                longitude = lon,
                zoneId = zoneId,
                calculationMethod = method,
                useIhtiyat = useIhtiyat,
                themeMode = themeMode,
                currentPhase = solar.currentPhase,
                nextPrayer = solar.nextPrayer,
                nextPrayerTimeFormatted = solar.nextPrayerTime?.format(timeFormatter) ?: "--:--",
                countdownFormatted = countdownFormatted,
                solarProgress = solar.progress,
                nextGt5TriggerSummary = nextGt5Summary,
                nextScheduledEvent = nextEvent,
                prayerRows = prayerRows,
                methodPreviews = methodPreviews,
                todaySchedule = todaySchedule,
                reminderActive = reminderActive,
                globalPreReminderMinutes = globalPre,
                notifyOnTime = notifyOnTime,
                adzanTripleBurst = adzanTripleBurst,
                doublePing = doublePing,
                useAlarmClockMode = alarmClockMode,
                compactGt5Format = compactGt5,
                phoneVibrateEnabled = prefs.isPhoneVibrateEnabled,
                phoneVibrateOnSilent = prefs.isPhoneVibrateOnSilent,
                phoneVibrateOnPreReminder = prefs.isPhoneVibrateOnPreReminder,
                phoneVibratePattern = PhoneVibrationPattern.fromKey(prefs.phoneVibratePatternKey),
                phoneRingerState = PhoneVibrationHelper.getRingerState(appContext),
                hasDeviceVibrator = PhoneVibrationHelper.hasVibrator(appContext),
                dailySummaryEnabled = prefs.isDailySummaryEnabled,
                lastWorkSyncLog = prefs.lastWorkSyncLog,
                hasNotificationPermission = NotificationHelper.hasNotificationPermission(appContext),
                hasExactAlarmPermission = AlarmScheduler.canScheduleExactAlarms(appContext),
                isIgnoringBatteryOptimizations = AlarmScheduler.isIgnoringBatteryOptimizations(appContext),
                huaweiChecklistStates = checklist,
                lastAlarmLog = prefs.lastAlarmLog
            )
        }
    }

    fun selectPreviousDay() {
        selectedDate = selectedDate.minusDays(1)
        refreshAll(rescheduleAlarm = false)
    }

    fun selectNextDay() {
        selectedDate = selectedDate.plusDays(1)
        refreshAll(rescheduleAlarm = false)
    }

    fun resetToToday() {
        selectedDate = LocalDate.now(prefs.zoneId)
        refreshAll(rescheduleAlarm = false)
    }

    fun selectCityPreset(preset: CityPreset) {
        prefs.setCityPreset(preset)
        selectedDate = LocalDate.now(prefs.zoneId)
        refreshAll(rescheduleAlarm = true)
        if (prefs.isReminderActive) {
            PrayerWorkScheduler.triggerImmediateSync(appContext, forceSummaryNotification = false)
        }
    }

    fun applyGpsLocation(): String? {
        val detected = LocationHelper.getBestLastKnownLocation(appContext) ?: return null
        prefs.setLocation(detected.cityName, detected.latitude, detected.longitude, detected.zoneId)
        selectedDate = LocalDate.now(prefs.zoneId)
        refreshAll(rescheduleAlarm = true)
        if (prefs.isReminderActive) {
            PrayerWorkScheduler.triggerImmediateSync(appContext, forceSummaryNotification = false)
        }
        return detected.cityName
    }

    fun setCalculationMethod(method: CalculationMethod) {
        prefs.calculationMethod = method
        refreshAll(rescheduleAlarm = true)
    }

    fun setUseIhtiyat(enabled: Boolean) {
        prefs.isUseIhtiyat = enabled
        refreshAll(rescheduleAlarm = true)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.themeModeName = mode.name
        refreshAll(rescheduleAlarm = false)
    }

    fun setReminderActive(active: Boolean) {
        prefs.isReminderActive = active
        refreshAll(rescheduleAlarm = true)
    }

    fun setGlobalPreReminderMinutes(minutes: Int) {
        prefs.preReminderMinutes = minutes
        refreshAll(rescheduleAlarm = true)
    }

    fun setNotifyOnTime(enabled: Boolean) {
        prefs.isNotifyOnTime = enabled
        refreshAll(rescheduleAlarm = true)
    }

    fun setAdzanTripleBurst(enabled: Boolean) {
        prefs.isAdzanTripleBurst = enabled
        refreshAll(rescheduleAlarm = false)
    }

    fun setDoublePing(enabled: Boolean) {
        prefs.isDoublePing = enabled
        refreshAll(rescheduleAlarm = false)
    }

    fun setUseAlarmClockMode(enabled: Boolean) {
        prefs.isUseAlarmClockMode = enabled
        refreshAll(rescheduleAlarm = true)
    }

    fun setCompactGt5Format(compact: Boolean) {
        prefs.isCompactGt5Format = compact
        refreshAll(rescheduleAlarm = false)
    }

    fun togglePrayerEnabled(type: PrayerType) {
        val nextVal = !prefs.isPrayerEnabled(type)
        prefs.setPrayerEnabled(type, nextVal)
        refreshAll(rescheduleAlarm = true)
    }

    fun setPrayerEnabled(type: PrayerType, enabled: Boolean) {
        prefs.setPrayerEnabled(type, enabled)
        refreshAll(rescheduleAlarm = true)
    }

    fun setPrayerPreReminderOverride(type: PrayerType, minutesOrFollowGlobal: Int) {
        prefs.setPrayerPreReminderOverride(type, minutesOrFollowGlobal)
        refreshAll(rescheduleAlarm = true)
    }

    fun getPrayerPreReminderOverride(type: PrayerType): Int {
        return prefs.getPrayerPreReminderOverride(type)
    }

    fun toggleHuaweiChecklistStep(stepIndex: Int) {
        val next = !prefs.isHuaweiStepChecked(stepIndex)
        prefs.setHuaweiStepChecked(stepIndex, next)
        refreshAll(rescheduleAlarm = false)
    }

    fun setPhoneVibrateEnabled(enabled: Boolean) {
        prefs.isPhoneVibrateEnabled = enabled
        refreshAll(rescheduleAlarm = false)
    }

    fun setPhoneVibrateOnSilent(enabled: Boolean) {
        prefs.isPhoneVibrateOnSilent = enabled
        refreshAll(rescheduleAlarm = false)
    }

    fun setPhoneVibrateOnPreReminder(enabled: Boolean) {
        prefs.isPhoneVibrateOnPreReminder = enabled
        refreshAll(rescheduleAlarm = false)
    }

    fun setPhoneVibratePattern(pattern: PhoneVibrationPattern) {
        prefs.phoneVibratePatternKey = pattern.key
        refreshAll(rescheduleAlarm = false)
        if (prefs.isPhoneVibrateEnabled) {
            PhoneVibrationHelper.vibratePreview(
                context = appContext,
                pattern = pattern,
                bypassSilent = prefs.isPhoneVibrateOnSilent
            )
        }
    }

    fun testPhoneVibration(): String {
        val ringer = PhoneVibrationHelper.getRingerState(appContext)
        val pattern = PhoneVibrationPattern.fromKey(prefs.phoneVibratePatternKey)
        val bypassSilent = prefs.isPhoneVibrateOnSilent
        if (!PhoneVibrationHelper.hasVibrator(appContext)) {
            return "Perangkat ini tidak memiliki motor getar."
        }
        if (ringer == PhoneRingerState.SILENT && !bypassSilent) {
            return "HP sedang Silent dan opsi Tembus Silent nonaktif — aktifkan agar HP tetap bergetar."
        }
        val triggered = PhoneVibrationHelper.vibratePreview(
            context = appContext,
            pattern = pattern,
            bypassSilent = bypassSilent
        )
        return when {
            !triggered -> "Gagal memicu getar HP. Cek pengaturan getar sistem."
            ringer == PhoneRingerState.SILENT -> "Menguji getar ${pattern.label} menembus mode Silent!"
            else -> "Menguji getar HP pola ${pattern.label}."
        }
    }

    fun setDailySummaryEnabled(enabled: Boolean) {
        prefs.isDailySummaryEnabled = enabled
        refreshAll(rescheduleAlarm = true)
    }

    fun triggerManualDailyWorkSync(): String {
        val forceSummary = prefs.isDailySummaryEnabled
        PrayerWorkScheduler.ensureDailySyncScheduled(appContext)
        PrayerWorkScheduler.triggerImmediateSync(appContext, forceSummaryNotification = forceSummary)
        viewModelScope.launch {
            delay(1_200L)
            refreshAll(rescheduleAlarm = false)
        }
        return if (forceSummary && NotificationHelper.hasNotificationPermission(appContext)) {
            "WorkManager menyinkronkan jadwal ${prefs.cityName} & mengirim ringkasan harian."
        } else {
            "WorkManager menyinkronkan jadwal & alarm untuk ${prefs.cityName}."
        }
    }

    fun sendImmediateTestNotification(): Boolean {
        if (!NotificationHelper.hasNotificationPermission(appContext)) {
            return false
        }
        if (prefs.isPhoneVibrateEnabled) {
            PhoneVibrationHelper.vibrateTestFromPrefs(appContext)
        }
        val cityName = prefs.cityName
        val compactGt5 = prefs.isCompactGt5Format
        if (prefs.isAdzanTripleBurst) {
            immediateBurstJob?.cancel()
            immediateBurstJob = viewModelScope.launch {
                val baseId = 900000 + ((System.currentTimeMillis() / 10L) % 80000L).toInt()
                val sentIds = IntArray(NotificationHelper.ADZAN_BURST_COUNT)
                try {
                    for (i in 0 until NotificationHelper.ADZAN_BURST_COUNT) {
                        _uiState.update { it.copy(activeTestBurstPulse = i + 1) }
                        sentIds[i] = NotificationHelper.showTestBurstPulse(
                            appContext, cityName, compactGt5, baseId, i
                        )
                        if (i < NotificationHelper.ADZAN_BURST_COUNT - 1) {
                            delay(NotificationHelper.ADZAN_BURST_INTERVAL_MS)
                        }
                    }
                    delay(NotificationHelper.ADZAN_BURST_CLEANUP_DELAY_MS)
                    NotificationHelper.cancelNotifications(appContext, sentIds[0], sentIds[1])
                } finally {
                    _uiState.update { it.copy(activeTestBurstPulse = 0) }
                }
            }
        } else {
            NotificationHelper.showTestNotification(appContext, cityName, compactGt5)
        }
        return true
    }

    fun triggerDelayedTestNotification(seconds: Int = 5): Boolean {
        if (!NotificationHelper.hasNotificationPermission(appContext)) {
            return false
        }
        AlarmScheduler.scheduleDelayedTestNotification(appContext, seconds)
        delayedTestJob?.cancel()
        delayedTestJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _uiState.update { it.copy(delayedTestCountdown = remaining) }
                delay(1_000L)
            }
            _uiState.update { it.copy(delayedTestCountdown = 0) }
        }
        return true
    }
}
