package com.expenseapp.salarytracker.domain.service.export

import com.expenseapp.salarytracker.domain.model.GoalStatus
import com.expenseapp.salarytracker.domain.model.GoalType
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.model.TreeHistory
import com.expenseapp.salarytracker.domain.model.TreeState
import com.expenseapp.salarytracker.domain.model.report.AccountReportItem
import com.expenseapp.salarytracker.domain.model.report.BudgetReportItem
import com.expenseapp.salarytracker.domain.model.report.CategoryReportItem
import com.expenseapp.salarytracker.domain.model.report.DailyReportItem
import com.expenseapp.salarytracker.domain.model.report.FinancialReport
import com.expenseapp.salarytracker.domain.model.report.GoalReportItem
import com.expenseapp.salarytracker.domain.model.report.ReportTransactionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class XlsxExportEngineTest {

    private val xlsxEngine = XlsxExportEngine()

    private fun createComprehensiveReport(): FinancialReport {
        val tx1 = ReportTransactionItem(
            id = 1L,
            date = LocalDate.of(2026, 9, 5),
            time = LocalTime.of(10, 15),
            type = TransactionType.EXPENSE,
            amount = MinorUnits(150000L), // 1500.00
            currency = "BDT",
            accountId = 1L,
            accountName = "Primary Checking",
            destinationAccountId = null,
            destinationAccountName = null,
            categoryId = 1L,
            categoryName = "Groceries",
            description = "=SUM(A1:B10) Test Description", // Malicious formula attempt
            notes = "Weekly grocery run",
            recurringRuleId = null,
            customFields = mapOf("Receipt" to "Yes"),
            createdAtEpochMs = 1759240000000L,
            updatedAtEpochMs = 1759240000000L
        )

        return FinancialReport(
            periodLabel = "September 2026",
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30),
            generatedAtEpochMs = 1759240000000L,
            currencyCode = "BDT",
            currencySymbol = "৳",
            openingBalance = MinorUnits(5000000L),
            closingBalance = MinorUnits(6200000L),
            totalIncome = MinorUnits(3000000L),
            totalExpenses = MinorUnits(1500000L),
            totalRefunds = MinorUnits(50000L),
            netExpenses = MinorUnits(1450000L),
            netSavings = MinorUnits(500000L),
            savingsRatePercent = 16.7,
            transfersTotal = MinorUnits.ZERO,
            donationsTotal = MinorUnits(50000L),
            debtPaidTotal = MinorUnits.ZERO,
            debtReceivedTotal = MinorUnits.ZERO,
            netCashFlow = MinorUnits(1200000L),
            transactionCount = 1,
            categoryBreakdown = listOf(
                CategoryReportItem(1L, "Groceries", "#10B981", MinorUnits(1450000L), 100.0, 1)
            ),
            dailyBreakdown = listOf(
                DailyReportItem(LocalDate.of(2026, 9, 5), MinorUnits(1450000L), MinorUnits.ZERO, -MinorUnits(1450000L), 1)
            ),
            accountBreakdown = listOf(
                AccountReportItem(1L, "Primary Checking", "Bank Account", MinorUnits(5000000L), MinorUnits(3000000L), MinorUnits(1500000L), MinorUnits.ZERO, MinorUnits.ZERO, MinorUnits(6500000L))
            ),
            budgetBreakdown = listOf(
                BudgetReportItem(1L, "Groceries Budget", "Groceries", MinorUnits(2000000L), MinorUnits(1450000L), MinorUnits(550000L), MinorUnits(550000L), 72.5, "On Track", false, false)
            ),
            overallBudget = BudgetReportItem(0L, "Overall Ceiling", null, MinorUnits(2500000L), MinorUnits(1450000L), MinorUnits(1050000L), MinorUnits(1050000L), 58.0, "On Track", false, false),
            goalBreakdown = listOf(
                GoalReportItem(1L, "Emergency Fund", GoalType.SAVE_AMOUNT, MinorUnits(10000000L), MinorUnits(5000000L), 50.0, GoalStatus.ACTIVE, listOf(25, 50), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
            ),
            treeState = TreeState.GROWING,
            treeLevel = 4,
            treeExplanation = "Savings goal reached 50% milestone",
            treeActionAdvice = "Maintain consistent spending limit",
            treeHistory = listOf(
                TreeHistory(1L, TreeState.YOUNG_PLANT, TreeState.GROWING, "Emergency Fund 50%", 1L, 1759240000000L)
            ),
            transactions = listOf(tx1)
        )
    }

    @Test
    fun `exportWorkbook generates valid zip package containing all required OpenXML parts and 8 sheets`() {
        val out = ByteArrayOutputStream()
        val report = createComprehensiveReport()

        xlsxEngine.exportWorkbook(report, out)
        val zipBytes = out.toByteArray()
        assertTrue("Output zip must not be empty", zipBytes.isNotEmpty())

        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes()
                entry = zis.nextEntry
            }
        }

        // Verify Core OpenXML Parts
        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("_rels/.rels"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/_rels/workbook.xml.rels"))
        assertTrue(entries.containsKey("xl/styles.xml"))

        // Verify All 8 Required Worksheets
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml")) // Summary
        assertTrue(entries.containsKey("xl/worksheets/sheet2.xml")) // Transactions
        assertTrue(entries.containsKey("xl/worksheets/sheet3.xml")) // Categories
        assertTrue(entries.containsKey("xl/worksheets/sheet4.xml")) // Accounts
        assertTrue(entries.containsKey("xl/worksheets/sheet5.xml")) // Budgets
        assertTrue(entries.containsKey("xl/worksheets/sheet6.xml")) // Goals
        assertTrue(entries.containsKey("xl/worksheets/sheet7.xml")) // Tree History
        assertTrue(entries.containsKey("xl/worksheets/sheet8.xml")) // Daily Summary
    }

    @Test
    fun `exportWorkbook generates well-formed XML that passes standard XML parser`() {
        val out = ByteArrayOutputStream()
        val report = createComprehensiveReport()
        xlsxEngine.exportWorkbook(report, out)

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()

        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val bytes = zis.readBytes()
                if (entry.name.endsWith(".xml") || entry.name.endsWith(".rels")) {
                    val doc = builder.parse(ByteArrayInputStream(bytes))
                    assertNotNull("XML Document for ${entry.name} must parse without error", doc)
                }
                entry = zis.nextEntry
            }
        }
    }

    @Test
    fun `exportWorkbook sanitizes formula injection in cells`() {
        val out = ByteArrayOutputStream()
        val report = createComprehensiveReport()
        xlsxEngine.exportWorkbook(report, out)

        var txSheetXml = ""
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "xl/worksheets/sheet2.xml") {
                    txSheetXml = String(zis.readBytes(), StandardCharsets.UTF_8)
                    break
                }
                entry = zis.nextEntry
            }
        }

        // Malicious description starting with = should be quoted with '
        assertTrue("Formula starting with = must be sanitized with ' prefix", txSheetXml.contains("'=SUM(A1:B10)"))
    }

    @Test
    fun `exportWorkbook contains frozen pane definitions for scrolling readability`() {
        val out = ByteArrayOutputStream()
        val report = createComprehensiveReport()
        xlsxEngine.exportWorkbook(report, out)

        var sheet1Xml = ""
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "xl/worksheets/sheet1.xml") {
                    sheet1Xml = String(zis.readBytes(), StandardCharsets.UTF_8)
                    break
                }
                entry = zis.nextEntry
            }
        }

        assertTrue("Worksheet must have frozen top pane", sheet1Xml.contains("state=\"frozen\""))
        assertTrue("Worksheet must have ySplit=1", sheet1Xml.contains("ySplit=\"1\""))
    }
}
