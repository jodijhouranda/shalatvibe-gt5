package com.jadwalshalat.gt5;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.Chip;
import com.jadwalshalat.gt5.alarm.AlarmScheduler;
import com.jadwalshalat.gt5.data.PreferencesManager;
import com.jadwalshalat.gt5.databinding.ActivityMainBinding;
import com.jadwalshalat.gt5.databinding.ItemPrayerRowBinding;
import com.jadwalshalat.gt5.location.LocationHelper;
import com.jadwalshalat.gt5.model.CalculationMethod;
import com.jadwalshalat.gt5.model.CityPreset;
import com.jadwalshalat.gt5.model.PrayerSchedule;
import com.jadwalshalat.gt5.model.PrayerType;
import com.jadwalshalat.gt5.notification.NotificationHelper;
import com.jadwalshalat.gt5.prayer.PrayerEventPlanner;
import com.jadwalshalat.gt5.prayer.PrayerTimeCalculator;
import com.jadwalshalat.gt5.qibla.QiblaActivity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Layar utama ShalatVibe GT5 untuk memantau jadwal shalat, mengatur waktu getar
 * sebelum adzan, dan memverifikasi sinkronisasi notifikasi ke Huawei Watch GT 5.
 */
public class MainActivity extends AppCompatActivity {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", new Locale("id", "ID"));

    private ActivityMainBinding binding;
    private PreferencesManager prefs;
    private boolean isBindingUi = false;

    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private final Runnable countdownTicker = new Runnable() {
        @Override
        public void run() {
            refreshDashboard();
            countdownHandler.postDelayed(this, 30_000L);
        }
    };

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                refreshPermissionBanner();
                if (granted) {
                    AlarmScheduler.scheduleNext(this);
                    Toast.makeText(this, "Izin notifikasi aktif.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Izin notifikasi ditolak; jam tidak akan menerima alert.",
                            Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (LocationHelper.hasLocationPermission(this)) {
                    applyGpsLocation();
                } else {
                    Toast.makeText(this, "Izin lokasi ditolak. Anda tetap bisa memilih kota manual.",
                            Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        prefs = new PreferencesManager(this);
        NotificationHelper.ensureChannel(this);

        setupListeners();
        bindSettingsControls();
        refreshDashboard();
        rescheduleAndNotifyStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionBanner();
        AlarmScheduler.scheduleNext(this);
        refreshDashboard();
        countdownHandler.removeCallbacks(countdownTicker);
        countdownHandler.postDelayed(countdownTicker, 30_000L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        countdownHandler.removeCallbacks(countdownTicker);
    }

    private void setupListeners() {
        binding.btnChooseCity.setOnClickListener(v -> showCityPickerDialog());
        binding.btnMethod.setOnClickListener(v -> showMethodPickerDialog());
        binding.btnUseGps.setOnClickListener(v -> onGpsClicked());
        binding.btnQibla.setOnClickListener(v ->
                startActivity(new Intent(this, QiblaActivity.class))
        );

        binding.btnGrantNotification.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });

        binding.btnGrantExactAlarm.setOnClickListener(v -> openExactAlarmSettings());
        binding.btnGrantBattery.setOnClickListener(v -> requestIgnoreBatteryOptimization());
        binding.btnBatterySettings.setOnClickListener(v -> requestIgnoreBatteryOptimization());

        binding.btnTestNow.setOnClickListener(v -> {
            if (!NotificationHelper.hasNotificationPermission(this)) {
                Toast.makeText(this, "Aktifkan izin notifikasi terlebih dahulu.", Toast.LENGTH_SHORT).show();
                return;
            }
            NotificationHelper.showTestNotification(this, prefs.getCityName());
            Toast.makeText(this, "Notifikasi tes dikirim.", Toast.LENGTH_SHORT).show();
        });

        binding.btnTestDelayed.setOnClickListener(v -> {
            if (!NotificationHelper.hasNotificationPermission(this)) {
                Toast.makeText(this, "Aktifkan izin notifikasi terlebih dahulu.", Toast.LENGTH_SHORT).show();
                return;
            }
            AlarmScheduler.scheduleDelayedTestNotification(this, 5);
            Toast.makeText(this,
                    "Notifikasi dikirim dalam 5 detik. Segera kunci layar HP Anda!",
                    Toast.LENGTH_LONG).show();
        });

        binding.switchMasterReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isBindingUi) return;
            prefs.setReminderActive(isChecked);
            rescheduleAndNotifyStatus();
        });

        binding.chipGroupPreMinutes.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isBindingUi || checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            int minutes = mapChipIdToMinutes(id);
            prefs.setPreReminderMinutes(minutes);
            rescheduleAndNotifyStatus();
        });

        binding.switchOnTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isBindingUi) return;
            prefs.setNotifyOnTime(isChecked);
            rescheduleAndNotifyStatus();
        });

        binding.switchDoublePing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isBindingUi) return;
            prefs.setDoublePing(isChecked);
            refreshDashboard();
        });

        binding.switchIhtiyat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isBindingUi) return;
            prefs.setUseIhtiyat(isChecked);
            rescheduleAndNotifyStatus();
        });

        binding.switchAlarmClockMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isBindingUi) return;
            prefs.setUseAlarmClockMode(isChecked);
            rescheduleAndNotifyStatus();
        });
    }

    private void bindSettingsControls() {
        isBindingUi = true;
        try {
            binding.switchMasterReminder.setChecked(prefs.isReminderActive());
            binding.switchOnTime.setChecked(prefs.isNotifyOnTime());
            binding.switchDoublePing.setChecked(prefs.isDoublePing());
            binding.switchIhtiyat.setChecked(prefs.isUseIhtiyat());
            binding.switchAlarmClockMode.setChecked(prefs.isUseAlarmClockMode());
            selectMinuteChip(prefs.getPreReminderMinutes());
        } finally {
            isBindingUi = false;
        }
    }

    private void selectMinuteChip(int minutes) {
        Chip target;
        if (minutes <= 5) {
            target = binding.chip5;
        } else if (minutes <= 10) {
            target = binding.chip10;
        } else if (minutes <= 15) {
            target = binding.chip15;
        } else if (minutes <= 20) {
            target = binding.chip20;
        } else {
            target = binding.chip30;
        }
        target.setChecked(true);
    }

    private int mapChipIdToMinutes(int id) {
        if (id == R.id.chip5) return 5;
        if (id == R.id.chip15) return 15;
        if (id == R.id.chip20) return 20;
        if (id == R.id.chip30) return 30;
        return 10;
    }

    private void rescheduleAndNotifyStatus() {
        AlarmScheduler.scheduleNext(this);
        refreshDashboard();
    }

    private void refreshPermissionBanner() {
        boolean hasNotif = NotificationHelper.hasNotificationPermission(this);
        boolean hasExact = AlarmScheduler.canScheduleExactAlarms(this);
        boolean ignoringBattery = AlarmScheduler.isIgnoringBatteryOptimizations(this);

        if (hasNotif && hasExact && ignoringBattery) {
            binding.cardPermissions.setVisibility(View.GONE);
            return;
        }

        binding.cardPermissions.setVisibility(View.VISIBLE);
        binding.btnGrantNotification.setVisibility(hasNotif ? View.GONE : View.VISIBLE);
        binding.btnGrantExactAlarm.setVisibility(hasExact ? View.GONE : View.VISIBLE);
        binding.btnGrantBattery.setVisibility(ignoringBattery ? View.GONE : View.VISIBLE);

        StringBuilder status = new StringBuilder("Agar GT 5 selalu bergetar tepat waktu:");
        if (!hasNotif) {
            status.append("\n• Izin Notifikasi belum aktif.");
        }
        if (!hasExact) {
            status.append("\n• Izin Alarm Tepat Waktu (Android 12+) belum diaktifkan.");
        }
        if (!ignoringBattery) {
            status.append("\n• Optimasi Baterai masih membatasi aplikasi di background.");
        }
        binding.tvPermissionStatus.setText(status.toString());
    }

    private void refreshDashboard() {
        ZoneId zoneId = prefs.getZoneId();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalDate today = now.toLocalDate();

        CalculationMethod method = prefs.getCalculationMethod();
        boolean useIhtiyat = prefs.isUseIhtiyat();
        double lat = prefs.getLatitude();
        double lon = prefs.getLongitude();

        PrayerSchedule todaySchedule = PrayerTimeCalculator.calculate(
                today, lat, lon, zoneId, method, useIhtiyat
        );
        PrayerSchedule tomorrowSchedule = PrayerTimeCalculator.calculate(
                today.plusDays(1), lat, lon, zoneId, method, useIhtiyat
        );

        binding.tvCityName.setText(String.format(
                Locale.US, "%s (%.2f, %.2f)", prefs.getCityName(), lat, lon
        ));
        binding.tvMethodBadge.setText(method.name().replace('_', ' '));
        binding.tvScheduleHeader.setText("Jadwal Shalat • " + today.format(DATE_FORMATTER));

        PrayerType nextPrayer = PrayerEventPlanner.findNextPrayer(todaySchedule, tomorrowSchedule, now);
        ZonedDateTime nextPrayerTime = todaySchedule.getTime(nextPrayer);
        if (nextPrayerTime == null || !nextPrayerTime.isAfter(now)) {
            nextPrayerTime = tomorrowSchedule.getTime(nextPrayer);
        }

        if (nextPrayerTime != null) {
            Duration remaining = Duration.between(now, nextPrayerTime);
            long totalMinutes = Math.max(0, remaining.toMinutes());
            long hours = totalMinutes / 60;
            long mins = totalMinutes % 60;
            String countdownText = hours > 0
                    ? String.format(Locale.getDefault(), "%d jam %d mnt lagi", hours, mins)
                    : String.format(Locale.getDefault(), "%d menit lagi", mins);
            binding.tvNextPrayerCountdown.setText(
                    "Menuju " + nextPrayer.getDisplayName() + " • " + countdownText
            );
        }

        if (!prefs.isReminderActive()) {
            binding.tvNextAlarmInfo.setText("Pengingat otomatis sedang dimatikan");
        } else {
            PrayerEventPlanner.ScheduledEvent nextEvent = PrayerEventPlanner.findNextEvent(
                    now, lat, lon, zoneId, method, useIhtiyat,
                    prefs.getEnabledPrayers(), prefs.getPreReminderMinutes(), prefs.isNotifyOnTime()
            );
            if (nextEvent != null) {
                String prefix = nextEvent.isPreReminder()
                        ? nextEvent.getMinutesBefore() + " mnt sebelum "
                        : "Tepat waktu ";
                binding.tvNextAlarmInfo.setText(
                        "Getar GT 5 berikutnya: " + nextEvent.getTriggerTime().format(TIME_FORMATTER)
                                + " (" + prefix + nextEvent.getPrayerType().getDisplayName() + ")"
                );
            } else {
                binding.tvNextAlarmInfo.setText("Tidak ada jadwal shalat yang diaktifkan");
            }
        }

        renderPrayerRows(todaySchedule, nextPrayer, now);
        binding.tvLastAlarmLog.setText("Log alarm terakhir: " + prefs.getLastAlarmLog());
    }

    private void renderPrayerRows(PrayerSchedule schedule, PrayerType nextPrayer, ZonedDateTime now) {
        binding.prayerListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int preMinutes = prefs.getPreReminderMinutes();

        for (PrayerType type : PrayerType.values()) {
            ItemPrayerRowBinding row = ItemPrayerRowBinding.inflate(
                    inflater, binding.prayerListContainer, false
            );
            ZonedDateTime time = schedule.getTime(type);
            boolean enabled = prefs.isPrayerEnabled(type);
            boolean isNext = (type == nextPrayer && time != null && time.isAfter(now));

            row.tvPrayerName.setText(type.getDisplayName());
            row.tvPrayerTime.setText(time != null ? time.format(TIME_FORMATTER) : "--:--");
            row.tvNextBadge.setVisibility(isNext ? View.VISIBLE : View.GONE);

            if (isNext) {
                row.rowContainer.setBackgroundColor(getColor(R.color.card_highlight));
            }

            if (enabled && time != null) {
                String preTime = time.minusMinutes(preMinutes).format(TIME_FORMATTER);
                row.tvReminderInfo.setText("Getar " + preMinutes + " mnt sebelumnya (" + preTime + ")");
            } else {
                row.tvReminderInfo.setText("Pengingat nonaktif");
            }

            row.switchPrayerEnabled.setOnCheckedChangeListener(null);
            row.switchPrayerEnabled.setChecked(enabled);
            row.switchPrayerEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.setPrayerEnabled(type, isChecked);
                rescheduleAndNotifyStatus();
            });

            binding.prayerListContainer.addView(row.getRoot());
        }
    }

    private void showCityPickerDialog() {
        List<CityPreset> presets = CityPreset.getIndonesianPresets();
        String[] names = new String[presets.size()];
        for (int i = 0; i < presets.size(); i++) {
            names[i] = presets.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Pilih Kota")
                .setItems(names, (dialog, which) -> {
                    prefs.setCityPreset(presets.get(which));
                    rescheduleAndNotifyStatus();
                    Toast.makeText(this, "Kota diubah ke " + presets.get(which).getName(),
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showMethodPickerDialog() {
        CalculationMethod[] methods = CalculationMethod.values();
        String[] labels = new String[methods.length];
        int currentIndex = 0;
        CalculationMethod current = prefs.getCalculationMethod();
        for (int i = 0; i < methods.length; i++) {
            labels[i] = methods[i].getLabel();
            if (methods[i] == current) {
                currentIndex = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Metode Perhitungan")
                .setSingleChoiceItems(labels, currentIndex, (dialog, which) -> {
                    prefs.setCalculationMethod(methods[which]);
                    rescheduleAndNotifyStatus();
                    dialog.dismiss();
                })
                .show();
    }

    private void onGpsClicked() {
        if (LocationHelper.hasLocationPermission(this)) {
            applyGpsLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void applyGpsLocation() {
        LocationHelper.DetectedLocation detected = LocationHelper.getBestLastKnownLocation(this);
        if (detected == null) {
            Toast.makeText(this,
                    "Lokasi GPS terakhir belum tersedia. Pastikan GPS aktif atau pilih kota manual.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        prefs.setLocation(
                detected.getCityName(),
                detected.getLatitude(),
                detected.getLongitude(),
                detected.getZoneId()
        );
        rescheduleAndNotifyStatus();
        Toast.makeText(this, "Lokasi diperbarui: " + detected.getCityName(),
                Toast.LENGTH_SHORT).show();
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                openAppDetailsSettings();
            }
        }
    }

    private void requestIgnoreBatteryOptimization() {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            openBatterySettings();
        }
    }

    private void openBatterySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            openAppDetailsSettings();
        }
    }

    private void openAppDetailsSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
}
