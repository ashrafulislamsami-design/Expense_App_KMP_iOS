package com.expenseapp.salarytracker.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.expenseapp.salarytracker.ExpenseApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val app = context.applicationContext as? ExpenseApplication ?: return
            app.applicationScope.launch {
                val settingsRepo = app.settingsRepository
                val enabled = settingsRepo.isDailyReminderEnabled().first()
                val timeString = settingsRepo.getDailyReminderTime().first()
                val days = settingsRepo.getDailyReminderDays().first()

                DailyReminderScheduler.schedule(
                    context = context,
                    enabled = enabled,
                    timeString = timeString,
                    activeDays = days
                )
            }
        }
    }
}
