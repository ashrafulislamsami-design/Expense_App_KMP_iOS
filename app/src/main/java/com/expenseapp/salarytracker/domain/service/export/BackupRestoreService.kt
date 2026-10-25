package com.expenseapp.salarytracker.domain.service.export

import androidx.room.withTransaction
import com.expenseapp.salarytracker.data.database.ExpenseDatabase
import com.expenseapp.salarytracker.data.entity.UserSettingEntity
import com.expenseapp.salarytracker.data.mapper.toDomain
import com.expenseapp.salarytracker.data.mapper.toEntity
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.report.BackupEnvelope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

data class MergeSummary(
    val accountsAdded: Int,
    val categoriesAdded: Int,
    val transactionsImported: Int,
    val duplicateTransactionsSkipped: Int,
    val budgetsImported: Int,
    val goalsImported: Int
)

class BackupRestoreService(
    private val database: ExpenseDatabase,
    private val jsonBackupEngine: JsonBackupEngine = JsonBackupEngine()
) {

    /**
     * Builds complete [BackupEnvelope] representing the entire authoritative database state.
     */
    suspend fun createBackupEnvelope(): BackupEnvelope {
        val currency = database.userSettingDao().getSettingDirect("currency") ?: "BDT"

        val salaryDay = database.userSettingDao().getSettingDirect("salary_day")?.toIntOrNull() ?: 1
        val anchorDateStr = database.userSettingDao().getSettingDirect("salary_anchor_date")
        val anchorDate = anchorDateStr?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() } ?: java.time.LocalDate.now()
        val freqStr = database.userSettingDao().getSettingDirect("salary_frequency") ?: "MONTHLY"
        val freq = runCatching { com.expenseapp.salarytracker.domain.model.SalaryFrequency.valueOf(freqStr) }.getOrElse { com.expenseapp.salarytracker.domain.model.SalaryFrequency.MONTHLY }
        val modeStr = database.userSettingDao().getSettingDirect("salary_mode") ?: "CALENDAR_MONTH"
        val mode = runCatching { com.expenseapp.salarytracker.domain.model.ReportingCycleMode.valueOf(modeStr) }.getOrElse { com.expenseapp.salarytracker.domain.model.ReportingCycleMode.CALENDAR_MONTH }

        val salaryConfig = SalaryCycleConfig(
            salaryDayOfMonth = salaryDay,
            anchorDate = anchorDate,
            frequency = freq,
            reportingMode = mode
        )

        val accounts = database.accountDao().getAllAccounts().first().map { it.toDomain() }
        val categories = database.categoryDao().getAllCategories().first().map { it.toDomain() }
        val transactions = database.transactionDao().getAllTransactions().first().map { it.toDomain() }
        val budgets = database.budgetDao().getAllBudgets().first().map { it.toDomain() }
        val goals = database.goalDao().getAllGoals().first().map { it.toDomain() }
        val recurringRules = database.recurringRuleDao().getAllRules().first().map { it.toDomain() }
        val customFields = database.customFieldDao().getAllCustomFields().first().map { it.toDomain() }
        val treeHistory = database.treeHistoryDao().getRecentHistory(limit = 200).first().map { it.toDomain() }

        return BackupEnvelope(
            formatVersion = JsonBackupEngine.CURRENT_FORMAT_VERSION,
            schemaVersion = JsonBackupEngine.CURRENT_SCHEMA_VERSION,
            exportedAtEpochMs = System.currentTimeMillis(),
            appVersion = "1.0.0",
            currency = currency,
            salaryCycleConfig = salaryConfig,
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            goals = goals,
            recurringRules = recurringRules,
            customFields = customFields,
            treeHistory = treeHistory
        )
    }

    /**
     * Completely replaces current database state with [envelope].
     * Executed within an atomic Room transaction.
     */
    suspend fun restoreWipeAndReplace(envelope: BackupEnvelope): Result<Unit> = runCatching {
        database.withTransaction {
            // 1. Wipe existing state
            database.transactionDao().deleteAll()
            database.budgetDao().deleteAll()
            database.goalDao().deleteAll()
            database.treeHistoryDao().deleteAll()
            database.recurringRuleDao().deleteAll()
            database.customFieldDao().deleteAll()
            database.categoryDao().deleteAll()
            database.accountDao().deleteAll()

            // 2. Restore settings
            database.userSettingDao().setSetting(UserSettingEntity("currency", envelope.currency))
            database.userSettingDao().setSetting(UserSettingEntity("salary_day", envelope.salaryCycleConfig.salaryDayOfMonth.toString()))
            database.userSettingDao().setSetting(UserSettingEntity("salary_anchor_date", envelope.salaryCycleConfig.anchorDate.toString()))
            database.userSettingDao().setSetting(UserSettingEntity("salary_frequency", envelope.salaryCycleConfig.frequency.name))
            database.userSettingDao().setSetting(UserSettingEntity("salary_mode", envelope.salaryCycleConfig.reportingMode.name))

            // 3. Restore accounts and categories
            envelope.accounts.forEach { database.accountDao().insert(it.toEntity()) }
            envelope.categories.forEach { database.categoryDao().insert(it.toEntity()) }

            // 4. Restore custom fields
            if (envelope.customFields.isNotEmpty()) {
                database.customFieldDao().insertAll(envelope.customFields.map { it.toEntity() })
            }

            // 5. Restore budgets and goals
            if (envelope.budgets.isNotEmpty()) {
                database.budgetDao().insertAll(envelope.budgets.map { it.toEntity() })
            }
            if (envelope.goals.isNotEmpty()) {
                database.goalDao().insertAll(envelope.goals.map { it.toEntity() })
            }

            // 6. Restore recurring rules and tree history
            if (envelope.recurringRules.isNotEmpty()) {
                database.recurringRuleDao().insertAll(envelope.recurringRules.map { it.toEntity() })
            }
            if (envelope.treeHistory.isNotEmpty()) {
                database.treeHistoryDao().insertAll(envelope.treeHistory.map { it.toEntity() })
            }

            // 7. Restore transactions
            if (envelope.transactions.isNotEmpty()) {
                database.transactionDao().insertAll(envelope.transactions.map { it.toEntity() })
            }
        }
    }

    /**
     * Merges [envelope] into current database state, deduplicating incoming transactions.
     */
    suspend fun restoreMerge(envelope: BackupEnvelope): Result<MergeSummary> = runCatching {
        database.withTransaction {
            var accountsAdded = 0
            var categoriesAdded = 0
            var txImported = 0
            var duplicatesSkipped = 0

            // 1. Merge accounts (skip existing names or IDs)
            val existingAccounts = database.accountDao().getAllAccounts().first()
            val existingAccNames = existingAccounts.map { it.name.trim().lowercase() }.toSet()
            for (acc in envelope.accounts) {
                if (acc.name.trim().lowercase() !in existingAccNames) {
                    database.accountDao().insert(acc.toEntity().copy(id = 0L))
                    accountsAdded++
                }
            }

            // 2. Merge categories
            val existingCategories = database.categoryDao().getAllCategories().first()
            val existingCatNames = existingCategories.map { it.name.trim().lowercase() }.toSet()
            for (cat in envelope.categories) {
                if (cat.name.trim().lowercase() !in existingCatNames) {
                    database.categoryDao().insert(cat.toEntity().copy(id = 0L))
                    categoriesAdded++
                }
            }

            // 3. Merge custom fields
            val existingFields = database.customFieldDao().getAllCustomFields().first()
            val existingFieldNames = existingFields.map { it.name.trim().lowercase() }.toSet()
            for (cf in envelope.customFields) {
                if (cf.name.trim().lowercase() !in existingFieldNames) {
                    database.customFieldDao().insert(cf.toEntity().copy(id = 0L))
                }
            }

            // 4. Merge transactions with multi-field duplicate detection
            // Identity: (date, amountMinorUnits, accountId, note.trim())
            val existingTx = database.transactionDao().getAllTransactions().first()
            val existingTxSignatures = existingTx.map { tx ->
                "${tx.date}_${tx.amountMinorUnits}_${tx.accountId}_${tx.note.trim().lowercase()}"
            }.toSet()

            val txToInsert = mutableListOf<com.expenseapp.salarytracker.data.entity.TransactionEntity>()
            for (tx in envelope.transactions) {
                val sig = "${tx.date}_${tx.amount.amountInMinorUnits}_${tx.accountId}_${tx.note.trim().lowercase()}"
                if (sig in existingTxSignatures) {
                    duplicatesSkipped++
                } else {
                    txToInsert.add(tx.toEntity().copy(id = 0L))
                    txImported++
                }
            }

            if (txToInsert.isNotEmpty()) {
                database.transactionDao().insertAll(txToInsert)
            }

            // 5. Budgets & Goals
            if (envelope.budgets.isNotEmpty()) {
                database.budgetDao().insertAll(envelope.budgets.map { it.toEntity().copy(id = 0L) })
            }
            if (envelope.goals.isNotEmpty()) {
                database.goalDao().insertAll(envelope.goals.map { it.toEntity().copy(id = 0L) })
            }

            MergeSummary(
                accountsAdded = accountsAdded,
                categoriesAdded = categoriesAdded,
                transactionsImported = txImported,
                duplicateTransactionsSkipped = duplicatesSkipped,
                budgetsImported = envelope.budgets.size,
                goalsImported = envelope.goals.size
            )
        }
    }
}
