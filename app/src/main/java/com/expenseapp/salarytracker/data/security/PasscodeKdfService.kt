package com.expenseapp.salarytracker.data.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Memory/CPU-hard passcode hashing and verification service using PBKDF2WithHmacSHA256.
 *
 * Adheres strictly to security standards:
 * - 100,000 PBKDF2 iterations
 * - 16-byte cryptographically secure random salt
 * - 256-bit derived key length
 * - Plaintext passcodes are NEVER stored.
 * - Constant-time comparison to prevent timing attacks.
 */
class PasscodeKdfService(
    private val iterations: Int = 100_000,
    private val keyLengthBits: Int = 256
) {
    private val secureRandom = SecureRandom()

    /**
     * Derives a cryptographic verifier string in format:
     * "pbkdf2:sha256:<iterations>:<base64-salt>:<base64-hash>"
     */
    fun hashPasscode(passcode: CharArray): String {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)

        val hash = deriveKey(passcode, salt, iterations, keyLengthBits)
        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val hashB64 = Base64.getEncoder().encodeToString(hash)

        return "pbkdf2:sha256:$iterations:$saltB64:$hashB64"
    }

    /**
     * Verifies user [passcode] against a stored [verifierString].
     * Uses constant-time byte comparison to eliminate side-channel timing leaks.
     */
    fun verifyPasscode(passcode: CharArray, verifierString: String): Boolean {
        return try {
            val parts = verifierString.split(":")
            if (parts.size != 5 || parts[0] != "pbkdf2" || parts[1] != "sha256") {
                return false
            }

            val storedIterations = parts[2].toInt()
            val salt = Base64.getDecoder().decode(parts[3])
            val expectedHash = Base64.getDecoder().decode(parts[4])

            val actualHash = deriveKey(passcode, salt, storedIterations, expectedHash.size * 8)
            constantTimeEquals(expectedHash, actualHash)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Derives raw key bytes directly using PBKDF2WithHmacSHA256.
     */
    fun deriveKey(passcode: CharArray, salt: ByteArray, iter: Int = iterations, keyBits: Int = keyLengthBits): ByteArray {
        val spec = PBEKeySpec(passcode, salt, iter, keyBits)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}
