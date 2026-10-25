# Requirements Traceability Matrix

## Project: Expense & Salary Tracker (Android)
**Last Updated**: Phase 4A (Daily Tracking, Notifications & Audit)  
**Status Legend**:
- `COMPLETE`: Fully implemented in production code and verified with unit/integration tests.
- `PARTIAL`: Partially implemented with specific limitations noted.
- `NOT IMPLEMENTED`: Not yet implemented.
- `BLOCKED`: Implementation blocked by external or platform dependencies.

---

### 1. Financial Tracking

| Requirement | Implementation | Screen / File | Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Arbitrary expenses** | Transaction recording with custom description, amount, date, account, and category. | `Transaction.kt`, `TransactionDao.kt`, `AddTransactionScreen.kt`, `QuickAddDialog.kt` | `TransactionDaoTest.kt`, `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Custom expense fields** | Metadata key-value JSON storage for custom fields. | `Transaction.kt` (`customFields: Map<String, String>`), `TransactionConverters.kt` | `TransactionConvertersTest.kt` | `COMPLETE` |
| **Daily tracking** | Grouping and daily expenditure breakdowns and day-level queries. | `TransactionRepositoryImpl.kt`, `DashboardScreen.kt`, `TransactionsScreen.kt` | `TransactionRepositoryTest.kt`, `ReportGenerationServiceTest.kt` | `COMPLETE` |
| **Monthly tracking** | Calendar month boundary aggregation and budget calculations. | `BudgetCalculation.kt`, `ReportGenerationService.kt`, `ReportsScreen.kt` | `BudgetCalculationTest.kt`, `ReportGenerationServiceTest.kt` | `COMPLETE` |
| **Salary tracking** | Dedicated salary income logging, cycle tracking, and allocation. | `SalaryCycleCalculator.kt`, `DashboardViewModel.kt`, `DashboardScreen.kt` | `SalaryCycleCalculatorTest.kt` | `COMPLETE` |
| **Salary-cycle dates** | Configurable salary day (1–31) with edge-case month-end snapping (e.g. Feb 28/29). | `SalaryCycleCalculator.kt`, `SettingsRepositoryImpl.kt` | `SalaryCycleCalculatorTest.kt` | `COMPLETE` |
| **Multiple accounts** | Multi-account persistence with individual balance tracking. | `Account.kt`, `AccountDao.kt`, `AccountsScreen.kt` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Cash account** | Standard built-in cash account type support. | `AccountType.CASH`, `AccountDao.kt`, `AccountsScreen.kt` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Bank account** | Checking/savings bank account support. | `AccountType.BANK`, `AccountDao.kt`, `AccountsScreen.kt` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **bKash mobile wallet** | Native bKash MFS account support. | `AccountType.BKASH`, `AccountDao.kt`, `AccountsScreen.kt` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Nagad mobile wallet** | Native Nagad MFS account support. | `AccountType.NAGAD`, `AccountDao.kt`, `AccountsScreen.kt` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Cards** | Credit and debit card account types with credit limit tracking. | `AccountType.CARD`, `AccountDao.kt`, `AccountsScreen.kt` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Custom payment sources** | User-created custom accounts with custom names and types. | `AccountType.OTHER`, `AccountDao.kt`, `AccountsScreen.kt` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |

---

### 2. Financial Calculations

| Requirement | Implementation | Screen / File | Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Income** | Positive inflow calculation into target account balance. | `FinancialCalculationEngine.kt`, `TransactionType.INCOME` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Expenses** | Outflow deduction from target account balance. | `FinancialCalculationEngine.kt`, `TransactionType.EXPENSE` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Refunds** | Offsets prior expenses without inflating gross income. | `FinancialCalculationEngine.kt`, `TransactionType.REFUND` | `FinancialCalculationEngineTest.kt`, `BudgetNotificationServiceTest.kt` | `COMPLETE` |
| **Transfers** | Double-entry transfer between accounts; never counted as expense or income. | `FinancialCalculationEngine.kt`, `TransactionType.TRANSFER` | `FinancialCalculationEngineTest.kt`, `BudgetNotificationServiceTest.kt` | `COMPLETE` |
| **Savings** | Dedicated savings transaction type moving money into savings/goals. | `FinancialCalculationEngine.kt`, `TransactionType.SAVING` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Donations** | Dedicated charity/zakat/donation tracking. | `FinancialCalculationEngine.kt`, `TransactionType.DONATION` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Debt payments** | Outflow paying down liabilities; affects liability balances. | `FinancialCalculationEngine.kt`, `TransactionType.DEBT_PAYMENT` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Debt received** | Inflow from borrowed money; tracked as liability. | `FinancialCalculationEngine.kt`, `TransactionType.DEBT_RECEIVED` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Adjustments** | Reconciliation adjustments for account balance discrepancy corrections. | `FinancialCalculationEngine.kt`, `TransactionType.ADJUSTMENT` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Exact currency arithmetic** | Exact 64-bit integer minor units (`MinorUnits` wrapping `Long`). No `Double`/`Float` for money. | `MinorUnits.kt`, `FinancialCalculationEngine.kt` | `FinancialCalculationEngineTest.kt` | `COMPLETE` |
| **Recurring transactions** | Automated recurrence calculation engine (daily, weekly, monthly, salary-cycle). | `RecurringTransactionEngine.kt`, `RecurringTransaction.kt` | `RecurringTransactionEngineTest.kt` | `COMPLETE` |

---

### 3. Budgets

| Requirement | Implementation | Screen / File | Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Monthly spending cap** | Overall monthly spending ceiling across all categories. | `Budget.kt` (categoryId = null), `BudgetCalculation.kt`, `BudgetsScreen.kt` | `BudgetCalculationTest.kt` | `COMPLETE` |
| **Category budgets** | Category-specific limits with independent tracking. | `Budget.kt` (categoryId != null), `BudgetCalculation.kt`, `BudgetsScreen.kt` | `BudgetCalculationTest.kt` | `COMPLETE` |
| **Planned vs actual** | Variance calculation (`planned - actual`) and percentage utilization. | `BudgetCalculation.kt`, `BudgetProgress.kt`, `BudgetsScreen.kt` | `BudgetCalculationTest.kt` | `COMPLETE` |
| **Warnings** | Configurable warning threshold percentage (default 80%) alert state. | `BudgetCalculation.kt`, `BudgetNotificationService.kt`, `BudgetsScreen.kt` | `BudgetCalculationTest.kt`, `BudgetNotificationServiceTest.kt` | `COMPLETE` |
| **Over-budget state** | Exceeded state detection when spending > 100% with overspend amount. | `BudgetCalculation.kt`, `BudgetNotificationService.kt`, `BudgetsScreen.kt` | `BudgetCalculationTest.kt`, `BudgetNotificationServiceTest.kt` | `COMPLETE` |

---

### 4. Goals & Tree of Hope

| Requirement | Implementation | Screen / File | Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Save target** | Accumulated savings target with deadline and progress tracking. | `GoalType.SAVINGS_TARGET`, `GoalCalculationEngine.kt`, `GoalsScreen.kt` | `GoalCalculationEngineTest.kt` | `COMPLETE` |
| **Spending target** | Maximum spending ceiling goal over a designated period. | `GoalType.SPENDING_TARGET`, `GoalCalculationEngine.kt`, `GoalsScreen.kt` | `GoalCalculationEngineTest.kt` | `COMPLETE` |
| **Category spending target** | Category-isolated spending reduction goal. | `GoalType.CATEGORY_SPENDING_TARGET`, `GoalCalculationEngine.kt`, `GoalsScreen.kt` | `GoalCalculationEngineTest.kt` | `COMPLETE` |
| **Donation target** | Charitable donation accumulation goal. | `GoalType.DONATION_TARGET`, `GoalCalculationEngine.kt`, `GoalsScreen.kt` | `GoalCalculationEngineTest.kt` | `COMPLETE` |
| **No-spend goal** | Zero-spending commitment days goal. | `GoalType.NO_SPEND_DAYS`, `GoalCalculationEngine.kt`, `GoalsScreen.kt` | `GoalCalculationEngineTest.kt` | `COMPLETE` |
| **Streak goal** | Daily budget adherence streak tracking. | `GoalType.STREAK_DAYS`, `GoalCalculationEngine.kt`, `GoalsScreen.kt` | `GoalCalculationEngineTest.kt` | `COMPLETE` |
| **Custom goal** | User-defined custom target amounts and metrics. | `GoalType.CUSTOM`, `GoalCalculationEngine.kt`, `GoalsScreen.kt` | `GoalCalculationEngineTest.kt` | `COMPLETE` |
| **Milestones** | Incremental achievements at 25%, 50%, 75%, and 100% completion. | `GoalCalculationEngine.kt`, `GoalNotificationService.kt`, `GoalsScreen.kt` | `GoalCalculationEngineTest.kt`, `GoalNotificationServiceTest.kt` | `COMPLETE` |
| **Tree of Hope** | Gamified visual progression tree (Seed -> Sprout -> Sapling -> Flowering -> Fruitful) with vitality and health score. | `TreeOfHopeEngine.kt`, `TreeOfHopeVisualizer.kt`, `GoalsScreen.kt` | `GoalCalculationEngineTest.kt` | `COMPLETE` |

---

### 5. Reporting, Export & Backup

| Requirement | Implementation | Screen / File | Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Today period** | Daily spending report for current calendar day. | `ReportPeriodType.TODAY`, `ReportGenerationService.kt`, `ReportsScreen.kt` | `ReportGenerationServiceTest.kt` | `COMPLETE` |
| **Yesterday period** | Daily spending report for previous calendar day. | `ReportPeriodType.YESTERDAY`, `ReportGenerationService.kt`, `ReportsScreen.kt` | `ReportGenerationServiceTest.kt` | `COMPLETE` |
| **This week period** | Monday-to-Sunday weekly report. | `ReportPeriodType.THIS_WEEK`, `ReportGenerationService.kt`, `ReportsScreen.kt` | `ReportGenerationServiceTest.kt` | `COMPLETE` |
| **Last week period** | Previous week report. | `ReportPeriodType.LAST_WEEK`, `ReportGenerationService.kt`, `ReportsScreen.kt` | `ReportGenerationServiceTest.kt` | `COMPLETE` |
| **This month period** | Current calendar month report. | `ReportPeriodType.THIS_MONTH`, `ReportGenerationService.kt`, `ReportsScreen.kt` | `ReportGenerationServiceTest.kt` | `COMPLETE` |
| **Last month period** | Prior calendar month report. | `ReportPeriodType.LAST_MONTH`, `ReportGenerationService.kt`, `ReportsScreen.kt` | `ReportGenerationServiceTest.kt` | `COMPLETE` |
| **Current salary cycle** | Report aligned to current salary disbursement cycle window. | `ReportPeriodType.CURRENT_SALARY_CYCLE`, `ReportGenerationService.kt`, `ReportsScreen.kt` | `ReportGenerationServiceTest.kt` | `COMPLETE` |
| **Previous salary cycle** | Report aligned to previous salary disbursement cycle window. | `ReportPeriodType.PREVIOUS_SALARY_CYCLE`, `ReportGenerationService.kt`, `ReportsScreen.kt` | `ReportGenerationServiceTest.kt` | `COMPLETE` |
| **Custom range** | Arbitrary start date and end date reporting. | `ReportPeriodType.CUSTOM`, `ReportGenerationService.kt`, `ReportsScreen.kt` | `ReportGenerationServiceTest.kt` | `COMPLETE` |
| **CSV export** | RFC 4180 standard CSV with UTF-8 BOM, escaped quotes, and preserved numbers. | `CsvExportEngine.kt` | `CsvExportEngineTest.kt` | `COMPLETE` |
| **XLSX export** | Real Microsoft Excel OpenXML (.xlsx) multi-sheet workbook (Summary, Transactions, Categories, Accounts) with formatted numeric cells. | `XlsxExportEngine.kt` | `XlsxExportEngineTest.kt` | `COMPLETE` |
| **PDF export** | Printable PDF report with header metadata, category breakdown table, and summary totals. | `PdfExportEngine.kt` | `PdfExportEngineTest.kt` | `COMPLETE` |
| **JSON backup** | Canonical offline JSON schema with SHA-256 integrity checksum. | `JsonBackupEngine.kt` | `JsonBackupEngineTest.kt` | `COMPLETE` |
| **Encrypted backup** | Password-derived AES-256-GCM encrypted backup package (`.enc.json`) with PBKDF2 salt and IV. | `EncryptedBackupEngine.kt` | `EncryptedBackupEngineTest.kt` | `COMPLETE` |
| **Restore** | Atomic database transaction restore with validation and duplicate prevention. | `BackupRestoreService.kt` | `BackupRestoreServiceTest.kt` | `COMPLETE` |

---

### 6. Privacy & Security

| Requirement | Implementation | Screen / File | Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Offline-first** | 100% local database and local processing. Zero cloud or third-party servers. | Whole Application Architecture | Architecture Audit | `COMPLETE` |
| **No Internet permission** | `android.permission.INTERNET` omitted entirely from `AndroidManifest.xml`. | `AndroidManifest.xml` | Security Audit | `COMPLETE` |
| **Android Keystore** | Hardware-backed key generation (`AndroidKeyStore`) with AES-256-GCM. | `AndroidKeystoreManager.kt`, `AndroidEncryptionService.kt` | `AndroidEncryptionServiceTest.kt` | `COMPLETE` |
| **AES-GCM encryption** | Authenticated encryption with associated data for sensitive fields & backups. | `AndroidEncryptionService.kt`, `EncryptedBackupEngine.kt` | `AndroidEncryptionServiceTest.kt` | `COMPLETE` |
| **Biometric lock** | BiometricPrompt integration for fingerprint / face unlock. | `BiometricAuthManager.kt`, `SecurityViewModel.kt`, `AppLockScreen.kt` | Security Audit | `COMPLETE` |
| **Passcode protection** | PBKDF2-HMAC-SHA256 salted key derivation (100,000 iterations) with constant-time comparison. | `PasscodeKdfService.kt`, `SecurityViewModel.kt`, `AppLockScreen.kt` | `PasscodeKdfServiceTest.kt` | `COMPLETE` |
| **Application lock** | Enforces lock on cold start, app backgrounding, and configurable timeout. Never bypassed by notifications. | `MainScreen.kt`, `MainActivity.kt`, `SecurityViewModel.kt` | Security Audit | `COMPLETE` |
| **FLAG_SECURE** | Window flag preventing screenshots and task switcher preview leaks. | `MainActivity.kt` (`setScreenshotsAllowed`) | Security Audit | `COMPLETE` |
| **Secure backup** | Offline encrypted export with authenticated checksum and verification. | `EncryptedBackupEngine.kt` | `EncryptedBackupEngineTest.kt` | `COMPLETE` |
| **Atomic restore** | All-or-nothing Room database transaction restoring accounts, categories, transactions, budgets, goals. | `BackupRestoreService.kt` | `BackupRestoreServiceTest.kt` | `COMPLETE` |

---

### 7. Notifications & Daily Tracking

| Requirement | Implementation | Screen / File | Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Daily reminder** | Respectful non-shaming reminder asking if today's expenses are entered. | `DailyReminderWorker.kt`, `AppNotificationManager.kt` | `DailyReminderSchedulerTest.kt` | `COMPLETE` |
| **Configurable reminder time** | User-selected time (default 23:00 / 11:00 PM) stored in settings and scheduled. | `SettingsRepositoryImpl.kt`, `DailyReminderScheduler.kt`, `SettingsScreen.kt` | `DailyReminderSchedulerTest.kt` | `COMPLETE` |
| **Configurable reminder days** | Granular DayOfWeek selection (Mon–Sun); skips unselected days. | `DailyReminderScheduler.kt`, `SettingsRepositoryImpl.kt`, `SettingsScreen.kt` | `DailyReminderSchedulerTest.kt` | `COMPLETE` |
| **Budget warnings** | Alerts when category or overall budget approaches warning threshold (default 80%) or exceeds limit. | `BudgetNotificationService.kt`, `AppNotificationManager.kt` | `BudgetNotificationServiceTest.kt` | `COMPLETE` |
| **Goal notifications** | Neutral, encouraging milestone celebrations (25%, 50%, 75%, 100%). | `GoalNotificationService.kt`, `AppNotificationManager.kt` | `GoalNotificationServiceTest.kt` | `COMPLETE` |
| **Android 13+ permission** | Runtime `POST_NOTIFICATIONS` check and user-friendly permission request UI. | `AppNotificationManager.kt`, `SettingsScreen.kt` | Architecture Audit | `COMPLETE` |
| **Reboot handling** | `BootReceiver` listening for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED` to reschedule reminders. | `BootReceiver.kt`, `AndroidManifest.xml` | Code Review | `COMPLETE` |
| **Notification deduplication** | Deterministic key deduplication in `UserSettingDao` preventing duplicate alerts per period/milestone. | `BudgetNotificationService.kt`, `GoalNotificationService.kt` | `BudgetNotificationServiceTest.kt`, `GoalNotificationServiceTest.kt` | `COMPLETE` |
| **Lock-screen privacy** | `NotificationCompat.VISIBILITY_PRIVATE` with generic `publicVersion` masking financial amounts/names. | `AppNotificationManager.kt` | Code Review | `COMPLETE` |
| **Notification navigation** | Action buttons ("Add Expense", "View Today's Report", "View Budget", "View Goal") routed safely without bypassing lock screen. | `MainActivity.kt`, `MainScreen.kt`, `ReportsScreen.kt` | Code Review | `COMPLETE` |
