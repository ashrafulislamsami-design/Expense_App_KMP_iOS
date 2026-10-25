package com.expenseapp.salarytracker.domain.service.export

import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.AccountType
import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.model.BudgetPeriod
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime

class JsonBackupEngineTest {

    private val jsonEngine = JsonBackupEngine()

    private fun createSampleBackupEnvelope(): BackupEnvelope {
        val account = Account(
            id = 1L,
            name = "City Bank Savings",
            type = AccountType.BANK,
            initialBalance = MinorUnits(5000000L),
            currency = "BDT",
            isArchived = false,
            sortOrder = 1,
            colorHex = "#3B82F6"
        )

        val category = Category(
            id = 1L,
            name = "Groceries",
            iconName = "shopping_cart",
            colorHex = "#10B981",
            parentCategoryId = null,
            isArchived = false,
            sortOrder = 1
        )

        val transaction = Transaction(
            id = 1L,
            amount = MinorUnits(25000L),
            type = TransactionType.EXPENSE,
            accountId = 1L,
            destinationAccountId = null,
            categoryId = 1L,
            subcategoryId = null,
            date = LocalDate.of(2026, 9, 20),
            time = LocalTime.of(15, 30),
            note = "Supermarket run",
            tags = listOf("food", "home"),
            recurringRuleId = null,
            originalTransactionId = null,
            customFieldValues = mapOf("PaymentMode" to "Card"),
            createdAtEpochMs = 1759240000000L,
            updatedAtEpochMs = 1759240000000L
        )

        val budget = Budget(
            id = 1L,
            limitAmount = MinorUnits(2000000L),
            categoryId = 1L,
            period = BudgetPeriod.MONTHLY,
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30),
            warningThresholdPercentage = 80
        )

        val goal = Goal(
            id = 1L,
            name = "MacBook Savings",
            targetAmount = MinorUnits(25000000L),
            type = GoalType.SAVE_AMOUNT,
            categoryId = null,
            accountId = 1L,
            status = GoalStatus.ACTIVE,
            reachedMilestones = listOf(25, 50),
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 12, 31),
            treeState = TreeState.GROWING,
            createdAtEpochMs = 1759240000000L,
            updatedAtEpochMs = 1759240000000L
        )

        val recurring = RecurringRule(
            id = 1L,
            name = "Internet Bill",
            amount = MinorUnits(150000L),
            type = TransactionType.EXPENSE,
            accountId = 1L,
            destinationAccountId = null,
            categoryId = 1L,
            frequency = RecurrenceFrequency.MONTHLY,
            intervalDays = 30,
            preferredDayOfMonth = 5,
            startDate = LocalDate.of(2026, 1, 1),
            nextDueDate = LocalDate.of(2026, 10, 5),
            endDate = null,
            autoPost = false,
            isPaused = false,
            note = "Fiber line"
        )

        val customField = CustomField(
            id = 1L,
            name = "Project Code",
            type = CustomFieldType.TEXT,
            optionsJson = emptyList(),
            isRequired = false
        )

        val treeHistory = TreeHistory(
            id = 1L,
            previousState = TreeState.YOUNG_PLANT,
            newState = TreeState.GROWING,
            reason = "Saved 50% for MacBook",
            contributingGoalId = 1L,
            timestampEpochMs = 1759240000000L
        )

        return BackupEnvelope(
            formatVersion = 1,
            schemaVersion = 2,
            exportedAtEpochMs = 1759240000000L,
            appVersion = "1.0.0",
            currency = "BDT",
            salaryCycleConfig = SalaryCycleConfig(
                salaryDayOfMonth = 1,
                anchorDate = LocalDate.of(2026, 9, 1),
                frequency = SalaryFrequency.MONTHLY,
                reportingMode = ReportingCycleMode.CALENDAR_MONTH
            ),
            accounts = listOf(account),
            categories = listOf(category),
            transactions = listOf(transaction),
            budgets = listOf(budget),
            goals = listOf(goal),
            recurringRules = listOf(recurring),
            customFields = listOf(customField),
            treeHistory = listOf(treeHistory)
        )
    }

    @Test
    fun `exportBackup and validateBackup perform perfect round-trip restoration`() {
        val originalEnvelope = createSampleBackupEnvelope()
        val out = ByteArrayOutputStream()

        jsonEngine.exportBackup(originalEnvelope, out)
        val jsonBytes = out.toByteArray()
        assertTrue("JSON output must not be empty", jsonBytes.isNotEmpty())

        val validation = jsonEngine.validateBackup(ByteArrayInputStream(jsonBytes))

        assertTrue("Validation must succeed for valid backup", validation.isValid)
        assertTrue("Validation errors must be empty", validation.errors.isEmpty())
        assertNotNull("Summary must be populated", validation.summary)
        assertEquals(1, validation.summary?.accountCount)
        assertEquals(1, validation.summary?.categoryCount)
        assertEquals(1, validation.summary?.transactionCount)
        assertEquals(1, validation.summary?.budgetCount)
        assertEquals(1, validation.summary?.goalCount)
        assertEquals(1, validation.summary?.recurringRuleCount)
        assertEquals(1, validation.summary?.customFieldCount)
        assertEquals(1, validation.summary?.treeHistoryCount)

        val restored = validation.envelope
        assertNotNull("Restored envelope must not be null", restored)
        assertEquals(originalEnvelope.currency, restored?.currency)
        assertEquals(originalEnvelope.accounts.size, restored?.accounts?.size)
        assertEquals("City Bank Savings", restored?.accounts?.first()?.name)
        assertEquals(MinorUnits(5000000L), restored?.accounts?.first()?.initialBalance)
        assertEquals("Groceries", restored?.categories?.first()?.name)
        assertEquals(MinorUnits(25000L), restored?.transactions?.first()?.amount)
        assertEquals(listOf(25, 50), restored?.goals?.first()?.reachedMilestones)
        assertEquals("Internet Bill", restored?.recurringRules?.first()?.name)
        assertEquals("Project Code", restored?.customFields?.first()?.name)
    }

    @Test
    fun `validateBackup detects empty backup stream`() {
        val validation = jsonEngine.validateBackup(ByteArrayInputStream(byteArrayOf()))
        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.contains("empty") })
    }

    @Test
    fun `validateBackup detects corrupted JSON syntax`() {
        val malformedJson = "{ \"formatVersion\": 1, \"payload\": "
        val validation = jsonEngine.validateBackup(ByteArrayInputStream(malformedJson.toByteArray(StandardCharsets.UTF_8)))
        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.contains("parsing error") || it.contains("Invalid JSON") })
    }

    @Test
    fun `validateBackup rejects unsupported higher format versions`() {
        val unsupportedJson = """
            {
                "formatVersion": 999,
                "schemaVersion": 2,
                "payload": {}
            }
        """.trimIndent()

        val validation = jsonEngine.validateBackup(ByteArrayInputStream(unsupportedJson.toByteArray(StandardCharsets.UTF_8)))
        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.contains("Unsupported backup format version") })
    }

    @Test
    fun `validateBackup flags missing payload`() {
        val missingPayloadJson = """
            {
                "formatVersion": 1,
                "schemaVersion": 2
            }
        """.trimIndent()

        val validation = jsonEngine.validateBackup(ByteArrayInputStream(missingPayloadJson.toByteArray(StandardCharsets.UTF_8)))
        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.contains("Missing 'payload'") })
        org.junit.Assert.assertNull("Envelope must be null on failure", validation.envelope)
    }

    @Test
    fun `validateBackup flags invalid account data and nullifies envelope preventing DB mutation`() {
        val corruptedAccountJson = """
            {
                "formatVersion": 1,
                "schemaVersion": 2,
                "payload": {
                    "currency": "BDT",
                    "accounts": [
                        {
                            "name": "",
                            "type": "INVALID_TYPE"
                        }
                    ]
                }
            }
        """.trimIndent()

        val validation = jsonEngine.validateBackup(ByteArrayInputStream(corruptedAccountJson.toByteArray(StandardCharsets.UTF_8)))
        assertFalse("Corrupted account must fail validation", validation.isValid)
        assertTrue("Validation must report errors", validation.errors.isNotEmpty())
        org.junit.Assert.assertNull("Failed validation must never produce an envelope", validation.envelope)
    }

    @Test
    fun `validateBackup flags negative transaction amounts and invalid dates`() {
        val invalidTxJson = """
            {
                "formatVersion": 1,
                "schemaVersion": 2,
                "payload": {
                    "currency": "BDT",
                    "transactions": [
                        {
                            "amountMinorUnits": -500,
                            "type": "EXPENSE",
                            "date": "invalid-date-string"
                        }
                    ]
                }
            }
        """.trimIndent()

        val validation = jsonEngine.validateBackup(ByteArrayInputStream(invalidTxJson.toByteArray(StandardCharsets.UTF_8)))
        assertFalse("Invalid transaction must fail validation", validation.isValid)
        assertTrue("Must report invalid amount or date", validation.errors.isNotEmpty())
        org.junit.Assert.assertNull("Failed validation must produce null envelope", validation.envelope)
    }
}

