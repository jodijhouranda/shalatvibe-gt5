package com.jadwalshalat.gt5.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.jadwalshalat.gt5.model.CalculationMethod;
import com.jadwalshalat.gt5.model.CityPreset;
import com.jadwalshalat.gt5.model.PrayerType;

import java.time.ZoneId;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Penyimpanan pengaturan pengguna menggunakan SharedPreferences.
 */
public final class PreferencesManager {

    private static final String PREF_NAME = "shalat_gt5_prefs";

    private static final String KEY_CITY_NAME = "city_name";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_ZONE_ID = "zone_id";
    private static final String KEY_METHOD = "calculation_method";
    private static final String KEY_IHTIYAT = "use_ihtiyat";
    private static final String KEY_PRE_MINUTES = "pre_reminder_minutes";
    private static final String KEY_NOTIFY_ON_TIME = "notify_on_time";
    private static final String KEY_ADZAN_TRIPLE_BURST = "adzan_triple_burst";
    private static final String KEY_DOUBLE_PING = "double_ping";
    private static final String KEY_REMINDER_ACTIVE = "reminder_active";
    private static final String KEY_ALARM_CLOCK_MODE = "alarm_clock_mode";
    private static final String KEY_LAST_ALARM_LOG = "last_alarm_log";
    private static final String KEY_PRAYER_PREFIX = "prayer_enabled_";
    private static final String KEY_PRAYER_PRE_PREFIX = "prayer_pre_minutes_";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_COMPACT_GT5_FORMAT = "compact_gt5_format";
    private static final String KEY_HUAWEI_STEP_PREFIX = "huawei_step_done_";
    private static final String KEY_PHONE_VIBRATE_ENABLED = "phone_vibrate_enabled";
    private static final String KEY_PHONE_VIBRATE_ON_SILENT = "phone_vibrate_on_silent";
    private static final String KEY_PHONE_VIBRATE_PRE_REMINDER = "phone_vibrate_pre_reminder";
    private static final String KEY_PHONE_VIBRATE_PATTERN = "phone_vibrate_pattern";
    private static final String KEY_DAILY_SUMMARY_ENABLED = "daily_summary_enabled";
    private static final String KEY_LAST_WORK_SYNC_LOG = "last_work_sync_log";
    private static final String KEY_LAST_DAILY_SUMMARY_DATE = "last_daily_summary_date";

    public static final int PRE_MINUTES_FOLLOW_GLOBAL = -1;

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getCityName() {
        return prefs.getString(KEY_CITY_NAME, "Jakarta Pusat");
    }

    public double getLatitude() {
        return Double.longBitsToDouble(
                prefs.getLong(KEY_LATITUDE, Double.doubleToRawLongBits(-6.1754))
        );
    }

    public double getLongitude() {
        return Double.longBitsToDouble(
                prefs.getLong(KEY_LONGITUDE, Double.doubleToRawLongBits(106.8272))
        );
    }

    public ZoneId getZoneId() {
        String id = prefs.getString(KEY_ZONE_ID, ZoneId.systemDefault().getId());
        try {
            return ZoneId.of(id);
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }

    public void setLocation(String cityName, double latitude, double longitude, String zoneId) {
        prefs.edit()
                .putString(KEY_CITY_NAME, cityName)
                .putLong(KEY_LATITUDE, Double.doubleToRawLongBits(latitude))
                .putLong(KEY_LONGITUDE, Double.doubleToRawLongBits(longitude))
                .putString(KEY_ZONE_ID, zoneId)
                .apply();
    }

    public void setCityPreset(CityPreset preset) {
        setLocation(preset.getName(), preset.getLatitude(), preset.getLongitude(), preset.getZoneId());
    }

    public CalculationMethod getCalculationMethod() {
        String name = prefs.getString(KEY_METHOD, CalculationMethod.KEMENAG_RI.name());
        try {
            return CalculationMethod.valueOf(name);
        } catch (IllegalArgumentException e) {
            return CalculationMethod.KEMENAG_RI;
        }
    }

    public void setCalculationMethod(CalculationMethod method) {
        prefs.edit().putString(KEY_METHOD, method.name()).apply();
    }

    public boolean isUseIhtiyat() {
        return prefs.getBoolean(KEY_IHTIYAT, true);
    }

    public void setUseIhtiyat(boolean enabled) {
        prefs.edit().putBoolean(KEY_IHTIYAT, enabled).apply();
    }

    public int getPreReminderMinutes() {
        return prefs.getInt(KEY_PRE_MINUTES, 10);
    }

    public void setPreReminderMinutes(int minutes) {
        prefs.edit().putInt(KEY_PRE_MINUTES, minutes).apply();
    }

    /**
     * Mengembalikan override jeda getar per waktu shalat, atau {@link #PRE_MINUTES_FOLLOW_GLOBAL} (-1)
     * jika mengikuti pengaturan global. Nilai 0 berarti menonaktifkan pre-reminder khusus shalat ini.
     */
    public int getPrayerPreReminderOverride(PrayerType type) {
        return prefs.getInt(KEY_PRAYER_PRE_PREFIX + type.getPrefKey(), PRE_MINUTES_FOLLOW_GLOBAL);
    }

    public void setPrayerPreReminderOverride(PrayerType type, int minutesOrFollowGlobal) {
        if (minutesOrFollowGlobal < 0) {
            prefs.edit().remove(KEY_PRAYER_PRE_PREFIX + type.getPrefKey()).apply();
        } else {
            prefs.edit().putInt(KEY_PRAYER_PRE_PREFIX + type.getPrefKey(), minutesOrFollowGlobal).apply();
        }
    }

    public int getEffectivePreReminderMinutes(PrayerType type) {
        int override = getPrayerPreReminderOverride(type);
        return override >= 0 ? override : getPreReminderMinutes();
    }

    public Map<PrayerType, Integer> getEffectivePreReminderMap() {
        EnumMap<PrayerType, Integer> map = new EnumMap<>(PrayerType.class);
        for (PrayerType type : PrayerType.values()) {
            map.put(type, getEffectivePreReminderMinutes(type));
        }
        return map;
    }

    public boolean isNotifyOnTime() {
        return prefs.getBoolean(KEY_NOTIFY_ON_TIME, true);
    }

    public void setNotifyOnTime(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFY_ON_TIME, enabled).apply();
    }

    public boolean isAdzanTripleBurst() {
        return prefs.getBoolean(KEY_ADZAN_TRIPLE_BURST, true);
    }

    public void setAdzanTripleBurst(boolean enabled) {
        prefs.edit().putBoolean(KEY_ADZAN_TRIPLE_BURST, enabled).apply();
    }

    public boolean isDoublePing() {
        return prefs.getBoolean(KEY_DOUBLE_PING, false);
    }

    public void setDoublePing(boolean enabled) {
        prefs.edit().putBoolean(KEY_DOUBLE_PING, enabled).apply();
    }

    public boolean isReminderActive() {
        return prefs.getBoolean(KEY_REMINDER_ACTIVE, true);
    }

    public void setReminderActive(boolean active) {
        prefs.edit().putBoolean(KEY_REMINDER_ACTIVE, active).apply();
    }

    public boolean isUseAlarmClockMode() {
        return prefs.getBoolean(KEY_ALARM_CLOCK_MODE, true);
    }

    public void setUseAlarmClockMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALARM_CLOCK_MODE, enabled).apply();
    }

    public boolean isCompactGt5Format() {
        return prefs.getBoolean(KEY_COMPACT_GT5_FORMAT, true);
    }

    public void setCompactGt5Format(boolean compact) {
        prefs.edit().putBoolean(KEY_COMPACT_GT5_FORMAT, compact).apply();
    }

    public String getThemeModeName() {
        return prefs.getString(KEY_THEME_MODE, "SYSTEM");
    }

    public void setThemeModeName(String modeName) {
        prefs.edit().putString(KEY_THEME_MODE, modeName).apply();
    }

    public boolean isHuaweiStepChecked(int stepIndex) {
        return prefs.getBoolean(KEY_HUAWEI_STEP_PREFIX + stepIndex, false);
    }

    public void setHuaweiStepChecked(int stepIndex, boolean checked) {
        prefs.edit().putBoolean(KEY_HUAWEI_STEP_PREFIX + stepIndex, checked).apply();
    }

    public String getLastAlarmLog() {
        return prefs.getString(KEY_LAST_ALARM_LOG, "Belum ada alarm otomatis yang tereksekusi.");
    }

    public void setLastAlarmLog(String log) {
        prefs.edit().putString(KEY_LAST_ALARM_LOG, log).apply();
    }

    public boolean isPhoneVibrateEnabled() {
        return prefs.getBoolean(KEY_PHONE_VIBRATE_ENABLED, true);
    }

    public void setPhoneVibrateEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PHONE_VIBRATE_ENABLED, enabled).apply();
    }

    public boolean isPhoneVibrateOnSilent() {
        return prefs.getBoolean(KEY_PHONE_VIBRATE_ON_SILENT, true);
    }

    public void setPhoneVibrateOnSilent(boolean enabled) {
        prefs.edit().putBoolean(KEY_PHONE_VIBRATE_ON_SILENT, enabled).apply();
    }

    public boolean isPhoneVibrateOnPreReminder() {
        return prefs.getBoolean(KEY_PHONE_VIBRATE_PRE_REMINDER, true);
    }

    public void setPhoneVibrateOnPreReminder(boolean enabled) {
        prefs.edit().putBoolean(KEY_PHONE_VIBRATE_PRE_REMINDER, enabled).apply();
    }

    public String getPhoneVibratePatternKey() {
        return prefs.getString(KEY_PHONE_VIBRATE_PATTERN, "MEDIUM");
    }

    public void setPhoneVibratePatternKey(String patternKey) {
        prefs.edit().putString(KEY_PHONE_VIBRATE_PATTERN, patternKey).apply();
    }

    public boolean isDailySummaryEnabled() {
        return prefs.getBoolean(KEY_DAILY_SUMMARY_ENABLED, true);
    }

    public void setDailySummaryEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DAILY_SUMMARY_ENABLED, enabled).apply();
    }

    public String getLastWorkSyncLog() {
        return prefs.getString(KEY_LAST_WORK_SYNC_LOG, "Menunggu sinkronisasi harian WorkManager.");
    }

    public void setLastWorkSyncLog(String log) {
        prefs.edit().putString(KEY_LAST_WORK_SYNC_LOG, log).apply();
    }

    public String getLastDailySummaryDate() {
        return prefs.getString(KEY_LAST_DAILY_SUMMARY_DATE, "");
    }

    public void setLastDailySummaryDate(String isoDate) {
        prefs.edit().putString(KEY_LAST_DAILY_SUMMARY_DATE, isoDate).apply();
    }

    public boolean isPrayerEnabled(PrayerType type) {
        return prefs.getBoolean(KEY_PRAYER_PREFIX + type.getPrefKey(), type.isDefaultEnabled());
    }

    public void setPrayerEnabled(PrayerType type, boolean enabled) {
        prefs.edit().putBoolean(KEY_PRAYER_PREFIX + type.getPrefKey(), enabled).apply();
    }

    public Set<PrayerType> getEnabledPrayers() {
        EnumSet<PrayerType> set = EnumSet.noneOf(PrayerType.class);
        for (PrayerType type : PrayerType.values()) {
            if (isPrayerEnabled(type)) {
                set.add(type);
            }
        }
        return set;
    }
}
