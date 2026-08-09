package com.privategallery.app.data.repository

import com.privategallery.app.data.database.entity.AlbumEntity
import com.privategallery.app.data.media.RawMediaRecord
import com.privategallery.app.domain.model.Album
import com.privategallery.app.domain.model.AlbumType
import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.model.MediaType

fun RawMediaRecord.toDomain(
    isFavorite: Boolean,
    albumIds: List<String>
): MediaItem {
    return MediaItem(
        id = id,
        uriString = uri.toString(),
        displayName = displayName,
        type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
        mimeType = mimeType,
        dateTakenMillis = dateTakenMillis,
        dateAddedMillis = dateAddedSeconds * 1000L,
        dateModifiedMillis = dateModifiedSeconds * 1000L,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        durationMillis = durationMillis,
        relativePath = relativePath,
        orientationDegrees = orientationDegrees,
        isFavorite = isFavorite,
        albumIds = albumIds
    )
}

fun AlbumEntity.toDomain(mediaCount: Int): Album {
    return Album(
        id = id,
        name = name,
        type = AlbumType.CUSTOM,
        coverMediaUriString = null,
        isLocked = isLocked,
        createdAtMillis = createdAtMillis,
        mediaCount = mediaCount
    )
}
