package com.privategallery.app.ui.privategallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.core.security.AuthSessionManager
import com.privategallery.app.data.preferences.SecurityScope
import com.privategallery.app.domain.model.PrivateMedia
import com.privategallery.app.domain.repository.PrivateGalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PrivateTab { IMAGES, VIDEOS, ALBUMS, FAVORITES }

/**
 * STEP 51 — PRIVATE GALLERY HOME
 *
 * Reads exclusively from PrivateGalleryRepository (STEP 52) — this
 * ViewModel never holds a MediaRepository reference, so there is no
 * code path by which private media could leak into a normal-gallery
 * query, satisfying RULE J/K structurally rather than just by
 * convention.
 */
class PrivateHomeViewModel(
    private val repository: PrivateGalleryRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(PrivateTab.IMAGES)
    val selectedTab: StateFlow<PrivateTab> = _selectedTab.asStateFlow()

    val images: StateFlow<List<PrivateMedia>> = repository.observeImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val videos: StateFlow<List<PrivateMedia>> = repository.observeVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites: StateFlow<List<PrivateMedia>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val albums = repository.observeAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onTabSelected(tab: PrivateTab) {
        _selectedTab.value = tab
    }

    /** STEP 58 — create a private album. */
    fun createAlbum(name: String, isLocked: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createAlbum(name.trim(), isLocked) }
    }

    /** STEP 59 — rename/lock/unlock/delete, all available at any time. */
    fun renameAlbum(albumId: String, newName: String) {
        viewModelScope.launch { repository.renameAlbum(albumId, newName) }
    }

    fun setAlbumLock(albumId: String, isLocked: Boolean) {
        viewModelScope.launch { repository.setAlbumLock(albumId, isLocked, securityRef = null) }
    }

    fun deleteAlbum(albumId: String) {
        viewModelScope.launch { repository.deleteAlbum(albumId) }
    }

    /** STEP 51/61 — "Lock Now": clears the session immediately. */
    fun lockNow() {
        AuthSessionManager.lock(SecurityScope.PRIVATE_GALLERY)
    }
}
