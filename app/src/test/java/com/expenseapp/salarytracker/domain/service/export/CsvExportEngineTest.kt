package com.expenseapp.salarytracker.domain.service.export

import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.model.TreeState
import com.expenseapp.salarytracker.domain.model.report.FinancialReport
import com.expenseapp.salarytracker.domain.model.report.ReportTransactionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime

class CsvExportEngineTest {

    private val csvEngine = CsvExportEngine()

    private fun createDummyReport(transactions: List<ReportTransactionItem>): FinancialReport {
        return FinancialReport(
            periodLabel = "September 2026",
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30),
            generatedAtEpochMs = 1759240000000L,
            currencyCode = "BDT",
            currencySymbol = "৳",
            openingBalance = MinorUnits(100000L),
            closingBalance = MinorUnits(150000L),
            totalIncome = MinorUnits(80000L),
            totalExpenses = MinorUnits(30000L),
            totalRefunds = MinorUnits(0L),
            netExpenses = MinorUnits(30000L),
            netSavings = MinorUnits(10000L),
            savingsRatePercent = 12.5,
            transfersTotal = MinorUnits.ZERO,
            donationsTotal = MinorUnits.ZERO,
            debtPaidTotal = MinorUnits.ZERO,
            debtReceivedTotal = MinorUnits.ZERO,
            netCashFlow = MinorUnits(50000L),
            transactionCount = transactions.size,
            categoryBreakdown = emptyList(),
            dailyBreakdown = emptyList(),
            accountBreakdown = emptyList(),
            budgetBreakdown = emptyList(),
            overallBudget = null,
            goalBreakdown = emptyList(),
            treeState = TreeState.YOUNG_PLANT,
            treeLevel = 3,
            treeExplanation = "On track with savings",
            treeActionAdvice = "Maintain habit",
            treeHistory = emptyList(),
            transactions = transactions
        )
    }

    @Test
    fun `exportTransactionsCsv writes UTF-8 BOM at the start`() {
        val out = ByteArrayOutputStream()
        val report = createDummyReport(emptyList())

        csvEngine.exportTransactionsCsv(report, out)
        val bytes = out.toByteArray()

        assertTrue(bytes.size >= 3)
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])
    }

    @Test
    fun `escapeCsv handles commas, quotes, and newlines per RFC 4180`() {
        assertEquals("SimpleText", csvEngine.escapeCsv("SimpleText"))
        assertEquals("\"Text, with comma\"", csvEngine.escapeCsv("Text, with comma"))
        assertEquals("\"He said \"\"Hello\"\" to me\"", csvEngine.escapeCsv("He said \"Hello\" to me"))
        assertEquals("\"Line1\nLine2\"", csvEngine.escapeCsv("Line1\nLine2"))
    }

    @Test
    fun `escapeCsv prevents formula injection`() {
        // Formaula injection starting with =
        assertEquals("\"'=cmd|' /C calc'!A0\"", csvEngine.escapeCsv("=cmd|' /C calc'!A0"))
        assertEquals("\"'+12345\"", csvEngine.escapeCsv("+12345"))
        assertEquals("\"'@SUM(A1:A10)\"", csvEngine.escapeCsv("@SUM(A1:A10)"))

        // Pure negative numbers should NOT be quoted with single quote
        assertEquals("-50.00", csvEngine.escapeCsv("-50.00"))
    }

    @Test
    fun `exportTransactionsCsv preserves Bengali and Unicode characters correctly`() {
        val tx = ReportTransactionItem(
            id = 101L,
            date = LocalDate.of(2026, 9, 15),
            time = LocalTime.of(14, 30),
            type = TransactionType.EXPENSE,
            amount = MinorUnits(12050L), // 120.50
            currency = "BDT",
            accountId = 1L,
            accountName = "bKash পার্সোনাল",
            destinationAccountId = null,
            destinationAccountName = null,
            categoryId = 2L,
            categoryName = "খাবার ও নাস্তা",
            description = "বিকেলের নাস্তা, মিষ্টি ও চা",
            notes = "দোকানদার: \"মিষ্টি মেলা\", ঢাকা",
            recurringRuleId = null,
            customFields = mapOf("Location" to "ধানমন্ডি"),
            createdAtEpochMs = 1759240000000L,
            updatedAtEpochMs = 1759240000000L
        )

        val out = ByteArrayOutputStream()
        val report = createDummyReport(listOf(tx))
        csvEngine.exportTransactionsCsv(report, out)

        val csvString = String(out.toByteArray(), StandardCharsets.UTF_8)

        assertTrue(csvString.contains("bKash পার্সোনাল"))
        assertTrue(csvString.contains("খাবার ও নাস্তা"))
        assertTrue(csvString.contains("\"বিকেলের নাস্তা, মিষ্টি ও চা\""))
        assertTrue(csvString.contains("\"দোকানদার: \"\"মিষ্টি মেলা\"\", ঢাকা\""))
        assertTrue(csvString.contains("120.50"))
    }

    @Test
    fun `exportSummaryCsv generates all executive sections cleanly`() {
        val out = ByteArrayOutputStream()
        val report = createDummyReport(emptyList())

        csvEngine.exportSummaryCsv(report, out)
        val csvString = String(out.toByteArray(), StandardCharsets.UTF_8)

        assertTrue(csvString.contains("EXECUTIVE FINANCIAL REPORT"))
        assertTrue(csvString.contains("September 2026"))
        assertTrue(csvString.contains("Opening Balance"))
        assertTrue(csvString.contains("Net Cash Flow"))
        assertTrue(csvString.contains("TREE OF HOPE"))
        assertTrue(csvString.contains("Young Plant"))
    }
}
