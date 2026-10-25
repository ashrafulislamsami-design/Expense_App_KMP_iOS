package com.expenseapp.salarytracker.data.repository

import com.expenseapp.salarytracker.data.dao.UserSettingDao
import com.expenseapp.salarytracker.data.entity.UserSettingEntity
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.ReportingCycleMode
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.SalaryFrequency
import com.expenseapp.salarytracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class SettingsRepositoryImpl(
    private val dao: UserSettingDao
) : SettingsRepository {

    override fun getSalaryCycleConfig(): Flow<SalaryCycleConfig> {
        return dao.getAllSettings().map { list ->
            val map = list.associate { it.key to it.value }
            val amountMinor = map["salary_amount"]?.toLongOrNull() ?: 12000000L // default ৳120,000.00
            val currency = map["currency"] ?: "BDT"
            val salaryDay = map["salary_day"]?.toIntOrNull() ?: 1
            val frequency = map["salary_frequency"]?.let { SalaryFrequency.fromString(it) } ?: SalaryFrequency.MONTHLY
            val reportingMode = map["reporting_mode"]?.let { ReportingCycleMode.fromString(it) } ?: ReportingCycleMode.SALARY_CYCLE

            SalaryCycleConfig(
                salaryAmount = MinorUnits(amountMinor),
                currency = currency,
                salaryDayOfMonth = salaryDay,
                frequency = frequency,
                reportingMode = reportingMode,
                anchorDate = LocalDate.now()
            )
        }
    }

    override suspend fun updateSalaryCycleConfig(config: SalaryCycleConfig) {
        dao.setSetting(UserSettingEntity("salary_amount", config.salaryAmount.amountInMinorUnits.toString()))
        dao.setSetting(UserSettingEntity("currency", config.currency))
        dao.setSetting(UserSettingEntity("salary_day", config.salaryDayOfMonth.toString()))
        dao.setSetting(UserSettingEntity("salary_frequency", config.frequency.name))
        dao.setSetting(UserSettingEntity("reporting_mode", config.reportingMode.name))
    }

    override fun getCurrency(): Flow<String> =
        dao.getSetting("currency").map { it ?: "BDT" }

    override suspend fun setCurrency(currency: String) {
        dao.setSetting(UserSettingEntity("currency", currency))
        val symbol = when (currency.uppercase()) {
            "BDT" -> "৳"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "INR" -> "₹"
            else -> currency
        }
        dao.setSetting(UserSettingEntity("currency_symbol", symbol))
    }

    override fun isBiometricEnabled(): Flow<Boolean> =
        dao.getSetting("biometric_enabled").map { it == "true" }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dao.setSetting(UserSettingEntity("biometric_enabled", enabled.toString()))
    }

    override fun isDailyReminderEnabled(): Flow<Boolean> =
        dao.getSetting("daily_reminder_enabled").map { it == "true" }

    override suspend fun setDailyReminderEnabled(enabled: Boolean) {
        dao.setSetting(UserSettingEntity("daily_reminder_enabled", enabled.toString()))
    }

    override fun getDailyReminderTime(): Flow<String> =
        dao.getSetting("daily_reminder_time").map { it ?: "23:00" }

    override suspend fun setDailyReminderTime(timeString: String) {
        dao.setSetting(UserSettingEntity("daily_reminder_time", timeString))
    }

    override fun getDailyReminderDays(): Flow<Set<java.time.DayOfWeek>> =
        dao.getSetting("daily_reminder_days").map { str ->
            if (str.isNullOrBlank()) {
                java.time.DayOfWeek.entries.toSet()
            } else {
                val parsed = str.split(",").mapNotNull { name ->
                    runCatching { java.time.DayOfWeek.valueOf(name.trim()) }.getOrNull()
                }.toSet()
                if (parsed.isEmpty()) java.time.DayOfWeek.entries.toSet() else parsed
            }
        }

    override suspend fun setDailyReminderDays(days: Set<java.time.DayOfWeek>) {
        val str = days.joinToString(",") { it.name }
        dao.setSetting(UserSettingEntity("daily_reminder_days", str))
    }

    override fun isBudgetAlertsEnabled(): Flow<Boolean> =
        dao.getSetting("budget_alerts_enabled").map { it == null || it == "true" }

    override suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        dao.setSetting(UserSettingEntity("budget_alerts_enabled", enabled.toString()))
    }

    override fun isGoalNotificationsEnabled(): Flow<Boolean> =
        dao.getSetting("goal_notifications_enabled").map { it == null || it == "true" }

    override suspend fun setGoalNotificationsEnabled(enabled: Boolean) {
        dao.setSetting(UserSettingEntity("goal_notifications_enabled", enabled.toString()))
    }

    override fun hasCompletedOnboarding(): Flow<Boolean> =
        dao.getSetting("has_completed_onboarding").map { it == "true" }

    override suspend fun setCompletedOnboarding(completed: Boolean) {
        dao.setSetting(UserSettingEntity("has_completed_onboarding", completed.toString()))
    }
}
