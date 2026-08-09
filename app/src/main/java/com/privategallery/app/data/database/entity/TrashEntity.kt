package com.privategallery.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val id: String,
    val originalMediaId: Long,
    val originalUriString: String,
    val trashedUriString: String,
    val displayName: String,
    val isVideo: Boolean,
    val deletedAtMillis: Long,
    val expiresAtMillis: Long
)
