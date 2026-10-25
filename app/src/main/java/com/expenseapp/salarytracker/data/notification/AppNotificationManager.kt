package com.expenseapp.salarytracker.data.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.expenseapp.salarytracker.R
import com.expenseapp.salarytracker.domain.service.notification.NotificationManagerService
import com.expenseapp.salarytracker.presentation.main.MainActivity

class AppNotificationManager(
    private val context: Context
) : NotificationManagerService {

    companion object {
        const val EXTRA_DESTINATION = "extra_destination"
        const val DEST_ADD_EXPENSE = "dest_add_expense"
        const val DEST_VIEW_TODAY_REPORT = "dest_view_today_report"
        const val DEST_BUDGETS = "dest_budgets"
        const val DEST_GOALS = "dest_goals"

        const val NOTIFICATION_ID_DAILY_REMINDER = 1001
        const val NOTIFICATION_ID_BUDGET_BASE = 2000
        const val NOTIFICATION_ID_GOAL_BASE = 3000
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    /**
     * Checks if notification permission is granted on Android 13+ (API 33).
     */
    override fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Displays the daily expense reminder asking the user if they've recorded today's spending.
     * Includes Quick Add and View Today's Report action buttons.
     */
    override fun showDailyReminderNotification() {
        if (!hasNotificationPermission() || notificationManager == null) return

        NotificationChannels.createChannels(context)

        // Main content intent: opens app
        val contentIntent = createNavigationPendingIntent(DEST_VIEW_TODAY_REPORT, requestCode = 101)

        // Action: Add Expense
        val addExpenseIntent = createNavigationPendingIntent(DEST_ADD_EXPENSE, requestCode = 102)
        val addExpenseAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_input_add,
            context.getString(R.string.daily_reminder_action_add),
            addExpenseIntent
        ).build()

        // Action: View Today's Report
        val viewReportIntent = createNavigationPendingIntent(DEST_VIEW_TODAY_REPORT, requestCode = 103)
        val viewReportAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_today,
            context.getString(R.string.daily_reminder_action_view_report),
            viewReportIntent
        ).build()

        // Public version for secure lock-screen (zero financial leakage)
        val publicNotification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_DAILY_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.daily_reminder_public_title))
            .setContentText(context.getString(R.string.daily_reminder_public_body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_DAILY_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.daily_reminder_title))
            .setContentText(context.getString(R.string.daily_reminder_body))
            .setContentIntent(contentIntent)
            .addAction(addExpenseAction)
            .addAction(viewReportAction)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID_DAILY_REMINDER, notification)
    }

    /**
     * Displays a budget alert when spending approaches or exceeds a limit.
     */
    override fun showBudgetAlertNotification(
        budgetId: Long,
        budgetName: String,
        percentage: Int,
        isExceeded: Boolean
    ) {
        if (!hasNotificationPermission() || notificationManager == null) return

        NotificationChannels.createChannels(context)

        val contentIntent = createNavigationPendingIntent(DEST_BUDGETS, requestCode = (200 + budgetId).toInt())
        val viewAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            context.getString(R.string.budget_alert_action_view),
            contentIntent
        ).build()

        val title = if (isExceeded) {
            context.getString(R.string.budget_alert_exceeded_title)
        } else {
            context.getString(R.string.budget_alert_warning_title)
        }

        val body = if (isExceeded) {
            context.getString(R.string.budget_alert_exceeded_body, budgetName)
        } else {
            context.getString(R.string.budget_alert_warning_body, percentage, budgetName)
        }

        // Public version for lockscreen (masks budget details)
        val publicNotification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.budget_alert_public_title))
            .setContentText(context.getString(R.string.budget_alert_public_body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .addAction(viewAction)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationId = NOTIFICATION_ID_BUDGET_BASE + (budgetId % 1000).toInt()
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Displays a goal milestone achievement notification.
     */
    override fun showGoalMilestoneNotification(
        goalId: Long,
        goalName: String,
        milestonePercentage: Int
    ) {
        if (!hasNotificationPermission() || notificationManager == null) return

        NotificationChannels.createChannels(context)

        val contentIntent = createNavigationPendingIntent(DEST_GOALS, requestCode = (300 + goalId).toInt())
        val viewAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            context.getString(R.string.goal_action_view),
            contentIntent
        ).build()

        val title = context.getString(R.string.goal_milestone_title)
        val body = context.getString(R.string.goal_milestone_body, milestonePercentage, goalName)

        val publicNotification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_GOAL_UPDATES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.goal_milestone_public_title))
            .setContentText(context.getString(R.string.goal_milestone_public_body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_GOAL_UPDATES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .addAction(viewAction)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationId = NOTIFICATION_ID_GOAL_BASE + (goalId % 1000).toInt()
        notificationManager.notify(notificationId, notification)
    }

    override fun cancelDailyReminder() {
        notificationManager?.cancel(NOTIFICATION_ID_DAILY_REMINDER)
    }

    private fun createNavigationPendingIntent(destination: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, destination)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
}
