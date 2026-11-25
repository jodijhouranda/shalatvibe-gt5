package com.jadwalshalat.gt5.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jadwalshalat.gt5.alarm.AlarmScheduler
import com.jadwalshalat.gt5.data.PreferencesManager
import com.jadwalshalat.gt5.model.PrayerType
import com.jadwalshalat.gt5.notification.NotificationHelper
import com.jadwalshalat.gt5.prayer.PrayerTimeCalculator
import com.jadwalshalat.gt5.widget.PrayerWidgetProvider
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Worker harian WorkManager untuk:
 * 1. Membaca lokasi GPS terakhir yang tersimpan di perangkat (hemat baterai, tanpa izin background GPS).
 * 2. Menghitung ulang jadwal shalat harian & memastikan AlarmManager tepat waktu selalu terpasang.
 * 3. Mengirim notifikasi ringkasan jadwal 5 waktu harian (opsional, maksimal 1x/hari atau saat diminta).
 * 4. Memperbarui Home Screen Widget dan mencatat stempel waktu sinkronisasi terakhir.
 */
class DailyPrayerSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = PreferencesManager(context)

        return try {
            val zoneId = prefs.zoneId
            val now = ZonedDateTime.now(zoneId)
            val today = now.toLocalDate()
            val todayIso = today.toString()
            val cityName = prefs.cityName
            val forceSummary = inputData.getBoolean(KEY_FORCE_SUMMARY, false)

            if (!prefs.isReminderActive) {
                AlarmScheduler.cancelAllPrayerAlarms(context)
                PrayerWidgetProvider.updateAllWidgets(context)
                prefs.lastWorkSyncLog = "WorkManager aktif, namun pengingat utama sedang nonaktif."
                return Result.success()
            }

            val schedule = PrayerTimeCalculator.calculate(
                today,
                prefs.latitude,
                prefs.longitude,
                zoneId,
                prefs.calculationMethod,
                prefs.isUseIhtiyat
            )

            // Re-arm exact AlarmManager untuk pengingat / Adzan berikutnya
            val nextEvent = AlarmScheduler.scheduleNext(context)
            PrayerWidgetProvider.updateAllWidgets(context)

            // Kirim notifikasi ringkasan jadwal harian jika diaktifkan (sekali sehari atau saat tombol sinkron ditekan)
            val shouldNotifySummary = prefs.isDailySummaryEnabled &&
                (forceSummary || prefs.lastDailySummaryDate != todayIso)

            if (shouldNotifySummary && NotificationHelper.hasNotificationPermission(context)) {
                val idLocale = Locale("id", "ID")
                val dateFmt = DateTimeFormatter.ofPattern("d MMM", idLocale)
                val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

                val subuh = schedule.getTime(PrayerType.SUBUH)?.format(timeFmt) ?: "--:--"
                val dzuhur = schedule.getTime(PrayerType.DZUHUR)?.format(timeFmt) ?: "--:--"
                val ashar = schedule.getTime(PrayerType.ASHAR)?.format(timeFmt) ?: "--:--"
                val maghrib = schedule.getTime(PrayerType.MAGHRIB)?.format(timeFmt) ?: "--:--"
                val isya = schedule.getTime(PrayerType.ISYA)?.format(timeFmt) ?: "--:--"

                val compactLine = "Subuh $subuh • Dzuhur $dzuhur • Ashar $ashar • Maghrib $maghrib • Isya $isya"
                val expandedBody = buildString {
                    append("Subuh: ").append(subuh)
                    append("  |  Dzuhur: ").append(dzuhur)
                    append("\nAshar: ").append(ashar)
                    append("  |  Maghrib: ").append(maghrib)
                    append("\nIsya: ").append(isya)
                    if (nextEvent != null) {
                        append("\nPengingat berikut: ")
                        append(nextEvent.prayerType.displayName)
                        append(" pukul ")
                        append(nextEvent.triggerTime.format(timeFmt))
                    }
                }

                NotificationHelper.showDailyScheduleSummary(
                    context,
                    cityName,
                    today.format(dateFmt),
                    compactLine,
                    expandedBody
                )
                prefs.lastDailySummaryDate = todayIso
            }

            val logFmt = DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale("id", "ID"))
            val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            val nextInfo = if (nextEvent != null) {
                " • Berikutnya ${nextEvent.prayerType.displayName} ${nextEvent.triggerTime.format(timeFmt)}"
            } else {
                ""
            }
            prefs.lastWorkSyncLog = "Sinkron $cityName (${now.format(logFmt)})$nextInfo"
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menjalankan DailyPrayerSyncWorker", e)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "DailyPrayerSyncWorker"
        const val KEY_FORCE_SUMMARY = "force_summary"
    }
}
