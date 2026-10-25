package com.expenseapp.salarytracker.presentation.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expenseapp.salarytracker.ExpenseApplication
import com.expenseapp.salarytracker.presentation.budgets.BudgetsScreen
import com.expenseapp.salarytracker.presentation.components.QuickAddDialog
import com.expenseapp.salarytracker.presentation.dashboard.DashboardScreen
import com.expenseapp.salarytracker.presentation.dashboard.DashboardViewModel
import com.expenseapp.salarytracker.presentation.goals.GoalsScreen
import com.expenseapp.salarytracker.presentation.navigation.Screen
import com.expenseapp.salarytracker.presentation.reports.ReportsScreen
import com.expenseapp.salarytracker.presentation.settings.SettingsScreen
import com.expenseapp.salarytracker.presentation.transactions.TransactionsScreen
import com.expenseapp.salarytracker.presentation.transactions.TransactionsViewModel
import kotlinx.coroutines.launch

import com.expenseapp.salarytracker.presentation.security.AppLockScreen
import com.expenseapp.salarytracker.presentation.security.SecurityViewModel

@Composable
fun MainScreen(
    app: ExpenseApplication,
    dashboardViewModel: DashboardViewModel,
    transactionsViewModel: TransactionsViewModel,
    securityViewModel: SecurityViewModel,
    pendingDestination: String? = null,
    onDestinationConsumed: () -> Unit = {}
) {
    com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
        com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.NAVIGATION_STARTED
    )
    val securityState by securityViewModel.uiState.collectAsStateWithLifecycle()

    if (securityState.isLocked) {
        AppLockScreen(
            securityViewModel = securityViewModel,
            biometricAuthManager = app.biometricAuthManager,
            onUnlocked = { securityViewModel.unlockSession() }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route
    val scope = rememberCoroutineScope()

    var showQuickAddDialog by remember { mutableStateOf(false) }
    var reportInitialPeriod by remember { mutableStateOf<com.expenseapp.salarytracker.domain.model.report.ReportPeriodType?>(null) }

    androidx.compose.runtime.LaunchedEffect(securityState.isLocked, pendingDestination) {
        if (!securityState.isLocked && pendingDestination != null) {
            when (pendingDestination) {
                com.expenseapp.salarytracker.data.notification.AppNotificationManager.DEST_ADD_EXPENSE -> {
                    showQuickAddDialog = true
                    onDestinationConsumed()
                }
                com.expenseapp.salarytracker.data.notification.AppNotificationManager.DEST_VIEW_TODAY_REPORT -> {
                    reportInitialPeriod = com.expenseapp.salarytracker.domain.model.report.ReportPeriodType.TODAY
                    navController.navigate(Screen.Reports.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onDestinationConsumed()
                }
                com.expenseapp.salarytracker.data.notification.AppNotificationManager.DEST_BUDGETS -> {
                    navController.navigate(Screen.Budgets.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onDestinationConsumed()
                }
                com.expenseapp.salarytracker.data.notification.AppNotificationManager.DEST_GOALS -> {
                    navController.navigate(Screen.Goals.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onDestinationConsumed()
                }
            }
        }
    }

    val hasCompletedOnboarding by app.settingsRepository.hasCompletedOnboarding().collectAsStateWithLifecycle(initialValue = null)
    var isExistingUserChecked by remember { mutableStateOf(false) }
    var isExistingUser by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val txCount = app.database.transactionDao().getCount()
        isExistingUser = txCount > 0
        if (isExistingUser) {
            app.settingsRepository.setCompletedOnboarding(true)
        }
        isExistingUserChecked = true
    }

    androidx.compose.runtime.LaunchedEffect(hasCompletedOnboarding, isExistingUserChecked) {
        if (isExistingUserChecked && hasCompletedOnboarding == false && !isExistingUser) {
            navController.navigate(Screen.Onboarding.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val categories by app.categoryRepository.getAllCategories().collectAsStateWithLifecycle(emptyList())
    val accounts by app.accountRepository.getAllAccounts().collectAsStateWithLifecycle(emptyList())

    val shouldShowBottomBar = currentRoute in Screen.bottomNavScreens.map { it.route }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Screen.bottomNavScreens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(imageVector = screen.icon, contentDescription = screen.title)
                            },
                            label = {
                                Text(text = screen.title, style = MaterialTheme.typography.labelSmall)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                com.expenseapp.salarytracker.presentation.onboarding.OnboardingScreen(
                    app = app,
                    onFinishOnboarding = { triggerAddExpense ->
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        if (triggerAddExpense) {
                            showQuickAddDialog = true
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToBudgets = {
                        navController.navigate(Screen.Budgets.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToGoals = {
                        navController.navigate(Screen.Goals.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToReports = {
                        navController.navigate(Screen.Reports.route)
                    },
                    onNavigateToSetup = {
                        navController.navigate(Screen.Onboarding.route)
                    },
                    onOpenQuickAdd = { showQuickAddDialog = true }
                )
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    viewModel = transactionsViewModel,
                    onOpenAddTransaction = { showQuickAddDialog = true }
                )
            }
            composable(Screen.Budgets.route) {
                BudgetsScreen()
            }
            composable(Screen.Goals.route) {
                GoalsScreen()
            }
            composable(Screen.Reports.route) {
                ReportsScreen(initialPeriodType = reportInitialPeriod)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    securityViewModel = securityViewModel,
                    settingsRepository = app.settingsRepository,
                    onNavigateToReports = {
                        navController.navigate(Screen.Reports.route)
                    }
                )
            }
        }

        if (showQuickAddDialog) {
            QuickAddDialog(
                categories = categories,
                accounts = accounts,
                onDismiss = { showQuickAddDialog = false },
                onSaveTransaction = { tx ->
                    scope.launch {
                        app.transactionRepository.insertTransaction(tx)
                        app.budgetNotificationService.evaluateBudgets(tx.date)
                    }
                },
                onSaveRecurringRule = { rule ->
                    scope.launch {
                        app.recurringRuleRepository.insertOrUpdate(rule)
                    }
                }
            )
        }
    }
}
