# PHASE 3E: COMPLETE UI/UX INVENTORY & FIRST-TIME USER AUDIT
**Project:** Expense & Salary Tracker Android Application  
**Audit Type:** Read-Only Source Code Analysis (Zero Code Modifications)  
**Date:** October 2026  
**Operating Mode:** 100% Offline, Privacy-First Architecture  

---

## 1. EXECUTIVE SUMMARY & INSPECTION METHODOLOGY

This document provides a complete, factual, code-verified inventory and first-time user audit of the Expense & Salary Tracker application. Every screen, dialog, user-facing label, button, navigation path, and financial calculation displayed to the user has been inspected directly from Kotlin source code, XML resources, and theme definitions.

### Scope of Inspected Codebase
- **Presentation Layer:**
  - `presentation/main/MainActivity.kt` & `MainScreen.kt`
  - `presentation/navigation/Screen.kt`
  - `presentation/dashboard/DashboardScreen.kt` & `DashboardViewModel.kt`
  - `presentation/transactions/TransactionsScreen.kt` & `TransactionsViewModel.kt`
  - `presentation/budgets/BudgetsScreen.kt` & `BudgetsViewModel.kt`
  - `presentation/goals/GoalsScreen.kt` & `GoalsViewModel.kt`
  - `presentation/reports/ReportsScreen.kt`, `ReportsViewModel.kt`, & `ExportManager.kt`
  - `presentation/settings/SettingsScreen.kt`
  - `presentation/security/AppLockScreen.kt`, `BiometricAuthManager.kt`, & `SecurityViewModel.kt`
  - `presentation/components/QuickAddDialog.kt`, `EmptyStateView.kt`, & `MoneyDisplay.kt`
  - `presentation/theme/`: `Color.kt`, `SemanticColors.kt`, `Theme.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Elevation.kt`
- **Diagnostics Layer:**
  - `diagnostics/StartupDiagnostics.kt`, `CrashRecoveryActivity.kt`, `BisectionStatusActivity.kt`
- **Data & Domain Layer (State Defaults & Prepopulation):**
  - `data/database/ExpenseDatabase.kt`
  - `domain/model/`: `SalaryCycleConfig.kt`, `TransactionType.kt`, `Category.kt`, `Account.kt`, `Budget.kt`, `Goal.kt`, `RecurringRule.kt`, `CustomField.kt`
  - `domain/service/`: `FinancialCalculationEngine.kt`, `SalaryCycleCalculator.kt`, `GoalCalculationEngine.kt`
- **Resources & Manifest:**
  - `res/values/strings.xml`, `AndroidManifest.xml`

---

## 2. COMPLETE SCREEN MAP

The application consists of **6 primary NavHost screens**, **1 root authentication screen**, **2 diagnostic standalone activities**, and **10 modals/dialogs**.

---

### SCREEN 1: Home (Dashboard)
- **Screen Name in Code:** `DashboardScreen` (`com.expenseapp.salarytracker.presentation.dashboard.DashboardScreen`)
- **User-Facing Title:** "Good morning" / "Good afternoon" / "Good evening" (TopBar title changes dynamically with hour)
- **Navigation Route:** `dashboard` (`Screen.Dashboard.route`)
- **Entry:** App launch (start destination), Bottom Navigation Bar ("Home" tab item)
- **Purpose:** Executive personal financial snapshot for the current salary cycle.
- **Primary Action:** Floating Action Button `+` ("Add expense") opening `QuickAddDialog`.
- **Secondary Actions:**
  - "View all" text button in Recent Activity section -> Navigates to `transactions` screen.
  - "Add expense" button in EmptyStateView (when no transactions exist).
- **Destinations:**
  - `transactions` (`Screen.Transactions.route`) via "View all"
  - `QuickAddDialog` via FAB or empty-state action button
- **Appears on First Launch:** YES (Default landing screen).
- **Requires Previous Setup:** NO (Database auto-populates defaults: ৳120,000 salary, monthly cycle on 1st).
- **Contains Forms:** NO (Display only).
- **Contains Advanced Functionality:** YES (Safe-to-spend algorithm, dynamic salary cycle window calculation, category spend allocation percentages).
- **Day-1 Importance:** Essential.
- **Current Complexity:** High (Combines 5 major financial cards on a single scrolling canvas).

---

### SCREEN 2: Financial Ledger / Activity (Transactions)
- **Screen Name in Code:** `TransactionsScreen` (`com.expenseapp.salarytracker.presentation.transactions.TransactionsScreen`)
- **User-Facing Title:** "Transaction History" (Headline), "Financial Ledger" (Overline label)
- **Navigation Route:** `transactions` (`Screen.Transactions.route`)
- **Entry:** Bottom Navigation Bar ("Activity" tab item), or "View all" link on Home.
- **Purpose:** Chronological record of all financial movements grouped by date with net daily expense calculations.
- **Primary Action:** Floating Action Button `+` ("Add transaction") opening `QuickAddDialog`.
- **Secondary Actions:**
  - Search text input (filters by note, category name, payee).
  - Clear search icon button `X`.
  - Type filter chips: "All", "Expenses", "Income", "Transfers", "Refunds".
  - Account filter chips: "Acc: Cash", "Acc: Prime Bank", "Acc: bKash", etc.
  - Category filter chips: "Transport", "Food", "Groceries", etc.
  - Delete icon button on individual transaction rows.
  - "Clear Filters" button in filtered empty state.
- **Destinations:**
  - `QuickAddDialog` via FAB or empty-state button
  - `DeleteConfirmationDialog` via row delete icon
- **Appears on First Launch:** NO (Reachable via tab 2).
- **Requires Previous Setup:** NO.
- **Contains Forms:** Search bar input; Delete confirmation prompt.
- **Contains Advanced Functionality:** Multi-factor filtering (Search + Type + Account + Category), daily net expense reduction logic (refunds reduce daily spend; transfers/loans excluded), transfer routing display ("Source → Destination").
- **Day-1 Importance:** Essential.
- **Current Complexity:** Medium-High.

---

### SCREEN 3: Budgets & Limits
- **Screen Name in Code:** `BudgetsScreen` (`com.expenseapp.salarytracker.presentation.budgets.BudgetsScreen`)
- **User-Facing Title:** "Budgets & Limits"
- **Navigation Route:** `budgets` (`Screen.Budgets.route`)
- **Entry:** Bottom Navigation Bar ("Budgets" tab item), or notification intent `DEST_BUDGETS`.
- **Purpose:** Configure and monitor overall master cycle spending limits and granular per-category spending ceilings.
- **Primary Action:** Floating Action Button `+` ("Add Budget") opening `BudgetDialog`.
- **Secondary Actions:**
  - "Set Master Spending Limit" button (if overall budget is not configured).
  - Edit icon button on Overall Budget card.
  - Delete icon button on Overall Budget card.
  - "Set Category Limit" button (if no category budgets exist).
  - Edit icon button on Category budget cards.
  - Delete icon button on Category budget cards.
- **Destinations:**
  - `BudgetDialog` (modal) for creating/editing budgets.
- **Appears on First Launch:** NO (Reachable via tab 3).
- **Requires Previous Setup:** Categories must exist (prepopulated by default).
- **Contains Forms:** Yes, via `BudgetDialog`.
- **Contains Advanced Functionality:** Dual budget hierarchy (Master spending cap vs category cap), cycle date window synchronization, variance calculation, customizable warning threshold percentages (50%–95%).
- **Day-1 Importance:** Useful to Advanced.
- **Current Complexity:** High.

---

### SCREEN 4: Goals & Tree of Hope
- **Screen Name in Code:** `GoalsScreen` (`com.expenseapp.salarytracker.presentation.goals.GoalsScreen`)
- **User-Facing Title:** "Goals & Tree of Hope"
- **Navigation Route:** `goals` (`Screen.Goals.route`)
- **Entry:** **ORPHANED FROM IN-APP NAVIGATION.** Only reachable via Notification Intent (`DEST_GOALS`) triggered by goal milestone alerts. Not listed in bottom nav; no button leads here from Home, Budgets, or Settings.
- **Purpose:** Gamified financial health tracking through a digital tree ("Tree of Hope") and financial goal tracking (savings targets, spending ceilings, tracking streaks).
- **Primary Action:** Floating Action Button `+` ("New Goal") opening `CreateGoalDialog`.
- **Secondary Actions:**
  - Filter tabs: "Active (N)", "Completed (N)", "Paused (N)".
  - "Plant First Goal" button in empty state.
  - Goal action buttons on cards: Complete (Check icon), Pause (Pause icon), Resume (Play icon), Delete (Trash icon).
- **Destinations:**
  - `CreateGoalDialog` (modal).
- **Appears on First Launch:** NO.
- **Requires Previous Setup:** NO.
- **Contains Forms:** Yes, via `CreateGoalDialog`.
- **Contains Advanced Functionality:** 7-stage organic growth simulation (Seed to Fruit-Bearing), financial vitality algorithm (0%–100%), milestone detection (25%, 50%, 75%, 100%), 7 distinct goal types (amount savings, spend-under ceiling, streaks, no-spend).
- **Day-1 Importance:** Optional / Gamification.
- **Current Complexity:** High.

---

### SCREEN 5: Reports & Export
- **Screen Name in Code:** `ReportsScreen` (`com.expenseapp.salarytracker.presentation.reports.ReportsScreen`)
- **User-Facing Title:** "Reports & Export" (TopBar), "Executive Financial Reporting" (Subtitle)
- **Navigation Route:** `reports` (`Screen.Reports.route`)
- **Entry:** **ORPHANED FROM IN-APP NAVIGATION.** Only reachable via Notification Intent (`DEST_VIEW_TODAY_REPORT`) triggered by the daily reminder notification. Not listed in bottom nav; no button leads here from Home or Settings.
- **Purpose:** Generate executive financial statements across 8 time windows, export documents via Android Storage Access Framework (SAF), share reports, and backup/restore encrypted local databases.
- **Primary Action:** "Save File (SAF)" button (launches Android system file picker to write file).
- **Secondary Actions:**
  - Period filter chips (8 periods: "This Month", "Salary Cycle", "Today", "Custom...", etc.).
  - Export format filter chips (6 formats: "Excel (.xlsx)", "PDF Report (.pdf)", "CSV Ledger", "CSV Summary", "JSON Backup", "Encrypted Backup").
  - "Share Report" button (generates file in private cache and opens Android system share sheet).
  - "Restore JSON" text button (launches file picker to import backup).
  - Custom date range picker dialog.
- **Destinations:**
  - Android SAF CreateDocument picker
  - Android SAF OpenDocument picker
  - `CustomDateRangeDialog`
  - `BackupPassphraseDialog` (for export encryption or import decryption)
  - `RestoreConfirmationDialog`
- **Appears on First Launch:** NO.
- **Requires Previous Setup:** Requires transactions to display meaningful statements.
- **Contains Forms:** Custom date range form, Passphrase entry forms, Restore strategy selector form.
- **Contains Advanced Functionality:** Multi-sheet OpenXML XLSX engine, Android Canvas PDF generator, RFC 4180 CSV engine with UTF-8 BOM, AES-256-GCM encrypted backup engine with PBKDF2 key derivation, database restore with schema verification and duplicate detection.
- **Day-1 Importance:** Optional / Advanced.
- **Current Complexity:** Very High.

---

### SCREEN 6: Settings & Privacy
- **Screen Name in Code:** `SettingsScreen` (`com.expenseapp.salarytracker.presentation.settings.SettingsScreen`)
- **User-Facing Title:** "Settings & Privacy"
- **Navigation Route:** `settings` (`Screen.Settings.route`)
- **Entry:** Bottom Navigation Bar ("Settings" tab item).
- **Purpose:** Manage local application security, biometric lock, screenshot blocking, notification schedules, and review privacy architecture.
- **Primary Action:** Toggle switches and radio buttons for security/notifications.
- **Secondary Actions:**
  - Radio buttons for Authentication Method: "Disabled", "Biometric or Passcode", "Biometric Only", "Passcode Only".
  - "Set PIN" / "Change PIN" button opening `PasscodeSetupDialog`.
  - Grace Period filter chips: "Immediately", "15 seconds", "1 minute", "5 minutes".
  - Screenshot & Recent-Apps Protection switch (`FLAG_SECURE`).
  - "Lock Application Now" button (instant lock).
  - Grant notification permission button (Android 13+ warning banner).
  - Daily Expense Reminder switch.
  - Reminder Time button opening `ReminderTimePickerDialog`.
  - Applicable reminder days chips (Mon–Sun).
  - Budget Limit & Warning Alerts switch.
  - Goal Milestone Updates switch.
- **Destinations:**
  - `PasscodeSetupDialog`
  - `ReminderTimePickerDialog`
  - Android system permission prompt
- **Appears on First Launch:** NO (Reachable via tab 4).
- **Requires Previous Setup:** NO.
- **Contains Forms:** Yes, PIN setup and reminder time dialogs.
- **Contains Advanced Functionality:** Android Keystore key management, PBKDF2 local passcode hashing (100,000 rounds), WorkManager background job scheduling, dynamic WindowManager `FLAG_SECURE` toggling.
- **Day-1 Importance:** Useful.
- **Current Complexity:** High.

---

### SCREEN 7: Application Lock (Authentication Screen)
- **Screen Name in Code:** `AppLockScreen` (`com.expenseapp.salarytracker.presentation.security.AppLockScreen`)
- **User-Facing Title:** "Expense & Salary Tracker"
- **Navigation Route:** None (Intercepts root composition in `MainScreen` when `securityState.isLocked == true`).
- **Entry:** App launch or foreground resume when App Lock is enabled and grace period has elapsed.
- **Purpose:** Protect sensitive financial data from unauthorized physical device access.
- **Primary Action:** Numeric 4-digit PIN entry via on-screen keypad.
- **Secondary Actions:**
  - Biometric authentication trigger (Fingerprint icon).
  - Backspace button.
- **Destinations:**
  - Dismisses to `MainScreen` upon successful PIN/Biometric verification.
- **Appears on First Launch:** NO (Lock mode defaults to `DISABLED`).
- **Requires Previous Setup:** Requires user to have enabled lock mode in Settings.
- **Contains Forms:** Numeric PIN entry.
- **Contains Advanced Functionality:** AndroidX BiometricPrompt integration, PBKDF2 hash verification.
- **Day-1 Importance:** Optional.
- **Current Complexity:** Low (Clean, single-purpose interface).

---

### SCREEN 8: Quick Add / Add Transaction (Modal Dialog)
- **Screen Name in Code:** `QuickAddDialog` (`com.expenseapp.salarytracker.presentation.components.QuickAddDialog`)
- **User-Facing Title:** "Add Transaction" (Header), "New Financial Entry" (Overline)
- **Navigation Route:** State-driven composable dialog hosted at `MainScreen` root (`showQuickAddDialog == true`).
- **Entry:** FAB `+` on Home, FAB `+` on Activity, Empty-state buttons on Home/Activity, Notification action "Add Expense".
- **Purpose:** Rapid creation of all transactions, transfers, refunds, debt, and recurring rules.
- **Primary Action:** "Save Expense" / "Save Income" / "Confirm Transfer" / "Record Refund", etc. (Dynamic label based on type).
- **Secondary Actions:**
  - Transaction type chip selector (9 types).
  - Amount text field input.
  - Quick amount increment chips (+50, +100, +500, +1000).
  - Contextual Account/Category pickers.
  - Date quick picker ("Today", "Yesterday", "Pick Date...").
  - "More Details" expander (Note/payee, Custom Fields).
  - "Repeat Transaction" expander (Enable Recurring Rule switch, recurrence frequency chips).
  - Close button `X`.
- **Destinations:**
  - Android system DatePickerDialog.
- **Appears on First Launch:** NO (Opens on user tap).
- **Requires Previous Setup:** Accounts and categories (prepopulated).
- **Contains Forms:** Yes, primary input form of the entire app.
- **Contains Advanced Functionality:** Context-adaptive form layout (transfers switch to Source → Destination pickers; refunds switch to Reversal Category + Credit Account), progressive disclosure of metadata, recurring schedule rule generation.
- **Day-1 Importance:** Essential (Core daily interaction).
- **Current Complexity:** Very High (Contains 9 types, 4 disclosure sections, and up to 12 distinct inputs).

---

### SCREEN 9: Startup Crash Recovery (Diagnostic Recovery Screen)
- **Screen Name in Code:** `CrashRecoveryActivity` (`com.expenseapp.salarytracker.diagnostics.CrashRecoveryActivity`)
- **User-Facing Title:** "Previous startup crash detected"
- **Navigation Route:** Standalone Activity (outside Compose/NavHost).
- **Entry:** Launched automatically by `MainActivity.onCreate()` if a crash marker file (`startup_crash_report.txt`) is found in internal storage.
- **Purpose:** Emergency fallback screen allowing the user to copy/share diagnostic reports and clear crashes without crashing in a loop.
- **Primary Action:** "COPY REPORT" / "SHARE REPORT".
- **Secondary Actions:**
  - "TRY STARTING APP AGAIN"
  - "TEST SAFE BISECTION MODE (STAGE 1)"
- **Destinations:**
  - Relaunches `MainActivity` or launches Android system share intent.
- **Appears on First Launch:** Only if a crash occurred during initialization.
- **Requires Previous Setup:** N/A.
- **Contains Forms:** NO.
- **Contains Advanced Functionality:** Pure Android programmatic Views without XML inflation, zero dependencies on Room or Compose.
- **Day-1 Importance:** Diagnostic only.
- **Current Complexity:** Technical / Developer-oriented.

---

### SCREEN 10: Safe Startup Bisection Status (Diagnostic Screen)
- **Screen Name in Code:** `BisectionStatusActivity` (`com.expenseapp.salarytracker.diagnostics.BisectionStatusActivity`)
- **User-Facing Title:** "Stage N Passed Successfully"
- **Navigation Route:** Standalone Activity.
- **Entry:** Triggered during safe-mode diagnostic troubleshooting.
- **Purpose:** Step-by-step diagnostic isolation of application startup stages.
- **Primary Action:** "ADVANCE TO STAGE N" / "RUN FULL APPLICATION".
- **Destinations:** Relaunches `MainActivity`.
- **Appears on First Launch:** NO.
- **Day-1 Importance:** Diagnostic only.
- **Current Complexity:** Technical.

---

## 3. COMPLETE NAVIGATION STRUCTURE

```
App Root
├── Startup Diagnostic Guard (MainActivity.onCreate)
│   ├── [Crash Report Detected] ──► CrashRecoveryActivity
│   └── [Bisection Mode Active] ──► BisectionStatusActivity
│
├── AppLock Guard (MainScreen)
│   └── [securityState.isLocked == true] ──► AppLockScreen (PIN Keypad / BiometricPrompt)
│
└── Main Scaffold (Scaffold + NavigationBar)
    │
    ├── BOTTOM NAVIGATION (Always visible across all tabs)
    │   ├── Tab 1: Dashboard ────► "Home" (Screen.Dashboard)
    │   ├── Tab 2: Transactions ─► "Activity" (Screen.Transactions)
    │   ├── Tab 3: Budgets ──────► "Budgets" (Screen.Budgets)
    │   └── Tab 4: Settings ─────► "Settings" (Screen.Settings)
    │
    ├── FLOATING ACTION BUTTON (FAB)
    │   ├── On Home: ────────────► Opens QuickAddDialog
    │   ├── On Activity: ────────► Opens QuickAddDialog
    │   ├── On Budgets: ─────────► Opens BudgetDialog
    │   └── On Goals: ───────────► Opens CreateGoalDialog
    │
    ├── MODAL DIALOGS (Contextual Overlays)
    │   ├── QuickAddDialog (Root Modal launched from Home FAB, Activity FAB, or Notification)
    │   │   └── DatePickerDialog (Android system picker via "Pick Date...")
    │   ├── DeleteConfirmationDialog (Launched from Activity row delete icon)
    │   ├── BudgetDialog (Launched from Budgets FAB or Card edit icon)
    │   ├── CreateGoalDialog (Launched from Goals FAB)
    │   ├── CustomDateRangeDialog (Launched from Reports period selector)
    │   ├── BackupPassphraseDialog - Export (Launched when saving Encrypted JSON)
    │   ├── BackupPassphraseDialog - Import (Launched when restoring Encrypted JSON)
    │   ├── RestoreConfirmationDialog (Launched after opening backup file)
    │   ├── PasscodeSetupDialog (Launched from Settings "Set PIN")
    │   └── ReminderTimePickerDialog (Launched from Settings reminder time)
    │
    └── ORPHANED / NOTIFICATION-ONLY SCREENS (In NavHost, but missing from Tab Bar)
        ├── Screen.Goals ("Goals & Tree of Hope")
        │   └── Accessible ONLY via Intent: DEST_GOALS (from milestone notification)
        └── Screen.Reports ("Reports & Export")
            └── Accessible ONLY via Intent: DEST_VIEW_TODAY_REPORT (from daily reminder notification)
```

---

## 4. EXACT USER-FACING WORDING AUDIT

Below is the verbatim transcription of all text rendered to users, transcribed directly from source code composables.

### 4.1 Home Screen (`DashboardScreen.kt`)
- **HEADINGS & TITLES:**
  - `"Good morning"` / `"Good afternoon"` / `"Good evening"`
  - `"Salary cycle · {N} days left"`
  - `"Available this month"`
  - `"Safe to spend"`
  - `"This month"`
  - `"Where you spent"`
  - `"Recent activity"`
- **SUBHEADINGS & LABELS:**
  - `"/ day"`
  - `"Income"`
  - `"Spent"`
  - `"Remaining"`
  - `"{percentage}%"`
  - `"{date} · {type.displayName}"`
- **BUTTONS & LINKS:**
  - `"View all"`
  - `"Add expense"`
- **EMPTY STATES:**
  - `"No spending recorded this cycle yet."`
  - Title: `"No transactions yet"`
  - Message: `"Add an expense to start tracking where your money goes."`
  - Action Button: `"Add expense"`
- **ACCESSIBILITY LABELS:**
  - `"Add expense"` (FAB contentDescription)
  - `"{amount} negative"` / `"{amount}"` (MoneyDisplay contentDescription)

---

### 4.2 Financial Ledger / Activity Screen (`TransactionsScreen.kt`)
- **HEADINGS & TITLES:**
  - `"Financial Ledger"`
  - `"Transaction History"`
  - `"{N} records"`
  - `"Today"` / `"Yesterday"` / `"{EEEE, d MMMM}"`
  - `"Delete Transaction?"`
- **SUBHEADINGS & LABELS:**
  - `"REFUND"` (Badge)
  - `"{categoryName} · {accountDisplay} · {time}"`
  - `"−{amount}"` / `"+{amount}"`
- **FILTERS & CHIPS:**
  - `"All"`
  - `"Expenses"`
  - `"Income"`
  - `"Transfers"`
  - `"Refunds"`
  - `"Acc: {account.name}"`
  - `"{category.name}"`
- **PLACEHOLDERS:**
  - `"Search by note, category..."`
- **BUTTONS:**
  - `"Clear Filters"`
  - `"Add Transaction"`
  - `"Delete"`
  - `"Cancel"`
- **CONFIRMATION DIALOGS:**
  - Title: `"Delete Transaction?"`
  - Text: `"Are you sure you want to delete '{note or type}'? This will update all affected balances and salary cycle totals."`
- **EMPTY STATES:**
  - When Filtered:
    - Title: `"No matching transactions"`
    - Message: `"No records match your active search query or filter criteria."`
    - Action: `"Clear Filters"`
  - When Global List Empty:
    - Title: `"No transactions yet"`
    - Message: `"Start tracking your income and expenses by tapping the + button below."`
    - Action: `"Add Transaction"`
- **ACCESSIBILITY LABELS:**
  - `"Add transaction"` (FAB)
  - `"Clear search"` (IconButton)
  - `"Delete transaction"` (IconButton)

---

### 4.3 Add Transaction Modal (`QuickAddDialog.kt`)
- **HEADINGS:**
  - `"New Financial Entry"`
  - `"Add Transaction"`
  - `"AMOUNT"`
  - `"From Account"`
  - `"To Account"`
  - `"Reversal Category"`
  - `"Refund Credited To"`
  - `"Income Source / Category"`
  - `"Category"`
  - `"Deposit To"`
  - `"Deduct From Account"`
  - `"Account to Adjust"`
  - `"Paid From Account"`
  - `"Date"`
  - `"More Details"`
  - `"Repeat Transaction"`
  - `"Custom Fields"`
  - `"Expense Reversal"`
- **TRANSACTION TYPE CHIPS:**
  - `"Expense"`
  - `"Income"`
  - `"Transfer"`
  - `"Refund"`
  - `"Saving"`
  - `"Donation"`
  - `"Debt Payment"`
  - `"Debt Received"`
  - `"Balance Adjustment"`
- **QUICK INCREMENT CHIPS:**
  - `"+50"`, `"+100"`, `"+500"`, `"+1000"`
- **DATE CHIPS:**
  - `"Today"`, `"Yesterday"`, `"Pick Date..."`
- **SUBHEADINGS & HELP TEXT:**
  - `"Note: {note}"` / `"Note, tags, custom fields"`
  - `"Recurring: {frequency}"` / `"One-time entry"`
  - `"Enable Recurring Rule"`
  - `"Refunds offset cycle spending and increase account balance without inflating income."`
- **RECURRENCE FREQUENCY CHIPS:**
  - `"Daily"`, `"Weekly"`, `"Bi-weekly (Every 2 Weeks)"`, `"Monthly"`, `"Yearly"`, `"Custom Days"`
- **PLACEHOLDERS:**
  - `"0.00"`
  - `"Add note or payee (e.g. Lunch at Cafe, Grocery)"`
- **PRIMARY ACTION BUTTONS (Dynamic based on selected type):**
  - EXPENSE: `"Save Expense"`
  - INCOME: `"Save Income"`
  - TRANSFER: `"Confirm Transfer"`
  - REFUND: `"Record Refund"`
  - SAVING: `"Save Allocation"`
  - DONATION: `"Record Donation"`
  - DEBT_PAYMENT: `"Record Debt Payment"`
  - DEBT_RECEIVED: `"Record Loan Received"`
  - ADJUSTMENT: `"Save Adjustment"`
- **ERROR & VALIDATION MESSAGES:**
  - `"Please enter an amount greater than 0"`
  - `"Please select an account"`
  - `"Source and destination accounts must be different"`
- **ACCESSIBILITY LABELS:**
  - `"Close dialog"`
  - `"Transaction amount input"`
  - `"Transfer Direction"`
  - `"Pick custom date"`
  - `"Collapse section"` / `"Expand section"`

---

### 4.4 Budgets Screen (`BudgetsScreen.kt`)
- **HEADINGS & TITLES:**
  - `"Budgets & Limits"`
  - `"Active Cycle: {startDate} – {endDate}"`
  - `"Overall Cycle Spending Budget"` / `"Master Cycle Spending Budget"`
  - `"Category Budgets"`
  - `"{N} Active"`
  - `"Create Spending Budget"` / `"Edit Spending Budget"`
- **SUBHEADINGS & LABELS:**
  - `"{daysLeft} day(s) left"`
  - `"Target: {amount}"`
  - `"Total Spent"`
  - `"Remaining"`
  - `"Overspent by"`
  - `"{percentage}% used"`
  - `"Exceeded budget limit"`
  - `"Warning threshold reached"`
  - `"{actual} of {planned}"`
  - `"{remaining} left"` / `"Over by {amount}"`
  - `"Overspent"` / `"Near Limit"`
  - `"Select Category"`
  - `"Budget Limit ({currencySymbol})"`
  - `"Alert Warning Threshold: {percentage}%"`
- **BUTTONS & TABS:**
  - `"Set Master Spending Limit"`
  - `"Set Category Limit"`
  - `"Overall Master"` (Tab)
  - `"Category Budget"` (Tab)
  - `"Cancel"`
  - `"Save Budget"`
- **EMPTY STATES:**
  - Master Budget Empty State:
    - `"Set a master spending limit across all categories for this salary cycle."`
    - Action: `"Set Master Spending Limit"`
  - Category Budgets Empty State:
    - `"No category budgets configured"`
    - Action: `"Set Category Limit"`

---

### 4.5 Goals & Tree of Hope Screen (`GoalsScreen.kt`)
- **HEADINGS & TITLES:**
  - `"Goals & Tree of Hope"`
  - `"Tree of Hope"`
  - `"Stage: {stageName} (Level {level} of 7)"`
  - `"{percentage}% Vitality"`
  - `"Plant New Financial Goal"`
- **FILTER TABS:**
  - `"Active ({count})"`
  - `"Completed ({count})"`
  - `"Paused ({count})"`
- **GOAL TYPES (Selector & Badges):**
  - `"Save Target Amount"`
  - `"Spend Under Overall Limit"`
  - `"Keep Category Spending Under"`
  - `"Donate Target Amount"`
  - `"No Spend Challenge"`
  - `"Daily Expense Tracking Streak"`
  - `"Custom Goal"`
- **CARD METRICS & LABELS:**
  - `"{current} of {target}"`
  - `"{remaining} remaining"`
  - `"{currentDays} of {targetDays} days"`
  - `"{remainingDays} days left"`
  - `"{percentage}% completed"`
  - Milestone Badges: `"25%"`, `"50%"`, `"75%"`, `"100%"`
- **TREE STAGE NAMES:**
  - `"Seed"`, `"Sprout"`, `"Young Plant"`, `"Growing"`, `"Mature"`, `"Flowering"`, `"Fruit-Bearing"`
- **BUTTONS:**
  - `"Plant First Goal"`
  - `"Plant Goal"`
  - `"Cancel"`
- **EMPTY STATES:**
  - Active: `"No active financial goals"`
  - Completed: `"No completed goals yet"`
  - Paused: `"No paused goals"`
  - Subtitle: `"Set a savings target, spend-under ceiling, or daily streak to grow your Tree of Hope."`

---

### 4.6 Reports & Export Screen (`ReportsScreen.kt`)
- **HEADINGS & TITLES:**
  - `"Reports & Export"`
  - `"Executive Financial Reporting"`
  - `"Report Period"`
  - `"Export Format"`
  - `"Database Backup & Restoration"`
  - `"Executive Statement Preview"`
  - `"Tree of Hope: {stage} (Level {level})"`
  - `"Top Spending Categories"`
  - `"Budget Performance"`
  - `"Select Custom Date Range"`
  - `"Encrypt Backup with Passphrase"`
  - `"Encrypted Backup Detected"`
  - `"Restore Backup Verification"`
- **PERIOD SELECTOR LABELS:**
  - `"This Month"`, `"Current Salary Cycle"`, `"Last Month"`, `"Previous Salary Cycle"`, `"This Week"`, `"Last Week"`, `"Today"`, `"Custom Range"`
- **EXPORT FORMAT LABELS:**
  - `"Excel Workbook (.xlsx)"`, `"PDF Statement (.pdf)"`, `"CSV (Transactions)"`, `"CSV (Summary)"`, `"JSON Backup (Plain)"`, `"Encrypted Backup (.enc)"`
- **EXECUTIVE STATEMENT METRICS:**
  - `"Opening Balance"`, `"Closing Balance"`, `"Total Income"`, `"Net Expenses"`, `"Net Savings ({rate}%)"`, `"Net Cash Flow"`, `"{N} transactions"`
- **BUTTONS & ACTIONS:**
  - `"Save File (SAF)"`
  - `"Share Report"`
  - `"Restore JSON"`
  - `"Start Date: {date}"`, `"End Date: {date}"`, `"Apply Range"`
  - `"Encrypt & Save"`
  - `"Decrypt & Verify"`
  - `"Confirm Merge"`, `"Confirm Wipe & Restore"`
- **RESTORE STRATEGY OPTIONS:**
  - `"Merge & Skip Duplicates (Safe)"`
  - `"Wipe & Replace Everything"`
- **ERROR MESSAGES:**
  - `"Error: Start date cannot be after end date"`
  - `"Passphrase cannot be empty"`
  - `"Passphrases do not match"`
  - `"Invalid Backup File:"`
  - `"Could not open selected file"`

---

### 4.7 Settings & Privacy Screen (`SettingsScreen.kt`)
- **HEADINGS & SECTIONS:**
  - `"Settings & Privacy"`
  - `"Application Lock & Security"`
  - `"Authentication Method"`
  - `"Lock Grace Period"`
  - `"Reminders & Notifications"`
  - `"Applicable Days"`
  - `"Privacy Guarantee"`
  - `"Set 4-Digit Passcode"`
  - `"Select Reminder Time"`
- **LABELS & DESCRIPTIONS:**
  - `"Disabled"` — `"Open app directly without authentication"`
  - `"Biometric or Passcode"` — `"Fingerprint/Face with 4-digit passcode fallback"`
  - `"Biometric Only"` — `"Device biometric prompt only"`
  - `"Passcode Only"` — `"Local 4-digit passcode keypad"`
  - `"Passcode Configured"` / `"Passcode Not Set"` — `"4-digit PBKDF2-hashed PIN for secure local authentication"`
  - Grace Period Options: `"Immediately"`, `"15 seconds"`, `"1 minute"`, `"5 minutes"`
  - `"Screenshot & Recent-Apps Protection"` — `"Blocks screenshots and masks app preview in Android Recents (FLAG_SECURE)"`
  - `"Daily Expense Reminder"` — `"Gentle prompt to record today's spending"`
  - `"Reminder Time"` — `"Scheduled daily at {time}"`
  - `"Budget Limit & Warning Alerts"` — `"Alerts when spending reaches 80% or exceeds budget cap"`
  - `"Goal Milestone Updates"` — `"Encouraging notifications when reaching 25%, 50%, 75%, 100%"`
  - `"100% Offline & Private by Design"`
- **BUTTONS:**
  - `"Set PIN"` / `"Change PIN"`
  - `"Lock Application Now"`
  - `"Grant"` (Notification permission)
  - `"Set Time"`
  - `"Save Passcode"`
  - `"Cancel"`

---

## 5. TERMINOLOGY AUDIT TABLE

Below is the evaluation of all personal finance, technical, and domain-specific terminology present in the app.

| Current Term | Where Used | What It Appears to Mean | Complexity | Could Confuse a Normal User? | Why |
|---|---|---|---|---|---|
| **Salary Cycle** | Home TopBar, Budgets banner, Reports period | The date interval between consecutive salary paydays (e.g. 25th to 24th). | High | YES | Normal users think in standard calendar months. If their salary day is set to the 25th, "Month" and "Cycle" report completely different totals. |
| **Safe to Spend** | Home Section B | Mathematical projection of remaining budget divided by remaining cycle days. | Medium | YES | Users wonder: Is this cash in hand? Can I spend this if I don't have savings? What happens if I overspend today? |
| **Budget Utilization / % Used** | Budgets screen, Reports | Spent amount divided by budget limit as a percentage. | Low-Medium | SLIGHTLY | "Utilization" sounds corporate/accounting; "% spent" is clearer. |
| **Variance** | Budgets (underlying model/display), Reports | The mathematical difference between planned budget and actual spending. | High | YES | "Variance" is formal cost accounting jargon. Normal people call it "Under budget" or "Over budget". |
| **Transfer** | Activity, Quick Add | Moving money from one owned account to another (e.g. Bank to bKash). | Medium | YES | Beginners often think transferring money to bKash is an "Expense". The app correctly excludes it from spending, which can confuse users if balance changes without an expense. |
| **Refund** | Activity, Quick Add | Money returned for a previous purchase. | Medium-High | YES | Users wonder: Is this Income? App treats it as a negative expense (reducing cycle spend without inflating salary income). Complex accounting logic. |
| **Balance Adjustment** | Quick Add (Type selector) | Manual balance override to reconcile wallet discrepancies. | High | YES | Sounds like auditing jargon. Users don't know when to use this vs adding an expense or income. |
| **Allocation** | Quick Add (Saving type button label) | Setting aside money into a savings goal or account. | High | YES | Button says "Save Allocation" when user selects "Saving". Highly abstract word. |
| **Debt Payment** | Quick Add, Activity | Paying back borrowed money. | Medium | YES | Is it an expense? App treats it as an outflow, but technically it is a liability reduction. |
| **Debt Received** | Quick Add, Activity | Borrowing money from someone. | High | YES | Button says "Record Loan Received". Users wonder if it is income or a liability. |
| **Tree of Hope / Vitality** | Goals screen, Reports | Gamified visual metaphor measuring savings health. | Medium | YES | A financial app suddenly showing a botanical tree and "Vitality %" without explanation feels abrupt and puzzling without onboarding. |
| **Milestone** | Goals, Notifications | Hitting 25%, 50%, 75%, 100% of a target. | Low | NO | Well-understood gamification term. |
| **Master Spending Limit / Overall Budget** | Budgets screen | A cap across all spending combined, separate from category budgets. | Medium | YES | Users may not understand how this interacts with individual category limits (e.g., what if category budgets sum to more than master?). |
| **Grace Period** | Settings (App Lock) | Time interval before app locks itself after leaving the screen. | Medium | YES | "Grace period" is insurance/banking jargon. "Lock delay" or "Lock after" is much simpler. |
| **FLAG_SECURE / Screenshot Protection** | Settings | Android WindowManager security flag blocking screen capture and recents masking. | High | YES | Explaining "FLAG_SECURE" or "masking app preview in Android Recents" is developer-level technical detail. |
| **PBKDF2-hashed PIN** | Settings | Cryptographic key derivation function used to hash the 4-digit PIN. | Very High | YES | Pure cryptographic implementation detail shown directly in UI subtext. |
| **AES-256-GCM** | Settings, Reports passphrase dialog | Symmetric encryption standard used for file backups. | Very High | YES | Developer/security specification shown to general consumers. |
| **Storage Access Framework (SAF)** | Reports screen button | Android's native file document picker mechanism. | Very High | YES | The button literally says `"Save File (SAF)"`. Regular users have no idea what "SAF" means. |
| **Restore Strategy: Merge vs Wipe** | Reports (Restore dialog) | Database deduplication vs drop-and-replace algorithm. | High | YES | Database administrative terms presented to users restoring a backup. |

---

## 6. HOME SCREEN DENSITY AUDIT

A detailed top-to-bottom inspection of the Home screen (`DashboardScreen.kt`).

```
┌──────────────────────────────────────────────────────────┐
│ TopAppBar: "Good morning"                                │
│ "Salary cycle · 24 days left"                            │
├──────────────────────────────────────────────────────────┤
│ Section A: Primary Available Balance                     │
│   ৳120,000.00                                            │
│   "Available this month"                                 │
├──────────────────────────────────────────────────────────┤
│ Section B: Card — "Safe to spend"                        │
│   ৳5,000.00 / day                                        │
│   "Based on 24 days remaining in cycle"                  │
├──────────────────────────────────────────────────────────┤
│ Section C: Card — "This month"                           │
│   Income: ৳120,000.00 | Spent: ৳0.00 | Remaining: ৳120k  │
│   [═══════════════════ Progress Bar ═══════════════════] │
├──────────────────────────────────────────────────────────┤
│ Section D: Card — "Where you spent"                      │
│   "No spending recorded this cycle yet."                 │
├──────────────────────────────────────────────────────────┤
│ Section E: Card — "Recent activity"                      │
│   "No transactions yet"                                  │
│   "Add an expense to start tracking..."                  │
│   [ Add expense ]                                        │
└──────────────────────────────────────────────────────────┘
 [FAB: + (Add expense)]
 [Bottom Navigation Bar: Home | Activity | Budgets | Settings]
```

### Detailed Element-by-Element Breakdown:

1. **Greeting & Cycle Header (TopAppBar)**
   - Text: `"Good morning"` / `"Good afternoon"` / `"Good evening"`, Subtitle: `"Salary cycle · {N} days left"`
   - Value shown: Current time-of-day greeting; calculated remaining days in salary cycle window.
   - Action: None.
   - Visual Importance: Secondary header.
   - Essential: Useful context, but introduces the abstract "Salary cycle" concept immediately.
   - First-time user comprehension: Confusing if the user hasn't set their salary date.

2. **Primary Available Balance (Open Canvas)**
   - Text: Large hero numeric amount (e.g. `৳120,000.00`), Subtitle: `"Available this month"`
   - Value shown: `remainingBudget` (Defaults to ৳120,000 on fresh install).
   - Action: None (Display only).
   - Visual Importance: Maximum (DisplayLarge 30sp bold).
   - Essential: Core metric.
   - Competition: Competes directly with "Remaining" in Section C and "Available this month" phrasing vs "Salary cycle".

3. **Safe to Spend Card**
   - Text: `"Safe to spend"`, Amount: `৳X.XX`, Unit: `"/ day"`, Explanation string below.
   - Value shown: Remaining budget divided by remaining days.
   - Action: None.
   - Visual Importance: High (Contained border card).
   - Essential: Highly distinctive feature, but relies on accurate salary configuration.
   - First-time user comprehension: Puzzling on Day 1 because it suggests spending an arbitrary chunk of the default ৳120,000.

4. **This Month Summary Card**
   - Text: Title `"This month"`, Columns: `"Income"` (Green), `"Spent"` (Red/Rust), `"Remaining"` (Forest Green). Slender progress bar.
   - Value shown: Salary amount, net expenses, remaining budget, linear progress indicator.
   - Action: None.
   - Visual Importance: High.
   - Essential: Yes, classic budget overview.
   - Competition: Duplicates the hero balance in Section A and the days/budget calculations in Section B.

5. **Where You Spent Card**
   - Text: Title `"Where you spent"`. If empty: `"No spending recorded this cycle yet."` If populated: Up to 4 category rows with name, percentage, amount, and horizontal progress bar.
   - Value shown: Top 4 categories by expenditure.
   - Action: None.
   - Visual Importance: Medium.
   - First-time user comprehension: Clear empty state, but another card surface on a long screen.

6. **Recent Activity Card**
   - Text: Title `"Recent activity"`, Link: `"View all"`. If empty: EmptyStateView with Receipt icon, `"No transactions yet"`, `"Add an expense to start tracking where your money goes."`, Button: `[Add expense]`. If populated: Up to 5 transaction rows with category icons, note/type, date, and amount.
   - Value shown: 5 most recent transactions across all time.
   - Action: "View all" navigates to Activity tab; "Add expense" button opens QuickAddDialog; clicking transaction row does nothing.
   - Visual Importance: High.

7. **Floating Action Button (FAB)**
   - Icon: `+`, Content description: `"Add expense"`.
   - Action: Opens `QuickAddDialog`.
   - Visual Importance: Persistent focal point (Bottom-right floating 56dp).

### Density Metrics:
- **Total Major Sections:** 5 (Primary Balance, Safe to Spend, Monthly Overview, Category Breakdown, Recent Activity).
- **Total Primary Actions:** 1 (FAB `+`).
- **Total Secondary Actions:** 2 ("View all" link, "Add expense" empty-state button).
- **Total Numeric Values Displayed Simultaneously:** 8 to 14 (Hero Balance, Safe/day, Days left, Income, Spent, Remaining, Budget Limit, Category 1 Amount, Category 1 %, Category 2 Amount, Category 2 %, Recent Tx 1 Amount, Recent Tx 2 Amount).
- **Total Cards / Containers:** 4 distinct bordered cards + 1 TopBar + BottomBar.
- **Total Interactive Elements:** 7 (FAB, View all, Add button, 4 Bottom Nav items).

---

## 7. ADD TRANSACTION / QUICK ADD FLOW AUDIT

Start: User taps FAB `+` or "Add expense".

### Modal Characteristics:
- Implemented as a full Compose `Dialog` with `usePlatformDefaultWidth = false`, max-height 760dp, width 94% of screen.
- Scrollable vertically (`verticalScroll`).

### Field-by-Field Breakdown:

1. **Header & Dismiss**
   - Label: `"New Financial Entry"` (Overline), `"Add Transaction"` (Title).
   - Dismiss: Round close icon button `X` in top-right corner.

2. **Transaction Type Selector (Horizontal Carousel)**
   - Input Type: Horizontally scrolling chips.
   - Options:
     1. `Expense` (Selected by default)
     2. `Income`
     3. `Transfer`
     4. `Refund`
     5. `Saving`
     6. `Donation`
     7. `Debt Payment`
     8. `Debt Received`
     9. `Balance Adjustment`
   - Visible: Immediately at the top.
   - Beginner confusion: 9 distinct types is overwhelming. Most users just want to record "I spent money" or "I got money".

3. **Hero Amount Area**
   - Label: `"AMOUNT"`.
   - Currency symbol: `"৳"` (or currency code).
   - Input Type: Large numeric decimal field with auto-filter (`all { it.isDigit() || it == '.' }`).
   - Default: Empty (Placeholder `"0.00"`).
   - Required: YES (Amount must be > 0).
   - Quick Increment Chips: `+50`, `+100`, `+500`, `+1000` (Directly increments minor units).

4. **Contextual Adaptive Section (Switches based on Type)**
   - **Case A: Standard Types (Expense, Income, Saving, Donation, Debt, Adjustment)**
     - Category Picker (Hidden for Balance Adjustment):
       - Label: `"Category"` (or `"Income Source / Category"` for Income).
       - Layout: FlowRow of category chips with icons and colors.
       - Default: First category or "Food".
     - Account Picker:
       - Label: `"Paid From Account"` / `"Deposit To"` / `"Deduct From Account"` / `"Account to Adjust"`.
       - Layout: FlowRow of account chips (Cash, Bank, bKash, Nagad).
       - Default: First account ("Cash").
   - **Case B: Transfer**
     - Hides category picker completely.
     - Section: `TransferRoutingSection`.
     - Source Account Label: `"From Account"` (FlowRow chips).
     - Downward arrow icon indicator.
     - Destination Account Label: `"To Account"` (FlowRow chips).
   - **Case C: Refund**
     - Banner: `"Expense Reversal"` — `"Refunds offset cycle spending and increase account balance without inflating income."`
     - Reversal Category Picker: `"Reversal Category"`.
     - Deposit Account Picker: `"Refund Credited To"`.

5. **Date Quick Picker**
   - Label: `"Date"`.
   - Quick Chips: `"Today"`, `"Yesterday"`.
   - Custom Date Chip: Appears if custom date is active.
   - Button: `"Pick Date..."` with calendar icon (Opens Android DatePickerDialog).

6. **Progressive Disclosure 1: "More Details"**
   - Header: `"More Details"`, Subtitle: `"Note: {note}"` or `"Note, tags, custom fields"`. Expand/collapse chevron.
   - Hidden by default: YES (Animated visibility).
   - Fields inside:
     - Note TextField: Placeholder `"Add note or payee (e.g. Lunch at Cafe, Grocery)"`.
     - Custom Fields: Dynamic list of text fields if custom fields are defined in DB.

7. **Progressive Disclosure 2: "Repeat Transaction"**
   - Header: `"Repeat Transaction"`, Subtitle: `"Recurring: {frequency}"` or `"One-time entry"`.
   - Hidden by default: YES.
   - Fields inside:
     - Switch: `"Enable Recurring Rule"`.
     - Frequency Chips (when switch ON): `"Daily"`, `"Weekly"`, `"Bi-weekly"`, `"Monthly"`, `"Yearly"`, `"Custom Days"`.

8. **Validation & Save Action**
   - Validation Errors displayed above button:
     - `"Please enter an amount greater than 0"`
     - `"Please select an account"`
     - `"Source and destination accounts must be different"` (for transfers)
   - Save Button Label: Adaptive based on type (`"Save Expense"`, `"Confirm Transfer"`, `"Record Refund"`, etc.).
   - On Success: Inserts transaction into Room, triggers `evaluateBudgets(tx.date)`, creates recurring rule if enabled, dismisses dialog.

---

## 8. ACTIVITY / HISTORY AUDIT

- **Title:** "Transaction History" (Header), "Financial Ledger" (Overline).
- **Search Bar:** Prominent search input (`OutlinedTextField`), placeholder: `"Search by note, category..."`, with trailing clear `X` button.
- **Filter Carousels:** Single horizontal row mixing three distinct filter types:
  - Global Reset: `"All"`
  - Type Filters: `"Expenses"`, `"Income"`, `"Transfers"`, `"Refunds"`
  - Account Filters: `"Acc: Cash"`, `"Acc: Prime Bank"`, `"Acc: bKash"`, `"Acc: Nagad"`
  - Category Filters: `"Transport"`, `"Food"`, `"Groceries"`, etc.
- **Date Grouping:** All transactions grouped into chronological date sections descending.
  - Section Header: `"Today"`, `"Yesterday"`, or formatted date (e.g. `"Sunday, 4 October"`).
  - Daily Net Outflow Metric: Formatted as `−৳X.XX` on the right side of the date header.
    - *Calculation rule in code:* Outflows (`EXPENSE`, `DONATION`, `DEBT_PAYMENT`) minus Inflows/Reversals (`REFUND`). Transfers, salary income, loans received, and savings are excluded from daily net spend.
- **Transaction Row Structure:**
  - Semantic container icon with colored background (e.g. shopping cart, fuel, food, arrow up, swap).
  - Title: Note text, or falls back to `type.displayName`.
  - Subtitle: `"{Category} · {Account} · {Time}"` (e.g. `"Food · Cash · 1:45 PM"`).
  - For Transfers: Account display changes to `"Source → Destination"` (e.g. `"Cash → bKash"`).
  - For Refunds: Includes a dedicated badge `[REFUND]` next to the title.
  - Amount: Bold formatted currency with `−` or `+` sign and semantic text coloring.
  - Trailing action: Trash/Delete icon button with 48dp touch target.
- **Delete Safeguard:**
  - Tapping trash icon opens `AlertDialog`:
    - Title: `"Delete Transaction?"`
    - Body: `"Are you sure you want to delete '{name}'? This will update all affected balances and salary cycle totals."`
    - Confirm: `"Delete"` (Red)
    - Dismiss: `"Cancel"`
- **Empty States:**
  - Filtered: `"No matching transactions"` / `"No records match your active search query or filter criteria."` -> Button: `"Clear Filters"`.
  - Unfiltered: `"No transactions yet"` / `"Start tracking your income and expenses by tapping the + button below."` -> Button: `"Add Transaction"`.

---

## 9. BUDGETS AUDIT

- **Title:** "Budgets & Limits"
- **Active Cycle Banner:** Surface showing `"Active Cycle: {startDate} – {endDate}"` and a badge with `"{N} day(s) left"`.
- **Master Spending Budget Card:**
  - Unconfigured state: Title `"Overall Cycle Spending Budget"`, Description `"Set a master spending limit across all categories for this salary cycle."`, Button `"Set Master Spending Limit"`.
  - Configured state:
    - Header: `"Master Cycle Spending Budget"`, Subtitle: `"Target: ৳X"`.
    - Edit & Delete icon buttons.
    - Two-column metric: `"Total Spent: ৳X"` vs `"Remaining: ৳X"` (or `"Overspent by: ৳X"`).
    - Progress Bar: Linear progress bar colored primary (<75%), amber warning (>75%), or red error (>90% or overspent).
    - Status footer: `"{percentage}% used"`.
    - Warning/Exceeded alerts: Red warning icon + `"Exceeded budget limit"` or Amber warning icon + `"Warning threshold reached"`.
- **Category Budgets Section:**
  - Header: `"Category Budgets"`, Counter: `"{N} Active"`.
  - Cards: One card per category budget showing category color dot, category name, Edit/Delete icons, spent vs planned (`"৳X of ৳Y"`), remaining/over text, progress bar, percentage used, and badges (`"Overspent"` or `"Near Limit"`).
- **Create / Edit Budget Flow (`BudgetDialog`):**
  - Segmented tab switch: `"Overall Master"` vs `"Category Budget"`.
  - If Category Budget selected: Exposed dropdown menu to pick category.
  - Budget Limit input text field (`KeyboardType.Decimal`).
  - Warning Threshold Slider: Discrete slider from 50% to 95% with live percentage text (`"Alert Warning Threshold: 80%"`).
  - Period: Defaults to `BudgetPeriod.SALARY_CYCLE`.
  - Actions: `"Cancel"` and `"Save Budget"`.

---

## 10. GOALS + TREE OF HOPE AUDIT

- **Title:** "Goals & Tree of Hope"
- **Tree of Hope Card:**
  - Left icon: Large circular avatar showing stage emoji (🌱 Seed, 🌿 Sprout, 🪴 Young Plant, 🌳 Growing, 🌲 Mature, 🌸 Flowering, 🍎 Fruit-Bearing).
  - Title: `"Tree of Hope"`.
  - Subtitle: `"Stage: {stageName} (Level {level} of 7)"`.
  - Vitality Badge: `"{score}% Vitality"`.
  - Progress bar: Linear vitality indicator.
  - Dynamic Explanation: e.g. `"Plant your financial goals to grow your Tree of Hope."`
  - Guidance row: Star icon + `"Create a goal to begin."`
- **Goal Filter Tabs:** Segmented row: `"Active (N)"`, `"Completed (N)"`, `"Paused (N)"`.
- **Goal Cards:**
  - Title: Goal name.
  - Badge: Goal type display name (`"Save Target Amount"`, `"Daily Expense Tracking Streak"`, etc.).
  - Actions: Check icon (Complete), Pause icon (Pause), Play icon (Resume), Trash icon (Delete).
  - Progress metrics:
    - Amount-based: `"৳X of ৳Y"`, `"৳Z remaining"`.
    - Streak/Day-based: `"X of Y days"`, `"Z days left"`.
  - Progress bar: Colored primary or error (if spending ceiling exceeded).
  - Footer: `"{percentage}% completed"`.
  - Milestone Badges: Four rectangular pills for `25%`, `50%`, `75%`, `100%` that highlight when achieved.
- **Create Goal Flow (`CreateGoalDialog`):**
  - Dropdown: Goal Type (7 choices).
  - TextField: Goal Name (e.g. "Emergency Fund").
  - Target: Target Amount (numeric) OR Target Days (numeric).
  - Category Dropdown: Visible only for `CATEGORY_SPEND_UNDER`.
  - Actions: `"Cancel"` and `"Plant Goal"`.

---

## 11. INSIGHTS / ANALYTICS / REPORTS AUDIT

- **Title:** "Reports & Export", Subtitle: "Executive Financial Reporting" (or active period label).
- **Period Filter Chips:** Horizontal scrolling selector with 8 presets:
  1. `"This Month"` (Calendar month 1st to end)
  2. `"Current Salary Cycle"` (Salary anchor to next anchor)
  3. `"Last Month"`
  4. `"Previous Salary Cycle"`
  5. `"This Week"`
  6. `"Last Week"`
  7. `"Today"`
  8. `"Custom Range"` (Opens `CustomDateRangeDialog`)
- **Export Format Chips:** 6 formats:
  1. `"Excel Workbook (.xlsx)"`
  2. `"PDF Statement (.pdf)"`
  3. `"CSV (Transactions)"`
  4. `"CSV (Summary)"`
  5. `"JSON Backup (Plain)"`
  6. `"Encrypted Backup (.enc)"`
- **Action Toolbar:**
  - Button 1: `"Save File (SAF)"` with Download icon -> Invokes Android SAF CreateDocument.
  - Button 2: `"Share Report"` with Share icon -> Generates file in cache and opens Intent chooser.
  - Row: `"Database Backup & Restoration"` -> Button `"Restore JSON"`.
- **Executive Statement Preview Card:**
  - Header: `"Executive Statement Preview"`, Counter: `"{N} transactions"`.
  - Metrics Grid:
    - `"Opening Balance"` vs `"Closing Balance"`
    - `"Total Income"` (Green) vs `"Net Expenses"` (Red)
    - `"Net Savings ({savingsRate}%)"` vs `"Net Cash Flow"`
- **Tree of Hope Status Card:** Eco icon + `"Tree of Hope: {State} (Level {N})"`, explanation, and `"Positive Next Step: {actionAdvice}"`.
- **Top Spending Categories Card:** Progress bars and percentage allocations for top 5 categories.
- **Budget Performance Card:** Overall ceiling spending progress bar and status text.
- **Restore Confirmation Dialog:** Displays backup metadata (`accountCount`, `categoryCount`, `transactionCount`, `budgetCount`, `goalCount`, `schemaVersion`), warnings, and radio buttons for restore strategy:
  - `(o) Merge & Skip Duplicates (Safe)`
  - `(o) Wipe & Replace Everything` (Destructive, Red)

---

## 12. SETTINGS AUDIT

Every setting present in `SettingsScreen.kt` classified by user familiarity:

### Section 1: Application Lock & Security
- **Authentication Method (Radio Group):**
  - `Disabled` — `[EVERYDAY]`
  - `Biometric or Passcode` — `[REGULAR]`
  - `Biometric Only` — `[REGULAR]`
  - `Passcode Only` — `[REGULAR]`
- **Passcode Configuration ("Set PIN" / "Change PIN"):** `[REGULAR]` (Standard 4-digit PIN setup dialog).
- **Lock Grace Period (Filter Chips: Immediately, 15s, 1m, 5m):** `[ADVANCED]` (Explains timeout before background lock triggers).
- **Screenshot & Recent-Apps Protection (FLAG_SECURE Toggle):** `[TECHNICAL]` (Subtext explicitly references FLAG_SECURE and recents preview masking).
- **"Lock Application Now" Button:** `[EVERYDAY]` (Manually locks session).

### Section 2: Reminders & Notifications
- **Daily Expense Reminder (Toggle):** `[EVERYDAY]` (Enables daily reminder prompt).
- **Notification Permission Banner (Android 13+):** `[TECHNICAL]` (Explains OS-level runtime permissions).
- **Reminder Time ("Scheduled daily at HH:mm"):** `[REGULAR]` (Time picker dialog with presets).
- **Applicable Days (Mon–Sun Chips):** `[REGULAR]` (Select which days reminder fires).
- **Budget Limit & Warning Alerts (Toggle):** `[REGULAR]` (Alerts at 80% threshold).
- **Goal Milestone Updates (Toggle):** `[REGULAR]` (Alerts at 25%, 50%, 75%, 100%).

### Section 3: Privacy Guarantee Card
- **100% Offline & Private by Design Description:** `[TECHNICAL]` (Lists technical specifications: Zero Internet permissions, hardware-backed Android Keystore AES-256-GCM, PBKDF2WithHmacSHA256 with 100,000 rounds).

---

## 13. FIRST LAUNCH / DAY-ONE EXPERIENCE AUDIT

### Exact First-Run Behavior Trace:
When a new user launches the app for the first time on a clean install:

1. **What does the user see first?**  
   The user is dropped immediately onto the `DashboardScreen` ("Home"). There is no splash screen, no tutorial, and no introductory slider.
2. **Is there onboarding?**  
   **NO FIRST-RUN ONBOARDING IMPLEMENTED.**
3. **Is there a welcome message?**  
   NO. Only the standard time-based greeting ("Good morning" / "Good afternoon" / "Good evening").
4. **Does the app explain what it does?**  
   NO.
5. **Does it ask for salary?**  
   NO. The database silently pre-populates a hardcoded salary of **৳120,000.00** (`UserSettingEntity("salary_amount", "12000000")`).
6. **Does it ask for salary date/cycle?**  
   NO. Pre-populates salary payday as the **1st of the month** (`UserSettingEntity("salary_day", "1")`).
7. **Does it ask for a spending limit?**  
   NO. No budget is configured by default.
8. **Does it ask for an account?**  
   NO. Silently pre-populates four accounts: `"Cash"`, `"Prime Bank"`, `"bKash"`, `"Nagad"`.
9. **Does it ask for categories?**  
   NO. Silently pre-populates 18 default categories (`Transport`, `Food`, `Groceries`, `Rent`, `Utilities`, etc.).
10. **Does it ask for notification permissions?**  
    NO. It only shows a warning banner if the user manually visits the Settings screen on Android 13+.
11. **Does it explain those permissions?**  
    NO, not on launch.
12. **Does it guide the user to add the first expense?**  
    Weakly: The Recent Activity card on Home shows an EmptyStateView saying `"Add an expense to start tracking where your money goes. [Add expense]"`.
13. **Does it explain what happens after the first expense?**  
    NO.
14. **Does it explain the Home screen?**  
    NO. The user immediately sees: `"Available this month: ৳120,000.00"`, `"Safe to spend: ৳4,000.00 / day"`, `"Income: ৳120,000.00"`. A new user who earns ৳30,000 or $2,000 is immediately shown inaccurate financial data.
15. **Does it explain budgets?**  
    NO.
16. **Does it explain goals?**  
    NO.
17. **Does it explain the Tree of Hope?**  
    NO.
18. **Does it offer a tutorial/help system?**  
    NO. There are no tooltips, help dialogs, or informational popups anywhere in the code.
19. **What happens if the user skips setup?**  
    There is no setup to skip. The app assumes complete defaults automatically.
20. **What is the minimum information required before the app becomes useful?**  
    The user must realize they need to go into Settings (or find where salary is configured) to input their actual income and payday; otherwise, every calculation (Safe to Spend, Available Balance, Remaining Budget, Tree Vitality) is completely fictitious.

---

## 14. FEATURE DENSITY AUDIT

| Feature | Role Classification | Why It Is Classified This Way |
|---|---|---|
| **Add Expense** | **A — CORE DAILY** | Fundamental action of the application. |
| **Add Income** | **B — IMPORTANT REGULAR** | Used once or twice a month on payday. |
| **View Available Balance** | **A — CORE DAILY** | Primary question users open the app to answer. |
| **View Safe to Spend** | **A — CORE DAILY** | Daily pacing guidance to avoid running out of money. |
| **Transaction History / Search** | **B — IMPORTANT REGULAR** | Reviewing past expenses, checking notes/receipts. |
| **Category Budgets** | **B — IMPORTANT REGULAR** | Monthly budget setting and monitoring. |
| **Master Spending Limit** | **B — IMPORTANT REGULAR** | Global monthly spending ceiling. |
| **Transfer between Accounts** | **C — OCCASIONAL** | Moving money from Bank to bKash or ATM cash withdrawal. |
| **Daily Reminder Notification** | **B — IMPORTANT REGULAR** | Evening prompt ensuring expenses get logged. |
| **App Lock / Biometrics** | **B — IMPORTANT REGULAR** | Privacy guard on app open. |
| **Tree of Hope Vitality** | **C — OCCASIONAL** | Gamified health check; checked periodically. |
| **Financial Goals (Targets / Streaks)**| **C — OCCASIONAL** | Set once every few months (e.g. Vacation, Emergency fund). |
| **Refund Tracking** | **C — OCCASIONAL** | Rare occurrence when store returns item. |
| **Donation / Debt Tracking** | **C — OCCASIONAL** | Specialized transaction types used sporadically. |
| **XLSX Multi-Sheet Export** | **C — OCCASIONAL** | Monthly tax/accounting export. |
| **PDF Statement Generation** | **C — OCCASIONAL** | Monthly printable financial statement. |
| **CSV Export** | **D — ADVANCED** | Raw data export for spreadsheets. |
| **Encrypted Database Backup (.enc)** | **D — ADVANCED** | Safeguarding database with AES-256 passphrase. |
| **Database Restore (Merge vs Wipe)** | **D — ADVANCED** | Restoring phone data or switching devices. |
| **Recurring Transaction Rules** | **D — ADVANCED** | Automated repeating expenses (Rent, Netflix, WiFi). |
| **Custom Fields Engine** | **D — ADVANCED** | Adding user-defined metadata fields to transactions. |
| **FLAG_SECURE Toggle** | **E — TECHNICAL** | Android WindowManager screenshot blocking. |
| **Lock Grace Period Selection** | **E — TECHNICAL** | Low-level security timeout customization. |
| **Startup Crash Recovery Mode** | **E — TECHNICAL** | Crash triage and recovery engine. |
| **Safe Startup Bisection Mode** | **E — TECHNICAL** | Developer diagnostic isolation mode. |

---

## 15. INFORMATION DUPLICATION AUDIT

| Information | Screen A | Screen B | Same Meaning? | Potential Confusion |
|---|---|---|---|---|
| **Remaining Balance / Available** | Home Section A (`৳120,000.00 "Available this month"`) | Home Section C (`৳120,000.00 "Remaining"`) | YES | Redundant on the very same screen. Hero balance shows ৳120,000, and 5 inches lower the Monthly Overview card shows ৳120,000 again. |
| **Master Budget Limit** | Home Section C (Progress bar limit) | Budgets (`"Master Cycle Spending Budget"`) | YES | If no master budget is set, Home uses salary (৳120k) as the limit, but Budgets screen says "No master budget configured". Inconsistent mental model. |
| **Category Spending Breakdown** | Home Section D (`"Where you spent"`) | Reports (`"Top Spending Categories"`) | PARTIALLY | Home shows current cycle top 4. Reports shows selected period top 5. User may see different percentages if period differs. |
| **Recent Transactions** | Home Section E (5 items) | Activity (Full list) | YES | Expected pattern, but Home displays rows in a card while Activity displays date-grouped sections. |
| **Tree of Hope Status** | Goals (`"Tree of Hope Card"`) | Reports (`"Tree of Hope Status Card"`) | YES | Exactly duplicate information (Stage name, Level, Vitality %, and Action Tip) displayed on two completely different screens. |
| **Opening & Closing Balances** | Reports (`Executive Statement`) | Activity (Daily totals) | PARTIALLY | Reports calculates opening and closing cash positions across all accounts. Activity only shows daily net outflows. |

---

## 16. USER DECISION LOAD AUDIT

### 1. `QuickAddDialog` (Add Transaction)
- **Number of Choices:** **18 distinct decisions** before pressing save:
  - 9 Transaction Types
  - Numeric amount
  - 4 Quick-increment buttons
  - Up to 18 Categories (or 2 Account pickers in Transfer mode)
  - 4 Accounts
  - 3 Date options + DatePicker
  - Note input
  - Custom fields
  - Recurring toggle + 6 recurrence frequencies
- **What the user is trying to do:** Quickly record that they just paid ৳150 for lunch.
- **Why decision load is high:** All transaction types (including exotic ones like Debt Received and Balance Adjustment) compete on the top row. Advanced fields like recurring rules and custom fields are in the same modal.

### 2. `TransactionsScreen` (Activity Filters)
- **Number of Choices:** **Up to 30 filter chips** in a single horizontal scrolling row (All, 4 types, 4 accounts, 18 categories).
- **What the user is trying to do:** Find an expense they made last week.
- **Why decision load is high:** Mixing type filters, account filters, and category filters into a single unsegmented carousel creates visual clutter and horizontal scroll fatigue.

### 3. `ReportsScreen` (Export & Reporting)
- **Number of Choices:** **8 Period chips + 6 Export format chips + 3 Action buttons = 17 choices.**
- **What the user is trying to do:** Share or download a summary of their expenses.
- **Why decision load is high:** Users must know whether they want XLSX, PDF, CSV Transactions, CSV Summary, JSON, or Encrypted JSON, and understand whether they want "This Month" vs "Salary Cycle".

### 4. `SettingsScreen` (Security Options)
- **Number of Choices:** 4 authentication radio buttons, PIN setup, 4 grace period chips, FLAG_SECURE switch, notification switches, time presets.
- **What the user is trying to do:** Protect their app with fingerprint.
- **Why decision load is high:** Exposes low-level implementation details (cryptography types, grace periods, window manager flags).

---

## 17. BEGINNER CONFUSION AUDIT

Screen-by-screen evaluation of prior knowledge requirements:

### 1. Home Screen
- **Prerequisite Knowledge:** Must understand what a "Salary Cycle" is, how "Safe to spend / day" is derived, and why the app displays ৳120,000 when they haven't entered any data yet.
- **Classification:** **MODERATE LEARNING REQUIRED.**
- **Reason:** The density of metrics (Safe to spend vs Remaining vs Available) requires understanding the app's pacing algorithm.

### 2. Activity / Ledger Screen
- **Prerequisite Knowledge:** Understanding the difference between Outflow, Inflow, Transfer, and Refund. Must understand why a transfer between accounts does not show a minus sign on the daily total.
- **Classification:** **MINOR EXPLANATION NEEDED.**
- **Reason:** Clean layout, but the filter carousel and daily net spending math require explanation.

### 3. Quick Add Dialog
- **Prerequisite Knowledge:** Must understand accounting transaction classification (Expense vs Transfer vs Refund vs Adjustment).
- **Classification:** **MODERATE LEARNING REQUIRED.**
- **Reason:** 9 transaction types cause hesitation for a beginner who just bought coffee.

### 4. Budgets Screen
- **Prerequisite Knowledge:** Understanding the distinction between a "Master Cycle Limit" and "Category Budgets", and how "Variance" works.
- **Classification:** **MODERATE LEARNING REQUIRED.**
- **Reason:** The dual-budget hierarchy is complex to configure without guidance.

### 5. Goals & Tree of Hope Screen
- **Prerequisite Knowledge:** Understanding how financial discipline translates into "Tree Vitality" and how streak goals differ from amount savings goals.
- **Classification:** **MODERATE LEARNING REQUIRED.**
- **Reason:** The gamification rules are never explained in the UI.

### 6. Reports & Export Screen
- **Prerequisite Knowledge:** Understanding MIME types, file formats (XLSX vs CSV vs JSON), Storage Access Framework, encryption passphrases, and database merge vs wipe strategies.
- **Classification:** **VERY ADVANCED.**
- **Reason:** Requires computer literacy and accounting knowledge.

### 7. Settings Screen
- **Prerequisite Knowledge:** Understanding Android security architecture, biometric fallbacks, and FLAG_SECURE.
- **Classification:** **ADVANCED.**
- **Reason:** Security settings read like an enterprise security whitepaper.

---

## 18. MAJOR USER JOURNEY AUDIT

### Journey 1: New User → First Launch → Setup → First Expense
1. User installs and opens app.
2. User is dropped directly onto Home with prepopulated ৳120,000 data.
3. No setup prompt appears. User is confused why their balance is ৳120k.
4. User taps FAB `+`.
5. QuickAddDialog appears with 9 types and amount input.
6. User enters amount, selects "Food", leaves Account as "Cash", taps "Save Expense".
7. Modal closes. Home balance updates to ৳119,850.
- **Friction Points:** Zero onboarding, false financial state on launch, no guidance on how to fix salary.

### Journey 2: User Receives Salary → Records Salary → Sees Available Money
1. User receives monthly paycheck.
2. User opens app, taps FAB `+`.
3. User scrolls transaction type carousel to find "Income".
4. Hero Amount changes to Income. Category changes to "Income Source / Category". Account changes to "Deposit To".
5. User enters salary amount, selects "Prime Bank", taps "Save Income".
6. User returns to Home.
- **Friction Points:** If the user records an Income transaction of ৳50,000, but their settings salary was default ৳120,000, the Home screen displays confusing numbers because `config.salaryAmount` is derived from Settings, not from the Income transaction log!

### Journey 3: User Spends Money → Records Expense → Checks Remaining
1. User buys groceries for ৳1,200.
2. User taps FAB `+`.
3. QuickAddDialog opens with Expense preselected.
4. User types `1200`, taps "Groceries", taps "Save Expense".
5. Dialog closes immediately.
6. Home screen Safe-to-Spend recalculates; Monthly Overview Spent increments by ৳1,200; Remaining decrements.
- **Friction Points:** Smooth flow, but user must scroll through 18 categories to find "Groceries".

### Journey 4: User Wants to Know Where Money Went
1. User taps "Activity" tab in bottom navigation.
2. User sees chronological list of transactions grouped by date.
3. User taps "Food" filter chip to see only food expenses.
4. User reviews transactions and daily spending subtotals.
- **Friction Points:** Cannot filter by date range without manual search or opening the hidden Reports screen.

### Journey 5: User Wants to Control Monthly Spending
1. User taps "Budgets" tab in bottom navigation.
2. User sees "Overall Cycle Spending Budget" card.
3. User taps "Set Master Spending Limit".
4. BudgetDialog opens with "Overall Master" selected.
5. User inputs limit (e.g. ৳30,000) and slider threshold (80%).
6. User taps "Save Budget".
7. Card transforms into live gauge showing spent vs remaining.
- **Friction Points:** If the user wants to set a budget for Dining Out, they must tap FAB `+`, switch tab to "Category Budget", open dropdown, find "Food", and save.

### Journey 6: User Wants to Save Money for a Goal
1. User looks for "Goals" tab in bottom navigation — **IT IS MISSING.**
2. User checks Settings — **NO LINK TO GOALS.**
3. User checks Home — **NO LINK TO GOALS.**
4. The user **cannot reach the Goals screen** unless they receive a goal milestone notification!
- **Friction Points:** Screen is completely orphaned from standard navigation.

### Journey 7: User Wants to Export Their History
1. User looks for "Reports" tab in bottom navigation — **IT IS MISSING.**
2. User checks Settings — **NO LINK TO REPORTS.**
3. User **cannot reach Reports** unless they receive the 23:00 daily reminder notification and tap "View Today's Report"!
- **Friction Points:** Screen is completely orphaned from standard navigation.

### Journey 8: User Wants to Restore Backup
1. User wants to restore a previously exported JSON backup.
2. User goes to Settings -> Finds no backup/restore option.
3. User cannot reach Restore unless they get into the orphaned Reports screen via notification.
- **Friction Points:** Database administration tools are tucked into an orphaned screen instead of Settings.

### Journey 9: User Wants to Lock the App
1. User taps "Settings" tab.
2. User sees "Application Lock & Security".
3. User taps "Biometric or Passcode".
4. Passcode setup dialog automatically pops up: user enters 4-digit PIN twice and taps "Save Passcode".
5. User selects Grace Period ("Immediately").
6. User taps "Lock Application Now".
7. App immediately replaces screen with `AppLockScreen` (numeric keypad and fingerprint prompt).
- **Friction Points:** Clear flow, but text contains cryptographic details that may confuse casual users.

---

## 19. MICROCOPY & TONE AUDIT

The tone of the application is formal, executive, and technically precise, but frequently slips into software engineering and accounting jargon.

### Examples of Technical / Engineering Jargon:
- `"Save File (SAF)"` — References Android internal API (Storage Access Framework).
- `"FLAG_SECURE"` — References Android WindowManager flag.
- `"PBKDF2-hashed PIN for secure local authentication"` — References cryptographic hashing algorithm.
- `"Hardware-backed Android Keystore key generation (AES-256-GCM)"` — Implementation specification.
- `"PBKDF2WithHmacSHA256 passcode derivation (100,000 rounds)"` — Cryptographic parameter.
- `"Zero Internet permissions declared in AndroidManifest"` — Android developer terminology.
- `"Schema Version: 2"` — Database migration terminology.
- `"Stage 1 Passed Successfully"` / `"Safe Startup Bisection"` — Compiler/debugging terminology.

### Examples of Formal / Financial Accounting Jargon:
- `"Financial Ledger"` — Banking/accounting terminology instead of "Activity" or "History".
- `"Expense Reversal"` — Accounting term for a store refund.
- `"Save Allocation"` — Abstract economic phrasing for "Save towards Goal".
- `"Balance Adjustment"` — Bookkeeping term for fixing a discrepancy.
- `"Debt Payment"` / `"Debt Received"` — Formal credit terminology.
- `"Net Cash Flow"` — Corporate treasury statement term.
- `"Variance"` — Cost accounting term for difference from budget.

---

## 20. ACCESSIBILITY & COMPREHENSION AUDIT

- **Touch Targets:** All interactive elements (`QuickAddDialog` chips, increment buttons, filter chips, delete buttons, bottom nav items) explicitly implement `minHeight = 48.dp` or `size(48.dp)` complying with WCAG 2.1 touch target guidelines.
- **Typography & Font Sizes:**
  - DisplayLarge: 36sp bold
  - DisplayMedium: 30sp bold
  - TitleLarge: 18sp semi-bold
  - BodyMedium: 13sp normal
  - LabelSmall: 11sp medium
  - Detail labels: 9sp bold (Refund badge) — borderline readability for low-vision users.
- **Semantic Color & Contrast:**
  - Text colors use dedicated `incomeTextColor()` (`#1E6145` light / `#85C3A6` dark), `expenseTextColor()` (`#962D22` light / `#E57373` dark), and `warningTextColor()` (`#7D4E0B` light / `#FFB74D` dark). All achieve WCAG AA contrast ratio (>4.5:1) against surface backgrounds.
  - Information is never conveyed by color alone: transaction rows pair colors with distinct icons (Arrow up, Receipt, Undo, Swap, Shopping cart).
- **Currency & Unicode Support:**
  - Currency defaults to Bengali Taka symbol `"৳"` (`\u09F3`) and renders correctly across all screens without truncation.
  - Formatted via `MinorUnits.format()` with thousand-separators.
- **Screen Reader Descriptions:**
  - FABs have explicit `contentDescription = "Add expense"` / `"Add transaction"`.
  - MoneyDisplay has accessibility semantics appending `"negative"` for minus balances.
  - Transaction delete icon has `contentDescription = "Delete transaction"`.
  - Icon-only buttons have descriptive content labels.

---

## 21. VISUAL STRUCTURE & DESIGN SYSTEM INVENTORY

Directly transcribed from the Phase 3D design system files:

| Element | Specification in Source Code |
|---|---|
| **Root Background** | Light: `AppBackground = Color(0xFFFBF9F5)` (Warm Ivory / Stone) <br> Dark: `DarkBackground = Color(0xFF121514)` (Deep Charcoal) |
| **Surface / Cards** | Light: `AppSurface = Color(0xFFFFFFFF)` (Pure soft white) <br> Dark: `DarkSurface = Color(0xFF1A1E1C)` |
| **Surface Variant** | Light: `AppSurfaceVariant = Color(0xFFF1EDE6)` <br> Dark: `DarkSurfaceVariant = Color(0xFF232926)` |
| **Primary Brand Color** | Light: `AppPrimary = Color(0xFF1B382B)` (Deep Forest Green) <br> Dark: `DarkPrimary = Color(0xFF85C3A6)` (Soft Sage) |
| **Secondary Color** | Light: `AppSecondary = Color(0xFF425C51)` (Muted Slate Teal) |
| **Border / Outline** | `AppBorder = Color(0xFFE8E4DC)` (1dp subtle outline on all cards) |
| **Card Shape / Radius** | `AppShapes.medium = RoundedCornerShape(10.dp)` for cards; `small = 6.dp` for badges; `extraLarge = 18.dp` for chips/pills |
| **Card Elevation** | `AppElevation.none = 0.dp` across all cards. Depth is achieved via 1dp border and tonal contrast, not drop shadows. |
| **Spacing System** | `AppSpacing.xs = 4.dp`, `sm = 8.dp`, `md = 12.dp`, `lg = 16.dp`, `xl = 24.dp`, `navClearance = 88.dp` |
| **Button Style** | Filled rectangular buttons with `RoundedCornerShape(10.dp)` or `12.dp`, containerColor `AppPrimary`, contentColor `White`. |
| **FAB Style** | Floating circle/rounded container `56.dp`, containerColor `AppPrimary`, icon size `24.dp`–`26.dp`. |
| **Bottom Navigation** | Material 3 `NavigationBar` with `surface` background, 4 tabs with outlined vector icons and `labelSmall` text. |
| **Top App Bar** | Material 3 `TopAppBar` with transparent/background container, large bold title, and contextual subtitle. |

---

## 22. COMPREHENSIVE STATE INVENTORY

| Screen | Normal State | Empty State | Loading State | Error State | Success State | Large-Data State |
|---|---|---|---|---|---|---|
| **Home (Dashboard)** | Displays all 5 cards with live financial calculations. | Handled in Category card ("No spending...") and Activity card (EmptyStateView). | Centered `CircularProgressIndicator` when `state.isLoading`. | NOT IMPLEMENTED (Silently falls back to default values). | State updates reactively via StateFlow. | LazyColumn scrolls smoothly with `navClearance` bottom padding. |
| **Activity (Ledger)** | Chronological date-grouped cards with daily net totals. | Two distinct empty states: 1) "No transactions yet", 2) "No matching transactions". | Handled reactively. | NOT IMPLEMENTED. | Delete confirmation dialog dismisses on confirm. | LazyColumn handles unlimited rows efficiently. |
| **Budgets** | Master card + Category budget cards with progress gauges. | Two distinct empty states: 1) Master unconfigured card, 2) "No category budgets configured". | Centered `CircularProgressIndicator`. | Inline validation error text in BudgetDialog. | Dialog dismisses and Room Flow emits update. | LazyColumn with vertical spacing. |
| **Goals** | Tree of Hope card + Goal progress cards with milestone badges. | Three tabbed empty states: "No active goals", "No completed goals", "No paused goals". | Centered `CircularProgressIndicator`. | NOT IMPLEMENTED. | Auto-persists reached milestones (25%, 50%, 75%, 100%). | LazyColumn with vertical spacing. |
| **Reports** | Statement preview, Tree card, Category breakdown, Budget card. | Shows zeroed metrics (Opening/Closing balance ৳0). | Centered `CircularProgressIndicator`. | Error messages displayed via SnackbarHost and dialog text. | Success notifications via SnackbarHost. | LazyColumn handles long reports. |
| **Settings** | Card sections with radio buttons, chips, and switches. | N/A (Static settings layout). | N/A. | Validation error text in PIN and time dialogs. | Settings persist instantly to SharedPreferences / SecureStorage. | N/A. |
| **App Lock** | PIN dot indicators + 3x4 numeric keypad + fingerprint icon. | N/A. | N/A. | Red error text: `"Incorrect passcode"` or `"Biometric not recognized"`. | Unlocks session and transitions to MainScreen. | N/A. |
| **Quick Add** | Full modal form with type selector and amount. | N/A. | Debounced saving flag (`isSaving`). | Red error text above save button: `"Please enter an amount greater than 0"`, etc. | Saves transaction, evaluates budget notifications, dismisses dialog. | Scrollable vertical Column (`heightIn(max = 760.dp)`). |

---

## 23. COMPLETE USER-FACING STRING INVENTORY

Grouped by screen for external UX review:

### Screen: Home
- Titles: `"Good morning"`, `"Good afternoon"`, `"Good evening"`, `"Available this month"`, `"Safe to spend"`, `"This month"`, `"Where you spent"`, `"Recent activity"`
- Labels: `"Salary cycle · {N} days left"`, `"/ day"`, `"Income"`, `"Spent"`, `"Remaining"`, `"{percentage}%"`, `"{date} · {type}"`
- Buttons: `"View all"`, `"Add expense"`
- Empty State: `"No spending recorded this cycle yet."`, `"No transactions yet"`, `"Add an expense to start tracking where your money goes."`

### Screen: Activity (Transactions)
- Titles: `"Financial Ledger"`, `"Transaction History"`, `"{N} records"`, `"Delete Transaction?"`
- Filters: `"All"`, `"Expenses"`, `"Income"`, `"Transfers"`, `"Refunds"`, `"Acc: {name}"`, `"{category}"`
- Placeholders: `"Search by note, category..."`
- Buttons: `"Clear Filters"`, `"Add Transaction"`, `"Delete"`, `"Cancel"`
- Dialog Body: `"Are you sure you want to delete '{name}'? This will update all affected balances and salary cycle totals."`

### Screen: Add Transaction (QuickAddDialog)
- Titles: `"New Financial Entry"`, `"Add Transaction"`, `"AMOUNT"`, `"Expense Reversal"`, `"From Account"`, `"To Account"`, `"Reversal Category"`, `"Refund Credited To"`, `"Category"`, `"Deposit To"`, `"Paid From Account"`, `"Date"`, `"More Details"`, `"Repeat Transaction"`, `"Custom Fields"`
- Chips: `"Expense"`, `"Income"`, `"Transfer"`, `"Refund"`, `"Saving"`, `"Donation"`, `"Debt Payment"`, `"Debt Received"`, `"Balance Adjustment"`, `"+50"`, `"+100"`, `"+500"`, `"+1000"`, `"Today"`, `"Yesterday"`, `"Pick Date..."`, `"Daily"`, `"Weekly"`, `"Bi-weekly (Every 2 Weeks)"`, `"Monthly"`, `"Yearly"`, `"Custom Days"`
- Buttons: `"Save Expense"`, `"Save Income"`, `"Confirm Transfer"`, `"Record Refund"`, `"Save Allocation"`, `"Record Donation"`, `"Record Debt Payment"`, `"Record Loan Received"`, `"Save Adjustment"`
- Errors: `"Please enter an amount greater than 0"`, `"Please select an account"`, `"Source and destination accounts must be different"`

### Screen: Budgets
- Titles: `"Budgets & Limits"`, `"Overall Cycle Spending Budget"`, `"Master Cycle Spending Budget"`, `"Category Budgets"`, `"Create Spending Budget"`, `"Edit Spending Budget"`
- Labels: `"Active Cycle: {start} – {end}"`, `"{N} day(s) left"`, `"Target: {amount}"`, `"Total Spent"`, `"Remaining"`, `"Overspent by"`, `"{percentage}% used"`, `"Exceeded budget limit"`, `"Warning threshold reached"`, `"{actual} of {planned}"`, `"{remaining} left"`, `"Over by {amount}"`, `"Overspent"`, `"Near Limit"`, `"Select Category"`, `"Budget Limit ({symbol})"`, `"Alert Warning Threshold: {N}%"`
- Buttons & Tabs: `"Set Master Spending Limit"`, `"Set Category Limit"`, `"Overall Master"`, `"Category Budget"`, `"Cancel"`, `"Save Budget"`

### Screen: Goals & Tree of Hope
- Titles: `"Goals & Tree of Hope"`, `"Tree of Hope"`, `"Plant New Financial Goal"`
- Labels: `"Stage: {stage} (Level {level} of 7)"`, `"{N}% Vitality"`, `"Active ({N})"`, `"Completed ({N})"`, `"Paused ({N})"`, `"{current} of {target}"`, `"{remaining} remaining"`, `"{currentDays} of {targetDays} days"`, `"{remainingDays} days left"`, `"{N}% completed"`, `"25%"`, `"50%"`, `"75%"`, `"100%"`
- Types: `"Save Target Amount"`, `"Spend Under Overall Limit"`, `"Keep Category Spending Under"`, `"Donate Target Amount"`, `"No Spend Challenge"`, `"Daily Expense Tracking Streak"`, `"Custom Goal"`
- Buttons: `"Plant First Goal"`, `"Plant Goal"`, `"Cancel"`

### Screen: Reports & Export
- Titles: `"Reports & Export"`, `"Executive Financial Reporting"`, `"Report Period"`, `"Export Format"`, `"Database Backup & Restoration"`, `"Executive Statement Preview"`, `"Top Spending Categories"`, `"Budget Performance"`, `"Select Custom Date Range"`, `"Encrypt Backup with Passphrase"`, `"Encrypted Backup Detected"`, `"Restore Backup Verification"`
- Labels: `"Opening Balance"`, `"Closing Balance"`, `"Total Income"`, `"Net Expenses"`, `"Net Savings ({rate}%)"`, `"Net Cash Flow"`, `"{N} transactions"`, `"Overall Ceiling"`
- Buttons: `"Save File (SAF)"`, `"Share Report"`, `"Restore JSON"`, `"Start Date: {d}"`, `"End Date: {d}"`, `"Apply Range"`, `"Encrypt & Save"`, `"Decrypt & Verify"`, `"Confirm Merge"`, `"Confirm Wipe & Restore"`, `"Cancel"`

### Screen: Settings & Privacy
- Titles: `"Settings & Privacy"`, `"Application Lock & Security"`, `"Authentication Method"`, `"Lock Grace Period"`, `"Reminders & Notifications"`, `"Applicable Days"`, `"Privacy Guarantee"`, `"Set 4-Digit Passcode"`, `"Select Reminder Time"`
- Labels: `"Disabled"`, `"Biometric or Passcode"`, `"Biometric Only"`, `"Passcode Only"`, `"Passcode Configured"`, `"Passcode Not Set"`, `"Immediately"`, `"15 seconds"`, `"1 minute"`, `"5 minutes"`, `"Screenshot & Recent-Apps Protection"`, `"Daily Expense Reminder"`, `"Reminder Time"`, `"Budget Limit & Warning Alerts"`, `"Goal Milestone Updates"`, `"100% Offline & Private by Design"`
- Buttons: `"Set PIN"`, `"Change PIN"`, `"Lock Application Now"`, `"Grant"`, `"Set Time"`, `"Save Passcode"`, `"Cancel"`

---

## 24. FINAL SUMMARY

# CURRENT APP UX SUMMARY

## What the app currently does
The application is a 100% offline, privacy-first personal expense tracker designed around salary cycles rather than purely calendar months. It automatically tracks available money, calculates a daily "Safe to spend" allowance, provides a chronological financial ledger with search and filters, supports dual-level budgets (overall spending cap and category limits), gamifies savings progress through a 7-stage "Tree of Hope", generates multi-format financial reports (Excel, PDF, CSV), and secures data using Android Keystore encryption, biometric authentication, and local database backups.

## Current primary navigation
The application is navigated primarily through a persistent 4-tab Material 3 bottom navigation bar:
1. **Home** (`Screen.Dashboard`)
2. **Activity** (`Screen.Transactions`)
3. **Budgets** (`Screen.Budgets`)
4. **Settings** (`Screen.Settings`)

Quick transaction entry is provided via a persistent Floating Action Button (`+`) on the Home and Activity screens.

## Current core daily features
- Checking remaining balance for the current salary cycle.
- Checking the daily "Safe to spend" recommendation.
- Recording daily expenses, income, and transfers via Quick Add.
- Reviewing recent transactions on Home and Activity.
- Receiving the 23:00 daily reminder notification to record spending.

## Current advanced features
- Dual-tier budgeting with customized warning threshold sliders (50%–95%).
- Context-adaptive transaction entry (Source → Destination transfer routing; reversal refunds).
- Recurring transaction schedule rules.
- 7-stage Tree of Hope vitality growth modeling.
- Multi-sheet Microsoft Excel OpenXML (.xlsx) generation.
- Formatted PDF financial statement generation with canvas vector graphics.
- RFC 4180 CSV ledger export with UTF-8 BOM encoding.
- AES-256-GCM encrypted database backups with PBKDF2 passphrase derivation.
- Database restore engine with duplicate transaction detection and schema verification.

## Current technical/complex features
- WindowManager `FLAG_SECURE` screenshot and recent-apps preview masking.
- Low-level lock grace period timeout customization.
- Startup crash detection and isolated recovery activity (`CrashRecoveryActivity`).
- Step-by-step diagnostic startup bisection mode (`BisectionStatusActivity`).
- Storage Access Framework (SAF) document tree handling.

## Current first-run experience
**There is NO first-run onboarding implemented.** On initial launch, the user is dropped directly onto the Home screen without an introduction, tutorial, or setup wizard. The database automatically pre-populates a fictitious salary of ৳120,000, payday on the 1st of the month, and four predefined accounts ("Cash", "Prime Bank", "bKash", "Nagad"). The user is presented with pre-calculated numbers that do not reflect their personal financial reality.

## Current terminology that may be difficult
- Financial/Accounting jargon: *Salary Cycle, Safe to Spend, Variance, Expense Reversal, Balance Adjustment, Save Allocation, Debt Received, Master Spending Limit.*
- Technical/Developer terms: *Save File (SAF), FLAG_SECURE, PBKDF2-hashed PIN, AES-256-GCM, Restore Strategy: Merge vs Wipe, Schema Version.*

## Current high-density screens
- **Home Screen:** Presents 5 major sections, 4 distinct cards, 8 to 14 numeric figures, and competing balance representations simultaneously.
- **Quick Add Modal:** Presents 9 transaction types, amount input, 4 increment chips, up to 18 category chips, 4 account chips, date pickers, notes, custom fields, and recurring options in a single modal.
- **Activity Screen:** Presents an unsegmented carousel of up to 30 filter chips (types, accounts, categories) alongside search.

## Current potentially confusing journeys
- **Recording Income vs Setting Salary:** Recording a salary paycheck in Transactions does not automatically update the Home screen's monthly salary budget, which is decoupled and stored in Settings.
- **Transferring Money:** Moving money between accounts does not reduce net cycle expenses, which confuses users expecting account balance shifts to appear as expenses.
- **Finding Goals or Reports:** A user looking for their savings goals or tax export cannot find them anywhere in the main interface because both screens are completely missing from the navigation menus.

## Current duplicated information
- Remaining cycle balance is shown as the hero balance in Section A of Home, and repeated identically inside the "This month" card in Section C.
- Tree of Hope status, vitality score, and advice tips are rendered identically on both the Goals screen and the Reports screen.
- Overall budget progress is rendered on Home (Section C progress bar), Budgets (Master card), and Reports (Budget Performance card).

## Current missing explanations/tutorials
- No explanation of what "Salary Cycle" means or how to set it.
- No explanation of how "Safe to spend / day" is calculated.
- No explanation of what "Tree of Hope" is or what actions cause it to grow or wilt.
- No explanation of how Transfers or Refunds impact balances.
- No explanation of why the app requires notification permissions.

## Features that are currently exposed to beginners
- 9 transaction types including Balance Adjustment and Debt Received in the primary Add modal.
- Cryptographic details (PBKDF2, AES-256) in Settings and backup prompts.
- Raw Storage Access Framework ("SAF") terminology on the export button.
- Complex dual-budgeting controls (Master vs Category caps with variance sliders).

## Features currently hidden behind secondary navigation
- **Goals & Tree of Hope:** Completely hidden from in-app navigation (only reachable via external milestone notifications).
- **Reports & Export:** Completely hidden from in-app navigation (only reachable via external daily reminder notification action).
- **Database Backup & Restore:** Hidden inside the orphaned Reports screen rather than in Settings.
- **Custom Fields:** Hidden inside the "More Details" progressive disclosure accordion in Quick Add.
- **Recurring Transactions:** Hidden inside the "Repeat Transaction" progressive disclosure accordion in Quick Add.

---

Files inspected: 42  
Screens identified: 10 (6 primary screens, 1 lock screen, 2 diagnostic activities, 1 add modal, plus 9 subdialogs)  
Navigation destinations: 6 NavHost routes, 4 bottom nav tabs, 2 orphaned notification routes  
User-facing terminology items: 20 major terms audited  
Major user journeys: 9 distinct journeys audited  
First-run onboarding status: NO FIRST-RUN ONBOARDING IMPLEMENTED  
Feature count: 25 distinct functional capabilities  
Tests run: 0 (Read-only static code inspection)  
Code changes: NONE  

STOP.
