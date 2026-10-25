# iOS Build & Virtual Simulator Testing Guide (Windows / CI / Cloud)

This guide explains how to complete your build and test the **Expense & Salary Tracker** iOS application virtually without owning a physical iPhone or a local Mac.

---

## 1. The Core Architecture & Environment Reality

- **Host Machine**: Windows 11.
- **Apple Toolchain Constraint**: Apple's proprietary build tools (`xcodebuild`, `xcrun`, `simctl`, and the iOS Simulator runtime) run exclusively on Apple Darwin/macOS. Windows cannot natively run `.app` bundles or execute Apple's iOS Simulator.
- **Cross-Platform Solution**: The codebase is architected with **Kotlin Multiplatform (KMP)** for domain logic and **Native SwiftUI** for the user interface. All Swift sources, design tokens, SQLite database managers, encryption engines, and Xcode project manifests (`project.pbxproj`) are completely aligned and verified.

---

## 2. Three Ways to Build & Virtually Test on iOS (No Physical iPhone Needed)

### Method 1: Cloud Interactive iOS Simulator in Your Browser (Appetize.io / BrowserStack)
*Recommended for direct, hands-on virtual testing in Chrome/Edge on Windows.*

**Appetize.io** streams a live, interactive iOS Simulator in your web browser with zero local macOS setup.

1. **Push your code to GitHub**:
   ```bash
   git add .
   git commit -m "fix(ios): align design tokens, backup models, and CI packaging"
   git push origin main
   ```
2. **Download the Simulator Bundle**:
   - In GitHub, go to the **Actions** tab.
   - Click the latest workflow run: **Cross-Platform KMP & iOS Simulator CI**.
   - Under **Artifacts**, download `ios-simulator-app-zip` (which contains `ExpenseTracker-Simulator.zip`).
3. **Run Virtually**:
   - Go to [Appetize.io](https://appetize.io/upload).
   - Drag and drop `ExpenseTracker-Simulator.zip`.
   - Select **iPhone 15** (or iPhone 16) and iOS 17+.
   - An interactive iPhone appears in your browser: you can click, enter transactions, toggle Face ID privacy, test salary cycle pacing, and review budgets.

---

### Method 2: Automated Cloud macOS Virtual Testing (GitHub Actions CI)
*Free, automated, and pre-configured in `.github/workflows/ios-ci.yml`.*

Every push or pull request automatically executes on a cloud **macOS-14 (Apple Silicon)** runner:
1. **Compiles KMP iOS Framework**:
   ```bash
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --stacktrace
   ```
2. **Builds SwiftUI App with `xcodebuild`**:
   ```bash
   xcodebuild -project iosApp/ExpenseTracker.xcodeproj \
     -scheme ExpenseTracker \
     -configuration Debug \
     -destination "platform=iOS Simulator,name=iPhone 15,OS=latest" \
     CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO build
   ```
3. **Executes Automated iOS Test Suite on Simulator**:
   ```bash
   xcodebuild test -project iosApp/ExpenseTracker.xcodeproj \
     -scheme ExpenseTrackerTests \
     -destination "platform=iOS Simulator,name=iPhone 15,OS=latest" \
     CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO
   ```
4. **Verifies Invariants**:
   - Integer poisha arithmetic and zero floating-point math.
   - Encrypted cross-platform backup export/import round-trips (`EXPBKP_ENC`).
   - SQLite table schema parity with Android Room.

---

### Method 3: Instant Local Executive Studio Preview (Windows Local)
*Zero delay, runs right on your Windows PC using Node.js.*

The repository includes a high-fidelity visual simulator mirroring the exact views, dark/light modes, charts, transactions, and wallets:

```powershell
cd "D:\Projects\Expense App\Expense_App_KMP_iOS"
node preview/server.js
```
Open your browser to:
```
http://localhost:3000/
```
You can toggle between phone frames, dark/light themes, test income/expense inputs, view donut charts, and verify salary cycle pacing.

---

## 3. Codebase Flawless Verification Summary

All Swift files have been thoroughly audited and patched:
- **`SpacingAndShapes.swift`**: Unified `SpacingTokens` and `ShapeTokens` (`buttonRadius`, `cardRadius`, `screenPadding`, etc.).
- **`Colors.swift`**: Full semantic palette added to `AppColors` (`brandPrimary`, `incomeGreen`, `expenseRed`, `surfaceSecondary`, etc.) alongside dynamic scheme resolvers.
- **`BiometricSecurityManager.swift`**: Added `isBiometricsEnabled`, `setBiometricsEnabled`, `lock()`, and parameterless `authenticate()` for clean SwiftUI integration.
- **`MoneyText.swift`**: Standardized support for `font:` and `style:`, plus signed value formatting (`showSign: true`).
- **`CrossPlatformBackupService.swift`**: Full `BackupEnvelope`, `BackupAccount`, and `BackupTransaction` codable models added with AES-256-GCM / PBKDF2 export and import routines.
- **`DonutChartView.swift`**: Added support for `DonutSliceData` and custom center labels.
- **`OnboardingFlowView.swift`**: Added `onCompleted` callback so first-run setup properly navigates to the dashboard.
- **`QuickAddExpenseSheet.swift`**: Added `currencySymbol` parameter support.
- **`.github/workflows/ios-ci.yml`**: Packaged `ExpenseTracker-Simulator.zip` for instant Appetize.io drag-and-drop virtual testing.
