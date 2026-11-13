package com.jadwalshalat.gt5;

import com.jadwalshalat.gt5.model.CalculationMethod;
import com.jadwalshalat.gt5.model.PrayerSchedule;
import com.jadwalshalat.gt5.model.PrayerType;
import com.jadwalshalat.gt5.prayer.PrayerEventPlanner;
import com.jadwalshalat.gt5.prayer.PrayerTimeCalculator;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
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
}
