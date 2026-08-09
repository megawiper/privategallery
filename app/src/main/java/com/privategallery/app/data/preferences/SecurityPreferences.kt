package com.privategallery.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.privategallery.app.data.security.CredentialHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.securityDataStore by preferencesDataStore(name = "security_preferences")

enum class AuthMethod { NONE, PIN, PASSWORD, PATTERN, BIOMETRIC }

enum class AutoLockDuration(val millis: Long) {
    IMMEDIATELY(0L),
    THIRTY_SECONDS(30_000L),
    ONE_MINUTE(60_000L),
    FIVE_MINUTES(5 * 60_000L),
    NEVER(Long.MAX_VALUE)
}

/** Keeps normal-gallery and private-gallery lock config fully separate. */
enum class SecurityScope(val key: String, val defaultAutoLock: AutoLockDuration) {
    NORMAL_GALLERY("normal", AutoLockDuration.NEVER),
    PRIVATE_GALLERY("private", AutoLockDuration.IMMEDIATELY)
}

/**
 * STEP 45/46/47/49 — SECURITY FOUNDATION, NORMAL & PRIVATE GALLERY LOCK,
 * AUTO-LOCK.
 *
 * Only the PBKDF2 hash+salt (STEP 45) is ever written here — never a raw
 * PIN/password/pattern. Normal-gallery and private-gallery lock config
 * are namespaced under separate key prefixes so they can be configured
 * fully independently, per STEP 47's explicit requirement.
 */
class SecurityPreferences(private val context: Context) {

    private object Keys {
        fun method(scope: String) = stringPreferencesKey("${scope}_auth_method")
        fun hash(scope: String) = stringPreferencesKey("${scope}_credential_hash")
        fun salt(scope: String) = stringPreferencesKey("${scope}_credential_salt")
        fun biometricEnabled(scope: String) = booleanPreferencesKey("${scope}_biometric_enabled")
        fun autoLock(scope: String) = stringPreferencesKey("${scope}_auto_lock")
    }

    fun observeAuthMethod(scope: SecurityScope): Flow<AuthMethod> {
        return context.securityDataStore.data.map { prefs ->
            val raw = prefs[Keys.method(scope.key)]
            raw?.let { AuthMethod.valueOf(it) } ?: AuthMethod.NONE
        }
    }

    fun observeAutoLockDuration(scope: SecurityScope): Flow<AutoLockDuration> {
        return context.securityDataStore.data.map { prefs ->
            val raw = prefs[Keys.autoLock(scope.key)]
            raw?.let { AutoLockDuration.valueOf(it) } ?: scope.defaultAutoLock
        }
    }

    fun observeBiometricEnabled(scope: SecurityScope): Flow<Boolean> {
        return context.securityDataStore.data.map { prefs -> prefs[Keys.biometricEnabled(scope.key)] ?: false }
    }

    suspend fun setCredential(scope: SecurityScope, method: AuthMethod, rawCredential: CharArray) {
        val hashed = CredentialHasher.hash(rawCredential)
        context.securityDataStore.edit { prefs ->
            prefs[Keys.method(scope.key)] = method.name
            prefs[Keys.hash(scope.key)] = hashed.hash
            prefs[Keys.salt(scope.key)] = hashed.salt
        }
    }

    suspend fun setBiometricEnabled(scope: SecurityScope, enabled: Boolean) {
        context.securityDataStore.edit { prefs -> prefs[Keys.biometricEnabled(scope.key)] = enabled }
    }

    suspend fun setAutoLockDuration(scope: SecurityScope, duration: AutoLockDuration) {
        context.securityDataStore.edit { prefs -> prefs[Keys.autoLock(scope.key)] = duration.name }
    }

    suspend fun verifyCredential(scope: SecurityScope, rawCredential: CharArray): Boolean {
        val prefs = context.securityDataStore.data.first()
        val hash = prefs[Keys.hash(scope.key)] ?: return false
        val salt = prefs[Keys.salt(scope.key)] ?: return false
        return CredentialHasher.verify(rawCredential, CredentialHasher.HashedCredential(hash, salt))
    }

    suspend fun clearCredential(scope: SecurityScope) {
        context.securityDataStore.edit { prefs ->
            prefs.remove(Keys.method(scope.key))
            prefs.remove(Keys.hash(scope.key))
            prefs.remove(Keys.salt(scope.key))
            prefs.remove(Keys.biometricEnabled(scope.key))
        }
    }
}
