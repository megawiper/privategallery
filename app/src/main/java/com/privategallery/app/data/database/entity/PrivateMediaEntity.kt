package com.privategallery.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "private_media")
data class PrivateMediaEntity(
    @PrimaryKey val secureId: String,
    val encryptedStorageReference: String,
    val encryptedThumbnailReference: String?,
    val isVideo: Boolean,
    val originalDisplayName: String,
    val dateAddedMillis: Long,
    val dateTakenMillis: Long?,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMillis: Long?,
    val albumId: String?,
    val isFavorite: Boolean
)

@Entity(tableName = "private_albums")
data class PrivateAlbumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isLocked: Boolean,
    val lockSecurityRef: String?,
    val coverSecureId: String?,
    val createdAtMillis: Long
)
