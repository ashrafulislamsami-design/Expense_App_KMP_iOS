package com.expenseapp.salarytracker.diagnostics

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider

/**
 * Minimal, native recovery screen displayed when a startup crash is detected on previous launch.
 *
 * Resilience & Isolation:
 * - Inherits from pure [android.app.Activity] (no AndroidX Fragment, no Compose, no Room, no ViewModels).
 * - Built with programmatic Android Views (no XML inflation dependency, immune to theme resolution crashes).
 * - Never touches financial data, Room database, or cryptographic keystores.
 * - 100% offline, privacy-safe.
 */
class CrashRecoveryActivity : Activity() {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, CrashRecoveryActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reportText = StartupDiagnostics.getCrashReportText(this) ?: "No crash details available."

        // Parse key diagnostic fields defensively
        val timestamp = extractField(reportText, "Timestamp:")
        val lastCheckpoint = extractField(reportText, "Last checkpoint:")
        val exceptionType = extractField(reportText, "Exception:")
        val exceptionMessage = extractField(reportText, "Message:")
        val androidVersion = extractField(reportText, "Android:")
        val appVersion = extractField(reportText, "App version:")

        val rootScrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#111827")) // Dark Slate 900
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val pad = dpToPx(20)
            setPadding(pad, dpToPx(36), pad, dpToPx(36))
        }

        // Header Warning Badge
        val badge = TextView(this).apply {
            text = "STARTUP FAILURE RECOVERY"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#EF4444"))
            letterSpacing = 0.1f
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#371B1B"))
                cornerRadius = dpToPx(6).toFloat()
            }
            background = bg
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dpToPx(8)
            layoutParams = lp
        }
        container.addView(badge)

        // Screen Title
        val titleView = TextView(this).apply {
            text = "Previous startup crash detected"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#F9FAFB"))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dpToPx(8)
            layoutParams = lp
        }
        container.addView(titleView)

        // Subtitle
        val subtitleView = TextView(this).apply {
            text = "The application closed immediately during its previous launch. Technical diagnostic info was saved safely offline without altering your financial data."
            textSize = 13f
            setTextColor(Color.parseColor("#9CA3AF"))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dpToPx(20)
            layoutParams = lp
        }
        container.addView(subtitleView)

        // Summary Card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val cardBg = GradientDrawable().apply {
                setColor(Color.parseColor("#1F2937"))
                cornerRadius = dpToPx(12).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#374151"))
            }
            background = cardBg
            val cardPad = dpToPx(16)
            setPadding(cardPad, cardPad, cardPad, cardPad)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dpToPx(20)
            layoutParams = lp
        }

        addSummaryRow(card, "Date / Time", timestamp)
        addSummaryRow(card, "Last Checkpoint", lastCheckpoint, highlightColor = "#F59E0B")
        addSummaryRow(card, "Exception Type", exceptionType, highlightColor = "#F87171")
        addSummaryRow(card, "Message", exceptionMessage)
        addSummaryRow(card, "Android Version", androidVersion)
        addSummaryRow(card, "App Version", appVersion)
        container.addView(card)

        // Action Buttons
        val btnCopy = createButton("COPY REPORT", "#2563EB") {
            copyReportToClipboard(reportText)
        }
        container.addView(btnCopy)

        val btnShare = createButton("SHARE REPORT", "#059669") {
            shareReport(reportText)
        }
        container.addView(btnShare)

        val btnTryAgain = createButton("TRY STARTING APP AGAIN", "#4B5563") {
            tryAgain()
        }
        container.addView(btnTryAgain)

        val btnBisect = createButton("TEST SAFE BISECTION MODE (STAGE 1)", "#7C3AED") {
            startBisectionMode()
        }
        container.addView(btnBisect)

        // Full Stack Trace / Log Section Title
        val detailsTitle = TextView(this).apply {
            text = "Full Diagnostic Report"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#D1D5DB"))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dpToPx(16)
            lp.bottomMargin = dpToPx(8)
            layoutParams = lp
        }
        container.addView(detailsTitle)

        // Monospace Detail Container
        val detailsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val dtBg = GradientDrawable().apply {
                setColor(Color.parseColor("#030712"))
                cornerRadius = dpToPx(8).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#1F2937"))
            }
            background = dtBg
            val dtPad = dpToPx(12)
            setPadding(dtPad, dtPad, dtPad, dtPad)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
        }

        val detailsText = TextView(this).apply {
            text = reportText
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#D1D5DB"))
            setTextIsSelectable(true)
        }
        detailsContainer.addView(detailsText)
        container.addView(detailsContainer)

        rootScrollView.addView(container)
        setContentView(rootScrollView)
    }

    private fun addSummaryRow(
        parent: LinearLayout,
        label: String,
        value: String,
        highlightColor: String? = null
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dpToPx(8)
            layoutParams = lp
        }

        val labelView = TextView(this).apply {
            text = label
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#9CA3AF"))
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            layoutParams = lp
        }
        row.addView(labelView)

        val valueView = TextView(this).apply {
            text = value
            textSize = 12f
            typeface = if (highlightColor != null) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextColor(if (highlightColor != null) Color.parseColor(highlightColor) else Color.parseColor("#F3F4F6"))
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.0f)
            layoutParams = lp
        }
        row.addView(valueView)

        parent.addView(row)
    }

    private fun createButton(label: String, bgColorHex: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor(bgColorHex))
                cornerRadius = dpToPx(8).toFloat()
            }
            background = bg
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            )
            lp.bottomMargin = dpToPx(12)
            layoutParams = lp
            setOnClickListener { onClick() }
        }
    }

    private fun copyReportToClipboard(reportText: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Startup Crash Report", reportText)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(this, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            Toast.makeText(this, "Failed to copy: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareReport(reportText: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "STARTUP CRASH REPORT")
                putExtra(Intent.EXTRA_TEXT, reportText)

                val file = StartupDiagnostics.getCrashReportFile(this@CrashRecoveryActivity)
                if (file.exists()) {
                    try {
                        val uri = FileProvider.getUriForFile(
                            this@CrashRecoveryActivity,
                            "${packageName}.fileprovider",
                            file
                        )
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (_: Throwable) {}
                }
            }
            val chooser = Intent.createChooser(shareIntent, "Share Startup Crash Report")
            startActivity(chooser)
        } catch (e: Throwable) {
            Toast.makeText(this, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryAgain() {
        StartupDiagnostics.clearCrashReport(this)
        StartupDiagnostics.resetBisection(this)
        restartMainActivity()
    }

    private fun startBisectionMode() {
        StartupDiagnostics.clearCrashReport(this)
        StartupDiagnostics.setBisectionStage(this, 1)
        Toast.makeText(this, "Bisection Stage 1 configured. Launching...", Toast.LENGTH_SHORT).show()
        restartMainActivity()
    }

    private fun restartMainActivity() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
            finish()
        } catch (_: Throwable) {
            finish()
        }
    }

    private fun extractField(report: String, prefix: String): String {
        return try {
            report.lineSequence()
                .firstOrNull { it.startsWith(prefix, ignoreCase = true) }
                ?.substringAfter(":")
                ?.trim()
                ?.ifBlank { "Unknown" }
                ?: "Unknown"
        } catch (_: Throwable) {
            "Unknown"
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
