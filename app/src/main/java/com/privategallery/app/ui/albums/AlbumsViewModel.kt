package com.privategallery.app.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.core.media.AutomaticAlbum
import com.privategallery.app.core.media.AutomaticAlbumsProvider
import com.privategallery.app.domain.model.Album
import com.privategallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlbumsUiState(
    val automaticAlbums: List<AutomaticAlbum> = emptyList(),
    val customAlbums: List<Album> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * STEP 15/16/17/18/19 — ALBUMS
 *
 * automaticAlbums come from AutomaticAlbumsProvider (STEP 16, computed,
 * not stored). customAlbums come from the repository's Room-backed album
 * table (STEP 10/17). Create/rename/lock/delete all route through the
 * same MediaRepository used by Home (STEP 11) — one architecture, per
 * RULE C — rather than a second album data path.
 */
class AlbumsViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val uiState: StateFlow<AlbumsUiState> = combine(
        mediaRepository.observeMedia(),
        mediaRepository.observeAlbums()
    ) { allMedia, customAlbums ->
        AlbumsUiState(
            automaticAlbums = AutomaticAlbumsProvider.buildAlbums(allMedia),
            customAlbums = customAlbums,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumsUiState()
    )

    /** STEP 18 — Create Album. Lock configuration (PIN/password/pattern/
     * biometric) is applied via SecuritySettings once STEP 47 exists;
     * this call only records whether the album starts locked. */
    fun createAlbum(name: String, isLocked: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch {
            mediaRepository.createAlbum(name.trim(), isLocked)
        }
    }

    /** STEP 19 — deleting an album never deletes underlying media unless
     * deleteMedia is explicitly requested by the user via confirmation. */
    fun deleteAlbum(albumId: String, deleteMedia: Boolean) {
        viewModelScope.launch {
            mediaRepository.deleteAlbum(albumId, deleteMedia)
        }
    }

    fun renameAlbum(albumId: String, newName: String) {
        viewModelScope.launch {
            mediaRepository.renameAlbum(albumId, newName)
        }
    }

    /** STEP 59 — album lock can be changed at any time after creation. */
    fun setAlbumLock(albumId: String, isLocked: Boolean, securityRef: String?) {
        viewModelScope.launch {
            mediaRepository.setAlbumLock(albumId, isLocked, securityRef)
        }
    }

    fun addMediaToAlbum(albumId: String, mediaIds: List<Long>) {
        viewModelScope.launch {
            mediaRepository.addMediaToAlbum(albumId, mediaIds)
        }
    }

    fun removeMediaFromAlbum(albumId: String, mediaIds: List<Long>) {
        viewModelScope.launch {
            mediaRepository.removeMediaFromAlbum(albumId, mediaIds)
        }
    }
}
