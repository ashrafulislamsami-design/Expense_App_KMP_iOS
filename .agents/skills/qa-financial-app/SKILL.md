---
name: qa-financial-app
description: >-
  Testing strategy and quality assurance protocols covering unit tests, integration tests,
  Compose UI tests, financial precision tests, Room migration validation, import/export fidelity,
  lifecycle resilience, process death, and device reboot scenarios. Use when writing tests,
  verifying test coverage, or validating app reliability.
---

# QA & Financial App Testing

This skill outlines the mandatory testing framework for the financial application. Financial software requires zero tolerance for regression. All business rules, boundary conditions, edge cases, and storage migrations must be rigorously covered by automated tests.

---

## 1. Test Automation Hierarchy

```text
               / \
              / UI\         Compose UI Tests (Screen interactions, navigation, accessibility)
             /-----\
            / Integ \       Integration Tests (Room DAOs, Repository + Room, Import/Export)
           /---------\
          / Unit Tests\     Unit Tests (Financial calculations, Salary cycles, ViewModels, Parsers)
         /-------------\
```

### Frameworks & Libraries
- **Unit Testing**: JUnit 4 / JUnit 5, Kotlin Test, Truth / AssertJ assertions.
- **Coroutines & Flow**: `kotlinx-coroutines-test` (`StandardTestDispatcher`, `runTest`), Turbine (`app.cash.turbine:turbine`) for testing `StateFlow` emissions.
- **Mocking**: `io.mockk:mockk` for repository mocking where domain isolation is tested; prefer fakes/in-memory databases over heavy mocks for data layers.
- **Database & Migration**: `androidx.room:room-testing` (`MigrationTestHelper`).
- **Compose UI Testing**: `androidx.compose.ui:ui-test-junit4` (`createComposeRule`).

---

## 2. Mandatory Financial & Domain Test Suites

### A. Exact Money & Arithmetic Tests
- **No loss of precision**:
  - Test recurring additions of fractional minor units over large counts (e.g. adding $0.01 10,000 times must equal exactly $100.00, not $99.999999999994).
  - Test currency division and rounding modes (e.g. split bill among 3 people: $100 split 3 ways yields $33.34, $33.33, $33.33 summing exactly to $100.00).

### B. Transfer & Refund Net Impact Tests
- **Transfer Invariance**:
  - Verify that adding a transfer of $500 between Checking and Savings decreases Checking by $500, increases Savings by $500, but has zero effect on the Monthly Expense summary and zero effect on the Budget consumption.
- **Refund Net Offset**:
  - Verify that a $50 refund on a $200 Electronics expense updates net Electronics spending to $150 and does not appear in Gross Income.

### C. Salary Cycles, Leap Years & Month Boundaries
- **Leap Year Boundary Tests**:
  - February 29th recurrences in leap year (e.g. 2024, 2028) correctly project to February 28th in non-leap year (e.g. 2025, 2026).
- **Month-End Boundary Tests**:
  - Salary paid on 31st of each month generates correct dates for 30-day months (April 30, June 30, Sept 30, Nov 30) and February (Feb 28/29).
  - Bi-weekly pay cycle generator correctly handles transitions across calendar year ends.
- **Custom Pay Periods**:
  - Pay cycle from 25th of month $M$ to 24th of month $M+1$: verify transactions on the 24th and 25th are mapped to the correct budget cycle.

### D. Recurring Transaction Schedules
- Recurring transactions with auto-post triggers:
  - Verify missed occurrences while the device was turned off are caught up sequentially without duplicate entries.
  - Verify recurrence termination rules ("End after N occurrences", "End by date").

---

## 3. Database Migration & Persistence Tests

- Every Room migration from version $N$ to $N+1$ must have an automated test using `MigrationTestHelper`:
  ```kotlin
  @Test
  fun migrate1To2_containsAllData() {
      helper.createDatabase(TEST_DB, 1).apply {
          execSQL("INSERT INTO transactions VALUES (1, 'Groceries', 5000, 1717200000)")
          close()
      }
      val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
      // Assert schema and data integrity
  }
  ```
- Verify foreign key cascades and integrity constraints.

---

## 4. Import / Export Round-Trip Tests

- **Round-Trip Fidelity Test**:
  1. Populate in-memory database with representative sample: multiple currencies, transfers, refunds, recurring rules, tags, and accounts.
  2. Execute XLSX, CSV, and JSON export.
  3. Clear database completely.
  4. Restore from the export file.
  5. Compare restored database state against original snapshot. Assert 100% data parity.
- **Corrupt / Malformed Import Tests**:
  - Feed truncated JSON, invalid CSV delimiters, missing columns, and illegal date strings.
  - Assert that import aborts safely with clear user-facing error messages and zero partial corruption of the database.

---

## 5. Android Lifecycle, Reboot & Background Tests

- **Process Death & SavedStateHandle**:
  - Test screen state restoration when Android OS kills the app in background to reclaim memory.
- **App Restart & Device Reboot Scenarios**:
  - Test `BOOT_COMPLETED` receiver or `WorkManager` rescheduling: verify scheduled bill notifications and recurring transaction jobs are restored after a simulated device reboot.
- **Biometric / Lock State Persistence**:
  - Verify that backgrounding the app locks the screen after timeout, requiring re-authentication.

---

## 6. QA Execution Checklist

Run before any release build:
- [ ] `./gradlew test` passes 100% of unit and financial safety tests.
- [ ] Room migration tests pass across all version transitions.
- [ ] Round-trip export and import tests succeed with zero discrepancies.
- [ ] Edge case test suite passes: leap years, month boundaries, salary paydays, transfers, refunds.
- [ ] Compose UI tests verify screen render and basic navigation flows.
