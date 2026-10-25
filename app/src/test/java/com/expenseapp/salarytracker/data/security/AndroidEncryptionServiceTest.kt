package com.expenseapp.salarytracker.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AndroidEncryptionServiceTest {

    private lateinit var testSecretKey: SecretKey
    private lateinit var encryptionService: AndroidEncryptionService
    private lateinit var passcodeKdfService: PasscodeKdfService

    @Before
    fun setUp() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, SecureRandom())
        testSecretKey = keyGen.generateKey()

        passcodeKdfService = PasscodeKdfService(iterations = 1000) // Lower iterations for fast unit test
        encryptionService = AndroidEncryptionService(
            keyProvider = { testSecretKey },
            passcodeKdfService = passcodeKdfService
        )
    }

    @Test
    fun `encrypt and decrypt round trip returns identical plaintext`() {
        val sampleText = "Sensitive financial transaction: ৳125,000 to Savings"
        val plaintext = sampleText.toByteArray(Charsets.UTF_8)

        val encryptedPayload = encryptionService.encrypt(plaintext)
        val decryptedBytes = encryptionService.decrypt(encryptedPayload)

        assertEquals(sampleText, String(decryptedBytes, Charsets.UTF_8))
    }

    @Test
    fun `encrypt generates unique IV for each call preventing nonce reuse`() {
        val plaintext = "Consistent Account Balance: ৳50,000".toByteArray(Charsets.UTF_8)

        val payload1 = encryptionService.encrypt(plaintext)
        val payload2 = encryptionService.encrypt(plaintext)

        assertFalse(payload1.iv.contentEquals(payload2.iv))
        assertFalse(payload1.ciphertext.contentEquals(payload2.ciphertext))
    }

    @Test(expected = Exception::class)
    fun `decrypt fails closed when ciphertext is tampered`() {
        val plaintext = "Authentic Financial Record".toByteArray(Charsets.UTF_8)
        val payload = encryptionService.encrypt(plaintext)

        // Tamper with the ciphertext (flip first byte)
        val tamperedCiphertext = payload.ciphertext.clone()
        tamperedCiphertext[0] = (tamperedCiphertext[0].toInt() xor 0xFF).toByte()
        val tamperedPayload = payload.copy(ciphertext = tamperedCiphertext)

        encryptionService.decrypt(tamperedPayload)
    }

    @Test
    fun `encryptWithPassphrase and decryptWithPassphrase round trip succeeds`() {
        val password = "SuperSecretFinancePassword!2026".toCharArray()
        val financialData = "Confidential Ledger State".toByteArray(Charsets.UTF_8)

        val payload = encryptionService.encryptWithPassphrase(financialData, password)
        assertTrue(payload.salt.isNotEmpty())
        assertEquals(12, payload.iv.size)

        val decrypted = encryptionService.decryptWithPassphrase(payload, password)
        assertEquals("Confidential Ledger State", String(decrypted, Charsets.UTF_8))
    }

    @Test(expected = Exception::class)
    fun `decryptWithPassphrase fails closed with incorrect passphrase`() {
        val rightPassword = "CorrectPassword123".toCharArray()
        val wrongPassword = "WrongPassword456".toCharArray()
        val data = "Confidential Data".toByteArray(Charsets.UTF_8)

        val payload = encryptionService.encryptWithPassphrase(data, rightPassword)
        encryptionService.decryptWithPassphrase(payload, wrongPassword)
    }
}
