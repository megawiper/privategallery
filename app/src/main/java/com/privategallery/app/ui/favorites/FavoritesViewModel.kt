package com.privategallery.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.repository.MediaFilter
import com.privategallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * STEP 20/21 — FAVORITES
 *
 * Favorite state lives independently of the media file (Room-backed,
 * STEP 10) and is toggled from every surface that shows a thumbnail or
 * viewer — Home (STEP 11), Viewer (STEP 24), multi-select (STEP 32) —
 * all through MediaRepository.setFavorite so the state stays consistent
 * everywhere immediately, per STEP 21's requirement.
 */
class FavoritesViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val favorites: StateFlow<List<MediaItem>> = mediaRepository
        .observeMedia(filter = MediaFilter(favoritesOnly = true))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleFavorite(mediaId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            mediaRepository.setFavorite(mediaId, isFavorite)
        }
    }
}
