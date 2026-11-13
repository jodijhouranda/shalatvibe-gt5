package com.jadwalshalat.gt5.prayer;

/**
 * Kalkulator arah kiblat (azimuth dari utara sejati) dan jarak ke Ka'bah di Makkah.
 */
public final class QiblaCalculator {

    public static final double KAABA_LATITUDE = 21.422487;
    public static final double KAABA_LONGITUDE = 39.826206;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private QiblaCalculator() {
    }

    /**
     * Menghitung sudut arah kiblat dalam derajat searah jarum jam dari Utara Sejati [0, 360).
     */
    public static double calculateBearingDegrees(double latitude, double longitude) {
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(KAABA_LATITUDE);
        double dLon = Math.toRadians(KAABA_LONGITUDE - longitude);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360.0) % 360.0;
    }

    /**
     * Menghitung jarak garis besar (great-circle) ke Ka'bah dalam kilometer.
     */
    public static double calculateDistanceKm(double latitude, double longitude) {
        double dLat = Math.toRadians(KAABA_LATITUDE - latitude);
        double dLon = Math.toRadians(KAABA_LONGITUDE - longitude);
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(KAABA_LATITUDE);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Mengembalikan label arah mata angin dalam bahasa Indonesia.
     */
    public static String formatCardinalDirection(double bearingDegrees) {
        String[] dirs = {
                "Utara", "Timur Laut", "Timur", "Tenggara",
                "Selatan", "Barat Daya", "Barat", "Barat Laut"
        };
        int index = (int) Math.round(((bearingDegrees % 360.0) + 360.0) % 360.0 / 45.0) % 8;
        return dirs[index];
    }

    /**
     * Menghitung selisih sudut terpendek pada rentang [-180, 180].
     */
    public static double shortestAngleDiff(double targetDegrees, double currentDegrees) {
        return ((targetDegrees - currentDegrees + 540.0) % 360.0) - 180.0;
    }
}
