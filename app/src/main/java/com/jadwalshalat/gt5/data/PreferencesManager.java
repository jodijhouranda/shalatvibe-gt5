package com.jadwalshalat.gt5.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.jadwalshalat.gt5.model.CalculationMethod;
import com.jadwalshalat.gt5.model.CityPreset;
import com.jadwalshalat.gt5.model.PrayerType;

import java.time.ZoneId;
import java.util.EnumSet;
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
    private static final String KEY_DOUBLE_PING = "double_ping";
    private static final String KEY_REMINDER_ACTIVE = "reminder_active";
    private static final String KEY_PRAYER_PREFIX = "prayer_enabled_";

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

    public boolean isNotifyOnTime() {
        return prefs.getBoolean(KEY_NOTIFY_ON_TIME, true);
    }

    public void setNotifyOnTime(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFY_ON_TIME, enabled).apply();
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
