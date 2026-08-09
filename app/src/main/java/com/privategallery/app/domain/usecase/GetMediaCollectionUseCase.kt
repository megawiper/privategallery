package com.privategallery.app.domain.usecase

import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.repository.MediaFilter
import com.privategallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.first

/**
 * STEP 23 — VIEWER SWIPE ENGINE (source collection resolution)
 *
 * The viewer is opened with a named source collection ("home",
 * "favorites", "album:<id>", "search:<query>") rather than a copy of
 * the media list, per STEP 4's navigation rule against passing large
 * objects through nav args. This use case turns that name back into
 * the actual ordered list the viewer swipes through, so Home results
 * stay Home results, Favorites results stay Favorites results, etc.
 */
class GetMediaCollectionUseCase(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(sourceCollection: String): List<MediaItem> {
        return when {
            sourceCollection == "home" -> mediaRepository.observeMedia().first()
            sourceCollection == "favorites" -> mediaRepository
                .observeMedia(filter = MediaFilter(favoritesOnly = true))
                .first()
            sourceCollection.startsWith("album:") -> {
                val albumId = sourceCollection.removePrefix("album:")
                mediaRepository.observeMedia(filter = MediaFilter(albumId = albumId)).first()
            }
            sourceCollection.startsWith("search:") -> {
                val query = sourceCollection.removePrefix("search:")
                mediaRepository.searchMedia(query)
            }
            else -> mediaRepository.observeMedia().first()
        }
    }
}
