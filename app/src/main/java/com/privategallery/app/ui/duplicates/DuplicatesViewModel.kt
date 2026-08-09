package com.privategallery.app.ui.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.core.media.DuplicateDetectionEngine
import com.privategallery.app.core.media.DuplicateGroup
import com.privategallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DuplicatesUiState(
    val isScanning: Boolean = false,
    val groups: List<DuplicateGroup> = emptyList(),
    val hasScanned: Boolean = false
)

/**
 * STEP 43/44 — DUPLICATE DETECTION ENGINE / DUPLICATE MANAGEMENT UI
 * Scanning is on-demand rather than automatic, since it's a
 * Dispatchers.Default-heavy pass over file bytes. Deletion always
 * routes through MediaRepository.moveToTrash — never a direct file
 * delete — so a mistaken duplicate removal is still recoverable, per
 * STEP 44's "never automatically delete" requirement.
 */
class DuplicatesViewModel(
    private val mediaRepository: MediaRepository,
    private val duplicateDetectionEngine: DuplicateDetectionEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    fun scanForDuplicates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            val allMedia = mediaRepository.observeMedia().first()
            val groups = duplicateDetectionEngine.findDuplicates(allMedia)
            _uiState.value = DuplicatesUiState(isScanning = false, groups = groups, hasScanned = true)
        }
    }

    fun deleteSelected(mediaIds: List<Long>) {
        if (mediaIds.isEmpty()) return
        viewModelScope.launch {
            mediaRepository.moveToTrash(mediaIds)
            val updatedGroups = _uiState.value.groups.mapNotNull { group ->
                val remaining = group.items.filterNot { mediaIds.contains(it.id) }
                if (remaining.size > 1) group.copy(items = remaining) else null
            }
            _uiState.value = _uiState.value.copy(groups = updatedGroups)
        }
    }
}
