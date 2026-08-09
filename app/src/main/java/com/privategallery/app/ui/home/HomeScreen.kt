package com.privategallery.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryEmptyState
import com.privategallery.app.core.ui.components.GalleryFullScreenLoading
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.core.ui.components.GridColumns
import com.privategallery.app.core.ui.components.MediaGrid
import com.privategallery.app.domain.model.MediaItem
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenViewer: (sourceCollection: String, startIndex: Int) -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPrivateGallery: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScopeCompat()

    LaunchedEffect(uiState.selectedTab) {
        val targetPage = if (uiState.selectedTab == HomeTab.IMAGES) 0 else 1
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onTabSelected(if (pagerState.currentPage == 0) HomeTab.IMAGES else HomeTab.ALBUMS)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar(
            selectionModeActive = uiState.selectionModeActive,
            selectedCount = uiState.selectedIds.size,
            onSearchClick = onOpenSearch,
            onPrivateClick = onOpenPrivateGallery,
            onSettingsClick = onOpenSettings,
            onExitSelection = viewModel::exitSelectionMode
        )

        TabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("Images") }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("Albums") }
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> ImagesTabContent(
                    uiState = uiState,
                    onItemClick = { item ->
                        if (uiState.selectionModeActive) {
                            viewModel.onItemClick(item)
                        } else {
                            val allIds = uiState.dateGroups.flatMap { it.items }.map { it.id }
                            onOpenViewer("home", allIds.indexOf(item.id))
                        }
                    },
                    onItemLongClick = viewModel::onItemLongClick
                )
                1 -> AlbumsTabPlaceholder(onOpenAlbums = onOpenAlbums)
            }
        }

        if (uiState.selectionModeActive) {
            var showDeleteConfirm by remember { mutableStateOf(false) }
            com.privategallery.app.core.ui.components.MultiSelectActionBar(
                onShare = { },
                onFavorite = viewModel::favoriteSelected,
                onDelete = { showDeleteConfirm = true },
                onAddToAlbum = { },
                onMoveToPrivate = { }
            )
            if (showDeleteConfirm) {
                com.privategallery.app.core.ui.components.GalleryConfirmDialog(
                    title = "Delete ${uiState.selectedIds.size} item(s)?",
                    message = "They'll be moved to Recently Deleted and permanently removed after the retention period.",
                    confirmLabel = "Delete",
                    dismissLabel = "Cancel",
                    isDestructive = true,
                    onConfirm = {
                        viewModel.deleteSelected()
                        showDeleteConfirm = false
                    },
                    onDismiss = { showDeleteConfirm = false }
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    selectionModeActive: Boolean,
    selectedCount: Int,
    onSearchClick: () -> Unit,
    onPrivateClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExitSelection: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = if (selectionModeActive) "$selectedCount selected" else "Private Gallery"
            )
        },
        navigationIcon = {
            if (selectionModeActive) {
                GalleryIconButton(
                    icon = GalleryIcons.Close,
                    contentDescription = "Cancel selection",
                    onClick = onExitSelection
                )
            }
        },
        actions = {
            if (!selectionModeActive) {
                GalleryIconButton(
                    icon = GalleryIcons.Search,
                    contentDescription = "Search",
                    onClick = onSearchClick
                )
                GalleryIconButton(
                    icon = GalleryIcons.Lock,
                    contentDescription = "Private Gallery",
                    onClick = onPrivateClick
                )
                GalleryIconButton(
                    icon = GalleryIcons.More,
                    contentDescription = "More",
                    onClick = onSettingsClick
                )
            }
        }
    )
}

@Composable
private fun ImagesTabContent(
    uiState: HomeUiState,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit
) {
    if (uiState.isLoading) {
        GalleryFullScreenLoading(message = "Loading your media…")
        return
    }

    if (uiState.dateGroups.isEmpty()) {
        GalleryEmptyState(
            icon = GalleryIcons.Albums,
            title = "No photos or videos yet",
            message = "Media you add to this device will show up here."
        )
        return
    }

    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(GridColumns.THREE.count),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Dimens.GridSpacing3Col)
    ) {
        uiState.dateGroups.forEach { group ->
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(
                        horizontal = Dimens.SpaceS,
                        vertical = Dimens.SpaceS
                    )
                )
            }
            items(
                items = group.items,
                key = { it.id }
            ) { mediaItem ->
                com.privategallery.app.core.ui.components.MediaThumbnail(
                    item = mediaItem,
                    isSelected = uiState.selectedIds.contains(mediaItem.id),
                    selectionModeActive = uiState.selectionModeActive,
                    onClick = { onItemClick(mediaItem) },
                    onLongClick = { onItemLongClick(mediaItem) }
                )
            }
        }
    }
}

@Composable
private fun AlbumsTabPlaceholder(onOpenAlbums: () -> Unit) {
    // STEP 15: visible selector already provided by TabRow above.
    // Full Albums content (automatic + custom albums) is STEP 16/17.
    GalleryEmptyState(
        icon = GalleryIcons.Albums,
        title = "Albums",
        message = "Open the full Albums screen to browse and manage albums.",
        actionLabel = "Open Albums",
        onAction = onOpenAlbums
    )
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()
