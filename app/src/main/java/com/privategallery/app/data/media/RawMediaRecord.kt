package com.privategallery.app.data.media

import android.net.Uri

/**
 * STEP 9 (partial, needed by STEP 8) — raw shape of a single MediaStore
 * row, before it is mapped into the clean domain MediaItem (STEP 9's
 * full domain model). Keeping this separate means MediaStore's column
 * quirks never leak into the domain layer.
 */
data class RawMediaRecord(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val dateTakenMillis: Long?,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMillis: Long?,
    val relativePath: String?,
    val orientationDegrees: Int,
    val isVideo: Boolean
)
