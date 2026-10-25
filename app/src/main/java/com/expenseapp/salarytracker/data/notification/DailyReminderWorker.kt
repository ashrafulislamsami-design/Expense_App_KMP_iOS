package com.expenseapp.salarytracker.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expenseapp.salarytracker.ExpenseApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ExpenseApplication
            ?: return Result.failure()

        val settingsRepo = app.settingsRepository
        val isEnabled = settingsRepo.isDailyReminderEnabled().first()
        val timeString = settingsRepo.getDailyReminderTime().first()
        val activeDays = settingsRepo.getDailyReminderDays().first()

        val today = LocalDate.now().dayOfWeek
        if (isEnabled && activeDays.contains(today)) {
            val notificationManager = AppNotificationManager(applicationContext)
            notificationManager.showDailyReminderNotification()
        }

        // Reschedule next occurrence
        DailyReminderScheduler.schedule(
            context = applicationContext,
            enabled = isEnabled,
            timeString = timeString,
            activeDays = activeDays
        )

        return Result.success()
    }
}
