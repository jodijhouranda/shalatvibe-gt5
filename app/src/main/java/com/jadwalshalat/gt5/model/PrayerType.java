package com.jadwalshalat.gt5.model;

/**
 * Jenis waktu shalat harian beserta metadata untuk notifikasi dan preferensi.
 */
public enum PrayerType {
    SUBUH("Subuh", "subuh", 101, 201, true),
    TERBIT("Terbit", "terbit", 102, 202, false),
    DZUHUR("Dzuhur", "dzuhur", 103, 203, true),
    ASHAR("Ashar", "ashar", 104, 204, true),
    MAGHRIB("Maghrib", "maghrib", 105, 205, true),
    ISYA("Isya", "isya", 106, 206, true);

    private final String displayName;
    private final String prefKey;
    private final int preAlarmRequestCode;
    private final int onTimeAlarmRequestCode;
    private final boolean defaultEnabled;

    PrayerType(String displayName, String prefKey, int preAlarmRequestCode,
               int onTimeAlarmRequestCode, boolean defaultEnabled) {
        this.displayName = displayName;
        this.prefKey = prefKey;
        this.preAlarmRequestCode = preAlarmRequestCode;
        this.onTimeAlarmRequestCode = onTimeAlarmRequestCode;
        this.defaultEnabled = defaultEnabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrefKey() {
        return prefKey;
    }

    public int getPreAlarmRequestCode() {
        return preAlarmRequestCode;
    }

    public int getOnTimeAlarmRequestCode() {
        return onTimeAlarmRequestCode;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public static PrayerType fromPrefKey(String key) {
        for (PrayerType type : values()) {
            if (type.prefKey.equals(key)) {
                return type;
            }
        }
        return SUBUH;
    }
}
