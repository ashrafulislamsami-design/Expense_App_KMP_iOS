package com.expenseapp.salarytracker.presentation.main

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.expenseapp.salarytracker.ExpenseApplication
import com.expenseapp.salarytracker.presentation.dashboard.DashboardViewModel
import com.expenseapp.salarytracker.presentation.security.SecurityViewModel
import com.expenseapp.salarytracker.presentation.theme.ExpenseAppTheme
import com.expenseapp.salarytracker.presentation.transactions.TransactionsViewModel
import kotlinx.coroutines.launch

/**
 * Main Activity hosting the financial application.
 *
 * Privacy & Security Protections:
 * - Inherits from [FragmentActivity] to host AndroidX [BiometricPrompt].
 * - Dynamically toggles [WindowManager.LayoutParams.FLAG_SECURE] to prevent screen capture
 *   and sensitive recent-apps preview leakage in accordance with user security settings.
 * - Manages application backgrounding timestamp and enforces lock grace period on resume.
 */
class MainActivity : FragmentActivity() {

    private val app by lazy { application as ExpenseApplication }

    val securityViewModel: SecurityViewModel by viewModels {
        SecurityViewModel.Factory(
            secureStorage = app.secureStorage,
            biometricAuthManager = app.biometricAuthManager
        )
    }

    private val dashboardViewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(
            transactionRepository = app.transactionRepository,
            categoryRepository = app.categoryRepository,
            budgetRepository = app.budgetRepository,
            goalRepository = app.goalRepository,
            settingsRepository = app.settingsRepository,
            calculationEngine = app.calculationEngine,
            cycleCalculator = app.salaryCycleCalculator
        )
    }

    private val transactionsViewModel: TransactionsViewModel by viewModels {
        TransactionsViewModel.Factory(
            transactionRepository = app.transactionRepository,
            categoryRepository = app.categoryRepository,
            accountRepository = app.accountRepository
        )
    }

    private val pendingDestination = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
            com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.MAIN_ACTIVITY_CREATED
        )

        // If a startup crash report is detected from previous launch, immediately open isolated CrashRecoveryActivity
        if (com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.hasCrashReport(this)) {
            com.expenseapp.salarytracker.diagnostics.CrashRecoveryActivity.start(this)
            finish()
            return
        }

        // Safe Startup Bisection mode check
        val bisectionStage = com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.getBisectionStage(this)
        if (bisectionStage < com.expenseapp.salarytracker.diagnostics.StartupStage.STAGE_5_THEME.level) {
            com.expenseapp.salarytracker.diagnostics.BisectionStatusActivity.start(this, bisectionStage)
            finish()
            return
        }

        enableEdgeToEdge()
        handleNavigationIntent(intent)

        // Observe screenshot protection flag safely across lifecycle states
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                securityViewModel.uiState.collect { state ->
                    if (state.isScreenshotProtectionEnabled && !com.expenseapp.salarytracker.BuildConfig.DEBUG) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
            com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.SET_CONTENT_STARTED
        )
        setContent {
            com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
                com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.THEME_STARTED
            )
            ExpenseAppTheme {
                com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
                    com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.MAIN_SCREEN_STARTED
                )
                MainScreen(
                    app = app,
                    dashboardViewModel = dashboardViewModel,
                    transactionsViewModel = transactionsViewModel,
                    securityViewModel = securityViewModel,
                    pendingDestination = pendingDestination.value,
                    onDestinationConsumed = { pendingDestination.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: android.content.Intent?) {
        val dest = intent?.getStringExtra(com.expenseapp.salarytracker.data.notification.AppNotificationManager.EXTRA_DESTINATION)
        if (dest != null) {
            pendingDestination.value = dest
        }
    }

    override fun onStop() {
        super.onStop()
        securityViewModel.onAppMovedToBackground()
    }

    override fun onStart() {
        super.onStart()
        securityViewModel.onAppMovedToForeground()
    }
}


