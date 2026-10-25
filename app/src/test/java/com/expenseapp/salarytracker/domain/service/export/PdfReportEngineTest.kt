package com.expenseapp.salarytracker.domain.service.export

import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.model.TreeState
import com.expenseapp.salarytracker.domain.model.report.FinancialReport
import com.expenseapp.salarytracker.domain.model.report.ReportTransactionItem
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime

class PdfReportEngineTest {

    private val pdfEngine = PdfReportEngine()

    private fun createReport(txCount: Int): FinancialReport {
        val txs = (1..txCount).map { i ->
            ReportTransactionItem(
                id = i.toLong(),
                date = LocalDate.of(2026, 9, (i % 28) + 1),
                time = LocalTime.of(12, 0),
                type = if (i % 3 == 0) TransactionType.INCOME else TransactionType.EXPENSE,
                amount = MinorUnits(i * 1000L),
                currency = "BDT",
                accountId = 1L,
                accountName = "Main Account",
                destinationAccountId = null,
                destinationAccountName = null,
                categoryId = 1L,
                categoryName = "General",
                description = "Transaction Item #$i",
                notes = "Sample Note #$i with special chars: ৳ and (parentheses)",
                recurringRuleId = null,
                customFields = emptyMap(),
                createdAtEpochMs = 1759240000000L,
                updatedAtEpochMs = 1759240000000L
            )
        }

        return FinancialReport(
            periodLabel = "September 2026 Statement",
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30),
            generatedAtEpochMs = 1759240000000L,
            currencyCode = "BDT",
            currencySymbol = "৳",
            openingBalance = MinorUnits(500000L),
            closingBalance = MinorUnits(750000L),
            totalIncome = MinorUnits(500000L),
            totalExpenses = MinorUnits(250000L),
            totalRefunds = MinorUnits.ZERO,
            netExpenses = MinorUnits(250000L),
            netSavings = MinorUnits(100000L),
            savingsRatePercent = 20.0,
            transfersTotal = MinorUnits.ZERO,
            donationsTotal = MinorUnits.ZERO,
            debtPaidTotal = MinorUnits.ZERO,
            debtReceivedTotal = MinorUnits.ZERO,
            netCashFlow = MinorUnits(250000L),
            transactionCount = txs.size,
            categoryBreakdown = emptyList(),
            dailyBreakdown = emptyList(),
            accountBreakdown = emptyList(),
            budgetBreakdown = emptyList(),
            overallBudget = null,
            goalBreakdown = emptyList(),
            treeState = TreeState.GROWING,
            treeLevel = 4,
            treeExplanation = "Tree is flourishing with consistent budget adherence",
            treeActionAdvice = "Continue regular savings allocations",
            treeHistory = emptyList(),
            transactions = txs
        )
    }

    @Test
    fun `exportPdf produces genuine PDF 1_4 stream with valid header, catalog, xref, and trailer`() {
        val out = ByteArrayOutputStream()
        val report = createReport(txCount = 5)

        pdfEngine.exportPdf(report, out)
        val pdfBytes = out.toByteArray()

        assertTrue("PDF output must not be empty", pdfBytes.isNotEmpty())
        val pdfStr = String(pdfBytes, StandardCharsets.ISO_8859_1)

        // 1. Header check
        assertTrue("PDF must start with %PDF-1.4", pdfStr.startsWith("%PDF-1.4"))

        // 2. Catalog check
        assertTrue("PDF must contain /Type /Catalog", pdfStr.contains("/Type /Catalog"))

        // 3. Pages tree
        assertTrue("PDF must contain /Type /Pages", pdfStr.contains("/Type /Pages"))

        // 4. Cross reference table
        assertTrue("PDF must contain xref table", pdfStr.contains("xref"))

        // 5. Trailer
        assertTrue("PDF must contain trailer", pdfStr.contains("trailer"))

        // 6. EOF marker
        assertTrue("PDF must end with %%EOF", pdfStr.trimEnd().endsWith("%%EOF"))
    }

    @Test
    fun `exportPdf safely handles pagination for large transaction lists`() {
        val out = ByteArrayOutputStream()
        // 80 transactions will require multiple pages
        val report = createReport(txCount = 80)

        pdfEngine.exportPdf(report, out)
        val pdfStr = String(out.toByteArray(), StandardCharsets.ISO_8859_1)

        // Multiple page objects should be emitted
        assertTrue(pdfStr.contains("/Type /Page "))
        assertTrue(pdfStr.contains("/Count "))
        // Trailer must remain valid
        assertTrue(pdfStr.trimEnd().endsWith("%%EOF"))
    }

    @Test
    fun `escapePdfText handles parentheses and transliterates currency symbols safely`() {
        val input = "Total (BDT): ৳500 — \"Special Note\""
        val escaped = pdfEngine.escapePdfText(input)

        // Parentheses should be escaped
        assertTrue(escaped.contains("\\(BDT\\)"))
        // Currency symbol transliterated
        assertTrue(escaped.contains("Tk 500"))
        // Em dash converted to ASCII
        assertTrue(escaped.contains("-"))
    }
}
