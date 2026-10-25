package com.expenseapp.salarytracker.domain.service.notification

import com.expenseapp.salarytracker.data.dao.UserSettingDao
import com.expenseapp.salarytracker.data.entity.UserSettingEntity
import com.expenseapp.salarytracker.domain.model.BudgetPeriod
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.repository.BudgetRepository
import com.expenseapp.salarytracker.domain.repository.CategoryRepository
import com.expenseapp.salarytracker.domain.repository.SettingsRepository
import com.expenseapp.salarytracker.domain.repository.TransactionRepository
import com.expenseapp.salarytracker.domain.service.FinancialCalculationEngine
import com.expenseapp.salarytracker.domain.service.SalaryCycleCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class BudgetNotificationService(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val userSettingDao: UserSettingDao,
    private val notificationManager: NotificationManagerService,
    private val calculationEngine: FinancialCalculationEngine = FinancialCalculationEngine(),
    private val salaryCycleCalculator: SalaryCycleCalculator = SalaryCycleCalculator()
) {

    /**
     * Evaluates current spending against all active budgets.
     * Deduplicates alerts deterministically per budget period and threshold.
     */
    suspend fun evaluateBudgets(today: LocalDate = LocalDate.now()) {
        val alertsEnabled = settingsRepository.isBudgetAlertsEnabled().first()
        if (!alertsEnabled) return

        val budgets = budgetRepository.getAllBudgets().first()
        if (budgets.isEmpty()) return

        val allTransactions = transactionRepository.getAllTransactions().first()
        val allCategories = categoryRepository.getAllCategories().first().associateBy { it.id }

        for (budget in budgets) {
            // Determine active dates for this budget
            val (startDate, endDate) = resolveBudgetDates(budget.period, budget.startDate, budget.endDate, today)

            // Filter transactions strictly within this budget window
            val relevantTxs = allTransactions.filter { tx ->
                !tx.date.isBefore(startDate) && !tx.date.isAfter(endDate)
            }

            val status = calculationEngine.calculateBudgetStatus(budget, relevantTxs)
            val budgetName = if (budget.categoryId != null) {
                allCategories[budget.categoryId]?.name ?: "Category"
            } else {
                "Overall Monthly"
            }

            val periodKey = "${budget.id}_${startDate}_${endDate}"

            if (status.isOverBudget) {
                val dedupKey = "budget_alert_exceeded_$periodKey"
                val alreadyNotified = userSettingDao.getSettingDirect(dedupKey) != null
                if (!alreadyNotified) {
                    notificationManager.showBudgetAlertNotification(
                        budgetId = budget.id,
                        budgetName = budgetName,
                        percentage = status.percentageUsed.toInt(),
                        isExceeded = true
                    )
                    userSettingDao.setSetting(UserSettingEntity(dedupKey, "true"))
                }
            } else if (status.isWarning) {
                val dedupKey = "budget_alert_warning_$periodKey"
                val alreadyNotified = userSettingDao.getSettingDirect(dedupKey) != null
                if (!alreadyNotified) {
                    notificationManager.showBudgetAlertNotification(
                        budgetId = budget.id,
                        budgetName = budgetName,
                        percentage = status.percentageUsed.toInt(),
                        isExceeded = false
                    )
                    userSettingDao.setSetting(UserSettingEntity(dedupKey, "true"))
                }
            }
        }
    }

    private fun resolveBudgetDates(
        period: BudgetPeriod,
        budgetStart: LocalDate?,
        budgetEnd: LocalDate?,
        referenceDate: LocalDate
    ): Pair<LocalDate, LocalDate> {
        return when (period) {
            BudgetPeriod.MONTHLY -> {
                val start = referenceDate.withDayOfMonth(1)
                val end = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth())
                start to end
            }
            BudgetPeriod.SALARY_CYCLE -> {
                val window = salaryCycleCalculator.calculateWindow(referenceDate, SalaryCycleConfig())
                window.startDate to window.endDate
            }
            BudgetPeriod.CUSTOM -> {
                val start = budgetStart ?: referenceDate
                val end = budgetEnd ?: start.plusMonths(1)
                start to end
            }
        }
    }
}
