package com.privategallery.app.data.security

import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * STEP 45 — SECURITY FOUNDATION
 *
 * Stream-based AES/GCM encryption so private media files (STEP 52) are
 * encrypted/decrypted without loading the whole file into memory —
 * important for video files which can be large. The GCM IV is written
 * as a small unencrypted header at the start of the output stream (IVs
 * are not secret) and read back on decrypt.
 */
object AesGcmCipherHelper {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128

    fun encryptStream(key: SecretKey, input: InputStream, output: OutputStream) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        // 1-byte length header (GCM IVs are always 12 bytes, well under 255).
        output.write(iv.size)
        output.write(iv)
        CipherOutputStream(output, cipher).use { cipherOut ->
            input.copyTo(cipherOut)
        }
    }

    fun decryptStream(key: SecretKey, input: InputStream, output: OutputStream) {
        val ivLength = input.read()
        val iv = ByteArray(ivLength)
        var totalRead = 0
        while (totalRead < ivLength) {
            val count = input.read(iv, totalRead, ivLength - totalRead)
            if (count == -1) break
            totalRead += count
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        CipherInputStream(input, cipher).use { cipherIn ->
            cipherIn.copyTo(output)
        }
    }
}
