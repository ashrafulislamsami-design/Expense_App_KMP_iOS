import XCTest
@testable import ExpenseTracker

final class ExpenseTrackerTests: XCTestCase {
    
    func testMinorUnitsFormatting() {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        
        let minorUnits: Int64 = 8500000 // 85,000.00
        let major = Decimal(minorUnits) / 100
        let formatted = formatter.string(from: NSDecimalNumber(decimal: major))
        
        XCTAssertEqual(formatted, "85,000.00")
    }
    
    func testNegativeMinorUnitsFormatting() {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        
        let minorUnits: Int64 = -450000 // -4,500.00
        let absMajor = Decimal(abs(minorUnits)) / 100
        let formatted = formatter.string(from: NSDecimalNumber(decimal: absMajor))
        
        XCTAssertEqual(formatted, "4,500.00")
    }
    
    func testBackupEnvelopeSerialization() throws {
        let service = CrossPlatformBackupService.shared
        let envelope = service.createSampleEnvelope()
        
        let jsonData = try service.exportJson(envelope: envelope)
        XCTAssertGreaterThan(jsonData.count, 0)
        
        let restored = try service.importJson(data: jsonData)
        XCTAssertEqual(restored.schemaVersion, 1)
        XCTAssertEqual(restored.accounts.count, 4)
        XCTAssertEqual(restored.transactions.count, 3)
    }
    
    func testEncryptedBackupRoundtrip() throws {
        let service = CrossPlatformBackupService.shared
        let envelope = service.createSampleEnvelope()
        let passphrase = "SafeMasterKey2026!"
        
        let encryptedData = try service.exportEncrypted(envelope: envelope, passphrase: passphrase)
        XCTAssertGreaterThan(encryptedData.count, 64)
        
        // Verify header
        let headerData = encryptedData.prefix(10)
        let headerString = String(data: headerData, encoding: .utf8)
        XCTAssertEqual(headerString, "EXPBKP_ENC")
        
        // Decrypt
        let restored = try service.importEncrypted(data: encryptedData, passphrase: passphrase)
        XCTAssertEqual(restored.schemaVersion, 1)
        XCTAssertEqual(restored.accounts.count, 4)
    }
    
    func testInvalidPassphraseFails() {
        let service = CrossPlatformBackupService.shared
        let envelope = service.createSampleEnvelope()
        let correctPass = "CorrectKey123"
        let wrongPass = "WrongKey456"
        
        guard let encryptedData = try? service.exportEncrypted(envelope: envelope, passphrase: correctPass) else {
            XCTFail("Export failed")
            return
        }
        
        XCTAssertThrowsError(try service.importEncrypted(data: encryptedData, passphrase: wrongPass))
    }
}
