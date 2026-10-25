package com.expenseapp.salarytracker.domain.service.notification

/**
 * Interface abstracting notification posting capabilities.
 * Allows pure unit testing of domain notification services without Android framework dependencies.
 */
interface NotificationManagerService {
    fun hasNotificationPermission(): Boolean
    fun showDailyReminderNotification()
    fun showBudgetAlertNotification(
        budgetId: Long,
        budgetName: String,
        percentage: Int,
        isExceeded: Boolean
    )
    fun showGoalMilestoneNotification(
        goalId: Long,
        goalName: String,
        milestonePercentage: Int
    )
    fun cancelDailyReminder()
}
