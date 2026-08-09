package com.privategallery.app.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

enum class EditSaveMode { REPLACE_ORIGINAL, SAVE_AS_NEW }

sealed interface SaveEditResult {
    data class Success(val resultUri: Uri) : SaveEditResult
    data class Failure(val reason: String) : SaveEditResult
}

/**
 * STEP 40 — EDIT SAVE OPTIONS
 *
 * Per RULE G (never delete source before destination verification): for
 * REPLACE_ORIGINAL this writes to a *new* MediaStore entry first,
 * verifies the write succeeded (readable, non-empty), and only then
 * deletes the original. If writing or verification fails at any point,
 * the original is left completely untouched.
 */
class SaveEditedImageUseCase(private val context: Context) {

    suspend fun invoke(
        originalUri: Uri,
        editedBitmap: Bitmap,
        displayName: String,
        mimeType: String,
        mode: EditSaveMode
    ): SaveEditResult = withContext(Dispatchers.IO) {
        val baseName = displayName.substringBeforeLast('.')
        val extension = displayName.substringAfterLast('.', "jpg")
        val newFileName = when (mode) {
            EditSaveMode.REPLACE_ORIGINAL -> displayName
            EditSaveMode.SAVE_AS_NEW -> "${baseName}_edited_${System.currentTimeMillis()}.$extension"
        }

        val newUri = createMediaStoreEntry(newFileName, mimeType)
            ?: return@withContext SaveEditResult.Failure("Could not create destination entry")

        val writeSucceeded = writeBitmap(newUri, editedBitmap, mimeType)
        if (!writeSucceeded) {
            context.contentResolver.delete(newUri, null, null)
            return@withContext SaveEditResult.Failure("Failed writing edited image")
        }

        val verified = verifyWrittenFile(newUri)
        if (!verified) {
            context.contentResolver.delete(newUri, null, null)
            return@withContext SaveEditResult.Failure("Verification failed after write")
        }

        if (mode == EditSaveMode.REPLACE_ORIGINAL) {
            // Original is deleted only now that the new copy is verified.
            context.contentResolver.delete(originalUri, null, null)
        }

        SaveEditResult.Success(newUri)
    }

    private fun createMediaStoreEntry(displayName: String, mimeType: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun writeBitmap(uri: Uri, bitmap: Bitmap, mimeType: String): Boolean {
        return try {
            val stream: OutputStream = context.contentResolver.openOutputStream(uri) ?: return false
            stream.use {
                val format = if (mimeType.contains("png")) {
                    Bitmap.CompressFormat.PNG
                } else {
                    Bitmap.CompressFormat.JPEG
                }
                bitmap.compress(format, 95, it)
            }
            val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            context.contentResolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun verifyWrittenFile(uri: Uri): Boolean {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return false
            input.use { it.read() != -1 }
        } catch (e: Exception) {
            false
        }
    }
}
