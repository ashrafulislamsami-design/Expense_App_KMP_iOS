package com.expenseapp.salarytracker.presentation.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * AndroidX BiometricPrompt abstraction.
 *
 * Requirements:
 * - Checks hardware availability and enrollment.
 * - Handles success, lockout, user cancellation, and fallback cleanly.
 * - Never stores biometric templates or credentials.
 * - Adheres strictly to privacy-security-audit guidelines.
 */
class BiometricAuthManager(
    private val context: Context
) {
    sealed class BiometricStatus {
        data object Ready : BiometricStatus()
        data object NotEnrolled : BiometricStatus()
        data object HardwareUnavailable : BiometricStatus()
        data object Unsupported : BiometricStatus()
    }

    sealed class AuthResult {
        data object Success : AuthResult()
        data class Error(val errorCode: Int, val errString: CharSequence) : AuthResult()
        data object UserCanceled : AuthResult()
        data object Failed : AuthResult()
    }

    private val biometricManager = BiometricManager.from(context)

    /**
     * Checks if biometric hardware is present and user has enrolled biometric credentials.
     */
    fun checkBiometricStatus(): BiometricStatus {
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Ready
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HardwareUnavailable
            else -> BiometricStatus.Unsupported
        }
    }

    /**
     * Checks if either biometric or device credentials (PIN/Pattern/Password) are available.
     */
    fun canAuthenticateWithBiometricsOrDeviceCredential(): Boolean {
        val result = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Displays system BiometricPrompt dialog.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Expense Tracker",
        subtitle: String = "Authenticate to access your financial data",
        negativeButtonText: String = "Use Passcode",
        onResult: (AuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(AuthResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        onResult(AuthResult.UserCanceled)
                    } else {
                        onResult(AuthResult.Error(errorCode, errString))
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onResult(AuthResult.Failed)
                }
            }
        )

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onResult(AuthResult.Error(-1, e.message ?: "Biometric prompt error"))
        }
    }
}
