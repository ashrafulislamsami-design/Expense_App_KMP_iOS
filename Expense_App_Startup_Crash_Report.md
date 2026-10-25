# Expense App Startup Crash Report

**Date:** 2026-10-07
**App:** Salary Tracker (`com.expenseapp.salarytracker`, debug build 1.0.0)
**Device:** OPPO Reno 10 Pro+ 5G (CPH2521), Android 16 (API 36)

The app closed on launch because of two bugs in its own code. Both are still in the 7 Oct build (`app-debug.apk`) and are patched in `app-debug-FIXED.apk`. A third crash came from my repackaging, not from the app, and is fixed in v3 and later builds.

---

## Findings at a glance

The crashes hit in order: each fix let the app get one stage further and expose the next.

| # | Where it crashes | Exception | Cause | Status |
|---|---|---|---|---|
| 1 | Application constructor, before any screen | `NullPointerException` in `StartupDiagnostics.install()` | Crash handler set up before Android attaches the Context | App bug; still in 7 Oct source; patched in APK |
| 2 | Bottom navigation bar, first frame of MainScreen | `NullPointerException` on `Screen.getRoute()` (MainScreen.kt:124) | `bottomNavScreens` built during a class-init cycle, so `Dashboard` is null | App bug; still in 7 Oct source; patched in APK |
| 3 | First draw of the Home screen | `IncompatibleClassChangeError` on `DrawScope` | My repackaging wrote dex 035 instead of 038 | Not an app bug; fixed in v3 and later builds |

---

## Bug 1: crash handler installed in the Application constructor

This is why the app closed instantly with the icon showing, nothing drawn and no crash report.

- **Where:** the `ExpenseApplication` constructor (Kotlin `init {}`) calls `StartupDiagnostics.install(this)`.
- **What happens:** `install()` calls `context.getApplicationContext()`. In the constructor the Application's base context is still `null`, so `ContextWrapper.getApplicationContext()` throws a `NullPointerException`.
- **Why there's no report:** the exception is thrown inside `install()`, before it sets the uncaught-exception handler that writes `startup_crash_report.txt`. The process dies inside Android's `makeApplication`, before any Activity exists. ColorOS also hides the usual "app keeps stopping" dialog.
- **Scope:** every device and every launch.
- **Fix:** call `install(this)` from `attachBaseContext()`, right after `super.attachBaseContext(base)`.

---

## Bug 2: null bottom-nav entry from a class-init cycle in `Screen`

Once Bug 1 was fixed, the app reached the main screen and crashed drawing the bottom bar, at checkpoint `NAVIGATION_STARTED`.

- **Where:** `Screen.kt`. The sealed class keeps a static list:
  `val bottomNavScreens = listOf(Dashboard, Transactions, Budgets, Goals, Settings)`
- **What happens:**
  1. `MainScreen` touches `Screen.Dashboard` first (its fallback route).
  2. Before `Dashboard` can finish initializing, the JVM initializes its parent class `Screen`.
  3. `Screen`'s initializer builds the list while `Dashboard.INSTANCE` is still `null`, so the list's first entry is `null` for the life of the process.
  4. `MainScreen` maps the list to routes, calls `getRoute()` on `null`, and crashes (`MainScreen.kt:124`).
- **Scope:** every launch. The 7 Oct build still has it, now with five tabs including Goals.
- **Fix:** make `bottomNavScreens` a getter, or `by lazy`, so the list is built on use, after every `Screen` object exists. I scanned every class in the app; `Screen` is the only one with this cycle.

---

## Bug 3: dex format downgrade during repackaging (not an app bug)

This crash came from my patching, not from the app, and does not need a source change.

- **Symptom:** `IncompatibleClassChangeError: Found interface androidx.compose.ui.graphics.drawscope.DrawScope, but class was expected`, on the first Home-screen draw (checkpoint `HOME_STARTED`).
- **Cause:** when I unpacked the APK, the API-level setting was lost. The rebuild then reassembled all 16 dex files in format 035 instead of the original 038. Dex 035 predates Java 8 interface methods, and Compose's `DrawScope` relies on them.
- **Affected builds:** my FIXED v1 and v2.
- **Fix:** v3 and later replace only the two dex files that hold the patched classes, rebuilt at API 26 (dex 038). The other 14 dex files are byte-for-byte identical to the original build.

---

## Required source changes

Make these two edits before the next build; otherwise it will crash the same way as the 7 Oct build.

**ExpenseApplication.kt:** remove `StartupDiagnostics.install(this)` from `init {}` and override `attachBaseContext`:

```kotlin
override fun attachBaseContext(base: Context?) {
    super.attachBaseContext(base)
    StartupDiagnostics.install(this)
    base?.let { StartupDiagnostics.onBaseContextAttached(it) }
}
```

**Screen.kt**, companion object: replace the plain `val` with a getter:

```kotlin
val bottomNavScreens: List<Screen>
    get() = listOf(Dashboard, Transactions, Budgets, Goals, Settings)
```

After rebuilding, uninstall the old app before installing. That also clears any saved crash report, which otherwise sends the app to the recovery screen and skips part of normal startup.

---

## Evidence and verification

Bug 1 was found by decompiling the APK. Bugs 2 and 3 were confirmed by crash reports from the phone (CPH2521, Android 16, API 36).

| Time (UTC) | Build | Last checkpoint | Exception | Finding |
|---|---|---|---|---|
| 4 Oct 09:49:17 | FIXED v2 | HOME_STARTED | `IncompatibleClassChangeError` (DrawScope) | Bug 3 |
| 4 Oct 09:41:33 | FIXED v1, via recovery screen "Try again" | NAVIGATION_STARTED | `NullPointerException`, `Screen.getRoute()` | Bug 2 (repeat) |
| 4 Oct 09:40:48 | FIXED v1 | NAVIGATION_STARTED | `NullPointerException`, `Screen.getRoute()` | Bug 2 |

**Checked in `app-debug-FIXED.apk` (7 Oct build):**

- Both patches present when decompiled: `install(this)` runs only in `attachBaseContext`, and the nav list is built on use.
- Only `classes4.dex` and `classes11.dex` changed, both dex 038 with class counts unchanged (195 and 115).
- APK zip-aligned and signed (v2 + v3), with a generic debug key.
- Also reviewed and cleared: database setup and default data, reminder scheduling (WorkManager), theme, security ViewModel, the new onboarding flow and its route, and the existing-user check.

**Limits:**

- Not run on a device or emulator. The build workspace had no hardware acceleration for an emulator and could not download Robolectric.
- Because of the debug-key signature, the patched APK will not install over a build signed with your key; uninstall first.
