# Expense App Agent Guidelines & Core Principles

This project adheres strictly to the project skills defined in `.agents/skills/`.

## Active Project Skills

1. **`android-finance-architecture`** (`.agents/skills/android-finance-architecture/SKILL.md`)
   - Kotlin, Jetpack Compose, Material 3, MVVM, Clean Architecture, Repository Pattern, Room, Coroutines, Flow/StateFlow, DI, and Android Lifecycle.
   - Business logic must never exist in UI code.
   - Financial calculations must be isolated in dedicated domain services.
   - Room must use proper migrations.
   - Reactive, state-driven UI with dark mode and accessibility support.

2. **`financial-calculation-safety`** (`.agents/skills/financial-calculation-safety/SKILL.md`)
   - Exact money calculations using `Long` minor units or `BigDecimal`. Never use `Double` or `Float` for money.
   - Transfers must never count as expenses or income.
   - Deterministic calculations; retroactive edits must recalculate all affected balances and cycles.
   - Correct handling of leap years, month boundaries, and salary cycles.

3. **`privacy-security-audit`** (`.agents/skills/privacy-security-audit/SKILL.md`)
   - 100% offline-first, privacy-by-design.
   - No analytics, no ads, no telemetry, no hidden network calls.
   - No banking credentials storage, no SMS scraping, no Accessibility Service abuse.
   - Android Keystore, biometric lock, secure window (`FLAG_SECURE`), and encrypted backups.

4. **`xlsx-reporting`** (`.agents/skills/xlsx-reporting/SKILL.md`)
   - Real Microsoft Excel OpenXML `.xlsx` exports with multiple worksheets.
   - CSV export (RFC 4180 with UTF-8 BOM), PDF reports, and canonical JSON backup.
   - Data import validation with duplicate detection.
   - Numeric and date formatting preservation.

5. **`qa-financial-app`** (`.agents/skills/qa-financial-app/SKILL.md`)
   - Comprehensive test automation: unit tests, financial edge cases, Room migrations, export/import round-trips.
   - Testing for salary cycles, leap years, month boundaries, transfers, refunds, and lifecycle/reboot scenarios.

6. **`git-release-engineering`** (`.agents/skills/git-release-engineering/SKILL.md`)
   - Small, logical atomic commits using Conventional Commits.
   - Strict secret prevention, clean `.gitignore`, SemVer versioning, and Proguard/R8 optimization.
