package com.expenseapp.salarytracker.domain.model.report

import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.CustomField
import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.model.RecurringRule
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TreeHistory

data class BackupEnvelope(
    val formatVersion: Int = 1,
    val schemaVersion: Int = 2,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val checksumSha256: String = "",
    val currency: String = "BDT",
    val salaryCycleConfig: SalaryCycleConfig,
    val accounts: List<Account>,
    val categories: List<Category>,
    val transactions: List<Transaction>,
    val budgets: List<Budget>,
    val goals: List<Goal>,
    val recurringRules: List<RecurringRule>,
    val customFields: List<CustomField>,
    val treeHistory: List<TreeHistory>
)

data class BackupSummary(
    val formatVersion: Int,
    val schemaVersion: Int,
    val exportedAtEpochMs: Long,
    val appVersion: String,
    val accountCount: Int,
    val categoryCount: Int,
    val transactionCount: Int,
    val budgetCount: Int,
    val goalCount: Int,
    val recurringRuleCount: Int,
    val customFieldCount: Int,
    val treeHistoryCount: Int
)

data class BackupValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val summary: BackupSummary? = null,
    val envelope: BackupEnvelope? = null
)
