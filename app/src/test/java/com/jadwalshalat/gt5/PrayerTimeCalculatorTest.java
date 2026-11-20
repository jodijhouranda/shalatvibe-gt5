package com.jadwalshalat.gt5;

import com.jadwalshalat.gt5.model.CalculationMethod;
import com.jadwalshalat.gt5.model.CityPreset;
import com.jadwalshalat.gt5.model.PrayerSchedule;
import com.jadwalshalat.gt5.model.PrayerType;
import com.jadwalshalat.gt5.prayer.HijriCalendarHelper;
import com.jadwalshalat.gt5.prayer.PrayerEventPlanner;
import com.jadwalshalat.gt5.prayer.PrayerTimeCalculator;
import com.jadwalshalat.gt5.prayer.QiblaCalculator;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PrayerTimeCalculatorTest {

    @Test
    public void jakartaScheduleIsOrderedAndReasonable() {
        ZoneId wib = ZoneId.of("Asia/Jakarta");
        LocalDate date = LocalDate.of(2026, 11, 13);

        PrayerSchedule schedule = PrayerTimeCalculator.calculate(
                date, -6.1754, 106.8272, wib, CalculationMethod.KEMENAG_RI, true
        );

        ZonedDateTime subuh = schedule.getTime(PrayerType.SUBUH);
        ZonedDateTime terbit = schedule.getTime(PrayerType.TERBIT);
        ZonedDateTime dzuhur = schedule.getTime(PrayerType.DZUHUR);
        ZonedDateTime ashar = schedule.getTime(PrayerType.ASHAR);
        ZonedDateTime maghrib = schedule.getTime(PrayerType.MAGHRIB);
        ZonedDateTime isya = schedule.getTime(PrayerType.ISYA);

        assertTrue(subuh.isBefore(terbit));
        assertTrue(terbit.isBefore(dzuhur));
        assertTrue(dzuhur.isBefore(ashar));
        assertTrue(ashar.isBefore(maghrib));
        assertTrue(maghrib.isBefore(isya));

        // Jakarta pertengahan November: Subuh sekitar 04:05-04:15, Dzuhur sekitar 11:38-11:48, Maghrib sekitar 17:45-18:00
        assertTrue(subuh.toLocalTime().isAfter(LocalTime.of(3, 55))
                && subuh.toLocalTime().isBefore(LocalTime.of(4, 25)));
        assertTrue(dzuhur.toLocalTime().isAfter(LocalTime.of(11, 30))
                && dzuhur.toLocalTime().isBefore(LocalTime.of(11, 55)));
        assertTrue(maghrib.toLocalTime().isAfter(LocalTime.of(17, 40))
                && maghrib.toLocalTime().isBefore(LocalTime.of(18, 5)));
    }

    @Test
    public void plannerSchedulesNextDaySubuhAfterIsya() {
        ZoneId wib = ZoneId.of("Asia/Jakarta");
        ZonedDateTime lateNight = ZonedDateTime.of(2026, 11, 13, 22, 30, 0, 0, wib);

        PrayerEventPlanner.ScheduledEvent next = PrayerEventPlanner.findNextEvent(
                lateNight,
                -6.1754,
                106.8272,
                wib,
                CalculationMethod.KEMENAG_RI,
                true,
                EnumSet.of(PrayerType.SUBUH, PrayerType.DZUHUR, PrayerType.ASHAR, PrayerType.MAGHRIB, PrayerType.ISYA),
                10,
                true
        );

        assertNotNull(next);
        assertEquals(PrayerType.SUBUH, next.getPrayerType());
        assertEquals(LocalDate.of(2026, 11, 14), next.getTriggerTime().toLocalDate());
        assertTrue(next.isPreReminder());
    }

    @Test
    public void plannerRespectsPerPrayerPreReminderOverride() {
        ZoneId wib = ZoneId.of("Asia/Jakarta");
        ZonedDateTime lateNight = ZonedDateTime.of(2026, 11, 13, 22, 30, 0, 0, wib);

        EnumMap<PrayerType, Integer> overrides = new EnumMap<>(PrayerType.class);
        for (PrayerType t : PrayerType.values()) {
            overrides.put(t, 10);
        }
        overrides.put(PrayerType.SUBUH, 25);

        PrayerEventPlanner.ScheduledEvent next = PrayerEventPlanner.findNextEvent(
                lateNight,
                -6.1754,
                106.8272,
                wib,
                CalculationMethod.KEMENAG_RI,
                true,
                EnumSet.of(PrayerType.SUBUH, PrayerType.DZUHUR, PrayerType.ASHAR, PrayerType.MAGHRIB, PrayerType.ISYA),
                overrides,
                true
        );

        assertNotNull(next);
        assertEquals(PrayerType.SUBUH, next.getPrayerType());
        assertTrue(next.isPreReminder());
        assertEquals(25, next.getMinutesBefore());
        assertEquals(next.getPrayerTime().minusMinutes(25), next.getTriggerTime());
    }

    @Test
    public void solarWindowComputesProgressInRange() {
        ZoneId wib = ZoneId.of("Asia/Jakarta");
        LocalDate today = LocalDate.of(2026, 11, 13);
        PrayerSchedule yesterday = PrayerTimeCalculator.calculate(
                today.minusDays(1), -6.1754, 106.8272, wib, CalculationMethod.KEMENAG_RI, true
        );
        PrayerSchedule todaySched = PrayerTimeCalculator.calculate(
                today, -6.1754, 106.8272, wib, CalculationMethod.KEMENAG_RI, true
        );
        PrayerSchedule tomorrow = PrayerTimeCalculator.calculate(
                today.plusDays(1), -6.1754, 106.8272, wib, CalculationMethod.KEMENAG_RI, true
        );

        ZonedDateTime midAfternoon = ZonedDateTime.of(today, LocalTime.of(13, 30), wib);
        PrayerEventPlanner.SolarWindow window = PrayerEventPlanner.computeSolarWindow(
                yesterday, todaySched, tomorrow, midAfternoon
        );

        assertEquals(PrayerType.DZUHUR, window.getCurrentPhase());
        assertEquals(PrayerType.ASHAR, window.getNextPrayer());
        assertTrue(window.getProgress() > 0.2f && window.getProgress() < 0.8f);
    }

    @Test
    public void hijriDateFormatsOfflineInIndonesian() {
        String formatted = HijriCalendarHelper.INSTANCE.formatIndonesianHijriDate(LocalDate.of(2026, 11, 19));
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
        assertTrue(formatted.endsWith(" H"));
    }

    @Test
    public void cityPresetsContainExpandedIndonesianCities() {
        List<CityPreset> presets = CityPreset.getIndonesianPresets();
        assertTrue(presets.size() >= 50);
        for (CityPreset city : presets) {
            assertNotNull(city.getName());
            assertFalse(city.getName().isEmpty());
            assertTrue(
                    "WIB".equals(city.getTimezoneLabel())
                            || "WITA".equals(city.getTimezoneLabel())
                            || "WIT".equals(city.getTimezoneLabel())
            );
        }
    }

    @Test
    public void qiblaBearingJakartaIsWestNorthwest() {
        double bearing = QiblaCalculator.calculateBearingDegrees(-6.1754, 106.8272);
        double distance = QiblaCalculator.calculateDistanceKm(-6.1754, 106.8272);

        assertTrue(bearing > 294.0 && bearing < 296.0);
        assertTrue(distance > 7800.0 && distance < 8100.0);
        assertEquals("Barat Laut", QiblaCalculator.formatCardinalDirection(bearing));
    }
}
