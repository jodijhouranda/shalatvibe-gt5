package com.jadwalshalat.gt5.model;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Menyimpan hasil perhitungan jadwal shalat untuk satu tanggal tertentu.
 */
public final class PrayerSchedule {
    private final LocalDate date;
    private final EnumMap<PrayerType, ZonedDateTime> times;

    public PrayerSchedule(LocalDate date, Map<PrayerType, ZonedDateTime> times) {
        this.date = date;
        this.times = new EnumMap<>(PrayerType.class);
        this.times.putAll(times);
    }

    public LocalDate getDate() {
        return date;
    }

    public ZonedDateTime getTime(PrayerType type) {
        return times.get(type);
    }

    public Map<PrayerType, ZonedDateTime> getAllTimes() {
        return new EnumMap<>(times);
    }
}
