package com.privategallery.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.core.media.DateGroup
import com.privategallery.app.core.media.DateGroupingEngine
import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.repository.MediaFilter
import com.privategallery.app.domain.repository.MediaRepository
import com.privategallery.app.domain.repository.MediaSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeTab { IMAGES, ALBUMS }

data class HomeUiState(
    val isLoading: Boolean = true,
    val dateGroups: List<DateGroup> = emptyList(),
    val selectedTab: HomeTab = HomeTab.IMAGES,
    val selectionModeActive: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
)

/**
 * STEP 11 — HOME GALLERY
 *
 * Combines STEP 10's repository stream with STEP 12's date grouping.
 * The Albums tab (STEP 15/17) reuses the same repository's
 * observeAlbums() rather than a second data path.
 */
class HomeViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(HomeTab.IMAGES)
    private val _selectionModeActive = MutableStateFlow(false)
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<HomeUiState> = combine(
        mediaRepository.observeMedia(sortOrder = MediaSortOrder.DATE_NEWEST_FIRST),
        _selectedTab,
        _selectionModeActive,
        _selectedIds
    ) { media, tab, selectionActive, selectedIds ->
        HomeUiState(
            isLoading = false,
            dateGroups = DateGroupingEngine.group(media),
            selectedTab = tab,
            selectionModeActive = selectionActive,
            selectedIds = selectedIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        viewModelScope.launch {
            mediaRepository.refresh()
        }
    }

    fun onTabSelected(tab: HomeTab) {
        _selectedTab.value = tab
    }

    fun onItemClick(item: MediaItem) {
        if (_selectionModeActive.value) {
            toggleSelection(item.id)
        }
        // Non-selection-mode click (open viewer) is handled by the
        // screen via a separate lambda since it triggers navigation.
    }

    fun onItemLongClick(item: MediaItem) {
        if (!_selectionModeActive.value) {
            _selectionModeActive.value = true
        }
        toggleSelection(item.id)
    }

    fun exitSelectionMode() {
        _selectionModeActive.value = false
        _selectedIds.value = emptySet()
    }

    fun selectAll(allIds: List<Long>) {
        _selectedIds.value = allIds.toSet()
    }

    /** STEP 32 — bulk favorite toggle for the current selection. */
    fun favoriteSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            mediaRepository.setFavorites(ids, isFavorite = true)
            exitSelectionMode()
        }
    }

    /** STEP 32 — bulk delete (move to trash) for the current selection. */
    fun deleteSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            mediaRepository.moveToTrash(ids)
            exitSelectionMode()
        }
    }

    fun addSelectedToAlbum(albumId: String) {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            mediaRepository.addMediaToAlbum(albumId, ids)
            exitSelectionMode()
        }
    }

    private fun toggleSelection(id: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (current.contains(id)) current - id else current + id
        if (_selectedIds.value.isEmpty()) {
            _selectionModeActive.value = false
        }
    }
}
