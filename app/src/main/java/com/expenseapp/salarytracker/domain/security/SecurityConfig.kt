package com.expenseapp.salarytracker.domain.security

/**
 * Supported authentication lock modes.
 */
enum class AppLockMode(val displayName: String) {
    DISABLED("Disabled"),
    BIOMETRIC_ONLY("Biometric Only"),
    PASSCODE_ONLY("Passcode Only"),
    BIOMETRIC_OR_PASSCODE("Biometric with Passcode Fallback");

    companion object {
        fun fromString(value: String): AppLockMode = try {
            valueOf(value)
        } catch (e: Exception) {
            DISABLED
        }
    }
}

/**
 * Configurable timeout before the app locks when moved to background.
 */
enum class LockGracePeriod(val displayName: String, val timeoutMs: Long) {
    IMMEDIATELY("Immediately", 0L),
    SECONDS_30("30 Seconds", 30_000L),
    MINUTES_1("1 Minute", 60_000L),
    MINUTES_5("5 Minutes", 300_000L),
    MINUTES_15("15 Minutes", 900_000L);

    companion object {
        fun fromString(value: String): LockGracePeriod = try {
            valueOf(value)
        } catch (e: Exception) {
            IMMEDIATELY
        }
    }
}

/**
 * Binary/Base64 envelope holding ciphertext and authentication metadata.
 */
data class EncryptedPayload(
    val salt: ByteArray,
    val iv: ByteArray,
    val ciphertext: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedPayload
        if (!salt.contentEquals(other.salt)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = salt.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }

    /**
     * Serializes this payload to a combined binary byte array:
     * [SALT_LEN (1B)][SALT][IV_LEN (1B)][IV][CIPHERTEXT_LEN (4B)][CIPHERTEXT]
     */
    fun toByteArray(): ByteArray {
        val totalSize = 1 + salt.size + 1 + iv.size + 4 + ciphertext.size
        val buffer = java.nio.ByteBuffer.allocate(totalSize)
        buffer.put(salt.size.toByte())
        buffer.put(salt)
        buffer.put(iv.size.toByte())
        buffer.put(iv)
        buffer.putInt(ciphertext.size)
        buffer.put(ciphertext)
        return buffer.array()
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): EncryptedPayload {
            val buffer = java.nio.ByteBuffer.wrap(bytes)
            val saltLen = buffer.get().toInt() and 0xFF
            val salt = ByteArray(saltLen)
            buffer.get(salt)

            val ivLen = buffer.get().toInt() and 0xFF
            val iv = ByteArray(ivLen)
            buffer.get(iv)

            val cipherLen = buffer.getInt()
            val ciphertext = ByteArray(cipherLen)
            buffer.get(ciphertext)

            return EncryptedPayload(salt, iv, ciphertext)
        }
    }
}
