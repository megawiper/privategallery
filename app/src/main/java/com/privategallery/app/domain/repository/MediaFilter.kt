package com.privategallery.app.domain.repository

enum class MediaSortOrder {
    DATE_NEWEST_FIRST,
    DATE_OLDEST_FIRST,
    NAME_A_Z,
    NAME_Z_A,
    SIZE_LARGEST_FIRST,
    SIZE_SMALLEST_FIRST
}

data class MediaFilter(
    val includeImages: Boolean = true,
    val includeVideos: Boolean = true,
    val favoritesOnly: Boolean = false,
    val albumId: String? = null,
    val dateRangeStartMillis: Long? = null,
    val dateRangeEndMillis: Long? = null
)
