package com.expenseapp.salarytracker.diagnostics

import android.content.Context
import android.os.Build
import android.os.Process
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Collections

/**
 * Checkpoints visited during application startup sequence.
 */
enum class StartupCheckpoint {
    APPLICATION_HANDLER_INSTALLED,
    APPLICATION_ON_CREATE_STARTED,
    DATABASE_INITIALIZATION_STARTED,
    DATABASE_INITIALIZATION_COMPLETED,
    SECURITY_INITIALIZATION_STARTED,
    SECURITY_INITIALIZATION_COMPLETED,
    MAIN_ACTIVITY_CREATED,
    SET_CONTENT_STARTED,
    THEME_STARTED,
    MAIN_SCREEN_STARTED,
    NAVIGATION_STARTED,
    HOME_STARTED,
    STARTUP_COMPLETED
}

/**
 * Safe startup bisection stages for sequential isolation of failures.
 */
enum class StartupStage(val level: Int, val title: String) {
    STAGE_1_APPLICATION_ONLY(1, "Stage 1: Application only"),
    STAGE_2_APPLICATION_DATABASE(2, "Stage 2: Application + Database"),
    STAGE_3_SECURITY(3, "Stage 3: + Security"),
    STAGE_4_MAIN_ACTIVITY(4, "Stage 4: + MainActivity"),
    STAGE_5_THEME(5, "Stage 5: + Theme"),
    STAGE_6_NAVIGATION(6, "Stage 6: + MainScreen / Navigation"),
    STAGE_7_HOME_FULL(7, "Stage 7: + Home (Full Startup)")
}

/**
 * Lightweight, privacy-safe diagnostic component installed at the earliest technical moment of application launch.
 *
 * Privacy & Security Guarantees:
 * - 100% offline-first. Never makes network requests.
 * - Zero financial data recorded (no transaction amounts, accounts, categories, salary, goals, PINs, or keys).
 * - Only records safe technical metadata (checkpoint, exception class, message, stack trace, device info).
 * - Independent of Room, DataStore, Compose, or app dependency graphs.
 */
object StartupDiagnostics {

    const val CRASH_REPORT_FILE_NAME = "startup_crash_report.txt"
    const val CHECKPOINT_LOG_FILE_NAME = "startup_checkpoints.txt"
    const val LAST_CHECKPOINT_FILE_NAME = "last_checkpoint.txt"
    const val BISECTION_CONFIG_FILE_NAME = "diagnostic_bisection_stage.txt"
    const val NORMAL_EXIT_FILE_NAME = "last_exit_normal.txt"

    @Volatile
    var currentCheckpoint: StartupCheckpoint? = null
        private set

    @Volatile
    var previousCheckpoint: StartupCheckpoint? = null
        private set

    private val checkpointHistory = Collections.synchronizedList(mutableListOf<Pair<Long, StartupCheckpoint>>())

    @Volatile
    private var isInstalled = false

    @Volatile
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    @Volatile
    private var appContext: Context? = null

    /**
     * Installs the uncaught exception handler as early as technically possible.
     * Preserves the previous uncaught exception handler and delegates to it after persisting the diagnostic report.
     */
    @Synchronized
    fun install(context: Context?) {
        if (context != null) {
            appContext = context.applicationContext ?: context
        }

        if (!isInstalled) {
            try {
                previousHandler = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                    handleUncaughtException(thread, throwable)
                }
                isInstalled = true
            } catch (_: Throwable) {
                // Defensive: Never throw from diagnostic installer
            }
        }

        recordCheckpoint(StartupCheckpoint.APPLICATION_HANDLER_INSTALLED)
    }

    /**
     * Updates context reference when Application.attachBaseContext() completes.
     */
    fun onBaseContextAttached(base: Context) {
        appContext = base.applicationContext ?: base
    }

    /**
     * Records a startup checkpoint in memory and safely to a tiny checkpoint file.
     */
    fun recordCheckpoint(checkpoint: StartupCheckpoint) {
        previousCheckpoint = currentCheckpoint
        currentCheckpoint = checkpoint
        val now = System.currentTimeMillis()
        try {
            checkpointHistory.add(now to checkpoint)
        } catch (_: Throwable) {}

        // Defensively persist last checkpoint name
        appContext?.let { ctx ->
            try {
                val file = File(ctx.filesDir, LAST_CHECKPOINT_FILE_NAME)
                file.writeText("${checkpoint.name}\n${Instant.ofEpochMilli(now)}")
            } catch (_: Throwable) {}
        }
    }

    /**
     * Handles uncaught exceptions by synchronously persisting safe diagnostic info.
     */
    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val report = buildCrashReport(thread, throwable)
            persistCrashReport(report)
        } catch (_: Throwable) {
            // Never allow diagnostic failure to suppress original exception
        } finally {
            // Delegate to previous handler so crash is not swallowed permanently
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Builds the formatted crash report adhering strictly to requirements.
     */
    fun buildCrashReport(thread: Thread?, throwable: Throwable?): String {
        val timestamp = try {
            DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        } catch (_: Throwable) {
            System.currentTimeMillis().toString()
        }

        val appVersionName = try {
            appContext?.packageManager?.getPackageInfo(appContext!!.packageName, 0)?.versionName ?: "1.0.0"
        } catch (_: Throwable) {
            "1.0.0"
        }

        val appVersionCode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                appContext?.packageManager?.getPackageInfo(appContext!!.packageName, 0)?.longVersionCode?.toString() ?: "1"
            } else {
                @Suppress("DEPRECATION")
                appContext?.packageManager?.getPackageInfo(appContext!!.packageName, 0)?.versionCode?.toString() ?: "1"
            }
        } catch (_: Throwable) {
            "1"
        }

        val exceptionClass = throwable?.javaClass?.name ?: "UnknownException"
        val exceptionMessage = throwable?.message ?: "No message provided"

        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        } ?: "No stack trace available"

        val causedBy = throwable?.cause?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        } ?: "None"

        val lastCp = currentCheckpoint?.name ?: "UNKNOWN"
        val prevCp = previousCheckpoint?.name ?: "NONE"

        val historyFormatted = synchronized(checkpointHistory) {
            checkpointHistory.joinToString("\n") { (time, cp) ->
                "- ${cp.name} at ${Instant.ofEpochMilli(time)}"
            }
        }

        val wasNormal = wasPreviousLaunchNormal()

        val pid = try {
            Process.myPid().toString()
        } catch (_: Throwable) {
            "N/A"
        }
        val androidRelease = try { Build.VERSION.RELEASE ?: "Unknown" } catch (_: Throwable) { "Unknown" }
        val androidSdk = try { Build.VERSION.SDK_INT.toString() } catch (_: Throwable) { "Unknown" }
        val deviceManufacturer = try { Build.MANUFACTURER ?: "Unknown" } catch (_: Throwable) { "Unknown" }
        val deviceModel = try { Build.MODEL ?: "Unknown" } catch (_: Throwable) { "Unknown" }
        val deviceCode = try { Build.DEVICE ?: "Unknown" } catch (_: Throwable) { "Unknown" }

        return buildString {
            appendLine("STARTUP CRASH REPORT")
            appendLine()
            appendLine("App version: $appVersionName ($appVersionCode)")
            appendLine("Android: $androidRelease (API $androidSdk)")
            appendLine("Device: $deviceManufacturer $deviceModel ($deviceCode)")
            appendLine("Timestamp: $timestamp")
            appendLine()
            appendLine("Last checkpoint: $lastCp")
            appendLine()
            appendLine("Exception: $exceptionClass")
            appendLine("Message: $exceptionMessage")
            appendLine()
            appendLine("Stack trace:")
            appendLine(stackTrace.trim())
            appendLine()
            appendLine("Caused by:")
            appendLine(causedBy.trim())
            appendLine()
            appendLine("Previous checkpoint: $prevCp")
            appendLine()
            appendLine("Process info:")
            appendLine("PID: $pid, Thread: ${thread?.name ?: Thread.currentThread().name}")
            appendLine("Previous launch ended normally: $wasNormal")
            appendLine()
            appendLine("Checkpoint history:")
            appendLine(historyFormatted.ifBlank { "- None recorded" })
        }
    }

    /**
     * Safely persists the crash report using the simplest reliable local file mechanism.
     */
    private fun persistCrashReport(report: String) {
        val ctx = appContext ?: return
        try {
            val dir = ctx.filesDir ?: File("/data/data/${ctx.packageName}/files")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val reportFile = File(dir, CRASH_REPORT_FILE_NAME)
            FileOutputStream(reportFile).use { fos ->
                fos.write(report.toByteArray(Charsets.UTF_8))
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (_: Throwable) {}
            }

            // Also persist checkpoints
            val historyFile = File(dir, CHECKPOINT_LOG_FILE_NAME)
            val historyText = synchronized(checkpointHistory) {
                checkpointHistory.joinToString("\n") { (time, cp) ->
                    "$time,${cp.name}"
                }
            }
            FileOutputStream(historyFile).use { fos ->
                fos.write(historyText.toByteArray(Charsets.UTF_8))
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (_: Throwable) {}
            }

            // Mark launch as not ended normally
            val normalExitFile = File(dir, NORMAL_EXIT_FILE_NAME)
            FileOutputStream(normalExitFile).use { fos ->
                fos.write("false".toByteArray(Charsets.UTF_8))
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {
            // Defensive: Never throw during crash report persistence
        }
    }

    /**
     * Checks if a crash report from a previous startup attempt exists.
     */
    fun hasCrashReport(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, CRASH_REPORT_FILE_NAME)
            file.exists() && file.length() > 0
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Reads the crash report text.
     */
    fun getCrashReportText(context: Context): String? {
        return try {
            val file = File(context.filesDir, CRASH_REPORT_FILE_NAME)
            if (file.exists() && file.length() > 0) file.readText(Charsets.UTF_8) else null
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Returns the File reference for the crash report.
     */
    fun getCrashReportFile(context: Context): File {
        return File(context.filesDir, CRASH_REPORT_FILE_NAME)
    }

    /**
     * Clears the current crash report file.
     */
    fun clearCrashReport(context: Context) {
        try {
            val file = File(context.filesDir, CRASH_REPORT_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
            val cpFile = File(context.filesDir, LAST_CHECKPOINT_FILE_NAME)
            if (cpFile.exists()) {
                cpFile.delete()
            }
        } catch (_: Throwable) {}
    }

    /**
     * Marks the application startup as completed normally.
     */
    fun markStartupCompletedNormally(context: Context) {
        try {
            val file = File(context.filesDir, NORMAL_EXIT_FILE_NAME)
            file.writeText("true")
        } catch (_: Throwable) {}
    }

    /**
     * Checks whether the previous run finished normally.
     */
    private fun wasPreviousLaunchNormal(): Boolean {
        return try {
            val ctx = appContext ?: return false
            val file = File(ctx.filesDir, NORMAL_EXIT_FILE_NAME)
            if (file.exists()) file.readText().trim() == "true" else false
        } catch (_: Throwable) {
            false
        }
    }

    // =========================================================================
    //  SAFE STARTUP BISECTION MODE
    // =========================================================================

    /**
     * Gets current configured bisection stage (1..7). Default is 7 (Full startup).
     */
    fun getBisectionStage(context: Context): Int {
        return try {
            val file = File(context.filesDir, BISECTION_CONFIG_FILE_NAME)
            if (file.exists()) {
                file.readText().trim().toIntOrNull() ?: StartupStage.STAGE_7_HOME_FULL.level
            } else {
                StartupStage.STAGE_7_HOME_FULL.level
            }
        } catch (_: Throwable) {
            StartupStage.STAGE_7_HOME_FULL.level
        }
    }

    /**
     * Sets bisection stage to enforce sequential startup isolation.
     */
    fun setBisectionStage(context: Context, stage: Int) {
        try {
            val file = File(context.filesDir, BISECTION_CONFIG_FILE_NAME)
            file.writeText(stage.coerceIn(1, 7).toString())
        } catch (_: Throwable) {}
    }

    /**
     * Resets bisection stage to full startup.
     */
    fun resetBisection(context: Context) {
        try {
            val file = File(context.filesDir, BISECTION_CONFIG_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (_: Throwable) {}
    }

    /**
     * Checks if startup execution can proceed to the given stage.
     */
    fun canProceedToStage(context: Context, stage: StartupStage): Boolean {
        return getBisectionStage(context) >= stage.level
    }
}
