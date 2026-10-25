package com.expenseapp.salarytracker.presentation.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expenseapp.salarytracker.ExpenseApplication
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.report.BackupEnvelope
import com.expenseapp.salarytracker.domain.model.report.BackupValidationResult
import com.expenseapp.salarytracker.domain.model.report.FinancialReport
import com.expenseapp.salarytracker.domain.model.report.ReportPeriodType
import com.expenseapp.salarytracker.domain.repository.AccountRepository
import com.expenseapp.salarytracker.domain.repository.BudgetRepository
import com.expenseapp.salarytracker.domain.repository.CategoryRepository
import com.expenseapp.salarytracker.domain.repository.GoalRepository
import com.expenseapp.salarytracker.domain.repository.SettingsRepository
import com.expenseapp.salarytracker.domain.repository.TransactionRepository
import com.expenseapp.salarytracker.domain.service.ReportGenerationService
import com.expenseapp.salarytracker.domain.service.export.BackupRestoreService
import com.expenseapp.salarytracker.domain.service.export.JsonBackupEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.LocalDate

data class ReportsUiState(
    val selectedPeriodType: ReportPeriodType = ReportPeriodType.THIS_MONTH,
    val customStartDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val customEndDate: LocalDate = LocalDate.now(),
    val selectedFormat: ExportFormat = ExportFormat.XLSX,
    val report: FinancialReport? = null,
    val currency: String = "BDT",
    val salaryConfig: SalaryCycleConfig = SalaryCycleConfig(),
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val shareIntent: Intent? = null,
    val backupEnvelope: BackupEnvelope? = null,
    val restoreValidationResult: BackupValidationResult? = null,
    val showRestoreDialog: Boolean = false
)

class ReportsViewModel(
    private val reportGenerationService: ReportGenerationService,
    private val backupRestoreService: BackupRestoreService,
    private val exportManager: ExportManager,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        observeDataSources()
    }

    private fun observeDataSources() {
        viewModelScope.launch {
            combine(
                combine(
                    transactionRepository.getAllTransactions(),
                    accountRepository.getAllAccounts(),
                    categoryRepository.getAllCategories()
                ) { txs, accs, cats -> Triple(txs, accs, cats) },
                combine(
                    budgetRepository.getAllBudgets(),
                    goalRepository.getAllGoals(),
                    goalRepository.getTreeHistory(50)
                ) { budgets, goals, treeHistory -> Triple(budgets, goals, treeHistory) },
                combine(
                    settingsRepository.getCurrency(),
                    settingsRepository.getSalaryCycleConfig()
                ) { curr, salaryCfg -> Pair(curr, salaryCfg) }
            ) { (txs, accs, cats), (budgets, goals, treeHistory), (curr, salaryCfg) ->
                val currentPeriod = _uiState.value.selectedPeriodType
                val customStart = _uiState.value.customStartDate
                val customEnd = _uiState.value.customEndDate

                val dateRange = reportGenerationService.resolveDateRange(
                    periodType = currentPeriod,
                    salaryConfig = salaryCfg,
                    customStart = customStart,
                    customEnd = customEnd
                )

                val generatedReport = reportGenerationService.generateReport(
                    dateRange = dateRange,
                    allTransactions = txs,
                    accounts = accs,
                    categories = cats,
                    budgets = budgets,
                    goals = goals,
                    treeHistory = treeHistory,
                    currencyCode = curr,
                    currencySymbol = if (curr == "BDT") "৳" else "$"
                )

                _uiState.update { current ->
                    current.copy(
                        report = generatedReport,
                        currency = curr,
                        salaryConfig = salaryCfg,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    fun selectPeriod(periodType: ReportPeriodType) {
        _uiState.update { it.copy(selectedPeriodType = periodType, isLoading = true) }
        refreshReport()
    }

    fun setCustomDateRange(start: LocalDate, end: LocalDate) {
        require(!start.isAfter(end)) { "Start date cannot be after end date" }
        _uiState.update {
            it.copy(
                selectedPeriodType = ReportPeriodType.CUSTOM,
                customStartDate = start,
                customEndDate = end,
                isLoading = true
            )
        }
        refreshReport()
    }

    fun selectFormat(format: ExportFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    private fun refreshReport() {
        viewModelScope.launch {
            val txs = transactionRepository.getAllTransactions().first()
            val accs = accountRepository.getAllAccounts().first()
            val cats = categoryRepository.getAllCategories().first()
            val budgets = budgetRepository.getAllBudgets().first()
            val goals = goalRepository.getAllGoals().first()
            val treeHistory = goalRepository.getTreeHistory(50).first()
            val curr = settingsRepository.getCurrency().first()
            val salaryCfg = settingsRepository.getSalaryCycleConfig().first()

            val dateRange = reportGenerationService.resolveDateRange(
                periodType = _uiState.value.selectedPeriodType,
                salaryConfig = salaryCfg,
                customStart = _uiState.value.customStartDate,
                customEnd = _uiState.value.customEndDate
            )

            val generatedReport = reportGenerationService.generateReport(
                dateRange = dateRange,
                allTransactions = txs,
                accounts = accs,
                categories = cats,
                budgets = budgets,
                goals = goals,
                treeHistory = treeHistory,
                currencyCode = curr,
                currencySymbol = if (curr == "BDT") "৳" else "$"
            )

            _uiState.update { it.copy(report = generatedReport, isLoading = false) }
        }
    }

    fun exportToUri(context: Context, uri: Uri, passphrase: CharArray? = null) {
        viewModelScope.launch {
            val report = _uiState.value.report ?: return@launch
            val format = _uiState.value.selectedFormat

            _uiState.update { it.copy(isExporting = true, statusMessage = null, errorMessage = null) }
            try {
                val envelope = if (format == ExportFormat.JSON_BACKUP || format == ExportFormat.JSON_ENCRYPTED_BACKUP) {
                    backupRestoreService.createBackupEnvelope()
                } else null

                context.contentResolver.openOutputStream(uri)?.use { os ->
                    exportManager.exportToStream(format, report, envelope, os, passphrase)
                } ?: error("Failed to open output stream for selected file")

                val filename = exportManager.getDefaultFilename(format, report)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "Successfully saved: $filename"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun prepareShareReport(context: Context) {
        viewModelScope.launch {
            val report = _uiState.value.report ?: return@launch
            val format = _uiState.value.selectedFormat

            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            try {
                val envelope = if (format == ExportFormat.JSON_BACKUP || format == ExportFormat.JSON_ENCRYPTED_BACKUP) {
                    backupRestoreService.createBackupEnvelope()
                } else null

                val shareIntent = exportManager.createShareIntent(context, format, report, envelope)
                _uiState.update { it.copy(isExporting = false, shareIntent = shareIntent) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Failed to prepare share file: ${e.message}"
                    )
                }
            }
        }
    }

    fun onShareIntentConsumed() {
        _uiState.update { it.copy(shareIntent = null) }
    }

    fun prepareRestoreFromStream(inputStream: InputStream, passphrase: CharArray? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val bytes = inputStream.readBytes()
                val isEncrypted = ExpenseApplication.instance.encryptedBackupEngine.isEncryptedBackup(bytes)

                val rawJsonBytes = if (isEncrypted) {
                    if (passphrase == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Encrypted backup detected. Please provide decryption passphrase."
                            )
                        }
                        return@launch
                    }
                    ExpenseApplication.instance.encryptedBackupEngine.decryptBackup(
                        java.io.ByteArrayInputStream(bytes),
                        passphrase
                    )
                } else {
                    bytes
                }

                val validation = JsonBackupEngine().validateBackup(java.io.ByteArrayInputStream(rawJsonBytes))
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        restoreValidationResult = validation,
                        showRestoreDialog = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not read backup file: ${e.message}"
                    )
                }
            }
        }
    }

    fun confirmRestore(wipeAndReplace: Boolean) {
        viewModelScope.launch {
            val envelope = _uiState.value.restoreValidationResult?.envelope ?: return@launch
            _uiState.update { it.copy(isLoading = true, showRestoreDialog = false) }

            try {
                if (wipeAndReplace) {
                    backupRestoreService.restoreWipeAndReplace(envelope).getOrThrow()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = "Full restoration complete: ${envelope.transactions.size} transactions restored"
                        )
                    }
                } else {
                    val mergeSummary = backupRestoreService.restoreMerge(envelope).getOrThrow()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = "Merge complete: ${mergeSummary.transactionsImported} transactions imported (${mergeSummary.duplicateTransactionsSkipped} duplicates skipped)"
                        )
                    }
                }
                refreshReport()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Restore failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun dismissRestoreDialog() {
        _uiState.update { it.copy(showRestoreDialog = false, restoreValidationResult = null) }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    class Factory(
        private val reportGenerationService: ReportGenerationService,
        private val backupRestoreService: BackupRestoreService,
        private val exportManager: ExportManager,
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val budgetRepository: BudgetRepository,
        private val goalRepository: GoalRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportsViewModel(
                reportGenerationService,
                backupRestoreService,
                exportManager,
                transactionRepository,
                accountRepository,
                categoryRepository,
                budgetRepository,
                goalRepository,
                settingsRepository
            ) as T
        }
    }
}
