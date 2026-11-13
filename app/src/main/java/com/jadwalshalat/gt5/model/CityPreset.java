package com.jadwalshalat.gt5.model;

import java.util.Arrays;
import java.util.List;

/**
 * Daftar kota preset di Indonesia agar aplikasi dapat langsung digunakan offline tanpa GPS.
 */
public final class CityPreset {
    private final String name;
    private final double latitude;
    private final double longitude;
    private final String zoneId;

    public CityPreset(String name, double latitude, double longitude, String zoneId) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.zoneId = zoneId;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getZoneId() {
        return zoneId;
    }

    public static List<CityPreset> getIndonesianPresets() {
        return Arrays.asList(
                new CityPreset("Jakarta Pusat", -6.1754, 106.8272, "Asia/Jakarta"),
                new CityPreset("Bandung", -6.9175, 107.6191, "Asia/Jakarta"),
                new CityPreset("Surabaya", -7.2575, 112.7521, "Asia/Jakarta"),
                new CityPreset("Yogyakarta", -7.7956, 110.3695, "Asia/Jakarta"),
                new CityPreset("Semarang", -6.9667, 110.4167, "Asia/Jakarta"),
                new CityPreset("Medan", 3.5952, 98.6722, "Asia/Jakarta"),
                new CityPreset("Palembang", -2.9761, 104.7754, "Asia/Jakarta"),
                new CityPreset("Banda Aceh", 5.5483, 95.3238, "Asia/Jakarta"),
                new CityPreset("Padang", -0.9471, 100.4172, "Asia/Jakarta"),
                new CityPreset("Pontianak", -0.0263, 109.3425, "Asia/Jakarta"),
                new CityPreset("Makassar", -5.1477, 119.4327, "Asia/Makassar"),
                new CityPreset("Denpasar", -8.6705, 115.2126, "Asia/Makassar"),
                new CityPreset("Balikpapan", -1.2379, 116.8529, "Asia/Makassar"),
                new CityPreset("Banjarmasin", -3.3194, 114.5908, "Asia/Makassar"),
                new CityPreset("Manado", 1.4748, 124.8421, "Asia/Makassar"),
                new CityPreset("Ambon", -3.6954, 128.1814, "Asia/Jayapura"),
                new CityPreset("Jayapura", -2.5916, 140.6690, "Asia/Jayapura")
        );
    }
}
