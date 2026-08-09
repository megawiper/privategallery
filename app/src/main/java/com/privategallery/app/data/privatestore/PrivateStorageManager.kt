package com.privategallery.app.data.privatestore

import android.content.Context
import com.privategallery.app.data.security.AesGcmCipherHelper
import com.privategallery.app.data.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * STEP 52 — SECURE PRIVATE STORAGE SYSTEM
 *
 * Private media lives in context.filesDir/private_media — app-private
 * internal storage, never DCIM/Pictures/Movies/Downloads or any public
 * cache/thumbnail directory (RULE H, K). Every file on disk here is
 * AES/GCM-encrypted (STEP 45) with a Keystore-backed key, so even a
 * rooted-device file browser sees only ciphertext. Thumbnails get their
 * own encrypted file per item, in a separate subdirectory, so a public
 * thumbnail is never generated for private media (RULE K).
 */
class PrivateStorageManager(private val context: Context) {

    private val mediaDir: File by lazy {
        File(context.filesDir, "private_media").apply { mkdirs() }
    }
    private val thumbnailDir: File by lazy {
        File(context.filesDir, "private_thumbnails").apply { mkdirs() }
    }

    /**
     * Encrypts and stores the given input stream as a new private media
     * file. Returns the storage reference (a UUID-based filename, not a
     * path) or null if the write failed, leaving no partial file behind.
     */
    suspend fun storeMedia(input: InputStream): String? = withContext(Dispatchers.IO) {
        val reference = UUID.randomUUID().toString()
        val destination = File(mediaDir, reference)
        try {
            val key = KeystoreManager.getOrCreateMediaKey()
            destination.outputStream().use { output ->
                AesGcmCipherHelper.encryptStream(key, input, output)
            }
            if (!destination.exists() || destination.length() == 0L) {
                destination.delete()
                return@withContext null
            }
            reference
        } catch (e: Exception) {
            destination.delete()
            null
        }
    }

    suspend fun storeThumbnail(reference: String, input: InputStream): Boolean = withContext(Dispatchers.IO) {
        val destination = File(thumbnailDir, reference)
        try {
            val key = KeystoreManager.getOrCreateMediaKey()
            destination.outputStream().use { output ->
                AesGcmCipherHelper.encryptStream(key, input, output)
            }
            destination.exists() && destination.length() > 0L
        } catch (e: Exception) {
            destination.delete()
            false
        }
    }

    /**
     * Decrypts a stored file into the given output stream — used by the
     * private viewer (STEP 55/56) to feed decrypted bytes directly to an
     * in-memory bitmap/player rather than writing decrypted bytes to disk.
     */
    suspend fun readMedia(reference: String, output: OutputStream): Boolean = withContext(Dispatchers.IO) {
        val source = File(mediaDir, reference)
        if (!source.exists()) return@withContext false
        try {
            val key = KeystoreManager.getOrCreateMediaKey()
            source.inputStream().use { input ->
                AesGcmCipherHelper.decryptStream(key, input, output)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun readThumbnail(reference: String, output: OutputStream): Boolean = withContext(Dispatchers.IO) {
        val source = File(thumbnailDir, reference)
        if (!source.exists()) return@withContext false
        try {
            val key = KeystoreManager.getOrCreateMediaKey()
            source.inputStream().use { input ->
                AesGcmCipherHelper.decryptStream(key, input, output)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteMedia(reference: String): Boolean = withContext(Dispatchers.IO) {
        val mediaFile = File(mediaDir, reference)
        val thumbFile = File(thumbnailDir, reference)
        val mediaDeleted = !mediaFile.exists() || mediaFile.delete()
        val thumbDeleted = !thumbFile.exists() || thumbFile.delete()
        mediaDeleted && thumbDeleted
    }

    suspend fun mediaExists(reference: String): Boolean = withContext(Dispatchers.IO) {
        File(mediaDir, reference).exists()
    }
}
