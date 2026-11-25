package com.jadwalshalat.gt5

import com.jadwalshalat.gt5.notification.PhoneVibrationPattern
import com.jadwalshalat.gt5.work.PrayerWorkScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerWorkAndVibrationTest {

    @Test
    fun computeDelayBeforeFourAmTargetsSameDay() {
        val zone = ZoneId.of("Asia/Jakarta")
        val now = ZonedDateTime.of(2026, 11, 25, 2, 30, 0, 0, zone)
        val delayMinutes = PrayerWorkScheduler.computeDelayToNextDailyAnchorMinutes(now)
        assertEquals(90L, delayMinutes)
    }

    @Test
    fun computeDelayAfterFourAmTargetsNextDay() {
        val zone = ZoneId.of("Asia/Jakarta")
        val now = ZonedDateTime.of(2026, 11, 25, 6, 0, 0, 0, zone)
        val delayMinutes = PrayerWorkScheduler.computeDelayToNextDailyAnchorMinutes(now)
        assertEquals(22L * 60L, delayMinutes)
    }

    @Test
    fun phoneVibrationPatternsAreWellFormed() {
        assertEquals(PhoneVibrationPattern.SHORT, PhoneVibrationPattern.fromKey("short"))
        assertEquals(PhoneVibrationPattern.STRONG, PhoneVibrationPattern.fromKey("STRONG"))
        assertEquals(PhoneVibrationPattern.MEDIUM, PhoneVibrationPattern.fromKey(null))
        assertEquals(PhoneVibrationPattern.MEDIUM, PhoneVibrationPattern.fromKey("unknown"))

        PhoneVibrationPattern.entries.forEach { pattern ->
            assertEquals(pattern.timings.size, pattern.amplitudes.size)
            assertTrue(pattern.timings.size >= 4)
            assertEquals(0L, pattern.timings.first())
            assertEquals(0, pattern.amplitudes.first())
        }
    }
}
