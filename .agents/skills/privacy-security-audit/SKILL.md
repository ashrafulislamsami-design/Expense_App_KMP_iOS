---
name: privacy-security-audit
description: >-
  Privacy-by-design enforcement, biometric lock, Android Keystore cryptography, permission minimization,
  secure local storage, encrypted backup creation, and dependency security audits. Use when configuring
  security controls, reviewing permissions, managing cryptography, or validating privacy compliance.
---

# Privacy & Security Audit

This skill enforces a zero-trust, privacy-first posture for the financial application. Financial data is deeply personal; the application is designed to be 100% offline-first, private, and secured against unauthorized local or remote access.

---

## 1. Absolute Privacy Prohibitions

The following are strictly banned from the codebase, manifest, and dependencies:

1. **NO Analytics SDKs**:
   - No Google Firebase Analytics, Mixpanel, Amplitude, Segment, Flurry, etc.
2. **NO Advertising SDKs**:
   - No Google AdMob, Unity Ads, AppLovin, Facebook Audience Network, etc.
3. **NO Telemetry / Tracking**:
   - No Crashlytics, Sentry, Bugsnag, or background error telemetry sending stack traces across the network.
   - All errors must be logged locally to internal debug buffers only (inspectable by the user upon request).
4. **NO Hidden Network Calls**:
   - No automatic background pings, update checkers, or CDN asset fetching.
   - The app must function 100% offline. `android.permission.INTERNET` should NOT be declared in `AndroidManifest.xml` unless an optional, explicitly user-initiated feature (like currency exchange rate download) is enabled by the user with clear consent.
5. **NO Banking Credentials or Sensitive Authentication Ingestion**:
   - Do NOT store or prompt for bank account passwords, PINs, card CVVs, or full 16-digit PANs.
   - Only user-defined nicknames or masked references (e.g. `Checking (...4821)`) are permitted.
6. **NO SMS Reading (`READ_SMS` / `RECEIVE_SMS`)**:
   - No automated SMS scraping. SMS scrapers are frequent security vulnerabilities and permission liabilities.
7. **NO Accessibility Service Abuse**:
   - Do not request `BIND_ACCESSIBILITY_SERVICE` to scrape financial apps or screen content.

---

## 2. Android Permissions Policy

- **Principle of Least Privilege**:
  - Request strictly the minimum set of permissions required for user-facing features.
  - Runtime permissions must be requested just-in-time with clear contextual rationale.
  - Allowed Permissions:
    - `USE_BIOMETRIC` (or `android.permission.USE_BIOMETRIC` / `USE_FINGERPRINT`) for app unlock.
    - `POST_NOTIFICATIONS` (Android 13+) for scheduled bill reminders and budget alerts only.
    - System Storage Access via Storage Access Framework (SAF) (`ActivityResultContracts.CreateDocument` / `OpenDocument`) — do NOT request broad `MANAGE_EXTERNAL_STORAGE` or legacy `READ_EXTERNAL_STORAGE`.

---

## 3. Cryptography & Secure Local Storage

### Android Keystore Integration
- Master keys must be generated inside the hardware-backed **Android Keystore** (`AndroidKeyStore` provider).
- Key Specifications:
  ```kotlin
  val keyGenSpec = KeyGenParameterSpec.Builder(
      "FinanceAppMasterKey",
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
  )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256)
      .setUserAuthenticationRequired(false) // Or true if tied directly to biometric authentication
      .build()
  ```
- Use AES-256-GCM for authenticated encryption of sensitive fields and export files.

### Biometric Lock
- Implement `androidx.biometric.BiometricPrompt` with `BiometricManager.Authenticators.BIOMETRIC_STRONG`.
- Features:
  - App launch lock.
  - Automatic lock on background / app minimized after configurable timeout (Immediate, 30s, 1m, 5m).
  - Secure Window Protection: Set `FLAG_SECURE` on Activity windows to prevent screen caching in the Android Recent Apps switcher and prevent unapproved screen captures:
    ```kotlin
    window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
    )
    ```

### Encrypted Backups
- Encrypted export files must use industry-standard encryption:
  - Key derivation: PBKDF2WithHmacSHA256 (minimum 100,000 iterations) or Argon2id from a user-provided passphrase with random salt.
  - Encryption: AES-256-GCM with a unique 12-byte initialization vector (IV) per export.
  - Encrypted backup payload structure:
    `[SALT (16 bytes)] [IV (12 bytes)] [CIPHERTEXT + AUTH_TAG]`.

---

## 4. Dependency Review & Audit Protocol

Before adding any third-party dependency to `build.gradle.kts`:
1. Check repository provenance: Official AndroidX, Jetpack, KotlinX, or well-established Apache-2.0 / MIT libraries only.
2. Verify dependency tree: Ensure transitive dependencies do not include network libraries (OkHttp/Retrofit) or analytics unless explicitly audited.
3. Keep dependency count minimal. Prefer standard platform APIs over heavy third-party bundles.

---

## 5. Security & Privacy Audit Checklist

Before releasing or verifying features:
- [ ] `AndroidManifest.xml` does not contain `READ_SMS`, `RECEIVE_SMS`, or `MANAGE_EXTERNAL_STORAGE`.
- [ ] No analytics or telemetry dependencies are present in `build.gradle.kts`.
- [ ] `FLAG_SECURE` is active when privacy mode or app lock is enabled.
- [ ] Biometric prompt correctly falls back to device PIN/pattern/password if biometric hardware is unavailable or not enrolled.
- [ ] Export files can be encrypted with a password and cannot be read without that password.
- [ ] All database data remains strictly on the local device.
