package com.privategallery.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isLocked: Boolean,
    val lockSecurityRef: String?,
    val coverMediaId: Long?,
    val createdAtMillis: Long
)

/**
 * Many-to-many join between albums and normal-gallery media IDs.
 * A media file can belong to multiple custom albums without duplicating
 * the underlying file (per STEP 19: deleting an album must not delete
 * the source media).
 */
@Entity(tableName = "album_media_cross_ref", primaryKeys = ["albumId", "mediaId"])
data class AlbumMediaCrossRef(
    val albumId: String,
    val mediaId: Long
)
