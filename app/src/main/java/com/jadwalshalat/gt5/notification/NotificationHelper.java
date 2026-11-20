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
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setContentIntent(contentIntent);

        NotificationManagerCompat.from(context).notify(id, builder.build());
    }
}
