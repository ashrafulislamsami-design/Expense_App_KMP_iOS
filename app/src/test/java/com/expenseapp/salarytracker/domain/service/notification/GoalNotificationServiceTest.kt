package com.expenseapp.salarytracker.domain.service.notification

import com.expenseapp.salarytracker.data.dao.UserSettingDao
import com.expenseapp.salarytracker.data.entity.UserSettingEntity
import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.model.GoalStatus
import com.expenseapp.salarytracker.domain.model.GoalType
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.TreeHistory
import com.expenseapp.salarytracker.domain.model.TreeState
import com.expenseapp.salarytracker.domain.repository.GoalRepository
import com.expenseapp.salarytracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class GoalNotificationServiceTest {

    private lateinit var fakeGoalRepo: FakeGoalRepo
    private lateinit var fakeSettingsRepo: FakeSettingsRepo
    private lateinit var fakeUserSettingDao: FakeUserSettingDao
    private lateinit var fakeNotificationManager: FakeNotificationManager

    private lateinit var service: GoalNotificationService

    @Before
    fun setUp() {
        fakeGoalRepo = FakeGoalRepo()
        fakeSettingsRepo = FakeSettingsRepo()
        fakeUserSettingDao = FakeUserSettingDao()
        fakeNotificationManager = FakeNotificationManager()

        service = GoalNotificationService(
            goalRepository = fakeGoalRepo,
            settingsRepository = fakeSettingsRepo,
            userSettingDao = fakeUserSettingDao,
            notificationManager = fakeNotificationManager
        )
    }

    @Test
    fun testGoalNotificationsDisabled_noNotification() = runTest {
        fakeSettingsRepo.goalNotificationsEnabled.value = false

        val goal = Goal(
            id = 1L,
            name = "Emergency Fund",
            targetAmount = MinorUnits(1000000L),
            type = GoalType.SAVE_AMOUNT,
            status = GoalStatus.ACTIVE,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusMonths(6),
            reachedMilestones = listOf(25, 50)
        )

        service.checkAndNotifyMilestones(goal)

        assertEquals(0, fakeNotificationManager.milestoneAlerts.size)
    }

    @Test
    fun testMilestonesNotifiedAndDeduplicated() = runTest {
        fakeSettingsRepo.goalNotificationsEnabled.value = true

        val goal = Goal(
            id = 5L,
            name = "New Laptop",
            targetAmount = MinorUnits(15000000L),
            type = GoalType.SAVE_AMOUNT,
            status = GoalStatus.ACTIVE,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusMonths(12),
            reachedMilestones = listOf(25, 50)
        )

        // First run: should notify for both 25% and 50%
        service.checkAndNotifyMilestones(goal)

        assertEquals(2, fakeNotificationManager.milestoneAlerts.size)
        assertEquals(25, fakeNotificationManager.milestoneAlerts[0].third)
        assertEquals(50, fakeNotificationManager.milestoneAlerts[1].third)
        assertEquals("New Laptop", fakeNotificationManager.milestoneAlerts[0].second)

        // Second run: deduplicated, no new notifications
        service.checkAndNotifyMilestones(goal)
        assertEquals(2, fakeNotificationManager.milestoneAlerts.size)

        // Reaching 75%: only 75% should be notified
        val updatedGoal = goal.copy(reachedMilestones = listOf(25, 50, 75))
        service.checkAndNotifyMilestones(updatedGoal)
        assertEquals(3, fakeNotificationManager.milestoneAlerts.size)
        assertEquals(75, fakeNotificationManager.milestoneAlerts[2].third)
    }

    // --- Fakes ---

    private class FakeNotificationManager : NotificationManagerService {
        val milestoneAlerts = mutableListOf<Triple<Long, String, Int>>()

        override fun hasNotificationPermission(): Boolean = true
        override fun showDailyReminderNotification() {}
        override fun showBudgetAlertNotification(budgetId: Long, budgetName: String, percentage: Int, isExceeded: Boolean) {}
        override fun showGoalMilestoneNotification(goalId: Long, goalName: String, milestonePercentage: Int) {
            milestoneAlerts.add(Triple(goalId, goalName, milestonePercentage))
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
        override fun isBudgetAlertsEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setBudgetAlertsEnabled(enabled: Boolean) {}
        override fun isGoalNotificationsEnabled(): Flow<Boolean> = goalNotificationsEnabled.asStateFlow()
        override suspend fun setGoalNotificationsEnabled(enabled: Boolean) { goalNotificationsEnabled.value = enabled }
        override fun hasCompletedOnboarding(): Flow<Boolean> = flowOf(true)
        override suspend fun setCompletedOnboarding(completed: Boolean) {}
    }

    private class FakeGoalRepo : GoalRepository {
        val goals = MutableStateFlow<List<Goal>>(emptyList())

        override fun getAllGoals(): Flow<List<Goal>> = goals.asStateFlow()
        override fun getActiveGoals(): Flow<List<Goal>> = flowOf(emptyList())
        override fun getGoalsByStatus(status: GoalStatus): Flow<List<Goal>> = flowOf(emptyList())
        override suspend fun getGoalById(id: Long): Goal? = null
        override suspend fun insertGoal(goal: Goal): Long = 1L
        override suspend fun updateGoal(goal: Goal) {}
        override suspend fun setGoalStatus(id: Long, status: GoalStatus) {}
        override suspend fun updateMilestones(id: Long, milestones: List<Int>) {}
        override suspend fun updateTreeState(id: Long, treeState: TreeState) {}
        override suspend fun deleteGoal(id: Long) {}
        override fun getTreeHistory(limit: Int): Flow<List<TreeHistory>> = flowOf(emptyList())
        override suspend fun recordTreeHistory(event: TreeHistory): Long = 1L
    }
}
