package com.jadwalshalat.gt5.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jadwalshalat.gt5.data.PreferencesManager;
import com.jadwalshalat.gt5.model.PrayerType;
import com.jadwalshalat.gt5.notification.NotificationHelper;

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

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

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

            if (!isFollowUp) {
                if (prefs.isDoublePing() && isPreReminder) {
                    AlarmScheduler.scheduleFollowUpPing(
                            context, prayerType, formattedTime, true, minutesBefore
                    );
                }
                AlarmScheduler.scheduleNext(context);
            }
        }
    }
}
