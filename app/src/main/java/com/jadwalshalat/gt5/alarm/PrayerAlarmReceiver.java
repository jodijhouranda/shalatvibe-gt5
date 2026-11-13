package com.jadwalshalat.gt5.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.jadwalshalat.gt5.data.PreferencesManager;
import com.jadwalshalat.gt5.model.PrayerType;
import com.jadwalshalat.gt5.notification.NotificationHelper;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Menerima sinyal alarm dari sistem untuk menampilkan notifikasi pengingat shalat
 * lalu menjadwalkan alarm berikutnya.
 */
public class PrayerAlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_PRAYER_REMINDER = "com.jadwalshalat.gt5.ACTION_PRAYER_REMINDER";
    public static final String ACTION_TEST_NOTIFICATION = "com.jadwalshalat.gt5.ACTION_TEST_NOTIFICATION";

    public static final String EXTRA_PRAYER_KEY = "extra_prayer_key";
    public static final String EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder";
    public static final String EXTRA_MINUTES_BEFORE = "extra_minutes_before";
    public static final String EXTRA_FORMATTED_TIME = "extra_formatted_time";
    public static final String EXTRA_IS_FOLLOW_UP = "extra_is_follow_up";

    private static final DateTimeFormatter LOG_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM HH:mm:ss", new Locale("id", "ID"));

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        PowerManager pm = context.getSystemService(PowerManager.class);
        PowerManager.WakeLock wakeLock = null;
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ShalatVibeGT5:AlarmWakeLock");
            wakeLock.acquire(10_000L);
        }

        try {
            PreferencesManager prefs = new PreferencesManager(context);

            if (ACTION_TEST_NOTIFICATION.equals(intent.getAction())) {
                NotificationHelper.showTestNotification(context, prefs.getCityName());
                return;
            }

            if (ACTION_PRAYER_REMINDER.equals(intent.getAction())) {
                String prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY);
                PrayerType prayerType = PrayerType.fromPrefKey(prayerKey);
                boolean isPreReminder = intent.getBooleanExtra(EXTRA_IS_PRE_REMINDER, true);
                int minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, prefs.getPreReminderMinutes());
                String formattedTime = intent.getStringExtra(EXTRA_FORMATTED_TIME);
                if (formattedTime == null) {
                    formattedTime = "--:--";
                }
                boolean isFollowUp = intent.getBooleanExtra(EXTRA_IS_FOLLOW_UP, false);

                NotificationHelper.showPrayerReminder(
                        context,
                        prayerType,
                        formattedTime,
                        prefs.getCityName(),
                        isPreReminder,
                        minutesBefore,
                        isFollowUp
                );

                String nowStr = ZonedDateTime.now(prefs.getZoneId()).format(LOG_FORMATTER);
                String kind = isPreReminder ? (minutesBefore + " mnt sebelum") : "tepat waktu";
                prefs.setLastAlarmLog("Terkirim: " + prayerType.getDisplayName()
                        + " (" + kind + ") pada " + nowStr);

                if (!isFollowUp) {
                    if (prefs.isDoublePing() && isPreReminder) {
                        AlarmScheduler.scheduleFollowUpPing(
                                context, prayerType, formattedTime, true, minutesBefore
                        );
                    }
                    AlarmScheduler.scheduleNext(context);
                }
            }
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }
}
