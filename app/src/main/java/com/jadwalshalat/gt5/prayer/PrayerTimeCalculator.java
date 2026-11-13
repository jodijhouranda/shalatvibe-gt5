package com.jadwalshalat.gt5.prayer;

import com.jadwalshalat.gt5.model.CalculationMethod;
import com.jadwalshalat.gt5.model.PrayerSchedule;
import com.jadwalshalat.gt5.model.PrayerType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;

/**
 * Kalkulator jadwal shalat astronomis offline berbasis algoritma posisi matahari (Meeus / PrayTimes).
 */
public final class PrayerTimeCalculator {

    private static final double SUNRISE_SUNSET_ANGLE = 0.833;
    private static final int REFINEMENT_ITERATIONS = 2;

    private PrayerTimeCalculator() {
    }

    public static PrayerSchedule calculate(
            LocalDate date,
            double latitude,
            double longitude,
            ZoneId zoneId,
            CalculationMethod method,
            boolean useIhtiyat
    ) {
        ZonedDateTime noonAnchor = date.atTime(12, 0).atZone(zoneId);
        double timezoneHours = noonAnchor.getOffset().getTotalSeconds() / 3600.0;
        double jd = julianDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());

        // Tebakan awal fraksi hari (jam / 24)
        double fajrDay = 5.0 / 24.0;
        double sunriseDay = 6.0 / 24.0;
        double dhuhrDay = 12.0 / 24.0;
        double asrDay = 15.0 / 24.0;
        double maghribDay = 18.0 / 24.0;
        double ishaDay = 19.0 / 24.0;

        for (int i = 0; i < REFINEMENT_ITERATIONS; i++) {
            fajrDay = sunAngleTime(jd, fajrDay, latitude, method.getFajrAngle(), true) / 24.0;
            sunriseDay = sunAngleTime(jd, sunriseDay, latitude, SUNRISE_SUNSET_ANGLE, true) / 24.0;
            dhuhrDay = midDay(jd, dhuhrDay) / 24.0;
            asrDay = asrTime(jd, asrDay, latitude, 1.0) / 24.0;
            maghribDay = sunAngleTime(jd, maghribDay, latitude, SUNRISE_SUNSET_ANGLE, false) / 24.0;
            if (method.getIshaMinutesAfterMaghrib() == 0) {
                ishaDay = sunAngleTime(jd, ishaDay, latitude, method.getIshaAngle(), false) / 24.0;
            }
        }

        double offsetHours = timezoneHours - (longitude / 15.0);
        int ihtiyat = useIhtiyat ? method.getDefaultIhtiyatMinutes() : 0;

        double fajrHour = fixHour(fajrDay * 24.0 + offsetHours) + (ihtiyat / 60.0);
        double sunriseHour = fixHour(sunriseDay * 24.0 + offsetHours) - (ihtiyat / 60.0);
        double dhuhrHour = fixHour(dhuhrDay * 24.0 + offsetHours) + (ihtiyat / 60.0);
        double asrHour = fixHour(asrDay * 24.0 + offsetHours) + (ihtiyat / 60.0);
        double maghribHour = fixHour(maghribDay * 24.0 + offsetHours) + (ihtiyat / 60.0);

        double ishaHour;
        if (method.getIshaMinutesAfterMaghrib() > 0) {
            ishaHour = fixHour(maghribHour + (method.getIshaMinutesAfterMaghrib() / 60.0));
        } else {
            ishaHour = fixHour(ishaDay * 24.0 + offsetHours) + (ihtiyat / 60.0);
        }

        EnumMap<PrayerType, ZonedDateTime> times = new EnumMap<>(PrayerType.class);
        times.put(PrayerType.SUBUH, toZonedDateTime(date, zoneId, fajrHour));
        times.put(PrayerType.TERBIT, toZonedDateTime(date, zoneId, sunriseHour));
        times.put(PrayerType.DZUHUR, toZonedDateTime(date, zoneId, dhuhrHour));
        times.put(PrayerType.ASHAR, toZonedDateTime(date, zoneId, asrHour));
        times.put(PrayerType.MAGHRIB, toZonedDateTime(date, zoneId, maghribHour));
        times.put(PrayerType.ISYA, toZonedDateTime(date, zoneId, ishaHour));

        return new PrayerSchedule(date, times);
    }

    private static ZonedDateTime toZonedDateTime(LocalDate date, ZoneId zoneId, double hourValue) {
        double normalized = fixHour(hourValue);
        int totalMinutes = (int) Math.round(normalized * 60.0);
        if (totalMinutes >= 24 * 60) {
            totalMinutes -= 24 * 60;
        } else if (totalMinutes < 0) {
            totalMinutes += 24 * 60;
        }
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        return date.atTime(LocalTime.of(hour, minute)).atZone(zoneId);
    }

    private static double julianDate(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        double a = Math.floor(year / 100.0);
        double b = 2.0 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (year + 4716))
                + Math.floor(30.6001 * (month + 1))
                + day + b - 1524.5;
    }

    private static double[] sunPosition(double jd) {
        double d = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * d);
        double q = fixAngle(280.459 + 0.98564736 * d);
        double l = fixAngle(q + 1.915 * dSin(g) + 0.020 * dSin(2.0 * g));
        double e = 23.439 - 0.00000036 * d;

        double ra = dAtan2(dCos(e) * dSin(l), dCos(l)) / 15.0;
        double eqTime = (q / 15.0) - fixHour(ra);
        double decl = dAsin(dSin(e) * dSin(l));
        return new double[]{decl, eqTime};
    }

    private static double midDay(double jd, double dayFraction) {
        double eqTime = sunPosition(jd + dayFraction)[1];
        return fixHour(12.0 - eqTime);
    }

    private static double sunAngleTime(double jd, double dayFraction, double latitude,
                                       double angleBelowHorizon, boolean beforeNoon) {
        double decl = sunPosition(jd + dayFraction)[0];
        double noon = midDay(jd, dayFraction);
        double numerator = -dSin(angleBelowHorizon) - dSin(decl) * dSin(latitude);
        double denominator = dCos(decl) * dCos(latitude);
        double cosH = clamp(numerator / denominator, -1.0, 1.0);
        double t = dAcos(cosH) / 15.0;
        return noon + (beforeNoon ? -t : t);
    }

    private static double asrTime(double jd, double dayFraction, double latitude, double shadowFactor) {
        double decl = sunPosition(jd + dayFraction)[0];
        double altitude = dAcot(shadowFactor + dTan(Math.abs(latitude - decl)));
        // sunAngleTime menerima sudut di bawah cakrawala, jadi ketinggian di atas cakrawala bernilai negatif
        return sunAngleTime(jd, dayFraction, latitude, -altitude, false);
    }

    private static double dSin(double deg) {
        return Math.sin(Math.toRadians(deg));
    }

    private static double dCos(double deg) {
        return Math.cos(Math.toRadians(deg));
    }

    private static double dTan(double deg) {
        return Math.tan(Math.toRadians(deg));
    }

    private static double dAsin(double val) {
        return Math.toDegrees(Math.asin(clamp(val, -1.0, 1.0)));
    }

    private static double dAcos(double val) {
        return Math.toDegrees(Math.acos(clamp(val, -1.0, 1.0)));
    }

    private static double dAtan2(double y, double x) {
        return Math.toDegrees(Math.atan2(y, x));
    }

    private static double dAcot(double x) {
        return Math.toDegrees(Math.atan(1.0 / x));
    }

    private static double fixAngle(double a) {
        return fix(a, 360.0);
    }

    private static double fixHour(double a) {
        return fix(a, 24.0);
    }

    private static double fix(double a, double b) {
        double result = a - b * Math.floor(a / b);
        return (result < 0) ? result + b : result;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
