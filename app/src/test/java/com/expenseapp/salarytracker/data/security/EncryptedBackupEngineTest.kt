package com.expenseapp.salarytracker.data.security

import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.AccountType
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.model.report.BackupEnvelope
import com.expenseapp.salarytracker.domain.service.export.JsonBackupEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime

class EncryptedBackupEngineTest {

    private lateinit var passcodeKdfService: PasscodeKdfService
    private lateinit var encryptedBackupEngine: EncryptedBackupEngine
    private lateinit var jsonBackupEngine: JsonBackupEngine

    @Before
    fun setUp() {
        passcodeKdfService = PasscodeKdfService(iterations = 1000)
        jsonBackupEngine = JsonBackupEngine()
        encryptedBackupEngine = EncryptedBackupEngine(
            passcodeKdfService = passcodeKdfService,
            jsonBackupEngine = jsonBackupEngine
        )
    }

    private fun createSampleEnvelope(): BackupEnvelope {
        return BackupEnvelope(
            formatVersion = 1,
            schemaVersion = 2,
            exportedAtEpochMs = 1759230000000L,
            appVersion = "1.0.0",
            currency = "BDT",
            salaryCycleConfig = SalaryCycleConfig(),
            accounts = listOf(
                Account(id = 1L, name = "Primary Bank", type = AccountType.BANK, initialBalance = MinorUnits(5000000L), currency = "BDT")
            ),
            categories = listOf(
                Category(id = 1L, name = "Groceries", iconName = "shopping_cart", colorHex = "#10B981")
            ),
            transactions = listOf(
                Transaction(
                    id = 101L,
                    amount = MinorUnits(150000L),
                    type = TransactionType.EXPENSE,
                    date = LocalDate.of(2026, 9, 15),
                    time = LocalTime.of(12, 30),
                    accountId = 1L,
                    categoryId = 1L,
                    note = "Bazar groceries and fish"
                )
            ),
            budgets = emptyList(),
            goals = emptyList(),
            recurringRules = emptyList(),
            customFields = emptyList(),
            treeHistory = emptyList()
        )
    }

    @Test
    fun `exportEncryptedBackup and decryptBackup round trip succeeds with valid passphrase`() {
        val envelope = createSampleEnvelope()
        val passphrase = "PassphraseSecret123!".toCharArray()

        val outputStream = ByteArrayOutputStream()
        encryptedBackupEngine.exportEncryptedBackup(envelope, passphrase, outputStream, iterations = 1000)

        val encryptedBytes = outputStream.toByteArray()
        assertTrue(encryptedBytes.size > EncryptedBackupEngine.MAGIC_BYTES.size)
        assertTrue(encryptedBackupEngine.isEncryptedBackup(encryptedBytes))

        // Decrypt using same passphrase
        val decryptedJsonBytes = encryptedBackupEngine.decryptBackup(
            ByteArrayInputStream(encryptedBytes),
            passphrase
        )

        // Validate structure through JsonBackupEngine
        val validation = jsonBackupEngine.validateBackup(ByteArrayInputStream(decryptedJsonBytes))
        assertTrue(validation.isValid)
        assertEquals(1, validation.summary?.transactionCount)
        assertEquals(1, validation.summary?.accountCount)
        assertEquals(1, validation.summary?.categoryCount)
    }

    @Test
    fun `portability test - encrypted backup decrypts independently of keystore or device context`() {
        val envelope = createSampleEnvelope()
        val password = "IndependentCrossPlatformPassword456#".toCharArray()

        // Produce encrypted backup on "Device A"
        val out = ByteArrayOutputStream()
        encryptedBackupEngine.exportEncryptedBackup(envelope, password, out, iterations = 2000)
        val containerBytes = out.toByteArray()

        // Create an entirely separate engine instance simulating "Device B"
        val separateKdf = PasscodeKdfService(iterations = 50_000) // Default iterations different
        val separateEngine = EncryptedBackupEngine(
            passcodeKdfService = separateKdf,
            jsonBackupEngine = JsonBackupEngine()
        )

        // "Device B" must be able to parse container's embedded KDF iterations and decrypt cleanly
        val restoredBytes = separateEngine.decryptBackup(ByteArrayInputStream(containerBytes), password)
        val validation = jsonBackupEngine.validateBackup(ByteArrayInputStream(restoredBytes))
        assertTrue(validation.isValid)
        assertEquals(1, validation.summary?.transactionCount)
    }

    @Test
    fun `same plaintext and password produces distinct ciphertexts due to fresh salt and IV`() {
        val envelope = createSampleEnvelope()
        val password = "DeterministicInputCheck".toCharArray()

        val out1 = ByteArrayOutputStream()
        encryptedBackupEngine.exportEncryptedBackup(envelope, password, out1, iterations = 1000)

        val out2 = ByteArrayOutputStream()
        encryptedBackupEngine.exportEncryptedBackup(envelope, password, out2, iterations = 1000)

        assertFalse(out1.toByteArray().contentEquals(out2.toByteArray()))
    }

    @Test(expected = Exception::class)
    fun `decryptBackup fails closed when given incorrect passphrase`() {
        val envelope = createSampleEnvelope()
        val correctPassphrase = "ValidPassphrase123".toCharArray()
        val wrongPassphrase = "WrongPassphrase999".toCharArray()

        val outputStream = ByteArrayOutputStream()
        encryptedBackupEngine.exportEncryptedBackup(envelope, correctPassphrase, outputStream, iterations = 1000)

        encryptedBackupEngine.decryptBackup(
            ByteArrayInputStream(outputStream.toByteArray()),
            wrongPassphrase
        )
    }

    @Test(expected = Exception::class)
    fun `decryptBackup fails closed when ciphertext is modified`() {
        val envelope = createSampleEnvelope()
        val passphrase = "PassphraseTamperTest".toCharArray()

        val outputStream = ByteArrayOutputStream()
        encryptedBackupEngine.exportEncryptedBackup(envelope, passphrase, outputStream, iterations = 1000)

        val bytes = outputStream.toByteArray()
        // Tamper with the last byte of ciphertext (part of GCM tag or payload)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0x01).toByte()

        encryptedBackupEngine.decryptBackup(ByteArrayInputStream(bytes), passphrase)
    }

    @Test(expected = Exception::class)
    fun `decryptBackup fails closed when KDF parameters in header are modified`() {
        val envelope = createSampleEnvelope()
        val passphrase = "PassphraseHeaderTamper".toCharArray()

        val outputStream = ByteArrayOutputStream()
        encryptedBackupEngine.exportEncryptedBackup(envelope, passphrase, outputStream, iterations = 1000)

        val bytes = outputStream.toByteArray()
        // Tamper with an iteration byte in header (AAD protected)
        // Header starts after 10 bytes of magic
        bytes[14] = (bytes[14].toInt() xor 0xFF).toByte()

        encryptedBackupEngine.decryptBackup(ByteArrayInputStream(bytes), passphrase)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decryptBackup rejects truncated files`() {
        val truncatedBytes = EncryptedBackupEngine.MAGIC_BYTES.copyOf(8)
        encryptedBackupEngine.decryptBackup(ByteArrayInputStream(truncatedBytes), "Pass".toCharArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decryptBackup rejects non-encrypted plain files`() {
        val plainJson = "{\"formatVersion\": 1}".toByteArray(Charsets.UTF_8)
        encryptedBackupEngine.decryptBackup(
            ByteArrayInputStream(plainJson),
            "AnyPassphrase".toCharArray()
        )
    }
}
