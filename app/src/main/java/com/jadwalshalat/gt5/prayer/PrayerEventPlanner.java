package com.jadwalshalat.gt5.prayer;

import com.jadwalshalat.gt5.model.CalculationMethod;
import com.jadwalshalat.gt5.model.PrayerSchedule;
import com.jadwalshalat.gt5.model.PrayerType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Menentukan event notifikasi berikutnya (sebelum adzan dan/atau saat adzan) secara deterministik.
 */
public final class PrayerEventPlanner {

    public static final class ScheduledEvent {
        private final PrayerType prayerType;
        private final ZonedDateTime triggerTime;
        private final ZonedDateTime prayerTime;
        private final boolean preReminder;
        private final int minutesBefore;

        public ScheduledEvent(PrayerType prayerType, ZonedDateTime triggerTime,
                              ZonedDateTime prayerTime, boolean preReminder, int minutesBefore) {
            this.prayerType = prayerType;
            this.triggerTime = triggerTime;
            this.prayerTime = prayerTime;
            this.preReminder = preReminder;
            this.minutesBefore = minutesBefore;
        }

        public PrayerType getPrayerType() {
            return prayerType;
        }

        public ZonedDateTime getTriggerTime() {
            return triggerTime;
        }

        public ZonedDateTime getPrayerTime() {
            return prayerTime;
        }

        public boolean isPreReminder() {
            return preReminder;
        }

        public int getMinutesBefore() {
            return minutesBefore;
        }

        public int getRequestCode() {
            return preReminder
                    ? prayerType.getPreAlarmRequestCode()
                    : prayerType.getOnTimeAlarmRequestCode();
        }
    }

    private PrayerEventPlanner() {
    }

    public static List<ScheduledEvent> buildEventsForRange(
            ZonedDateTime now,
            double latitude,
            double longitude,
            ZoneId zoneId,
            CalculationMethod method,
            boolean useIhtiyat,
            Set<PrayerType> enabledPrayers,
            int preReminderMinutes,
            boolean notifyOnTime
    ) {
        if (enabledPrayers == null || enabledPrayers.isEmpty()) {
            return Collections.emptyList();
        }
        if (preReminderMinutes <= 0 && !notifyOnTime) {
            return Collections.emptyList();
        }

        List<ScheduledEvent> events = new ArrayList<>();
        LocalDate baseDate = now.withZoneSameInstant(zoneId).toLocalDate();

        for (int dayOffset = 0; dayOffset <= 1; dayOffset++) {
            PrayerSchedule schedule = PrayerTimeCalculator.calculate(
                    baseDate.plusDays(dayOffset),
                    latitude,
                    longitude,
                    zoneId,
                    method,
                    useIhtiyat
            );

            for (PrayerType type : PrayerType.values()) {
                if (!enabledPrayers.contains(type)) {
                    continue;
                }
                ZonedDateTime prayerTime = schedule.getTime(type);
                if (prayerTime == null) {
                    continue;
                }

                if (preReminderMinutes > 0) {
                    ZonedDateTime preTime = prayerTime.minusMinutes(preReminderMinutes);
                    if (preTime.isAfter(now)) {
                        events.add(new ScheduledEvent(type, preTime, prayerTime, true, preReminderMinutes));
                    }
                }

                if (notifyOnTime && prayerTime.isAfter(now)) {
                    events.add(new ScheduledEvent(type, prayerTime, prayerTime, false, 0));
                }
            }
        }

        events.sort(Comparator.comparing(ScheduledEvent::getTriggerTime));
        return events;
    }

    public static ScheduledEvent findNextEvent(
            ZonedDateTime now,
            double latitude,
            double longitude,
            ZoneId zoneId,
            CalculationMethod method,
            boolean useIhtiyat,
            Set<PrayerType> enabledPrayers,
            int preReminderMinutes,
            boolean notifyOnTime
    ) {
        List<ScheduledEvent> events = buildEventsForRange(
                now, latitude, longitude, zoneId, method, useIhtiyat,
                enabledPrayers, preReminderMinutes, notifyOnTime
        );
        return events.isEmpty() ? null : events.get(0);
    }

    public static PrayerType findNextPrayer(PrayerSchedule todaySchedule,
                                            PrayerSchedule tomorrowSchedule,
                                            ZonedDateTime now) {
        for (PrayerType type : PrayerType.values()) {
            if (type == PrayerType.TERBIT) {
                continue;
            }
            ZonedDateTime time = todaySchedule.getTime(type);
            if (time != null && time.isAfter(now)) {
                return type;
            }
        }
        return PrayerType.SUBUH;
    }
}
