package com.privategallery.app.domain.model

enum class AlbumType {
    AUTOMATIC,
    CUSTOM
}

data class Album(
    val id: String,
    val name: String,
    val type: AlbumType,
    val coverMediaUriString: String?,
    val isLocked: Boolean,
    val createdAtMillis: Long,
    val mediaCount: Int = 0
)
