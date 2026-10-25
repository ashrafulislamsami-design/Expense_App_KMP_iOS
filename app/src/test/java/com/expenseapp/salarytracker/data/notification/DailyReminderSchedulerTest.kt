package com.expenseapp.salarytracker.data.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class DailyReminderSchedulerTest {

    @Test
    fun testParseTimeString_validFormats() {
        val t1 = DailyReminderScheduler.parseTimeString("23:00")
        assertEquals(LocalTime.of(23, 0), t1)

        val t2 = DailyReminderScheduler.parseTimeString("08:15")
        assertEquals(LocalTime.of(8, 15), t2)

        val t3 = DailyReminderScheduler.parseTimeString("00:00")
        assertEquals(LocalTime.of(0, 0), t3)
    }

    @Test
    fun testParseTimeString_invalidFallsBackTo2300() {
        val tInvalid = DailyReminderScheduler.parseTimeString("not_a_time")
        assertEquals(LocalTime.of(23, 0), tInvalid)

        val tEmpty = DailyReminderScheduler.parseTimeString("")
        assertEquals(LocalTime.of(23, 0), tEmpty)
    }

    @Test
    fun testCalculateInitialDelay_emptyActiveDays_returnsZero() {
        val now = LocalDateTime.of(2026, 3, 15, 10, 0)
        val delay = DailyReminderScheduler.calculateInitialDelay(
            now = now,
            targetTime = LocalTime.of(23, 0),
            activeDays = emptySet()
        )
        assertEquals(0L, delay)
    }

    @Test
    fun testCalculateInitialDelay_sameDayLater() {
        // Wednesday March 18, 2026, 14:00. Target: 23:00. Wednesday is active.
        val now = LocalDateTime.of(2026, 3, 18, 14, 0)
        val target = LocalTime.of(23, 0)
        val activeDays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

        val delay = DailyReminderScheduler.calculateInitialDelay(now, target, activeDays)
        // 9 hours later = 9 * 3600 * 1000 = 32_400_000 ms
        val expectedMs = TimeUnit.HOURS.toMillis(9)
        assertEquals(expectedMs, delay)
    }

    @Test
    fun testCalculateInitialDelay_sameDayPassed_rollsOverToNextActiveDay() {
        // Wednesday March 18, 2026, 23:30 (already passed 23:00).
        // Active days: Wednesday and Friday. Next active day should be Friday March 20, 2026 at 23:00.
        val now = LocalDateTime.of(2026, 3, 18, 23, 30)
        val target = LocalTime.of(23, 0)
        val activeDays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

        val delay = DailyReminderScheduler.calculateInitialDelay(now, target, activeDays)
        // From Wed 23:30 to Thu 23:30 = 24h. To Fri 23:00 = 47.5 hours = 47h 30m
        val expectedMs = TimeUnit.HOURS.toMillis(47) + TimeUnit.MINUTES.toMillis(30)
        assertEquals(expectedMs, delay)
    }

    @Test
    fun testCalculateInitialDelay_todayNotActive_findsNextActiveDay() {
        // Tuesday March 17, 2026, 10:00.
        // Active days: Thursday and Saturday. Next active day: Thursday March 19 at 21:00.
        val now = LocalDateTime.of(2026, 3, 17, 10, 0)
        val target = LocalTime.of(21, 0)
        val activeDays = setOf(DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)

        val delay = DailyReminderScheduler.calculateInitialDelay(now, target, activeDays)
        // Tue 10:00 to Wed 10:00 (24h) + Wed 10:00 to Thu 10:00 (24h) + Thu 10:00 to 21:00 (11h) = 59 hours
        val expectedMs = TimeUnit.HOURS.toMillis(59)
        assertEquals(expectedMs, delay)
    }

    @Test
    fun testCalculateInitialDelay_monthBoundaryRollover() {
        // Jan 31, 2026 at 23:45. Target is 23:00 (passed).
        // Active days: All days. Next day is Feb 1, 2026 at 23:00.
        val now = LocalDateTime.of(2026, 1, 31, 23, 45)
        val target = LocalTime.of(23, 0)
        val allDays = DayOfWeek.values().toSet()

        val delay = DailyReminderScheduler.calculateInitialDelay(now, target, allDays)
        // From Jan 31 23:45 to Feb 1 23:00 is 23 hours 15 minutes.
        val expectedMs = TimeUnit.HOURS.toMillis(23) + TimeUnit.MINUTES.toMillis(15)
        assertEquals(expectedMs, delay)
    }

    @Test
    fun testCalculateInitialDelay_leapYearBoundaryRollover() {
        // Leap year 2024: Feb 28, 2024 at 23:15. Target is 23:00 (passed).
        // Next active day is Feb 29, 2024 at 23:00.
        val now = LocalDateTime.of(2024, 2, 28, 23, 15)
        val target = LocalTime.of(23, 0)
        val allDays = DayOfWeek.values().toSet()

        val delay = DailyReminderScheduler.calculateInitialDelay(now, target, allDays)
        val expectedMs = TimeUnit.HOURS.toMillis(23) + TimeUnit.MINUTES.toMillis(45)
        assertEquals(expectedMs, delay)
    }
}
