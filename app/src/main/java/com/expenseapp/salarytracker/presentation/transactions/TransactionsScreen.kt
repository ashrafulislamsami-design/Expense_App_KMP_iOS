package com.expenseapp.salarytracker.presentation.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.presentation.components.EmptyStateView
import com.expenseapp.salarytracker.presentation.theme.AppBorder
import com.expenseapp.salarytracker.presentation.theme.AppDebt
import com.expenseapp.salarytracker.presentation.theme.AppDonation
import com.expenseapp.salarytracker.presentation.theme.AppElevation
import com.expenseapp.salarytracker.presentation.theme.AppExpense
import com.expenseapp.salarytracker.presentation.theme.AppIncome
import com.expenseapp.salarytracker.presentation.theme.AppRefund
import com.expenseapp.salarytracker.presentation.theme.AppSaving
import com.expenseapp.salarytracker.presentation.theme.AppSecondary
import com.expenseapp.salarytracker.presentation.theme.AppSpacing
import com.expenseapp.salarytracker.presentation.theme.AppTransfer
import com.expenseapp.salarytracker.presentation.theme.CategoryDefaultIcon
import com.expenseapp.salarytracker.presentation.theme.CategoryFoodIcon
import com.expenseapp.salarytracker.presentation.theme.CategoryGroceryIcon
import com.expenseapp.salarytracker.presentation.theme.CategoryTransportIcon
import com.expenseapp.salarytracker.presentation.theme.expenseTextColor
import com.expenseapp.salarytracker.presentation.theme.incomeTextColor
import com.expenseapp.salarytracker.presentation.theme.savingTextColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// =============================================================================
//  TRANSACTIONS / LEDGER SCREEN (Phase 3D-C Redesign)
//
//  Answers: "What transactions happened, when, where, and for how much?"
//  Features:
//  - Clean search bar with instant clear action
//  - Type, Category, and Account filter chip carousels
//  - Chronological date-grouped cards with accurate daily totals
//  - Semantic icons & signed colors across all 9 transaction types
//  - Explicit "Source -> Destination" transfer presentation
//  - Visual distinction for refunds (reversals, not ordinary income)
//  - Delete confirmation safeguard dialog
//  - Calm, informative empty states
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onOpenAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val filteredList = remember(state.transactions, selectedTypeFilter) {
        if (selectedTypeFilter == null) state.transactions
        else state.transactions.filter { it.type == selectedTypeFilter }
    }

    // Group transactions by date descending
    val groupedTransactions = remember(filteredList) {
        filteredList.groupBy { it.date }
            .toSortedMap(compareByDescending { it })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Recent spending & income",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Activity",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                        ) {
                            Text(
                                text = "${filteredList.size} items",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
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
                onClick = onOpenAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.semantics { contentDescription = "Add transaction" }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add transaction",
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppSpacing.lg)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        text = "Search by note, category...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchQueryChanged("") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Filter Chips Carousel (Type, Category, and Account filters)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isAllActive = selectedTypeFilter == null && state.selectedCategoryId == null && state.selectedAccountId == null
                    LedgerFilterChip(
                        label = "All",
                        isSelected = isAllActive,
                        onClick = {
                            selectedTypeFilter = null
                            viewModel.clearFilters()
                        }
                    )
                }
                item {
                    LedgerFilterChip(
                        label = "Expenses",
                        isSelected = selectedTypeFilter == TransactionType.EXPENSE,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE }
                    )
                }
                item {
                    LedgerFilterChip(
                        label = "Income",
                        isSelected = selectedTypeFilter == TransactionType.INCOME,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME }
                    )
                }
                item {
                    LedgerFilterChip(
                        label = "Transfers",
                        isSelected = selectedTypeFilter == TransactionType.TRANSFER,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.TRANSFER) null else TransactionType.TRANSFER }
                    )
                }
                item {
                    LedgerFilterChip(
                        label = "Refunds",
                        isSelected = selectedTypeFilter == TransactionType.REFUND,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.REFUND) null else TransactionType.REFUND }
                    )
                }

                // Account Filter Chips
                items(state.accounts) { acc ->
                    LedgerFilterChip(
                        label = "Acc: ${acc.name}",
                        isSelected = state.selectedAccountId == acc.id,
                        onClick = { viewModel.onAccountFilterSelected(acc.id) }
                    )
                }

                // Category Filter Chips
                items(state.categories) { cat ->
                    LedgerFilterChip(
                        label = cat.name,
                        isSelected = state.selectedCategoryId == cat.id,
                        onClick = { viewModel.onCategoryFilterSelected(cat.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Transaction List / Empty State
            if (groupedTransactions.isEmpty()) {
                val isFiltered = state.searchQuery.isNotEmpty() || selectedTypeFilter != null ||
                    state.selectedCategoryId != null || state.selectedAccountId != null
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFiltered) {
                        EmptyStateView(
                            title = "No matching activity",
                            message = "No entries match your search query or selected filter.",
                            actionButtonText = "Clear filters",
                            onActionClick = {
                                selectedTypeFilter = null
                                viewModel.clearFilters()
                            }
                        )
                    } else {
                        EmptyStateView(
                            title = "No activity yet",
                            message = "Your expenses and other money movements will appear here.",
                            actionButtonText = "Add expense",
                            onActionClick = onOpenAddTransaction
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    contentPadding = PaddingValues(bottom = AppSpacing.navClearance)
                ) {
                    groupedTransactions.forEach { (date, txList) ->
                        item(key = date.toString()) {
                            DateGroupedTransactionSection(
                                date = date,
                                transactions = txList,
                                categories = state.categories,
                                accounts = state.accounts,
                                onDeleteClick = { tx -> transactionToDelete = tx }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (transactionToDelete != null) {
        val tx = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Text(
                    text = "Delete entry?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${tx.note.ifBlank { tx.type.displayName }}'? This will update your balances and pay period totals.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
//  FILTER CHIP
// =============================================================================

@Composable
fun LedgerFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    MaterialTheme.shapes.extraLarge
                )
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// =============================================================================
//  DATE GROUPED TRANSACTION SECTION
// =============================================================================

@Composable
fun DateGroupedTransactionSection(
    date: LocalDate,
    transactions: List<Transaction>,
    categories: List<com.expenseapp.salarytracker.domain.model.Category>,
    accounts: List<com.expenseapp.salarytracker.domain.model.Account>,
    onDeleteClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val today = LocalDate.now()
    val dateLabel = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    }

    // Daily net spending calculation:
    // Outflows: EXPENSE, DONATION, DEBT_PAYMENT
    // Inflows/Reversals reducing expense: REFUND
    // Transfers, Income, Debt Received, Savings, and Adjustments do NOT count as daily expense spending.
    val dayOutflows = transactions
        .filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.DONATION || it.type == TransactionType.DEBT_PAYMENT }
        .fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }
    val dayRefunds = transactions
        .filter { it.type == TransactionType.REFUND }
        .fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }
    val netDayExpenses = if (dayOutflows >= dayRefunds) dayOutflows - dayRefunds else MinorUnits.ZERO

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.xs, horizontal = AppSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (netDayExpenses.isPositive()) {
                Text(
                    text = "−${netDayExpenses.format()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.xs))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.none)
        ) {
            Column(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)) {
                transactions.forEachIndexed { index, tx ->
                    val categoryName = tx.categoryId?.let { categoryMap[it]?.name }
                    val sourceAccountName = accountMap[tx.accountId]?.name ?: "Cash"
                    val destAccountName = tx.destinationAccountId?.let { accountMap[it]?.name }

                    LedgerTransactionRow(
                        transaction = tx,
                        categoryName = categoryName,
                        sourceAccountName = sourceAccountName,
                        destinationAccountName = destAccountName,
                        onDeleteClick = { onDeleteClick(tx) }
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

// =============================================================================
//  LEDGER TRANSACTION ROW
// =============================================================================

private data class RowVisualTokens(
    val iconColor: Color,
    val icon: ImageVector,
    val amountPrefix: String,
    val amountColor: Color,
    val isRefund: Boolean = false
)

@Composable
fun LedgerTransactionRow(
    transaction: Transaction,
    categoryName: String?,
    sourceAccountName: String,
    destinationAccountName: String?,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expenseColor = expenseTextColor()
    val incomeColor = incomeTextColor()
    val savingColor = savingTextColor()

    val tokens: RowVisualTokens = when (transaction.type) {
        TransactionType.EXPENSE -> {
            val (cColor, cIcon) = when {
                transaction.note.contains("Food", ignoreCase = true) ||
                    transaction.note.contains("Lunch", ignoreCase = true) ||
                    transaction.note.contains("Dinner", ignoreCase = true) ||
                    transaction.note.contains("Breakfast", ignoreCase = true) ->
                    Pair(CategoryFoodIcon, Icons.Outlined.Fastfood)
                transaction.note.contains("Transport", ignoreCase = true) ||
                    transaction.note.contains("Gas", ignoreCase = true) ||
                    transaction.note.contains("Rickshaw", ignoreCase = true) ->
                    Pair(CategoryTransportIcon, Icons.Outlined.LocalGasStation)
                transaction.note.contains("Grocery", ignoreCase = true) ||
                    transaction.note.contains("Market", ignoreCase = true) ->
                    Pair(CategoryGroceryIcon, Icons.Outlined.ShoppingCart)
                else -> Pair(AppExpense, Icons.AutoMirrored.Outlined.ReceiptLong)
            }
            RowVisualTokens(
                iconColor = cColor,
                icon = cIcon,
                amountPrefix = "−",
                amountColor = expenseColor
            )
        }
        TransactionType.INCOME -> {
            RowVisualTokens(
                iconColor = AppIncome,
                icon = Icons.Outlined.ArrowUpward,
                amountPrefix = "+",
                amountColor = incomeColor
            )
        }
        TransactionType.REFUND -> {
            RowVisualTokens(
                iconColor = AppRefund,
                icon = Icons.AutoMirrored.Outlined.Undo,
                amountPrefix = "+",
                amountColor = AppRefund,
                isRefund = true
            )
        }
        TransactionType.TRANSFER -> {
            RowVisualTokens(
                iconColor = AppTransfer,
                icon = Icons.Outlined.SwapHoriz,
                amountPrefix = "",
                amountColor = MaterialTheme.colorScheme.onSurface
            )
        }
        TransactionType.SAVING -> {
            RowVisualTokens(
                iconColor = AppSaving,
                icon = Icons.Outlined.Savings,
                amountPrefix = "",
                amountColor = savingColor
            )
        }
        TransactionType.DONATION -> {
            RowVisualTokens(
                iconColor = AppDonation,
                icon = Icons.Outlined.VolunteerActivism,
                amountPrefix = "−",
                amountColor = AppDonation
            )
        }
        TransactionType.DEBT_PAYMENT -> {
            RowVisualTokens(
                iconColor = AppDebt,
                icon = Icons.Outlined.Payment,
                amountPrefix = "−",
                amountColor = expenseColor
            )
        }
        TransactionType.DEBT_RECEIVED -> {
            RowVisualTokens(
                iconColor = AppSecondary,
                icon = Icons.Outlined.AccountBalanceWallet,
                amountPrefix = "+",
                amountColor = incomeColor
            )
        }
        TransactionType.ADJUSTMENT -> {
            RowVisualTokens(
                iconColor = CategoryDefaultIcon,
                icon = Icons.Outlined.Tune,
                amountPrefix = "",
                amountColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val containerBg = tokens.iconColor.copy(alpha = 0.15f)

    // Account label presentation:
    // Transfers: "Cash → bKash"
    // Other types: "Cash"
    val accountDisplay = if (transaction.type == TransactionType.TRANSFER && destinationAccountName != null) {
        "$sourceAccountName → $destinationAccountName"
    } else {
        sourceAccountName
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(containerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tokens.icon,
                    contentDescription = null,
                    tint = tokens.iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(AppSpacing.md))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.note.ifBlank { transaction.type.displayName },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (tokens.isRefund) {
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(AppRefund.copy(alpha = 0.18f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "REFUND",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = AppRefund
                            )
                        }
                    }
                }
                val metaDetails = listOfNotNull(
                    categoryName,
                    accountDisplay,
                    transaction.time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
                ).joinToString(" · ")
                Text(
                    text = metaDetails,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            val amountFormatted = "${tokens.amountPrefix}${transaction.amount.format()}"
            Text(
                text = amountFormatted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = tokens.amountColor
            )

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete transaction",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

