package com.expenseapp.salarytracker.domain.service.export

import com.expenseapp.salarytracker.domain.model.report.FinancialReport
import java.io.OutputStream
import java.nio.charset.StandardCharsets

/**
 * RFC 4180-compliant CSV Export Engine with UTF-8 BOM encoding for Microsoft Excel compatibility.
 */
class CsvExportEngine {

    companion object {
        // UTF-8 BOM bytes: EF BB BF
        val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        const val CRLF = "\r\n"
    }

    /**
     * Exports full itemized transaction ledger into RFC 4180 CSV with UTF-8 BOM.
     */
    fun exportTransactionsCsv(report: FinancialReport, outputStream: OutputStream) {
        // 1. Write BOM
        outputStream.write(UTF8_BOM)

        val writer = outputStream.bufferedWriter(StandardCharsets.UTF_8)

        // 2. Write CSV Header
        val headers = listOf(
            "Transaction ID",
            "Date",
            "Time",
            "Type",
            "Amount",
            "Currency",
            "Account ID",
            "Account Name",
            "Destination Account ID",
            "Destination Account Name",
            "Category ID",
            "Category Name",
            "Description",
            "Notes",
            "Recurring Rule ID",
            "Custom Fields",
            "Created Timestamp"
        )
        writeRow(writer, headers)

        // 3. Write Data Rows
        for (tx in report.transactions) {
            val customFieldsStr = if (tx.customFields.isNotEmpty()) {
                tx.customFields.entries.joinToString("; ") { "${it.key}: ${it.value}" }
            } else ""

            val row = listOf(
                tx.id.toString(),
                tx.date.toString(),
                tx.time.toString(),
                tx.type.name,
                tx.amount.toBigDecimal().toPlainString(),
                tx.currency,
                tx.accountId.toString(),
                tx.accountName,
                tx.destinationAccountId?.toString() ?: "",
                tx.destinationAccountName ?: "",
                tx.categoryId?.toString() ?: "",
                tx.categoryName ?: "",
                tx.description,
                tx.notes,
                tx.recurringRuleId?.toString() ?: "",
                customFieldsStr,
                tx.createdAtEpochMs.toString()
            )
            writeRow(writer, row)
        }

        writer.flush()
    }

    /**
     * Exports comprehensive executive report summary into CSV.
     */
    fun exportSummaryCsv(report: FinancialReport, outputStream: OutputStream) {
        outputStream.write(UTF8_BOM)
        val writer = outputStream.bufferedWriter(StandardCharsets.UTF_8)

        // Section 1: Report Metadata
        writeRow(writer, listOf("EXECUTIVE FINANCIAL REPORT"))
        writeRow(writer, listOf("Period", report.periodLabel))
        writeRow(writer, listOf("Start Date", report.startDate.toString()))
        writeRow(writer, listOf("End Date", report.endDate.toString()))
        writeRow(writer, listOf("Currency", report.currencyCode))
        writeRow(writer, listOf())

        // Section 2: KPIs
        writeRow(writer, listOf("FINANCIAL METRIC", "AMOUNT (${report.currencyCode})"))
        writeRow(writer, listOf("Opening Balance", report.openingBalance.toBigDecimal().toPlainString()))
        writeRow(writer, listOf("Total Income", report.totalIncome.toBigDecimal().toPlainString()))
        writeRow(writer, listOf("Total Expenses", report.totalExpenses.toBigDecimal().toPlainString()))
        writeRow(writer, listOf("Total Refunds", report.totalRefunds.toBigDecimal().toPlainString()))
        writeRow(writer, listOf("Net Expenses", report.netExpenses.toBigDecimal().toPlainString()))
        writeRow(writer, listOf("Net Savings Allocated", report.netSavings.toBigDecimal().toPlainString()))
        writeRow(writer, listOf("Savings Rate (%)", String.format(java.util.Locale.US, "%.1f%%", report.savingsRatePercent)))
        writeRow(writer, listOf("Donations", report.donationsTotal.toBigDecimal().toPlainString()))
        writeRow(writer, listOf("Debt Payments", report.debtPaidTotal.toBigDecimal().toPlainString()))
        writeRow(writer, listOf("Debt Inflows", report.debtReceivedTotal.toBigDecimal().toPlainString()))
        writeRow(writer, listOf("Net Cash Flow", report.netCashFlow.toBigDecimal().toPlainString()))
        writeRow(writer, listOf("Closing Balance", report.closingBalance.toBigDecimal().toPlainString()))
        writeRow(writer, listOf("Transaction Count", report.transactionCount.toString()))
        writeRow(writer, listOf())

        // Section 3: Categories
        writeRow(writer, listOf("CATEGORY BREAKDOWN", "AMOUNT", "% OF TOTAL SPENDING", "TRANSACTION COUNT"))
        for (cat in report.categoryBreakdown) {
            writeRow(
                writer,
                listOf(
                    cat.categoryName,
                    cat.amount.toBigDecimal().toPlainString(),
                    String.format(java.util.Locale.US, "%.1f%%", cat.percentageOfTotal),
                    cat.transactionCount.toString()
                )
            )
        }
        writeRow(writer, listOf())

        // Section 4: Accounts
        writeRow(writer, listOf("ACCOUNT", "TYPE", "OPENING BALANCE", "INFLOWS", "OUTFLOWS", "CLOSING BALANCE"))
        for (acc in report.accountBreakdown) {
            writeRow(
                writer,
                listOf(
                    acc.accountName,
                    acc.accountType,
                    acc.openingBalance.toBigDecimal().toPlainString(),
                    acc.totalInflows.toBigDecimal().toPlainString(),
                    acc.totalOutflows.toBigDecimal().toPlainString(),
                    acc.closingBalance.toBigDecimal().toPlainString()
                )
            )
        }
        writeRow(writer, listOf())

        // Section 5: Budgets
        writeRow(writer, listOf("BUDGET", "PLANNED", "ACTUAL", "REMAINING", "VARIANCE", "% USED", "STATUS"))
        for (b in report.budgetBreakdown) {
            writeRow(
                writer,
                listOf(
                    b.name,
                    b.plannedAmount.toBigDecimal().toPlainString(),
                    b.actualAmount.toBigDecimal().toPlainString(),
                    b.remainingAmount.toBigDecimal().toPlainString(),
                    b.variance.toBigDecimal().toPlainString(),
                    String.format(java.util.Locale.US, "%.1f%%", b.percentageUsed),
                    b.statusText
                )
            )
        }
        writeRow(writer, listOf())

        // Section 6: Goals
        writeRow(writer, listOf("GOAL", "TYPE", "TARGET", "PROGRESS", "% REACHED", "STATUS"))
        for (g in report.goalBreakdown) {
            writeRow(
                writer,
                listOf(
                    g.name,
                    g.type.displayName,
                    g.targetAmount.toBigDecimal().toPlainString(),
                    g.currentProgress.toBigDecimal().toPlainString(),
                    String.format(java.util.Locale.US, "%.1f%%", g.percentageReached),
                    g.status.name
                )
            )
        }
        writeRow(writer, listOf())

        // Section 7: Tree of Hope
        writeRow(writer, listOf("TREE OF HOPE", "STAGE", "LEVEL", "EXPLANATION", "ACTION ADVICE"))
        writeRow(
            writer,
            listOf(
                "Tree Status",
                report.treeState.displayName,
                report.treeLevel.toString(),
                report.treeExplanation,
                report.treeActionAdvice
            )
        )

        writer.flush()
    }

    private fun writeRow(writer: java.io.BufferedWriter, fields: List<String>) {
        val line = fields.joinToString(",") { escapeCsv(it) }
        writer.write(line)
        writer.write(CRLF)
    }

    /**
     * Escapes a single CSV value per RFC 4180:
     * - If field contains comma, quote, or newline, surround with quotes and escape internal quotes with `""`.
     * - Formula injection sanitization: If value starts with `=`, `+`, `-`, or `@`, prepend `'` before quoting.
     */
    fun escapeCsv(value: String): String {
        var cleanValue = value
        // Formula injection guard
        if (cleanValue.isNotEmpty() && (cleanValue.startsWith("=") || cleanValue.startsWith("+") || cleanValue.startsWith("-") || cleanValue.startsWith("@"))) {
            // Negative numbers like -50.00 are valid financial figures and should not be prefixed with quote
            val isNegativeNumber = cleanValue.startsWith("-") && cleanValue.toDoubleOrNull() != null
            if (!isNegativeNumber) {
                cleanValue = "'$cleanValue"
                return "\"" + cleanValue.replace("\"", "\"\"") + "\""
            }
        }

        val needsQuotes = cleanValue.contains(',') || cleanValue.contains('"') || cleanValue.contains('\n') || cleanValue.contains('\r')
        return if (needsQuotes) {
            "\"" + cleanValue.replace("\"", "\"\"") + "\""
        } else {
            cleanValue
        }
    }
}
