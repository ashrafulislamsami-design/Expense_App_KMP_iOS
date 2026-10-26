package com.expenseapp.salarytracker.domain.repository

import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface SettingsRepository {
    fun getSalaryCycleConfig(): Flow<SalaryCycleConfig>
    suspend fun updateSalaryCycleConfig(config: SalaryCycleConfig)
    fun getCurrency(): Flow<String>
    suspend fun setCurrency(currency: String)
    fun isBiometricEnabled(): Flow<Boolean>
    suspend fun setBiometricEnabled(enabled: Boolean)
    fun isDailyReminderEnabled(): Flow<Boolean>
    suspend fun setDailyReminderEnabled(enabled: Boolean)
    fun getDailyReminderTime(): Flow<String>
    suspend fun setDailyReminderTime(timeString: String)
    fun getDailyReminderDays(): Flow<Set<java.time.DayOfWeek>>
    suspend fun setDailyReminderDays(days: Set<java.time.DayOfWeek>)
    fun isBudgetAlertsEnabled(): Flow<Boolean>
    suspend fun setBudgetAlertsEnabled(enabled: Boolean)
    fun isGoalNotificationsEnabled(): Flow<Boolean>
    suspend fun setGoalNotificationsEnabled(enabled: Boolean)
    fun hasCompletedOnboarding(): Flow<Boolean>
    suspend fun setCompletedOnboarding(completed: Boolean)

    // User Profile
    fun getUserName(): Flow<String> = flowOf("User")
    suspend fun setUserName(name: String) {}
    fun getUserAvatar(): Flow<String> = flowOf("avatar_1")
    suspend fun setUserAvatar(avatar: String) {}
}
