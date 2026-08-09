package com.privategallery.app.ui.privategallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.domain.model.PrivateMedia
import com.privategallery.app.domain.repository.PrivateGalleryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrivateSearchUiState(
    val query: String = "",
    val results: List<PrivateMedia> = emptyList(),
    val isSearching: Boolean = false
)

/**
 * STEP 57 — PRIVATE SEARCH
 *
 * Wraps PrivateGalleryRepository.search only — this class has no
 * reference to MediaRepository at all, which is what makes RULE J
 * ("private search must never leak into normal Gallery search")
 * structurally true rather than merely policy.
 */
class PrivateSearchViewModel(
    private val repository: PrivateGalleryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivateSearchUiState())
    val uiState: StateFlow<PrivateSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val results = repository.search(query)
            _uiState.value = _uiState.value.copy(results = results, isSearching = false)
        }
    }
}
