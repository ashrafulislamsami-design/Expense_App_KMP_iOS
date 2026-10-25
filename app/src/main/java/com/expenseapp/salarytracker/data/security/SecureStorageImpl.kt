package com.expenseapp.salarytracker.data.security

import android.content.Context
import android.content.SharedPreferences
import com.expenseapp.salarytracker.domain.security.AppLockMode
import com.expenseapp.salarytracker.domain.security.EncryptedPayload
import com.expenseapp.salarytracker.domain.security.EncryptionService
import com.expenseapp.salarytracker.domain.security.LockGracePeriod
import com.expenseapp.salarytracker.domain.security.SecureStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Secure persistent storage for security configuration and secrets.
 *
 * Values stored securely:
 * - App lock mode
 * - Grace period
 * - Screenshot protection flag
 * - Passcode PBKDF2 verifier hash
 * - Arbitrary encrypted string values
 */
class SecureStorageImpl(
    private val context: Context,
    private val encryptionService: EncryptionService,
    private val passcodeKdfService: PasscodeKdfService = PasscodeKdfService(),
    prefName: String = "expense_app_secure_prefs"
) : SecureStorage {

    private val prefs: SharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

    companion object {
        private const val KEY_APP_LOCK_MODE = "sec_app_lock_mode"
        private const val KEY_LOCK_GRACE_PERIOD = "sec_lock_grace_period"
        private const val KEY_SCREENSHOT_PROTECTION = "sec_screenshot_protection"
        private const val KEY_PASSCODE_VERIFIER = "sec_passcode_verifier"
    }

    override fun getString(key: String): String? {
        val encryptedB64 = prefs.getString(key, null) ?: return null
        return try {
            val bytes = Base64.getDecoder().decode(encryptedB64)
            val payload = EncryptedPayload.fromByteArray(bytes)
            val decryptedBytes = encryptionService.decrypt(payload)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    override fun putString(key: String, value: String) {
        val payload = encryptionService.encrypt(value.toByteArray(StandardCharsets.UTF_8))
        val combinedBytes = payload.toByteArray()
        val encryptedB64 = Base64.getEncoder().encodeToString(combinedBytes)
        prefs.edit().putString(key, encryptedB64).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    override fun getAppLockMode(): Flow<AppLockMode> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_APP_LOCK_MODE || key == null) {
                val modeStr = prefs.getString(KEY_APP_LOCK_MODE, AppLockMode.DISABLED.name) ?: AppLockMode.DISABLED.name
                trySend(AppLockMode.fromString(modeStr))
            }
        }
        val current = prefs.getString(KEY_APP_LOCK_MODE, AppLockMode.DISABLED.name) ?: AppLockMode.DISABLED.name
        trySend(AppLockMode.fromString(current))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun getAppLockModeSync(): AppLockMode {
        val modeStr = prefs.getString(KEY_APP_LOCK_MODE, AppLockMode.DISABLED.name) ?: AppLockMode.DISABLED.name
        return AppLockMode.fromString(modeStr)
    }

    override suspend fun setAppLockMode(mode: AppLockMode) {
        prefs.edit().putString(KEY_APP_LOCK_MODE, mode.name).apply()
    }

    override fun getLockGracePeriod(): Flow<LockGracePeriod> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LOCK_GRACE_PERIOD || key == null) {
                val periodStr = prefs.getString(KEY_LOCK_GRACE_PERIOD, LockGracePeriod.IMMEDIATELY.name) ?: LockGracePeriod.IMMEDIATELY.name
                trySend(LockGracePeriod.fromString(periodStr))
            }
        }
        val current = prefs.getString(KEY_LOCK_GRACE_PERIOD, LockGracePeriod.IMMEDIATELY.name) ?: LockGracePeriod.IMMEDIATELY.name
        trySend(LockGracePeriod.fromString(current))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun getLockGracePeriodSync(): LockGracePeriod {
        val periodStr = prefs.getString(KEY_LOCK_GRACE_PERIOD, LockGracePeriod.IMMEDIATELY.name) ?: LockGracePeriod.IMMEDIATELY.name
        return LockGracePeriod.fromString(periodStr)
    }

    override suspend fun setLockGracePeriod(period: LockGracePeriod) {
        prefs.edit().putString(KEY_LOCK_GRACE_PERIOD, period.name).apply()
    }

    override fun isScreenshotProtectionEnabled(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SCREENSHOT_PROTECTION || key == null) {
                trySend(prefs.getBoolean(KEY_SCREENSHOT_PROTECTION, true)) // Default true for privacy
            }
        }
        trySend(prefs.getBoolean(KEY_SCREENSHOT_PROTECTION, true))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun isScreenshotProtectionEnabledSync(): Boolean {
        return prefs.getBoolean(KEY_SCREENSHOT_PROTECTION, true)
    }

    override suspend fun setScreenshotProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREENSHOT_PROTECTION, enabled).apply()
    }

    override fun isPasscodeSet(): Boolean {
        return prefs.contains(KEY_PASSCODE_VERIFIER)
    }

    override fun verifyPasscode(passcode: String): Boolean {
        val storedVerifier = prefs.getString(KEY_PASSCODE_VERIFIER, null) ?: return false
        return passcodeKdfService.verifyPasscode(passcode.toCharArray(), storedVerifier)
    }

    override fun setPasscode(passcode: String) {
        val verifier = passcodeKdfService.hashPasscode(passcode.toCharArray())
        prefs.edit().putString(KEY_PASSCODE_VERIFIER, verifier).apply()
    }

    override fun clearPasscode() {
        prefs.edit().remove(KEY_PASSCODE_VERIFIER).apply()
    }
}
