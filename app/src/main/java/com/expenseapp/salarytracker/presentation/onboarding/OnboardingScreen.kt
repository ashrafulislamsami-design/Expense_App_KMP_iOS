package com.expenseapp.salarytracker.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseapp.salarytracker.ExpenseApplication
import com.expenseapp.salarytracker.data.entity.AccountEntity
import com.expenseapp.salarytracker.domain.model.AccountType
import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.model.BudgetPeriod
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.presentation.theme.AppElevation
import com.expenseapp.salarytracker.presentation.theme.AppExpense
import com.expenseapp.salarytracker.presentation.theme.AppIncome
import com.expenseapp.salarytracker.presentation.theme.AppSpacing
import com.expenseapp.salarytracker.presentation.theme.AppWarning
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WalletPresetOption(
    val name: String,
    val subtitle: String,
    val type: AccountType,
    val colorHex: String,
    val icon: ImageVector
)

val DEFAULT_WALLET_PRESETS = listOf(
    WalletPresetOption("Cash", "Physical wallet", AccountType.CASH, "#10B981", Icons.Outlined.AccountBalanceWallet),
    WalletPresetOption("BRAC Bank Astha", "BRAC Bank app & A/C", AccountType.BANK, "#2563EB", Icons.Outlined.AccountBalance),
    WalletPresetOption("bKash", "Mobile wallet", AccountType.BKASH, "#E11D48", Icons.Outlined.PhoneAndroid),
    WalletPresetOption("Card", "Debit / Credit Card", AccountType.DEBIT_CARD, "#8B5CF6", Icons.Outlined.CreditCard),
    WalletPresetOption("Bank Account", "Salary / Savings bank", AccountType.BANK, "#0284C7", Icons.Outlined.AccountBalance),
    WalletPresetOption("Citytouch", "City Bank digital", AccountType.BANK, "#DC2626", Icons.Outlined.AccountBalance),
    WalletPresetOption("Nagad", "Mobile wallet", AccountType.NAGAD, "#F97316", Icons.Outlined.PhoneAndroid),
    WalletPresetOption("Rocket / Upay", "Mobile banking", AccountType.MOBILE_WALLET, "#7C3AED", Icons.Outlined.PhoneAndroid),
    WalletPresetOption("DBBL NexusPay", "Dutch-Bangla Bank", AccountType.BANK, "#059669", Icons.Outlined.CreditCard)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    app: ExpenseApplication,
    onFinishOnboarding: (openAddExpense: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 5

    val currency by app.settingsRepository.getCurrency().collectAsStateWithLifecycle(initialValue = "BDT")
    val currencySymbol = if (currency.equals("BDT", ignoreCase = true)) "৳" else "$"

    // Step 2: Multi-Wallet selection state
    val availablePresets = remember {
        mutableStateListOf<WalletPresetOption>().apply { addAll(DEFAULT_WALLET_PRESETS) }
    }
    val selectedWalletNames = remember {
        mutableStateListOf("Cash", "BRAC Bank Astha", "bKash", "Card")
    }
    var customWalletInput by remember { mutableStateOf("") }
    var walletError by remember { mutableStateOf<String?>(null) }

    // Step 3: Monthly Income & Salary Date
    var monthlyIncomeInput by remember { mutableStateOf("50000") }
    var incomeError by remember { mutableStateOf<String?>(null) }
    var selectedPayDay by remember { mutableIntStateOf(1) }

    // Step 4: Budget & Mid-Cycle "Already Spent" state
    var spendingLimitInput by remember { mutableStateOf("35000") }
    var hasSpendingLimit by remember { mutableStateOf(true) }
    var limitError by remember { mutableStateOf<String?>(null) }

    // If current date != salary date, ask how much already spent this cycle
    var alreadySpentInput by remember { mutableStateOf("") }
    var spentWalletName by remember { mutableStateOf("BRAC Bank Astha") }

    val completeSetupAndProceed = { openAddExpense: Boolean ->
        scope.launch {
            val today = LocalDate.now()

            // 1. Update salary cycle config
            val currentConfig = app.settingsRepository.getSalaryCycleConfig().first()
            val incomeMinor = MinorUnits.fromDecimalString(monthlyIncomeInput.ifBlank { "0" })
            val effectiveSalary = if (incomeMinor.isPositive()) incomeMinor else currentConfig.salaryAmount
            val updatedConfig = currentConfig.copy(
                salaryAmount = effectiveSalary,
                salaryDayOfMonth = selectedPayDay.coerceIn(1, 31)
            )
            app.settingsRepository.updateSalaryCycleConfig(updatedConfig)

            // 2. Create or update master spending limit budget
            val limitMinor = if (hasSpendingLimit && spendingLimitInput.isNotBlank()) {
                MinorUnits.fromDecimalString(spendingLimitInput)
            } else {
                MinorUnits.ZERO
            }
            if (limitMinor.isPositive()) {
                val existingOverall = app.budgetRepository.getOverallBudget().first()
                val budget = Budget(
                    id = existingOverall?.id ?: 0L,
                    categoryId = null,
                    limitAmount = limitMinor,
                    period = BudgetPeriod.SALARY_CYCLE,
                    warningThresholdPercentage = 80,
                    startDate = today
                )
                app.budgetRepository.insertOrUpdateBudget(budget)
            }

            // 3. Sync selected multiple wallets into Room Database
            val chosenNames = if (selectedWalletNames.isEmpty()) listOf("Cash") else selectedWalletNames.toList()
            val accDao = app.database.accountDao()
            val existingAccounts = app.accountRepository.getAllAccounts().first()
            val allTransactions = app.transactionRepository.getAllTransactions().first()

            // Archive or remove unselected default accounts if they have no transactions
            for (existing in existingAccounts) {
                val isSelected = chosenNames.any { it.equals(existing.name, ignoreCase = true) }
                if (!isSelected) {
                    val hasTx = allTransactions.any { it.accountId == existing.id || it.destinationAccountId == existing.id }
                    if (!hasTx) {
                        accDao.deleteById(existing.id)
                    } else {
                        accDao.archiveAccount(existing.id)
                    }
                } else if (existing.isArchived) {
                    app.accountRepository.updateAccount(existing.copy(isArchived = false))
                }
            }

            val refreshedAccounts = app.accountRepository.getAllAccounts().first()
            val savedAccountIdsByName = mutableMapOf<String, Long>()
            refreshedAccounts.forEach { acc ->
                savedAccountIdsByName[acc.name.lowercase()] = acc.id
            }

            // Calculate initial balance to put on primary wallet if no transactions exist yet
            val alreadySpentMinor = MinorUnits.fromDecimalString(alreadySpentInput.ifBlank { "0" })
            val baseStartingPool = if (effectiveSalary.isPositive()) effectiveSalary else limitMinor

            chosenNames.forEachIndexed { idx, walletName ->
                val existingId = savedAccountIdsByName[walletName.lowercase()]
                val preset = availablePresets.firstOrNull { it.name.equals(walletName, ignoreCase = true) }
                val walletInitialBalance = if (idx == 0 && allTransactions.isEmpty()) baseStartingPool else MinorUnits.ZERO

                if (existingId == null) {
                    val newId = accDao.insert(
                        AccountEntity(
                            name = walletName,
                            type = preset?.type ?: AccountType.MOBILE_WALLET,
                            initialBalanceMinorUnits = walletInitialBalance.amountInMinorUnits,
                            colorHex = preset?.colorHex ?: "#3B82F6",
                            sortOrder = idx + 1,
                            isArchived = false
                        )
                    )
                    savedAccountIdsByName[walletName.lowercase()] = newId
                } else if (idx == 0 && allTransactions.isEmpty()) {
                    val existingAcc = refreshedAccounts.first { it.id == existingId }
                    app.accountRepository.updateAccount(
                        existingAcc.copy(
                            initialBalance = walletInitialBalance,
                            isArchived = false,
                            sortOrder = idx + 1
                        )
                    )
                }
            }

            // 4. If today != salary date AND user entered an "Already Spent" amount, record it in current cycle
            if (today.dayOfMonth != selectedPayDay && alreadySpentMinor.isPositive()) {
                val primaryAccountId = savedAccountIdsByName[spentWalletName.lowercase()]
                    ?: savedAccountIdsByName.values.firstOrNull()
                    ?: 1L
                val categories = app.categoryRepository.getAllCategories().first()
                val defaultCatId = categories.firstOrNull()?.id

                app.transactionRepository.insertTransaction(
                    Transaction(
                        id = 0L,
                        amount = alreadySpentMinor,
                        type = TransactionType.EXPENSE,
                        accountId = primaryAccountId,
                        categoryId = defaultCatId,
                        date = today,
                        note = "Spent before setup (current cycle)"
                    )
                )
            }

            // 5. Mark onboarding completed
            app.settingsRepository.setCompletedOnboarding(true)

            // 6. Navigate to Dashboard (or open Quick Add)
            onFinishOnboarding(openAddExpense)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
        ) {
            // Top bar: Back button, Step indicator, Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (currentStep > 1) {
                        IconButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous step",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "Step $currentStep of $totalSteps",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = { completeSetupAndProceed(false) },
                    modifier = Modifier.semantics { contentDescription = "Skip onboarding" }
                ) {
                    Text(
                        text = "Quick finish",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            LinearProgressIndicator(
                progress = { currentStep / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboarding_steps"
            ) { step ->
                when (step) {
                    1 -> Step1Welcome(
                        onGetStarted = { currentStep = 2 }
                    )
                    2 -> Step2SelectMultipleWallets(
                        presets = availablePresets,
                        selectedWallets = selectedWalletNames,
                        customWalletInput = customWalletInput,
                        errorMessage = walletError,
                        onToggleWallet = { name ->
                            walletError = null
                            if (selectedWalletNames.any { it.equals(name, ignoreCase = true) }) {
                                selectedWalletNames.removeAll { it.equals(name, ignoreCase = true) }
                            } else {
                                selectedWalletNames.add(name)
                            }
                            if (spentWalletName !in selectedWalletNames && selectedWalletNames.isNotEmpty()) {
                                spentWalletName = selectedWalletNames.first()
                            }
                        },
                        onCustomWalletChange = { customWalletInput = it },
                        onAddCustomWallet = {
                            val trimmed = customWalletInput.trim()
                            if (trimmed.isNotEmpty()) {
                                if (availablePresets.none { it.name.equals(trimmed, ignoreCase = true) }) {
                                    availablePresets.add(
                                        WalletPresetOption(
                                            name = trimmed,
                                            subtitle = "Custom wallet / app",
                                            type = AccountType.BANK,
                                            colorHex = "#0EA5E9",
                                            icon = Icons.Outlined.AccountBalanceWallet
                                        )
                                    )
                                }
                                if (selectedWalletNames.none { it.equals(trimmed, ignoreCase = true) }) {
                                    selectedWalletNames.add(trimmed)
                                }
                                customWalletInput = ""
                                walletError = null
                            }
                        },
                        onNext = {
                            if (selectedWalletNames.isEmpty()) {
                                walletError = "Please select at least one wallet or payment method"
                            } else {
                                if (spentWalletName !in selectedWalletNames) {
                                    spentWalletName = selectedWalletNames.first()
                                }
                                currentStep = 3
                            }
                        }
                    )
                    3 -> Step3SalaryAndPayDate(
                        currencySymbol = currencySymbol,
                        incomeInput = monthlyIncomeInput,
                        selectedDay = selectedPayDay,
                        errorMessage = incomeError,
                        onIncomeChange = {
                            monthlyIncomeInput = it
                            incomeError = null
                        },
                        onDaySelected = { selectedPayDay = it },
                        onNext = {
                            val minor = MinorUnits.fromDecimalString(monthlyIncomeInput)
                            if (monthlyIncomeInput.isNotBlank() && !minor.isPositive()) {
                                incomeError = "Please enter a valid salary or income amount"
                            } else {
                                currentStep = 4
                            }
                        }
                    )
                    4 -> Step4BudgetAndAlreadySpent(
                        currencySymbol = currencySymbol,
                        limitInput = spendingLimitInput,
                        hasLimit = hasSpendingLimit,
                        selectedPayDay = selectedPayDay,
                        alreadySpentInput = alreadySpentInput,
                        selectedWallets = selectedWalletNames,
                        spentWalletName = spentWalletName,
                        errorMessage = limitError,
                        onLimitChange = {
                            spendingLimitInput = it
                            limitError = null
                        },
                        onHasLimitToggle = { hasSpendingLimit = it },
                        onAlreadySpentChange = { alreadySpentInput = it },
                        onSpentWalletSelected = { spentWalletName = it },
                        onNext = {
                            if (hasSpendingLimit && spendingLimitInput.isNotBlank()) {
                                val minor = MinorUnits.fromDecimalString(spendingLimitInput)
                                if (!minor.isPositive()) {
                                    limitError = "Please enter a valid monthly budget"
                                    return@Step4BudgetAndAlreadySpent
                                }
                            }
                            currentStep = 5
                        }
                    )
                    5 -> Step5SummaryAndFinish(
                        currencySymbol = currencySymbol,
                        selectedWallets = selectedWalletNames,
                        monthlyIncomeInput = monthlyIncomeInput,
                        selectedPayDay = selectedPayDay,
                        spendingLimitInput = if (hasSpendingLimit) spendingLimitInput else "",
                        alreadySpentInput = alreadySpentInput,
                        onGoToDashboard = { completeSetupAndProceed(false) },
                        onAddFirstExpense = { completeSetupAndProceed(true) }
                    )
                }
            }
        }
    }
}

// =============================================================================
//  STEP 1: Welcome
// =============================================================================

@Composable
private fun Step1Welcome(
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Savings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        Text(
            text = "Salary & Expense Command Center",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        Text(
            text = "Select all your wallets (BRAC Bank Astha, bKash, Cards, Bank, Cash), set your monthly budget, and track your mid-cycle spending at a glance.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.md)
        )

        Spacer(modifier = Modifier.height(AppSpacing.xl))

        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.none),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = AppIncome,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "100% Private & Offline-First",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = "All your wallets, budgets, and charts stay strictly on your phone. No cloud sync, no internet permission, and zero ads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.xl))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Set up my wallets & budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

// =============================================================================
//  STEP 2: Multi-Wallet Selection (Cards, Bank, bKash, BRAC Bank Astha, etc.)
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step2SelectMultipleWallets(
    presets: List<WalletPresetOption>,
    selectedWallets: List<String>,
    customWalletInput: String,
    errorMessage: String?,
    onToggleWallet: (String) -> Unit,
    onCustomWalletChange: (String) -> Unit,
    onAddCustomWallet: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select your wallets",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${selectedWallets.size} selected",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.xs))

        Text(
            text = "Choose all the apps, cards, and accounts you use (you can select multiple):",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(AppSpacing.md))

        // Multi-select grid of wallets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { preset ->
                        val isSelected = selectedWallets.any { it.equals(preset.name, ignoreCase = true) }
                        val accentColor = try {
                            Color(android.graphics.Color.parseColor(preset.colorHex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onToggleWallet(preset.name) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.none)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(accentColor.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = preset.icon,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = preset.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.md))

        // Add Custom Wallet (e.g., EBL Skybanking, Standard Chartered, Upay, etc.)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customWalletInput,
                onValueChange = onCustomWalletChange,
                label = { Text("Add another bank / app wallet") },
                placeholder = { Text("e.g. EBL Skybanking, Upay") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onAddCustomWallet,
                enabled = customWalletInput.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.height(54.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add custom wallet", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Continue (${selectedWallets.size} wallets)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// =============================================================================
//  STEP 3: Salary & Pay Date
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step3SalaryAndPayDate(
    currencySymbol: String,
    incomeInput: String,
    selectedDay: Int,
    errorMessage: String?,
    onIncomeChange: (String) -> Unit,
    onDaySelected: (Int) -> Unit,
    onNext: () -> Unit
) {
    val todayDay = LocalDate.now().dayOfMonth

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Salary & Payday",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AppSpacing.xs))

        Text(
            text = "Enter your monthly salary/income and the date you get paid.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        OutlinedTextField(
            value = incomeInput,
            onValueChange = { input ->
                if (input.all { it.isDigit() || it == '.' }) {
                    onIncomeChange(input)
                }
            },
            label = { Text("Monthly salary / income") },
            placeholder = { Text("50000") },
            leadingIcon = {
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = AppSpacing.sm)
                )
            },
            singleLine = true,
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        Text(
            text = "Which date of the month do you receive your salary?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        val payDays = listOf(1, 2, 3, 5, 7, 10, 15, 20, 25, 27, 28, 30, 31)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            payDays.forEach { day ->
                val isSelected = selectedDay == day
                FilterChip(
                    selected = isSelected,
                    onClick = { onDaySelected(day) },
                    label = {
                        Text(
                            text = if (day == 1) "1st of month" else "$day" + getOrdinalSuffix(day),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.md))

        // Helpful indicator if today != selectedDay
        if (todayDay != selectedDay) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Today is the ${todayDay}${getOrdinalSuffix(todayDay)}, while your salary date is the ${selectedDay}${getOrdinalSuffix(selectedDay)}. On the next step, we'll ask how much you've already spent so far in this cycle!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(AppSpacing.lg))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Continue to Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private fun getOrdinalSuffix(n: Int): String {
    if (n in 11..13) return "th"
    return when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

// =============================================================================
//  STEP 4: Monthly Budget & Mid-Cycle "Already Spent" Prompt
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step4BudgetAndAlreadySpent(
    currencySymbol: String,
    limitInput: String,
    hasLimit: Boolean,
    selectedPayDay: Int,
    alreadySpentInput: String,
    selectedWallets: List<String>,
    spentWalletName: String,
    errorMessage: String?,
    onLimitChange: (String) -> Unit,
    onHasLimitToggle: (Boolean) -> Unit,
    onAlreadySpentChange: (String) -> Unit,
    onSpentWalletSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    val today = LocalDate.now()
    val isMidCycle = today.dayOfMonth != selectedPayDay
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Budget & Current Cycle Spend",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AppSpacing.xs))

        Text(
            text = "Set your target monthly budget so we can calculate your daily safe-to-spend limit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(AppSpacing.md))

        if (hasLimit) {
            OutlinedTextField(
                value = limitInput,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' }) {
                        onLimitChange(input)
                    }
                },
                label = { Text("Monthly budget (spending limit)") },
                placeholder = { Text("35000") },
                leadingIcon = {
                    Text(
                        text = currencySymbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = AppSpacing.sm)
                    )
                },
                singleLine = true,
                isError = errorMessage != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        // Mid-Cycle "Already Spent" Section (Shown prominently when today != salary day, or always available)
        AnimatedVisibility(visible = isMidCycle) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.5.dp, AppWarning.copy(alpha = 0.65f)),
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.none),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.sm)
            ) {
                Column(modifier = Modifier.padding(AppSpacing.md)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = AppWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Mid-Cycle Check (${today.format(dateFmt)} ≠ ${selectedPayDay}${getOrdinalSuffix(selectedPayDay)})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Since today is not your salary date (${selectedPayDay}${getOrdinalSuffix(selectedPayDay)}), how much have you already spent so far in this cycle?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = alreadySpentInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' }) {
                                onAlreadySpentChange(input)
                            }
                        },
                        label = { Text("Already spent so far this cycle") },
                        placeholder = { Text("e.g. 12500 (leave 0 if none)") },
                        leadingIcon = {
                            Text(
                                text = currencySymbol,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppExpense,
                                modifier = Modifier.padding(start = AppSpacing.sm)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick amount presets for fast tap
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("0", "5000", "10000", "15000", "20000").forEach { presetAmt ->
                            FilterChip(
                                selected = alreadySpentInput == presetAmt,
                                onClick = { onAlreadySpentChange(presetAmt) },
                                label = {
                                    Text(
                                        text = "$currencySymbol$presetAmt",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }

                    if (selectedWallets.size > 1 && alreadySpentInput.isNotBlank() && alreadySpentInput != "0") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Deduct from wallet:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            selectedWallets.forEach { wallet ->
                                FilterChip(
                                    selected = spentWalletName.equals(wallet, ignoreCase = true),
                                    onClick = { onSpentWalletSelected(wallet) },
                                    label = {
                                        Text(
                                            text = wallet,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(AppSpacing.lg))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Review & Launch Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// =============================================================================
//  STEP 5: Ready & Live Preview Summary
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step5SummaryAndFinish(
    currencySymbol: String,
    selectedWallets: List<String>,
    monthlyIncomeInput: String,
    selectedPayDay: Int,
    spendingLimitInput: String,
    alreadySpentInput: String,
    onGoToDashboard: () -> Unit,
    onAddFirstExpense: () -> Unit
) {
    val incomeMinor = MinorUnits.fromDecimalString(monthlyIncomeInput.ifBlank { "0" })
    val budgetMinor = MinorUnits.fromDecimalString(spendingLimitInput.ifBlank { "0" })
    val spentMinor = MinorUnits.fromDecimalString(alreadySpentInput.ifBlank { "0" })
    val effectiveLimit = if (budgetMinor.isPositive()) budgetMinor else incomeMinor
    val remainingBudget = if (effectiveLimit > spentMinor) effectiveLimit - spentMinor else MinorUnits.ZERO

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AppIncome.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AppIncome,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.md))

        Text(
            text = "All Set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Here is your starting financial snapshot:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.none)
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryRowItem("Selected Wallets (${selectedWallets.size})", selectedWallets.joinToString(", "))
                SummaryRowItem("Monthly Salary", incomeMinor.formatCompactOnboarding(currencySymbol))
                SummaryRowItem("Salary Payday", "${selectedPayDay}${getOrdinalSuffix(selectedPayDay)} of every month")
                if (budgetMinor.isPositive()) {
                    SummaryRowItem("Monthly Budget", budgetMinor.formatCompactOnboarding(currencySymbol))
                }
                if (spentMinor.isPositive()) {
                    SummaryRowItem("Already Spent This Cycle", "- ${spentMinor.formatCompactOnboarding(currencySymbol)}")
                }
                SummaryRowItem("Remaining Available Budget", remainingBudget.formatCompactOnboarding(currencySymbol), highlight = true)
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.xl))

        Button(
            onClick = onGoToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Open Dashboard & Charts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        OutlinedButton(
            onClick = onAddFirstExpense,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Add another expense now", style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun SummaryRowItem(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f)
        )
    }
}

private fun MinorUnits.formatCompactOnboarding(currencySymbol: String): String {
    return if (amountInMinorUnits % 100L == 0L) {
        format(currencySymbol, decimals = 2).removeSuffix(".00")
    } else {
        format(currencySymbol, decimals = 2)
    }
}
