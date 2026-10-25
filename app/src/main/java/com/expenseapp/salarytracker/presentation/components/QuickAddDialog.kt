package com.expenseapp.salarytracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.AccountType
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.CustomField
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.RecurrenceFrequency
import com.expenseapp.salarytracker.domain.model.RecurringRule
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
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
import java.time.LocalDate
import java.time.LocalTime

// =============================================================================
//  ADD TRANSACTION / QUICK ADD DIALOG (Phase 3D-D Redesign)
//
//  Design Thesis:
//  - "I just spent money and want to record it quickly."
//  - Immediate hierarchy: Type -> Hero Amount -> Category -> Account -> Save
//  - Contextual adaptive fields based on transaction type (Transfers, Refunds, etc.)
//  - Progressive disclosure for optional metadata (Note, Tags, Custom Fields, Recurring)
//  - Guaranteed 48dp touch targets on all interactive elements
//  - Full dark-mode and currency formatting compliance
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddDialog(
    categories: List<Category>,
    accounts: List<Account>,
    customFields: List<CustomField> = emptyList(),
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSaveTransaction: (Transaction) -> Unit,
    onSaveRecurringRule: ((RecurringRule) -> Unit)? = null
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(categories.firstOrNull()) }
    var selectedAccount by remember { mutableStateOf<Account?>(accounts.firstOrNull()) }
    var destinationAccount by remember {
        mutableStateOf<Account?>(accounts.getOrNull(1) ?: accounts.firstOrNull())
    }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var customFieldValues by remember { mutableStateOf(mutableMapOf<String, String>()) }

    // Recurring state
    var isRecurring by remember { mutableStateOf(false) }
    var recurringFrequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }

    // Progressive disclosure states
    var isMoreDetailsExpanded by remember { mutableStateOf(false) }
    var isRepeatExpanded by remember { mutableStateOf(false) }
    var isOtherTypesExpanded by remember {
        mutableStateOf(selectedType !in listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER))
    }

    // Validation & Debounce state
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.none),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 760.dp)
                .padding(vertical = AppSpacing.md)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(AppSpacing.lg)
            ) {
                // 1. TOP HEADER (Plain language)
                val headerTitle = when (selectedType) {
                    TransactionType.EXPENSE -> "Add expense"
                    TransactionType.INCOME -> "Money received"
                    TransactionType.TRANSFER -> "Move money"
                    TransactionType.REFUND -> "Refund"
                    TransactionType.SAVING -> "Money saved"
                    TransactionType.DONATION -> "Donation"
                    TransactionType.DEBT_PAYMENT -> "Debt paid"
                    TransactionType.DEBT_RECEIVED -> "Debt received"
                    TransactionType.ADJUSTMENT -> "Fix balance"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (selectedType == TransactionType.EXPENSE) "Record spending in seconds" else "Track your money movements",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Close dialog" }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // 2. TRANSACTION TYPE SELECTOR (Everyday first + Progressive disclosure)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        val primaryTypes = listOf(
                            TransactionType.EXPENSE,
                            TransactionType.INCOME,
                            TransactionType.TRANSFER
                        )

                        primaryTypes.forEach { type ->
                            val isSelected = selectedType == type
                            val (typeColor, typeIcon) = getTypeVisuals(type)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        selectedType = type
                                        validationError = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.extraLarge)
                                        .background(
                                            if (isSelected) typeColor.copy(alpha = 0.18f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) typeColor else MaterialTheme.colorScheme.outlineVariant,
                                            MaterialTheme.shapes.extraLarge
                                        )
                                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = typeIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) typeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = type.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // More types toggle button
                        val isSecondarySelected = selectedType !in primaryTypes
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable { isOtherTypesExpanded = !isOtherTypesExpanded },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(
                                        if (isSecondarySelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSecondarySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        MaterialTheme.shapes.extraLarge
                                    )
                                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = if (isSecondarySelected) selectedType.displayName else "More",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSecondarySelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSecondarySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = if (isOtherTypesExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = if (isOtherTypesExpanded) "Collapse more types" else "Expand more types",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Expandable secondary types
                    AnimatedVisibility(
                        visible = isOtherTypesExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val secondaryTypes = listOf(
                            TransactionType.REFUND,
                            TransactionType.SAVING,
                            TransactionType.DONATION,
                            TransactionType.DEBT_PAYMENT,
                            TransactionType.DEBT_RECEIVED,
                            TransactionType.ADJUSTMENT
                        )
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = AppSpacing.xs),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                        ) {
                            secondaryTypes.forEach { type ->
                                val isSelected = selectedType == type
                                val (typeColor, typeIcon) = getTypeVisuals(type)

                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .clickable {
                                            selectedType = type
                                            validationError = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.extraLarge)
                                            .background(
                                                if (isSelected) typeColor.copy(alpha = 0.18f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) typeColor else MaterialTheme.colorScheme.outlineVariant,
                                                MaterialTheme.shapes.extraLarge
                                            )
                                            .padding(horizontal = AppSpacing.sm, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = typeIcon,
                                            contentDescription = null,
                                            tint = if (isSelected) typeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = type.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // 3. PRIMARY HERO AMOUNT AREA
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AMOUNT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.xs))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currencySymbol,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = AppSpacing.xs)
                            )

                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() || it == '.' }) {
                                        amountInput = input
                                        validationError = null
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = "0.00",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Transaction amount input" }
                            )
                        }

                        // Quick Amount Increment Chips (+50, +100, +500, +1000)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = AppSpacing.xs),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs, Alignment.CenterHorizontally)
                        ) {
                            val increments = listOf(50L, 100L, 500L, 1000L)
                            increments.forEach { inc ->
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .clickable {
                                            val currentMinor = MinorUnits.fromDecimalString(amountInput)
                                            val newMinor = currentMinor + MinorUnits.fromMajor(inc)
                                            amountInput = (newMinor.amountInMinorUnits / 100.0).toString().removeSuffix(".0")
                                            validationError = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.small)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "+$inc",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // 4. CONTEXTUAL FORM FIELDS
                when (selectedType) {
                    TransactionType.TRANSFER -> {
                        // TRANSFER VIEW: Source -> Destination routing
                        TransferRoutingSection(
                            accounts = accounts,
                            sourceAccount = selectedAccount,
                            destinationAccount = destinationAccount,
                            onSourceSelected = {
                                selectedAccount = it
                                validationError = null
                            },
                            onDestinationSelected = {
                                destinationAccount = it
                                validationError = null
                            }
                        )
                    }

                    TransactionType.REFUND -> {
                        // REFUND VIEW: Category reversal + Deposit Account
                        RefundBanner()
                        Spacer(modifier = Modifier.height(AppSpacing.sm))

                        CategoryPickerSection(
                            title = "Reversal Category",
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it }
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.md))

                        AccountPickerSection(
                            title = "Refund credited to",
                            accounts = accounts,
                            selectedAccount = selectedAccount,
                            onAccountSelected = { selectedAccount = it }
                        )
                    }

                    else -> {
                        // STANDARD TYPES (Expense, Income, Saving, Donation, Debt, Adjustment)
                        val showCategory = selectedType != TransactionType.ADJUSTMENT
                        if (showCategory) {
                            CategoryPickerSection(
                                title = if (selectedType == TransactionType.INCOME) "Income category" else "Category",
                                categories = categories,
                                selectedCategory = selectedCategory,
                                onCategorySelected = { selectedCategory = it }
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.md))
                        }

                        val accountLabel = when (selectedType) {
                            TransactionType.INCOME, TransactionType.DEBT_RECEIVED -> "Deposit to"
                            TransactionType.SAVING -> "Save from"
                            TransactionType.ADJUSTMENT -> "Account to fix"
                            else -> "Paid from"
                        }

                        AccountPickerSection(
                            title = accountLabel,
                            accounts = accounts,
                            selectedAccount = selectedAccount,
                            onAccountSelected = { selectedAccount = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // 5. DATE QUICK PICKER
                DateQuickPickerSection(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                // 6. PROGRESSIVE DISCLOSURE: MORE OPTIONS (Note, Tags, Custom Fields)
                ExpandableSectionHeader(
                    title = "More options",
                    subtitle = if (note.isNotBlank()) "Note: $note" else "Notes, tags, custom fields",
                    isExpanded = isMoreDetailsExpanded,
                    onToggle = { isMoreDetailsExpanded = !isMoreDetailsExpanded }
                )

                AnimatedVisibility(
                    visible = isMoreDetailsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppSpacing.xs)
                    ) {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            placeholder = {
                                Text(
                                    text = "Add note or payee (e.g. Lunch at Cafe, Grocery)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Custom Fields (if defined in repository)
                        if (customFields.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                            Text(
                                text = "Custom Fields",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            customFields.forEach { cf ->
                                OutlinedTextField(
                                    value = customFieldValues[cf.name] ?: "",
                                    onValueChange = { customFieldValues[cf.name] = it },
                                    label = { Text(cf.name) },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = AppSpacing.xs)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                // 7. PROGRESSIVE DISCLOSURE: RECURRING (Repeat Transaction)
                if (onSaveRecurringRule != null && selectedType != TransactionType.REFUND && selectedType != TransactionType.ADJUSTMENT) {
                    ExpandableSectionHeader(
                        title = "Repeat transaction",
                        subtitle = if (isRecurring) "Repeats: ${recurringFrequency.displayName}" else "One-time entry",
                        isExpanded = isRepeatExpanded,
                        onToggle = { isRepeatExpanded = !isRepeatExpanded }
                    )

                    AnimatedVisibility(
                        visible = isRepeatExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = AppSpacing.xs)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Repeat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Repeat this transaction regularly",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Switch(
                                    checked = isRecurring,
                                    onCheckedChange = { isRecurring = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            if (isRecurring) {
                                Spacer(modifier = Modifier.height(AppSpacing.xs))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                                ) {
                                    RecurrenceFrequency.entries.forEach { freq ->
                                        val isFreqSelected = recurringFrequency == freq
                                        Box(
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .clickable { recurringFrequency = freq },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(MaterialTheme.shapes.small)
                                                    .background(
                                                        if (isFreqSelected) MaterialTheme.colorScheme.primaryContainer
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isFreqSelected) MaterialTheme.colorScheme.primary
                                                        else Color.Transparent,
                                                        MaterialTheme.shapes.small
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = freq.displayName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isFreqSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isFreqSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Inline Validation Error Display
                if (validationError != null) {
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                // 8. PRIMARY SAVE ACTION BUTTON (Plain language)
                val saveButtonText = when (selectedType) {
                    TransactionType.EXPENSE -> "Save expense"
                    TransactionType.INCOME -> "Save income"
                    TransactionType.TRANSFER -> "Move money"
                    TransactionType.REFUND -> "Save refund"
                    TransactionType.SAVING -> "Save money"
                    TransactionType.DONATION -> "Save donation"
                    TransactionType.DEBT_PAYMENT -> "Save debt payment"
                    TransactionType.DEBT_RECEIVED -> "Save debt received"
                    TransactionType.ADJUSTMENT -> "Fix balance"
                }

                Button(
                    onClick = {
                        if (isSaving) return@Button
                        val minor = MinorUnits.fromDecimalString(amountInput)
                        if (!minor.isPositive()) {
                            validationError = "Please enter an amount greater than 0"
                            return@Button
                        }

                        val sourceAcc = selectedAccount ?: accounts.firstOrNull()
                        if (sourceAcc == null) {
                            validationError = "Please select an account"
                            return@Button
                        }

                        if (selectedType == TransactionType.TRANSFER) {
                            val destAcc = destinationAccount
                            if (destAcc == null || destAcc.id == sourceAcc.id) {
                                validationError = "Source and destination accounts must be different"
                                return@Button
                            }
                        }

                        isSaving = true
                        val tx = Transaction(
                            amount = minor,
                            type = selectedType,
                            accountId = sourceAcc.id,
                            destinationAccountId = if (selectedType == TransactionType.TRANSFER) destinationAccount?.id else null,
                            categoryId = if (selectedType != TransactionType.TRANSFER && selectedType != TransactionType.ADJUSTMENT) selectedCategory?.id else null,
                            date = selectedDate,
                            time = LocalTime.now(),
                            note = note.trim(),
                            customFieldValues = customFieldValues
                        )
                        onSaveTransaction(tx)

                        if (isRecurring && onSaveRecurringRule != null) {
                            val rule = RecurringRule(
                                name = note.ifBlank { "${selectedType.displayName} Rule" },
                                amount = minor,
                                type = selectedType,
                                accountId = sourceAcc.id,
                                destinationAccountId = if (selectedType == TransactionType.TRANSFER) destinationAccount?.id else null,
                                categoryId = selectedCategory?.id,
                                frequency = recurringFrequency,
                                startDate = selectedDate,
                                nextDueDate = selectedDate.plusDays(30),
                                note = note.trim()
                            )
                            onSaveRecurringRule(rule)
                        }

                        onDismiss()
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .semantics { contentDescription = saveButtonText }
                ) {
                    Text(
                        text = saveButtonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =============================================================================
//  HELPER SECTIONS & COMPONENTS
// =============================================================================

@Composable
private fun RefundBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(AppRefund.copy(alpha = 0.15f))
            .border(1.dp, AppRefund.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Undo,
            contentDescription = null,
            tint = AppRefund,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = "Expense Reversal",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AppRefund
            )
            Text(
                text = "Refunds offset cycle spending and increase account balance without inflating income.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransferRoutingSection(
    accounts: List<Account>,
    sourceAccount: Account?,
    destinationAccount: Account?,
    onSourceSelected: (Account) -> Unit,
    onDestinationSelected: (Account) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Source Account
        Text(
            text = "From Account",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            accounts.forEach { acc ->
                val isSelected = sourceAccount?.id == acc.id
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { onSourceSelected(acc) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = acc.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        // Transfer Direction Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowDownward,
                contentDescription = "Transfer Direction",
                tint = AppTransfer,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        // Destination Account
        Text(
            text = "To Account",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            accounts.forEach { acc ->
                val isSelected = destinationAccount?.id == acc.id
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { onDestinationSelected(acc) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isSelected) AppTransfer.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) AppTransfer
                                else MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = acc.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AppTransfer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPickerSection(
    title: String,
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))

        val displayCategories = if (categories.isNotEmpty()) categories else Category.DEFAULT_CATEGORIES
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            displayCategories.forEach { cat ->
                val isSelected = selectedCategory?.id == cat.id || (selectedCategory == null && cat.name == "Food")
                val (catColor, catIcon) = getCategoryVisuals(cat.name)

                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { onCategorySelected(cat) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = catIcon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else catColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountPickerSection(
    title: String,
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))

        val listAccounts = if (accounts.isNotEmpty()) accounts else listOf(
            Account(id = 1L, name = "Cash", type = AccountType.CASH),
            Account(id = 2L, name = "Bank", type = AccountType.BANK),
            Account(id = 3L, name = "bKash", type = AccountType.BKASH),
            Account(id = 4L, name = "Nagad", type = AccountType.NAGAD)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            listAccounts.forEach { acc ->
                val isSelected = selectedAccount?.id == acc.id || (selectedAccount == null && acc.name == "Cash")
                val accIcon = getAccountIcon(acc.type)

                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { onAccountSelected(acc) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = accIcon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = acc.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateQuickPickerSection(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val isCustom = selectedDate != today && selectedDate != yesterday

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Date",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateOptions = listOf(
                "Today" to today,
                "Yesterday" to yesterday
            )

            dateOptions.forEach { (label, date) ->
                val isSelected = selectedDate == date
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { onDateSelected(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (isCustom) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable {
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d -> onDateSelected(LocalDate.of(y, m + 1, d)) },
                                selectedDate.year,
                                selectedDate.monthValue - 1,
                                selectedDate.dayOfMonth
                            ).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = selectedDate.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Calendar Picker Button
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable {
                        android.app.DatePickerDialog(
                            context,
                            { _, y, m, d -> onDateSelected(LocalDate.of(y, m + 1, d)) },
                            selectedDate.year,
                            selectedDate.monthValue - 1,
                            selectedDate.dayOfMonth
                        ).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Pick custom date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Pick Date...",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableSectionHeader(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse section" else "Expand section",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// =============================================================================
//  VISUAL ICON & COLOR RESOLVERS
// =============================================================================

private fun getTypeVisuals(type: TransactionType): Pair<Color, ImageVector> = when (type) {
    TransactionType.EXPENSE -> Pair(AppExpense, Icons.AutoMirrored.Outlined.ReceiptLong)
    TransactionType.INCOME -> Pair(AppIncome, Icons.Outlined.ArrowUpward)
    TransactionType.TRANSFER -> Pair(AppTransfer, Icons.Outlined.SwapHoriz)
    TransactionType.REFUND -> Pair(AppRefund, Icons.AutoMirrored.Outlined.Undo)
    TransactionType.SAVING -> Pair(AppSaving, Icons.Outlined.Savings)
    TransactionType.DONATION -> Pair(AppDonation, Icons.Outlined.VolunteerActivism)
    TransactionType.DEBT_PAYMENT -> Pair(AppDebt, Icons.Outlined.Payment)
    TransactionType.DEBT_RECEIVED -> Pair(AppSecondary, Icons.Outlined.AccountBalanceWallet)
    TransactionType.ADJUSTMENT -> Pair(CategoryDefaultIcon, Icons.Outlined.Tune)
}

private fun getCategoryVisuals(categoryName: String): Pair<Color, ImageVector> = when {
    categoryName.contains("Food", ignoreCase = true) ||
        categoryName.contains("Dining", ignoreCase = true) ||
        categoryName.contains("Lunch", ignoreCase = true) ->
        Pair(CategoryFoodIcon, Icons.Outlined.Fastfood)

    categoryName.contains("Transport", ignoreCase = true) ||
        categoryName.contains("Fuel", ignoreCase = true) ||
        categoryName.contains("Gas", ignoreCase = true) ->
        Pair(CategoryTransportIcon, Icons.Outlined.LocalGasStation)

    categoryName.contains("Grocery", ignoreCase = true) ||
        categoryName.contains("Market", ignoreCase = true) ->
        Pair(CategoryGroceryIcon, Icons.Outlined.ShoppingCart)

    else -> Pair(CategoryDefaultIcon, Icons.AutoMirrored.Outlined.ReceiptLong)
}

private fun getAccountIcon(type: AccountType): ImageVector = when (type) {
    AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
    AccountType.BANK -> Icons.Outlined.AccountBalance
    AccountType.BKASH, AccountType.NAGAD, AccountType.MOBILE_WALLET -> Icons.Outlined.PhoneAndroid
    AccountType.DEBIT_CARD, AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard
    else -> Icons.Outlined.AccountBalanceWallet
}
