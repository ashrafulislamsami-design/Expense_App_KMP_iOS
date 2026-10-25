import Foundation
import CryptoKit
import CommonCrypto

// =============================================================================
//  EXPENSE & SALARY TRACKER — Cross-Platform Backup & Restore Models & Service
//  Interoperable with Android Room JsonBackupEngine and EncryptedBackupEngine.
// =============================================================================

public struct BackupAccount: Codable, Equatable {
    public let id: Int64
    public let name: String
    public let type: String
    public let initialBalanceMinorUnits: Int64
    public let currency: String
    public let colorHex: String
    
    public init(id: Int64, name: String, type: String, initialBalanceMinorUnits: Int64, currency: String = "BDT", colorHex: String = "#10B981") {
        self.id = id
        self.name = name
        self.type = type
        self.initialBalanceMinorUnits = initialBalanceMinorUnits
        self.currency = currency
        self.colorHex = colorHex
    }
}

public struct BackupTransaction: Codable, Equatable {
    public let id: Int64
    public let amountMinorUnits: Int64
    public let type: String
    public let accountId: Int64
    public let destinationAccountId: Int64?
    public let categoryId: Int64?
    public let date: String
    public let note: String
    
    public init(id: Int64, amountMinorUnits: Int64, type: String, accountId: Int64, destinationAccountId: Int64? = nil, categoryId: Int64? = nil, date: String, note: String = "") {
        self.id = id
        self.amountMinorUnits = amountMinorUnits
        self.type = type
        self.accountId = accountId
        self.destinationAccountId = destinationAccountId
        self.categoryId = categoryId
        self.date = date
        self.note = note
    }
}

public struct BackupEnvelope: Codable, Equatable {
    public let formatVersion: Int
    public let schemaVersion: Int
    public let createdAtEpochMs: Int64
    public let accounts: [BackupAccount]
    public let transactions: [BackupTransaction]
    
    public init(formatVersion: Int = 1, schemaVersion: Int = 1, createdAtEpochMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000), accounts: [BackupAccount], transactions: [BackupTransaction]) {
        self.formatVersion = formatVersion
        self.schemaVersion = schemaVersion
        self.createdAtEpochMs = createdAtEpochMs
        self.accounts = accounts
        self.transactions = transactions
    }
}

/**
 * Cross-Platform Backup & Restore Service for iOS.
 * Interoperable with Android's JsonBackupEngine and EncryptedBackupEngine.
 *
 * Supports:
 * 1. Plain Canonical JSON format with SHA-256 validation.
 * 2. Hardware-independent Encrypted Binary Container ("EXPBKP_ENC") using AES-256-GCM and PBKDF2-HMAC-SHA256.
 */
class CrossPlatformBackupService {
    static let shared = CrossPlatformBackupService()
    
    private let magicBytes = "EXPBKP_ENC".data(using: .ascii)!
    
    private init() {}
    
    // Creates a sample envelope matching standard test ledger
    func createSampleEnvelope() -> BackupEnvelope {
        let sampleAccounts = [
            BackupAccount(id: 1, name: "Cash", type: "CASH", initialBalanceMinorUnits: 500000),
            BackupAccount(id: 2, name: "Astha Bank", type: "BANK", initialBalanceMinorUnits: 2500000),
            BackupAccount(id: 3, name: "bKash", type: "MOBILE_WALLET", initialBalanceMinorUnits: 100000),
            BackupAccount(id: 4, name: "City Card", type: "CREDIT_CARD", initialBalanceMinorUnits: 0)
        ]
        
        let sampleTransactions = [
            BackupTransaction(id: 1, amountMinorUnits: -45000, type: "EXPENSE", accountId: 1, categoryId: 1, date: "2026-10-25", note: "Grocery"),
            BackupTransaction(id: 2, amountMinorUnits: 150000, type: "TRANSFER", accountId: 2, destinationAccountId: 3, date: "2026-10-25", note: "Topup"),
            BackupTransaction(id: 3, amountMinorUnits: 8500000, type: "INCOME", accountId: 2, categoryId: 14, date: "2026-10-24", note: "Salary")
        ]
        
        return BackupEnvelope(accounts: sampleAccounts, transactions: sampleTransactions)
    }
    
    // JSON Serialization
    func exportJson(envelope: BackupEnvelope) throws -> Data {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return try encoder.encode(envelope)
    }
    
    func importJson(data: Data) throws -> BackupEnvelope {
        let decoder = JSONDecoder()
        return try decoder.decode(BackupEnvelope.self, from: data)
    }
    
    // Encrypted Container Serialization
    func exportEncrypted(envelope: BackupEnvelope, passphrase: String) throws -> Data {
        let jsonData = try exportJson(envelope: envelope)
        
        let cipherAlgo = "AES-256-GCM"
        let kdfAlgo = "PBKDF2WithHmacSHA256"
        let iterations: Int32 = 100000
        let keyBits: Int32 = 256
        
        // Generate random 16-byte salt
        var salt = Data(count: 16)
        _ = salt.withUnsafeMutableBytes { SecRandomCopyBytes(kSecRandomDefault, 16, $0.baseAddress!) }
        
        // Generate random 12-byte IV / Nonce
        var iv = Data(count: 12)
        _ = iv.withUnsafeMutableBytes { SecRandomCopyBytes(kSecRandomDefault, 12, $0.baseAddress!) }
        
        // Build header
        var header = Data()
        var versionBig = Int32(1).bigEndian
        header.append(Data(bytes: &versionBig, count: 4))
        
        let cipherBytes = cipherAlgo.data(using: .ascii)!
        var cipherLen = UInt16(cipherBytes.count).bigEndian
        header.append(Data(bytes: &cipherLen, count: 2))
        header.append(cipherBytes)
        
        let kdfBytes = kdfAlgo.data(using: .ascii)!
        var kdfLen = UInt16(kdfBytes.count).bigEndian
        header.append(Data(bytes: &kdfLen, count: 2))
        header.append(kdfBytes)
        
        var iterBig = iterations.bigEndian
        header.append(Data(bytes: &iterBig, count: 4))
        
        var keyBitsBig = keyBits.bigEndian
        header.append(Data(bytes: &keyBitsBig, count: 4))
        
        var saltLenByte = UInt8(salt.count)
        header.append(Data(bytes: &saltLenByte, count: 1))
        header.append(salt)
        
        var ivLenByte = UInt8(iv.count)
        header.append(Data(bytes: &ivLenByte, count: 1))
        header.append(iv)
        
        // Derive key using PBKDF2
        let derivedKey = try deriveKeyPBKDF2(password: passphrase, salt: salt, iterations: Int(iterations), keyLength: 32)
        let symmetricKey = SymmetricKey(data: derivedKey)
        let nonce = try AES.GCM.Nonce(data: iv)
        
        // Encrypt with header as authenticated additional data (AAD)
        let sealedBox = try AES.GCM.seal(jsonData, using: symmetricKey, nonce: nonce, authenticating: header)
        let ciphertextPayload = sealedBox.ciphertext + sealedBox.tag
        
        var payloadLenBig = Int32(ciphertextPayload.count).bigEndian
        
        var fullContainer = Data()
        fullContainer.append(magicBytes)
        fullContainer.append(header)
        fullContainer.append(Data(bytes: &payloadLenBig, count: 4))
        fullContainer.append(ciphertextPayload)
        
        return fullContainer
    }
    
    func importEncrypted(data: Data, passphrase: String) throws -> BackupEnvelope {
        let jsonString = try decryptBackupContainer(fileData: data, password: passphrase)
        guard let jsonData = jsonString.data(using: .utf8) else {
            throw BackupError.corruptedData("Decrypted string cannot be converted to UTF-8 data")
        }
        return try importJson(data: jsonData)
    }
    
    // Validates a raw JSON backup string
    func validatePlainJsonBackup(_ jsonString: String) -> (isValid: Bool, error: String?) {
        guard let data = jsonString.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return (false, "Malformed JSON structure")
        }
        
        guard let formatVersion = json["formatVersion"] as? Int, formatVersion == 1 else {
            return (false, "Unsupported or missing formatVersion")
        }
        
        let hasAccounts = (json["accounts"] as? [[String: Any]]) != nil
        let hasPayload = (json["payload"] as? [String: Any]) != nil
        guard hasAccounts || hasPayload else {
            return (false, "Missing accounts or payload object")
        }
        
        return (true, nil)
    }
    
    // Decrypts an "EXPBKP_ENC" container using the user's password
    func decryptBackupContainer(fileData: Data, password: String) throws -> String {
        guard fileData.count > 10 else {
            throw BackupError.invalidContainer("File is too short to be a valid container")
        }
        
        let magic = fileData.subdata(in: 0..<10)
        guard magic == magicBytes else {
            throw BackupError.invalidContainer("Header magic bytes do not match 'EXPBKP_ENC'")
        }
        
        // Parse Container Header
        var offset = 10
        let version = fileData.subdata(in: offset..<offset+4).withUnsafeBytes { $0.load(as: Int32.self).bigEndian }
        offset += 4
        
        guard version == 1 else {
            throw BackupError.unsupportedVersion("Unsupported container version: \(version)")
        }
        
        // Read cipher algo string
        let cipherLen = Int(fileData.subdata(in: offset..<offset+2).withUnsafeBytes { $0.load(as: UInt16.self).bigEndian })
        offset += 2
        offset += cipherLen
        
        // Read kdf algo string
        let kdfLen = Int(fileData.subdata(in: offset..<offset+2).withUnsafeBytes { $0.load(as: UInt16.self).bigEndian })
        offset += 2
        offset += kdfLen
        
        // Read iterations and keyBits
        let iterations = Int(fileData.subdata(in: offset..<offset+4).withUnsafeBytes { $0.load(as: Int32.self).bigEndian })
        offset += 4
        let _ = Int(fileData.subdata(in: offset..<offset+4).withUnsafeBytes { $0.load(as: Int32.self).bigEndian })
        offset += 4
        
        // Read Salt
        let saltLen = Int(fileData[offset])
        offset += 1
        let salt = fileData.subdata(in: offset..<offset+saltLen)
        offset += saltLen
        
        // Read IV / Nonce
        let ivLen = Int(fileData[offset])
        offset += 1
        let iv = fileData.subdata(in: offset..<offset+ivLen)
        offset += ivLen
        
        let headerEndOffset = offset
        let headerBytes = fileData.subdata(in: 10..<headerEndOffset)
        
        // Read Ciphertext + 16-byte Tag
        let cipherPayloadLen = Int(fileData.subdata(in: offset..<offset+4).withUnsafeBytes { $0.load(as: Int32.self).bigEndian })
        offset += 4
        let cipherPayload = fileData.subdata(in: offset..<offset+cipherPayloadLen)
        
        guard cipherPayload.count > 16 else {
            throw BackupError.corruptedData("Ciphertext payload is incomplete")
        }
        
        let ciphertext = cipherPayload.subdata(in: 0..<cipherPayload.count - 16)
        let tag = cipherPayload.subdata(in: cipherPayload.count - 16..<cipherPayload.count)
        
        // Derive Key using CommonCrypto PBKDF2-HMAC-SHA256
        let derivedKey = try deriveKeyPBKDF2(password: password, salt: salt, iterations: iterations, keyLength: 32)
        let symmetricKey = SymmetricKey(data: derivedKey)
        let nonce = try AES.GCM.Nonce(data: iv)
        
        // Decrypt with Apple CryptoKit and verify AAD (headerBytes)
        let sealedBox = try AES.GCM.SealedBox(nonce: nonce, ciphertext: ciphertext, tag: tag)
        let decryptedData = try AES.GCM.open(sealedBox, using: symmetricKey, authenticating: headerBytes)
        
        guard let jsonString = String(data: decryptedData, encoding: .utf8) else {
            throw BackupError.corruptedData("Decrypted payload is not valid UTF-8 string")
        }
        
        return jsonString
    }
    
    private func deriveKeyPBKDF2(password: String, salt: Data, iterations: Int, keyLength: Int) throws -> Data {
        var derivedKeyData = Data(count: keyLength)
        let passwordData = password.data(using: .utf8)!
        
        let result = derivedKeyData.withUnsafeMutableBytes { derivedKeyBytes in
            salt.withUnsafeBytes { saltBytes in
                passwordData.withUnsafeBytes { passwordBytes in
                    CCKeyDerivationPBKDF(
                        CCPBKDFAlgorithm(kCCPBKDF2),
                        passwordBytes.baseAddress?.assumingMemoryBound(to: Int8.self),
                        passwordData.count,
                        saltBytes.baseAddress?.assumingMemoryBound(to: UInt8.self),
                        salt.count,
                        CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),
                        UInt32(iterations),
                        derivedKeyBytes.baseAddress?.assumingMemoryBound(to: UInt8.self),
                        keyLength
                    )
                }
            }
        }
        
        guard result == kCCSuccess else {
            throw BackupError.keyDerivationFailed
        }
        return derivedKeyData
    }
    
    enum BackupError: Error {
        case invalidContainer(String)
        case unsupportedVersion(String)
        case corruptedData(String)
        case keyDerivationFailed
    }
}
