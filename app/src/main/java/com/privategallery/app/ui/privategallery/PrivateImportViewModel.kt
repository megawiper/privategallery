package com.privategallery.app.ui.privategallery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.domain.repository.MoveToPrivateResult
import com.privategallery.app.domain.repository.PrivateGalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrivateImportUiState(
    val isImporting: Boolean = false,
    val importedCount: Int = 0,
    val failedCount: Int = 0,
    val totalCount: Int = 0
)

data class ImportCandidate(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val isVideo: Boolean
)

/**
 * STEP 54 — PRIVATE IMPORT
 *
 * Reuses the exact same encrypt-verify-store path as STEP 53's move
 * flow (PrivateGalleryRepository.importCopy shares its implementation
 * with moveFromNormalGallery) — one architecture, per RULE C, rather
 * than a second import-specific storage path. Unlike STEP 53's move,
 * the picked source is never deleted, since import is additive.
 */
class PrivateImportViewModel(
    private val repository: PrivateGalleryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivateImportUiState())
    val uiState: StateFlow<PrivateImportUiState> = _uiState.asStateFlow()

    fun importCandidates(candidates: List<ImportCandidate>) {
        if (candidates.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = PrivateImportUiState(isImporting = true, totalCount = candidates.size)
            var imported = 0
            var failed = 0
            candidates.forEach { candidate ->
                val result = repository.importCopy(
                    sourceUri = candidate.uri.toString(),
                    displayName = candidate.displayName,
                    mimeType = candidate.mimeType,
                    isVideo = candidate.isVideo
                )
                if (result is MoveToPrivateResult.Success) imported++ else failed++
                _uiState.value = _uiState.value.copy(importedCount = imported, failedCount = failed)
            }
            _uiState.value = _uiState.value.copy(isImporting = false)
        }
    }
}
