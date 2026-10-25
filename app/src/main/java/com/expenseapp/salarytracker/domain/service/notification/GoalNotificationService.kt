package com.expenseapp.salarytracker.domain.service.notification

import com.expenseapp.salarytracker.data.dao.UserSettingDao
import com.expenseapp.salarytracker.data.entity.UserSettingEntity
import com.expenseapp.salarytracker.data.notification.AppNotificationManager
import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.repository.GoalRepository
import com.expenseapp.salarytracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class GoalNotificationService(
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val userSettingDao: UserSettingDao,
    private val notificationManager: NotificationManagerService
) {

    /**
     * Evaluates new milestones for the given goal and posts a notification for any
     * milestone that has not yet been notified.
     */
    suspend fun checkAndNotifyMilestones(goal: Goal) {
        val enabled = settingsRepository.isGoalNotificationsEnabled().first()
        if (!enabled) return

        for (milestone in goal.reachedMilestones) {
            val dedupKey = "goal_notified_${goal.id}_milestone_$milestone"
            val alreadyNotified = userSettingDao.getSettingDirect(dedupKey) != null
            if (!alreadyNotified) {
                notificationManager.showGoalMilestoneNotification(
                    goalId = goal.id,
                    goalName = goal.name,
                    milestonePercentage = milestone
                )
                userSettingDao.setSetting(UserSettingEntity(dedupKey, "true"))
            }
        }
    }
}
