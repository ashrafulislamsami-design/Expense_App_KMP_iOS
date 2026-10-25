package com.expenseapp.salarytracker.domain.security

import kotlinx.coroutines.flow.Flow

/**
 * Storage interface for sensitive security credentials and settings.
 * Ensures values are never persisted in plaintext.
 */
interface SecureStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
    fun clear()

    fun getAppLockMode(): Flow<AppLockMode>
    fun getAppLockModeSync(): AppLockMode
    suspend fun setAppLockMode(mode: AppLockMode)

    fun getLockGracePeriod(): Flow<LockGracePeriod>
    fun getLockGracePeriodSync(): LockGracePeriod
    suspend fun setLockGracePeriod(period: LockGracePeriod)

    fun isScreenshotProtectionEnabled(): Flow<Boolean>
    fun isScreenshotProtectionEnabledSync(): Boolean
    suspend fun setScreenshotProtectionEnabled(enabled: Boolean)

    fun isPasscodeSet(): Boolean
    fun verifyPasscode(passcode: String): Boolean
    fun setPasscode(passcode: String)
    fun clearPasscode()
}
