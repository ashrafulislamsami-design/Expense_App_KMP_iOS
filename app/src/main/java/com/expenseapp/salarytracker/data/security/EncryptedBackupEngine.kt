package com.expenseapp.salarytracker.data.security

import com.expenseapp.salarytracker.domain.model.report.BackupEnvelope
import com.expenseapp.salarytracker.domain.service.export.JsonBackupEngine
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable, Self-Describing Encrypted Backup Container Engine.
 *
 * Container Binary Structure:
 * 1. Magic Bytes: "EXPBKP_ENC" (10 bytes ASCII)
 * 2. Format Version: Int32 (e.g. 1)
 * 3. Cipher Algorithm ID: UTF-8 string prefixed with 2-byte length (e.g. "AES-256-GCM")
 * 4. KDF Algorithm ID: UTF-8 string prefixed with 2-byte length (e.g. "PBKDF2WithHmacSHA256")
 * 5. KDF Iteration Count: Int32 (e.g. 100,000)
 * 6. KDF Key Length Bits: Int32 (e.g. 256)
 * 7. Salt: Prefixed with 1-byte length (16 bytes)
 * 8. Nonce / IV: Prefixed with 1-byte length (12 bytes)
 * 9. Ciphertext + GCM Tag: Prefixed with 4-byte length
 *
 * Security & Portability Properties:
 * - 100% self-describing container: contains all non-secret cryptographic metadata
 * - Completely independent of device hardware or Android Keystore
 * - Can be restored on any Android device or external system using ONLY the user's password
 * - Random 16-byte salt and fresh 12-byte nonce generated on every export
 * - Authenticated Additional Data (AAD) binds the container header parameters to prevent parameter tampering
 */
class EncryptedBackupEngine(
    private val passcodeKdfService: PasscodeKdfService = PasscodeKdfService(),
    private val jsonBackupEngine: JsonBackupEngine = JsonBackupEngine()
) {
    companion object {
        val MAGIC_BYTES = "EXPBKP_ENC".toByteArray(StandardCharsets.US_ASCII)
        const val CONTAINER_VERSION = 1
        const val CIPHER_ALGORITHM = "AES-256-GCM"
        const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val DEFAULT_ITERATIONS = 100_000
        const val KEY_LENGTH_BITS = 256
        const val SALT_LENGTH_BYTES = 16
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
    }

    private val secureRandom = SecureRandom()

    /**
     * Serializes [envelope] to JSON, derives an AES key from [passphrase],
     * encrypts with AES-256-GCM, and packages everything into a self-describing binary stream.
     */
    fun exportEncryptedBackup(
        envelope: BackupEnvelope,
        passphrase: CharArray,
        outputStream: OutputStream,
        iterations: Int = DEFAULT_ITERATIONS
    ) {
        val jsonBytes = jsonBackupEngine.buildFullJsonString(envelope).toByteArray(StandardCharsets.UTF_8)

        // 1. Generate fresh salt & IV
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        // 2. Derive key from passphrase
        val keyBytes = passcodeKdfService.deriveKey(passphrase, salt, iter = iterations, keyBits = KEY_LENGTH_BITS)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        // 3. Build Header for AAD (binds cryptographic parameters into GCM authentication tag)
        val headerBytes = buildHeaderBytes(
            version = CONTAINER_VERSION,
            cipherAlgo = CIPHER_ALGORITHM,
            kdfAlgo = KDF_ALGORITHM,
            kdfIterations = iterations,
            keyBits = KEY_LENGTH_BITS,
            salt = salt,
            iv = iv
        )

        // 4. Encrypt with AES-GCM and authenticate header
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        cipher.updateAAD(headerBytes) // Header tampering will invalidate decryption
        val ciphertext = cipher.doFinal(jsonBytes)

        // 5. Write complete portable container
        outputStream.write(MAGIC_BYTES)
        outputStream.write(headerBytes)
        val cipherLenBuf = ByteBuffer.allocate(4).putInt(ciphertext.size).array()
        outputStream.write(cipherLenBuf)
        outputStream.write(ciphertext)
        outputStream.flush()
    }

    /**
     * Checks if [headerBytes] starts with the magic signature "EXPBKP_ENC".
     */
    fun isEncryptedBackup(headerBytes: ByteArray): Boolean {
        if (headerBytes.size < MAGIC_BYTES.size) return false
        for (i in MAGIC_BYTES.indices) {
            if (headerBytes[i] != MAGIC_BYTES[i]) return false
        }
        return true
    }

    /**
     * Decrypts [inputStream] using [passphrase], validating magic, header parameters,
     * deriving key with the container's stored salt and iterations, and verifying GCM auth tag.
     */
    fun decryptBackup(inputStream: InputStream, passphrase: CharArray): ByteArray {
        val allBytes = inputStream.readBytes()
        if (allBytes.size < MAGIC_BYTES.size + 16) {
            throw IllegalArgumentException("Truncated encrypted backup file")
        }

        // 1. Verify Magic
        val magic = allBytes.copyOfRange(0, MAGIC_BYTES.size)
        if (!magic.contentEquals(MAGIC_BYTES)) {
            throw IllegalArgumentException("Not an encrypted expense backup file (invalid magic header)")
        }

        val buffer = ByteBuffer.wrap(allBytes, MAGIC_BYTES.size, allBytes.size - MAGIC_BYTES.size)

        // 2. Parse Self-Describing Header
        val headerStartPos = buffer.position()
        val version = buffer.getInt()
        if (version != CONTAINER_VERSION) {
            throw IllegalArgumentException("Unsupported encrypted backup container version: $version")
        }

        val cipherAlgoLen = buffer.getShort().toInt() and 0xFFFF
        val cipherAlgoBytes = ByteArray(cipherAlgoLen)
        buffer.get(cipherAlgoBytes)
        val cipherAlgo = String(cipherAlgoBytes, StandardCharsets.UTF_8)
        if (cipherAlgo != CIPHER_ALGORITHM) {
            throw IllegalArgumentException("Unsupported cipher algorithm: $cipherAlgo")
        }

        val kdfAlgoLen = buffer.getShort().toInt() and 0xFFFF
        val kdfAlgoBytes = ByteArray(kdfAlgoLen)
        buffer.get(kdfAlgoBytes)
        val kdfAlgo = String(kdfAlgoBytes, StandardCharsets.UTF_8)
        if (kdfAlgo != KDF_ALGORITHM) {
            throw IllegalArgumentException("Unsupported KDF algorithm: $kdfAlgo")
        }

        val iterations = buffer.getInt()
        if (iterations < 1000 || iterations > 2_000_000) {
            throw IllegalArgumentException("Invalid KDF iteration count in container: $iterations")
        }

        val keyBits = buffer.getInt()
        if (keyBits != KEY_LENGTH_BITS) {
            throw IllegalArgumentException("Unsupported key length: $keyBits")
        }

        val saltLen = buffer.get().toInt() and 0xFF
        val salt = ByteArray(saltLen)
        buffer.get(salt)

        val ivLen = buffer.get().toInt() and 0xFF
        val iv = ByteArray(ivLen)
        buffer.get(iv)

        val headerEndPos = buffer.position()
        val headerBytes = allBytes.copyOfRange(headerStartPos, headerEndPos)

        val cipherLen = buffer.getInt()
        if (cipherLen <= 0 || buffer.remaining() < cipherLen) {
            throw IllegalArgumentException("Corrupted ciphertext length or truncated file")
        }

        val ciphertext = ByteArray(cipherLen)
        buffer.get(ciphertext)

        // 3. Derive key using parameters stored in header
        val keyBytes = passcodeKdfService.deriveKey(passphrase, salt, iter = iterations, keyBits = keyBits)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        // 4. Decrypt & Authenticate
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        cipher.updateAAD(headerBytes) // Validates that header parameters were not tampered with

        return cipher.doFinal(ciphertext)
    }

    private fun buildHeaderBytes(
        version: Int,
        cipherAlgo: String,
        kdfAlgo: String,
        kdfIterations: Int,
        keyBits: Int,
        salt: ByteArray,
        iv: ByteArray
    ): ByteArray {
        val cipherAlgoBytes = cipherAlgo.toByteArray(StandardCharsets.UTF_8)
        val kdfAlgoBytes = kdfAlgo.toByteArray(StandardCharsets.UTF_8)

        val totalSize = 4 + 2 + cipherAlgoBytes.size + 2 + kdfAlgoBytes.size + 4 + 4 + 1 + salt.size + 1 + iv.size
        val buffer = ByteBuffer.allocate(totalSize)

        buffer.putInt(version)
        buffer.putShort(cipherAlgoBytes.size.toShort())
        buffer.put(cipherAlgoBytes)
        buffer.putShort(kdfAlgoBytes.size.toShort())
        buffer.put(kdfAlgoBytes)
        buffer.putInt(kdfIterations)
        buffer.putInt(keyBits)
        buffer.put(salt.size.toByte())
        buffer.put(salt)
        buffer.put(iv.size.toByte())
        buffer.put(iv)

        return buffer.array()
    }
}
