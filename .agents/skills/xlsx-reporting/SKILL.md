---
name: xlsx-reporting
description: >-
  Standards for generating native Excel XLSX spreadsheets with multiple worksheets, formatted CSV exports,
  printable PDF reports, canonical JSON backups, and robust data import validation with duplicate detection.
  Use when developing, testing, or updating data export, import, or reporting features.
---

# XLSX Reporting & Data Interchange

This skill governs data export, report compilation, and data import subsystems. It ensures complete fidelity with Microsoft Excel, standard spreadsheet software, and reliable round-trip database backup and restoration.

---

## 1. XLSX Export Standards (Real Excel Compatibility)

### File Format & Engine
- Generate genuine OpenXML `.xlsx` files (using a lightweight streaming engine or POI-compatible format).
- Files must open cleanly without repair prompts or warnings in Microsoft Excel, Google Sheets, LibreOffice Calc, and Numbers.

### Multi-Worksheet Architecture
Every full workbook export must organize data into structured, dedicated worksheets:

1. **`Summary Dashboard`**:
   - High-level KPIs: Total Income, Total Expenses, Net Savings, Savings Rate, Net Worth.
   - Period-over-period comparison.
   - Top 5 spending categories.
2. **`Transactions Ledger`**:
   - Complete itemized log of all transactions.
   - Columns: `Date`, `Account`, `Category`, `Subcategory`, `Type` (Income/Expense/Transfer), `Payee/Payer`, `Note`, `Amount`, `Currency`, `Status` (Cleared/Pending).
3. **`Budgets & Variance`**:
   - Category budget targets, actual spending, absolute variance, and percentage utilized.
4. **`Account Balances`**:
   - List of all accounts (Checking, Savings, Cash, Credit Card, Investment), starting balance, total debits, total credits, and current cleared balance.
5. **`Recurring Obligations`**:
   - Scheduled bills, subscriptions, salary cycle rules, and next due dates.

### Cell Data Types & Formatting Rules
- **NEVER export numbers or dates as plain unformatted text**:
  - **Currency Amounts**: Store as numeric cell type (`CellType.NUMERIC`) and apply accounting/currency cell formats:
    - Positive: `"$#,##0.00"`
    - Negative: `"($#,##0.00)"` or `"-$#,##0.00"` (red font styling where supported)
  - **Dates**: Store as date/numeric values with date formatting (`"yyyy-mm-dd"` or `"yyyy-mm-dd hh:mm"`).
  - **Formulas**: Include real Excel formulas for summary totals (e.g. `=SUM(H2:H120)`), so manual edits in Excel preserve dynamic recalculation.
  - **Header Styling**: Freeze top rows (`freezePane(0, 1)`), apply bold text, subtle fill color (e.g., `#F1F5F9`), and auto-fit column widths.

---

## 2. CSV & PDF Export Specifications

### CSV Export (RFC 4180 Compliant)
- Delimiter: Standard comma (`,`) or semicolon (`;`) for European locales.
- Text Encoding: UTF-8 with BOM (Byte Order Mark `\uFEFF`) to ensure Excel correctly opens UTF-8 characters (e.g., accent marks, international currency symbols).
- Enclose all fields containing commas, double quotes, or newlines in double quotes, escaping internal quotes as `""`.

### PDF Financial Reports
- Generate formatted, printable PDF financial statements using Android's native `PdfDocument` or Canvas rendering.
- Layout:
  - Header: Application name, statement date range, export timestamp, user profile nickname.
  - Summary Cards: Income, Expense, Net Savings, Budget Health.
  - Tabular Data: Category breakdown with percentage bar visualizations.
  - Footer: Page numbers (`Page X of Y`) and confidentiality note.
  - Margins: Standard A4 / US Letter printable margins (36pt / 0.5 in).

---

## 3. JSON Backup & Restore System

- Canonical JSON backup format capturing full relational data:
  ```json
  {
    "version": 1,
    "exportedAt": "2026-09-29T16:45:00Z",
    "appVersion": "1.0.0",
    "accounts": [...],
    "categories": [...],
    "transactions": [...],
    "budgets": [...],
    "recurringRules": [...]
  }
  ```
- **Integrity Checksums**: Include a SHA-256 hash or authenticated MAC in the backup header to prevent restoring corrupted or tampered files.
- Versioned migrations: The restore engine must support importing older JSON schema versions into newer app schemas.

---

## 4. Import Engine & Duplicate Detection

### Validation Pipeline
1. **File Type Detection**: Detect MIME type and file structure (XLSX, CSV, JSON).
2. **Schema & Header Mapping**:
   - Provide an intelligent column mapper (e.g., matching "Date", "Transaction Date", "Posting Date" to the Date field).
3. **Data Type Validation**:
   - Validate date formats, non-empty amounts, recognized currency codes, and positive/negative conventions.
   - Collect and display all parsing errors to the user before applying imports (dry-run mode).

### Duplicate Detection Strategy
- Every incoming transaction is evaluated against existing records using a deterministic hash key or multi-field identity check:
  - Multi-field identity: Same `date`, same `amountInCents`, same `accountId`, and fuzzy match on `payee`/`note`.
- Actions on Duplicate Match:
  - `SKIP`: Do not import duplicate (default safe option).
  - `OVERWRITE`: Update existing record.
  - `IMPORT_AS_NEW`: Force import with explicit user confirmation.

---

## 5. Verification Checklist

Before releasing export/import functionality:
- [ ] Exported `.xlsx` opens in Microsoft Excel without error or recovery alerts.
- [ ] Exported spreadsheet contains distinct, formatted worksheets (Dashboard, Transactions, Accounts, Budgets).
- [ ] Monetary columns in `.xlsx` are numeric with currency formatting, not strings.
- [ ] Date columns in `.xlsx` are date-formatted cells.
- [ ] CSV file includes UTF-8 BOM and RFC 4180 quote escaping.
- [ ] Full round-trip test passes: Export database -> Wipe database -> Import backup -> Resulting database is identical.
- [ ] Import dry-run detects duplicate transactions accurately.
