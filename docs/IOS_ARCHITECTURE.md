# iOS Architecture & Kotlin Multiplatform Foundation Specification

## 1. Executive Summary

This document specifies the technical architecture for extending the **Expense & Salary Tracker** application into a native, high-performance, and secure iOS application using **Kotlin Multiplatform (KMP)** for domain logic and **SwiftUI** for the user interface.

The system guarantees:
1. **Zero Regressions on Android**: The existing Jetpack Compose Android app remains intact and fully functional.
2. **Absolute Financial Precision**: Integer minor-unit 64-bit arithmetic (`MinorUnits` / `Long` / `Int64`) with zero floating-point math across platforms.
3. **100% Offline Operation & Total Privacy**: Zero network permissions, zero telemetry, zero analytics, zero external API dependencies. All data resides exclusively on the physical device.
4. **Native iOS Experience**: Interface implemented in SwiftUI adopting Apple's Human Interface Guidelines, SF Symbols, dynamic type, smooth spring animations, and native Face ID / Touch ID authentication.

---

## 2. High-Level System Architecture

```
+-------------------------------------------------------------------------+
|                              iOS Client                                 |
|  +-------------------------------------------------------------------+  |
|  |             SwiftUI View Layer (Native Human Interface)            |  |
|  |   - HomeDashboardView (Wallets Carousel, 2x2 KPIs, Charts)        |  |
|  |   - OnboardingFlowView (Multi-wallet selection, Mid-cycle check)  |  |
|  |   - TransactionsListView (Search, Filters, Detail Row)            |  |
|  |   - BudgetsOverviewView (Ceilings, Progress Rings, Warnings)       |  |
|  |   - GoalsListView & TreeOfHopeView (Gamified Vitality Canvas)     |  |
|  |   - SettingsView (Biometrics, Salary cycle, Backup/Restore)       |  |
|  +---------------------------------+---------------------------------+  |
|                                    |                                    |
|  +---------------------------------v---------------------------------+  |
|  |              Swift Native Services & Hardware Layer               |  |
|  |   - DatabaseManager (Offline SQLite Engine)                       |  |
|  |   - BiometricSecurityManager (LocalAuthentication / Face ID)      |  |
|  |   - CrossPlatformBackupService (CryptoKit AES-256-GCM / PBKDF2)   |  |
|  |   - NotificationScheduler (UNUserNotificationCenter)              |  |
|  +---------------------------------+---------------------------------+  |
|                                    |                                    |
|  +---------------------------------v---------------------------------+  |
|  |            SharedCore Framework (Kotlin Multiplatform)            |  |
|  |   - MinorUnits: 64-bit Fixed-Point Currency Math Engine           |  |
|  |   - FinancialDate: Zero-Dependency Gregorian Calendar Math        |  |
|  |   - SalaryCycleCalculator: Month boundary clamps, Safe daily pace  |  |
|  |   - FinancialCalculationEngine: Invariants, Double-entry, Net     |  |
|  |   - GoalCalculationEngine: Vitality score & Tree of Hope stages   |  |
|  |   - FinancialInvariantsValidator: Pre/Post transaction integrity  |  |
|  +-------------------------------------------------------------------+  |
+-------------------------------------------------------------------------+
```

---

## 3. Module Boundaries & Responsibilities

### 3.1 `:shared` Module (Kotlin Multiplatform)
- **Target Platforms**: `androidTarget()`, `jvm()`, `iosX64()`, `iosArm64()`, `iosSimulatorArm64()`.
- **Framework Output**: `SharedCore.framework` (compiled via Kotlin/Native for iOS targets).
- **Core Principle**: Zero platform-specific imports. No `java.*`, no `android.*`, no `javax.*`.
- **Key Components**:
  - `MinorUnits.kt`: Integer cents/poisha representation. Overloaded operators (`+`, `-`, `*`), explicit divide with rounding mode, overflow checks, zero-dependency decimal string parser.
  - `FinancialDate.kt`: Proleptic Gregorian calendar engine. Calculates day-of-week, leap years, days in month, day offsets, date comparisons without `java.time` or `NSDate`.
  - `SalaryCycleCalculator.kt`: Calculates exact pay period boundaries (e.g., 25th of current month to 24th of next month), clamping for short months (February 28/29, April 30).
  - `FinancialCalculationEngine.kt`: Computes available balance across selected accounts, total salary cycle spend, safe daily spend rate, and budget utilization.
  - `GoalCalculationEngine.kt`: Tracks goal milestones, computes Tree of Hope vitality percentage ($0-100\%$), and derives growth stages (`SEED`, `SPROUT`, `SAPLING`, `TREE`, `FRUIT_BEARING`, `WITHERED`).

### 3.2 `iosApp` Target (Native SwiftUI)
- **Framework**: SwiftUI on iOS 16.0+.
- **Design Tokens**: `Colors.swift` (dynamic light/dark semantic palette), `SpacingAndShapes.swift` (Apple-standard 8pt grid, continuous squircle corners).
- **State Architecture**: `@State`, `@StateObject`, `@ObservedObject`, `@AppStorage`.
- **Database Engine**: `DatabaseManager.swift` manages an offline SQLite file (`expense_tracker.sqlite`) created in the user's `Application Support` sandboxed directory. Tables and indexes match Android's Room schema exactly.

### 3.3 `:app` Target (Android Jetpack Compose)
- Preserved 100% without structural regressions.
- Existing database schema, encryption keystore, and biometric flows remain unchanged.

---

## 4. Financial Correctness Invariants

To eliminate rounding drift, currency overflow, and corrupted ledgers:
1. **Never Float/Double**: All monetary balances, budget ceilings, goal targets, and transaction amounts are stored and calculated strictly as signed 64-bit integers (`Long` / `Int64`) representing minor units (1 BDT = 100 Poisha, 1 USD = 100 Cents).
2. **Double-Entry Transfers**: A transfer transaction strictly requires an origin account and a destination account. The validator verifies that:
   $$\Delta \text{Balance}(\text{Source}) = -\text{Amount}, \quad \Delta \text{Balance}(\text{Target}) = +\text{Amount}$$
   Net system cashflow effect of an internal transfer is identically 0.
3. **Refund Offsets**: Refunds are credited back to the original category or account without inflating monthly gross income figures.
4. **Safe Daily Spend Formula**:
   $$\text{Safe Daily Spend} = \max\left(0, \frac{\text{Budget Envelope} - \text{Cycle Spend}}{\text{Days Remaining in Cycle}}\right)$$

---

## 5. Security & Privacy Architecture

- **Biometric Protection**: Enforced via Apple's `LocalAuthentication` framework using `LAPolicy.deviceOwnerAuthenticationWithBiometrics`.
- **Multitasking Privacy Shield**: When the app transitions to `inactive` or `background` (e.g. app switcher), a full-screen privacy shield immediately obscures financial data to prevent sensitive financial information from appearing in iOS system app snapshots.
- **Offline Assurance**: Neither the Android manifest nor iOS `Info.plist` requests `android.permission.INTERNET` or iOS networking capabilities. No third-party network SDKs, analytics, or crash reporters are integrated.
