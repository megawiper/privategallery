package com.privategallery.app.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryActionRow
import com.privategallery.app.core.ui.components.GalleryActionSheet
import com.privategallery.app.core.ui.components.GalleryFullScreenLoading
import com.privategallery.app.core.ui.components.GalleryIconButton

@Composable
fun ImageViewerScreen(
    viewModel: ImageViewerViewModel,
    onBack: () -> Unit,
    onOpenEditor: (mediaId: Long) -> Unit,
    onOpenInfo: (mediaId: Long) -> Unit,
    onShare: (mediaId: Long) -> Unit,
    onAddToAlbum: (mediaId: Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMoreSheet by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            GalleryFullScreenLoading()
        }
        return
    }

    if (uiState.items.isEmpty()) {
        onBack()
        return
    }

    val pagerState = rememberPagerState(
        initialPage = uiState.currentIndex,
        pageCount = { uiState.items.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // STEP 23 — swipe left/right through the preserved source collection
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = uiState.items[page]
            ZoomableImage(
                model = item.uriString,
                contentDescription = item.displayName,
                onSingleTap = viewModel::toggleControlsVisibility,
                onSwipeDownToClose = onBack
            )
        }

        AnimatedVisibility(
            visible = uiState.controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .statusBarsPadding()
                    .padding(Dimens.SpaceS)
            ) {
                GalleryIconButton(
                    icon = GalleryIcons.Back,
                    contentDescription = "Close",
                    onClick = onBack,
                    tint = Color.White
                )
                Text(
                    text = uiState.counterLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.5f))) {
                uiState.currentItem?.let { item ->
                    ViewerActionPanel(
                        isFavorite = item.isFavorite,
                        onShare = { onShare(item.id) },
                        onEdit = { onOpenEditor(item.id) },
                        onToggleFavorite = viewModel::toggleFavoriteCurrent,
                        onDelete = { viewModel.deleteCurrent(onDeleted = onBack) },
                        onMoreClick = { showMoreSheet = true }
                    )
                }
            }
        }
    }

    if (showMoreSheet) {
        val item = uiState.currentItem
        GalleryActionSheet(onDismiss = { showMoreSheet = false }) {
            GalleryActionRow(
                icon = GalleryIcons.Info,
                label = "Info",
                onClick = { showMoreSheet = false; item?.let { onOpenInfo(it.id) } }
            )
            GalleryActionRow(
                icon = GalleryIcons.Albums,
                label = "Add to Album",
                onClick = { showMoreSheet = false; item?.let { onAddToAlbum(it.id) } }
            )
        }
    }
}
