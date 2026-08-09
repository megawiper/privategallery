package com.privategallery.app.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.repository.MediaRepository
import com.privategallery.app.domain.usecase.GetMediaCollectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ImageViewerUiState(
    val isLoading: Boolean = true,
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val controlsVisible: Boolean = true
) {
    val currentItem: MediaItem? get() = items.getOrNull(currentIndex)
    val counterLabel: String get() = if (items.isEmpty()) "" else "${currentIndex + 1} / ${items.size}"
}

/**
 * STEP 22/23/24 — FULL-SCREEN IMAGE VIEWER
 *
 * Loads the exact source collection (STEP 23) the caller navigated
 * from, so swiping never mixes results from a different screen. Favorite
 * toggling and deletion route through the same MediaRepository used
 * everywhere else (RULE C), and this ViewModel's job stops at state —
 * the action panel (STEP 24) invokes these methods from the screen.
 */
class ImageViewerViewModel(
    private val mediaRepository: MediaRepository,
    private val getMediaCollectionUseCase: GetMediaCollectionUseCase,
    sourceCollection: String,
    startIndex: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageViewerUiState(currentIndex = startIndex))
    val uiState: StateFlow<ImageViewerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val items = getMediaCollectionUseCase(sourceCollection)
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

    fun toggleControlsVisibility() {
        _uiState.value = _uiState.value.copy(controlsVisible = !_uiState.value.controlsVisible)
    }

    fun toggleFavoriteCurrent() {
        val item = _uiState.value.currentItem ?: return
        viewModelScope.launch {
            mediaRepository.setFavorite(item.id, !item.isFavorite)
            val updatedItems = _uiState.value.items.map {
                if (it.id == item.id) it.copy(isFavorite = !item.isFavorite) else it
            }
            _uiState.value = _uiState.value.copy(items = updatedItems)
        }
    }

    fun deleteCurrent(onDeleted: () -> Unit) {
        val item = _uiState.value.currentItem ?: return
        viewModelScope.launch {
            val success = mediaRepository.moveToTrash(listOf(item.id))
            if (success) {
                val updatedItems = _uiState.value.items.filterNot { it.id == item.id }
                val newIndex = _uiState.value.currentIndex.coerceIn(0, (updatedItems.size - 1).coerceAtLeast(0))
                _uiState.value = _uiState.value.copy(items = updatedItems, currentIndex = newIndex)
                if (updatedItems.isEmpty()) onDeleted()
            }
        }
    }
}
