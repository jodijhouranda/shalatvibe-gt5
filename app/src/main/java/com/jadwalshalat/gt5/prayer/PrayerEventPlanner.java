package com.jadwalshalat.gt5.prayer;

import com.jadwalshalat.gt5.model.CalculationMethod;
import com.jadwalshalat.gt5.model.PrayerSchedule;
import com.jadwalshalat.gt5.model.PrayerType;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Menentukan event notifikasi berikutnya (sebelum adzan dan/atau saat adzan) secara deterministik
 * serta menghitung progres waktu antara shalat sebelumnya dan shalat berikutnya.
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

    public static final class SolarWindow {
        private final PrayerType currentPhase;
        private final ZonedDateTime windowStart;
        private final PrayerType nextPrayer;
        private final ZonedDateTime nextPrayerTime;
        private final float progress;

        public SolarWindow(PrayerType currentPhase, ZonedDateTime windowStart,
                           PrayerType nextPrayer, ZonedDateTime nextPrayerTime, float progress) {
            this.currentPhase = currentPhase;
            this.windowStart = windowStart;
            this.nextPrayer = nextPrayer;
            this.nextPrayerTime = nextPrayerTime;
            this.progress = progress;
        }

        public PrayerType getCurrentPhase() {
            return currentPhase;
        }

        public ZonedDateTime getWindowStart() {
            return windowStart;
        }

        public PrayerType getNextPrayer() {
            return nextPrayer;
        }

        public ZonedDateTime getNextPrayerTime() {
            return nextPrayerTime;
        }

        public float getProgress() {
            return progress;
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
        EnumMap<PrayerType, Integer> map = new EnumMap<>(PrayerType.class);
        for (PrayerType type : PrayerType.values()) {
            map.put(type, preReminderMinutes);
        }
        return buildEventsForRange(
                now, latitude, longitude, zoneId, method, useIhtiyat,
                enabledPrayers, map, notifyOnTime
        );
    }

    public static List<ScheduledEvent> buildEventsForRange(
            ZonedDateTime now,
            double latitude,
            double longitude,
            ZoneId zoneId,
            CalculationMethod method,
            boolean useIhtiyat,
            Set<PrayerType> enabledPrayers,
            Map<PrayerType, Integer> perPrayerMinutes,
            boolean notifyOnTime
    ) {
        if (enabledPrayers == null || enabledPrayers.isEmpty()) {
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

                int preMinutes = 0;
                if (perPrayerMinutes != null) {
                    Integer val = perPrayerMinutes.get(type);
                    if (val != null) {
                        preMinutes = val;
                    }
                }

                if (preMinutes > 0) {
                    ZonedDateTime preTime = prayerTime.minusMinutes(preMinutes);
                    if (preTime.isAfter(now)) {
                        events.add(new ScheduledEvent(type, preTime, prayerTime, true, preMinutes));
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

    public static ScheduledEvent findNextEvent(
            ZonedDateTime now,
            double latitude,
            double longitude,
            ZoneId zoneId,
            CalculationMethod method,
            boolean useIhtiyat,
            Set<PrayerType> enabledPrayers,
            Map<PrayerType, Integer> perPrayerMinutes,
            boolean notifyOnTime
    ) {
        List<ScheduledEvent> events = buildEventsForRange(
                now, latitude, longitude, zoneId, method, useIhtiyat,
                enabledPrayers, perPrayerMinutes, notifyOnTime
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

    /**
     * Menghitung fase waktu shalat saat ini, shalat berikutnya, dan fraksi progres [0f..1f]
     * untuk animasi busur matahari (Solar Arc).
     */
    public static SolarWindow computeSolarWindow(
            PrayerSchedule yesterdaySchedule,
            PrayerSchedule todaySchedule,
            PrayerSchedule tomorrowSchedule,
            ZonedDateTime now
    ) {
        PrayerType[] order = new PrayerType[]{
                PrayerType.SUBUH,
                PrayerType.DZUHUR,
                PrayerType.ASHAR,
                PrayerType.MAGHRIB,
                PrayerType.ISYA
        };

        PrayerType prevType = PrayerType.ISYA;
        ZonedDateTime prevTime = yesterdaySchedule.getTime(PrayerType.ISYA);
        PrayerType nextType = PrayerType.SUBUH;
        ZonedDateTime nextTime = tomorrowSchedule.getTime(PrayerType.SUBUH);

        for (int i = 0; i < order.length; i++) {
            PrayerType candidate = order[i];
            ZonedDateTime candidateTime = todaySchedule.getTime(candidate);
            if (candidateTime != null && candidateTime.isAfter(now)) {
                nextType = candidate;
                nextTime = candidateTime;
                if (i == 0) {
                    prevType = PrayerType.ISYA;
                    prevTime = yesterdaySchedule.getTime(PrayerType.ISYA);
                } else {
                    prevType = order[i - 1];
                    prevTime = todaySchedule.getTime(prevType);
                }
                break;
            }
            if (i == order.length - 1) {
                prevType = PrayerType.ISYA;
                prevTime = todaySchedule.getTime(PrayerType.ISYA);
                nextType = PrayerType.SUBUH;
                nextTime = tomorrowSchedule.getTime(PrayerType.SUBUH);
            }
        }

        float progress = 0f;
        if (prevTime != null && nextTime != null) {
            long totalSec = Math.max(1L, Duration.between(prevTime, nextTime).getSeconds());
            long elapsedSec = Duration.between(prevTime, now).getSeconds();
            progress = Math.max(0f, Math.min(1f, (float) elapsedSec / (float) totalSec));
        }

        return new SolarWindow(prevType, prevTime, nextType, nextTime, progress);
    }
}
