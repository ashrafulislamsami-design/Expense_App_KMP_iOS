package com.expenseapp.salarytracker.presentation.reports

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expenseapp.salarytracker.ExpenseApplication
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.report.FinancialReport
import com.expenseapp.salarytracker.domain.model.report.ReportPeriodType
import com.expenseapp.salarytracker.presentation.theme.AppExpense
import com.expenseapp.salarytracker.presentation.theme.AppIncome
import com.expenseapp.salarytracker.presentation.theme.expenseTextColor
import com.expenseapp.salarytracker.presentation.theme.incomeTextColor
import com.expenseapp.salarytracker.presentation.theme.warningTextColor
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    initialPeriodType: ReportPeriodType? = null,
    viewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModel.Factory(
            reportGenerationService = ExpenseApplication.instance.reportGenerationService,
            backupRestoreService = ExpenseApplication.instance.backupRestoreService,
            exportManager = ExpenseApplication.instance.exportManager,
            transactionRepository = ExpenseApplication.instance.transactionRepository,
            accountRepository = ExpenseApplication.instance.accountRepository,
            categoryRepository = ExpenseApplication.instance.categoryRepository,
            budgetRepository = ExpenseApplication.instance.budgetRepository,
            goalRepository = ExpenseApplication.instance.goalRepository,
            settingsRepository = ExpenseApplication.instance.settingsRepository
        )
    )
) {
    androidx.compose.runtime.LaunchedEffect(initialPeriodType) {
        if (initialPeriodType != null) {
            viewModel.selectPeriod(initialPeriodType)
        }
    }

    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingSaveUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingRestoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showPassphraseDialogForExport by remember { mutableStateOf(false) }
    var showPassphraseDialogForImport by remember { mutableStateOf(false) }

    // Android SAF Launcher for saving exported document
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(state.selectedFormat.mimeType)
    ) { uri ->
        uri?.let {
            if (state.selectedFormat == ExportFormat.JSON_ENCRYPTED_BACKUP) {
                pendingSaveUri = it
                showPassphraseDialogForExport = true
            } else {
                viewModel.exportToUri(context, it)
            }
        }
    }

    // Android SAF Launcher for opening JSON backup
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            if (inputStream != null) {
                val bytes = inputStream.readBytes()
                val isEncrypted = ExpenseApplication.instance.encryptedBackupEngine.isEncryptedBackup(bytes)
                if (isEncrypted) {
                    pendingRestoreBytes = bytes
                    showPassphraseDialogForImport = true
                } else {
                    viewModel.prepareRestoreFromStream(java.io.ByteArrayInputStream(bytes))
                }
            } else {
                Toast.makeText(context, "Could not open selected file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Share Intent Consumer
    LaunchedEffect(state.shareIntent) {
        state.shareIntent?.let {
            context.startActivity(Intent.createChooser(it, "Share Financial Statement"))
            viewModel.onShareIntentConsumed()
        }
    }

    // Status / Error Snackbars
    LaunchedEffect(state.statusMessage, state.errorMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    // Custom Date Range Picker state
    var showCustomDateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Reports & Export", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = state.report?.periodLabel ?: "Executive Financial Reporting",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Period Selector Chips
            item {
                Text(
                    text = "Report Period",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val periods = listOf(
                        ReportPeriodType.THIS_MONTH,
                        ReportPeriodType.CURRENT_SALARY_CYCLE,
                        ReportPeriodType.LAST_MONTH,
                        ReportPeriodType.PREVIOUS_SALARY_CYCLE,
                        ReportPeriodType.THIS_WEEK,
                        ReportPeriodType.LAST_WEEK,
                        ReportPeriodType.TODAY,
                        ReportPeriodType.CUSTOM
                    )
                    for (period in periods) {
                        val isSelected = state.selectedPeriodType == period
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (period == ReportPeriodType.CUSTOM) {
                                    showCustomDateDialog = true
                                } else {
                                    viewModel.selectPeriod(period)
                                }
                            },
                            label = { Text(period.displayName) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // 2. Export Format Selector
            item {
                Text(
                    text = "Export Format",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (fmt in ExportFormat.entries) {
                        val isSelected = state.selectedFormat == fmt
                        val icon = when (fmt) {
                            ExportFormat.XLSX -> Icons.Default.TableChart
                            ExportFormat.PDF -> Icons.Default.PictureAsPdf
                            ExportFormat.CSV_TRANSACTIONS, ExportFormat.CSV_SUMMARY -> Icons.Default.FileDownload
                            ExportFormat.JSON_BACKUP -> Icons.Default.Download
                            ExportFormat.JSON_ENCRYPTED_BACKUP -> Icons.Default.Lock
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectFormat(fmt) },
                            label = { Text(fmt.displayName) },
                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            // 3. Action Toolbar (Save SAF, Share, Restore)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // SAF Save Button
                            Button(
                                onClick = {
                                    val defaultName = state.report?.let {
                                        ExpenseApplication.instance.exportManager.getDefaultFilename(state.selectedFormat, it)
                                    } ?: "ExpenseReport.${state.selectedFormat.extension}"
                                    createDocLauncher.launch(defaultName)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isExporting && state.report != null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (state.isExporting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Save File (SAF)", fontSize = 13.sp)
                            }

                            // Share Button
                            OutlinedButton(
                                onClick = { viewModel.prepareShareReport(context) },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isExporting && state.report != null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Report", fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Restore / Backup Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Database Backup & Restoration",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { openDocLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore JSON", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 4. Executive Financial Summary Preview Card
            item {
                val report = state.report
                if (report != null) {
                    ExecutiveReportPreviewCard(report = report)
                } else if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            // 5. Tree of Hope Status Card
            item {
                state.report?.let { report ->
                    TreeOfHopeCard(report = report)
                }
            }

            // 6. Top Categories Breakdown Card
            item {
                state.report?.let { report ->
                    CategoryBreakdownCard(report = report)
                }
            }

            // 7. Budget Performance Card
            item {
                state.report?.let { report ->
                    if (report.budgetBreakdown.isNotEmpty() || report.overallBudget != null) {
                        BudgetPerformanceCard(report = report)
                    }
                }
            }
        }
    }

    // Custom Date Range Dialog
    if (showCustomDateDialog) {
        CustomDateRangeDialog(
            currentStart = state.customStartDate,
            currentEnd = state.customEndDate,
            onDismiss = { showCustomDateDialog = false },
            onConfirm = { start, end ->
                viewModel.setCustomDateRange(start, end)
                showCustomDateDialog = false
            }
        )
    }

    // Export Passphrase Dialog
    if (showPassphraseDialogForExport && pendingSaveUri != null) {
        BackupPassphraseDialog(
            title = "Encrypt Backup with Passphrase",
            description = "Enter a password to encrypt your financial database with AES-256-GCM. You will need this password to restore your data.",
            confirmButtonText = "Encrypt & Save",
            onDismiss = {
                showPassphraseDialogForExport = false
                pendingSaveUri = null
            },
            onConfirm = { pwd ->
                showPassphraseDialogForExport = false
                pendingSaveUri?.let { uri ->
                    viewModel.exportToUri(context, uri, pwd.toCharArray())
                }
                pendingSaveUri = null
            }
        )
    }

    // Import Decryption Passphrase Dialog
    if (showPassphraseDialogForImport && pendingRestoreBytes != null) {
        BackupPassphraseDialog(
            title = "Encrypted Backup Detected",
            description = "This backup is protected with AES-256-GCM encryption. Please enter the decryption passphrase to unlock and inspect contents.",
            confirmButtonText = "Decrypt & Verify",
            onDismiss = {
                showPassphraseDialogForImport = false
                pendingRestoreBytes = null
            },
            onConfirm = { pwd ->
                showPassphraseDialogForImport = false
                pendingRestoreBytes?.let { bytes ->
                    viewModel.prepareRestoreFromStream(java.io.ByteArrayInputStream(bytes), pwd.toCharArray())
                }
                pendingRestoreBytes = null
            }
        )
    }

    // Restore Preview & Confirmation Dialog
    if (state.showRestoreDialog && state.restoreValidationResult != null) {
        val validation = state.restoreValidationResult!!
        RestoreConfirmationDialog(
            validation = validation,
            onDismiss = { viewModel.dismissRestoreDialog() },
            onConfirm = { wipeAndReplace ->
                viewModel.confirmRestore(wipeAndReplace)
            }
        )
    }
}

@Composable
private fun BackupPassphraseDialog(
    title: String,
    description: String,
    confirmButtonText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    val isExport = title.contains("Encrypt", ignoreCase = true)
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                androidx.compose.material3.OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it; errorMsg = null },
                    label = { Text("Passphrase") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isExport) {
                    androidx.compose.material3.OutlinedTextField(
                        value = confirmPassphrase,
                        onValueChange = { confirmPassphrase = it; errorMsg = null },
                        label = { Text("Confirm Passphrase") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passphrase.isBlank()) {
                        errorMsg = "Passphrase cannot be empty"
                    } else if (isExport && passphrase != confirmPassphrase) {
                        errorMsg = "Passphrases do not match"
                    } else {
                        onConfirm(passphrase)
                    }
                }
            ) {
                Text(confirmButtonText)
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
private fun ExecutiveReportPreviewCard(report: FinancialReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Executive Statement Preview",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${report.transactionCount} transactions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Opening & Closing Balances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricColumn(label = "Opening Balance", amount = report.openingBalance, currency = report.currencyCode)
                SummaryMetricColumn(label = "Closing Balance", amount = report.closingBalance, currency = report.currencyCode, isBold = true)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Income & Net Expense
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricColumn(label = "Total Income", amount = report.totalIncome, currency = report.currencyCode, color = incomeTextColor())
                SummaryMetricColumn(label = "Net Expenses", amount = report.netExpenses, currency = report.currencyCode, color = expenseTextColor())
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Savings & Net Cash Flow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricColumn(
                    label = "Net Savings (${String.format(Locale.US, "%.1f%%", report.savingsRatePercent)})",
                    amount = report.netSavings,
                    currency = report.currencyCode,
                    color = incomeTextColor()
                )
                SummaryMetricColumn(
                    label = "Net Cash Flow",
                    amount = report.netCashFlow,
                    currency = report.currencyCode,
                    color = if (report.netCashFlow.isNegative()) expenseTextColor() else incomeTextColor()
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricColumn(
    label: String,
    amount: MinorUnits,
    currency: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Column {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = amount.format(if (currency == "BDT") "৳" else "$"),
            fontSize = 15.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun TreeOfHopeCard(report: FinancialReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppIncome.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppIncome),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Eco, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Tree of Hope: ${report.treeState.displayName} (Level ${report.treeLevel})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = incomeTextColor()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = report.treeExplanation,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Positive Next Step: ${report.treeActionAdvice}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = incomeTextColor()
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(report: FinancialReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Top Spending Categories",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            for (cat in report.categoryBreakdown.take(5)) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = cat.categoryName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${cat.amount.format(if (report.currencyCode == "BDT") "৳" else "$")} (${String.format(Locale.US, "%.1f%%", cat.percentageOfTotal)})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (cat.percentageOfTotal / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(android.graphics.Color.parseColor(cat.colorHex.ifBlank { "#3B82F6" })),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetPerformanceCard(report: FinancialReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Budget Performance",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            report.overallBudget?.let { ob ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Overall Ceiling", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            text = "Spent ${ob.actualAmount.format()} of ${ob.plannedAmount.format()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${ob.percentageUsed.toInt()}% (${ob.statusText})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ob.isOverBudget) expenseTextColor() else incomeTextColor()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (ob.percentageUsed / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (ob.isOverBudget) expenseTextColor() else incomeTextColor(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CustomDateRangeDialog(
    currentStart: LocalDate,
    currentEnd: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    var start by remember { mutableStateOf(currentStart) }
    var end by remember { mutableStateOf(currentEnd) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Custom Date Range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Start Date Picker Button
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(context, { _, y, m, d ->
                            start = LocalDate.of(y, m + 1, d)
                        }, start.year, start.monthValue - 1, start.dayOfMonth).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Date: $start")
                }

                // End Date Picker Button
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(context, { _, y, m, d ->
                            end = LocalDate.of(y, m + 1, d)
                        }, end.year, end.monthValue - 1, end.dayOfMonth).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End Date: $end")
                }

                if (start.isAfter(end)) {
                    Text("Error: Start date cannot be after end date", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(start, end) },
                enabled = !start.isAfter(end)
            ) {
                Text("Apply Range")
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
private fun RestoreConfirmationDialog(
    validation: com.expenseapp.salarytracker.domain.model.report.BackupValidationResult,
    onDismiss: () -> Unit,
    onConfirm: (wipeAndReplace: Boolean) -> Unit
) {
    var wipeAndReplace by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore Backup Verification")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!validation.isValid) {
                    Text("Invalid Backup File:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    for (err in validation.errors) {
                        Text("• $err", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    val summary = validation.summary
                    if (summary != null) {
                        Text("Backup File Verified:", fontWeight = FontWeight.Bold)
                        Text("• Accounts: ${summary.accountCount}", fontSize = 12.sp)
                        Text("• Categories: ${summary.categoryCount}", fontSize = 12.sp)
                        Text("• Transactions: ${summary.transactionCount}", fontSize = 12.sp)
                        Text("• Budgets: ${summary.budgetCount}", fontSize = 12.sp)
                        Text("• Goals: ${summary.goalCount}", fontSize = 12.sp)
                        Text("• Schema Version: ${summary.schemaVersion}", fontSize = 12.sp)
                    }

                    if (validation.warnings.isNotEmpty()) {
                        Text("Warnings:", fontWeight = FontWeight.SemiBold, color = warningTextColor())
                        for (w in validation.warnings) {
                            Text("• $w", fontSize = 11.sp, color = warningTextColor())
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Restore Strategy:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { wipeAndReplace = false }
                    ) {
                        RadioButton(selected = !wipeAndReplace, onClick = { wipeAndReplace = false })
                        Text("Merge & Skip Duplicates (Safe)", fontSize = 12.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { wipeAndReplace = true }
                    ) {
                        RadioButton(selected = wipeAndReplace, onClick = { wipeAndReplace = true })
                        Text("Wipe & Replace Everything", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (validation.isValid) {
                Button(
                    onClick = { onConfirm(wipeAndReplace) },
                    colors = if (wipeAndReplace) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) {
                    Text(if (wipeAndReplace) "Confirm Wipe & Restore" else "Confirm Merge")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
