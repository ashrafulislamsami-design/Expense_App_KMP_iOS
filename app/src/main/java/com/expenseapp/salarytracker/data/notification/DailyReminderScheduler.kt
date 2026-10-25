package com.expenseapp.salarytracker.data.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object DailyReminderScheduler {

    const val WORK_NAME_DAILY_REMINDER = "daily_expense_reminder_work"
    const val TAG_DAILY_REMINDER = "tag_daily_reminder"

    /**
     * Calculates the initial delay in milliseconds from [now] to the next occurrence of [targetTime]
     * on any day present in [activeDays].
     *
     * Pure function with deterministic output:
     * - Respects month boundaries, leap years, and week boundaries.
     * - Returns 0L if invalid or empty days provided.
     */
    fun calculateInitialDelay(
        now: LocalDateTime,
        targetTime: LocalTime,
        activeDays: Set<DayOfWeek>
    ): Long {
        if (activeDays.isEmpty()) return 0L

        val today = now.toLocalDate()
        val isTodayActive = activeDays.contains(today.dayOfWeek)
        val isTargetLaterToday = now.toLocalTime().isBefore(targetTime)

        val targetDateTime = if (isTodayActive && isTargetLaterToday) {
            LocalDateTime.of(today, targetTime)
        } else {
            // Find the next active day
            var nextActiveDate: LocalDate? = null
            for (daysAhead in 1L..7L) {
                val candidateDate = today.plusDays(daysAhead)
                if (activeDays.contains(candidateDate.dayOfWeek)) {
                    nextActiveDate = candidateDate
                    break
                }
            }
            val resolvedDate = nextActiveDate ?: today.plusDays(1)
            LocalDateTime.of(resolvedDate, targetTime)
        }

        val duration = Duration.between(now, targetDateTime)
        return duration.toMillis().coerceAtLeast(0L)
    }

    /**
     * Parses a string formatted as "HH:mm" into [LocalTime], defaulting to 23:00 if invalid.
     */
    fun parseTimeString(timeString: String): LocalTime {
        return runCatching {
            LocalTime.parse(timeString.trim(), DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrElse {
            LocalTime.of(23, 0)
        }
    }

    /**
     * Enqueues or cancels the unique WorkManager reminder according to user settings.
     */
    fun schedule(
        context: Context,
        enabled: Boolean,
        timeString: String,
        activeDays: Set<DayOfWeek>
    ) {
        val workManager = WorkManager.getInstance(context)

        if (!enabled || activeDays.isEmpty()) {
            workManager.cancelUniqueWork(WORK_NAME_DAILY_REMINDER)
            return
        }

        val targetTime = parseTimeString(timeString)
        val now = LocalDateTime.now()
        val delayMillis = calculateInitialDelay(now, targetTime, activeDays)

        val workRequest = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(TAG_DAILY_REMINDER)
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME_DAILY_REMINDER,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
