package com.privategallery.app.ui.privategallery

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.privategallery.app.data.privatestore.PrivateStorageManager
import com.privategallery.app.domain.model.PrivateMedia
import com.privategallery.app.domain.repository.PrivateGalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PrivateVideoUiState(
    val isLoading: Boolean = true,
    val items: List<PrivateMedia> = emptyList(),
    val currentIndex: Int = 0
)

/**
 * STEP 56 — PRIVATE VIDEO PLAYER
 *
 * ExoPlayer needs a real Uri to play from, so unlike the image viewer
 * (STEP 55, which decrypts fully into memory via Coil), video playback
 * decrypts into a file in context.cacheDir/private_playback — inside
 * the app's private cache, still never a public directory, and every
 * file there is deleted the moment a different item is played or this
 * ViewModel is cleared. This is the one place a decrypted private byte
 * ever touches disk, scoped as tightly and briefly as ExoPlayer's API
 * allows — never exposed via a public/exported URI (RULE H, STEP 56's
 * "do not expose private video through public temporary storage").
 */
class PrivateVideoPlayerViewModel(
    context: Context,
    private val repository: PrivateGalleryRepository,
    private val storageManager: PrivateStorageManager,
    private val startSecureId: String
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _uiState = MutableStateFlow(PrivateVideoUiState())
    val uiState: StateFlow<PrivateVideoUiState> = _uiState.asStateFlow()

    private val playbackCacheDir: File = File(context.cacheDir, "private_playback").apply { mkdirs() }
    private var currentPlaybackFile: File? = null

    init {
        viewModelScope.launch {
            val items = repository.observeVideos().first()
            val startIndex = items.indexOfFirst { it.secureId == startSecureId }.coerceAtLeast(0)
            _uiState.value = PrivateVideoUiState(isLoading = false, items = items, currentIndex = startIndex)
            playCurrent()
        }
    }

    fun onSwipeToIndex(index: Int) {
        if (index == _uiState.value.currentIndex) return
        _uiState.value = _uiState.value.copy(currentIndex = index)
        playCurrent()
    }

    private fun playCurrent() {
        val item = _uiState.value.items.getOrNull(_uiState.value.currentIndex) ?: return
        viewModelScope.launch {
            cleanupCurrentPlaybackFile()
            val decrypted = withContext(Dispatchers.IO) {
                val target = File(playbackCacheDir, "${item.secureId}.tmp")
                target.outputStream().use { output ->
                    val ok = storageManager.readMedia(item.encryptedStorageReference, output)
                    if (!ok) target.delete()
                }
                target
            }
            if (decrypted.exists()) {
                currentPlaybackFile = decrypted
                player.setMediaItem(ExoMediaItem.fromUri(Uri.fromFile(decrypted)))
                player.prepare()
                player.playWhenReady = true
            }
        }
    }

    private fun cleanupCurrentPlaybackFile() {
        currentPlaybackFile?.delete()
        currentPlaybackFile = null
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
        cleanupCurrentPlaybackFile()
        // Defensive sweep in case a previous session left files behind
        // (e.g. process death mid-playback).
        playbackCacheDir.listFiles()?.forEach { it.delete() }
    }
}
