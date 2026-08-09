package com.privategallery.app.domain.model

data class PrivateMedia(
    val secureId: String,
    val encryptedStorageReference: String,
    val type: MediaType,
    val originalDisplayName: String,
    val dateAddedMillis: Long,
    val dateTakenMillis: Long?,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMillis: Long?,
    val albumId: String?,
    val isFavorite: Boolean = false
)
