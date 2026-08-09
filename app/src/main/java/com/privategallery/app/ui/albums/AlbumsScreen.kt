package com.privategallery.app.ui.albums

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.privategallery.app.core.media.AutomaticAlbum
import com.privategallery.app.core.ui.AlbumCoverShape
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryActionRow
import com.privategallery.app.core.ui.components.GalleryActionSheet
import com.privategallery.app.core.ui.components.GalleryConfirmDialog
import com.privategallery.app.core.ui.components.GalleryEmptyState
import com.privategallery.app.core.ui.components.GalleryFullScreenLoading
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.core.ui.components.GalleryInputDialog
import com.privategallery.app.core.ui.components.GalleryTextActionButton
import com.privategallery.app.domain.model.Album

@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    onOpenAutomaticAlbum: (key: String) -> Unit,
    onOpenCustomAlbum: (albumId: String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var albumPendingDeletion by remember { mutableStateOf<Album?>(null) }
    var albumPendingRename by remember { mutableStateOf<Album?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Albums") },
                navigationIcon = {
                    GalleryIconButton(
                        icon = GalleryIcons.Back,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                },
                actions = {
                    GalleryIconButton(
                        icon = GalleryIcons.Add,
                        contentDescription = "Create album",
                        onClick = { showCreateDialog = true }
                    )
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            GalleryFullScreenLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text(
                    text = "Automatic Albums",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(Dimens.SpaceL)
                )
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                    contentPadding = PaddingValues(horizontal = Dimens.SpaceL)
                ) {
                    items(uiState.automaticAlbums, key = { it.key }) { album ->
                        AutomaticAlbumCard(album = album, onClick = { onOpenAutomaticAlbum(album.key) })
                    }
                }
            }

            item {
                Text(
                    text = "My Albums",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(Dimens.SpaceL)
                )
            }

            if (uiState.customAlbums.isEmpty()) {
                item {
                    GalleryEmptyState(
                        icon = GalleryIcons.Albums,
                        title = "No custom albums yet",
                        message = "Tap + to create your first album.",
                        actionLabel = "Create Album",
                        onAction = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpaceXL)
                    )
                }
            } else {
                items(uiState.customAlbums, key = { it.id }) { album ->
                    CustomAlbumRow(
                        album = album,
                        onClick = { onOpenCustomAlbum(album.id) },
                        onRename = { albumPendingRename = album },
                        onToggleLock = {
                            viewModel.setAlbumLock(album.id, !album.isLocked, null)
                        },
                        onDelete = { albumPendingDeletion = album }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, isLocked ->
                viewModel.createAlbum(name, isLocked)
                showCreateDialog = false
            }
        )
    }

    albumPendingRename?.let { album ->
        var newName by remember(album.id) { mutableStateOf(album.name) }
        GalleryInputDialog(
            title = "Rename Album",
            value = newName,
            onValueChange = { newName = it },
            confirmLabel = "Rename",
            dismissLabel = "Cancel",
            onConfirm = {
                viewModel.renameAlbum(album.id, newName)
                albumPendingRename = null
            },
            onDismiss = { albumPendingRename = null }
        )
    }

    albumPendingDeletion?.let { album ->
        GalleryConfirmDialog(
            title = "Delete album?",
            message = "\"${album.name}\" will be removed. The photos and videos inside it will stay in your gallery.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteAlbum(album.id, deleteMedia = false)
                albumPendingDeletion = null
            },
            onDismiss = { albumPendingDeletion = null }
        )
    }
}

@Composable
private fun AutomaticAlbumCard(album: AutomaticAlbum, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(Dimens.AlbumCoverSize)
            .combinedClickable(onClick = onClick, onLongClick = {}),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(Dimens.AlbumCoverSize)
                .clip(AlbumCoverShape),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            val coverUri = album.items.firstOrNull()?.uriString
            if (coverUri != null) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = album.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.padding(top = Dimens.SpaceXS))
        Text(text = album.displayName, style = MaterialTheme.typography.labelMedium)
        Text(
            text = "${album.items.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CustomAlbumRow(
    album: Album,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
            .padding(horizontal = Dimens.SpaceL, vertical = Dimens.SpaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clip(AlbumCoverShape),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            val coverUri = album.coverMediaUriString
            if (coverUri != null) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = album.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.width(Dimens.SpaceL))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = album.name, style = MaterialTheme.typography.titleSmall)
                if (album.isLocked) {
                    Spacer(modifier = Modifier.width(Dimens.SpaceXS))
                    Icon(
                        imageVector = GalleryIcons.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "${album.mediaCount} items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        GalleryIconButton(
            icon = GalleryIcons.More,
            contentDescription = "Album options",
            onClick = { showMenu = true }
        )
    }

    if (showMenu) {
        GalleryActionSheet(onDismiss = { showMenu = false }) {
            GalleryActionRow(
                icon = GalleryIcons.Edit,
                label = "Rename",
                onClick = { showMenu = false; onRename() }
            )
            GalleryActionRow(
                icon = if (album.isLocked) GalleryIcons.LockOpen else GalleryIcons.Lock,
                label = if (album.isLocked) "Unlock album" else "Lock album",
                onClick = { showMenu = false; onToggleLock() }
            )
            GalleryActionRow(
                icon = GalleryIcons.Delete,
                label = "Delete album",
                isDestructive = true,
                onClick = { showMenu = false; onDelete() }
            )
        }
    }
}

@Composable
private fun CreateAlbumDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, isLocked: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var lockEnabled by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Album") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Album Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.padding(top = Dimens.SpaceL))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lock Album")
                    Switch(checked = lockEnabled, onCheckedChange = { lockEnabled = it })
                }
                if (lockEnabled) {
                    Text(
                        text = "Security method can be configured after creation in album settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            GalleryTextActionButton(
                text = "Create",
                onClick = { if (name.isNotBlank()) onCreate(name, lockEnabled) }
            )
        },
        dismissButton = {
            GalleryTextActionButton(text = "Cancel", onClick = onDismiss)
        }
    )
}
