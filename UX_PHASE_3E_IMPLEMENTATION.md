# Phase 3E Implementation Report: Product UX Simplification, Plain Language & First-Run Onboarding

**Project**: Expense & Salary Tracker  
**Phase**: 3E  
**Target Audience**: "C — Extremely simple everyday expense tracking for ordinary users, with powerful financial tools available when needed."  
**Branch**: `main`  
**Date**: October 4, 2026  

---

## 1. Executive Summary

Phase 3E delivers a comprehensive product-layer simplification across the application without reducing its underlying financial calculation power or compromising its 100% offline, privacy-first security model.

The everyday user experience is focused on answering 4 fundamental questions within 2 seconds of opening the app:
1. **How much money do I have?**
2. **How much can I roughly spend today?**
3. **What have I spent?**
4. **Where did my money go?**

Any routine expense can now be recorded in **5–10 seconds** via a simplified `Add expense` dialog that defaults to `Expense` with primary categories, while all 8 advanced transaction types remain accessible via a smooth progressive disclosure expander.

A lightweight, 6-step local Day-1 onboarding wizard welcomes first-time users, while existing users with transactions or saved configurations bypass onboarding entirely with 0 data loss and 0 disruption.

---

## 2. Navigation Architecture: Before vs. After

### Before
Bottom Navigation (4 items):
1. **Dashboard**
2. **Transactions** (labeled "Ledger" / "Financial Ledger")
3. **Budgets** (labeled "Budgets & Limits")
4. **Settings** (cluttered mix of security, reminders, system settings)

Advanced features like Reports, CSV/XLSX export, and detailed analytics competed directly with basic navigation or were hard to discover.

### After
Bottom Navigation (5 core tabs):
1. **Home** (`Screen.Dashboard`): Daily overview, safe-to-spend guidance, monthly summary, recent spending, and primary "+ Add expense" FAB/action.
2. **Activity** (`Screen.Transactions`): Filterable chronological activity stream with search, categorized chips, and friendly labels.
3. **Budgets** (`Screen.Budgets`): Pay period spending limits and category limits with progress indicators.
4. **Goals** (`Screen.Goals`): Saving/spending targets and the nurturing, non-shaming Tree of Hope.
5. **More** (`Screen.Settings`): Organized hub for secondary/advanced features:
   - **Insights & Data**: Reports & Export (Excel XLSX, PDF, CSV, encrypted backup)
   - **Reminders**: Daily expense reminders, spending limit warnings, goal updates
   - **Security**: App lock (PIN, Biometric), screenshot protection (FLAG_SECURE)
   - **About**: Local database stats, version info

*Note*: When Onboarding is active, the bottom navigation bar is hidden.

---

## 3. Plain-Language Terminology System

In accordance with Phase 3E guidelines, technical accounting, database, and algorithmic jargon was replaced with intuitive, friendly terms:

| Technical / Internal Concept | Replaced In UI With | Context / Screen |
|---|---|---|
| Financial Ledger / Ledger | **Activity** | Bottom Nav & Transactions Screen |
| Transaction History | **Recent spending & income** | Top App Bar Subtitle |
| Safe to Spend Today | **You can spend about today** | Home Screen Hero Section |
| Available Balance | **Available this month** | Home Screen Balance Card |
| Net Cash Flow | **Money in / Money out** (or Summary) | Home & Reports |
| Variance / Remaining | **Left** / **Difference** | Budgets & Home |
| Master Cycle Spending Budget | **Monthly spending limit** | Budgets Screen |
| Exceeded budget limit | **Over limit** | Budgets Warnings |
| Warning threshold reached | **Getting close to limit** | Budgets Warnings |
| Save Allocation | **Money saved** | Transaction Types |
| Expense Reversal | **Refund** | Transaction Types |
| Balance Adjustment | **Fix balance** | Transaction Types |
| Salary Cycle | **Pay period** | Across Home, Budgets & Dialogs |
| Source Account / Paid From Account | **Paid from** | Quick Add Dialog |
| Deposit To Account | **Deposit to** | Income & Debt Inflows |
| Create recurring transaction / Rule | **Repeat transaction** | Quick Add Dialog |
| PBKDF2 / AES-256 GCM technical text | **Encrypted Local Storage** / **App Lock** | Settings / More |

---

## 4. First-Run Onboarding Flow (Day-1 Wizard)

Implemented in `com.expenseapp.salarytracker.presentation.onboarding.OnboardingScreen.kt`:

1. **Step 1: Welcome**
   - Welcoming card: "Let's set up the basics so you can see where your money goes."
   - Actions: `Get started` or `Skip for now` (guarantees the app is usable immediately).
2. **Step 2: Monthly Income**
   - "How much do you usually receive each month?"
   - Dynamically bound to the configured currency symbol (BDT ৳ or USD $). Never hardcoded.
   - Explains: "You can change this anytime in Settings."
3. **Step 3: Pay Date**
   - "When do you usually get paid?"
   - Day of month selector (1–31) with smart presets (1st, 5th, 10th, 25th, Last day of month).
   - Explains: "This helps us show your spending for the right pay period."
4. **Step 4: Spending Limit**
   - "How much would you like to spend each month?"
   - Optional toggle: "I don't want to set a limit yet" (never forces a budget).
5. **Step 5: Where Money Is Kept**
   - "Where do you usually keep your money?"
   - Fast selection chips: Cash, Bank, Card, bKash, Nagad, Wallet.
   - Creates a starting account only if accounts list is empty; never creates duplicate accounts.
6. **Step 6: First Expense Ready**
   - "You're all set!"
   - Primary action: `Add first expense` (opens Quick Add immediately).
   - Secondary action: `Go to Home`.

### Persistence & Existing User Migration
- Persistence uses the existing key-value `user_settings` table under key `has_completed_onboarding`. No Room schema migration was required.
- **Returning User Safeguard**: On app startup, `TransactionDao.getCount()` is checked. If existing transactions > 0, `setCompletedOnboarding(true)` is automatically applied and the app opens straight to Home. Existing users never see the onboarding wizard or lose any data.

---

## 5. Add Expense (Everyday-First Redesign)

Implemented in `com.expenseapp.salarytracker.presentation.components.QuickAddDialog.kt`:
- **Everyday First**: Defaults to `Expense`.
- **Primary Type Switcher**: 3 clean buttons for the 90% use case: `Expense`, `Money received`, and `Move money`.
- **Progressive Disclosure**: A `More` toggle smoothly expands to reveal secondary types (`Refund`, `Money saved`, `Donation`, `Debt paid`, `Debt received`, `Fix balance`).
- **Contextual Form**:
  - `Amount` with currency symbol and fast increment chips (+50, +100, +500, +1000).
  - `Category` picker.
  - `Paid from` account picker.
  - Progressive disclosure for `More options` (Note, tags, custom fields).
  - Progressive disclosure for `Repeat transaction` (replaces technical "Recurring Rule").
- **Save Actions**: Plain language buttons ("Save expense", "Save income", "Move money", "Fix balance").

---

## 6. Financial & Security Invariants Preserved

1. **Zero Financial Engine Changes**:
   - `FinancialCalculationEngine`, `SalaryCycleCalculator`, `MinorUnits`, and all math algorithms are untouched.
   - Exact `MinorUnits` (`Long`) representation preserved. Zero `Double`/`Float` arithmetic introduced for financial values.
   - Currency symbol is dynamically sourced from settings (no hardcoded BDT or USD).
2. **Zero Security Compromises**:
   - Offline-only architecture preserved. Zero `android.permission.INTERNET` in `AndroidManifest.xml`.
   - `FLAG_SECURE` window protection maintained.
   - Biometric/PIN authentication and Keystore encryption untouched.
   - Zero telemetry, analytics, Firebase, or external SDKs added.
3. **Room Database**:
   - Database version remains at version 6.
   - Zero schema migrations needed.

---

## 7. Build & Test Verification

- **Unit Test Suite**: `./gradlew.bat testDebugUnitTest`  
  **Result**: `BUILD SUCCESSFUL` (25 tasks executed/up-to-date, all unit tests passed).
- **Debug Assembly**: `./gradlew.bat assembleDebug`  
  **Result**: `BUILD SUCCESSFUL`.
- **Physical-Device UX Verification**: Physical-device UX verification remains pending.
