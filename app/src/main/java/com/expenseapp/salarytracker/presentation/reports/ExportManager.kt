package com.expenseapp.salarytracker.presentation.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.expenseapp.salarytracker.domain.model.report.BackupEnvelope
import com.expenseapp.salarytracker.domain.model.report.FinancialReport
import com.expenseapp.salarytracker.domain.service.export.CsvExportEngine
import com.expenseapp.salarytracker.domain.service.export.JsonBackupEngine
import com.expenseapp.salarytracker.domain.service.export.PdfReportEngine
import com.expenseapp.salarytracker.domain.service.export.XlsxExportEngine
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ExportFormat(val displayName: String, val extension: String, val mimeType: String) {
    XLSX("Excel Workbook (.xlsx)", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV_TRANSACTIONS("Transactions (.csv)", "csv", "text/csv"),
    CSV_SUMMARY("Summary (.csv)", "csv", "text/csv"),
    PDF("Printable Report (.pdf)", "pdf", "application/pdf"),
    JSON_BACKUP("Full Backup (.json)", "json", "application/json"),
    JSON_ENCRYPTED_BACKUP("Encrypted Backup (.enc.json)", "enc.json", "application/octet-stream")
}

class ExportManager(
    private val xlsxEngine: XlsxExportEngine = XlsxExportEngine(),
    private val csvEngine: CsvExportEngine = CsvExportEngine(),
    private val pdfEngine: PdfReportEngine = PdfReportEngine(),
    private val jsonEngine: JsonBackupEngine = JsonBackupEngine(),
    private val encryptedBackupEngine: com.expenseapp.salarytracker.data.security.EncryptedBackupEngine? = null
) {

    fun getDefaultFilename(format: ExportFormat, report: FinancialReport? = null): String {
        val dateSuffix = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return when (format) {
            ExportFormat.XLSX -> "ExpenseApp_Report_$dateSuffix.xlsx"
            ExportFormat.CSV_TRANSACTIONS -> "ExpenseApp_Transactions_$dateSuffix.csv"
            ExportFormat.CSV_SUMMARY -> "ExpenseApp_Summary_$dateSuffix.csv"
            ExportFormat.PDF -> "ExpenseApp_Statement_$dateSuffix.pdf"
            ExportFormat.JSON_BACKUP -> "ExpenseApp_Backup_$dateSuffix.json"
            ExportFormat.JSON_ENCRYPTED_BACKUP -> "ExpenseApp_EncryptedBackup_$dateSuffix.enc.json"
        }
    }

    /**
     * Writes formatted output directly to the given [outputStream] (e.g. from SAF URI).
     */
    fun exportToStream(
        format: ExportFormat,
        report: FinancialReport,
        backupEnvelope: BackupEnvelope?,
        outputStream: OutputStream,
        passphrase: CharArray? = null
    ) {
        when (format) {
            ExportFormat.XLSX -> xlsxEngine.exportWorkbook(report, outputStream)
            ExportFormat.CSV_TRANSACTIONS -> csvEngine.exportTransactionsCsv(report, outputStream)
            ExportFormat.CSV_SUMMARY -> csvEngine.exportSummaryCsv(report, outputStream)
            ExportFormat.PDF -> pdfEngine.exportPdf(report, outputStream)
            ExportFormat.JSON_BACKUP -> {
                val envelope = backupEnvelope ?: error("BackupEnvelope required for JSON backup")
                jsonEngine.exportBackup(envelope, outputStream)
            }
            ExportFormat.JSON_ENCRYPTED_BACKUP -> {
                val envelope = backupEnvelope ?: error("BackupEnvelope required for JSON backup")
                val pwd = passphrase ?: error("Passphrase required for encrypted backup")
                val encEngine = encryptedBackupEngine ?: error("EncryptedBackupEngine not initialized")
                encEngine.exportEncryptedBackup(envelope, pwd, outputStream)
            }
        }
    }

    /**
     * Creates a cached temporary file and builds an Android Share Intent.
     */
    fun createShareIntent(
        context: Context,
        format: ExportFormat,
        report: FinancialReport,
        backupEnvelope: BackupEnvelope?
    ): Intent {
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val filename = getDefaultFilename(format, report)
        val file = File(cacheDir, filename)

        FileOutputStream(file).use { os ->
            exportToStream(format, report, backupEnvelope, os)
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Financial Report: ${report.periodLabel}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
