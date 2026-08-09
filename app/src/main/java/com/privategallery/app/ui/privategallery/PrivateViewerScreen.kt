package com.privategallery.app.ui.privategallery

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
import com.privategallery.app.core.security.SecureScreenEffect
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryActionRow
import com.privategallery.app.core.ui.components.GalleryActionSheet
import com.privategallery.app.core.ui.components.GalleryFullScreenLoading
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.ui.viewer.ViewerActionPanel
import com.privategallery.app.ui.viewer.ZoomableImage

@Composable
fun PrivateViewerScreen(
    viewModel: PrivateViewerViewModel,
    onBack: () -> Unit
) {
    SecureScreenEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var controlsVisible by remember { mutableStateOf(true) }
    var showMoreSheet by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
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
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val item = uiState.items[page]
            ZoomableImage(
                model = "private://${item.secureId}",
                contentDescription = item.originalDisplayName,
                onSingleTap = { controlsVisible = !controlsVisible },
                onSwipeDownToClose = onBack
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
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
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.5f))) {
                uiState.currentItem?.let { item ->
                    ViewerActionPanel(
                        isFavorite = item.isFavorite,
                        onShare = { },
                        onEdit = { },
                        onToggleFavorite = viewModel::toggleFavoriteCurrent,
                        onDelete = { viewModel.deleteCurrent(onDeleted = onBack) },
                        onMoreClick = { showMoreSheet = true }
                    )
                }
            }
        }
    }

    if (showMoreSheet) {
        GalleryActionSheet(onDismiss = { showMoreSheet = false }) {
            GalleryActionRow(
                icon = GalleryIcons.LockOpen,
                label = "Move to Gallery",
                onClick = {
                    showMoreSheet = false
                    viewModel.moveToNormalGallery(onMoved = onBack)
                }
            )
        }
    }
}
