package com.expenseapp.salarytracker.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.expenseapp.salarytracker.R

object NotificationChannels {
    const val CHANNEL_DAILY_REMINDERS = "channel_daily_reminders"
    const val CHANNEL_BUDGET_ALERTS = "channel_budget_alerts"
    const val CHANNEL_GOAL_UPDATES = "channel_goal_updates"

    /**
     * Creates all notification channels for the app if running on Android 8.0 (API 26) or higher.
     * Safe to call repeatedly as existing channels with identical IDs are not recreated or modified.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // 1. Daily Reminders (DEFAULT importance, gentle prompt)
        val dailyReminderChannel = NotificationChannel(
            CHANNEL_DAILY_REMINDERS,
            context.getString(R.string.notification_channel_daily_reminder_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_daily_reminder_desc)
            enableLights(true)
            enableVibration(true)
        }

        // 2. Budget Alerts (HIGH importance to alert user when cap is reached/exceeded)
        val budgetAlertsChannel = NotificationChannel(
            CHANNEL_BUDGET_ALERTS,
            context.getString(R.string.notification_channel_budget_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_budget_alerts_desc)
            enableLights(true)
            enableVibration(true)
        }

        // 3. Goal Milestones (DEFAULT importance, positive reinforcement)
        val goalUpdatesChannel = NotificationChannel(
            CHANNEL_GOAL_UPDATES,
            context.getString(R.string.notification_channel_goal_updates_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_goal_updates_desc)
            enableLights(true)
            enableVibration(false)
        }

        notificationManager.createNotificationChannels(
            listOf(dailyReminderChannel, budgetAlertsChannel, goalUpdatesChannel)
        )
    }
}
