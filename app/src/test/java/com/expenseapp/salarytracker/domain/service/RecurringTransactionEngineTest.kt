package com.expenseapp.salarytracker.domain.service

import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.RecurrenceFrequency
import com.expenseapp.salarytracker.domain.model.RecurringRule
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurringTransactionEngineTest {

    private val engine = RecurringTransactionEngine()

    @Test
    fun testJanuary31ToFebruaryInNonLeapYear() {
        val jan31 = LocalDate.of(2025, 1, 31) // 2025 is non-leap year
        val nextDue = engine.calculateNextDueDate(
            currentDueDate = jan31,
            frequency = RecurrenceFrequency.MONTHLY,
            preferredDayOfMonth = 31
        )
        // In 2025 February has 28 days -> must clamp to Feb 28
        assertEquals(LocalDate.of(2025, 2, 28), nextDue)
    }

    @Test
    fun testJanuary31ToFebruaryInLeapYear() {
        val jan31 = LocalDate.of(2024, 1, 31) // 2024 is leap year
        val nextDue = engine.calculateNextDueDate(
            currentDueDate = jan31,
            frequency = RecurrenceFrequency.MONTHLY,
            preferredDayOfMonth = 31
        )
        // In 2024 February has 29 days -> must clamp to Feb 29
        assertEquals(LocalDate.of(2024, 2, 29), nextDue)

        // Then from Feb 29 to March with preferredDay 31 -> must roll to March 31
        val marchDue = engine.calculateNextDueDate(
            currentDueDate = nextDue,
            frequency = RecurrenceFrequency.MONTHLY,
            preferredDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2024, 3, 31), marchDue)
    }

    @Test
    fun testYearlyLeapYearRollOver() {
        val leapDay = LocalDate.of(2024, 2, 29)
        val nextYearDue = engine.calculateNextDueDate(
            currentDueDate = leapDay,
            frequency = RecurrenceFrequency.YEARLY
        )
        // 2025 is non-leap year -> rolls safely to Feb 28
        assertEquals(LocalDate.of(2025, 2, 28), nextYearDue)
    }

    @Test
    fun testDuplicateAvoidance() {
        val rule = RecurringRule(
            id = 42,
            name = "Broadband Internet",
            amount = MinorUnits(150000L), // ৳1,500.00
            accountId = 1,
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2026, 1, 1),
            nextDueDate = LocalDate.of(2026, 1, 1)
        )

        // Transaction already exists on 2026-01-01 for rule 42
        val existingTx = listOf(
            Transaction(
                id = 101,
                amount = MinorUnits(150000L),
                type = TransactionType.EXPENSE,
                accountId = 1,
                date = LocalDate.of(2026, 1, 1),
                recurringRuleId = 42
            )
        )

        // Generate as of 2026-02-15
        val generated = engine.generateDueTransactions(
            rule = rule,
            asOfDate = LocalDate.of(2026, 2, 15),
            existingTransactions = existingTx
        )

        // Should ONLY generate for 2026-02-01, since 2026-01-01 already exists!
        assertEquals(1, generated.size)
        assertEquals(LocalDate.of(2026, 2, 1), generated[0].date)
        assertEquals(42L, generated[0].recurringRuleId)
    }

    @Test
    fun testSkipAndPauseRule() {
        val rule = RecurringRule(
            id = 10,
            name = "Gym Membership",
            amount = MinorUnits(200000L),
            accountId = 1,
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2026, 3, 1),
            nextDueDate = LocalDate.of(2026, 3, 1)
        )

        // Skip occurrence
        val skipped = engine.skipNextOccurrence(rule)
        assertEquals(LocalDate.of(2026, 4, 1), skipped.nextDueDate)

        // Pause rule
        val paused = engine.pauseRule(skipped)
        assertTrue(paused.isPaused)

        // No transactions generated while paused
        val generated = engine.generateDueTransactions(
            rule = paused,
            asOfDate = LocalDate.of(2026, 5, 1),
            existingTransactions = emptyList()
        )
        assertTrue(generated.isEmpty())

        // Resume rule
        val resumed = engine.resumeRule(paused)
        assertFalse(resumed.isPaused)
    }
}
