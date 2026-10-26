package com.expenseapp.salarytracker.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Onboarding : Screen("onboarding", "Welcome", Icons.Outlined.Home)
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Outlined.PieChart)
    data object Transactions : Screen("transactions", "Activity", Icons.AutoMirrored.Outlined.ReceiptLong)
    data object Budgets : Screen("budgets", "Budgets", Icons.Outlined.Assessment)
    data object Goals : Screen("goals", "Goals", Icons.Outlined.Park)
    data object Reports : Screen("reports", "Reports", Icons.Outlined.Assessment)
    data object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)
    data object Accounts : Screen("accounts", "Wallets & Accounts", Icons.Outlined.AccountBalanceWallet)
    data object QuickAdd : Screen("quick_add", "Quick Add", Icons.Outlined.Home)

    companion object {
        val bottomNavScreens: List<Screen>
            get() = listOf(
                Dashboard,
                Transactions,
                Budgets,
                Goals,
                Settings
            )
    }
}
