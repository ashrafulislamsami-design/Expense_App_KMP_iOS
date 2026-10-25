package com.expenseapp.salarytracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expenseapp.salarytracker.ExpenseApplication
import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.repository.AccountRepository
import com.expenseapp.salarytracker.domain.repository.BudgetRepository
import com.expenseapp.salarytracker.domain.repository.CategoryRepository
import com.expenseapp.salarytracker.domain.repository.GoalRepository
import com.expenseapp.salarytracker.domain.repository.SettingsRepository
import com.expenseapp.salarytracker.domain.repository.TransactionRepository
import com.expenseapp.salarytracker.domain.service.BudgetStatus
import com.expenseapp.salarytracker.domain.service.FinancialCalculationEngine
import com.expenseapp.salarytracker.domain.service.FinancialSummary
import com.expenseapp.salarytracker.domain.service.GoalCalculationEngine
import com.expenseapp.salarytracker.domain.service.GoalProgress
import com.expenseapp.salarytracker.domain.service.SafeToSpendEstimate
import com.expenseapp.salarytracker.domain.service.SalaryCycleCalculator
import com.expenseapp.salarytracker.domain.service.TreeOfHopeStatus
import com.expenseapp.salarytracker.domain.model.TreeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class DashboardTimeframe(val label: String) {
    CURRENT_CYCLE("Current cycle"),
    THIS_MONTH("This month"),
    LAST_7_DAYS("7 days"),
    LAST_30_DAYS("30 days")
}

data class CategorySpend(
    val categoryId: Long?,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val amount: MinorUnits,
    val percentage: Int,
    val transactionCount: Int
)

data class DailyTrendPoint(
    val date: LocalDate,
    val dayLabel: String,
    val currentAmount: MinorUnits,
    val previousAmount: MinorUnits
)

data class AccountBalanceItem(
    val account: Account,
    val balance: MinorUnits
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val selectedTimeframe: DashboardTimeframe = DashboardTimeframe.CURRENT_CYCLE,
    val periodLabel: String = "",
    val config: SalaryCycleConfig = SalaryCycleConfig(),
    val summary: FinancialSummary = FinancialSummary(),
    val previousSummary: FinancialSummary = FinancialSummary(),
    val effectiveIncome: MinorUnits = MinorUnits.ZERO,
    val totalBalance: MinorUnits = MinorUnits.ZERO,
    val accountBalances: List<AccountBalanceItem> = emptyList(),
    val safeToSpend: SafeToSpendEstimate = SafeToSpendEstimate(MinorUnits.ZERO, MinorUnits.ZERO, 0, ""),
    val overallBudget: Budget? = null,
    val overallBudgetStatus: BudgetStatus? = null,
    val categoryBudgets: List<BudgetStatus> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val categoriesMap: Map<Long, Category> = emptyMap(),
    val accountsMap: Map<Long, Account> = emptyMap(),
    val topSpendingCategories: List<CategorySpend> = emptyList(),
    val topIncomeCategories: List<CategorySpend> = emptyList(),
    val topDebtCategories: List<CategorySpend> = emptyList(),
    val totalDebtFlow: MinorUnits = MinorUnits.ZERO,
    val dailyTrend: List<DailyTrendPoint> = emptyList(),
    val current7DaySpend: MinorUnits = MinorUnits.ZERO,
    val previous7DaySpend: MinorUnits = MinorUnits.ZERO,
    val spendDeltaPercent: Int? = null,
    val incomeDeltaPercent: Int? = null,
    val savingsRatePercent: Int = 0,
    val activeGoals: List<Goal> = emptyList(),
    val goalProgressList: List<GoalProgress> = emptyList(),
    val treeOfHopeStatus: TreeOfHopeStatus = TreeOfHopeStatus(
        state = TreeState.SEED,
        scorePercentage = 0.0,
        stageName = TreeState.SEED.stageName,
        explanation = "",
        positiveActionTip = "",
        contributingFactors = emptyList()
    ),
    val cycleStartDate: LocalDate = LocalDate.now(),
    val cycleEndDate: LocalDate = LocalDate.now(),
    val daysRemaining: Long = 0,
    val currencySymbol: String = "৳"
)

private data class CoreFlowBundle(
    val config: SalaryCycleConfig,
    val budgets: List<Budget>,
    val goals: List<Goal>,
    val allTx: List<Transaction>,
    val allCategories: List<Category>
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val calculationEngine: FinancialCalculationEngine,
    private val cycleCalculator: SalaryCycleCalculator,
    private val accountRepository: AccountRepository = ExpenseApplication.instance.accountRepository,
    private val goalCalculationEngine: GoalCalculationEngine = ExpenseApplication.instance.goalCalculationEngine
) : ViewModel() {

    private val selectedTimeframeFlow = MutableStateFlow(DashboardTimeframe.CURRENT_CYCLE)

    fun selectTimeframe(timeframe: DashboardTimeframe) {
        selectedTimeframeFlow.value = timeframe
    }

    private val coreBundleFlow = combine(
        settingsRepository.getSalaryCycleConfig(),
        budgetRepository.getAllBudgets(),
        goalRepository.getActiveGoals(),
        transactionRepository.getAllTransactions(),
        categoryRepository.getAllCategories()
    ) { config: SalaryCycleConfig, budgets: List<Budget>, goals: List<Goal>, allTx: List<Transaction>, allCategories: List<Category> ->
        CoreFlowBundle(config, budgets, goals, allTx, allCategories)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        coreBundleFlow,
        accountRepository.getAllAccounts(),
        selectedTimeframeFlow
    ) { bundle: CoreFlowBundle, allAccounts: List<Account>, timeframe: DashboardTimeframe ->
        val today = LocalDate.now()
        val config = bundle.config
        val budgets = bundle.budgets
        val goals = bundle.goals
        val allTx = bundle.allTx
        val allCategories = bundle.allCategories

        val overallBudget = budgets.firstOrNull { it.categoryId == null }
        val cycleWindow = cycleCalculator.calculateWindow(today, config)

        // Determine active date window and previous comparison date window based on selected timeframe
        val (activeStart, activeEnd, prevStart, prevEnd) = when (timeframe) {
            DashboardTimeframe.CURRENT_CYCLE -> {
                val cycleDays = java.time.temporal.ChronoUnit.DAYS.between(cycleWindow.startDate, cycleWindow.endDate) + 1
                listOf(
                    cycleWindow.startDate,
                    cycleWindow.endDate,
                    cycleWindow.startDate.minusDays(cycleDays),
                    cycleWindow.startDate.minusDays(1)
                )
            }
            DashboardTimeframe.THIS_MONTH -> {
                val firstOfMonth = today.withDayOfMonth(1)
                val lastOfMonth = today.withDayOfMonth(today.lengthOfMonth())
                val prevFirst = firstOfMonth.minusMonths(1)
                val prevLast = firstOfMonth.minusDays(1)
                listOf(firstOfMonth, lastOfMonth, prevFirst, prevLast)
            }
            DashboardTimeframe.LAST_7_DAYS -> {
                listOf(today.minusDays(6), today, today.minusDays(13), today.minusDays(7))
            }
            DashboardTimeframe.LAST_30_DAYS -> {
                listOf(today.minusDays(29), today, today.minusDays(59), today.minusDays(30))
            }
        }

        val periodFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        val periodLabel = "${activeStart.format(periodFormatter)} – ${activeEnd.format(periodFormatter)}"

        // Filter transactions strictly within active and previous periods
        val periodTx = allTx.filter { !it.date.isBefore(activeStart) && !it.date.isAfter(activeEnd) }
        val prevPeriodTx = allTx.filter { !it.date.isBefore(prevStart) && !it.date.isAfter(prevEnd) }
        val cycleTx = allTx.filter { !it.date.isBefore(cycleWindow.startDate) && !it.date.isAfter(cycleWindow.endDate) }

        // Core Financial Summaries
        val summary = calculationEngine.calculateSummary(periodTx)
        val prevSummary = calculationEngine.calculateSummary(prevPeriodTx)

        val safeEstimate = calculationEngine.calculateSafeToSpend(
            window = cycleWindow,
            currentDate = today,
            overallBudget = overallBudget,
            salaryIncome = config.salaryAmount,
            periodTransactions = cycleTx
        )

        val effectiveIncome = if (summary.totalIncome.isPositive()) {
            summary.totalIncome
        } else {
            config.salaryAmount
        }

        // Percentage deltas vs previous period
        val spendDeltaPct: Int? = if (prevSummary.netExpense.isPositive()) {
            val diff = summary.netExpense.amountInMinorUnits - prevSummary.netExpense.amountInMinorUnits
            ((diff.toDouble() / prevSummary.netExpense.amountInMinorUnits.toDouble()) * 100.0).toInt()
        } else null

        val prevEffectiveIncome = if (prevSummary.totalIncome.isPositive()) prevSummary.totalIncome else config.salaryAmount
        val incomeDeltaPct: Int? = if (prevEffectiveIncome.isPositive() && summary.totalIncome.isPositive()) {
            val diff = summary.totalIncome.amountInMinorUnits - prevEffectiveIncome.amountInMinorUnits
            ((diff.toDouble() / prevEffectiveIncome.amountInMinorUnits.toDouble()) * 100.0).toInt()
        } else null

        // Savings / Retention rate in current period
        val netSavedOrRetained = if (summary.totalSavings.isPositive()) {
            summary.totalSavings
        } else if (effectiveIncome > summary.netExpense) {
            effectiveIncome - summary.netExpense
        } else {
            MinorUnits.ZERO
        }
        val savingsRatePct = if (effectiveIncome.isPositive()) {
            ((netSavedOrRetained.amountInMinorUnits.toDouble() / effectiveIncome.amountInMinorUnits.toDouble()) * 100.0)
                .toInt()
                .coerceIn(0, 100)
        } else 0

        // Account balances across all history
        val accountBalances = allAccounts.filter { !it.isArchived }.map { account ->
            AccountBalanceItem(
                account = account,
                balance = calculationEngine.calculateAccountBalance(account, allTx)
            )
        }
        val totalAccountBalance = if (accountBalances.isNotEmpty()) {
            val sum = accountBalances.fold(MinorUnits.ZERO) { acc, item -> acc + item.balance }
            if (sum.isZero() && allTx.isEmpty()) safeEstimate.remainingBudget else sum
        } else {
            safeEstimate.remainingBudget
        }

        // Category breakdown with real colors and icons (expenses + donations, minus category refunds)
        val categoryMap = allCategories.associateBy { it.id }
        val accountsMap = allAccounts.associateBy { it.id }
        val categoryNetMap = calculationEngine.calculateCategoryBreakdown(periodTx)
        val uncategorizedSpend = periodTx
            .filter { it.categoryId == null && (it.type == TransactionType.EXPENSE || it.type == TransactionType.DONATION) }
            .fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }

        val totalCategorySpendUnits = categoryNetMap.values.sumOf { it.amountInMinorUnits } + uncategorizedSpend.amountInMinorUnits

        val topCategories = if (totalCategorySpendUnits > 0L) {
            val list = mutableListOf<CategorySpend>()
            for ((catId, amount) in categoryNetMap) {
                if (!amount.isPositive()) continue
                val cat = categoryMap[catId]
                val count = periodTx.count {
                    it.categoryId == catId && (it.type == TransactionType.EXPENSE || it.type == TransactionType.DONATION)
                }
                val pct = ((amount.amountInMinorUnits.toDouble() / totalCategorySpendUnits.toDouble()) * 100.0)
                    .toInt()
                    .coerceIn(1, 100)
                list.add(
                    CategorySpend(
                        categoryId = catId,
                        name = cat?.name ?: "Other",
                        colorHex = cat?.colorHex ?: "#3B82F6",
                        iconName = cat?.iconName ?: "category",
                        amount = amount,
                        percentage = pct,
                        transactionCount = count
                    )
                )
            }
            if (uncategorizedSpend.isPositive()) {
                val count = periodTx.count {
                    it.categoryId == null && (it.type == TransactionType.EXPENSE || it.type == TransactionType.DONATION)
                }
                val pct = ((uncategorizedSpend.amountInMinorUnits.toDouble() / totalCategorySpendUnits.toDouble()) * 100.0)
                    .toInt()
                    .coerceIn(1, 100)
                list.add(
                    CategorySpend(
                        categoryId = null,
                        name = "Uncategorized",
                        colorHex = "#64748B",
                        iconName = "more_horiz",
                        amount = uncategorizedSpend,
                        percentage = pct,
                        transactionCount = count
                    )
                )
            }
            list.sortedByDescending { it.amount.amountInMinorUnits }.take(6)
        } else {
            emptyList()
        }

        // Income Category Breakdown
        val incomeTx = periodTx.filter { it.type == TransactionType.INCOME }
        val totalIncomeUnits = incomeTx.sumOf { it.amount.amountInMinorUnits }
        val topIncomeCategories = if (totalIncomeUnits > 0L) {
            val list = mutableListOf<CategorySpend>()
            val groupedByCat = incomeTx.groupBy { it.categoryId }
            for ((catId, txList) in groupedByCat) {
                val sum = txList.fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }
                if (!sum.isPositive()) continue
                val cat = if (catId != null) categoryMap[catId] else null
                val pct = ((sum.amountInMinorUnits.toDouble() / totalIncomeUnits.toDouble()) * 100.0).toInt().coerceIn(1, 100)
                list.add(
                    CategorySpend(
                        categoryId = catId,
                        name = cat?.name ?: "General Income",
                        colorHex = cat?.colorHex ?: "#10B981",
                        iconName = cat?.iconName ?: "payments",
                        amount = sum,
                        percentage = pct,
                        transactionCount = txList.size
                    )
                )
            }
            list.sortedByDescending { it.amount.amountInMinorUnits }.take(6)
        } else {
            emptyList()
        }

        // Debt & Loan Category Breakdown
        val debtTx = periodTx.filter { it.type == TransactionType.DEBT_RECEIVED || it.type == TransactionType.DEBT_PAYMENT }
        val totalDebtUnits = debtTx.sumOf { it.amount.amountInMinorUnits }
        val topDebtCategories = if (totalDebtUnits > 0L) {
            val list = mutableListOf<CategorySpend>()
            val groupedByCat = debtTx.groupBy { it.categoryId }
            for ((catId, txList) in groupedByCat) {
                val sum = txList.fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }
                if (!sum.isPositive()) continue
                val cat = if (catId != null) categoryMap[catId] else null
                val firstType = txList.first().type
                val defaultName = if (firstType == TransactionType.DEBT_RECEIVED) "Debt Received" else "Debt Repayment"
                val defaultColor = if (firstType == TransactionType.DEBT_RECEIVED) "#E11D48" else "#F97316"
                val pct = ((sum.amountInMinorUnits.toDouble() / totalDebtUnits.toDouble()) * 100.0).toInt().coerceIn(1, 100)
                list.add(
                    CategorySpend(
                        categoryId = catId,
                        name = cat?.name ?: defaultName,
                        colorHex = cat?.colorHex ?: defaultColor,
                        iconName = cat?.iconName ?: "credit_card",
                        amount = sum,
                        percentage = pct,
                        transactionCount = txList.size
                    )
                )
            }
            list.sortedByDescending { it.amount.amountInMinorUnits }.take(6)
        } else {
            emptyList()
        }

        // 7-Day Daily Spending Pulse (Current 7 days vs Previous 7 days)
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        val dailyTrendPoints = (6 downTo 0).map { daysAgo ->
            val targetDay = today.minusDays(daysAgo.toLong())
            val prevDay = targetDay.minusDays(7)

            val daySpend = allTx
                .filter { it.date == targetDay && (it.type == TransactionType.EXPENSE || it.type == TransactionType.DONATION) }
                .fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }
            val dayRefund = allTx
                .filter { it.date == targetDay && it.type == TransactionType.REFUND }
                .fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }
            val netDaySpend = if (daySpend > dayRefund) daySpend - dayRefund else MinorUnits.ZERO

            val prevDaySpend = allTx
                .filter { it.date == prevDay && (it.type == TransactionType.EXPENSE || it.type == TransactionType.DONATION) }
                .fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }
            val prevDayRefund = allTx
                .filter { it.date == prevDay && it.type == TransactionType.REFUND }
                .fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }
            val netPrevDaySpend = if (prevDaySpend > prevDayRefund) prevDaySpend - prevDayRefund else MinorUnits.ZERO

            DailyTrendPoint(
                date = targetDay,
                dayLabel = targetDay.format(dayFormatter),
                currentAmount = netDaySpend,
                previousAmount = netPrevDaySpend
            )
        }

        val current7DayTotal = dailyTrendPoints.fold(MinorUnits.ZERO) { acc, pt -> acc + pt.currentAmount }
        val previous7DayTotal = dailyTrendPoints.fold(MinorUnits.ZERO) { acc, pt -> acc + pt.previousAmount }

        // Budgets status
        val allBudgetsStatus = calculationEngine.calculateAllBudgetsStatus(budgets, cycleTx, categoryMap)
        val overallBudgetStatus = allBudgetsStatus.firstOrNull { it.budget.categoryId == null }
        val categoryBudgetsStatus = allBudgetsStatus
            .filter { it.budget.categoryId != null }
            .sortedByDescending { it.percentageUsed }

        // Goals & Tree of Hope status
        val goalProgressList = goals.map { goal ->
            goalCalculationEngine.calculateGoalProgress(goal, allTx, today)
        }
        val treeOfHope = goalCalculationEngine.calculateTreeOfHopeState(
            activeGoalsWithProgress = goalProgressList,
            overallBudgetStatus = overallBudgetStatus
        )

        DashboardUiState(
            isLoading = false,
            selectedTimeframe = timeframe,
            periodLabel = periodLabel,
            config = config,
            summary = summary,
            previousSummary = prevSummary,
            effectiveIncome = effectiveIncome,
            totalBalance = totalAccountBalance,
            accountBalances = accountBalances,
            safeToSpend = safeEstimate,
            overallBudget = overallBudget,
            overallBudgetStatus = overallBudgetStatus,
            categoryBudgets = categoryBudgetsStatus,
            recentTransactions = allTx.take(6),
            categoriesMap = categoryMap,
            accountsMap = accountsMap,
            topSpendingCategories = topCategories,
            topIncomeCategories = topIncomeCategories,
            topDebtCategories = topDebtCategories,
            totalDebtFlow = MinorUnits(totalDebtUnits),
            dailyTrend = dailyTrendPoints,
            current7DaySpend = current7DayTotal,
            previous7DaySpend = previous7DayTotal,
            spendDeltaPercent = spendDeltaPct,
            incomeDeltaPercent = incomeDeltaPct,
            savingsRatePercent = savingsRatePct,
            activeGoals = goals,
            goalProgressList = goalProgressList,
            treeOfHopeStatus = treeOfHope,
            cycleStartDate = cycleWindow.startDate,
            cycleEndDate = cycleWindow.endDate,
            daysRemaining = cycleWindow.remainingDays(today),
            currencySymbol = when (config.currency.uppercase()) {
                "BDT" -> "৳"
                "USD" -> "$"
                "EUR" -> "€"
                "GBP" -> "£"
                else -> config.currency
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository = ExpenseApplication.instance.categoryRepository,
        private val budgetRepository: BudgetRepository,
        private val goalRepository: GoalRepository,
        private val settingsRepository: SettingsRepository,
        private val calculationEngine: FinancialCalculationEngine,
        private val cycleCalculator: SalaryCycleCalculator,
        private val accountRepository: AccountRepository = ExpenseApplication.instance.accountRepository,
        private val goalCalculationEngine: GoalCalculationEngine = ExpenseApplication.instance.goalCalculationEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                transactionRepository = transactionRepository,
                categoryRepository = categoryRepository,
                budgetRepository = budgetRepository,
                goalRepository = goalRepository,
                settingsRepository = settingsRepository,
                calculationEngine = calculationEngine,
                cycleCalculator = cycleCalculator,
                accountRepository = accountRepository,
                goalCalculationEngine = goalCalculationEngine
            ) as T
        }
    }
}
