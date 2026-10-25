# Expense & Salary Tracker — Security & Privacy Architecture

## 1. Threat Model & Protected Assets

### Protected Assets
1. **Financial Records**: Transaction history, minor-unit amounts, timestamps, accounts, categories, custom fields, and notes.
2. **Budget & Salary Information**: Salary cycle dates, pay schedules, budget caps, and savings goals.
3. **Application Access**: Access to visual financial screens, balances, and operational settings.
4. **Exported & Portable Backups**: Offloaded `.enc.json` / binary backup containers containing the complete financial graph.
5. **Cryptographic Material**: Application passcodes, PBKDF2 verifiers, salts, Keystore AES keys, and derived encryption keys.

### Threat Model & Adversary Profiles
* **Casual / Opportunistic Snooper**: Someone who picks up an unlocked phone or views the phone screen over the user's shoulder. Addressed by Biometric/Passcode application lock, auto-lock timeout, grace period controls, and `FLAG_SECURE` window protections.
* **Malicious App on Same Device**: Another non-root application attempting to read app storage, sniff network traffic, or intercept exports. Addressed by Android app sandboxing, internal storage, encrypted preferences, private FileProvider cache paths, and an offline-only architecture with zero network permissions (`android.permission.INTERNET` absent).
* **Backup Interception / Cloud Storage Compromise**: When a user exports their encrypted backup to Google Drive, email, or a USB drive, an adversary gaining access to the file must not be able to decrypt it without the user's passphrase. Addressed by self-describing PBKDF2-HMAC-SHA256 (100,000 iterations) with a 16-byte cryptographically secure random salt, 256-bit AES-GCM encryption with a 12-byte unique nonce, and Authenticated Additional Data (AAD) header binding.
* **Database Tampering & Restore Injection**: An attacker modifying a backup to inject invalid relational references, negative/malformed amounts, or corrupted schema. Addressed by comprehensive pre-restore validation (schema, JSON, ID, enum, and date integrity) and atomic SQLite/Room transaction execution (`withTransaction`).

### What the Application Does NOT Protect Against
* **Compromised OS / Rooted Device**: If an adversary has root access, kernel-level compromises, or physical memory extraction while the device is in an authenticated BFU/AFU state, they can inspect process memory, hook cryptographic APIs, or install keyloggers.
* **Hardware-Level Keylogger or Compromised Keyboard**: Third-party keyboards logging user keystrokes when typing passcodes or passphrases.
* **Physical Coercion / Rubber-Hose Cryptanalysis**: Forcing the user to provide their fingerprint or passcode.
* **Physical Display Photography**: An external physical camera taking a photograph of the physical OLED/LCD screen.

---

## 2. Passcode Key Derivation Function (KDF)

### Algorithm & Parameters
* **Algorithm**: `PBKDF2WithHmacSHA256`
* **Iterations**: `100,000`
* **Salt**: `16 bytes` (128 bits) generated using `java.security.SecureRandom`
* **Key Length**: `256 bits` (32 bytes)
* **Verifier Storage**: Base64-encoded composite format:
  `PBKDF2WithHmacSHA256:100000:<salt_b64>:<hash_b64>`

### Security Guarantees
1. **No Plaintext Persistence**: Passcodes and PINs are never stored in memory longer than necessary, never logged, and never written to disk in plaintext.
2. **Unique Random Salt**: A fresh, cryptographically secure 16-byte salt is generated for every credential setup and update.
3. **Constant-Time Verification**: Verification utilizes `MessageDigest.isEqual()` on derived byte arrays to prevent timing side-channel attacks.
4. **Credential Invalidation**: Changing the passcode immediately generates a new random salt and derives a new verifier, atomically overwriting the previous verifier in encrypted preferences.
5. **Safe Failure**: Any malformed, truncated, or tampered verifier string results in safe authentication failure.

### Benchmark & Rationale
* **Selected Iteration Count**: `100,000`
* **Benchmark Environment**: Modern Android Runtime (ART) / ARM64 & x86_64 host JVM.
* **Derivation Time**: ~30ms to 120ms on typical mid-to-high tier mobile processors.
* **Rationale**: 100,000 iterations provides a robust defense against offline dictionary and brute-force attacks against short PINs and passcodes without introducing noticeable UI latency (<150ms) during user unlock.

---

## 3. Biometric Security Architecture

* **Standard AndroidX Biometrics**: Integrated via `androidx.biometric.BiometricPrompt` with `BIOMETRIC_STRONG` and `DEVICE_CREDENTIAL` authenticators.
* **No Template Storage**: The application never accesses, stores, or processes raw biometric data, fingerprint images, or facial geometry. Authentication decisions are delegated entirely to the Android OS and TEE/StrongBox.
* **Comprehensive Error Handling**:
  - Handles `ERROR_LOCKOUT` and `ERROR_LOCKOUT_PERMANENT` gracefully by falling back to passcode.
  - Handles `ERROR_NO_BIOMETRICS` and `ERROR_HW_UNAVAILABLE` by directing users to passcode or system enrollment.
  - User cancellation returns immediately to the passcode entry screen without exposing any financial state.
* **Zero Lifecycle Flash**: The lock screen state is resolved synchronously upon initial composition from encrypted storage, preventing any single-frame render of financial data before authentication completes.

---

## 4. Android Keystore & AES-256-GCM Storage

* **Keystore Alias**: `expense_app_master_key`
* **Provider**: `AndroidKeyStore`
* **Cipher Specification**: `AES/GCM/NoPadding` with 256-bit key size.
* **Key Storage**: Keys are generated within the hardware-backed keystore (TEE or StrongBox when available on supported hardware) with `PURPOSE_ENCRYPT` and `PURPOSE_DECRYPT`.
* **Application Use**: Used for encrypting sensitive local preferences (e.g., app lock settings, passcodes, and security configuration).
* **Isolation from Portable Backups**: The Android Keystore key is strictly device-bound and hardware-bound. It is **never** used for exportable backups to ensure cross-device portability.

---

## 5. Encrypted Backup Container & Portability

### Construction
```text
User Passphrase
       ↓
Random Salt (16 bytes, SecureRandom)
       ↓
PBKDF2WithHmacSHA256 (100,000 iterations, 256-bit key)
       ↓
AES-256-GCM (12-byte unique IV/Nonce)
       ↓
Header Bound via AAD (Magic, Version, KDF, Iterations, Salt, IV)
       ↓
Authenticated Portable Backup Container (.enc.json)
```

### Self-Describing Container Format
The backup file begins with an explicit, self-describing binary header:

| Field | Size | Description |
|---|---|---|
| Magic Identifier | 10 bytes | ASCII `EXPBKP_ENC` |
| Container Version | 4 bytes (Int) | Version number (`1`) |
| Cipher Algorithm | Var (Len + UTF-8) | `AES-256-GCM` |
| KDF Algorithm | Var (Len + UTF-8) | `PBKDF2WithHmacSHA256` |
| KDF Iterations | 4 bytes (Int) | `100000` |
| Key Length | 4 bytes (Int) | `256` |
| Salt Length + Salt | 4 bytes + 16 bytes | Cryptographic random salt |
| Nonce Length + Nonce | 4 bytes + 12 bytes | Cryptographic random IV |
| Ciphertext Length + Payload | 4 bytes + N bytes | AES-GCM ciphertext + 16-byte authentication tag |

### Portability Guarantees
* **Independent of Hardware & Device**: Contains all non-secret metadata necessary to re-derive the 256-bit encryption key on any other device, OS, or architecture running the app using only the user's passphrase.
* **Authenticated Additional Data (AAD)**: All header metadata (magic, version, algorithms, iterations, salt, and IV) is fed into AES-GCM as AAD (`updateAAD`). Any unauthorized modification or tampering with KDF iterations or algorithms results in immediate GCM authentication tag verification failure.
* **Cryptographic Freshness**: Every backup export generates a fresh 16-byte salt and fresh 12-byte IV. Exporting the same database with the same passphrase produces completely distinct ciphertexts.

---

## 6. Restore Atomicity & Validation Pipeline

The restoration sequence strictly guarantees all-or-nothing execution:

```text
Backup File Stream
       ↓
Decrypt Stream (Authenticated via AES-GCM Tag & AAD)
       ↓
Parse JSON Structure
       ↓
Validate Format & Schema Version
       ↓
Validate Data Types, Dates, & MinorUnits
       ↓
Validate Relational References (Accounts, Categories, Rules)
       ↓
Present Summary Preview to User
       ↓
User Explicit Confirmation (Merge vs Wipe & Replace)
       ↓
Atomic SQLite Transaction (Room database.withTransaction { ... })
       ↓
Post-Restore State & UI Recalculation
```

### Atomicity Guarantees
* Restoration is executed inside an atomic Room transaction (`database.withTransaction`).
* If any validation error, SQL constraint failure, or I/O error occurs at any point during restoration, the entire transaction rolls back completely.
* The existing database remains 100% intact and untouched if an invalid or corrupted file is supplied.

---

## 7. Window Security (FLAG_SECURE)

* **Implementation**: Dynamically toggled via `window.setFlags(FLAG_SECURE, FLAG_SECURE)` and `window.clearFlags(FLAG_SECURE)` based on user security preferences.
* **Protections**:
  - Prevents system and third-party screenshot captures.
  - Prevents screen recording of financial details.
  - Blanks or obfuscates the app thumbnail preview in Android's Recent Apps / App Switcher overview.
* **Known Android & OEM Limitations**:
  - `FLAG_SECURE` is enforced by the Android SurfaceFlinger/Window Manager. Custom OEM ROMs or modified Android builds may disregard or bypass this flag.
  - Screen projection via USB/DisplayPort or hardware capture cards may or may not respect `FLAG_SECURE` depending on OEM implementation.
  - Physical photography of the device display cannot be prevented by software.

---

## 8. Privacy Model & Zero-Telemetry Architecture

* **100% Offline-First**: The application requires no internet connectivity to operate.
* **Zero Network Permissions**: The `android.permission.INTERNET` permission is completely absent from `AndroidManifest.xml`. The Android OS prevents the app from opening any TCP/UDP sockets or making any HTTP/HTTPS requests.
* **No Analytics or Telemetry**: No third-party analytics (Google Analytics, Firebase, Mixpanel, etc.) or tracking SDKs are bundled.
* **No Crash Reporting Exfiltration**: No remote crash-reporting tools (Crashlytics, Sentry, Bugsnag) that could inadvertently transmit sensitive financial records or account names.
* **Permission Minimization**:
  - No `READ_SMS` or `RECEIVE_SMS` (no financial transaction scraping).
  - No `BIND_ACCESSIBILITY_SERVICE` (no background screen reading).
  - No `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`.
  - No `READ_CONTACTS`.
  - Only local permissions are declared: `USE_BIOMETRIC`, `USE_FINGERPRINT`, `POST_NOTIFICATIONS` (local reminders), `RECEIVE_BOOT_COMPLETED` (rescheduling recurring transaction alarms).
* **Storage & File Sharing**:
  - Internal application database is stored in private internal app storage (`/data/data/com.expenseapp.salarytracker/databases/`).
  - `FileProvider` (`com.expenseapp.salarytracker.fileprovider`) exposes strictly `<cache-path name="exports" path="exports/" />`. The internal database, preferences, and root filesystem are strictly protected and never exposed.
  - Storage Access Framework (SAF) is used for direct user-controlled exports and imports.
