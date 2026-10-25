package com.expenseapp.salarytracker.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.model.TreeState
import com.expenseapp.salarytracker.domain.service.BudgetStatus
import com.expenseapp.salarytracker.domain.service.GoalProgress
import com.expenseapp.salarytracker.domain.service.TreeOfHopeStatus
import com.expenseapp.salarytracker.presentation.components.EmptyStateView
import com.expenseapp.salarytracker.presentation.components.MoneyDisplay
import com.expenseapp.salarytracker.presentation.theme.AppElevation
import com.expenseapp.salarytracker.presentation.theme.AppExpense
import com.expenseapp.salarytracker.presentation.theme.AppIncome
import com.expenseapp.salarytracker.presentation.theme.AppSaving
import com.expenseapp.salarytracker.presentation.theme.AppSpacing
import com.expenseapp.salarytracker.presentation.theme.AppTransfer
import com.expenseapp.salarytracker.presentation.theme.AppWarning
import com.expenseapp.salarytracker.presentation.theme.CategoryDefaultIcon
import com.expenseapp.salarytracker.presentation.theme.CategoryFoodIcon
import com.expenseapp.salarytracker.presentation.theme.CategoryGroceryIcon
import com.expenseapp.salarytracker.presentation.theme.CategoryTransportIcon
import com.expenseapp.salarytracker.presentation.theme.expenseTextColor
import com.expenseapp.salarytracker.presentation.theme.incomeTextColor
import com.expenseapp.salarytracker.presentation.theme.savingTextColor
import com.expenseapp.salarytracker.presentation.theme.warningTextColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// =============================================================================
//  EXECUTIVE AT-A-GLANCE DASHBOARD
//  Inspired by PowerPixel's executive command-center clarity:
//  1. Header + Period Filter Pill + Export Shortcut
//  2. 2x2 At-a-Glance KPI Metric Grid (Available, Spent, Safe Today, Growth/Saved)
//  3. Interactive 7-Day Cashflow & Spending Pulse Curve (Current vs Previous week)
//  4. Interactive Category Distribution Donut Ring + Legend
//  5. Active Budgets & Tree of Hope Compact Glance Row
//  6. Structured Recent Activity Table with Category & Account Metadata + Export
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToSetup: () -> Unit = {},
    onOpenQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
        com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.HOME_STARTED
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.recordCheckpoint(
            com.expenseapp.salarytracker.diagnostics.StartupCheckpoint.STARTUP_COMPLETED
        )
        com.expenseapp.salarytracker.diagnostics.StartupDiagnostics.markStartupCompletedNormally(context)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var periodMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = AppSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dashboard",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${state.periodLabel} · ${state.daysRemaining}d left in cycle",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Period Selector Dropdown Pill (inspired by PowerPixel date selector)
                        Box {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { periodMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = state.selectedTimeframe.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Select timeframe",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = periodMenuExpanded,
                                onDismissRequest = { periodMenuExpanded = false }
                            ) {
                                DashboardTimeframe.entries.forEach { tf ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = tf.label,
                                                fontWeight = if (tf == state.selectedTimeframe) FontWeight.Bold else FontWeight.Normal,
                                                color = if (tf == state.selectedTimeframe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            viewModel.selectTimeframe(tf)
                                            periodMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenQuickAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .size(56.dp)
                    .semantics { contentDescription = "Add expense" }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add expense",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = AppSpacing.lg,
                    end = AppSpacing.lg,
                    top = AppSpacing.xs,
                    bottom = AppSpacing.navClearance
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                // 1. Account Wallets Strip (quick horizontal glance + Wallets & Setup action)
                item {
                    AccountWalletsStrip(
                        accounts = state.accountBalances,
                        totalBalance = state.totalBalance,
                        currencySymbol = state.currencySymbol,
                        onManageWalletsClick = onNavigateToSetup
                    )
                }

                // 2. 2x2 At-a-Glance KPI Metric Grid
                item {
                    KpiQuadGridSection(
                        state = state,
                        onBudgetsClick = onNavigateToBudgets,
                        onTransactionsClick = onNavigateToTransactions,
                        onGoalsClick = onNavigateToGoals
                    )
                }

                // 3. 7-Day Spending & Cashflow Pulse Curve Card
                item {
                    SpendingPulseChartCard(
                        dailyTrend = state.dailyTrend,
                        currentWeekTotal = state.current7DaySpend,
                        previousWeekTotal = state.previous7DaySpend,
                        safeDailyAllowance = state.safeToSpend.safeAmountPerDay,
                        currencySymbol = state.currencySymbol,
                        onNavigateToReports = onNavigateToReports
                    )
                }

                // 4. Category Distribution Donut Chart + Interactive Breakdown Card
                item {
                    CategoryDonutBreakdownCard(
                        spendingCategories = state.topSpendingCategories,
                        incomeCategories = state.topIncomeCategories,
                        debtCategories = state.topDebtCategories,
                        totalSpent = state.summary.netExpense,
                        totalIncome = state.summary.totalIncome,
                        totalDebt = state.totalDebtFlow,
                        remainingBudget = state.safeToSpend.remainingBudget,
                        effectiveIncome = state.effectiveIncome,
                        currencySymbol = state.currencySymbol,
                        onNavigateToBudgets = onNavigateToBudgets
                    )
                }

                // 5. Budget Health & Tree of Hope Compact Glance Row
                item {
                    BudgetAndGoalsGlanceRow(
                        overallBudgetStatus = state.overallBudgetStatus,
                        categoryBudgets = state.categoryBudgets,
                        treeStatus = state.treeOfHopeStatus,
                        activeGoalsCount = state.activeGoals.size,
                        currencySymbol = state.currencySymbol,
                        onNavigateToBudgets = onNavigateToBudgets,
                        onNavigateToGoals = onNavigateToGoals
                    )
                }

                // 6. Recent Activity Structured Table with Export Shortcut
                item {
                    RecentActivityTableCard(
                        transactions = state.recentTransactions,
                        categoriesMap = state.categoriesMap,
                        accountsMap = state.accountsMap,
                        currencySymbol = state.currencySymbol,
                        onViewAllClick = onNavigateToTransactions,
                        onExportClick = onNavigateToReports,
                        onAddTransactionClick = onOpenQuickAdd
                    )
                }
            }
        }
    }
}

// =============================================================================
//  1. ACCOUNT WALLETS STRIP
// =============================================================================

@Composable
private fun AccountWalletsStrip(
    accounts: List<AccountBalanceItem>,
    totalBalance: MinorUnits,
    currencySymbol: String,
    onManageWalletsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Total Net Worth Pill
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Net Balance:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = totalBalance.formatCompact(currencySymbol),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Individual Account Pills
        accounts.forEach { item ->
            val dotColor = parseHexColor(item.account.colorHex, 2)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = item.account.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.balance.formatCompact(currencySymbol),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.balance.isNegative()) expenseTextColor() else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // "+ Wallets & Setup" Action Chip
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onManageWalletsClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Manage wallets and setup",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Wallets & Setup",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// =============================================================================
//  2. 2x2 AT-A-GLANCE KPI METRIC GRID
// =============================================================================

@Composable
private fun KpiQuadGridSection(
    state: DashboardUiState,
    onBudgetsClick: () -> Unit,
    onTransactionsClick: () -> Unit,
    onGoalsClick: () -> Unit
) {
    val currency = state.currencySymbol
    val limit = state.overallBudget?.limitAmount ?: state.effectiveIncome
    val remaining = state.safeToSpend.remainingBudget
    val remainingPct = if (limit.isPositive()) {
        ((remaining.amountInMinorUnits.toDouble() / limit.amountInMinorUnits.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    } else 100

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            // Card 1: Available Budget / Balance
            KpiStatCard(
                title = "Available",
                value = remaining.formatCompact(currency),
                badgeText = "$remainingPct% left",
                isBadgePositive = remainingPct > 20,
                subtitle = if (state.overallBudget != null) "Of ${limit.formatCompact(currency)} limit" else "Of ${limit.formatCompact(currency)} salary",
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = onBudgetsClick,
                modifier = Modifier.weight(1f)
            )

            // Card 2: Period Spending
            val spendDelta = state.spendDeltaPercent
            val spendBadge = when {
                spendDelta == null -> "${state.summary.transactionCount} items"
                spendDelta > 0 -> "+$spendDelta%"
                spendDelta < 0 -> "$spendDelta%"
                else -> "0%"
            }
            // For spending, lower vs previous period is positive (good)
            val isSpendGood = spendDelta == null || spendDelta <= 0
            KpiStatCard(
                title = "Spent",
                value = state.summary.netExpense.formatCompact(currency),
                badgeText = spendBadge,
                isBadgePositive = isSpendGood,
                subtitle = if (spendDelta != null) "vs previous period" else "In selected period",
                accentColor = AppExpense,
                onClick = onTransactionsClick,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            // Card 3: Safe to Spend Today
            val todaySpend = state.dailyTrend.lastOrNull()?.currentAmount ?: MinorUnits.ZERO
            val safeToday = state.safeToSpend.safeAmountPerDay
            val underDailyLimit = todaySpend <= safeToday || safeToday.isZero()
            KpiStatCard(
                title = "Safe today",
                value = safeToday.formatCompact(currency),
                badgeText = if (underDailyLimit) "On track" else "High today",
                isBadgePositive = underDailyLimit,
                subtitle = "${state.daysRemaining} days remaining",
                accentColor = AppIncome,
                onClick = onBudgetsClick,
                modifier = Modifier.weight(1f)
            )

            // Card 4: Savings & Retention Growth
            val rate = state.savingsRatePercent
            val savedAmount = if (state.summary.totalSavings.isPositive()) {
                state.summary.totalSavings
            } else if (state.effectiveIncome > state.summary.netExpense) {
                state.effectiveIncome - state.summary.netExpense
            } else {
                MinorUnits.ZERO
            }
            KpiStatCard(
                title = "Saved & kept",
                value = "+ $rate%",
                badgeText = savedAmount.formatCompact(currency),
                isBadgePositive = rate >= 15,
                subtitle = "Income retention rate",
                accentColor = AppSaving,
                onClick = onGoalsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    badgeText: String,
    isBadgePositive: Boolean,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = if (isBadgePositive) incomeTextColor() else expenseTextColor()
    val badgeBg = if (isBadgePositive) AppIncome.copy(alpha = 0.10f) else AppExpense.copy(alpha = 0.10f)
    val badgeArrow = if (isBadgePositive) "↑" else "↓"

    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.none)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = "$badgeArrow $badgeText",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =============================================================================
//  3. 7-DAY SPENDING PULSE CHART CARD (Inspired by PowerPixel's Revenue Curve)
// =============================================================================

@Composable
private fun SpendingPulseChartCard(
    dailyTrend: List<DailyTrendPoint>,
    currentWeekTotal: MinorUnits,
    previousWeekTotal: MinorUnits,
    safeDailyAllowance: MinorUnits,
    currencySymbol: String,
    onNavigateToReports: () -> Unit
) {
    var selectedDayIndex by remember(dailyTrend) {
        val peakIdx = dailyTrend.indices.maxByOrNull { dailyTrend[it].currentAmount.amountInMinorUnits } ?: dailyTrend.lastIndex
        mutableIntStateOf(if (peakIdx >= 0) peakIdx else 0)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.none)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending pulse & daily pace",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Analytics →",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onNavigateToReports() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Current 7d vs Daily Safe Limit Summary Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current 7 days
                Column {
                    Text(
                        text = "Last 7 days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = currentWeekTotal.formatCompact(currencySymbol),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Daily Safe Pace or Previous 7 days
                Column {
                    Text(
                        text = if (previousWeekTotal.isPositive()) "Prev 7 days" else "Safe daily pace",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AppIncome)
                        )
                        Text(
                            text = if (previousWeekTotal.isPositive()) {
                                previousWeekTotal.formatCompact(currencySymbol)
                            } else {
                                "${safeDailyAllowance.formatCompact(currencySymbol)}/d"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Selected Day Callout Badge (like PowerPixel "$ 18,542")
                if (dailyTrend.isNotEmpty() && selectedDayIndex in dailyTrend.indices) {
                    val selectedPt = dailyTrend[selectedDayIndex]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = selectedPt.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = selectedPt.currentAmount.formatCompact(currencySymbol),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Smooth Bezier Curve Canvas with Y-Axis Scale Labels
            if (dailyTrend.isNotEmpty()) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val safeLineColor = AppIncome.copy(alpha = 0.65f)
                val prevLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                val surfaceColor = MaterialTheme.colorScheme.surface

                val maxVal = remember(dailyTrend, safeDailyAllowance) {
                    val maxCurrent = dailyTrend.maxOfOrNull { it.currentAmount.amountInMinorUnits } ?: 0L
                    val maxPrev = dailyTrend.maxOfOrNull { it.previousAmount.amountInMinorUnits } ?: 0L
                    val safeUnits = safeDailyAllowance.amountInMinorUnits
                    val ceiling = maxOf(maxCurrent, maxPrev, safeUnits * 15L / 10L)
                    if (ceiling <= 0L) 200000L else (ceiling * 1.2f).toLong()
                }

                val midVal = maxVal / 2L

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Y-Axis Scale Labels (Top, Mid, 0) like PowerPixel 20000 / 10000 / 0
                    Column(
                        modifier = Modifier
                            .width(46.dp)
                            .fillMaxSize()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = MinorUnits(maxVal).formatAxisCompact(currencySymbol),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = MinorUnits(midVal).formatAxisCompact(currencySymbol),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "${currencySymbol}0",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Graph Plot Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            val topPad = 12.dp.toPx()
                            val bottomPad = 10.dp.toPx()
                            val usableHeight = (height - topPad - bottomPad).coerceAtLeast(1f)
                            val count = dailyTrend.size
                            val stepX = if (count > 1) width / (count - 1).toFloat() else width

                            // Horizontal subtle grid lines (3 levels: max, mid, zero)
                            for (i in 0..2) {
                                val y = topPad + (usableHeight * i / 2f)
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(width, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                            }

                            fun pointOffset(index: Int, amountUnits: Long): Offset {
                                val x = index * stepX
                                val ratio = (amountUnits.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                                val y = topPad + usableHeight * (1f - ratio)
                                return Offset(x, y)
                            }

                            // Draw Safe Daily Allowance Reference Line (Always visible when safeDailyAllowance > 0)
                            if (safeDailyAllowance.isPositive()) {
                                val safeRatio = (safeDailyAllowance.amountInMinorUnits.toFloat() / maxVal.toFloat()).coerceIn(0.08f, 0.92f)
                                val safeY = topPad + usableHeight * (1f - safeRatio)

                                // Subtle safe zone fill below safeY
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            safeLineColor.copy(alpha = 0.08f),
                                            safeLineColor.copy(alpha = 0.01f)
                                        ),
                                        startY = safeY,
                                        endY = topPad + usableHeight
                                    ),
                                    topLeft = Offset(0f, safeY),
                                    size = Size(width, (topPad + usableHeight - safeY).coerceAtLeast(1f))
                                )

                                drawLine(
                                    color = safeLineColor,
                                    start = Offset(0f, safeY),
                                    end = Offset(width, safeY),
                                    strokeWidth = 1.8.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )
                            }

                            val prevPoints = dailyTrend.mapIndexed { idx, pt ->
                                pointOffset(idx, pt.previousAmount.amountInMinorUnits)
                            }
                            val currPoints = dailyTrend.mapIndexed { idx, pt ->
                                pointOffset(idx, pt.currentAmount.amountInMinorUnits)
                            }

                            // Draw Previous Week dashed smooth curve if previous week has spend
                            if (previousWeekTotal.isPositive() && prevPoints.size > 1) {
                                val prevPath = Path().apply {
                                    moveTo(prevPoints.first().x, prevPoints.first().y)
                                    for (i in 0 until prevPoints.lastIndex) {
                                        val p0 = prevPoints[i]
                                        val p1 = prevPoints[i + 1]
                                        val ctrlX = (p0.x + p1.x) / 2f
                                        cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                                    }
                                }
                                drawPath(
                                    path = prevPath,
                                    color = prevLineColor,
                                    style = Stroke(
                                        width = 1.8.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                    )
                                )
                            }

                            // Draw Current Week smooth curve + gradient fill
                            if (currPoints.size > 1) {
                                val currPath = Path().apply {
                                    moveTo(currPoints.first().x, currPoints.first().y)
                                    for (i in 0 until currPoints.lastIndex) {
                                        val p0 = currPoints[i]
                                        val p1 = currPoints[i + 1]
                                        val ctrlX = (p0.x + p1.x) / 2f
                                        cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                                    }
                                }

                                val fillPath = Path().apply {
                                    addPath(currPath)
                                    lineTo(currPoints.last().x, topPad + usableHeight)
                                    lineTo(currPoints.first().x, topPad + usableHeight)
                                    close()
                                }

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            primaryColor.copy(alpha = 0.24f),
                                            primaryColor.copy(alpha = 0.02f)
                                        ),
                                        startY = topPad,
                                        endY = topPad + usableHeight
                                    )
                                )

                                drawPath(
                                    path = currPath,
                                    color = primaryColor,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Draw data point nodes and highlight selected node
                                currPoints.forEachIndexed { idx, pt ->
                                    val isSelected = idx == selectedDayIndex
                                    if (isSelected) {
                                        drawLine(
                                            color = primaryColor.copy(alpha = 0.35f),
                                            start = Offset(pt.x, pt.y),
                                            end = Offset(pt.x, topPad + usableHeight),
                                            strokeWidth = 1.5.dp.toPx()
                                        )
                                        drawCircle(
                                            color = primaryColor,
                                            radius = 6.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = surfaceColor,
                                            radius = 3.dp.toPx(),
                                            center = pt
                                        )
                                    } else {
                                        drawCircle(
                                            color = surfaceColor,
                                            radius = 3.5.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = primaryColor.copy(alpha = 0.75f),
                                            radius = 3.5.dp.toPx(),
                                            center = pt,
                                            style = Stroke(width = 1.5.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Interactive Day Selector Labels (Mon, Tue, Wed...) aligned with plot area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dailyTrend.forEachIndexed { idx, pt ->
                        val isSelected = idx == selectedDayIndex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable { selectedDayIndex = idx }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pt.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
//  4. CATEGORY DISTRIBUTION & BUDGET ALLOCATION DONUT RING + LEGEND
// =============================================================================

@Composable
private fun CategoryDonutBreakdownCard(
    spendingCategories: List<CategorySpend>,
    incomeCategories: List<CategorySpend>,
    debtCategories: List<CategorySpend>,
    totalSpent: MinorUnits,
    totalIncome: MinorUnits,
    totalDebt: MinorUnits,
    remainingBudget: MinorUnits,
    effectiveIncome: MinorUnits,
    currencySymbol: String,
    onNavigateToBudgets: () -> Unit
) {
    var activeModeIndex by remember { mutableIntStateOf(0) } // 0: Spending, 1: Income, 2: Debt

    val displaySlices = remember(activeModeIndex, spendingCategories, incomeCategories, debtCategories, totalSpent, totalIncome, totalDebt, remainingBudget, effectiveIncome) {
        when (activeModeIndex) {
            0 -> {
                // SPENDING & BUDGET
                if (spendingCategories.isNotEmpty()) {
                    val mutable = spendingCategories.toMutableList()
                    if (remainingBudget.isPositive()) {
                        val totalPool = (totalSpent + remainingBudget).amountInMinorUnits.coerceAtLeast(1L)
                        val normalized = mutable.map { cat ->
                            val pct = ((cat.amount.amountInMinorUnits.toDouble() / totalPool.toDouble()) * 100.0).toInt().coerceAtLeast(2)
                            cat.copy(percentage = pct)
                        }.toMutableList()
                        val remPct = (100 - normalized.sumOf { it.percentage }).coerceAtLeast(5)
                        normalized.add(
                            CategorySpend(
                                categoryId = -1L,
                                name = "Available budget",
                                colorHex = "#10B981",
                                iconName = "savings",
                                amount = remainingBudget,
                                percentage = remPct,
                                transactionCount = 0
                            )
                        )
                        normalized
                    } else {
                        mutable
                    }
                } else {
                    val avail = if (remainingBudget.isPositive()) remainingBudget else effectiveIncome
                    val unallocatedSavings = if (effectiveIncome > avail && avail.isPositive()) {
                        effectiveIncome - avail
                    } else {
                        MinorUnits(avail.amountInMinorUnits / 4L)
                    }
                    val availPct = if ((avail + unallocatedSavings).isPositive()) {
                        ((avail.amountInMinorUnits.toDouble() / (avail + unallocatedSavings).amountInMinorUnits.toDouble()) * 100.0).toInt().coerceIn(10, 90)
                    } else 75

                    listOf(
                        CategorySpend(
                            categoryId = -1L,
                            name = "Available budget",
                            colorHex = "#10B981",
                            iconName = "savings",
                            amount = avail,
                            percentage = availPct,
                            transactionCount = 0
                        ),
                        CategorySpend(
                            categoryId = -2L,
                            name = "Savings buffer",
                            colorHex = "#3B82F6",
                            iconName = "payments",
                            amount = unallocatedSavings,
                            percentage = (100 - availPct).coerceAtLeast(10),
                            transactionCount = 0
                        ),
                        CategorySpend(
                            categoryId = -3L,
                            name = "Spent so far",
                            colorHex = "#F59E0B",
                            iconName = "receipt",
                            amount = totalSpent,
                            percentage = 4,
                            transactionCount = 0
                        )
                    )
                }
            }
            1 -> {
                // INCOME
                if (incomeCategories.isNotEmpty()) {
                    incomeCategories
                } else {
                    listOf(
                        CategorySpend(
                            categoryId = -10L,
                            name = "Monthly Salary",
                            colorHex = "#10B981",
                            iconName = "payments",
                            amount = if (totalIncome.isPositive()) totalIncome else effectiveIncome,
                            percentage = 100,
                            transactionCount = 1
                        )
                    )
                }
            }
            else -> {
                // DEBTS & LOANS
                if (debtCategories.isNotEmpty()) {
                    debtCategories
                } else {
                    listOf(
                        CategorySpend(
                            categoryId = -20L,
                            name = "No debts this cycle",
                            colorHex = "#8B5CF6",
                            iconName = "handshake",
                            amount = totalDebt,
                            percentage = 100,
                            transactionCount = 0
                        )
                    )
                }
            }
        }
    }

    var selectedSliceIndex by remember(displaySlices) { mutableIntStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.none)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            // Header Row: Title & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val headerTitle = when (activeModeIndex) {
                    0 -> if (spendingCategories.isNotEmpty()) "Where money went & left" else "Cycle budget breakdown"
                    1 -> "Income sources & inflow"
                    else -> "Debts & loans breakdown"
                }

                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Budgets â†’",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onNavigateToBudgets() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3-Pill Interactive Category View Switcher (Spending | Income | Debts)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    Triple(0, "Spending", Color(0xFFF97316)),
                    Triple(1, "Income", Color(0xFF10B981)),
                    Triple(2, "Debts & Loans", Color(0xFF8B5CF6))
                )

                tabs.forEach { (modeIdx, label, themeColor) ->
                    val isSelected = activeModeIndex == modeIdx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) themeColor.copy(alpha = 0.16f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) themeColor else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                activeModeIndex = modeIdx
                                selectedSliceIndex = 0
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val activeCat = displaySlices.getOrNull(selectedSliceIndex) ?: displaySlices.first()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg)
            ) {
                // Donut Chart with Center Callout
                Box(
                    modifier = Modifier.size(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val totalPct = displaySlices.sumOf { it.percentage }.coerceAtLeast(1)

                    Canvas(modifier = Modifier.size(120.dp)) {
                        val strokeWidth = 18.dp.toPx()
                        val selectedStrokeWidth = 22.dp.toPx()
                        val diameter = size.minDimension - selectedStrokeWidth
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )
                        val arcSize = Size(diameter, diameter)

                        var startAngle = -90f
                        val gapDegrees = if (displaySlices.size > 1) 3f else 0f

                        displaySlices.forEachIndexed { index, cat ->
                            val sweep = ((cat.percentage.toFloat() / totalPct.toFloat()) * 360f - gapDegrees)
                                .coerceAtLeast(5f)
                            val sliceColor = parseHexColor(cat.colorHex, index)
                            val isSelected = index == selectedSliceIndex

                            drawArc(
                                color = sliceColor,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = if (isSelected) selectedStrokeWidth else strokeWidth,
                                    cap = StrokeCap.Butt
                                )
                            )
                            startAngle += sweep + gapDegrees
                        }
                    }

                    // Center Callout (Selected Slice Amount + Label)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = activeCat.amount.formatCompact(currencySymbol),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeCat.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = parseHexColor(activeCat.colorHex, selectedSliceIndex)
                        )
                    }
                }

                // Interactive Category / Allocation Legend List
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    displaySlices.forEachIndexed { idx, item ->
                        val isSelected = idx == selectedSliceIndex
                        val dotColor = parseHexColor(item.colorHex, idx)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    else Color.Transparent
                                )
                                .clickable { selectedSliceIndex = idx }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(dotColor.copy(alpha = if (isSelected) 0.3f else 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.amount.formatCompact(currencySymbol),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================//  5. BUDGET HEALTH & TREE OF HOPE GLANCE ROW
// =============================================================================

@Composable
private fun BudgetAndGoalsGlanceRow(
    overallBudgetStatus: BudgetStatus?,
    categoryBudgets: List<BudgetStatus>,
    treeStatus: TreeOfHopeStatus,
    activeGoalsCount: Int,
    currencySymbol: String,
    onNavigateToBudgets: () -> Unit,
    onNavigateToGoals: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Left Card: Budget Ceiling & Alerts
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .clickable { onNavigateToBudgets() },
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.none)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budget health",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Outlined.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val primaryStatus = overallBudgetStatus ?: categoryBudgets.firstOrNull()
                if (primaryStatus != null) {
                    val pctUsed = primaryStatus.percentageUsed.toInt().coerceIn(0, 999)
                    val barProgress = (primaryStatus.percentageUsed / 100.0).toFloat().coerceIn(0f, 1f)
                    val statusColor = when {
                        primaryStatus.isOverBudget -> AppExpense
                        primaryStatus.isWarning -> AppWarning
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Text(
                        text = "$pctUsed% used",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (primaryStatus.isOverBudget) expenseTextColor() else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { barProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (primaryStatus.isOverBudget) {
                            "Over by ${(-primaryStatus.variance).formatCompact(currencySymbol)}"
                        } else {
                            "${primaryStatus.remainingAmount.formatCompact(currencySymbol)} left"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "No limit set",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to set a monthly or category limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Right Card: Tree of Hope & Active Goals
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .clickable { onNavigateToGoals() },
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.none)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tree of Hope",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Outlined.Park,
                        contentDescription = null,
                        tint = AppIncome,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = treeStatus.stageName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val vitalityProgress = (treeStatus.scorePercentage / 100.0).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { vitalityProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AppIncome,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (activeGoalsCount > 0) {
                        "$activeGoalsCount active goal(s) · ${treeStatus.scorePercentage.toInt()}% vitality"
                    } else {
                        "Tap to plant a savings goal"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =============================================================================
//  6. RECENT ACTIVITY STRUCTURED TABLE (Inspired by PowerPixel Top Selling Table)
// =============================================================================

@Composable
private fun RecentActivityTableCard(
    transactions: List<Transaction>,
    categoriesMap: Map<Long, Category>,
    accountsMap: Map<Long, Account>,
    currencySymbol: String,
    onViewAllClick: () -> Unit,
    onExportClick: () -> Unit,
    onAddTransactionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.none)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            // Card Header with Export & View All actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export shortcut button (like PowerPixel "Export ↓")
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppExpense.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onExportClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Export",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = expenseTextColor()
                            )
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = "Export reports",
                                tint = expenseTextColor(),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    if (transactions.isNotEmpty()) {
                        Text(
                            text = "View all",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onViewAllClick() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (transactions.isEmpty()) {
                EmptyStateView(
                    title = "No transactions yet",
                    message = "Add an expense or income to see your activity ledger at a glance.",
                    actionButtonText = "Add expense",
                    onActionClick = onAddTransactionClick,
                    modifier = Modifier.padding(vertical = AppSpacing.sm)
                )
            } else {
                // Table Column Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ITEM / CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.8f)
                    )
                    Text(
                        text = "DATE / ACCOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = "AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                transactions.forEachIndexed { index, tx ->
                    val category = tx.categoryId?.let { categoriesMap[it] }
                    val account = accountsMap[tx.accountId]

                    DashboardTableTransactionRow(
                        transaction = tx,
                        category = category,
                        account = account,
                        currencySymbol = currencySymbol
                    )
                    if (index < transactions.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTableTransactionRow(
    transaction: Transaction,
    category: Category?,
    account: Account?,
    currencySymbol: String
) {
    val isIncome = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.REFUND
    val isTransfer = transaction.type == TransactionType.TRANSFER
    val isSaving = transaction.type == TransactionType.SAVING

    val (iconColor, icon) = when {
        isIncome -> Pair(AppIncome, Icons.Outlined.ArrowUpward)
        isTransfer -> Pair(AppTransfer, Icons.Outlined.SwapHoriz)
        isSaving -> Pair(AppSaving, Icons.Outlined.Savings)
        category != null -> Pair(parseHexColor(category.colorHex, 0), resolveCategoryIcon(category.iconName))
        else -> Pair(CategoryDefaultIcon, Icons.AutoMirrored.Outlined.ReceiptLong)
    }

    val containerBg = iconColor.copy(alpha = 0.12f)
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()) }

    val primaryTitle = transaction.note.ifBlank {
        category?.name ?: transaction.type.displayName
    }
    val secondarySubtitle = category?.name ?: transaction.type.displayName

    val amountColor = when {
        isIncome -> incomeTextColor()
        isTransfer -> MaterialTheme.colorScheme.onSurfaceVariant
        isSaving -> savingTextColor()
        else -> MaterialTheme.colorScheme.onSurface
    }
    val amountPrefix = when {
        isIncome -> "+"
        isTransfer -> ""
        else -> "-"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: Icon + Item / Category
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.8f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(containerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = primaryTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = secondarySubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Column 2: Date & Account
        Column(
            modifier = Modifier.weight(1.2f)
        ) {
            Text(
                text = transaction.date.format(dateFmt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = account?.name ?: transaction.type.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Column 3: Formatted Amount
        Text(
            text = "$amountPrefix${transaction.amount.formatCompact(currencySymbol)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
    }
}

// =============================================================================
//  HELPERS: Color & Icon Resolution
// =============================================================================

private fun MinorUnits.formatCompact(currencySymbol: String): String {
    return if (amountInMinorUnits % 100L == 0L) {
        format(currencySymbol, decimals = 2).removeSuffix(".00")
    } else {
        format(currencySymbol, decimals = 2)
    }
}

private fun MinorUnits.formatAxisCompact(currencySymbol: String): String {
    val major = amountInMinorUnits / 100L
    return when {
        major >= 1000L -> {
            val k = major / 1000.0
            if (major % 1000L == 0L) "${currencySymbol}${major / 1000L}k"
            else String.format(Locale.US, "%s%.1fk", currencySymbol, k)
        }
        else -> "$currencySymbol$major"
    }
}

private val FallbackChartPalette = listOf(
    Color(0xFF1B382B),
    Color(0xFFD97706),
    Color(0xFF3B82F6),
    Color(0xFF059669),
    Color(0xFF9E4770),
    Color(0xFF476582)
)

private fun parseHexColor(hex: String, fallbackIndex: Int): Color {
    return try {
        if (hex.isNotBlank()) {
            Color(android.graphics.Color.parseColor(hex))
        } else {
            FallbackChartPalette[fallbackIndex % FallbackChartPalette.size]
        }
    } catch (_: Exception) {
        FallbackChartPalette[fallbackIndex % FallbackChartPalette.size]
    }
}

private fun resolveCategoryIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "directions_bus", "transport" -> Icons.Outlined.DirectionsBus
        "restaurant", "food" -> Icons.Outlined.Restaurant
        "shopping_cart", "groceries" -> Icons.Outlined.ShoppingCart
        "home", "rent" -> Icons.Outlined.Home
        "bolt", "utilities" -> Icons.Outlined.Bolt
        "smartphone", "mobile" -> Icons.Outlined.Smartphone
        "wifi", "internet" -> Icons.Outlined.Wifi
        "shopping_bag", "shopping" -> Icons.Outlined.ShoppingBag
        "movie", "entertainment" -> Icons.Outlined.Movie
        "medical_services", "health" -> Icons.Outlined.MedicalServices
        "school", "education" -> Icons.Outlined.School
        "work", "office" -> Icons.Outlined.Work
        "volunteer_activism", "donation" -> Icons.Outlined.VolunteerActivism
        "savings" -> Icons.Outlined.Savings
        "credit_card", "debt" -> Icons.Outlined.CreditCard
        "payments", "salary" -> Icons.Outlined.Payments
        else -> Icons.AutoMirrored.Outlined.ReceiptLong
    }
}
