package com.privategallery.app.data.media

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * STEP 8 — MEDIASTORE SCANNER
 *
 * Queries the device MediaStore for images and videos and maps rows into
 * RawMediaRecord. This class talks to Android's ContentResolver directly
 * and nothing else — no Room, no UI. STEP 10 (Media Repository) is the
 * only caller, keeping MediaStore code out of ViewModels/Composables per
 * the roadmap's architecture rule.
 */
class MediaStoreScanner(private val context: Context) {

    /**
     * Scans both images and videos. Emits partial batches as they're read
     * so the caller can show scan progress (per STEP 8's requirement) on
     * large libraries, then a final complete list.
     */
    fun scanAllMedia(batchSize: Int = 200): Flow<List<RawMediaRecord>> = flow {
        val images = scanImages()
        emit(images)
        val videos = scanVideos()
        emit(images + videos)
    }.flowOn(Dispatchers.IO)

    suspend fun scanImages(): List<RawMediaRecord> = withContext(Dispatchers.IO) {
        queryMedia(
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            isVideo = false
        )
    }

    suspend fun scanVideos(): List<RawMediaRecord> = withContext(Dispatchers.IO) {
        queryMedia(
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            isVideo = true
        )
    }

    private fun queryMedia(collection: Uri, isVideo: Boolean): List<RawMediaRecord> {
        val projection = buildProjection(isVideo)
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        val results = mutableListOf<RawMediaRecord>()

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateTakenCol = if (!isVideo) {
                cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            } else {
                cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
            }
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val durationCol = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
            val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                -1
            }
            val orientationCol = cursor.getColumnIndex(MediaStore.MediaColumns.ORIENTATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(collection, id)

                results.add(
                    RawMediaRecord(
                        id = id,
                        uri = contentUri,
                        displayName = cursor.getStringOrEmpty(nameCol),
                        mimeType = cursor.getStringOrEmpty(mimeCol),
                        dateTakenMillis = dateTakenCol.takeIfValid()?.let { cursor.getLongOrNull(it) },
                        dateAddedSeconds = cursor.getLong(dateAddedCol),
                        dateModifiedSeconds = cursor.getLong(dateModifiedCol),
                        sizeBytes = cursor.getLong(sizeCol),
                        width = widthCol.takeIfValid()?.let { cursor.getInt(it) } ?: 0,
                        height = heightCol.takeIfValid()?.let { cursor.getInt(it) } ?: 0,
                        durationMillis = durationCol.takeIfValid()?.let { cursor.getLongOrNull(it) },
                        relativePath = relativePathCol.takeIfValid()?.let { cursor.getString(it) },
                        orientationDegrees = orientationCol.takeIfValid()?.let { cursor.getInt(it) } ?: 0,
                        isVideo = isVideo
                    )
                )
            }
        }

        return results
    }

    private fun buildProjection(isVideo: Boolean): Array<String> {
        val base = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION
        )
        base.add(if (!isVideo) MediaStore.Images.Media.DATE_TAKEN else MediaStore.Video.Media.DATE_TAKEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            base.add(MediaStore.MediaColumns.RELATIVE_PATH)
        }
        if (!isVideo) {
            base.add(MediaStore.MediaColumns.ORIENTATION)
        }
        return base.toTypedArray()
    }

    private fun Int.takeIfValid(): Int? = if (this >= 0) this else null

    private fun Cursor.getStringOrEmpty(columnIndex: Int): String {
        return if (columnIndex >= 0) getString(columnIndex).orEmpty() else ""
    }

    private fun Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (isNull(columnIndex)) null else getLong(columnIndex)
    }
}
