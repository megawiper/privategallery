package com.privategallery.app.ui.video

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.repository.MediaRepository
import com.privategallery.app.domain.usecase.GetMediaCollectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VideoViewerUiState(
    val isLoading: Boolean = true,
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0
) {
    val currentItem: MediaItem? get() = items.getOrNull(currentIndex)
}

/**
 * STEP 25/26 — VIDEO PLAYER / VIDEO SWIPE NAVIGATION
 *
 * Owns a single ExoPlayer instance and swaps its media item as the user
 * swipes (STEP 26), rather than creating a new player per video — this
 * keeps playback state (volume, speed) consistent across swipes and
 * matches STEP 26's "support mixed media navigation" requirement since
 * the same source collection (STEP 23) can contain both images and
 * videos; only the active video page holds an ExoPlayer instance.
 */
class VideoPlayerViewModel(
    context: Context,
    private val mediaRepository: MediaRepository,
    private val getMediaCollectionUseCase: GetMediaCollectionUseCase,
    sourceCollection: String,
    startMediaId: Long
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _uiState = MutableStateFlow(VideoViewerUiState())
    val uiState: StateFlow<VideoViewerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val items = getMediaCollectionUseCase(sourceCollection)
            val startIndex = items.indexOfFirst { it.id == startMediaId }.coerceAtLeast(0)
            _uiState.value = VideoViewerUiState(isLoading = false, items = items, currentIndex = startIndex)
            playCurrent()
        }
    }

    fun onSwipeToIndex(index: Int) {
        if (index == _uiState.value.currentIndex) return
        _uiState.value = _uiState.value.copy(currentIndex = index)
        playCurrent()
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    private fun playCurrent() {
        val item = _uiState.value.currentItem ?: return
        player.setMediaItem(ExoMediaItem.fromUri(item.uriString))
        player.prepare()
        player.playWhenReady = true
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
