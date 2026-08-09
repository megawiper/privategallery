package com.privategallery.app.data.security

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom

private val Context.dbKeyDataStore by preferencesDataStore(name = "private_db_key_store")

/**
 * STEP 52 — SECURE PRIVATE STORAGE SYSTEM
 *
 * Android Keystore never exposes raw AES key bytes (by design — that's
 * what makes it hardware-backed and secure), so a Keystore key cannot
 * be handed directly to SQLCipher as a passphrase. Instead this uses
 * envelope encryption: generate a random passphrase once, encrypt *it*
 * with the Keystore key (AesGcmCipherHelper, STEP 45), and persist only
 * the ciphertext. The plaintext passphrase exists in memory only for
 * the moment it's handed to SQLCipher's SupportFactory at DB-open time.
 */
object PrivateDbPassphraseManager {

    private val ENCRYPTED_PASSPHRASE_KEY = stringPreferencesKey("encrypted_db_passphrase")

    suspend fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.dbKeyDataStore.data.first()
        val existing = prefs[ENCRYPTED_PASSPHRASE_KEY]
        if (existing != null) {
            return decryptPassphrase(existing)
        }

        val newPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encrypted = encryptPassphrase(newPassphrase)
        context.dbKeyDataStore.edit { it[ENCRYPTED_PASSPHRASE_KEY] = encrypted }
        return newPassphrase
    }

    private fun encryptPassphrase(passphrase: ByteArray): String {
        val key = KeystoreManager.getOrCreateDatabaseKey()
        val output = ByteArrayOutputStream()
        AesGcmCipherHelper.encryptStream(key, ByteArrayInputStream(passphrase), output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun decryptPassphrase(encoded: String): ByteArray {
        val key = KeystoreManager.getOrCreateDatabaseKey()
        val encryptedBytes = Base64.decode(encoded, Base64.NO_WRAP)
        val output = ByteArrayOutputStream()
        AesGcmCipherHelper.decryptStream(key, ByteArrayInputStream(encryptedBytes), output)
        return output.toByteArray()
    }
}
