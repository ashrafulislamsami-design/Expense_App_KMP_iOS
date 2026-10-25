package com.expenseapp.salarytracker

import android.app.Application
import com.expenseapp.salarytracker.data.database.ExpenseDatabase
import com.expenseapp.salarytracker.data.repository.AccountRepositoryImpl
import com.expenseapp.salarytracker.data.repository.BudgetRepositoryImpl
import com.expenseapp.salarytracker.data.repository.CategoryRepositoryImpl
import com.expenseapp.salarytracker.data.repository.GoalRepositoryImpl
import com.expenseapp.salarytracker.data.repository.SettingsRepositoryImpl
import com.expenseapp.salarytracker.data.repository.TransactionRepositoryImpl
import com.expenseapp.salarytracker.domain.repository.AccountRepository
import com.expenseapp.salarytracker.domain.repository.BudgetRepository
import com.expenseapp.salarytracker.domain.repository.CategoryRepository
import com.expenseapp.salarytracker.domain.repository.GoalRepository
import com.expenseapp.salarytracker.domain.repository.SettingsRepository
import com.expenseapp.salarytracker.domain.repository.TransactionRepository
import com.expenseapp.salarytracker.domain.service.FinancialCalculationEngine
import com.expenseapp.salarytracker.domain.service.SalaryCycleCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExpenseApplication : Application() {

    override fun attachBaseContext(base: android.content.Context?) {
        super.attachBaseContext(base)
        com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.install(this)
        base?.let { com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.onBaseContextAttached(it) }
    }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: ExpenseDatabase by lazy {
        com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
            com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.DATABASE_INITIALIZATION_STARTED
        )
        val db = ExpenseDatabase.getInstance(this, applicationScope)
        com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
            com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.DATABASE_INITIALIZATION_COMPLETED
        )
        db
    }

    val calculationEngine: FinancialCalculationEngine by lazy {
        FinancialCalculationEngine()
    }

    val salaryCycleCalculator: SalaryCycleCalculator by lazy {
        SalaryCycleCalculator()
    }

    val goalCalculationEngine: com.expenseapp.salarytracker.domain.service.GoalCalculationEngine by lazy {
        com.expenseapp.salarytracker.domain.service.GoalCalculationEngine()
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepositoryImpl(database.accountDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao())
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database.transactionDao())
    }

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(database.budgetDao())
    }

    val goalRepository: GoalRepository by lazy {
        GoalRepositoryImpl(database.goalDao(), database.treeHistoryDao())
    }

    val recurringTransactionEngine: com.expenseapp.salarytracker.domain.service.RecurringTransactionEngine by lazy {
        com.expenseapp.salarytracker.domain.service.RecurringTransactionEngine()
    }

    val recurringRuleRepository: com.expenseapp.salarytracker.domain.repository.RecurringRuleRepository by lazy {
        com.expenseapp.salarytracker.data.repository.RecurringRuleRepositoryImpl(database.recurringRuleDao())
    }

    val customFieldRepository: com.expenseapp.salarytracker.domain.repository.CustomFieldRepository by lazy {
        com.expenseapp.salarytracker.data.repository.CustomFieldRepositoryImpl(database.customFieldDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(database.userSettingDao())
    }

    val reportGenerationService: com.expenseapp.salarytracker.domain.service.ReportGenerationService by lazy {
        com.expenseapp.salarytracker.domain.service.ReportGenerationService(
            calculationEngine = calculationEngine,
            salaryCycleCalculator = salaryCycleCalculator,
            goalCalculationEngine = goalCalculationEngine
        )
    }

    val backupRestoreService: com.expenseapp.salarytracker.domain.service.export.BackupRestoreService by lazy {
        com.expenseapp.salarytracker.domain.service.export.BackupRestoreService(database)
    }

    val keystoreManager: com.expenseapp.salarytracker.data.security.AndroidKeystoreManager by lazy {
        com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
            com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.SECURITY_INITIALIZATION_STARTED
        )
        com.expenseapp.salarytracker.data.security.AndroidKeystoreManager()
    }

    val passcodeKdfService: com.expenseapp.salarytracker.data.security.PasscodeKdfService by lazy {
        com.expenseapp.salarytracker.data.security.PasscodeKdfService()
    }

    val encryptionService: com.expenseapp.salarytracker.domain.security.EncryptionService by lazy {
        com.expenseapp.salarytracker.data.security.AndroidEncryptionService(
            keyProvider = { keystoreManager.getOrCreateMasterKey() },
            passcodeKdfService = passcodeKdfService
        )
    }

    val secureStorage: com.expenseapp.salarytracker.domain.security.SecureStorage by lazy {
        com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
            com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.SECURITY_INITIALIZATION_STARTED
        )
        val storage = com.expenseapp.salarytracker.data.security.SecureStorageImpl(
            context = this,
            encryptionService = encryptionService,
            passcodeKdfService = passcodeKdfService
        )
        com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
            com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.SECURITY_INITIALIZATION_COMPLETED
        )
        storage
    }

    val biometricAuthManager: com.expenseapp.salarytracker.presentation.security.BiometricAuthManager by lazy {
        com.expenseapp.salarytracker.presentation.security.BiometricAuthManager(this)
    }

    val encryptedBackupEngine: com.expenseapp.salarytracker.data.security.EncryptedBackupEngine by lazy {
        com.expenseapp.salarytracker.data.security.EncryptedBackupEngine(
            passcodeKdfService = passcodeKdfService
        )
    }

    val exportManager: com.expenseapp.salarytracker.presentation.reports.ExportManager by lazy {
        com.expenseapp.salarytracker.presentation.reports.ExportManager(
            encryptedBackupEngine = encryptedBackupEngine
        )
    }

    val appNotificationManager: com.expenseapp.salarytracker.data.notification.AppNotificationManager by lazy {
        com.expenseapp.salarytracker.data.notification.AppNotificationManager(this)
    }

    val budgetNotificationService: com.expenseapp.salarytracker.domain.service.notification.BudgetNotificationService by lazy {
        com.expenseapp.salarytracker.domain.service.notification.BudgetNotificationService(
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            settingsRepository = settingsRepository,
            userSettingDao = database.userSettingDao(),
            notificationManager = appNotificationManager
        )
    }

    val goalNotificationService: com.expenseapp.salarytracker.domain.service.notification.GoalNotificationService by lazy {
        com.expenseapp.salarytracker.domain.service.notification.GoalNotificationService(
            goalRepository = goalRepository,
            settingsRepository = settingsRepository,
            userSettingDao = database.userSettingDao(),
            notificationManager = appNotificationManager
        )
    }

    override fun onCreate() {
        super.onCreate()
        com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
            com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.APPLICATION_ON_CREATE_STARTED
        )
        instance = this

        if (com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.hasCrashReport(this)) {
            // A startup crash report is awaiting review from previous run.
            // Bypass background initialization so MainActivity / CrashRecoveryActivity
            // can safely present the diagnostic report to the user without secondary crashes.
            return
        }

        // Safe Startup Bisection Mode check
        if (!com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.canProceedToStage(
                this,
                com.expenseapp.salarytracker.diagnostics.StartupStage.STAGE_2_APPLICATION_DATABASE
            )
        ) {
            return
        }

        // Initialize Notification Channels
        com.expenseapp.salarytracker.data.notification.NotificationChannels.createChannels(this)

        // Ensure default accounts, categories, settings, and custom fields exist
        applicationScope.launch(Dispatchers.IO) {
            database.prepopulateDefaults()
            customFieldRepository.ensureDefaultCustomFields()

            // Initialize / verify daily reminder schedule
            val isReminderEnabled = settingsRepository.isDailyReminderEnabled().first()
            val reminderTime = settingsRepository.getDailyReminderTime().first()
            val reminderDays = settingsRepository.getDailyReminderDays().first()
            com.expenseapp.salarytracker.data.notification.DailyReminderScheduler.schedule(
                context = this@ExpenseApplication,
                enabled = isReminderEnabled,
                timeString = reminderTime,
                activeDays = reminderDays
            )
        }
    }

    companion object {
        lateinit var instance: ExpenseApplication
            private set
    }
}
