package com.expenseapp.salarytracker.domain.service.notification

import com.expenseapp.salarytracker.data.dao.UserSettingDao
import com.expenseapp.salarytracker.data.entity.UserSettingEntity
import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.AccountType
import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.model.BudgetPeriod
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.repository.BudgetRepository
import com.expenseapp.salarytracker.domain.repository.CategoryRepository
import com.expenseapp.salarytracker.domain.repository.SettingsRepository
import com.expenseapp.salarytracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class BudgetNotificationServiceTest {

    private lateinit var fakeBudgetRepo: FakeBudgetRepo
    private lateinit var fakeTxRepo: FakeTxRepo
    private lateinit var fakeCategoryRepo: FakeCategoryRepo
    private lateinit var fakeSettingsRepo: FakeSettingsRepo
    private lateinit var fakeUserSettingDao: FakeUserSettingDao
    private lateinit var fakeNotificationManager: FakeNotificationManager

    private lateinit var service: BudgetNotificationService

    @Before
    fun setUp() {
        fakeBudgetRepo = FakeBudgetRepo()
        fakeTxRepo = FakeTxRepo()
        fakeCategoryRepo = FakeCategoryRepo()
        fakeSettingsRepo = FakeSettingsRepo()
        fakeUserSettingDao = FakeUserSettingDao()
        fakeNotificationManager = FakeNotificationManager()

        service = BudgetNotificationService(
            budgetRepository = fakeBudgetRepo,
            transactionRepository = fakeTxRepo,
            categoryRepository = fakeCategoryRepo,
            settingsRepository = fakeSettingsRepo,
            userSettingDao = fakeUserSettingDao,
            notificationManager = fakeNotificationManager
        )
    }

    @Test
    fun testAlertsDisabled_noNotificationSent() = runTest {
        fakeSettingsRepo.budgetAlertsEnabled.value = false

        val budget = Budget(
            id = 1L,
            categoryId = null,
            limitAmount = MinorUnits(100000L), // 1000.00
            period = BudgetPeriod.MONTHLY,
            warningThresholdPercentage = 80
        )
        fakeBudgetRepo.budgets.value = listOf(budget)

        // Spending 1200.00 (exceeded)
        val tx = Transaction(
            id = 1L,
            amount = MinorUnits(120000L),
            type = TransactionType.EXPENSE,
            accountId = 1L,
            date = LocalDate.of(2026, 3, 15)
        )
        fakeTxRepo.transactions.value = listOf(tx)

        service.evaluateBudgets(LocalDate.of(2026, 3, 15))

        assertEquals(0, fakeNotificationManager.budgetAlerts.size)
    }

    @Test
    fun testWarningThreshold_sendsWarningAndDeduplicates() = runTest {
        fakeSettingsRepo.budgetAlertsEnabled.value = true

        val budget = Budget(
            id = 1L,
            categoryId = 10L,
            limitAmount = MinorUnits(100000L), // 1000.00
            period = BudgetPeriod.MONTHLY,
            warningThresholdPercentage = 80
        )
        fakeBudgetRepo.budgets.value = listOf(budget)
        fakeCategoryRepo.categories.value = listOf(
            Category(id = 10L, name = "Dining", iconName = "food", colorHex = "#3B82F6")
        )

        // Spend 850.00 -> 85%
        fakeTxRepo.transactions.value = listOf(
            Transaction(
                id = 1L,
                amount = MinorUnits(85000L),
                type = TransactionType.EXPENSE,
                accountId = 1L,
                categoryId = 10L,
                date = LocalDate.of(2026, 3, 15)
            )
        )

        service.evaluateBudgets(LocalDate.of(2026, 3, 15))

        assertEquals(1, fakeNotificationManager.budgetAlerts.size)
        val alert = fakeNotificationManager.budgetAlerts[0]
        assertEquals(1L, alert.budgetId)
        assertEquals("Dining", alert.budgetName)
        assertEquals(85, alert.percentage)
        assertFalse(alert.isExceeded)

        // Second evaluation with identical state should NOT notify again
        service.evaluateBudgets(LocalDate.of(2026, 3, 15))
        assertEquals(1, fakeNotificationManager.budgetAlerts.size)
    }

    @Test
    fun testExceededThreshold_sendsExceededAlert() = runTest {
        fakeSettingsRepo.budgetAlertsEnabled.value = true

        val budget = Budget(
            id = 2L,
            categoryId = null,
            limitAmount = MinorUnits(500000L), // 5000.00
            period = BudgetPeriod.MONTHLY,
            warningThresholdPercentage = 80
        )
        fakeBudgetRepo.budgets.value = listOf(budget)

        // Spend 5500.00 -> 110%
        fakeTxRepo.transactions.value = listOf(
            Transaction(
                id = 1L,
                amount = MinorUnits(550000L),
                type = TransactionType.EXPENSE,
                accountId = 1L,
                date = LocalDate.of(2026, 3, 20)
            )
        )

        service.evaluateBudgets(LocalDate.of(2026, 3, 20))

        assertEquals(1, fakeNotificationManager.budgetAlerts.size)
        val alert = fakeNotificationManager.budgetAlerts[0]
        assertEquals(2L, alert.budgetId)
        assertEquals("Overall Monthly", alert.budgetName)
        assertTrue(alert.isExceeded)

        // Repeated run deduplicated
        service.evaluateBudgets(LocalDate.of(2026, 3, 20))
        assertEquals(1, fakeNotificationManager.budgetAlerts.size)
    }

    @Test
    fun testTransfersDoNotTriggerBudgetWarning() = runTest {
        fakeSettingsRepo.budgetAlertsEnabled.value = true

        val budget = Budget(
            id = 3L,
            categoryId = null,
            limitAmount = MinorUnits(100000L), // 1000.00
            period = BudgetPeriod.MONTHLY,
            warningThresholdPercentage = 80
        )
        fakeBudgetRepo.budgets.value = listOf(budget)

        // Only transfers - transfers must NEVER count as expense
        fakeTxRepo.transactions.value = listOf(
            Transaction(
                id = 1L,
                amount = MinorUnits(500000L), // 5000.00 transfer
                type = TransactionType.TRANSFER,
                accountId = 1L,
                destinationAccountId = 2L,
                date = LocalDate.of(2026, 3, 10)
            )
        )

        service.evaluateBudgets(LocalDate.of(2026, 3, 15))

        assertEquals(0, fakeNotificationManager.budgetAlerts.size)
    }

    @Test
    fun testRefundsReduceSpendingBelowThreshold() = runTest {
        fakeSettingsRepo.budgetAlertsEnabled.value = true

        val budget = Budget(
            id = 4L,
            categoryId = null,
            limitAmount = MinorUnits(100000L), // 1000.00
            period = BudgetPeriod.MONTHLY,
            warningThresholdPercentage = 80
        )
        fakeBudgetRepo.budgets.value = listOf(budget)

        // Expense 900.00, Refund 200.00 -> Net 700.00 (70% < 80%)
        fakeTxRepo.transactions.value = listOf(
            Transaction(
                id = 1L,
                amount = MinorUnits(90000L),
                type = TransactionType.EXPENSE,
                accountId = 1L,
                date = LocalDate.of(2026, 3, 10)
            ),
            Transaction(
                id = 2L,
                amount = MinorUnits(20000L),
                type = TransactionType.REFUND,
                accountId = 1L,
                date = LocalDate.of(2026, 3, 11)
            )
        )

        service.evaluateBudgets(LocalDate.of(2026, 3, 15))

        // Net 70% is below 80% warning threshold, no notification
        assertEquals(0, fakeNotificationManager.budgetAlerts.size)
    }

    // --- Fakes ---

    private class FakeNotificationManager : NotificationManagerService {
        data class Alert(val budgetId: Long, val budgetName: String, val percentage: Int, val isExceeded: Boolean)

        val budgetAlerts = mutableListOf<Alert>()
        val goalAlerts = mutableListOf<Triple<Long, String, Int>>()
        var dailyReminderShown = false

        override fun hasNotificationPermission(): Boolean = true
        override fun showDailyReminderNotification() { dailyReminderShown = true }
        override fun showBudgetAlertNotification(budgetId: Long, budgetName: String, percentage: Int, isExceeded: Boolean) {
            budgetAlerts.add(Alert(budgetId, budgetName, percentage, isExceeded))
        }
        override fun showGoalMilestoneNotification(goalId: Long, goalName: String, milestonePercentage: Int) {
            goalAlerts.add(Triple(goalId, goalName, milestonePercentage))
        }
        override fun cancelDailyReminder() {}
    }

    private class FakeUserSettingDao : UserSettingDao {
        private val settings = mutableMapOf<String, String>()

        override fun getAllSettings(): Flow<List<UserSettingEntity>> = flowOf(settings.map { UserSettingEntity(it.key, it.value) })
        override fun getSetting(key: String): Flow<String?> = flowOf(settings[key])
        override suspend fun getSettingDirect(key: String): String? = settings[key]
        override suspend fun setSetting(setting: UserSettingEntity) { settings[setting.key] = setting.value }
    }

    private class FakeSettingsRepo : SettingsRepository {
        val budgetAlertsEnabled = MutableStateFlow(true)
        val goalNotificationsEnabled = MutableStateFlow(true)

        override fun getSalaryCycleConfig(): Flow<SalaryCycleConfig> = flowOf(SalaryCycleConfig())
        override suspend fun updateSalaryCycleConfig(config: SalaryCycleConfig) {}
        override fun getCurrency(): Flow<String> = flowOf("BDT")
        override suspend fun setCurrency(currency: String) {}
        override fun isBiometricEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setBiometricEnabled(enabled: Boolean) {}
        override fun isDailyReminderEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setDailyReminderEnabled(enabled: Boolean) {}
        override fun getDailyReminderTime(): Flow<String> = flowOf("23:00")
        override suspend fun setDailyReminderTime(timeString: String) {}
        override fun getDailyReminderDays(): Flow<Set<DayOfWeek>> = flowOf(DayOfWeek.values().toSet())
        override suspend fun setDailyReminderDays(days: Set<DayOfWeek>) {}
        override fun isBudgetAlertsEnabled(): Flow<Boolean> = budgetAlertsEnabled.asStateFlow()
        override suspend fun setBudgetAlertsEnabled(enabled: Boolean) { budgetAlertsEnabled.value = enabled }
        override fun isGoalNotificationsEnabled(): Flow<Boolean> = goalNotificationsEnabled.asStateFlow()
        override suspend fun setGoalNotificationsEnabled(enabled: Boolean) { goalNotificationsEnabled.value = enabled }
        override fun hasCompletedOnboarding(): Flow<Boolean> = flowOf(true)
        override suspend fun setCompletedOnboarding(completed: Boolean) {}
    }

    private class FakeBudgetRepo : BudgetRepository {
        val budgets = MutableStateFlow<List<Budget>>(emptyList())

        override fun getAllBudgets(): Flow<List<Budget>> = budgets.asStateFlow()
        override fun getOverallBudget(): Flow<Budget?> = flowOf(budgets.value.find { it.categoryId == null })
        override suspend fun getBudgetById(id: Long): Budget? = budgets.value.find { it.id == id }
        override suspend fun insertOrUpdateBudget(budget: Budget): Long = 1L
        override suspend fun deleteBudget(id: Long) {}
    }

    private class FakeTxRepo : TransactionRepository {
        val transactions = MutableStateFlow<List<Transaction>>(emptyList())

        override fun getAllTransactions(): Flow<List<Transaction>> = transactions.asStateFlow()
        override fun getTransactionsBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>> =
            flowOf(transactions.value.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) })
        override fun getTransactionsForAccount(accountId: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun getTransactionById(id: Long): Transaction? = transactions.value.find { it.id == id }
        override suspend fun insertTransaction(transaction: Transaction): Long = 1L
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}
    }

    private class FakeCategoryRepo : CategoryRepository {
        val categories = MutableStateFlow<List<Category>>(emptyList())

        override fun getAllCategories(): Flow<List<Category>> = categories.asStateFlow()
        override fun getActiveCategories(): Flow<List<Category>> = flowOf(categories.value.filter { !it.isArchived })
        override suspend fun getCategoryById(id: Long): Category? = categories.value.find { it.id == id }
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun archiveCategory(id: Long) {}
    }
}
