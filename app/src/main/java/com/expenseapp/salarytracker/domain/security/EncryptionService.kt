package com.expenseapp.salarytracker.domain.security

/**
 * Clean domain abstraction for authenticated cryptographic operations.
 */
interface EncryptionService {
    /**
     * Encrypts [plaintext] with AES-256-GCM using a generated 12-byte random IV.
     * Optionally authenticates [additionalData].
     */
    fun encrypt(plaintext: ByteArray, additionalData: ByteArray? = null): EncryptedPayload

    /**
     * Decrypts [payload] with AES-256-GCM.
     * Throws [SecurityException] or [javax.crypto.AEADBadTagException] if tampered or incorrect key.
     */
    fun decrypt(payload: EncryptedPayload, additionalData: ByteArray? = null): ByteArray

    /**
     * Encrypts [plaintext] using a key derived from a user-supplied [passphrase] via PBKDF2WithHmacSHA256.
     */
    fun encryptWithPassphrase(plaintext: ByteArray, passphrase: CharArray): EncryptedPayload

    /**
     * Decrypts [payload] using a key derived from the user-supplied [passphrase].
     */
    fun decryptWithPassphrase(payload: EncryptedPayload, passphrase: CharArray): ByteArray
}
