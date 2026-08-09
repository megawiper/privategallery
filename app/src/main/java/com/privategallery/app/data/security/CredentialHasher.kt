package com.privategallery.app.data.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * STEP 45/46/47 — SECURITY FOUNDATION / NORMAL & PRIVATE GALLERY LOCK
 *
 * PIN/password/pattern credentials are salted and hashed with PBKDF2
 * before storage — the raw credential is never persisted anywhere, per
 * RULE I. Pattern credentials are converted by the caller into a
 * node-sequence string (e.g. "0-1-4-7") before being passed here, so
 * this hasher treats PIN, password, and pattern identically.
 */
object CredentialHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    data class HashedCredential(val hash: String, val salt: String)

    fun hash(rawCredential: CharArray): HashedCredential {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hashBytes = deriveKey(rawCredential, salt)
        return HashedCredential(
            hash = Base64.encodeToString(hashBytes, Base64.NO_WRAP),
            salt = Base64.encodeToString(salt, Base64.NO_WRAP)
        )
    }

    fun verify(rawCredential: CharArray, stored: HashedCredential): Boolean {
        val salt = Base64.decode(stored.salt, Base64.NO_WRAP)
        val candidateHash = deriveKey(rawCredential, salt)
        val storedHash = Base64.decode(stored.hash, Base64.NO_WRAP)
        return constantTimeEquals(candidateHash, storedHash)
    }

    private fun deriveKey(rawCredential: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(rawCredential, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
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
