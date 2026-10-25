package com.expenseapp.salarytracker.domain.service

import com.expenseapp.salarytracker.domain.model.RecurrenceFrequency
import com.expenseapp.salarytracker.domain.model.RecurringRule
import com.expenseapp.salarytracker.domain.model.Transaction
import java.time.LocalDate
import java.time.LocalTime

class RecurringTransactionEngine {

    /**
     * Calculates next deterministic due date respecting month-end bounds and leap years.
     * If preferredDayOfMonth is provided (e.g., 31), rolls correctly into 28/29/30-day months.
     */
    fun calculateNextDueDate(
        currentDueDate: LocalDate,
        frequency: RecurrenceFrequency,
        preferredDayOfMonth: Int? = null,
        intervalDays: Int = 30
    ): LocalDate {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> currentDueDate.plusDays(1)
            RecurrenceFrequency.WEEKLY -> currentDueDate.plusWeeks(1)
            RecurrenceFrequency.BI_WEEKLY -> currentDueDate.plusWeeks(2)
            RecurrenceFrequency.MONTHLY -> {
                val targetMonth = currentDueDate.plusMonths(1)
                val targetDay = if (preferredDayOfMonth != null) {
                    minOf(preferredDayOfMonth, targetMonth.lengthOfMonth())
                } else {
                    minOf(currentDueDate.dayOfMonth, targetMonth.lengthOfMonth())
                }
                targetMonth.withDayOfMonth(targetDay)
            }
            RecurrenceFrequency.YEARLY -> {
                currentDueDate.plusYears(1)
            }
            RecurrenceFrequency.CUSTOM -> {
                currentDueDate.plusDays(maxOf(1, intervalDays).toLong())
            }
        }
    }

    /**
     * Identifies pending transactions for a rule up to asOfDate.
     * Invariant: Never generates a transaction if one already exists for that rule on that date.
     */
    fun generateDueTransactions(
        rule: RecurringRule,
        asOfDate: LocalDate,
        existingTransactions: Collection<Transaction>
    ): List<Transaction> {
        if (rule.isPaused) return emptyList()

        val results = mutableListOf<Transaction>()
        var cursorDate = rule.nextDueDate
        val prefDay = rule.preferredDayOfMonth ?: rule.startDate.dayOfMonth

        while (!cursorDate.isAfter(asOfDate)) {
            // Check end date bounds
            if (rule.endDate != null && cursorDate.isAfter(rule.endDate)) {
                break
            }

            // Invariant: Avoid duplicate transactions for the same recurring rule on the same date
            val alreadyExists = existingTransactions.any {
                it.recurringRuleId == rule.id && it.date == cursorDate
            }

            if (!alreadyExists) {
                val transaction = Transaction(
                    id = 0,
                    amount = rule.amount,
                    type = rule.type,
                    accountId = rule.accountId,
                    destinationAccountId = rule.destinationAccountId,
                    categoryId = rule.categoryId,
                    date = cursorDate,
                    time = LocalTime.of(9, 0),
                    note = if (rule.note.isNotBlank()) rule.note else "Recurring: ${rule.name}",
                    recurringRuleId = rule.id
                )
                results.add(transaction)
            }

            cursorDate = calculateNextDueDate(
                currentDueDate = cursorDate,
                frequency = rule.frequency,
                preferredDayOfMonth = prefDay,
                intervalDays = rule.intervalDays
            )
        }

        return results
    }

    /**
     * Skips the current occurrence of a rule, advancing its nextDueDate to the next period.
     */
    fun skipNextOccurrence(rule: RecurringRule): RecurringRule {
        val prefDay = rule.preferredDayOfMonth ?: rule.startDate.dayOfMonth
        val nextDate = calculateNextDueDate(
            currentDueDate = rule.nextDueDate,
            frequency = rule.frequency,
            preferredDayOfMonth = prefDay,
            intervalDays = rule.intervalDays
        )
        return rule.copy(nextDueDate = nextDate)
    }

    /**
     * Pauses the recurring rule.
     */
    fun pauseRule(rule: RecurringRule): RecurringRule = rule.copy(isPaused = true)

    /**
     * Resumes the recurring rule.
     */
    fun resumeRule(rule: RecurringRule): RecurringRule = rule.copy(isPaused = false)
}
