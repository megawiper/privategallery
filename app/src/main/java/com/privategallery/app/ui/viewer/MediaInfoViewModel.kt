package com.privategallery.app.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * STEP 27 — MEDIA INFORMATION SCREEN
 * Reads from the same MediaRepository as every other screen — no
 * separate info data path — and never surfaces private storage
 * implementation details (this ViewModel is only ever constructed for
 * normal-gallery media IDs; the private viewer, STEP 55, uses its own
 * info path that stays within private storage).
 */
class MediaInfoViewModel(
    private val mediaRepository: MediaRepository,
    private val mediaId: Long
) : ViewModel() {

    private val _item = MutableStateFlow<MediaItem?>(null)
    val item: StateFlow<MediaItem?> = _item.asStateFlow()

    init {
        viewModelScope.launch {
            _item.value = mediaRepository.getMediaById(mediaId)
        }
    }
}
