package com.jadwalshalat.gt5.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.jadwalshalat.gt5.MainActivity;
import com.jadwalshalat.gt5.R;
import com.jadwalshalat.gt5.model.PrayerType;

/**
 * Mengelola channel dan pengiriman notifikasi prioritas tinggi agar otomatis
 * diteruskan oleh aplikasi Huawei Health dan menggetarkan Huawei Watch GT 5.
 */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "shalat_gt5_alert_v1";
    public static final String CHANNEL_DAILY_SUMMARY_ID = "shalat_daily_summary_v1";
    public static final int DAILY_SUMMARY_NOTIFICATION_ID = 77001;
    public static final int ADZAN_BURST_COUNT = 3;
    public static final long ADZAN_BURST_INTERVAL_MS = 1_800L;
    public static final long ADZAN_BURST_CLEANUP_DELAY_MS = 1_400L;
    private static final long[] VIBRATION_PATTERN = new long[]{0, 450, 180, 450, 180, 450};

    private NotificationHelper() {
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_channel_desc));
        channel.enableVibration(true);
        channel.setVibrationPattern(VIBRATION_PATTERN);
        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        channel.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                audioAttributes
        );

        manager.createNotificationChannel(channel);
        ensureDailySummaryChannel(context);
    }

    public static void ensureDailySummaryChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        if (manager.getNotificationChannel(CHANNEL_DAILY_SUMMARY_ID) != null) {
            return;
        }

        NotificationChannel summaryChannel = new NotificationChannel(
                CHANNEL_DAILY_SUMMARY_ID,
                context.getString(R.string.daily_summary_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        summaryChannel.setDescription(context.getString(R.string.daily_summary_channel_desc));
        summaryChannel.enableVibration(false);
        summaryChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(summaryChannel);
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void showPrayerReminder(
            Context context,
            PrayerType prayerType,
            String formattedTime,
            String cityName,
            boolean isPreReminder,
            int minutesBefore,
            boolean isFollowUpPing
    ) {
        showPrayerReminder(
                context, prayerType, formattedTime, cityName,
                isPreReminder, minutesBefore, isFollowUpPing, false
        );
    }

    public static void showPrayerReminder(
            Context context,
            PrayerType prayerType,
            String formattedTime,
            String cityName,
            boolean isPreReminder,
            int minutesBefore,
            boolean isFollowUpPing,
            boolean compactGt5Format
    ) {
        if (!hasNotificationPermission(context)) {
            return;
        }
        ensureChannel(context);

        String title;
        String body;
        if (compactGt5Format) {
            if (isPreReminder) {
                if (isFollowUpPing) {
                    title = prayerType.getDisplayName() + " • Segera";
                    body = "Adzan " + formattedTime + " • " + cityName;
                } else {
                    title = minutesBefore + " mnt • " + prayerType.getDisplayName();
                    body = "Pukul " + formattedTime + " • " + cityName;
                }
            } else {
                title = "Adzan " + prayerType.getDisplayName();
                body = formattedTime + " • " + cityName;
            }
        } else {
            if (isPreReminder) {
                if (isFollowUpPing) {
                    title = prayerType.getDisplayName() + " Sebentar Lagi";
                    body = "Bersiap menuju waktu " + prayerType.getDisplayName()
                            + " (" + formattedTime + ") • " + cityName;
                } else {
                    title = minutesBefore + " Menit Lagi " + prayerType.getDisplayName();
                    body = "Jadwal " + prayerType.getDisplayName() + " pukul "
                            + formattedTime + " • " + cityName;
                }
            } else {
                title = "Masuk Waktu " + prayerType.getDisplayName();
                body = "Pukul " + formattedTime + " • " + cityName + " (Waktunya shalat)";
            }
        }

        int notificationId = (prayerType.ordinal() + 1) * 100
                + (isPreReminder ? 10 : 20)
                + (isFollowUpPing ? 5 : 0);

        dispatchNotification(context, notificationId, title, body);
    }

    /**
     * Mengirim satu denyut dari rangkaian 3x getar mulus saat Adzan tiba.
     * Setiap denyut memakai ID unik dan teks yang sedikit bervariasi agar Huawei Health
     * tidak menyaringnya sebagai duplikat dan getaran di GT 5 terasa menyambung.
     */
    public static int showAdzanBurstPulse(
            Context context,
            PrayerType prayerType,
            String formattedTime,
            String cityName,
            boolean compactGt5Format,
            int pulseIndex
    ) {
        if (!hasNotificationPermission(context)) {
            return -1;
        }
        ensureChannel(context);

        int safePulse = Math.max(0, Math.min(pulseIndex, ADZAN_BURST_COUNT - 1));
        String prayerName = prayerType.getDisplayName();
        String title;
        String body;

        if (compactGt5Format) {
            if (safePulse == 0) {
                title = "Adzan " + prayerName;
                body = formattedTime + " • " + cityName;
            } else if (safePulse == 1) {
                title = "Masuk " + prayerName;
                body = "Pukul " + formattedTime + " • " + cityName;
            } else {
                title = "Waktunya " + prayerName;
                body = "Adzan " + formattedTime + " • " + cityName;
            }
        } else {
            if (safePulse == 0) {
                title = "Masuk Waktu " + prayerName;
                body = "Pukul " + formattedTime + " • " + cityName + " (Waktunya shalat)";
            } else if (safePulse == 1) {
                title = "Adzan " + prayerName + " Berkumandang";
                body = "Saatnya menunaikan shalat " + prayerName + " (" + formattedTime + ") • " + cityName;
            } else {
                title = "Waktunya Shalat " + prayerName;
                body = "Pukul " + formattedTime + " • " + cityName;
            }
        }

        int notificationId = (prayerType.ordinal() + 1) * 100 + 20 + safePulse;
        dispatchNotification(context, notificationId, title, body);
        return notificationId;
    }

    public static void showTestNotification(Context context, String cityName) {
        showTestNotification(context, cityName, true);
    }

    public static void showTestNotification(Context context, String cityName, boolean compactGt5Format) {
        if (!hasNotificationPermission(context)) {
            return;
        }
        ensureChannel(context);
        int uniqueId = (int) (System.currentTimeMillis() % 100000);
        String title = compactGt5Format ? "Tes Getar GT 5 ✓" : "Tes Getar Huawei GT 5";
        String body = compactGt5Format
                ? "Sinkronisasi aktif • " + cityName
                : "Jika jam Anda bergetar sekarang, sinkronisasi Huawei Health berhasil (" + cityName + ").";
        dispatchNotification(context, 900000 + uniqueId, title, body);
    }

    public static int showTestBurstPulse(
            Context context,
            String cityName,
            boolean compactGt5Format,
            int baseId,
            int pulseIndex
    ) {
        if (!hasNotificationPermission(context)) {
            return -1;
        }
        ensureChannel(context);

        int safePulse = Math.max(0, Math.min(pulseIndex, ADZAN_BURST_COUNT - 1));
        String title;
        String body;

        if (compactGt5Format) {
            if (safePulse == 0) {
                title = "Tes Adzan GT 5 (1/3)";
                body = "Getar mulus dimulai • " + cityName;
            } else if (safePulse == 1) {
                title = "Tes Adzan GT 5 (2/3)";
                body = "Getar menyambung • " + cityName;
            } else {
                title = "Tes Adzan GT 5 ✓";
                body = "3x getar mulus selesai • " + cityName;
            }
        } else {
            if (safePulse == 0) {
                title = "Tes Getar Adzan GT 5 (1/3)";
                body = "Gelombang 1 dari 3 • " + cityName;
            } else if (safePulse == 1) {
                title = "Tes Getar Adzan GT 5 (2/3)";
                body = "Gelombang 2 dari 3 • " + cityName;
            } else {
                title = "Tes Getar Adzan GT 5 (3/3) ✓";
                body = "3x getar beruntun selesai dikirim ke Huawei Watch GT 5 (" + cityName + ").";
            }
        }

        int notificationId = baseId + safePulse;
        dispatchNotification(context, notificationId, title, body);
        return notificationId;
    }

    public static void showDailyScheduleSummary(
            Context context,
            String cityName,
            String dateLabel,
            String compactLine,
            String expandedBody
    ) {
        if (!hasNotificationPermission(context)) {
            return;
        }
        ensureDailySummaryChannel(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                DAILY_SUMMARY_NOTIFICATION_ID,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Jadwal Shalat • " + cityName + " (" + dateLabel + ")";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_DAILY_SUMMARY_ID)
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentTitle(title)
                .setContentText(compactLine)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(expandedBody))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent);

        NotificationManagerCompat.from(context).notify(DAILY_SUMMARY_NOTIFICATION_ID, builder.build());
    }

    public static void cancelNotifications(Context context, int... ids) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        for (int id : ids) {
            if (id > 0) {
                manager.cancel(id);
            }
        }
    }

    private static void dispatchNotification(Context context, int id, String title, String body) {
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                id,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(VIBRATION_PATTERN)
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_LIGHTS)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setContentIntent(contentIntent);

        NotificationManagerCompat.from(context).notify(id, builder.build());
    }
}
