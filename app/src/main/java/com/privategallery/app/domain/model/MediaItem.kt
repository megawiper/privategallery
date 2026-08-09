package com.privategallery.app.domain.model

/**
 * STEP 9 — MEDIA DATABASE MODELS
 *
 * Clean, Android-framework-light domain representation of a single
 * normal-gallery media item (image or video). This is what flows through
 * ViewModels and Composables — MediaStore-specific quirks stay in
 * data/media (STEP 8) and get mapped into this shape by the repository
 * (STEP 10).
 *
 * `uriString` is stored as a String rather than android.net.Uri so this
 * model has zero Android framework dependency and is trivially testable;
 * it is parsed back to Uri only at the point of use (Coil, ExoPlayer, etc).
 */
data class MediaItem(
    val id: Long,
    val uriString: String,
    val displayName: String,
    val type: MediaType,
    val mimeType: String,
    val dateTakenMillis: Long?,
    val dateAddedMillis: Long,
    val dateModifiedMillis: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMillis: Long?,
    val relativePath: String?,
    val orientationDegrees: Int,
    val isFavorite: Boolean = false,
    val albumIds: List<String> = emptyList()
)
