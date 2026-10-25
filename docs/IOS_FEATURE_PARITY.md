# iOS Feature Parity Matrix

This document provides a comprehensive audit of all 26 core features of the **Expense & Salary Tracker** application, verifying exact functional and visual parity between the Android and iOS platforms.

| # | Feature / Capability | Android Implementation | iOS Implementation | Parity Status | Verification Proof |
|---|----------------------|------------------------|--------------------|:-------------:|-------------------|
| 1 | **Executive At-a-Glance Dashboard** | Jetpack Compose `DashboardScreen.kt` | SwiftUI `HomeDashboardView.swift` | **Full Parity** | Multi-wallet carousel, 2x2 KPIs, pulse curve chart, donut breakdown, recent activity. |
| 2 | **Multi-Wallet Carousel & Filtering** | `WalletSelectorRow.kt` | `HomeDashboardView.swift` (`WalletsCarousel`) | **Full Parity** | Horizontal carousel with Cash, BRAC Bank Astha, bKash, Card, Bank, Citytouch, Nagad. Interactive multi-account filtering. |
| 3 | **2x2 KPI Metric Cards** | `KpiCardGrid.kt` | `KPICardView.swift` & `HomeDashboardView.swift` | **Full Parity** | 1. Monthly Salary<br>2. Available Balance<br>3. Spent This Month<br>4. Safe Daily Spend. Color-coded trends and progress bars. |
| 4 | **Spending Trend Pulse Curve Chart** | Custom Canvas Bezier curve with pace line | `PulseCurveChartView.swift` | **Full Parity** | Cubic Bezier spending curve with subtle vertical gradient fill, dynamic Y-axis currency scale labels, and dashed safe pace line. |
| 5 | **Category Spending Donut Chart** | Custom Canvas Donut | `DonutChartView.swift` | **Full Parity** | Smooth ring segments with center total callout and percentage distribution legend. |
| 6 | **Transaction Activity Ledger** | `TransactionListScreen.kt` | `TransactionsListView.swift` | **Full Parity** | Real-time search, category/type filter chips (All, Expense, Income, Transfer), grouped date rows, swipe-to-delete. |
| 7 | **Quick Add Transaction Modal** | `AddTransactionBottomSheet.kt` | `QuickAddExpenseSheet.swift` | **Full Parity** | Native iOS bottom sheet, integer minor units input, account picker, category grid selector with icons, date picker. |
| 8 | **Category Budget Ceilings** | `BudgetScreen.kt` | `BudgetsOverviewView.swift` | **Full Parity** | Category ceiling allocation, spent amount tracking, remaining amount calculation. |
| 9 | **Budget Ceiling Breach Warnings** | Alert dialogs and red progress rings | `BudgetRowCard` in `BudgetsOverviewView.swift` | **Full Parity** | Dynamic threshold color changes: Green (<70%), Yellow (70-90%), Red (>90% or over ceiling with overage text). |
| 10 | **Savings Goals Progress Tracking** | `GoalsScreen.kt` | `GoalsListView.swift` | **Full Parity** | Target amount, current saved, progress percentage, target date, remaining amount calculation. |
| 11 | **Tree of Hope Financial Vitality** | `TreeOfHopeScreen.kt` | `TreeOfHopeView.swift` | **Full Parity** | Gamified vitality score (0-100%), ambient glow visualizer, dynamic stage representation. |
| 12 | **Tree Stages & Discipline Streak** | `GoalCalculationEngine.kt` | `GoalCalculationEngine.kt` (Shared) & `TreeOfHopeView.swift` | **Full Parity** | Stages: `SEED`, `SPROUT`, `SAPLING`, `TREE`, `FRUIT_BEARING`, `WITHERED`. Streak counter for days within safe daily pace. |
| 13 | **Mid-Cycle Already-Spent Calibration** | `OnboardingScreen.kt` | `OnboardingFlowView.swift` | **Full Parity** | Prompts user on first launch if current date $\neq$ payday to calibrate starting ledger with already-spent amount. |
| 14 | **Customizable Payday (1-31)** | `SalaryCycleConfig.kt` | `SalaryCycleCalculator.kt` (Shared) & `SettingsView.swift` | **Full Parity** | Clamps dates automatically for short months (February 28/29, April 30, etc.). |
| 15 | **Dual Reporting Mode** | `ReportingCycleMode.kt` | `ReportingCycleMode.kt` (Shared) & `SettingsView.swift` | **Full Parity** | Switch between Salary Cycle (e.g. 25th to 24th) and Standard Calendar Month (1st to month end). |
| 16 | **Biometric App Lock** | AndroidX `BiometricPrompt` | Apple `LocalAuthentication` (`BiometricSecurityManager.swift`) | **Full Parity** | Native Face ID / Touch ID lock overlay on app launch and foreground resume. |
| 17 | **Multitasking Privacy Shield** | `FLAG_SECURE` window attribute | `PrivacyShieldOverlayView` in `ExpenseTrackerApp.swift` | **Full Parity** | Obscures UI in iOS multitasking app switcher to prevent sensitive balance exposure. |
| 18 | **Cross-Platform Plain JSON Backup** | `BackupEngine.kt` | `CrossPlatformBackupService.swift` | **Full Parity** | Cross-platform JSON schema version 1 envelope with exact key/value alignment. |
| 19 | **Cross-Platform AES-256 Encrypted Backup** | `EncryptedBackupEngine.kt` | `CrossPlatformBackupService.swift` | **Full Parity** | `EXPBKP_ENC` binary container with PBKDF2-HMAC-SHA256 (100k iterations) and AES-256-GCM authenticated cipher. |
| 20 | **Double-Entry Transfer Invariants** | Room Transaction validation | `FinancialCalculationEngine.kt` (Shared) | **Full Parity** | Verifies equal and opposite balance mutations across source and destination wallets with zero net impact. |
| 21 | **Refund Offset Accounting** | Custom ledger rules | `FinancialCalculationEngine.kt` (Shared) | **Full Parity** | Offsets category expenditures without inflating gross salary income. |
| 22 | **Gregorian Calendar Date Engine** | Java `LocalDate` | `FinancialDate.kt` (Shared pure Kotlin) | **Full Parity** | Zero external platform dependencies. Proleptic Gregorian leap year math verified across both OSes. |
| 23 | **Offline Notification Scheduling** | Android `WorkManager` | Apple `UNUserNotificationCenter` (`NotificationScheduler.swift`) | **Full Parity** | Daily evening check-in reminders and payday notifications scheduled 100% on-device. |
| 24 | **CSV Ledger Export** | Custom CSV string builder | `ReportsView.swift` (`generateCSVExport`) | **Full Parity** | RFC 4180 compliant CSV export with timestamped filenames and system share sheet. |
| 25 | **PDF Executive Report Generation** | Android `PdfDocument` | `ReportsView.swift` (`generatePDFExport`) | **Full Parity** | Formatted executive cashflow report and category breakdown sheet. |
| 26 | **100% Offline & Zero Network Permissions** | Manifest has no `INTERNET` permission | `Info.plist` has zero network domains, zero ATS exceptions | **Full Parity** | Zero outbound traffic, zero analytics, zero telemetry. 100% offline verified. |

---

## 3. Design System Parity & Platform Idioms

While visual parity is maintained with 100% feature coverage, the iOS implementation deliberately respects Apple Human Interface Guidelines rather than cloning Android components:

| Element | Android Jetpack Compose | iOS SwiftUI Native |
|---------|-------------------------|-------------------|
| **Typography** | Roboto / Dynamic Material Type | Apple San Francisco / SF Pro Rounded for currency figures |
| **Icons** | Material Icons (`Icons.Default.*`) | Apple SF Symbols (`systemName: "cart.fill"`, `"leaf.fill"`, etc.) |
| **Haptics** | `HapticFeedbackType.LongPress` | `UIImpactFeedbackGenerator(style: .medium)` |
| **Corner Curvature** | Material RoundedCornerShape | Apple Continuous Squircles (`style: .continuous`) |
| **Sheets & Dialogs** | ModalBottomSheet | Native iOS `.sheet(isPresented:)` with native pull-down dismiss gesture |
| **Biometrics** | Android BiometricManager (Fingerprint/Face) | Apple LocalAuthentication (Face ID / Touch ID) |
