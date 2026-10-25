---
name: android-finance-architecture
description: >-
  Architectural guidelines and standards for building the Android finance application using
  Kotlin, Jetpack Compose, Material 3, MVVM, Clean Architecture, Repository pattern, Room,
  Coroutines, Flow, StateFlow, Dependency Injection, and modern Android lifecycle APIs.
  Use when designing or implementing application structure, presentation layers, database layers,
  or component communication.
---

# Android Finance Architecture

This skill defines the architectural blueprint and non-negotiable coding standards for the Android Finance Application. It enforces Clean Architecture, MVVM, reactive unidirectional data flow (UDF), and modern Android development best practices.

---

## 1. Core Architectural Principles

1. **Separation of Concerns & Clean Architecture**:
   - **UI Layer (`presentation`)**: Jetpack Compose UI, ViewModels, and UI state definitions. Business logic must NEVER exist in UI code.
   - **Domain Layer (`domain`)**: Pure Kotlin domain models, business rules, and dedicated financial use cases/services. Zero Android framework dependencies (no `Context`, no UI imports).
   - **Data Layer (`data`)**: Room database, DAOs, entities, repositories, data sources, and converters. Repositories expose immutable domain models to the domain/presentation layers.

2. **Unidirectional Data Flow (UDF)**:
   - UI emits user actions/events (`Event` / `Intent`) to the ViewModel.
   - ViewModel processes events, invokes domain use cases, and updates an immutable `StateFlow<UiState>`.
   - UI observes `UiState` via `collectAsStateWithLifecycle()` and renders deterministically.

3. **Domain Calculation Isolation**:
   - Financial math and rule evaluation MUST be executed in dedicated domain services (e.g., `FinancialCalculationEngine`, `SalaryCycleCalculator`).
   - UI components and ViewModels only display pre-computed values or request calculations via domain use cases.

---

## 2. Technology Stack Standards

### Kotlin & Coroutines
- Use Kotlin 2.0+ idioms: data classes, sealed interfaces for UI states and events, value classes for type safety.
- Concurrency via Coroutines & Flow:
  - Repositories return `Flow<T>` for observable data and `suspend` functions for one-shot operations.
  - Dispatchers: `Dispatchers.IO` for database/file IO, `Dispatchers.Default` for heavy financial calculations, `Dispatchers.Main.immediate` for UI state updates.
  - Never use `GlobalScope`. Use `viewModelScope` in ViewModels and structured concurrency in repositories.

### Jetpack Compose & Material 3
- Fully declarative UI built exclusively with Jetpack Compose and Material 3 (`androidx.compose.material3`).
- Dynamic theming support with dedicated light and dark color schemes (`ColorScheme`), tonal elevation, and Material 3 typography tokens.
- Composable guidelines:
  - Keep Composables stateless whenever possible through state hoisting.
  - Pass events up via lambda parameters: `onEvent: (TransactionUiEvent) -> Unit`.
  - Use `@Stable` and `@Immutable` annotations on UI state models to optimize recomposition performance.
  - Support accessibility: minimum 48x48dp touch targets, TalkBack `contentDescription` on non-text elements, dynamic font scaling support.
  - Support edge-to-edge display using `WindowInsets` and `Scaffold`.

### ViewModel & Lifecycle
- Every screen is backed by an `androidx.lifecycle.ViewModel`.
- ViewModel manages state using `MutableStateFlow` internally and exposes `StateFlow` externally:
  ```kotlin
  private val _uiState = MutableStateFlow<ExpenseUiState>(ExpenseUiState.Loading)
  val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()
  ```
- Use `SavedStateHandle` to preserve critical navigation arguments and survive process death.
- In Compose, collect state safely:
  ```kotlin
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  ```

### Room Database & Local Persistence
- Room is the single source of truth for app data.
- Database rules:
  - Entities represent raw persistence schemas; map them to Domain models via mapper extensions before exposing them to the domain/UI layer.
  - Foreign keys must declare explicit `onDelete` behaviors (`CASCADE`, `RESTRICT`).
  - Use Room `TypeConverters` for custom types (e.g., `LocalDate`, `Instant`, `CurrencyUnit`).
  - All migrations must be explicitly declared and tested (`AutoMigrationSpec` or manual `Migration`). Set `exportSchema = true` in `@Database`.
  - Database queries that return streams must return `Flow<List<T>>` to trigger reactive UI updates on data changes.

### Dependency Injection
- Organize dependencies using a robust DI container (Hilt or clean dependency injection modules).
- Scoping rules:
  - `@Singleton`: Database instances, Room DAOs, DataStores, global calculation engines.
  - `@ViewModelScoped`: Repositories and use cases injected into ViewModels.
- Repositories must be accessed via interfaces (`TransactionRepository`) to facilitate mocking and unit testing.

---

## 3. Package & Module Structure

```text
com.expenseapp/
├── core/
│   ├── designsystem/   # Material 3 theme, Color, Type, Shape, reusable UI components
│   ├── common/         # Dispatcher annotations, Result wrapper, extensions
│   └── database/       # Room database instance, converters, migrations
├── domain/
│   ├── model/          # Clean domain models (Transaction, Account, Budget, Category)
│   ├── repository/     # Repository interfaces
│   └── service/        # Pure domain services (FinancialCalculationEngine)
├── data/
│   ├── entity/         # Room entities
│   ├── dao/            # Room DAOs
│   ├── mapper/         # Entity <-> Domain mappers
│   └── repository/     # Repository implementations
└── presentation/
    ├── navigation/     # NavHost and screen destinations
    ├── dashboard/      # Dashboard screen Composable, ViewModel, UiState
    ├── transactions/   # Transaction list/edit Composable, ViewModel, UiState
    ├── budgets/        # Budgeting Composable, ViewModel, UiState
    └── settings/       # Settings, export/import, security controls
```

---

## 4. Verification & Architecture Review Checklist

Before approving any architectural change or component:
- [ ] No UI code calls Room DAOs directly; all calls go through ViewModel -> Use Case -> Repository.
- [ ] No Composable contains financial computation logic (e.g. sum, tax, conversion, balance calculation).
- [ ] UI states are modeled as sealed interfaces or immutable data classes.
- [ ] Database schema changes include a version increment and migration script.
- [ ] Light and dark themes render cleanly without unreadable contrast.
- [ ] Edge-to-edge window insets are handled correctly on all screens.
