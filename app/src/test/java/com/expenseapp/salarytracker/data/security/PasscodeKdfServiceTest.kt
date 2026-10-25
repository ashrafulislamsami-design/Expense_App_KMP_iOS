package com.expenseapp.salarytracker.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PasscodeKdfServiceTest {

    private lateinit var kdfService: PasscodeKdfService

    @Before
    fun setUp() {
        // Use 1,000 iterations in unit test environment for rapid test execution
        kdfService = PasscodeKdfService(iterations = 1000)
    }

    @Test
    fun `hashPasscode produces valid PBKDF2 verifier format`() {
        val pin = "1234".toCharArray()
        val hash = kdfService.hashPasscode(pin)

        assertTrue(hash.startsWith("pbkdf2:sha256:1000:"))
        val parts = hash.split(":")
        assertEquals(5, parts.size)
        assertTrue(parts[3].isNotEmpty()) // Base64 salt
        assertTrue(parts[4].isNotEmpty()) // Base64 hash
    }

    @Test
    fun `verifyPasscode succeeds for matching passcode`() {
        val pin = "9876".toCharArray()
        val verifier = kdfService.hashPasscode(pin)

        assertTrue(kdfService.verifyPasscode("9876".toCharArray(), verifier))
    }

    @Test
    fun `verifyPasscode fails for incorrect passcode`() {
        val pin = "9876".toCharArray()
        val verifier = kdfService.hashPasscode(pin)

        assertFalse(kdfService.verifyPasscode("1234".toCharArray(), verifier))
        assertFalse(kdfService.verifyPasscode("9870".toCharArray(), verifier))
    }

    @Test
    fun `hashPasscode generates unique salts for identical passcodes`() {
        val pin = "4444".toCharArray()
        val hash1 = kdfService.hashPasscode(pin)
        val hash2 = kdfService.hashPasscode(pin)

        assertNotEquals(hash1, hash2)
        val salt1 = hash1.split(":")[3]
        val salt2 = hash2.split(":")[3]
        assertNotEquals(salt1, salt2)
    }

    @Test
    fun `verifyPasscode fails gracefully on malformed verifier strings`() {
        assertFalse(kdfService.verifyPasscode("1234".toCharArray(), "invalid_verifier"))
        assertFalse(kdfService.verifyPasscode("1234".toCharArray(), "pbkdf2:sha256:not_a_number:salt:hash"))
        assertFalse(kdfService.verifyPasscode("1234".toCharArray(), ""))
    }

    @Test
    fun `passcode changes invalidate the previous verifier`() {
        val oldPin = "1111".toCharArray()
        val newPin = "2222".toCharArray()

        val oldVerifier = kdfService.hashPasscode(oldPin)
        val newVerifier = kdfService.hashPasscode(newPin)

        // Verifying new pin against old verifier must fail
        assertFalse(kdfService.verifyPasscode(newPin, oldVerifier))
        // Verifying old pin against new verifier must fail
        assertFalse(kdfService.verifyPasscode(oldPin, newVerifier))
        // Verifying new pin against new verifier must succeed
        assertTrue(kdfService.verifyPasscode(newPin, newVerifier))
    }

    @Test
    fun `benchmark PBKDF2WithHmacSHA256 derivation cost`() {
        // Benchmark 100,000 iterations to measure CPU cost
        val productionKdf = PasscodeKdfService(iterations = 100_000)
        val testPin = "7890".toCharArray()

        val startTime = System.nanoTime()
        val verifier = productionKdf.hashPasscode(testPin)
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000

        val verifyStartTime = System.nanoTime()
        val isValid = productionKdf.verifyPasscode(testPin, verifier)
        val verifyElapsedMs = (System.nanoTime() - verifyStartTime) / 1_000_000

        assertTrue(isValid)
        // 100,000 iterations should take between 10ms and 500ms on modern CPUs,
        // fast enough for responsive UI while presenting 100,000 SHA-256 rounds of brute force resistance
        assertTrue("Derivation took ${elapsedMs}ms", elapsedMs < 2000)
        assertTrue("Verification took ${verifyElapsedMs}ms", verifyElapsedMs < 2000)
    }
}
