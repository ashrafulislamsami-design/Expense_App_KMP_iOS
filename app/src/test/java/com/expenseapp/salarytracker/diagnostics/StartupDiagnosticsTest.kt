package com.expenseapp.salarytracker.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StartupDiagnosticsTest {

    @Before
    fun setUp() {
        StartupDiagnostics.install(null)
    }

    @Test
    fun testCheckpointProgressionTracksCurrentAndPrevious() {
        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.APPLICATION_HANDLER_INSTALLED)
        assertEquals(StartupCheckpoint.APPLICATION_HANDLER_INSTALLED, StartupDiagnostics.currentCheckpoint)

        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.APPLICATION_ON_CREATE_STARTED)
        assertEquals(StartupCheckpoint.APPLICATION_ON_CREATE_STARTED, StartupDiagnostics.currentCheckpoint)
        assertEquals(StartupCheckpoint.APPLICATION_HANDLER_INSTALLED, StartupDiagnostics.previousCheckpoint)

        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.DATABASE_INITIALIZATION_STARTED)
        assertEquals(StartupCheckpoint.DATABASE_INITIALIZATION_STARTED, StartupDiagnostics.currentCheckpoint)
        assertEquals(StartupCheckpoint.APPLICATION_ON_CREATE_STARTED, StartupDiagnostics.previousCheckpoint)

        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.DATABASE_INITIALIZATION_COMPLETED)
        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.SECURITY_INITIALIZATION_STARTED)
        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.SECURITY_INITIALIZATION_COMPLETED)
        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.MAIN_ACTIVITY_CREATED)
        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.SET_CONTENT_STARTED)
        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.THEME_STARTED)
        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.MAIN_SCREEN_STARTED)
        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.NAVIGATION_STARTED)
        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.HOME_STARTED)
        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.STARTUP_COMPLETED)

        assertEquals(StartupCheckpoint.STARTUP_COMPLETED, StartupDiagnostics.currentCheckpoint)
        assertEquals(StartupCheckpoint.HOME_STARTED, StartupDiagnostics.previousCheckpoint)
    }

    @Test
    fun testBuildCrashReportContainsRequiredTechnicalFields() {
        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.THEME_STARTED)

        val testException = IllegalStateException("Test crash message", RuntimeException("Root cause error"))
        val report = StartupDiagnostics.buildCrashReport(Thread.currentThread(), testException)

        assertNotNull(report)
        assertTrue("Report must start with STARTUP CRASH REPORT", report.contains("STARTUP CRASH REPORT"))
        assertTrue("Report must contain App version", report.contains("App version:"))
        assertTrue("Report must contain Android", report.contains("Android:"))
        assertTrue("Report must contain Device", report.contains("Device:"))
        assertTrue("Report must contain Timestamp", report.contains("Timestamp:"))
        assertTrue("Report must contain Last checkpoint", report.contains("Last checkpoint: THEME_STARTED"))
        assertTrue("Report must contain Exception", report.contains("Exception: java.lang.IllegalStateException"))
        assertTrue("Report must contain Message", report.contains("Message: Test crash message"))
        assertTrue("Report must contain Stack trace", report.contains("Stack trace:"))
        assertTrue("Report must contain Caused by", report.contains("Caused by:"))
        assertTrue("Report must contain Previous checkpoint", report.contains("Previous checkpoint:"))
    }

    @Test
    fun testCrashReportContainsZeroFinancialOrPersonalData() {
        StartupDiagnostics.recordCheckpoint(StartupCheckpoint.DATABASE_INITIALIZATION_STARTED)

        val exception = RuntimeException("Crash during initialization")
        val report = StartupDiagnostics.buildCrashReport(Thread.currentThread(), exception)

        // Strict privacy assertions: zero financial / personal data
        assertFalse(report.contains("transaction_amount"))
        assertFalse(report.contains("salary_amount"))
        assertFalse(report.contains("bank_account"))
        assertFalse(report.contains("passcode_hash"))
        assertFalse(report.contains("encryption_key"))
        assertFalse(report.contains("master_key"))
    }

    @Test
    fun testStartupStagesHierarchy() {
        val stages = StartupStage.entries.sortedBy { it.level }
        assertEquals(7, stages.size)
        assertEquals(StartupStage.STAGE_1_APPLICATION_ONLY, stages[0])
        assertEquals(StartupStage.STAGE_2_APPLICATION_DATABASE, stages[1])
        assertEquals(StartupStage.STAGE_3_SECURITY, stages[2])
        assertEquals(StartupStage.STAGE_4_MAIN_ACTIVITY, stages[3])
        assertEquals(StartupStage.STAGE_5_THEME, stages[4])
        assertEquals(StartupStage.STAGE_6_NAVIGATION, stages[5])
        assertEquals(StartupStage.STAGE_7_HOME_FULL, stages[6])
    }
}
