package com.expenseapp.salarytracker.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseapp.salarytracker.data.notification.DailyReminderScheduler
import com.expenseapp.salarytracker.domain.repository.SettingsRepository
import com.expenseapp.salarytracker.domain.security.AppLockMode
import com.expenseapp.salarytracker.domain.security.LockGracePeriod
import com.expenseapp.salarytracker.presentation.security.SecurityViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek

import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FileDownload

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    securityViewModel: SecurityViewModel,
    settingsRepository: SettingsRepository,
    onNavigateToReports: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val secState by securityViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val reminderEnabled by settingsRepository.isDailyReminderEnabled().collectAsStateWithLifecycle(initialValue = true)
    val reminderTime by settingsRepository.getDailyReminderTime().collectAsStateWithLifecycle(initialValue = "23:00")
    val reminderDays by settingsRepository.getDailyReminderDays().collectAsStateWithLifecycle(initialValue = DayOfWeek.entries.toSet())
    val budgetAlertsEnabled by settingsRepository.isBudgetAlertsEnabled().collectAsStateWithLifecycle(initialValue = true)
    val goalNotificationsEnabled by settingsRepository.isGoalNotificationsEnabled().collectAsStateWithLifecycle(initialValue = true)

    var showPasscodeSetupDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Insights & Reports Hub
            item {
                Text(
                    text = "Insights & Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToReports() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Assessment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Reports & Export",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Export Excel (XLSX), PDF, CSV, and encrypted backup",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Open reports",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Security & Application Lock
            item {
                Text(
                    text = "App Lock & Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Lock Mode Selection
                        Text(
                            text = "Authentication Method",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        val modes = listOf(
                            AppLockMode.DISABLED,
                            AppLockMode.BIOMETRIC_OR_PASSCODE,
                            AppLockMode.BIOMETRIC_ONLY,
                            AppLockMode.PASSCODE_ONLY
                        )

                        for (mode in modes) {
                            val isSelected = secState.lockMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (mode != AppLockMode.DISABLED && !secState.isPasscodeConfigured) {
                                            showPasscodeSetupDialog = true
                                        }
                                        securityViewModel.setLockMode(mode)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (mode != AppLockMode.DISABLED && !secState.isPasscodeConfigured) {
                                            showPasscodeSetupDialog = true
                                        }
                                        securityViewModel.setLockMode(mode)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = mode.displayName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    val desc = when (mode) {
                                        AppLockMode.DISABLED -> "Open app directly without authentication"
                                        AppLockMode.BIOMETRIC_OR_PASSCODE -> "Fingerprint/Face with 4-digit passcode fallback"
                                        AppLockMode.BIOMETRIC_ONLY -> "Device biometric prompt only"
                                        AppLockMode.PASSCODE_ONLY -> "Local 4-digit passcode keypad"
                                    }
                                    Text(text = desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Passcode Config Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (secState.isPasscodeConfigured) "Passcode Configured" else "Passcode Not Set",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "4-digit PIN for secure local access",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { showPasscodeSetupDialog = true }) {
                                Text(if (secState.isPasscodeConfigured) "Change PIN" else "Set PIN", fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Grace Period Selector
                        Text(
                            text = "Lock Grace Period",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (period in LockGracePeriod.entries) {
                                val isSelected = secState.gracePeriod == period
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { securityViewModel.setGracePeriod(period) },
                                    label = { Text(period.displayName, fontSize = 12.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Screenshot / App Privacy Protection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "App Privacy & Screen Protection", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    text = "Blocks screenshots and hides financial details in recent apps",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = secState.isScreenshotProtectionEnabled,
                                onCheckedChange = { securityViewModel.setScreenshotProtection(it) }
                            )
                        }

                        // Instant Lock Button
                        if (secState.lockMode != AppLockMode.DISABLED) {
                            Button(
                                onClick = { securityViewModel.lockNow() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lock Application Now", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 2. Reminders & Alerts
            item {
                Text(
                    text = "Reminders & Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Permission warning banner on Android 13+
                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Notification Permission Required",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Android 13+ requires explicit permission to post reminders.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Grant", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Daily Reminder Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Daily Expense Reminder", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    text = "Gentle prompt to record today's spending",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { isChecked ->
                                    scope.launch {
                                        settingsRepository.setDailyReminderEnabled(isChecked)
                                        DailyReminderScheduler.schedule(context, isChecked, reminderTime, reminderDays)
                                        if (isChecked && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                }
                            )
                        }

                        if (reminderEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            // Reminder Time Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Reminder Time", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        text = "Scheduled daily at $reminderTime",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(onClick = { showTimePickerDialog = true }) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(reminderTime, fontSize = 12.sp)
                                }
                            }

                            // Reminder Days Row
                            Text(
                                text = "Applicable Days",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val dayLabels = listOf(
                                    DayOfWeek.MONDAY to "Mon",
                                    DayOfWeek.TUESDAY to "Tue",
                                    DayOfWeek.WEDNESDAY to "Wed",
                                    DayOfWeek.THURSDAY to "Thu",
                                    DayOfWeek.FRIDAY to "Fri",
                                    DayOfWeek.SATURDAY to "Sat",
                                    DayOfWeek.SUNDAY to "Sun"
                                )
                                dayLabels.forEach { (day, label) ->
                                    val isSelected = reminderDays.contains(day)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            scope.launch {
                                                val newDays = if (isSelected) {
                                                    if (reminderDays.size > 1) reminderDays - day else reminderDays
                                                } else {
                                                    reminderDays + day
                                                }
                                                settingsRepository.setDailyReminderDays(newDays)
                                                DailyReminderScheduler.schedule(context, reminderEnabled, reminderTime, newDays)
                                            }
                                        },
                                        label = { Text(label, fontSize = 12.sp) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        // Budget Alerts Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Spending Limit Reminders", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    text = "Get a reminder when you're getting close to your monthly limit",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = budgetAlertsEnabled,
                                onCheckedChange = { isChecked ->
                                    scope.launch {
                                        settingsRepository.setBudgetAlertsEnabled(isChecked)
                                    }
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        // Goal Milestone Notifications Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Goal Milestone Updates", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    text = "Encouraging notifications when reaching 25%, 50%, 75%, 100%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = goalNotificationsEnabled,
                                onCheckedChange = { isChecked ->
                                    scope.launch {
                                        settingsRepository.setGoalNotificationsEnabled(isChecked)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 3. Privacy Architecture Guarantee
            item {
                Text(
                    text = "Privacy Guarantee",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "100% Offline & Private by Design",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "• Zero Internet access or background sync\n• No analytics, tracking, or advertisements\n• Protected by your device's hardware security\n• All financial records stay strictly on your device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    if (showPasscodeSetupDialog) {
        PasscodeSetupDialog(
            onDismiss = { showPasscodeSetupDialog = false },
            onConfirm = { pin ->
                securityViewModel.setupPasscode(pin)
                showPasscodeSetupDialog = false
            }
        )
    }

    if (showTimePickerDialog) {
        ReminderTimePickerDialog(
            currentTime = reminderTime,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { selectedTime ->
                scope.launch {
                    settingsRepository.setDailyReminderTime(selectedTime)
                    DailyReminderScheduler.schedule(context, reminderEnabled, selectedTime, reminderDays)
                }
                showTimePickerDialog = false
            }
        )
    }
}

@Composable
private fun ReminderTimePickerDialog(
    currentTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var timeInput by remember { mutableStateOf(currentTime) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val presets = listOf("20:00", "21:00", "22:00", "23:00", "23:30")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Reminder Time")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Quick Presets:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = timeInput == preset,
                            onClick = {
                                timeInput = preset
                                errorText = null
                            },
                            label = { Text(preset) }
                        )
                    }
                }

                Text(
                    text = "Or enter custom 24-hour time (HH:mm):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = timeInput,
                    onValueChange = {
                        timeInput = it.take(5)
                        errorText = null
                    },
                    label = { Text("Time (HH:mm)") },
                    placeholder = { Text("23:00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(text = errorText!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isValid = runCatching {
                        java.time.LocalTime.parse(timeInput.trim())
                    }.isSuccess
                    if (isValid) {
                        onConfirm(timeInput.trim())
                    } else {
                        errorText = "Invalid time format. Please use HH:mm (e.g. 23:00)"
                    }
                }
            ) {
                Text("Set Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PasscodeSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set 4-Digit Passcode")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter a 4-digit numeric PIN for offline fallback authentication.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("Confirm 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(text = errorText!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length != 4) {
                        errorText = "PIN must be exactly 4 digits"
                    } else if (pin != confirmPin) {
                        errorText = "PINs do not match"
                    } else {
                        onConfirm(pin)
                    }
                }
            ) {
                Text("Save Passcode")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

