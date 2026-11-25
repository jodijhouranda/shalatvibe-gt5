package com.jadwalshalat.gt5.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.jadwalshalat.gt5.data.PreferencesManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Mengatur penjadwalan tugas harian WorkManager (periodik 24 jam sekitar pukul 04:00 lokal
 * sebelum Subuh) dan sinkronisasi instan saat lokasi/pengaturan berubah.
 */
object PrayerWorkScheduler {

    private const val UNIQUE_DAILY_WORK_NAME = "nida_daily_prayer_sync_periodic"
    private const val UNIQUE_IMMEDIATE_WORK_NAME = "nida_daily_prayer_sync_immediate"
    private const val DAILY_SYNC_HOUR = 4
    private const val DAILY_SYNC_MINUTE = 0

    @JvmStatic
    fun ensureDailySyncScheduled(context: Context) {
        val appContext = context.applicationContext
        val prefs = PreferencesManager(appContext)
        val workManager = WorkManager.getInstance(appContext)

        if (!prefs.isReminderActive) {
            cancelDailySync(appContext)
            return
        }

        val initialDelayMinutes = computeDelayToNextDailyAnchorMinutes(
            ZonedDateTime.now(prefs.zoneId)
        )

        val periodicRequest = PeriodicWorkRequestBuilder<DailyPrayerSyncWorker>(
            24L,
            TimeUnit.HOURS,
            30L,
            TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .addTag(UNIQUE_DAILY_WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )

        // Jika hari ini belum pernah disinkronkan, langsung jalankan sekali di background
        val todayIso = ZonedDateTime.now(prefs.zoneId).toLocalDate().toString()
        if (prefs.lastDailySummaryDate != todayIso) {
            val catchUpRequest = OneTimeWorkRequestBuilder<DailyPrayerSyncWorker>()
                .setInputData(workDataOf(DailyPrayerSyncWorker.KEY_FORCE_SUMMARY to false))
                .addTag(UNIQUE_IMMEDIATE_WORK_NAME)
                .build()
            workManager.enqueueUniqueWork(
                UNIQUE_IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                catchUpRequest
            )
        }
    }

    @JvmStatic
    fun triggerImmediateSync(context: Context, forceSummaryNotification: Boolean = false) {
        val appContext = context.applicationContext
        val workManager = WorkManager.getInstance(appContext)

        val request = OneTimeWorkRequestBuilder<DailyPrayerSyncWorker>()
            .setInputData(workDataOf(DailyPrayerSyncWorker.KEY_FORCE_SUMMARY to forceSummaryNotification))
            .addTag(UNIQUE_IMMEDIATE_WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    @JvmStatic
    fun cancelDailySync(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.cancelUniqueWork(UNIQUE_DAILY_WORK_NAME)
        workManager.cancelUniqueWork(UNIQUE_IMMEDIATE_WORK_NAME)
    }

    internal fun computeDelayToNextDailyAnchorMinutes(now: ZonedDateTime): Long {
        var target = now
            .withHour(DAILY_SYNC_HOUR)
            .withMinute(DAILY_SYNC_MINUTE)
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now.plusMinutes(1))) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target).toMinutes().coerceAtLeast(1L)
    }
}
