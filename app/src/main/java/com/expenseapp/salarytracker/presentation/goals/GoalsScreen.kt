package com.expenseapp.salarytracker.presentation.goals

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expenseapp.salarytracker.ExpenseApplication
import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.GoalStatus
import com.expenseapp.salarytracker.domain.model.GoalType
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.TreeState
import com.expenseapp.salarytracker.domain.service.GoalProgress
import com.expenseapp.salarytracker.domain.service.TreeOfHopeStatus
import com.expenseapp.salarytracker.presentation.theme.AppWarning
import com.expenseapp.salarytracker.presentation.theme.AppWarningText
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as ExpenseApplication
    val viewModel: GoalsViewModel = viewModel(
        factory = GoalsViewModel.Factory(
            goalRepository = app.goalRepository,
            transactionRepository = app.transactionRepository,
            categoryRepository = app.categoryRepository,
            accountRepository = app.accountRepository,
            budgetRepository = app.budgetRepository,
            settingsRepository = app.settingsRepository,
            goalCalculationEngine = app.goalCalculationEngine,
            calculationEngine = app.calculationEngine
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(GoalStatus.ACTIVE) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goals", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "New goal")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tree of Hope Hero Card
                item {
                    TreeOfHopeCard(treeStatus = uiState.treeOfHopeStatus)
                }

                // Filter Tabs (Active, Completed, Paused)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(4.dp)
                    ) {
                        GoalFilterTab(
                            title = "Active (${uiState.activeGoals.size})",
                            isSelected = selectedTab == GoalStatus.ACTIVE,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = GoalStatus.ACTIVE }
                        )
                        GoalFilterTab(
                            title = "Completed (${uiState.completedGoals.size})",
                            isSelected = selectedTab == GoalStatus.COMPLETED,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = GoalStatus.COMPLETED }
                        )
                        GoalFilterTab(
                            title = "Paused (${uiState.pausedGoals.size})",
                            isSelected = selectedTab == GoalStatus.PAUSED,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = GoalStatus.PAUSED }
                        )
                    }
                }

                // Goals List according to selectedTab
                val currentList = when (selectedTab) {
                    GoalStatus.ACTIVE -> uiState.activeGoals
                    GoalStatus.COMPLETED -> uiState.completedGoals
                    GoalStatus.PAUSED -> uiState.pausedGoals
                    else -> uiState.activeGoals
                }

                if (currentList.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Park,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = when (selectedTab) {
                                        GoalStatus.ACTIVE -> "No goals yet"
                                        GoalStatus.COMPLETED -> "No completed goals yet"
                                        GoalStatus.PAUSED -> "No paused goals"
                                        else -> "No goals"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Set a goal when you want to save, spend less, or donate.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                if (selectedTab == GoalStatus.ACTIVE) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { showCreateDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Set a goal")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(currentList, key = { it.goal.id }) { progress ->
                        GoalItemCard(
                            progress = progress,
                            currencySymbol = uiState.currencySymbol,
                            onPause = { viewModel.setGoalStatus(progress.goal.id, GoalStatus.PAUSED) },
                            onResume = { viewModel.setGoalStatus(progress.goal.id, GoalStatus.ACTIVE) },
                            onComplete = { viewModel.setGoalStatus(progress.goal.id, GoalStatus.COMPLETED) },
                            onDelete = { viewModel.deleteGoal(progress.goal.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGoalDialog(
            categories = uiState.categories,
            accounts = uiState.accounts,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, type, amount, days, catId, accId, start, end ->
                viewModel.createGoal(
                    name = name,
                    description = desc,
                    type = type,
                    targetAmount = amount,
                    targetDays = days,
                    categoryId = catId,
                    accountId = accId,
                    startDate = start,
                    endDate = end
                )
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun TreeOfHopeCard(treeStatus: TreeOfHopeStatus) {
    val stageEmoji = when (treeStatus.state) {
        TreeState.SEED -> "🌱"
        TreeState.SPROUT -> "🌿"
        TreeState.YOUNG_PLANT -> "🪴"
        TreeState.GROWING -> "🌳"
        TreeState.MATURE -> "🌲"
        TreeState.FLOWERING -> "🌸"
        TreeState.FRUIT_BEARING -> "🍎"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = stageEmoji, fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Tree of Hope",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stage: ${treeStatus.stageName} (Level ${treeStatus.state.level} of 7)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${String.format("%.0f", treeStatus.scorePercentage)}% Vitality",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vitality Progress Bar
            val progress = (treeStatus.scorePercentage / 100.0).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real Data Explanation
            Text(
                text = treeStatus.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Constructive Guidance
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Stars,
                    contentDescription = null,
                    tint = AppWarning,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = treeStatus.positiveActionTip,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GoalFilterTab(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GoalItemCard(
    progress: GoalProgress,
    currencySymbol: String,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val goal = progress.goal
    val isAmountBased = goal.type != GoalType.NO_SPEND && goal.type != GoalType.STREAK

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name, Type Badge, and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = goal.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (goal.status == GoalStatus.ACTIVE) {
                        IconButton(onClick = onComplete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = "Complete", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.PauseCircleOutline, contentDescription = "Pause", tint = AppWarning, modifier = Modifier.size(18.dp))
                        }
                    } else if (goal.status == GoalStatus.PAUSED) {
                        IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.PlayCircleOutline, contentDescription = "Resume", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (goal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isAmountBased) {
                    Text(
                        text = "${progress.currentAmount.format(currencySymbol)} of ${progress.targetAmount.format(currencySymbol)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${progress.remainingAmount.format(currencySymbol)} remaining",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "${progress.currentDays} of ${progress.targetDays} days",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${progress.remainingDaysToTarget} days left",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            val barProgress = (progress.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { barProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress.isCeilingExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Footer: Milestones & Window Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format("%.1f", progress.progressPercentage)}% completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Milestones badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(25, 50, 75, 100).forEach { m ->
                        val reached = progress.progressPercentage >= m
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (reached) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = "${m}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (reached) FontWeight.Bold else FontWeight.Normal,
                                color = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalDialog(
    categories: List<Category>,
    accounts: List<Account>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        description: String,
        type: GoalType,
        targetAmount: MinorUnits,
        targetDays: Int,
        categoryId: Long?,
        accountId: Long?,
        startDate: LocalDate,
        endDate: LocalDate
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(GoalType.SAVE_AMOUNT) }
    var amountText by remember { mutableStateOf("") }
    var daysText by remember { mutableStateOf("30") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today.plusMonths(1)) }

    val isAmountBased = selectedType != GoalType.NO_SPEND && selectedType != GoalType.STREAK

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Plant New Financial Goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Goal Type Dropdown
                Text("Goal Type", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        GoalType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name Field
                Text("Goal Name", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Emergency Fund, Vacation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Target Value
                if (isAmountBased) {
                    Text("Target Amount ($currencySymbol)", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("e.g. 50000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Text("Target Days", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("e.g. 30") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (selectedType == GoalType.CATEGORY_SPEND_UNDER) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "Select Category",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val targetAmount = if (isAmountBased) MinorUnits.fromDecimalString(amountText) else MinorUnits.ZERO
                                val targetDays = if (!isAmountBased) (daysText.toIntOrNull() ?: 30) else 0
                                onCreate(
                                    name,
                                    description,
                                    selectedType,
                                    targetAmount,
                                    targetDays,
                                    selectedCategory?.id,
                                    null,
                                    startDate,
                                    endDate
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Plant Goal")
                    }
                }
            }
        }
    }
}
