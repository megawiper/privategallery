package com.privategallery.app.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * STEP 45 — SECURITY FOUNDATION
 *
 * Owns Android Keystore key generation only. Never touches plaintext
 * PINs/passwords or biometric data — those are handled by
 * CredentialHasher (PIN/password) and the OS's own BiometricPrompt
 * (STEP 48), which never exposes raw biometric data to the app. Keys
 * generated here never leave the secure hardware-backed keystore; only
 * a SecretKey handle is returned, never raw key bytes.
 */
object KeystoreManager {

    private const val PROVIDER = "AndroidKeyStore"
    private const val PRIVATE_MEDIA_KEY_ALIAS = "private_gallery_media_key"
    private const val PRIVATE_DB_KEY_ALIAS = "private_gallery_db_key"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(PROVIDER).apply { load(null) }
    }

    /** Key used to encrypt/decrypt private media files (STEP 52). */
    fun getOrCreateMediaKey(): SecretKey = getOrCreateKey(PRIVATE_MEDIA_KEY_ALIAS)

    /** Key used to encrypt the private-media Room database (STEP 52). */
    fun getOrCreateDatabaseKey(): SecretKey = getOrCreateKey(PRIVATE_DB_KEY_ALIAS)

    private fun getOrCreateKey(alias: String): SecretKey {
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /** Removes all app-generated keys — used by the secure-wipe path
     *  if the user resets Private Gallery security entirely. */
    fun deleteAllKeys() {
        if (keyStore.containsAlias(PRIVATE_MEDIA_KEY_ALIAS)) {
            keyStore.deleteEntry(PRIVATE_MEDIA_KEY_ALIAS)
        }
        if (keyStore.containsAlias(PRIVATE_DB_KEY_ALIAS)) {
            keyStore.deleteEntry(PRIVATE_DB_KEY_ALIAS)
        }
    }
}
