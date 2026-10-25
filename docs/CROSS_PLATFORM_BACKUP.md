# Cross-Platform Backup & Restore Specification

## 1. Overview

This document specifies the data interchange format and cryptographic container standard for the **Expense & Salary Tracker** application. This specification guarantees complete bidirectional interoperability between Android and iOS without relying on third-party cloud servers or proprietary binary formats.

Users can export an unencrypted JSON backup or an AES-256-GCM encrypted backup on Android and restore it seamlessly on iOS, and vice versa.

---

## 2. Plain JSON Schema Specification (Version 1)

Unencrypted backups are exported as standard UTF-8 JSON files with the `.json` extension.

### 2.1 JSON Schema Structure

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-10-25T16:45:00Z",
  "appVersion": "1.0.0",
  "platform": "Android", // or "iOS"
  "currency": "BDT",
  "accounts": [
    {
      "id": "acc-cash-1",
      "name": "Cash",
      "type": "CASH",
      "initialBalanceMinor": 500000,
      "currentBalanceMinor": 350000,
      "icon": "banknote",
      "colorHex": "#10B981"
    },
    {
      "id": "acc-brac-1",
      "name": "BRAC Bank Astha",
      "type": "BANK",
      "initialBalanceMinor": 8500000,
      "currentBalanceMinor": 7200000,
      "icon": "building.columns",
      "colorHex": "#3B82F6"
    },
    {
      "id": "acc-bkash-1",
      "name": "bKash",
      "type": "MOBILE_WALLET",
      "initialBalanceMinor": 1500000,
      "currentBalanceMinor": 1150000,
      "icon": "iphone",
      "colorHex": "#E11D48"
    }
  ],
  "categories": [
    {
      "id": "cat-groceries",
      "name": "Groceries",
      "type": "EXPENSE",
      "icon": "cart",
      "colorHex": "#10B981"
    },
    {
      "id": "cat-salary",
      "name": "Salary",
      "type": "INCOME",
      "icon": "briefcase",
      "colorHex": "#0EA5E9"
    }
  ],
  "transactions": [
    {
      "id": "tx-1",
      "title": "Shwapno Supermarket",
      "categoryId": "cat-groceries",
      "accountId": "acc-cash-1",
      "targetAccountId": null,
      "amountMinor": -450000,
      "type": "EXPENSE",
      "date": "2026-10-25T14:30:00Z",
      "note": "Weekly groceries"
    },
    {
      "id": "tx-2",
      "title": "Transfer to bKash",
      "categoryId": "cat-transfer",
      "accountId": "acc-brac-1",
      "targetAccountId": "acc-bkash-1",
      "amountMinor": 1500000,
      "type": "TRANSFER",
      "date": "2026-10-25T11:15:00Z",
      "note": "Add money to bKash"
    }
  ],
  "budgets": [
    {
      "id": "b-1",
      "categoryId": "cat-groceries",
      "allocatedMinor": 2500000,
      "period": "MONTHLY"
    }
  ],
  "goals": [
    {
      "id": "g-1",
      "title": "Emergency Fund",
      "targetMinor": 30000000,
      "currentMinor": 18500000,
      "targetDate": "2026-12-31",
      "status": "ACTIVE"
    }
  ],
  "salaryConfig": {
    "salaryAmountMinor": 8500000,
    "salaryPayday": 1,
    "reportingMode": "SALARY_CYCLE"
  }
}
```

---

## 3. Encrypted Backup Container Specification (`.enc.expbkp`)

When a user exports an encrypted backup with a passphrase, the JSON envelope is compressed and encrypted into a single self-contained binary file.

### 3.1 Binary Container Format

```
+-----------------------------------------------------------------------+
| Magic Header  | Salt          | Nonce / IV    | Ciphertext + GCM Tag  |
| 10 Bytes      | 16 Bytes      | 12 Bytes      | N + 16 Bytes          |
| "EXPBKP_ENC"  | Random Bytes  | Random Bytes  | Encrypted JSON Data   |
+-----------------------------------------------------------------------+
```

| Offset (Bytes) | Length (Bytes) | Field Name | Description |
|:--------------:|:--------------:|------------|-------------|
| 0 .. 9 | 10 | **Magic Header** | ASCII string `EXPBKP_ENC` identifying the container. |
| 10 .. 25 | 16 | **Salt** | Cryptographically secure random bytes for PBKDF2 key derivation. |
| 26 .. 37 | 12 | **Nonce / IV** | Initialization vector for AES-256-GCM. |
| 38 .. End | Variable | **Ciphertext + Tag** | AES-256-GCM encrypted payload, terminating with the 16-byte authentication tag. |

### 3.2 Cryptographic Parameters

1. **Key Derivation Function (KDF)**:
   - Algorithm: **PBKDF2-HMAC-SHA256**
   - Iteration Count: **100,000 iterations**
   - Derived Key Length: **256 bits (32 bytes)**
2. **Authenticated Cipher**:
   - Algorithm: **AES-256-GCM** (Galois/Counter Mode)
   - Nonce Size: **96 bits (12 bytes)**
   - Tag Size: **128 bits (16 bytes)**
   - Associated Data (AAD): Optional (defaults to empty).

---

## 4. Platform Implementation Mapping

| Security Function | Android Implementation | iOS Implementation |
|-------------------|------------------------|--------------------|
| **Key Derivation** | `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")` with `PBEKeySpec` | `CCKeyDerivationPBKDF(kCCPBKDF2, ..., kCCPRFHmacAlgSHA256)` in `CommonCrypto` |
| **AES-256-GCM** | `Cipher.getInstance("AES/GCM/NoPadding")` with `GCMParameterSpec` | `CryptoKit.AES.GCM.seal(...)` / `CryptoKit.AES.GCM.open(...)` |
| **Random Number Generation** | `java.security.SecureRandom` | `SecRandomCopyBytes(kSecRandomDefault, ...)` |

---

## 5. Invariant & Restoration Rules

During import on either Android or iOS, the restoration engine enforces:
1. **Schema Validation**: If `schemaVersion` is higher than supported, prompt user to update the app.
2. **Transaction Balance Verification**: For every `TRANSFER`, the validator ensures both source and destination accounts exist. If one account is missing, the transaction is flagged or recreated under a default placeholder wallet.
3. **Integer Money Preservation**: Amounts in minor units are imported directly into 64-bit integer columns without floating-point conversion.
4. **Duplicate Deduplication**: Transactions and categories are matched on UUID `id` to prevent double-counting if a backup is restored over existing data.
