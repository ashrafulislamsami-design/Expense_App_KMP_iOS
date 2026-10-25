package com.expenseapp.salarytracker.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.model.GoalStatus
import com.expenseapp.salarytracker.domain.model.GoalType
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TreeHistory
import com.expenseapp.salarytracker.domain.model.TreeState
import com.expenseapp.salarytracker.domain.repository.AccountRepository
import com.expenseapp.salarytracker.domain.repository.BudgetRepository
import com.expenseapp.salarytracker.domain.repository.CategoryRepository
import com.expenseapp.salarytracker.domain.repository.GoalRepository
import com.expenseapp.salarytracker.domain.repository.SettingsRepository
import com.expenseapp.salarytracker.domain.repository.TransactionRepository
import com.expenseapp.salarytracker.domain.service.FinancialCalculationEngine
import com.expenseapp.salarytracker.domain.service.GoalCalculationEngine
import com.expenseapp.salarytracker.domain.service.GoalProgress
import com.expenseapp.salarytracker.domain.service.TreeOfHopeStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoalsUiState(
    val isLoading: Boolean = false,
    val activeGoals: List<GoalProgress> = emptyList(),
    val completedGoals: List<GoalProgress> = emptyList(),
    val pausedGoals: List<GoalProgress> = emptyList(),
    val treeOfHopeStatus: TreeOfHopeStatus = TreeOfHopeStatus(
        state = TreeState.SEED,
        scorePercentage = 0.0,
        stageName = "Seed",
        explanation = "Plant your financial goals to grow your Tree of Hope.",
        positiveActionTip = "Create a goal to begin.",
        contributingFactors = emptyList()
    ),
    val treeHistory: List<TreeHistory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val currencySymbol: String = "৳"
)

class GoalsViewModel(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val goalCalculationEngine: GoalCalculationEngine,
    private val calculationEngine: FinancialCalculationEngine
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.getAllGoals(),
        transactionRepository.getAllTransactions(),
        categoryRepository.getAllCategories(),
        accountRepository.getAllAccounts(),
        settingsRepository.getCurrency()
    ) { allGoals: List<Goal>, allTxs: List<Transaction>, categories: List<Category>, accounts: List<Account>, currencyCode: String ->
        val currencySymbol = when (currencyCode) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "INR" -> "₹"
            else -> "৳"
        }
        val today = LocalDate.now()

        // Calculate progress for each goal
        val allProgress = allGoals.map { goal ->
            val progress = goalCalculationEngine.calculateGoalProgress(goal, allTxs, today)

            // Auto-persist newly reached milestones if any
            if (progress.newMilestonesReached.isNotEmpty()) {
                val updatedMilestones = (goal.reachedMilestones + progress.newMilestonesReached).distinct().sorted()
                viewModelScope.launch {
                    goalRepository.updateMilestones(goal.id, updatedMilestones)
                    for (m in progress.newMilestonesReached) {
                        goalRepository.recordTreeHistory(
                            TreeHistory(
                                previousState = goal.treeState,
                                newState = progress.treeState,
                                reason = "${goal.name} reached ${m}% milestone!",
                                contributingGoalId = goal.id
                            )
                        )
                    }
                }
            }

            progress
        }

        val active = allProgress.filter { it.goal.status == GoalStatus.ACTIVE }
        val completed = allProgress.filter { it.goal.status == GoalStatus.COMPLETED }
        val paused = allProgress.filter { it.goal.status == GoalStatus.PAUSED }

        val treeStatus = goalCalculationEngine.calculateTreeOfHopeState(active, null)

        GoalsUiState(
            isLoading = false,
            activeGoals = active,
            completedGoals = completed,
            pausedGoals = paused,
            treeOfHopeStatus = treeStatus,
            categories = categories,
            accounts = accounts,
            currencySymbol = currencySymbol
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsUiState(isLoading = true)
    )

    fun createGoal(
        name: String,
        description: String,
        type: GoalType,
        targetAmount: MinorUnits,
        targetDays: Int,
        categoryId: Long?,
        accountId: Long?,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        viewModelScope.launch {
            val goal = Goal(
                name = name,
                description = description,
                type = type,
                targetAmount = targetAmount,
                targetDays = targetDays,
                categoryId = categoryId,
                accountId = accountId,
                startDate = startDate,
                endDate = endDate,
                status = GoalStatus.ACTIVE
            )
            val newId = goalRepository.insertGoal(goal)
            goalRepository.recordTreeHistory(
                TreeHistory(
                    previousState = TreeState.SEED,
                    newState = TreeState.SEED,
                    reason = "New goal planted: $name",
                    contributingGoalId = newId
                )
            )
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.updateGoal(goal)
        }
    }

    fun setGoalStatus(id: Long, status: GoalStatus) {
        viewModelScope.launch {
            goalRepository.setGoalStatus(id, status)
            val actionName = when (status) {
                GoalStatus.ACTIVE -> "resumed"
                GoalStatus.PAUSED -> "paused"
                GoalStatus.COMPLETED -> "completed! 🎉"
                GoalStatus.ARCHIVED -> "archived"
            }
            goalRepository.recordTreeHistory(
                TreeHistory(
                    previousState = TreeState.SEED,
                    newState = TreeState.SEED,
                    reason = "Goal was $actionName",
                    contributingGoalId = id
                )
            )
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            goalRepository.deleteGoal(id)
        }
    }

    class Factory(
        private val goalRepository: GoalRepository,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val accountRepository: AccountRepository,
        private val budgetRepository: BudgetRepository,
        private val settingsRepository: SettingsRepository,
        private val goalCalculationEngine: GoalCalculationEngine,
        private val calculationEngine: FinancialCalculationEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoalsViewModel(
                goalRepository,
                transactionRepository,
                categoryRepository,
                accountRepository,
                budgetRepository,
                settingsRepository,
                goalCalculationEngine,
                calculationEngine
            ) as T
        }
    }
}
