package com.jadwalshalat.gt5.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.jadwalshalat.gt5.data.PreferencesManager;
import com.jadwalshalat.gt5.model.PrayerType;
import com.jadwalshalat.gt5.notification.NotificationHelper;
import com.jadwalshalat.gt5.notification.PhoneVibrationHelper;
import com.jadwalshalat.gt5.widget.PrayerWidgetProvider;
import com.jadwalshalat.gt5.work.PrayerWorkScheduler;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static final ExecutorService BURST_EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        final Context appContext = context.getApplicationContext();
        PowerManager pm = appContext.getSystemService(PowerManager.class);
        PowerManager.WakeLock wakeLock = null;
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ShalatVibeGT5:AlarmWakeLock");
            wakeLock.acquire(12_000L);
        }

        boolean handedOffToAsync = false;
        try {
            PreferencesManager prefs = new PreferencesManager(appContext);

            if (ACTION_TEST_NOTIFICATION.equals(intent.getAction())) {
                if (prefs.isPhoneVibrateEnabled()) {
                    PhoneVibrationHelper.vibrateTestFromPrefs(appContext);
                }
                if (prefs.isAdzanTripleBurst()) {
                    final String cityName = prefs.getCityName();
                    final boolean compactGt5 = prefs.isCompactGt5Format();
                    final int baseId = 900000 + (int) ((System.currentTimeMillis() / 10L) % 80000L);
                    final int firstId = NotificationHelper.showTestBurstPulse(
                            appContext, cityName, compactGt5, baseId, 0
                    );
                    if (firstId > 0) {
                        handedOffToAsync = runRemainingBurstAsync(
                                wakeLock,
                                firstId,
                                pulseIndex -> NotificationHelper.showTestBurstPulse(
                                        appContext, cityName, compactGt5, baseId, pulseIndex
                                ),
                                appContext
                        );
                    }
                } else {
                    NotificationHelper.showTestNotification(
                            appContext,
                            prefs.getCityName(),
                            prefs.isCompactGt5Format()
                    );
                }
                return;
            }

            if (ACTION_PRAYER_REMINDER.equals(intent.getAction())) {
                String prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY);
                final PrayerType prayerType = PrayerType.fromPrefKey(prayerKey);
                boolean isPreReminder = intent.getBooleanExtra(EXTRA_IS_PRE_REMINDER, true);
                int minutesBefore = intent.getIntExtra(
                        EXTRA_MINUTES_BEFORE,
                        prefs.getEffectivePreReminderMinutes(prayerType)
                );
                String rawFormattedTime = intent.getStringExtra(EXTRA_FORMATTED_TIME);
                final String formattedTime = rawFormattedTime != null ? rawFormattedTime : "--:--";
                boolean isFollowUp = intent.getBooleanExtra(EXTRA_IS_FOLLOW_UP, false);
                final String cityName = prefs.getCityName();
                final boolean compactGt5 = prefs.isCompactGt5Format();
                boolean useAdzanBurst = !isPreReminder && !isFollowUp && prefs.isAdzanTripleBurst();

                int firstBurstId = -1;
                if (useAdzanBurst) {
                    firstBurstId = NotificationHelper.showAdzanBurstPulse(
                            appContext, prayerType, formattedTime, cityName, compactGt5, 0
                    );
                } else {
                    NotificationHelper.showPrayerReminder(
                            appContext,
                            prayerType,
                            formattedTime,
                            cityName,
                            isPreReminder,
                            minutesBefore,
                            isFollowUp,
                            compactGt5
                    );
                }

                String nowStr = ZonedDateTime.now(prefs.getZoneId()).format(LOG_FORMATTER);
                String kind = isPreReminder
                        ? (minutesBefore + " mnt sebelum")
                        : (useAdzanBurst ? "tepat waktu • 3x getar" : "tepat waktu");
                prefs.setLastAlarmLog("Terkirim: " + prayerType.getDisplayName()
                        + " (" + kind + ") pada " + nowStr);

                if (!isFollowUp) {
                    PhoneVibrationHelper.vibrateForPrayer(appContext, isPreReminder);
                    if (prefs.isDoublePing() && isPreReminder) {
                        AlarmScheduler.scheduleFollowUpPing(
                                appContext, prayerType, formattedTime, true, minutesBefore
                        );
                    }
                    AlarmScheduler.scheduleNext(appContext);
                    PrayerWorkScheduler.ensureDailySyncScheduled(appContext);
                } else {
                    PrayerWidgetProvider.updateAllWidgets(appContext);
                }

                if (useAdzanBurst && firstBurstId > 0) {
                    handedOffToAsync = runRemainingBurstAsync(
                            wakeLock,
                            firstBurstId,
                            pulseIndex -> NotificationHelper.showAdzanBurstPulse(
                                    appContext, prayerType, formattedTime, cityName, compactGt5, pulseIndex
                            ),
                            appContext
                    );
                }
            }
        } finally {
            if (!handedOffToAsync && wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private interface PulsePoster {
        int post(int pulseIndex);
    }

    /**
     * Menjalankan denyut ke-2 dan ke-3 dalam satu thread background ber-WakeLock
     * agar irama 1,8 detik tetap mulus tanpa jeda/jitter AlarmManager, lalu membersihkan
     * notifikasi awal sehingga hanya 1 notifikasi Adzan yang tersisa rapi di bilah status.
     */
    private boolean runRemainingBurstAsync(
            final PowerManager.WakeLock wakeLock,
            final int firstNotificationId,
            final PulsePoster poster,
            final Context appContext
    ) {
        final PendingResult pendingResult = goAsync();
        if (pendingResult == null) {
            return false;
        }
        BURST_EXECUTOR.execute(() -> {
            int[] sentIds = new int[NotificationHelper.ADZAN_BURST_COUNT];
            sentIds[0] = firstNotificationId;
            try {
                for (int i = 1; i < NotificationHelper.ADZAN_BURST_COUNT; i++) {
                    Thread.sleep(NotificationHelper.ADZAN_BURST_INTERVAL_MS);
                    sentIds[i] = poster.post(i);
                }
                Thread.sleep(NotificationHelper.ADZAN_BURST_CLEANUP_DELAY_MS);
                // Hapus denyut 1 & 2 dari bilah status HP agar hanya menyisakan 1 notifikasi bersih
                NotificationHelper.cancelNotifications(appContext, sentIds[0], sentIds[1]);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                }
                pendingResult.finish();
            }
        });
        return true;
    }
}
