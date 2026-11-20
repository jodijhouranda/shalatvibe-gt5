package com.jadwalshalat.gt5.model;

import java.util.Arrays;
import java.util.List;

/**
 * Daftar kota preset di Indonesia agar aplikasi dapat langsung digunakan offline tanpa GPS.
 */
public final class CityPreset {
    private final String name;
    private final String region;
    private final double latitude;
    private final double longitude;
    private final String zoneId;

    public CityPreset(String name, double latitude, double longitude, String zoneId) {
        this(name, "", latitude, longitude, zoneId);
    }

    public CityPreset(String name, String region, double latitude, double longitude, String zoneId) {
        this.name = name;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.zoneId = zoneId;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
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

    public String getTimezoneLabel() {
        if ("Asia/Makassar".equals(zoneId)) {
            return "WITA";
        }
        if ("Asia/Jayapura".equals(zoneId)) {
            return "WIT";
        }
        return "WIB";
    }

    public static List<CityPreset> getIndonesianPresets() {
        return Arrays.asList(
                // DKI Jakarta & Jabodetabek (WIB)
                new CityPreset("Jakarta Pusat", "DKI Jakarta", -6.1754, 106.8272, "Asia/Jakarta"),
                new CityPreset("Jakarta Selatan", "DKI Jakarta", -6.2615, 106.8106, "Asia/Jakarta"),
                new CityPreset("Jakarta Timur", "DKI Jakarta", -6.2250, 106.9004, "Asia/Jakarta"),
                new CityPreset("Jakarta Barat", "DKI Jakarta", -6.1674, 106.7637, "Asia/Jakarta"),
                new CityPreset("Jakarta Utara", "DKI Jakarta", -6.1384, 106.8639, "Asia/Jakarta"),
                new CityPreset("Bogor", "Jawa Barat", -6.5971, 106.8060, "Asia/Jakarta"),
                new CityPreset("Depok", "Jawa Barat", -6.4025, 106.7942, "Asia/Jakarta"),
                new CityPreset("Tangerang", "Banten", -6.1783, 106.6319, "Asia/Jakarta"),
                new CityPreset("Tangerang Selatan", "Banten", -6.2886, 106.7179, "Asia/Jakarta"),
                new CityPreset("Bekasi", "Jawa Barat", -6.2383, 106.9756, "Asia/Jakarta"),
                new CityPreset("Serang", "Banten", -6.1200, 106.1503, "Asia/Jakarta"),

                // Jawa (WIB)
                new CityPreset("Bandung", "Jawa Barat", -6.9175, 107.6191, "Asia/Jakarta"),
                new CityPreset("Cirebon", "Jawa Barat", -6.7320, 108.5523, "Asia/Jakarta"),
                new CityPreset("Tasikmalaya", "Jawa Barat", -7.3274, 108.2207, "Asia/Jakarta"),
                new CityPreset("Semarang", "Jawa Tengah", -6.9667, 110.4167, "Asia/Jakarta"),
                new CityPreset("Solo (Surakarta)", "Jawa Tengah", -7.5755, 110.8243, "Asia/Jakarta"),
                new CityPreset("Purwokerto", "Jawa Tengah", -7.4243, 109.2396, "Asia/Jakarta"),
                new CityPreset("Tegal", "Jawa Tengah", -6.8694, 109.1402, "Asia/Jakarta"),
                new CityPreset("Yogyakarta", "DI Yogyakarta", -7.7956, 110.3695, "Asia/Jakarta"),
                new CityPreset("Surabaya", "Jawa Timur", -7.2575, 112.7521, "Asia/Jakarta"),
                new CityPreset("Malang", "Jawa Timur", -7.9666, 112.6326, "Asia/Jakarta"),
                new CityPreset("Kediri", "Jawa Timur", -7.8480, 112.0178, "Asia/Jakarta"),
                new CityPreset("Jember", "Jawa Timur", -8.1845, 113.6681, "Asia/Jakarta"),
                new CityPreset("Banyuwangi", "Jawa Timur", -8.2192, 114.3691, "Asia/Jakarta"),

                // Sumatra (WIB)
                new CityPreset("Banda Aceh", "Aceh", 5.5483, 95.3238, "Asia/Jakarta"),
                new CityPreset("Medan", "Sumatra Utara", 3.5952, 98.6722, "Asia/Jakarta"),
                new CityPreset("Padang", "Sumatra Barat", -0.9471, 100.4172, "Asia/Jakarta"),
                new CityPreset("Pekanbaru", "Riau", 0.5071, 101.4478, "Asia/Jakarta"),
                new CityPreset("Batam", "Kepulauan Riau", 1.0456, 104.0305, "Asia/Jakarta"),
                new CityPreset("Jambi", "Jambi", -1.6101, 103.6131, "Asia/Jakarta"),
                new CityPreset("Palembang", "Sumatra Selatan", -2.9761, 104.7754, "Asia/Jakarta"),
                new CityPreset("Bengkulu", "Bengkulu", -3.8004, 102.2655, "Asia/Jakarta"),
                new CityPreset("Bandar Lampung", "Lampung", -5.4292, 105.2610, "Asia/Jakarta"),
                new CityPreset("Pangkalpinang", "Bangka Belitung", -2.1316, 106.1169, "Asia/Jakarta"),

                // Kalimantan (WIB & WITA)
                new CityPreset("Pontianak", "Kalimantan Barat", -0.0263, 109.3425, "Asia/Jakarta"),
                new CityPreset("Palangka Raya", "Kalimantan Tengah", -2.2136, 113.9108, "Asia/Jakarta"),
                new CityPreset("Banjarmasin", "Kalimantan Selatan", -3.3194, 114.5908, "Asia/Makassar"),
                new CityPreset("Balikpapan", "Kalimantan Timur", -1.2379, 116.8529, "Asia/Makassar"),
                new CityPreset("Samarinda", "Kalimantan Timur", -0.5022, 117.1536, "Asia/Makassar"),
                new CityPreset("Ibu Kota Nusantara", "Kalimantan Timur", -0.9730, 116.7094, "Asia/Makassar"),

                // Bali, Nusa Tenggara & Sulawesi (WITA)
                new CityPreset("Denpasar", "Bali", -8.6705, 115.2126, "Asia/Makassar"),
                new CityPreset("Mataram", "NTB", -8.5833, 116.1167, "Asia/Makassar"),
                new CityPreset("Kupang", "NTT", -10.1772, 123.6070, "Asia/Makassar"),
                new CityPreset("Makassar", "Sulawesi Selatan", -5.1477, 119.4327, "Asia/Makassar"),
                new CityPreset("Palu", "Sulawesi Tengah", -0.8917, 119.8707, "Asia/Makassar"),
                new CityPreset("Kendari", "Sulawesi Tenggara", -3.9985, 122.5129, "Asia/Makassar"),
                new CityPreset("Gorontalo", "Gorontalo", 0.5435, 123.0568, "Asia/Makassar"),
                new CityPreset("Manado", "Sulawesi Utara", 1.4748, 124.8421, "Asia/Makassar"),

                // Maluku & Papua (WIT)
                new CityPreset("Ambon", "Maluku", -3.6954, 128.1814, "Asia/Jayapura"),
                new CityPreset("Ternate", "Maluku Utara", 0.7893, 127.3878, "Asia/Jayapura"),
                new CityPreset("Sorong", "Papua Barat Daya", -0.8762, 131.2558, "Asia/Jayapura"),
                new CityPreset("Jayapura", "Papua", -2.5916, 140.6690, "Asia/Jayapura")
        );
    }
}
