package com.privategallery.app.domain.model

data class TrashItem(
    val id: String,
    val originalMediaId: Long,
    val originalUriString: String,
    val trashedUriString: String,
    val displayName: String,
    val type: MediaType,
    val deletedAtMillis: Long,
    val expiresAtMillis: Long
)
