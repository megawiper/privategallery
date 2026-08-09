package com.privategallery.app.core.media

import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.model.MediaType

/**
 * STEP 16 — DEFAULT ALBUMS
 *
 * Derives automatic albums (All Images, Camera, Screenshots, Downloads,
 * Videos, Recently Added) from MediaItem metadata already scanned in
 * STEP 8/10. These are computed views, not stored rows — so they always
 * reflect the live media set with no separate sync step.
 * Favorites / Recently Deleted / Private are handled by their own
 * dedicated screens (STEP 20, 41, 51) rather than as computed albums here.
 */
data class AutomaticAlbum(
    val key: String,
    val displayName: String,
    val items: List<MediaItem>
)

object AutomaticAlbumsProvider {

    fun buildAlbums(allMedia: List<MediaItem>): List<AutomaticAlbum> {
        val albums = mutableListOf<AutomaticAlbum>()

        albums += AutomaticAlbum("all_images", "All Images", allMedia)

        val camera = allMedia.filter { pathContains(it, "DCIM/Camera") }
        if (camera.isNotEmpty()) albums += AutomaticAlbum("camera", "Camera", camera)

        val screenshots = allMedia.filter { pathContains(it, "Screenshots") }
        if (screenshots.isNotEmpty()) albums += AutomaticAlbum("screenshots", "Screenshots", screenshots)

        val downloads = allMedia.filter { pathContains(it, "Download") }
        if (downloads.isNotEmpty()) albums += AutomaticAlbum("downloads", "Downloads", downloads)

        val videos = allMedia.filter { it.type == MediaType.VIDEO }
        if (videos.isNotEmpty()) albums += AutomaticAlbum("videos", "Videos", videos)

        val recentlyAdded = allMedia
            .sortedByDescending { it.dateAddedMillis }
            .take(RECENTLY_ADDED_LIMIT)
        if (recentlyAdded.isNotEmpty()) {
            albums += AutomaticAlbum("recently_added", "Recently Added", recentlyAdded)
        }

        return albums
    }

    private fun pathContains(item: MediaItem, fragment: String): Boolean {
        return item.relativePath?.contains(fragment, ignoreCase = true) == true
    }

    private const val RECENTLY_ADDED_LIMIT = 60
}
