package com.privategallery.app.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.domain.model.TrashItem
import com.privategallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * STEP 41/42 — RECENTLY DELETED UI / TRASH OPERATIONS
 * Restore and permanent delete both require confirmation at the UI
 * layer — this ViewModel only executes what the screen has already
 * confirmed; it never deletes user media silently.
 */
class TrashViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val trashItems: StateFlow<List<TrashItem>> = mediaRepository.observeTrash()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun restore(trashIds: List<String>) {
        viewModelScope.launch { mediaRepository.restoreFromTrash(trashIds) }
    }

    fun permanentlyDelete(trashIds: List<String>) {
        viewModelScope.launch { mediaRepository.permanentlyDelete(trashIds) }
    }

    fun emptyTrash() {
        val allIds = trashItems.value.map { it.id }
        if (allIds.isEmpty()) return
        viewModelScope.launch { mediaRepository.permanentlyDelete(allIds) }
    }
}
