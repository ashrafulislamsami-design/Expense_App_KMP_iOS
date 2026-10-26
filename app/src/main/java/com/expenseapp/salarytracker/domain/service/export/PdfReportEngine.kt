package com.expenseapp.salarytracker.domain.service.export

import com.expenseapp.salarytracker.domain.model.report.FinancialReport
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

/**
 * Pure Kotlin, zero-dependency PDF 1.4 Statement Generator.
 *
 * Generates genuine, printable multi-page PDF documents.
 * Fully compatible with both standard JVM unit tests (no Android framework stubs required)
 * and Android runtime execution.
 */
class PdfReportEngine {

    companion object {
        const val PAGE_WIDTH = 595.0f  // A4 width in points
        const val PAGE_HEIGHT = 842.0f // A4 height in points
        const val MARGIN_LEFT = 40.0f
        const val MARGIN_RIGHT = 555.0f
        const val MARGIN_TOP = 800.0f
        const val MARGIN_BOTTOM = 50.0f
    }

    /**
     * Generates a complete executive PDF financial report to [outputStream].
     */
    fun exportPdf(report: FinancialReport, outputStream: OutputStream) {
        val pages = mutableListOf<String>()
        var currentStream = StringBuilder()
        var currentY = MARGIN_TOP
        var pageNumber = 1

        fun checkPageBreak(requiredHeight: Float) {
            if (currentY - requiredHeight < MARGIN_BOTTOM) {
                // Finish current page
                pages.add(currentStream.toString())
                currentStream = StringBuilder()
                pageNumber++
                currentY = MARGIN_TOP

                // Header for subsequent pages
                drawHeader(currentStream, report, pageNumber, isSubsequent = true)
                currentY -= 40f
            }
        }

        // --- PAGE 1: Start ---
        drawHeader(currentStream, report, pageNumber, isSubsequent = false)
        currentY -= 90f

        // 1. Executive Summary KPIs Box
        checkPageBreak(130f)
        drawSectionTitle(currentStream, "1. Executive Financial Summary", currentY)
        currentY -= 20f

        val kpiLines = listOf(
            Pair("Statement Period", report.periodLabel),
            Pair("Opening Balance", "${report.openingBalance.format(report.currencyCode)}"),
            Pair("Total Earned Income", "${report.totalIncome.format(report.currencyCode)}"),
            Pair("Gross Expenses", "${report.totalExpenses.format(report.currencyCode)}"),
            Pair("Refunds Offset", "${report.totalRefunds.format(report.currencyCode)}"),
            Pair("Net Expenses", "${report.netExpenses.format(report.currencyCode)}"),
            Pair("Savings Allocated", "${report.netSavings.format(report.currencyCode)} (${String.format(java.util.Locale.US, "%.1f%%", report.savingsRatePercent)})"),
            Pair("Donations", "${report.donationsTotal.format(report.currencyCode)}"),
            Pair("Net Cash Flow", "${report.netCashFlow.format(report.currencyCode)}"),
            Pair("Closing Balance", "${report.closingBalance.format(report.currencyCode)}")
        )

        // Draw 2-column KPI cards
        drawKpiTable(currentStream, kpiLines, currentY)
        currentY -= (kpiLines.size * 14f + 15f)

        // 2. Tree of Hope Card
        checkPageBreak(70f)
        drawSectionTitle(currentStream, "2. Financial Health & Savings Momentum", currentY)
        currentY -= 18f
        drawTreeCard(currentStream, report, currentY)
        currentY -= 55f

        // 3. Budgets Overview
        if (report.budgetBreakdown.isNotEmpty() || report.overallBudget != null) {
            checkPageBreak(80f)
            drawSectionTitle(currentStream, "3. Budget Performance", currentY)
            currentY -= 18f

            drawTableHeader(currentStream, listOf("Target", "Limit", "Actual", "Variance", "Utilized", "Status"), currentY)
            currentY -= 16f

            report.overallBudget?.let { ob ->
                drawBudgetRow(currentStream, "Overall Budget", ob.plannedAmount.format(report.currencyCode), ob.actualAmount.format(report.currencyCode), ob.variance.format(report.currencyCode), String.format(java.util.Locale.US, "%.0f%%", ob.percentageUsed), ob.statusText, currentY)
                currentY -= 14f
            }

            for (b in report.budgetBreakdown) {
                checkPageBreak(16f)
                drawBudgetRow(currentStream, b.name, b.plannedAmount.format(report.currencyCode), b.actualAmount.format(report.currencyCode), b.variance.format(report.currencyCode), String.format(java.util.Locale.US, "%.0f%%", b.percentageUsed), b.statusText, currentY)
                currentY -= 14f
            }
            currentY -= 15f
        }

        // 4. Category Breakdown
        if (report.categoryBreakdown.isNotEmpty()) {
            checkPageBreak(80f)
            drawSectionTitle(currentStream, "4. Top Category Expenses", currentY)
            currentY -= 18f

            drawTableHeader(currentStream, listOf("Category", "Amount", "% Total", "Transactions"), currentY, colWidths = listOf(180f, 110f, 100f, 100f))
            currentY -= 16f

            for (cat in report.categoryBreakdown.take(10)) {
                checkPageBreak(16f)
                drawCategoryRow(currentStream, cat.categoryName, cat.amount.format(report.currencyCode), String.format(java.util.Locale.US, "%.1f%%", cat.percentageOfTotal), cat.transactionCount.toString(), currentY)
                currentY -= 14f
            }
            currentY -= 15f
        }

        // 5. Itemized Transactions Ledger
        if (report.transactions.isNotEmpty()) {
            checkPageBreak(80f)
            drawSectionTitle(currentStream, "5. Transactions Ledger", currentY)
            currentY -= 18f

            drawTableHeader(currentStream, listOf("Date", "Account", "Category", "Type", "Amount", "Note / Payee"), currentY, colWidths = listOf(65f, 95f, 95f, 75f, 85f, 100f))
            currentY -= 16f

            for (tx in report.transactions) {
                checkPageBreak(16f)
                val sign = if (tx.type == com.expenseapp.salarytracker.domain.model.TransactionType.EXPENSE) "-" else "+"
                val amtStr = "$sign${tx.amount.format(report.currencyCode)}"
                drawTxRow(
                    currentStream,
                    tx.date.toString(),
                    tx.accountName,
                    tx.categoryName ?: "-",
                    tx.type.name,
                    amtStr,
                    tx.description.take(24),
                    currentY
                )
                currentY -= 14f
            }
        }

        // Add last page
        pages.add(currentStream.toString())

        // Build and write valid PDF 1.4 document
        writePdfDocument(pages, outputStream)
    }

    // ==========================================
    // PDF Low-Level Graphics & Drawing Routines
    // ==========================================

    private fun drawHeader(sb: StringBuilder, report: FinancialReport, pageNum: Int, isSubsequent: Boolean) {
        if (!isSubsequent) {
            // Document Title
            sb.append("BT /F2 18 Tf 40 805 Td (FINANCIAL STATEMENT & EXECUTIVE REPORT) Tj ET\n")
            sb.append("BT /F1 10 Tf 40 790 Td (Generated by Expense & Salary Tracker  |  Confidential) Tj ET\n")
            sb.append("BT /F2 11 Tf 40 770 Td (Period: ${escapePdfText(report.periodLabel)}) Tj ET\n")
            sb.append("BT /F1 9 Tf 40 755 Td (Dates: ${report.startDate} to ${report.endDate}   |   Currency: ${report.currencyCode}) Tj ET\n")
            // Divider line
            sb.append("q 0.8 0.85 0.9 RG 1.5 w 40 745 m 555 745 l S Q\n")
        } else {
            // Subsequent page minimal header
            sb.append("BT /F2 10 Tf 40 815 Td (Financial Statement  |  ${escapePdfText(report.periodLabel)}) Tj ET\n")
            sb.append("BT /F1 9 Tf 500 815 Td (Page $pageNum) Tj ET\n")
            sb.append("q 0.85 0.85 0.85 RG 0.75 w 40 808 m 555 808 l S Q\n")
        }
    }

    private fun drawSectionTitle(sb: StringBuilder, title: String, y: Float) {
        sb.append("q 0.94 0.96 0.98 rg 40 ${y - 4} 515 18 re f Q\n")
        sb.append("BT /F2 11 Tf 45 $y Td (${escapePdfText(title)}) Tj ET\n")
    }

    private fun drawKpiTable(sb: StringBuilder, lines: List<Pair<String, String>>, startY: Float) {
        var y = startY
        for ((label, value) in lines) {
            sb.append("BT /F1 9.5 Tf 45 $y Td (${escapePdfText(label)}) Tj ET\n")
            sb.append("BT /F2 9.5 Tf 350 $y Td (${escapePdfText(value)}) Tj ET\n")
            sb.append("q 0.9 0.9 0.9 RG 0.5 w 45 ${y - 3} m 550 ${y - 3} l S Q\n")
            y -= 14f
        }
    }

    private fun drawTreeCard(sb: StringBuilder, report: FinancialReport, y: Float) {
        sb.append("q 0.95 0.98 0.95 rg 40 ${y - 45} 515 48 re f Q\n")
        sb.append("q 0.2 0.6 0.3 RG 1 w 40 ${y - 45} 515 48 re s Q\n")
        sb.append("BT /F2 10 Tf 50 ${y - 12} Td (Current Stage: ${escapePdfText(report.treeState.displayName)} [Level ${report.treeLevel}]) Tj ET\n")
        sb.append("BT /F1 8.5 Tf 50 ${y - 25} Td (Assessment: ${escapePdfText(report.treeExplanation.take(85))}) Tj ET\n")
        sb.append("BT /F1 8.5 Tf 50 ${y - 37} Td (Action: ${escapePdfText(report.treeActionAdvice.take(85))}) Tj ET\n")
    }

    private fun drawTableHeader(sb: StringBuilder, headers: List<String>, y: Float, colWidths: List<Float>? = null) {
        sb.append("q 0.9 0.93 0.96 rg 40 ${y - 4} 515 15 re f Q\n")
        val widths = colWidths ?: List(headers.size) { 515f / headers.size }
        var currentX = 45f
        for (i in headers.indices) {
            sb.append("BT /F2 8.5 Tf $currentX $y Td (${escapePdfText(headers[i])}) Tj ET\n")
            currentX += widths[i]
        }
    }

    private fun drawBudgetRow(sb: StringBuilder, target: String, limit: String, actual: String, variance: String, used: String, status: String, y: Float) {
        sb.append("BT /F1 8 Tf 45 $y Td (${escapePdfText(target.take(18))}) Tj ET\n")
        sb.append("BT /F1 8 Tf 130 $y Td (${escapePdfText(limit)}) Tj ET\n")
        sb.append("BT /F1 8 Tf 210 $y Td (${escapePdfText(actual)}) Tj ET\n")
        sb.append("BT /F1 8 Tf 290 $y Td (${escapePdfText(variance)}) Tj ET\n")
        sb.append("BT /F1 8 Tf 370 $y Td (${escapePdfText(used)}) Tj ET\n")
        sb.append("BT /F2 8 Tf 440 $y Td (${escapePdfText(status)}) Tj ET\n")
        sb.append("q 0.92 0.92 0.92 RG 0.5 w 40 ${y - 3} m 555 ${y - 3} l S Q\n")
    }

    private fun drawCategoryRow(sb: StringBuilder, name: String, amount: String, pct: String, count: String, y: Float) {
        sb.append("BT /F1 8.5 Tf 45 $y Td (${escapePdfText(name.take(25))}) Tj ET\n")
        sb.append("BT /F1 8.5 Tf 225 $y Td (${escapePdfText(amount)}) Tj ET\n")
        sb.append("BT /F1 8.5 Tf 335 $y Td (${escapePdfText(pct)}) Tj ET\n")
        sb.append("BT /F1 8.5 Tf 435 $y Td (${escapePdfText(count)}) Tj ET\n")
        sb.append("q 0.92 0.92 0.92 RG 0.5 w 40 ${y - 3} m 555 ${y - 3} l S Q\n")
    }

    private fun drawTxRow(sb: StringBuilder, date: String, acc: String, cat: String, type: String, amount: String, note: String, y: Float) {
        sb.append("BT /F1 7.5 Tf 45 $y Td (${escapePdfText(date)}) Tj ET\n")
        sb.append("BT /F1 7.5 Tf 110 $y Td (${escapePdfText(acc.take(16))}) Tj ET\n")
        sb.append("BT /F1 7.5 Tf 205 $y Td (${escapePdfText(cat.take(16))}) Tj ET\n")
        sb.append("BT /F1 7.5 Tf 300 $y Td (${escapePdfText(type)}) Tj ET\n")
        sb.append("BT /F2 7.5 Tf 375 $y Td (${escapePdfText(amount)}) Tj ET\n")
        sb.append("BT /F1 7.5 Tf 460 $y Td (${escapePdfText(note.take(18))}) Tj ET\n")
        sb.append("q 0.92 0.92 0.92 RG 0.5 w 40 ${y - 2} m 555 ${y - 2} l S Q\n")
    }

    /**
     * Sanitizes and escapes text for PDF literal strings `(...)`.
     * Non-ASCII Unicode characters (such as Bengali or currency glyphs) are converted to clean ASCII
     * transliterations to prevent Type 1 font rendering corruptions.
     */
    fun escapePdfText(input: String): String {
        val ascii = input.map { ch ->
            when (ch) {
                '\u09F3' -> "Tk "
                '(' -> "\\("
                ')' -> "\\)"
                '\\' -> "\\\\"
                in ' '..'~' -> ch.toString()
                else -> {
                    // Normalize or transliterate non-ASCII Unicode to printable ASCII
                    when (ch) {
                        '\u2013', '\u2014' -> "-"
                        '\u201C', '\u201D' -> "\""
                        '\u2018', '\u2019' -> "'"
                        '\u00B7' -> " - "
                        '\u2192' -> " -> "
                        else -> {
                            val code = ch.code
                            if (code < 128) ch.toString() else "?"
                        }
                    }
                }
            }
        }.joinToString("")
        return ascii
    }

    // ==========================================
    // Low-Level PDF 1.4 File Container Writer
    // ==========================================

    private fun writePdfDocument(pageContents: List<String>, outputStream: OutputStream) {
        val out = ByteArrayOutputStream()
        val offsets = mutableListOf<Long>()

        fun writeBytes(bytes: ByteArray): Long {
            val offset = out.size().toLong()
            out.write(bytes)
            return offset
        }

        fun writeString(str: String): Long {
            return writeBytes(str.toByteArray(StandardCharsets.US_ASCII))
        }

        // 1. PDF Header
        writeString("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")

        val totalPages = pageContents.size
        // Object IDs:
        // 1: Catalog
        // 2: Pages
        // 3: Font Helvetica
        // 4: Font Helvetica-Bold
        // 5 + 2*i: Page i
        // 6 + 2*i: Content stream i

        val fontF1Obj = 3
        val fontF2Obj = 4

        // Reserve slots for offsets (1-indexed object ids)
        val numObjects = 4 + totalPages * 2
        for (i in 0..numObjects) {
            offsets.add(0L)
        }

        // Obj 1: Catalog
        offsets[1] = writeString("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        // Obj 2: Pages tree
        val kids = (1..totalPages).joinToString(" ") { "${4 + (it - 1) * 2 + 1} 0 R" }
        offsets[2] = writeString("2 0 obj\n<< /Type /Pages /Kids [$kids] /Count $totalPages >>\nendobj\n")

        // Obj 3: Font F1 (Helvetica)
        offsets[3] = writeString("3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n")

        // Obj 4: Font F2 (Helvetica-Bold)
        offsets[4] = writeString("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n")

        // Write Pages and Content Streams
        for (i in 0 until totalPages) {
            val pageObjId = 5 + i * 2
            val streamObjId = pageObjId + 1

            val contentBytes = pageContents[i].toByteArray(StandardCharsets.ISO_8859_1)

            // Page Object
            offsets[pageObjId] = writeString(
                "$pageObjId 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $PAGE_WIDTH $PAGE_HEIGHT] /Contents $streamObjId 0 R /Resources << /Font << /F1 $fontF1Obj 0 R /F2 $fontF2Obj 0 R >> >> >>\nendobj\n"
            )

            // Content Stream Object
            offsets[streamObjId] = writeString(
                "$streamObjId 0 obj\n<< /Length ${contentBytes.size} >>\nstream\n"
            )
            writeBytes(contentBytes)
            writeString("\nendstream\nendobj\n")
        }

        // Cross-reference table
        val startXref = out.size().toLong()
        writeString("xref\n0 ${numObjects + 1}\n")
        writeString("0000000000 65535 f \n")
        for (i in 1..numObjects) {
            val off = offsets[i]
            writeString(String.format(java.util.Locale.US, "%010d 00000 n \n", off))
        }

        // Trailer
        writeString("trailer\n<< /Size ${numObjects + 1} /Root 1 0 R >>\n")
        writeString("startxref\n$startXref\n%%EOF\n")

        outputStream.write(out.toByteArray())
        outputStream.flush()
    }
}
