package com.expenseapp.salarytracker.data.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages the application master encryption key in hardware-backed Android Keystore.
 *
 * Rules:
 * - Master key generated and stored exclusively through `AndroidKeyStore`.
 * - AES-256-GCM.
 * - Keys are never hard-coded or exported.
 */
class AndroidKeystoreManager(
    private val keyAlias: String = MASTER_KEY_ALIAS
) {
    companion object {
        const val MASTER_KEY_ALIAS = "ExpenseApp_MasterEncryptionKey"
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Retrieves or generates the 256-bit AES master key in the Android Keystore.
     */
    @Synchronized
    fun getOrCreateMasterKey(): SecretKey {
        if (keyStore.containsAlias(keyAlias)) {
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER
        )

        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Checks if the master key already exists in Keystore.
     */
    fun hasMasterKey(): Boolean = keyStore.containsAlias(keyAlias)

    /**
     * Deletes the key from Keystore (e.g. on full wipe).
     */
    fun deleteMasterKey() {
        if (keyStore.containsAlias(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }
    }
}
