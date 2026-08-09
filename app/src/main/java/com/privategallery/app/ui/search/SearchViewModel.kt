package com.privategallery.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.model.MediaType
import com.privategallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * STEP 30 — SEARCH FILTER SYSTEM
 * Filters combine (e.g. Videos + August 2026), matching the roadmap's
 * example exactly.
 */
data class SearchFilters(
    val imagesOnly: Boolean = false,
    val videosOnly: Boolean = false,
    val favoritesOnly: Boolean = false,
    val albumId: String? = null
)

data class SearchUiState(
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val results: List<MediaItem> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isSearching: Boolean = false
)

/**
 * STEP 28/29/30 — SEARCH
 *
 * Searches only normal-gallery media through the shared MediaRepository
 * (RULE J: normal search must never return private media — the private
 * search path, STEP 57, uses a completely separate ViewModel and never
 * touches this repository instance's private tables).
 */
class SearchViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        runSearch()
    }

    fun onFiltersChanged(filters: SearchFilters) {
        _uiState.value = _uiState.value.copy(filters = filters)
        runSearch()
    }

    fun onSearchSubmitted() {
        val query = _uiState.value.query
        if (query.isBlank()) return
        val updated = (listOf(query) + _uiState.value.recentSearches)
            .distinct()
            .take(MAX_RECENT_SEARCHES)
        _uiState.value = _uiState.value.copy(recentSearches = updated)
    }

    private fun runSearch() {
        searchJob?.cancel()
        val state = _uiState.value
        if (state.query.isBlank() && !hasActiveFilters(state.filters)) {
            _uiState.value = state.copy(results = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val base = mediaRepository.searchMedia(state.query)
            val filtered = applyFilters(base, state.filters)
            _uiState.value = _uiState.value.copy(results = filtered, isSearching = false)
        }
    }

    private fun applyFilters(items: List<MediaItem>, filters: SearchFilters): List<MediaItem> {
        return items.filter { item ->
            (!filters.imagesOnly || item.type == MediaType.IMAGE) &&
                (!filters.videosOnly || item.type == MediaType.VIDEO) &&
                (!filters.favoritesOnly || item.isFavorite) &&
                (filters.albumId == null || item.albumIds.contains(filters.albumId))
        }
    }

    private fun hasActiveFilters(filters: SearchFilters): Boolean {
        return filters.imagesOnly || filters.videosOnly || filters.favoritesOnly || filters.albumId != null
    }

    private companion object {
        const val MAX_RECENT_SEARCHES = 10
    }
}
