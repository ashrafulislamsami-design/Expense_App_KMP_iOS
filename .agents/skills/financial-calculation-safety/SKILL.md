---
name: financial-calculation-safety
description: >-
  Strict standards and algorithms for financial calculations, money representation, salary cycle
  schedules, date boundary handling, transfer invariants, refunds, budgets, recurring transactions,
  and retroactive recalculations. Use when writing, modifying, or testing any financial math,
  currency arithmetic, or date-based accounting logic.
---

# Financial Calculation Safety

This skill establishes absolute mathematical and logical rules for all financial calculations in the application. Precision and deterministic consistency are paramount: financial discrepancies directly undermine user trust.

---

## 1. Absolute Financial Rules & Invariants

### Rule 1: Zero Floating-Point Arithmetic for Money
- **STRICT PROHIBITION**: Never use `Float` or `Double` to store, compute, or format monetary amounts.
- **Allowed Monetary Representations**:
  1. **Minor Units (Cents/Pence/Micros) as `Long`**: Store and calculate amounts in the currency's smallest sub-unit (e.g., $10.50 is stored as `1050L` cents).
  2. **`BigDecimal` with Explicit Precision & Rounding**:
     - Always instantiate `BigDecimal` from `String` or `Long`, never from `Double` (e.g. `BigDecimal("10.50")` or `BigDecimal.valueOf(1050, 2)`).
     - Always specify a `MathContext` or explicit `RoundingMode` (default: `RoundingMode.HALF_EVEN`, also known as Banker's Rounding, or `RoundingMode.HALF_UP` if designated by currency conventions).
- Value Class Wrapper Example:
  ```kotlin
  @JvmInline
  value class MinorUnits(val value: Long) {
      operator fun plus(other: MinorUnits): MinorUnits = MinorUnits(value + other.value)
      operator fun minus(other: MinorUnits): MinorUnits = MinorUnits(value - other.value)
      operator fun times(scalar: Long): MinorUnits = MinorUnits(value * scalar)
      fun toBigDecimal(decimals: Int = 2): BigDecimal = BigDecimal.valueOf(value, decimals)
  }
  ```

### Rule 2: Invariant on Transfers (Double-Entry Principle)
- A **Transfer** moves funds from Source Account $A$ to Destination Account $B$.
- **Transfers must NEVER count as an Expense or as Income**:
  - Net global expense delta = `0`.
  - Net global income delta = `0`.
  - Delta Account $A$ = $-amount$.
  - Delta Account $B$ = $+amount$.
  - Net net-worth change = $0$ (excluding explicit transfer fee transactions, which must be tracked as a separate attached expense).
- Budget consumption engines MUST exclude transfer transactions from expense budget totals.

### Rule 3: Refunds and Adjustments
- A **Refund** is a negative expense or a direct credit offsetting a specific expense category or original transaction.
- Refunds MUST NOT be categorized as general "Income":
  - Categorizing a refund as income falsely inflates gross income and skews expense reporting.
  - A refund decreases the spent amount in the associated category/budget.

### Rule 4: Determinism & Audit Recalculation
- All calculations must be 100% deterministic functions: given identical inputs (transactions, dates, rates), the output is bit-for-bit identical.
- **Retroactive Edits / History Modification**:
  - When an older transaction is added, updated, or deleted, the system MUST recalculate:
    1. The account balance(s) associated with that transaction.
    2. All affected budget cycle totals (historical and current).
    3. Recurring scheduled projection balances.
    4. Goal progress snapshots.
  - Snapshots can be cached for performance, but the cache invalidation strategy must cascade from the modified transaction's date forward.

---

## 2. Date Calculations & Cycle Scheduling

### Modern Time API Only
- Use `java.time` (`LocalDate`, `YearMonth`, `Period`, `ZonedDateTime`) exclusively.
- Do NOT use `java.util.Date`, `java.util.Calendar`, or epoch millisecond arithmetic for date math.

### Salary Cycle & Pay Period Handling
- Salary cycles vary significantly and require accurate boundary calculations:
  - **Monthly (Specific Day of Month)**: E.g., 25th of every month.
    - If the day does not exist in a given month (e.g. 31st in February or April), handle boundary via `month.length(isLeapYear)`:
      ```kotlin
      val targetDay = minOf(configuredDay, yearMonth.lengthOfMonth())
      ```
    - Weekend/Holiday adjustments (if configured): e.g. "Previous business day if on weekend" or "Next business day".
  - **Bi-Weekly / Every Two Weeks**:
    - Calculated as `anchorDate.plusWeeks(2 * n)`.
  - **Semi-Monthly**:
    - Typically 15th and last day of month, or 1st and 15th.
- **Leap Years**:
  - February 29th recurrences must cleanly transition to February 28th in non-leap years.

### Budget Cycles & Windows
- Budget periods must strictly align with the user's selected cycle:
  - Calendar Month: `YearMonth.atDay(1)` to `YearMonth.atEndOfMonth()`.
  - Paycheck-to-Paycheck: From salary date $S_n$ up to $S_{n+1} - 1 \text{ day}$.
  - Custom Rolling Window: Explicit `[startDate, endDate]` inclusive range.

---

## 3. Recurring Transactions & Scheduling Engine

- Every recurring template defines:
  - Frequency rule: Daily, Weekly, Bi-Weekly, Monthly, Yearly.
  - Recurrence interval: Every $k$ periods.
  - Execution rule: Auto-post vs. Confirmation required.
  - End conditions: Never, After $N$ occurrences, or Until specific end date.
- Occurrence generation must handle timezone shifts and Daylight Saving Time (DST) gracefully by calculating in `LocalDate` and attaching time/timezone only when required for notifications.

---

## 4. Financial Calculations Checklist

Before committing any financial logic, ensure:
- [ ] No `Double` or `Float` is used in any calculation or entity definition.
- [ ] All monetary operations use minor units (`Long`) or `BigDecimal` with explicit rounding.
- [ ] Account transfer tests confirm: `totalExpense(withTransfer) == totalExpense(withoutTransfer)`.
- [ ] Editing a transaction from 3 months ago correctly updates current account balances and historical budget totals.
- [ ] Salary cycle calculator passes leap-year tests (February 29) and month-end tests (31st to 30th/28th).
- [ ] Every single calculation algorithm has corresponding unit test coverage in `qa-financial-app`.
