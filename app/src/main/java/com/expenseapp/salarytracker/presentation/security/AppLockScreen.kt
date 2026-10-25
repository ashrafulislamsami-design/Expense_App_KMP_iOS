package com.expenseapp.salarytracker.presentation.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.expenseapp.salarytracker.domain.security.AppLockMode

@Composable
fun AppLockScreen(
    securityViewModel: SecurityViewModel,
    biometricAuthManager: BiometricAuthManager,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var enteredPasscode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val lockMode = securityViewModel.uiState.value.lockMode
    val isBiometricAvailable = securityViewModel.uiState.value.isBiometricAvailable

    // Auto-prompt biometric if configured
    LaunchedEffect(Unit) {
        if (activity != null && (lockMode == AppLockMode.BIOMETRIC_ONLY || lockMode == AppLockMode.BIOMETRIC_OR_PASSCODE)) {
            biometricAuthManager.authenticate(
                activity = activity,
                title = "Unlock Expense Tracker",
                subtitle = "Verify your identity to access financial records",
                negativeButtonText = if (lockMode == AppLockMode.BIOMETRIC_OR_PASSCODE) "Enter Passcode" else "Cancel"
            ) { result ->
                when (result) {
                    is BiometricAuthManager.AuthResult.Success -> {
                        securityViewModel.unlockSession()
                        onUnlocked()
                    }
                    is BiometricAuthManager.AuthResult.Error -> {
                        errorMessage = result.errString.toString()
                    }
                    is BiometricAuthManager.AuthResult.Failed -> {
                        errorMessage = "Biometric not recognized. Try again."
                    }
                    BiometricAuthManager.AuthResult.UserCanceled -> {
                        // Keep passcode keypad open
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Icon & Lock Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Expense & Salary Tracker",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Personal finance data is encrypted & protected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Passcode PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val filled = index < enteredPasscode.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error display
            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Keypad (3x4 grid)
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIOMETRIC", "0", "BACKSPACE")
            )

            for (row in keys) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (key in row) {
                        when (key) {
                            "BIOMETRIC" -> {
                                if (isBiometricAvailable && activity != null) {
                                    IconButton(
                                        onClick = {
                                            biometricAuthManager.authenticate(
                                                activity = activity,
                                                onResult = { result ->
                                                    if (result is BiometricAuthManager.AuthResult.Success) {
                                                        securityViewModel.unlockSession()
                                                        onUnlocked()
                                                    }
                                                }
                                            )
                                        },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Fingerprint,
                                            contentDescription = "Authenticate with Biometrics",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(64.dp))
                                }
                            }
                            "BACKSPACE" -> {
                                IconButton(
                                    onClick = {
                                        if (enteredPasscode.isNotEmpty()) {
                                            enteredPasscode = enteredPasscode.dropLast(1)
                                            errorMessage = null
                                        }
                                    },
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable {
                                            if (enteredPasscode.length < 4) {
                                                enteredPasscode += key
                                                errorMessage = null

                                                if (enteredPasscode.length == 4) {
                                                    val verified = securityViewModel.verifyPasscode(enteredPasscode)
                                                    if (verified) {
                                                        onUnlocked()
                                                    } else {
                                                        errorMessage = "Incorrect passcode"
                                                        enteredPasscode = ""
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
