package com.privategallery.app.ui.privategallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.privategallery.app.core.security.SecureScreenEffect
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryFullScreenLoading
import com.privategallery.app.core.ui.components.GalleryIconButton

@Composable
fun PrivateVideoPlayerScreen(
    viewModel: PrivateVideoPlayerViewModel,
    onBack: () -> Unit
) {
    SecureScreenEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

    val pagerState = rememberPagerState(
        initialPage = uiState.currentIndex,
        pageCount = { uiState.items.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onSwipeToIndex(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == uiState.currentIndex) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = viewModel.player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(Dimens.SpaceS)
        ) {
            GalleryIconButton(
                icon = GalleryIcons.Back,
                contentDescription = "Close",
                onClick = onBack,
                tint = Color.White,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}
