package com.expenseapp.salarytracker.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.model.BudgetPeriod
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.repository.BudgetRepository
import com.expenseapp.salarytracker.domain.repository.CategoryRepository
import com.expenseapp.salarytracker.domain.repository.SettingsRepository
import com.expenseapp.salarytracker.domain.repository.TransactionRepository
import com.expenseapp.salarytracker.domain.service.BudgetStatus
import com.expenseapp.salarytracker.domain.service.DateWindow
import com.expenseapp.salarytracker.domain.service.FinancialCalculationEngine
import com.expenseapp.salarytracker.domain.service.SalaryCycleCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BudgetsUiState(
    val isLoading: Boolean = false,
    val overallBudgetStatus: BudgetStatus? = null,
    val categoryBudgets: List<BudgetStatus> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val unbudgetedCategories: List<Category> = emptyList(),
    val activeWindow: DateWindow = DateWindow(LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())),
    val currencySymbol: String = "৳"
)

class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val calculationEngine: FinancialCalculationEngine,
    private val salaryCycleCalculator: SalaryCycleCalculator
) : ViewModel() {

    val uiState: StateFlow<BudgetsUiState> = combine(
        budgetRepository.getAllBudgets(),
        transactionRepository.getAllTransactions(),
        categoryRepository.getAllCategories(),
        settingsRepository.getSalaryCycleConfig(),
        settingsRepository.getCurrency()
    ) { budgets: List<Budget>, allTxs: List<Transaction>, categories: List<Category>, cycleConfig: SalaryCycleConfig, currencyCode: String ->
        val currencySymbol = when (currencyCode) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "INR" -> "₹"
            else -> "৳"
        }
        val today = LocalDate.now()
        val window = salaryCycleCalculator.calculateWindow(today, cycleConfig)
        val categoriesMap = categories.associateBy { it.id }

        // Filter transactions within the active cycle window
        val periodTxs = allTxs.filter { window.contains(it.date) }

        // Calculate overall budget
        val overallBudget = budgets.firstOrNull { it.categoryId == null }
        val overallStatus = overallBudget?.let {
            calculationEngine.calculateBudgetStatus(it, periodTxs, null)
        }

        // Calculate category budgets
        val categoryBudgetList = budgets.filter { it.categoryId != null }
        val categoryStatuses = calculationEngine.calculateAllBudgetsStatus(
            budgets = categoryBudgetList,
            transactions = periodTxs,
            categoriesMap = categoriesMap
        )

        // Find categories that don't have a budget yet
        val budgetedCategoryIds = categoryBudgetList.mapNotNull { it.categoryId }.toSet()
        val unbudgeted = categories.filter { cat -> !budgetedCategoryIds.contains(cat.id) }

        BudgetsUiState(
            isLoading = false,
            overallBudgetStatus = overallStatus,
            categoryBudgets = categoryStatuses,
            allCategories = categories,
            unbudgetedCategories = unbudgeted,
            activeWindow = window,
            currencySymbol = currencySymbol
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState(isLoading = true)
    )

    fun saveBudget(
        id: Long = 0,
        categoryId: Long?,
        limitAmount: MinorUnits,
        period: BudgetPeriod = BudgetPeriod.SALARY_CYCLE,
        warningThreshold: Int = 80
    ) {
        viewModelScope.launch {
            val budget = Budget(
                id = id,
                categoryId = categoryId,
                limitAmount = limitAmount,
                period = period,
                warningThresholdPercentage = warningThreshold
            )
            budgetRepository.insertOrUpdateBudget(budget)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(id)
        }
    }

    class Factory(
        private val budgetRepository: BudgetRepository,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val settingsRepository: SettingsRepository,
        private val calculationEngine: FinancialCalculationEngine,
        private val salaryCycleCalculator: SalaryCycleCalculator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BudgetsViewModel(
                budgetRepository,
                transactionRepository,
                categoryRepository,
                settingsRepository,
                calculationEngine,
                salaryCycleCalculator
            ) as T
        }
    }
}
