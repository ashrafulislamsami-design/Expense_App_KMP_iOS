package com.expenseapp.salarytracker.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expenseapp.salarytracker.domain.security.AppLockMode
import com.expenseapp.salarytracker.domain.security.LockGracePeriod
import com.expenseapp.salarytracker.domain.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecurityUiState(
    val lockMode: AppLockMode = AppLockMode.DISABLED,
    val gracePeriod: LockGracePeriod = LockGracePeriod.IMMEDIATELY,
    val isScreenshotProtectionEnabled: Boolean = true,
    val isPasscodeConfigured: Boolean = false,
    val isLocked: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Manages runtime authentication state, lock lifecycle, and grace period verification.
 */
class SecurityViewModel(
    private val secureStorage: SecureStorage,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val initialLockMode = secureStorage.getAppLockModeSync()
    private val initialGracePeriod = secureStorage.getLockGracePeriodSync()
    private val initialScreenshotProtection = secureStorage.isScreenshotProtectionEnabledSync()
    private val initialHasPasscode = secureStorage.isPasscodeSet()

    private val _uiState = MutableStateFlow(
        SecurityUiState(
            lockMode = initialLockMode,
            gracePeriod = initialGracePeriod,
            isScreenshotProtectionEnabled = initialScreenshotProtection,
            isPasscodeConfigured = initialHasPasscode,
            isLocked = initialLockMode != AppLockMode.DISABLED && !isUnlockedThisSession,
            isBiometricAvailable = biometricAuthManager.checkBiometricStatus() is BiometricAuthManager.BiometricStatus.Ready
        )
    )
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private var backgroundTimestamp: Long = 0L

    init {
        observeSecuritySettings()
    }

    private fun observeSecuritySettings() {
        viewModelScope.launch {
            secureStorage.getAppLockMode().collect { mode ->
                _uiState.update {
                    it.copy(
                        lockMode = mode,
                        isLocked = mode != AppLockMode.DISABLED && !isUnlockedThisSession
                    )
                }
            }
        }
        viewModelScope.launch {
            secureStorage.getLockGracePeriod().collect { period ->
                _uiState.update { it.copy(gracePeriod = period) }
            }
        }
        viewModelScope.launch {
            secureStorage.isScreenshotProtectionEnabled().collect { enabled ->
                _uiState.update { it.copy(isScreenshotProtectionEnabled = enabled) }
            }
        }
        refreshPasscodeStatus()
        checkBiometrics()
    }

    fun refreshPasscodeStatus() {
        val hasPasscode = secureStorage.isPasscodeSet()
        _uiState.update { it.copy(isPasscodeConfigured = hasPasscode) }
    }

    fun checkBiometrics() {
        val status = biometricAuthManager.checkBiometricStatus()
        _uiState.update { it.copy(isBiometricAvailable = status is BiometricAuthManager.BiometricStatus.Ready) }
    }

    fun setLockMode(mode: AppLockMode) {
        viewModelScope.launch {
            secureStorage.setAppLockMode(mode)
            if (mode == AppLockMode.DISABLED) {
                _uiState.update { it.copy(isLocked = false) }
            }
        }
    }

    fun setGracePeriod(period: LockGracePeriod) {
        viewModelScope.launch {
            secureStorage.setLockGracePeriod(period)
        }
    }

    fun setScreenshotProtection(enabled: Boolean) {
        viewModelScope.launch {
            secureStorage.setScreenshotProtectionEnabled(enabled)
        }
    }

    fun setupPasscode(passcode: String) {
        secureStorage.setPasscode(passcode)
        refreshPasscodeStatus()
    }

    fun verifyPasscode(passcode: String): Boolean {
        val success = secureStorage.verifyPasscode(passcode)
        if (success) {
            unlockSession()
        }
        return success
    }

    fun unlockSession() {
        isUnlockedThisSession = true
        _uiState.update { it.copy(isLocked = false, errorMessage = null) }
    }

    fun lockNow() {
        if (_uiState.value.lockMode != AppLockMode.DISABLED) {
            isUnlockedThisSession = false
            _uiState.update { it.copy(isLocked = true) }
        }
    }

    /**
     * Called when the app enters background (e.g., onStop in MainActivity).
     */
    fun onAppMovedToBackground() {
        backgroundTimestamp = System.currentTimeMillis()
    }

    /**
     * Called when the app returns to foreground (e.g., onStart in MainActivity).
     * Enforces grace period timeout.
     */
    fun onAppMovedToForeground() {
        if (_uiState.value.lockMode == AppLockMode.DISABLED) return

        val now = System.currentTimeMillis()
        val elapsed = now - backgroundTimestamp
        val graceMs = _uiState.value.gracePeriod.timeoutMs

        if (backgroundTimestamp > 0L && elapsed > graceMs) {
            isUnlockedThisSession = false
            _uiState.update { it.copy(isLocked = true) }
        }
        backgroundTimestamp = 0L
    }

    companion object {
        @Volatile
        private var isUnlockedThisSession: Boolean = false
    }

    class Factory(
        private val secureStorage: SecureStorage,
        private val biometricAuthManager: BiometricAuthManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SecurityViewModel(secureStorage, biometricAuthManager) as T
        }
    }
}
