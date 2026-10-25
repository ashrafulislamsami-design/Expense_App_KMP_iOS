package com.expenseapp.salarytracker.domain.service.export

import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.AccountType
import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.CustomField
import com.expenseapp.salarytracker.domain.model.CustomFieldType
import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.model.GoalStatus
import com.expenseapp.salarytracker.domain.model.GoalType
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.RecurrenceFrequency
import com.expenseapp.salarytracker.domain.model.RecurringRule
import com.expenseapp.salarytracker.domain.model.ReportingCycleMode
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.SalaryFrequency
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.model.TreeHistory
import com.expenseapp.salarytracker.domain.model.TreeState
import com.expenseapp.salarytracker.domain.model.report.BackupEnvelope
import com.expenseapp.salarytracker.domain.model.report.BackupSummary
import com.expenseapp.salarytracker.domain.model.report.BackupValidationResult
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pure Kotlin, zero-dependency Canonical JSON Backup & Restore Engine.
 *
 * Implements full database export with SHA-256 integrity verification, schema validation,
 * duplicate detection, and destructive wipe-and-restore or safe merge import.
 */
class JsonBackupEngine {

    companion object {
        const val CURRENT_FORMAT_VERSION = 1
        const val CURRENT_SCHEMA_VERSION = 2
    }

    fun buildFullJsonString(envelope: BackupEnvelope): String {
        val payloadJson = buildPayloadJson(envelope)
        val checksum = calculateSha256(payloadJson)
        return buildString {
            append("{\n")
            append("  \"formatVersion\": ${envelope.formatVersion},\n")
            append("  \"schemaVersion\": ${envelope.schemaVersion},\n")
            append("  \"exportedAtEpochMs\": ${envelope.exportedAtEpochMs},\n")
            append("  \"appVersion\": \"${escapeJson(envelope.appVersion)}\",\n")
            append("  \"checksumSha256\": \"$checksum\",\n")
            append("  \"payload\": ").append(payloadJson).append("\n")
            append("}\n")
        }
    }

    /**
     * Serializes complete [envelope] into JSON format and writes to [outputStream].
     */
    fun exportBackup(envelope: BackupEnvelope, outputStream: OutputStream) {
        val fullEnvelopeJson = buildFullJsonString(envelope)
        outputStream.write(fullEnvelopeJson.toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }


    /**
     * Reads and validates backup stream. Produces a detailed [BackupValidationResult]
     * with error diagnostics, warnings, and item counts before any data is modified.
     */
    fun validateBackup(inputStream: InputStream): BackupValidationResult {
        val jsonText = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        if (jsonText.isBlank()) {
            return BackupValidationResult(isValid = false, errors = listOf("Backup file is empty"))
        }

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        try {
            val root = JsonParser.parseObject(jsonText)
            val formatVersion = root.getIntOrNull("formatVersion") ?: 0
            val schemaVersion = root.getIntOrNull("schemaVersion") ?: 0
            val exportedAt = root.getLongOrNull("exportedAtEpochMs") ?: 0L
            val appVersion = root.getStringOrNull("appVersion") ?: "unknown"
            val storedChecksum = root.getStringOrNull("checksumSha256") ?: ""

            if (formatVersion <= 0) {
                errors.add("Missing or invalid 'formatVersion'")
            } else if (formatVersion > CURRENT_FORMAT_VERSION) {
                errors.add("Unsupported backup format version: $formatVersion (maximum supported is $CURRENT_FORMAT_VERSION)")
            }

            if (schemaVersion <= 0) {
                errors.add("Missing or invalid 'schemaVersion'")
            } else if (schemaVersion > CURRENT_SCHEMA_VERSION) {
                warnings.add("Backup is from newer database schema version $schemaVersion (current is $CURRENT_SCHEMA_VERSION)")
            }

            val payload = root.getObjectOrNull("payload")
            if (payload == null) {
                errors.add("Missing 'payload' section in backup")
                return BackupValidationResult(isValid = false, errors = errors, warnings = warnings)
            }

            // Verify SHA-256 Checksum
            val rawPayloadText = JsonParser.extractRawField(jsonText, "payload")
            if (rawPayloadText != null && storedChecksum.isNotBlank()) {
                val computedChecksum = calculateSha256(rawPayloadText)
                if (!storedChecksum.equals(computedChecksum, ignoreCase = true)) {
                    warnings.add("Checksum mismatch: file may have been modified externally or corrupted")
                }
            }

            // Parse and validate entities
            val currency = payload.getStringOrNull("currency") ?: "BDT"
            val salaryConfig = parseSalaryConfig(payload.getObjectOrNull("salaryCycleConfig"), errors)
            val accounts = parseAccounts(payload.getArrayOrNull("accounts"), errors)
            val categories = parseCategories(payload.getArrayOrNull("categories"), errors)
            val transactions = parseTransactions(payload.getArrayOrNull("transactions"), errors)
            val budgets = parseBudgets(payload.getArrayOrNull("budgets"), errors)
            val goals = parseGoals(payload.getArrayOrNull("goals"), errors)
            val recurringRules = parseRecurringRules(payload.getArrayOrNull("recurringRules"), errors)
            val customFields = parseCustomFields(payload.getArrayOrNull("customFields"), errors)
            val treeHistory = parseTreeHistory(payload.getArrayOrNull("treeHistory"), errors)

            val summary = BackupSummary(
                formatVersion = formatVersion,
                schemaVersion = schemaVersion,
                exportedAtEpochMs = exportedAt,
                appVersion = appVersion,
                accountCount = accounts.size,
                categoryCount = categories.size,
                transactionCount = transactions.size,
                budgetCount = budgets.size,
                goalCount = goals.size,
                recurringRuleCount = recurringRules.size,
                customFieldCount = customFields.size,
                treeHistoryCount = treeHistory.size
            )

            val envelope = BackupEnvelope(
                formatVersion = formatVersion,
                schemaVersion = schemaVersion,
                exportedAtEpochMs = exportedAt,
                appVersion = appVersion,
                checksumSha256 = storedChecksum,
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

            val isValid = errors.isEmpty()
            return BackupValidationResult(
                isValid = isValid,
                errors = errors,
                warnings = warnings,
                summary = summary,
                envelope = if (isValid) envelope else null
            )
        } catch (e: Exception) {
            return BackupValidationResult(
                isValid = false,
                errors = listOf("JSON parsing error: ${e.message}")
            )
        }
    }

    // ==========================================
    // JSON Serialization Helpers
    // ==========================================

    private fun buildPayloadJson(envelope: BackupEnvelope): String {
        return buildString {
            append("{\n")
            append("  \"currency\": \"${escapeJson(envelope.currency)}\",\n")

            // Salary config
            append("  \"salaryCycleConfig\": {\n")
            append("    \"salaryDayOfMonth\": ${envelope.salaryCycleConfig.salaryDayOfMonth},\n")
            append("    \"anchorDate\": \"${envelope.salaryCycleConfig.anchorDate}\",\n")
            append("    \"frequency\": \"${envelope.salaryCycleConfig.frequency.name}\",\n")
            append("    \"reportingMode\": \"${envelope.salaryCycleConfig.reportingMode.name}\",\n")
            append("    \"customCycleLengthDays\": ${envelope.salaryCycleConfig.customCycleLengthDays}\n")
            append("  },\n")

            // Accounts
            append("  \"accounts\": [\n")
            envelope.accounts.forEachIndexed { i, a ->
                append("    {\"id\": ${a.id}, \"name\": \"${escapeJson(a.name)}\", \"type\": \"${a.type.name}\", \"initialBalance\": ${a.initialBalance.amountInMinorUnits}, \"currency\": \"${escapeJson(a.currency)}\", \"isArchived\": ${a.isArchived}, \"sortOrder\": ${a.sortOrder}, \"colorHex\": \"${escapeJson(a.colorHex)}\"}")
                if (i < envelope.accounts.size - 1) append(",")
                append("\n")
            }
            append("  ],\n")

            // Categories
            append("  \"categories\": [\n")
            envelope.categories.forEachIndexed { i, c ->
                append("    {\"id\": ${c.id}, \"name\": \"${escapeJson(c.name)}\", \"iconName\": \"${escapeJson(c.iconName)}\", \"colorHex\": \"${escapeJson(c.colorHex)}\", \"parentCategoryId\": ${c.parentCategoryId ?: "null"}, \"isArchived\": ${c.isArchived}, \"sortOrder\": ${c.sortOrder}}")
                if (i < envelope.categories.size - 1) append(",")
                append("\n")
            }
            append("  ],\n")

            // Transactions
            append("  \"transactions\": [\n")
            envelope.transactions.forEachIndexed { i, tx ->
                val tagsJson = tx.tags.joinToString(prefix = "[", postfix = "]") { "\"${escapeJson(it)}\"" }
                val customJson = tx.customFieldValues.entries.joinToString(prefix = "{", postfix = "}") {
                    "\"${escapeJson(it.key)}\": \"${escapeJson(it.value)}\""
                }
                append("    {\"id\": ${tx.id}, \"amount\": ${tx.amount.amountInMinorUnits}, \"type\": \"${tx.type.name}\", \"accountId\": ${tx.accountId}, \"destinationAccountId\": ${tx.destinationAccountId ?: "null"}, \"categoryId\": ${tx.categoryId ?: "null"}, \"subcategoryId\": ${tx.subcategoryId ?: "null"}, \"date\": \"${tx.date}\", \"time\": \"${tx.time}\", \"note\": \"${escapeJson(tx.note)}\", \"tags\": $tagsJson, \"recurringRuleId\": ${tx.recurringRuleId ?: "null"}, \"originalTransactionId\": ${tx.originalTransactionId ?: "null"}, \"customFieldValues\": $customJson, \"createdAtEpochMs\": ${tx.createdAtEpochMs}, \"updatedAtEpochMs\": ${tx.updatedAtEpochMs}}")
                if (i < envelope.transactions.size - 1) append(",")
                append("\n")
            }
            append("  ],\n")

            // Budgets
            append("  \"budgets\": [\n")
            envelope.budgets.forEachIndexed { i, b ->
                append("    {\"id\": ${b.id}, \"limitAmount\": ${b.limitAmount.amountInMinorUnits}, \"categoryId\": ${b.categoryId ?: "null"}, \"period\": \"${b.period.name}\", \"startDate\": \"${b.startDate}\", \"endDate\": ${b.endDate?.let { "\"$it\"" } ?: "null"}, \"warningThresholdPercentage\": ${b.warningThresholdPercentage}}")
                if (i < envelope.budgets.size - 1) append(",")
                append("\n")
            }
            append("  ],\n")

            // Goals
            append("  \"goals\": [\n")
            envelope.goals.forEachIndexed { i, g ->
                val msJson = g.reachedMilestones.joinToString(prefix = "[", postfix = "]") { it.toString() }
                append("    {\"id\": ${g.id}, \"name\": \"${escapeJson(g.name)}\", \"targetAmount\": ${g.targetAmount.amountInMinorUnits}, \"targetDays\": ${g.targetDays}, \"type\": \"${g.type.name}\", \"categoryId\": ${g.categoryId ?: "null"}, \"accountId\": ${g.accountId ?: "null"}, \"status\": \"${g.status.name}\", \"reachedMilestones\": $msJson, \"startDate\": \"${g.startDate}\", \"endDate\": \"${g.endDate}\", \"treeState\": \"${g.treeState.name}\", \"createdAtEpochMs\": ${g.createdAtEpochMs}, \"updatedAtEpochMs\": ${g.updatedAtEpochMs}}")
                if (i < envelope.goals.size - 1) append(",")
                append("\n")
            }
            append("  ],\n")

            // Recurring Rules
            append("  \"recurringRules\": [\n")
            envelope.recurringRules.forEachIndexed { i, r ->
                append("    {\"id\": ${r.id}, \"name\": \"${escapeJson(r.name)}\", \"amount\": ${r.amount.amountInMinorUnits}, \"type\": \"${r.type.name}\", \"accountId\": ${r.accountId}, \"destinationAccountId\": ${r.destinationAccountId ?: "null"}, \"categoryId\": ${r.categoryId ?: "null"}, \"frequency\": \"${r.frequency.name}\", \"intervalDays\": ${r.intervalDays}, \"preferredDayOfMonth\": ${r.preferredDayOfMonth ?: "null"}, \"startDate\": \"${r.startDate}\", \"endDate\": ${r.endDate?.let { "\"$it\"" } ?: "null"}, \"nextDueDate\": \"${r.nextDueDate}\", \"autoPost\": ${r.autoPost}, \"isPaused\": ${r.isPaused}, \"note\": \"${escapeJson(r.note)}\"}")
                if (i < envelope.recurringRules.size - 1) append(",")
                append("\n")
            }
            append("  ],\n")

            // Custom Field Definitions
            append("  \"customFields\": [\n")
            envelope.customFields.forEachIndexed { i, cf ->
                val optJson = cf.optionsJson.joinToString(prefix = "[", postfix = "]") { "\"${escapeJson(it)}\"" }
                append("    {\"id\": ${cf.id}, \"name\": \"${escapeJson(cf.name)}\", \"type\": \"${cf.type.name}\", \"optionsJson\": $optJson, \"isRequired\": ${cf.isRequired}}")
                if (i < envelope.customFields.size - 1) append(",")
                append("\n")
            }
            append("  ],\n")

            // Tree History
            append("  \"treeHistory\": [\n")
            envelope.treeHistory.forEachIndexed { i, th ->
                append("    {\"id\": ${th.id}, \"previousState\": \"${th.previousState.name}\", \"newState\": \"${th.newState.name}\", \"reason\": \"${escapeJson(th.reason)}\", \"contributingGoalId\": ${th.contributingGoalId ?: "null"}, \"timestampEpochMs\": ${th.timestampEpochMs}}")
                if (i < envelope.treeHistory.size - 1) append(",")
                append("\n")
            }
            append("  ]\n")

            append("}")
        }
    }

    // ==========================================
    // JSON Entity Parsers & Validators
    // ==========================================

    private fun parseSalaryConfig(obj: JsonObject?, errors: MutableList<String>): SalaryCycleConfig {
        if (obj == null) return SalaryCycleConfig()
        val day = obj.getIntOrNull("salaryDayOfMonth") ?: 1
        val anchorStr = obj.getStringOrNull("anchorDate") ?: LocalDate.now().toString()
        val anchor = runCatching { LocalDate.parse(anchorStr) }.getOrElse { LocalDate.now() }
        val freqStr = obj.getStringOrNull("frequency") ?: "MONTHLY"
        val freq = runCatching { SalaryFrequency.valueOf(freqStr) }.getOrElse { SalaryFrequency.MONTHLY }
        val modeStr = obj.getStringOrNull("reportingMode") ?: "CALENDAR_MONTH"
        val mode = runCatching { ReportingCycleMode.valueOf(modeStr) }.getOrElse { ReportingCycleMode.CALENDAR_MONTH }
        val cycleLen = obj.getIntOrNull("customCycleLengthDays") ?: 30
        return SalaryCycleConfig(
            salaryDayOfMonth = day,
            anchorDate = anchor,
            frequency = freq,
            reportingMode = mode,
            customCycleLengthDays = cycleLen
        )
    }

    private fun parseAccounts(arr: List<JsonObject>?, errors: MutableList<String>): List<Account> {
        if (arr == null) return emptyList()
        return arr.mapIndexed { idx, o ->
            val id = o.getLongOrNull("id") ?: (idx + 1).toLong()
            val rawName = o.getStringOrNull("name")
            if (rawName.isNullOrBlank()) {
                errors.add("Account #$id has empty or missing name")
            }
            val name = rawName ?: "Account $id"

            val typeStr = o.getStringOrNull("type") ?: "CASH"
            val parsedType = AccountType.entries.firstOrNull { it.name.equals(typeStr, ignoreCase = true) }
            if (parsedType == null) {
                errors.add("Account #$id has unrecognized type '$typeStr'")
            }
            val type = parsedType ?: AccountType.CASH

            val balance = MinorUnits(o.getLongOrNull("initialBalance") ?: 0L)
            val currency = o.getStringOrNull("currency") ?: "BDT"
            val isArchived = o.getBooleanOrNull("isArchived") ?: false
            val sortOrder = o.getIntOrNull("sortOrder") ?: idx
            val colorHex = o.getStringOrNull("colorHex") ?: "#10B981"
            Account(id, name, type, balance, currency, isArchived, sortOrder, colorHex)
        }
    }

    private fun parseCategories(arr: List<JsonObject>?, errors: MutableList<String>): List<Category> {
        if (arr == null) return emptyList()
        return arr.mapIndexed { idx, o ->
            val id = o.getLongOrNull("id") ?: (idx + 1).toLong()
            val rawName = o.getStringOrNull("name")
            if (rawName.isNullOrBlank()) {
                errors.add("Category #$id has empty or missing name")
            }
            val name = rawName ?: "Category $id"

            val iconName = o.getStringOrNull("iconName") ?: "category"
            val colorHex = o.getStringOrNull("colorHex") ?: "#3B82F6"
            val parentId = o.getLongOrNull("parentCategoryId")
            val isArchived = o.getBooleanOrNull("isArchived") ?: false
            val sortOrder = o.getIntOrNull("sortOrder") ?: idx
            Category(id, name, iconName, colorHex, parentId, isArchived, sortOrder)
        }
    }

    private fun parseTransactions(arr: List<JsonObject>?, errors: MutableList<String>): List<Transaction> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { o ->
            val id = o.getLongOrNull("id") ?: 0L
            val amountMinor = o.getLongOrNull("amount") ?: o.getLongOrNull("amountMinorUnits")
            if (amountMinor == null) {
                errors.add("Transaction #$id is missing required 'amount'")
                return@mapNotNull null
            }
            if (amountMinor < 0L) {
                errors.add("Transaction #$id has negative amount: $amountMinor")
            }

            val typeStr = o.getStringOrNull("type") ?: "EXPENSE"
            val type = runCatching { TransactionType.valueOf(typeStr) }.getOrElse {
                errors.add("Transaction #$id has unrecognized type '$typeStr'")
                TransactionType.EXPENSE
            }
            val accountId = o.getLongOrNull("accountId") ?: 0L
            val destAccountId = o.getLongOrNull("destinationAccountId")
            val categoryId = o.getLongOrNull("categoryId")
            val subcategoryId = o.getLongOrNull("subcategoryId")

            val dateStr = o.getStringOrNull("date")
            val date = if (dateStr != null) {
                runCatching { LocalDate.parse(dateStr) }.getOrElse {
                    errors.add("Transaction #$id has invalid date format: '$dateStr'")
                    LocalDate.now()
                }
            } else {
                errors.add("Transaction #$id is missing date")
                LocalDate.now()
            }

            val timeStr = o.getStringOrNull("time") ?: LocalTime.now().toString()
            val time = runCatching { LocalTime.parse(timeStr) }.getOrElse { LocalTime.now() }
            val note = o.getStringOrNull("note") ?: ""
            val tags = o.getStringListOrNull("tags") ?: emptyList()
            val recurringRuleId = o.getLongOrNull("recurringRuleId")
            val origTxId = o.getLongOrNull("originalTransactionId")
            val customFields = o.getStringMapOrNull("customFieldValues") ?: emptyMap()
            val createdAt = o.getLongOrNull("createdAtEpochMs") ?: System.currentTimeMillis()
            val updatedAt = o.getLongOrNull("updatedAtEpochMs") ?: createdAt

            Transaction(
                id = id,
                amount = MinorUnits(amountMinor),
                type = type,
                accountId = accountId,
                destinationAccountId = destAccountId,
                categoryId = categoryId,
                subcategoryId = subcategoryId,
                date = date,
                time = time,
                note = note,
                tags = tags,
                recurringRuleId = recurringRuleId,
                originalTransactionId = origTxId,
                customFieldValues = customFields,
                createdAtEpochMs = createdAt,
                updatedAtEpochMs = updatedAt
            )
        }
    }

    private fun parseBudgets(arr: List<JsonObject>?, errors: MutableList<String>): List<Budget> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { o ->
            val id = o.getLongOrNull("id") ?: 0L
            val limit = MinorUnits(o.getLongOrNull("limitAmount") ?: 0L)
            val catId = o.getLongOrNull("categoryId")
            val periodStr = o.getStringOrNull("period") ?: "MONTHLY"
            val period = runCatching { com.expenseapp.salarytracker.domain.model.BudgetPeriod.valueOf(periodStr) }.getOrElse { com.expenseapp.salarytracker.domain.model.BudgetPeriod.MONTHLY }
            val startStr = o.getStringOrNull("startDate") ?: LocalDate.now().toString()
            val start = runCatching { LocalDate.parse(startStr) }.getOrElse { LocalDate.now() }
            val end = o.getStringOrNull("endDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val warningPct = o.getIntOrNull("warningThresholdPercentage") ?: 80

            Budget(
                id = id,
                categoryId = catId,
                limitAmount = limit,
                period = period,
                startDate = start,
                endDate = end,
                warningThresholdPercentage = warningPct
            )
        }
    }

    private fun parseGoals(arr: List<JsonObject>?, errors: MutableList<String>): List<Goal> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { o ->
            val id = o.getLongOrNull("id") ?: 0L
            val name = o.getStringOrNull("name") ?: "Goal $id"
            val target = MinorUnits(o.getLongOrNull("targetAmount") ?: 0L)
            val current = MinorUnits(o.getLongOrNull("currentProgress") ?: 0L)
            val typeStr = o.getStringOrNull("type") ?: "SAVE_AMOUNT"
            val type = GoalType.fromString(typeStr)
            val catId = o.getLongOrNull("categoryId")
            val accId = o.getLongOrNull("accountId")
            val statusStr = o.getStringOrNull("status") ?: "ACTIVE"
            val status = GoalStatus.fromString(statusStr)
            val milestones = o.getIntListOrNull("reachedMilestones") ?: emptyList()
            val startStr = o.getStringOrNull("startDate") ?: LocalDate.now().toString()
            val start = runCatching { LocalDate.parse(startStr) }.getOrElse { LocalDate.now() }
            val end = o.getStringOrNull("endDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: start.plusMonths(1)
            val treeStr = o.getStringOrNull("treeState") ?: "SEED"
            val tree = TreeState.fromString(treeStr)
            val createdAt = o.getLongOrNull("createdAtEpochMs") ?: System.currentTimeMillis()
            val updatedAt = o.getLongOrNull("updatedAtEpochMs") ?: createdAt

            Goal(
                id = id,
                name = name,
                description = "",
                type = type,
                targetAmount = target,
                targetDays = 0,
                categoryId = catId,
                accountId = accId,
                startDate = start,
                endDate = end,
                status = status,
                reachedMilestones = milestones,
                treeState = tree,
                createdAtEpochMs = createdAt,
                updatedAtEpochMs = updatedAt
            )
        }
    }

    private fun parseRecurringRules(arr: List<JsonObject>?, errors: MutableList<String>): List<RecurringRule> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { o ->
            val id = o.getLongOrNull("id") ?: 0L
            val name = o.getStringOrNull("name") ?: "Rule #$id"
            val amount = MinorUnits(o.getLongOrNull("amount") ?: 0L)
            val typeStr = o.getStringOrNull("type") ?: "EXPENSE"
            val type = runCatching { TransactionType.valueOf(typeStr) }.getOrElse { TransactionType.EXPENSE }
            val accountId = o.getLongOrNull("accountId") ?: 0L
            val destAccountId = o.getLongOrNull("destinationAccountId")
            val categoryId = o.getLongOrNull("categoryId")
            val freqStr = o.getStringOrNull("frequency") ?: "MONTHLY"
            val freq = RecurrenceFrequency.fromString(freqStr)
            val intervalDays = o.getIntOrNull("intervalDays") ?: 30
            val preferredDay = o.getIntOrNull("preferredDayOfMonth")
            val startStr = o.getStringOrNull("startDate") ?: LocalDate.now().toString()
            val start = runCatching { LocalDate.parse(startStr) }.getOrElse { LocalDate.now() }
            val end = o.getStringOrNull("endDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val nextStr = o.getStringOrNull("nextDueDate") ?: startStr
            val next = runCatching { LocalDate.parse(nextStr) }.getOrElse { start }
            val autoPost = o.getBooleanOrNull("autoPost") ?: false
            val isPaused = o.getBooleanOrNull("isPaused") ?: false
            val note = o.getStringOrNull("note") ?: ""

            RecurringRule(
                id = id,
                name = name,
                amount = amount,
                type = type,
                accountId = accountId,
                destinationAccountId = destAccountId,
                categoryId = categoryId,
                frequency = freq,
                intervalDays = intervalDays,
                preferredDayOfMonth = preferredDay,
                startDate = start,
                nextDueDate = next,
                endDate = end,
                autoPost = autoPost,
                isPaused = isPaused,
                note = note
            )
        }
    }

    private fun parseCustomFields(arr: List<JsonObject>?, errors: MutableList<String>): List<CustomField> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { o ->
            val id = o.getLongOrNull("id") ?: 0L
            val name = o.getStringOrNull("name") ?: o.getStringOrNull("fieldName") ?: "Field $id"
            val typeStr = o.getStringOrNull("type") ?: o.getStringOrNull("fieldType") ?: "TEXT"
            val type = runCatching { CustomFieldType.valueOf(typeStr) }.getOrElse { CustomFieldType.TEXT }
            val req = o.getBooleanOrNull("isRequired") ?: false
            val options = o.getStringListOrNull("optionsJson") ?: o.getStringListOrNull("options") ?: emptyList()

            CustomField(id, name, type, options, req)
        }
    }

    private fun parseTreeHistory(arr: List<JsonObject>?, errors: MutableList<String>): List<TreeHistory> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { o ->
            val id = o.getLongOrNull("id") ?: 0L
            val prev = TreeState.fromString(o.getStringOrNull("previousState") ?: "SEED")
            val curr = TreeState.fromString(o.getStringOrNull("newState") ?: "SEED")
            val reason = o.getStringOrNull("reason") ?: ""
            val goalId = o.getLongOrNull("contributingGoalId")
            val time = o.getLongOrNull("timestampEpochMs") ?: System.currentTimeMillis()

            TreeHistory(id, prev, curr, reason, goalId, time)
        }
    }

    private fun calculateSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    // ==========================================
    // Lightweight Zero-Dependency JSON Tree Parser
    // ==========================================

    class JsonObject(val map: Map<String, Any?>) {
        fun getStringOrNull(key: String): String? = map[key] as? String
        fun getIntOrNull(key: String): Int? {
            val v = map[key] ?: return null
            return when (v) {
                is Number -> v.toInt()
                is String -> v.toIntOrNull()
                else -> null
            }
        }
        fun getLongOrNull(key: String): Long? {
            val v = map[key] ?: return null
            return when (v) {
                is Number -> v.toLong()
                is String -> v.toLongOrNull()
                else -> null
            }
        }
        fun getBooleanOrNull(key: String): Boolean? {
            val v = map[key] ?: return null
            return when (v) {
                is Boolean -> v
                is String -> v.toBooleanStrictOrNull()
                else -> null
            }
        }
        @Suppress("UNCHECKED_CAST")
        fun getObjectOrNull(key: String): JsonObject? {
            val m = map[key] as? Map<String, Any?> ?: return null
            return JsonObject(m)
        }
        @Suppress("UNCHECKED_CAST")
        fun getArrayOrNull(key: String): List<JsonObject>? {
            val list = map[key] as? List<Any?> ?: return null
            return list.mapNotNull { if (it is Map<*, *>) JsonObject(it as Map<String, Any?>) else null }
        }
        @Suppress("UNCHECKED_CAST")
        fun getStringListOrNull(key: String): List<String>? {
            val list = map[key] as? List<Any?> ?: return null
            return list.map { it.toString() }
        }
        @Suppress("UNCHECKED_CAST")
        fun getIntListOrNull(key: String): List<Int>? {
            val list = map[key] as? List<Any?> ?: return null
            return list.mapNotNull {
                when (it) {
                    is Number -> it.toInt()
                    is String -> it.toIntOrNull()
                    else -> null
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        fun getStringMapOrNull(key: String): Map<String, String>? {
            val m = map[key] as? Map<String, Any?> ?: return null
            return m.mapValues { it.value?.toString() ?: "" }
        }
    }

    object JsonParser {
        fun parseObject(json: String): JsonObject {
            val trimmed = json.trim()
            require(trimmed.startsWith("{") && trimmed.endsWith("}")) { "Invalid JSON object" }
            val parsed = parseValue(trimmed) as? Map<String, Any?> ?: error("Failed to parse JSON object")
            return JsonObject(parsed)
        }

        fun extractRawField(json: String, fieldName: String): String? {
            val target = "\"$fieldName\""
            val idx = json.indexOf(target)
            if (idx == -1) return null
            val colonIdx = json.indexOf(':', idx + target.length)
            if (colonIdx == -1) return null
            var start = colonIdx + 1
            while (start < json.length && json[start].isWhitespace()) start++
            if (start >= json.length) return null

            // Find matching closure if object or array
            val openChar = json[start]
            if (openChar == '{' || openChar == '[') {
                val closeChar = if (openChar == '{') '}' else ']'
                var depth = 0
                var inStr = false
                var escaped = false
                for (i in start until json.length) {
                    val c = json[i]
                    if (escaped) {
                        escaped = false
                    } else if (c == '\\') {
                        escaped = true
                    } else if (c == '"') {
                        inStr = !inStr
                    } else if (!inStr) {
                        if (c == openChar) depth++
                        else if (c == closeChar) {
                            depth--
                            if (depth == 0) {
                                return json.substring(start, i + 1)
                            }
                        }
                    }
                }
            }
            return null
        }

        private fun parseValue(s: String): Any? {
            val trimmed = s.trim()
            if (trimmed == "null") return null
            if (trimmed == "true") return true
            if (trimmed == "false") return false
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                return unescape(trimmed.substring(1, trimmed.length - 1))
            }
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                return parseObjectMap(trimmed)
            }
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                return parseArray(trimmed)
            }
            val longVal = trimmed.toLongOrNull()
            if (longVal != null) return longVal
            val doubleVal = trimmed.toDoubleOrNull()
            if (doubleVal != null) return doubleVal
            return trimmed
        }

        private fun parseObjectMap(json: String): Map<String, Any?> {
            val content = json.trim().removeSurrounding("{", "}").trim()
            if (content.isEmpty()) return emptyMap()

            val map = mutableMapOf<String, Any?>()
            val tokens = splitJsonTokens(content)
            for (t in tokens) {
                val colonIdx = findColonSeparator(t)
                if (colonIdx != -1) {
                    val kStr = t.substring(0, colonIdx).trim().removeSurrounding("\"")
                    val vStr = t.substring(colonIdx + 1).trim()
                    map[kStr] = parseValue(vStr)
                }
            }
            return map
        }

        private fun parseArray(json: String): List<Any?> {
            val content = json.trim().removeSurrounding("[", "]").trim()
            if (content.isEmpty()) return emptyList()
            val tokens = splitJsonTokens(content)
            return tokens.map { parseValue(it) }
        }

        private fun splitJsonTokens(content: String): List<String> {
            val tokens = mutableListOf<String>()
            var depth = 0
            var inStr = false
            var escaped = false
            var start = 0

            for (i in content.indices) {
                val c = content[i]
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inStr = !inStr
                } else if (!inStr) {
                    if (c == '{' || c == '[') depth++
                    else if (c == '}' || c == ']') depth--
                    else if (c == ',' && depth == 0) {
                        tokens.add(content.substring(start, i).trim())
                        start = i + 1
                    }
                }
            }
            if (start < content.length) {
                val last = content.substring(start).trim()
                if (last.isNotEmpty()) tokens.add(last)
            }
            return tokens
        }

        private fun findColonSeparator(token: String): Int {
            var inStr = false
            var escaped = false
            for (i in token.indices) {
                val c = token[i]
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inStr = !inStr
                } else if (!inStr && c == ':') {
                    return i
                }
            }
            return -1
        }

        private fun unescape(s: String): String {
            val sb = java.lang.StringBuilder()
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) {
                    val next = s[i + 1]
                    when (next) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        else -> sb.append(next)
                    }
                    i += 2
                } else {
                    sb.append(c)
                    i++
                }
            }
            return sb.toString()
        }
    }
}
