package com.jadwalshalat.gt5.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.jadwalshalat.gt5.MainActivity
import com.jadwalshalat.gt5.R
import com.jadwalshalat.gt5.data.PreferencesManager
import com.jadwalshalat.gt5.model.PrayerType
import com.jadwalshalat.gt5.prayer.HijriCalendarHelper
import com.jadwalshalat.gt5.prayer.PrayerEventPlanner
import com.jadwalshalat.gt5.prayer.PrayerTimeCalculator
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Widget layar utama (Home Screen Widget) ShalatVibe GT5 yang menampilkan
 * hitung mundur shalat berikutnya, jadwal getar GT 5, dan ringkasan 5 waktu shalat.
 */
class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

        @JvmStatic
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, PrayerWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    updateSingleWidget(context, manager, id)
                }
            }
        }

        private fun updateSingleWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int
        ) {
            val prefs = PreferencesManager(context)
            val zoneId = prefs.zoneId
            val now = ZonedDateTime.now(zoneId)
            val today = now.toLocalDate()
            val lat = prefs.latitude
            val lon = prefs.longitude
            val method = prefs.calculationMethod
            val useIhtiyat = prefs.isUseIhtiyat

            val yesterday = PrayerTimeCalculator.calculate(today.minusDays(1), lat, lon, zoneId, method, useIhtiyat)
            val todaySchedule = PrayerTimeCalculator.calculate(today, lat, lon, zoneId, method, useIhtiyat)
            val tomorrowSchedule = PrayerTimeCalculator.calculate(today.plusDays(1), lat, lon, zoneId, method, useIhtiyat)

            val solar = PrayerEventPlanner.computeSolarWindow(yesterday, todaySchedule, tomorrowSchedule, now)
            val nextPrayer = solar.nextPrayer
            val nextTime = solar.nextPrayerTime

            val remaining = if (nextTime != null) Duration.between(now, nextTime) else Duration.ZERO
            val totalMinutes = remaining.toMinutes().coerceAtLeast(0L)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            val countdownText = if (hours > 0) {
                "${hours}j ${mins}m lagi"
            } else {
                "$mins mnt lagi"
            }

            val nextEvent = if (prefs.isReminderActive) {
                PrayerEventPlanner.findNextEvent(
                    now, lat, lon, zoneId, method, useIhtiyat,
                    prefs.enabledPrayers,
                    prefs.effectivePreReminderMap,
                    prefs.isNotifyOnTime
                )
            } else {
                null
            }

            val gt5Text = when {
                !prefs.isReminderActive -> "GT 5 nonaktif"
                nextEvent != null -> {
                    val triggerStr = nextEvent.triggerTime.format(TIME_FORMATTER)
                    "GT 5 • $triggerStr"
                }
                else -> "GT 5 siap"
            }

            val hijriStr = HijriCalendarHelper.formatIndonesianHijriDate(today)

            val views = RemoteViews(context.packageName, R.layout.widget_prayer_schedule)
            views.setTextViewText(R.id.tvWidgetCity, prefs.cityName)
            views.setTextViewText(R.id.tvWidgetHijri, hijriStr)
            views.setTextViewText(
                R.id.tvWidgetNextPrayer,
                "Menuju ${nextPrayer.displayName} • ${nextTime?.format(TIME_FORMATTER) ?: "--:--"}"
            )
            views.setTextViewText(R.id.tvWidgetCountdown, countdownText)
            views.setTextViewText(R.id.tvWidgetGt5Badge, gt5Text)

            val fivePrayers = listOf(
                Triple(PrayerType.SUBUH, R.id.tvWidgetSubuhTime, R.id.colWidgetSubuh),
                Triple(PrayerType.DZUHUR, R.id.tvWidgetDzuhurTime, R.id.colWidgetDzuhur),
                Triple(PrayerType.ASHAR, R.id.tvWidgetAsharTime, R.id.colWidgetAshar),
                Triple(PrayerType.MAGHRIB, R.id.tvWidgetMaghribTime, R.id.colWidgetMaghrib),
                Triple(PrayerType.ISYA, R.id.tvWidgetIsyaTime, R.id.colWidgetIsya)
            )

            val activeBg = R.drawable.bg_widget_highlight
            val inactiveBg = android.R.color.transparent
            val activeTextColor = Color.parseColor("#34D399")
            val normalTextColor = Color.parseColor("#F1F5F9")

            for ((type, timeViewId, colViewId) in fivePrayers) {
                val timeStr = todaySchedule.getTime(type)?.format(TIME_FORMATTER) ?: "--:--"
                val isNext = (type == nextPrayer)
                views.setTextViewText(timeViewId, timeStr)
                views.setTextColor(timeViewId, if (isNext) activeTextColor else normalTextColor)
                views.setInt(colViewId, "setBackgroundResource", if (isNext) activeBg else inactiveBg)
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                8800,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            manager.updateAppWidget(widgetId, views)
        }
    }
}
