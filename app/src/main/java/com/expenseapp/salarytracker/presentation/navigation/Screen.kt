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

import androidx.compose.material.icons.outlined.MoreHoriz

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Onboarding : Screen("onboarding", "Welcome", Icons.Outlined.Home)
    data object Dashboard : Screen("dashboard", "Home", Icons.Outlined.Home)
    data object Transactions : Screen("transactions", "Activity", Icons.AutoMirrored.Outlined.ReceiptLong)
    data object Budgets : Screen("budgets", "Budgets", Icons.Outlined.PieChart)
    data object Goals : Screen("goals", "Goals", Icons.Outlined.Park)
    data object Reports : Screen("reports", "Reports", Icons.Outlined.Assessment)
    data object Settings : Screen("settings", "More", Icons.Outlined.MoreHoriz)
    data object Accounts : Screen("accounts", "Where your money is", Icons.Outlined.AccountBalanceWallet)
    data object QuickAdd : Screen("quick_add", "Add expense", Icons.Outlined.Home)

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
