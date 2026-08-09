package com.privategallery.app.ui.privategallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.domain.model.PrivateMedia
import com.privategallery.app.domain.repository.MoveToNormalResult
import com.privategallery.app.domain.repository.PrivateGalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PrivateViewerUiState(
    val isLoading: Boolean = true,
    val items: List<PrivateMedia> = emptyList(),
    val currentIndex: Int = 0
) {
    val currentItem: PrivateMedia? get() = items.getOrNull(currentIndex)
    val counterLabel: String get() = if (items.isEmpty()) "" else "${currentIndex + 1} / ${items.size}"
}

/**
 * STEP 55/56 — PRIVATE MEDIA VIEWER / PRIVATE VIDEO PLAYER
 *
 * Same shape as ImageViewerViewModel (STEP 22) and VideoPlayerViewModel
 * (STEP 25) intentionally — same swipe/zoom/action UX — but every read
 * goes through PrivateGalleryRepository, never MediaRepository, so
 * private content structurally cannot cross into the normal viewer's
 * data path.
 */
class PrivateViewerViewModel(
    private val repository: PrivateGalleryRepository,
    collection: String,
    startIndex: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivateViewerUiState(currentIndex = startIndex))
    val uiState: StateFlow<PrivateViewerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val items = when (collection) {
                "private_videos" -> repository.observeVideos().first()
                "private_favorites" -> repository.observeFavorites().first()
                else -> repository.observeImages().first()
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                items = items,
                currentIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
            )
        }
    }

    fun onPageChanged(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
    }

    fun toggleFavoriteCurrent() {
        val item = _uiState.value.currentItem ?: return
        viewModelScope.launch {
            repository.setFavorite(item.secureId, !item.isFavorite)
            val updated = _uiState.value.items.map {
                if (it.secureId == item.secureId) it.copy(isFavorite = !item.isFavorite) else it
            }
            _uiState.value = _uiState.value.copy(items = updated)
        }
    }

    fun deleteCurrent(onDeleted: () -> Unit) {
        val item = _uiState.value.currentItem ?: return
        viewModelScope.launch {
            val success = repository.deletePrivateMedia(item.secureId)
            if (success) {
                val updated = _uiState.value.items.filterNot { it.secureId == item.secureId }
                val newIndex = _uiState.value.currentIndex.coerceIn(0, (updated.size - 1).coerceAtLeast(0))
                _uiState.value = _uiState.value.copy(items = updated, currentIndex = newIndex)
                if (updated.isEmpty()) onDeleted()
            }
        }
    }

    /** STEP 60 — Move to Normal Gallery, invoked from this viewer's action panel. */
    fun moveToNormalGallery(onMoved: () -> Unit) {
        val item = _uiState.value.currentItem ?: return
        viewModelScope.launch {
            val result = repository.moveToNormalGallery(item.secureId)
            if (result is MoveToNormalResult.Success) {
                val updated = _uiState.value.items.filterNot { it.secureId == item.secureId }
                val newIndex = _uiState.value.currentIndex.coerceIn(0, (updated.size - 1).coerceAtLeast(0))
                _uiState.value = _uiState.value.copy(items = updated, currentIndex = newIndex)
                if (updated.isEmpty()) onMoved()
            }
        }
    }
}
