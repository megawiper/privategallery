package com.privategallery.app.ui.privategallery

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privategallery.app.core.security.SecureScreenEffect
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryEmptyState
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.core.ui.components.GridColumns
import com.privategallery.app.core.ui.components.MediaGrid
import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.model.MediaType
import com.privategallery.app.domain.model.PrivateMedia

@Composable
fun PrivateHomeScreen(
    viewModel: PrivateHomeViewModel,
    onOpenViewer: (startIndex: Int, collection: String) -> Unit,
    onOpenImport: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAlbum: (albumId: String) -> Unit,
    onLockNow: () -> Unit,
    onSwitchToNormalGallery: () -> Unit
) {
    // STEP 50 — screenshot/recording/recent-apps protection for the
    // entire duration this screen is on screen.
    SecureScreenEffect()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val images by viewModel.images.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private Gallery") },
                actions = {
                    GalleryIconButton(
                        icon = GalleryIcons.Search,
                        contentDescription = "Search",
                        onClick = onOpenSearch
                    )
                    GalleryIconButton(
                        icon = GalleryIcons.Add,
                        contentDescription = "Import",
                        onClick = onOpenImport
                    )
                    GalleryIconButton(
                        icon = GalleryIcons.Lock,
                        contentDescription = "Lock Now",
                        onClick = {
                            viewModel.lockNow()
                            onLockNow()
                        }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = PrivateTab.values().indexOf(selectedTab)) {
                PrivateTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            when (selectedTab) {
                PrivateTab.IMAGES -> PrivateMediaGrid(
                    items = images,
                    emptyTitle = "No private photos",
                    onItemClick = { index -> onOpenViewer(index, "private_images") }
                )
                PrivateTab.VIDEOS -> PrivateMediaGrid(
                    items = videos,
                    emptyTitle = "No private videos",
                    onItemClick = { index -> onOpenViewer(index, "private_videos") }
                )
                PrivateTab.FAVORITES -> PrivateMediaGrid(
                    items = favorites,
                    emptyTitle = "No private favorites",
                    onItemClick = { index -> onOpenViewer(index, "private_favorites") }
                )
                PrivateTab.ALBUMS -> {
                    var showCreateDialog by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(false)
                    }
                    if (albums.isEmpty()) {
                        GalleryEmptyState(
                            icon = GalleryIcons.Albums,
                            title = "No private albums yet",
                            message = "Create an album to organize your private media.",
                            actionLabel = "Create Album",
                            onAction = { showCreateDialog = true }
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(albums, key = { it.id }) { album ->
                                var showMenu by androidx.compose.runtime.remember {
                                    androidx.compose.runtime.mutableStateOf(false)
                                }
                                ListItem(
                                    headlineContent = { Text(album.name) },
                                    supportingContent = { Text("${album.mediaCount} items") },
                                    leadingContent = {
                                        if (album.isLocked) {
                                            Icon(imageVector = GalleryIcons.Lock, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.combinedClickable(
                                        onClick = { onOpenAlbum(album.id) },
                                        onLongClick = { showMenu = true }
                                    )
                                )
                                if (showMenu) {
                                    com.privategallery.app.core.ui.components.GalleryActionSheet(
                                        onDismiss = { showMenu = false }
                                    ) {
                                        com.privategallery.app.core.ui.components.GalleryActionRow(
                                            icon = if (album.isLocked) GalleryIcons.LockOpen else GalleryIcons.Lock,
                                            label = if (album.isLocked) "Unlock album" else "Lock album",
                                            onClick = {
                                                showMenu = false
                                                viewModel.setAlbumLock(album.id, !album.isLocked)
                                            }
                                        )
                                        com.privategallery.app.core.ui.components.GalleryActionRow(
                                            icon = GalleryIcons.Delete,
                                            label = "Delete album",
                                            isDestructive = true,
                                            onClick = {
                                                showMenu = false
                                                viewModel.deleteAlbum(album.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (showCreateDialog) {
                        PrivateCreateAlbumDialog(
                            onDismiss = { showCreateDialog = false },
                            onCreate = { name, isLocked ->
                                viewModel.createAlbum(name, isLocked)
                                showCreateDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivateMediaGrid(
    items: List<PrivateMedia>,
    emptyTitle: String,
    onItemClick: (Int) -> Unit
) {
    if (items.isEmpty()) {
        GalleryEmptyState(icon = GalleryIcons.Albums, title = emptyTitle)
        return
    }
    val asMediaItems = items.map { it.toDisplayMediaItem() }
    MediaGrid(
        items = asMediaItems,
        columns = GridColumns.THREE,
        onItemClick = { item -> onItemClick(asMediaItems.indexOf(item)) },
        onItemLongClick = { }
    )
}

/**
 * Adapts PrivateMedia to the shared MediaItem shape purely for reusing
 * MediaGrid/MediaThumbnail. uriString is a private:// pseudo-URI
 * resolved only by the private image loader (STEP 55's Coil Fetcher),
 * never a real content:// URI, so it can never be accidentally
 * resolved by a normal-gallery code path or leak a real file path.
 */
internal fun PrivateMedia.toDisplayMediaItem(): MediaItem = MediaItem(
    id = secureId.hashCode().toLong(),
    uriString = "private://$secureId",
    displayName = originalDisplayName,
    type = type,
    mimeType = if (type == MediaType.VIDEO) "video/*" else "image/*",
    dateTakenMillis = dateTakenMillis,
    dateAddedMillis = dateAddedMillis,
    dateModifiedMillis = dateAddedMillis,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    durationMillis = durationMillis,
    relativePath = null,
    orientationDegrees = 0,
    isFavorite = isFavorite
)

@Composable
private fun PrivateCreateAlbumDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, isLocked: Boolean) -> Unit
) {
    var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var lockEnabled by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Private Album") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Album Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text("Lock this album")
                    androidx.compose.material3.Switch(checked = lockEnabled, onCheckedChange = { lockEnabled = it })
                }
            }
        },
        confirmButton = {
            com.privategallery.app.core.ui.components.GalleryTextActionButton(
                text = "Create",
                onClick = { if (name.isNotBlank()) onCreate(name, lockEnabled) }
            )
        },
        dismissButton = {
            com.privategallery.app.core.ui.components.GalleryTextActionButton(text = "Cancel", onClick = onDismiss)
        }
    )
}
