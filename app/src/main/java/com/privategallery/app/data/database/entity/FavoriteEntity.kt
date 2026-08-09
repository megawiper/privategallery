package com.privategallery.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * STEP 9/10/20 — favorite state stored independently of the media file
 * itself, keyed by the MediaStore media ID.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
    val addedAtMillis: Long
)
