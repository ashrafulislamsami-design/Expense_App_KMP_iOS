package com.expenseapp.salarytracker.data.security

import com.expenseapp.salarytracker.domain.security.EncryptedPayload
import com.expenseapp.salarytracker.domain.security.EncryptionService
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Robust, production-quality AES-256-GCM authenticated encryption service.
 *
 * Requirements:
 * - 12-byte IV/nonce generated fresh for EVERY encryption (never reused).
 * - 128-bit authentication tag length (standard GCM tag).
 * - Authenticated Additional Data (AAD) support.
 * - Password-based encryption using PBKDF2WithHmacSHA256 (100,000 iterations, 16-byte random salt).
 * - Fails closed: tampering or invalid key throws exception, never returns corrupted plaintext.
 */
class AndroidEncryptionService(
    private val keyProvider: () -> SecretKey,
    private val passcodeKdfService: PasscodeKdfService = PasscodeKdfService()
) : EncryptionService {

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val SALT_LENGTH_BYTES = 16
    }

    private val secureRandom = SecureRandom()

    override fun encrypt(plaintext: ByteArray, additionalData: ByteArray?): EncryptedPayload {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider(), spec)

        if (additionalData != null && additionalData.isNotEmpty()) {
            cipher.updateAAD(additionalData)
        }

        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedPayload(
            salt = ByteArray(0), // Keystore master key uses zero salt
            iv = iv,
            ciphertext = ciphertext
        )
    }

    override fun decrypt(payload: EncryptedPayload, additionalData: ByteArray?): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, keyProvider(), spec)

        if (additionalData != null && additionalData.isNotEmpty()) {
            cipher.updateAAD(additionalData)
        }

        return cipher.doFinal(payload.ciphertext)
    }

    override fun encryptWithPassphrase(plaintext: ByteArray, passphrase: CharArray): EncryptedPayload {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)

        val keyBytes = passcodeKdfService.deriveKey(passphrase, salt, iter = 100_000, keyBits = 256)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedPayload(
            salt = salt,
            iv = iv,
            ciphertext = ciphertext
        )
    }

    override fun decryptWithPassphrase(payload: EncryptedPayload, passphrase: CharArray): ByteArray {
        if (payload.salt.isEmpty()) {
            throw IllegalArgumentException("Salt is required for passphrase decryption")
        }

        val keyBytes = passcodeKdfService.deriveKey(passphrase, payload.salt, iter = 100_000, keyBits = 256)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(payload.ciphertext)
    }
}
