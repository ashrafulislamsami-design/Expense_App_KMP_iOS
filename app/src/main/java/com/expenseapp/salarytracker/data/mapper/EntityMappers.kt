package com.expenseapp.salarytracker.data.mapper

import com.expenseapp.salarytracker.data.entity.AccountEntity
import com.expenseapp.salarytracker.data.entity.BudgetEntity
import com.expenseapp.salarytracker.data.entity.CategoryEntity
import com.expenseapp.salarytracker.data.entity.GoalEntity
import com.expenseapp.salarytracker.data.entity.TransactionEntity
import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.Transaction

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = type,
    initialBalance = MinorUnits(initialBalanceMinorUnits),
    currency = currency,
    isArchived = isArchived,
    sortOrder = sortOrder,
    colorHex = colorHex
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type,
    initialBalanceMinorUnits = initialBalance.amountInMinorUnits,
    currency = currency,
    isArchived = isArchived,
    sortOrder = sortOrder,
    colorHex = colorHex
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    parentCategoryId = parentCategoryId,
    isArchived = isArchived,
    sortOrder = sortOrder
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    parentCategoryId = parentCategoryId,
    isArchived = isArchived,
    sortOrder = sortOrder
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amount = MinorUnits(amountMinorUnits),
    type = type,
    accountId = accountId,
    destinationAccountId = destinationAccountId,
    categoryId = categoryId,
    subcategoryId = subcategoryId,
    date = date,
    time = time,
    note = note,
    tags = tags,
    recurringRuleId = recurringRuleId,
    originalTransactionId = originalTransactionId,
    customFieldValues = com.expenseapp.salarytracker.data.converter.CustomFieldsJsonSerializer.deserialize(customFieldsJson),
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amountMinorUnits = amount.amountInMinorUnits,
    type = type,
    accountId = accountId,
    destinationAccountId = destinationAccountId,
    categoryId = categoryId,
    subcategoryId = subcategoryId,
    date = date,
    time = time,
    note = note,
    tags = tags,
    recurringRuleId = recurringRuleId,
    originalTransactionId = originalTransactionId,
    customFieldsJson = com.expenseapp.salarytracker.data.converter.CustomFieldsJsonSerializer.serialize(customFieldValues),
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    categoryId = categoryId,
    limitAmount = MinorUnits(limitMinorUnits),
    period = period,
    startDate = startDate,
    endDate = endDate,
    warningThresholdPercentage = warningThresholdPercentage
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    categoryId = categoryId,
    limitMinorUnits = limitAmount.amountInMinorUnits,
    period = period,
    startDate = startDate,
    endDate = endDate,
    warningThresholdPercentage = warningThresholdPercentage
)

fun GoalEntity.toDomain(): Goal = Goal(
    id = id,
    name = name,
    description = description,
    type = type,
    targetAmount = MinorUnits(targetAmountMinorUnits),
    targetDays = targetDays,
    categoryId = categoryId,
    accountId = accountId,
    startDate = startDate,
    endDate = endDate,
    status = com.expenseapp.salarytracker.domain.model.GoalStatus.fromString(status),
    reachedMilestones = if (reachedMilestonesJson.isBlank() || reachedMilestonesJson.trim() == "[]") emptyList() else reachedMilestonesJson.removeSurrounding("[", "]").split(",").mapNotNull { it.trim().toIntOrNull() },
    treeState = treeState,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    name = name,
    description = description,
    type = type,
    targetAmountMinorUnits = targetAmount.amountInMinorUnits,
    targetDays = targetDays,
    categoryId = categoryId,
    accountId = accountId,
    startDate = startDate,
    endDate = endDate,
    status = status.name,
    reachedMilestonesJson = "[" + reachedMilestones.joinToString(",") + "]",
    treeState = treeState,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    isCompleted = status == com.expenseapp.salarytracker.domain.model.GoalStatus.COMPLETED,
    isFailed = status == com.expenseapp.salarytracker.domain.model.GoalStatus.ARCHIVED
)

fun com.expenseapp.salarytracker.data.entity.TreeHistoryEntity.toDomain(): com.expenseapp.salarytracker.domain.model.TreeHistory = com.expenseapp.salarytracker.domain.model.TreeHistory(
    id = id,
    previousState = previousState,
    newState = newState,
    reason = reason,
    contributingGoalId = contributingGoalId,
    timestampEpochMs = timestampEpochMs
)

fun com.expenseapp.salarytracker.domain.model.TreeHistory.toEntity(): com.expenseapp.salarytracker.data.entity.TreeHistoryEntity = com.expenseapp.salarytracker.data.entity.TreeHistoryEntity(
    id = id,
    previousState = previousState,
    newState = newState,
    reason = reason,
    contributingGoalId = contributingGoalId,
    timestampEpochMs = timestampEpochMs
)

fun com.expenseapp.salarytracker.data.entity.CustomFieldEntity.toDomain(): com.expenseapp.salarytracker.domain.model.CustomField = com.expenseapp.salarytracker.domain.model.CustomField(
    id = id,
    name = name,
    type = type,
    optionsJson = if (optionsJson.isBlank() || optionsJson == "[]") emptyList() else optionsJson.removeSurrounding("[", "]").split(",").map { it.trim().removeSurrounding("\"") },
    isRequired = isRequired
)

fun com.expenseapp.salarytracker.domain.model.CustomField.toEntity(): com.expenseapp.salarytracker.data.entity.CustomFieldEntity = com.expenseapp.salarytracker.data.entity.CustomFieldEntity(
    id = id,
    name = name,
    type = type,
    optionsJson = "[" + optionsJson.joinToString(",") { "\"$it\"" } + "]",
    isRequired = isRequired
)

fun com.expenseapp.salarytracker.data.entity.RecurringRuleEntity.toDomain(): com.expenseapp.salarytracker.domain.model.RecurringRule = com.expenseapp.salarytracker.domain.model.RecurringRule(
    id = id,
    name = name,
    amount = com.expenseapp.salarytracker.domain.model.MinorUnits(amountMinorUnits),
    type = type,
    accountId = accountId,
    destinationAccountId = destinationAccountId,
    categoryId = categoryId,
    frequency = frequency,
    intervalDays = intervalDays,
    preferredDayOfMonth = preferredDayOfMonth,
    startDate = startDate,
    nextDueDate = nextDueDate,
    endDate = endDate,
    autoPost = autoPost,
    isPaused = isPaused,
    note = note
)

fun com.expenseapp.salarytracker.domain.model.RecurringRule.toEntity(): com.expenseapp.salarytracker.data.entity.RecurringRuleEntity = com.expenseapp.salarytracker.data.entity.RecurringRuleEntity(
    id = id,
    name = name,
    amountMinorUnits = amount.amountInMinorUnits,
    type = type,
    accountId = accountId,
    destinationAccountId = destinationAccountId,
    categoryId = categoryId,
    frequency = frequency,
    intervalDays = intervalDays,
    preferredDayOfMonth = preferredDayOfMonth,
    startDate = startDate,
    nextDueDate = nextDueDate,
    endDate = endDate,
    autoPost = autoPost,
    isPaused = isPaused,
    note = note
)


