package com.jadwalshalat.gt5.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import com.jadwalshalat.gt5.MainActivity;
import com.jadwalshalat.gt5.data.PreferencesManager;
import com.jadwalshalat.gt5.model.PrayerType;
import com.jadwalshalat.gt5.prayer.PrayerEventPlanner;
import com.jadwalshalat.gt5.widget.PrayerWidgetProvider;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Menjadwalkan alarm pengingat shalat berikutnya secara presisi menggunakan AlarmManager.
 */
public final class AlarmScheduler {

    private static final int TEST_DELAY_REQUEST_CODE = 9901;
    private static final int FOLLOW_UP_PING_REQUEST_CODE = 9902;
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

    private AlarmScheduler() {
    }

    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    public static boolean isIgnoringBatteryOptimizations(Context context) {
        PowerManager powerManager = context.getSystemService(PowerManager.class);
        return powerManager != null && powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static PrayerEventPlanner.ScheduledEvent scheduleNext(Context context) {
        cancelAllPrayerAlarms(context);

        PreferencesManager prefs = new PreferencesManager(context);
        if (!prefs.isReminderActive()) {
            PrayerWidgetProvider.updateAllWidgets(context);
            return null;
        }

        ZonedDateTime now = ZonedDateTime.now(prefs.getZoneId());
        PrayerEventPlanner.ScheduledEvent nextEvent = PrayerEventPlanner.findNextEvent(
                now,
                prefs.getLatitude(),
                prefs.getLongitude(),
                prefs.getZoneId(),
                prefs.getCalculationMethod(),
                prefs.isUseIhtiyat(),
                prefs.getEnabledPrayers(),
                prefs.getEffectivePreReminderMap(),
                prefs.isNotifyOnTime()
        );

        if (nextEvent == null) {
            PrayerWidgetProvider.updateAllWidgets(context);
            return null;
        }

        Intent intent = new Intent(context, PrayerAlarmReceiver.class);
        intent.setAction(PrayerAlarmReceiver.ACTION_PRAYER_REMINDER);
        intent.putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_KEY, nextEvent.getPrayerType().getPrefKey());
        intent.putExtra(PrayerAlarmReceiver.EXTRA_IS_PRE_REMINDER, nextEvent.isPreReminder());
        intent.putExtra(PrayerAlarmReceiver.EXTRA_MINUTES_BEFORE, nextEvent.getMinutesBefore());
        intent.putExtra(PrayerAlarmReceiver.EXTRA_FORMATTED_TIME,
                nextEvent.getPrayerTime().format(TIME_FORMATTER));
        intent.putExtra(PrayerAlarmReceiver.EXTRA_IS_FOLLOW_UP, false);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                nextEvent.getRequestCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        scheduleExactOrFallback(context, nextEvent.getTriggerTime().toInstant().toEpochMilli(), pendingIntent);
        PrayerWidgetProvider.updateAllWidgets(context);
        return nextEvent;
    }

    public static void scheduleFollowUpPing(
            Context context,
            PrayerType prayerType,
            String formattedTime,
            boolean isPreReminder,
            int minutesBefore
    ) {
        Intent intent = new Intent(context, PrayerAlarmReceiver.class);
        intent.setAction(PrayerAlarmReceiver.ACTION_PRAYER_REMINDER);
        intent.putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_KEY, prayerType.getPrefKey());
        intent.putExtra(PrayerAlarmReceiver.EXTRA_IS_PRE_REMINDER, isPreReminder);
        intent.putExtra(PrayerAlarmReceiver.EXTRA_MINUTES_BEFORE, minutesBefore);
        intent.putExtra(PrayerAlarmReceiver.EXTRA_FORMATTED_TIME, formattedTime);
        intent.putExtra(PrayerAlarmReceiver.EXTRA_IS_FOLLOW_UP, true);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                FOLLOW_UP_PING_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAt = System.currentTimeMillis() + 30_000L;
        scheduleExactOrFallback(context, triggerAt, pendingIntent);
    }

    public static void scheduleDelayedTestNotification(Context context, int delaySeconds) {
        Intent intent = new Intent(context, PrayerAlarmReceiver.class);
        intent.setAction(PrayerAlarmReceiver.ACTION_TEST_NOTIFICATION);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                TEST_DELAY_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAt = System.currentTimeMillis() + (delaySeconds * 1000L);
        scheduleExactOrFallback(context, triggerAt, pendingIntent);
    }

    public static void cancelAllPrayerAlarms(Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }
        for (PrayerType type : PrayerType.values()) {
            cancelCode(context, alarmManager, type.getPreAlarmRequestCode());
            cancelCode(context, alarmManager, type.getOnTimeAlarmRequestCode());
        }
        cancelCode(context, alarmManager, FOLLOW_UP_PING_REQUEST_CODE);
    }

    private static void cancelCode(Context context, AlarmManager alarmManager, int requestCode) {
        Intent intent = new Intent(context, PrayerAlarmReceiver.class);
        intent.setAction(PrayerAlarmReceiver.ACTION_PRAYER_REMINDER);
        PendingIntent existing = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (existing != null) {
            alarmManager.cancel(existing);
            existing.cancel();
        }
    }

    private static void scheduleExactOrFallback(Context context, long triggerAtMillis,
                                                PendingIntent pendingIntent) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }

        if (canScheduleExactAlarms(context)) {
            PreferencesManager prefs = new PreferencesManager(context);
            if (prefs.isUseAlarmClockMode()) {
                Intent showIntent = new Intent(context, MainActivity.class);
                showIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent showPendingIntent = PendingIntent.getActivity(
                        context,
                        9900,
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.setAlarmClock(
                        new AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                        pendingIntent
                );
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            }
        } else {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }
    }
}
