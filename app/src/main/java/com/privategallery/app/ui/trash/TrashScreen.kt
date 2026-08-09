package com.privategallery.app.ui.trash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.ThumbnailShape
import com.privategallery.app.core.ui.components.GalleryConfirmDialog
import com.privategallery.app.core.ui.components.GalleryEmptyState
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.core.ui.components.GalleryTextActionButton
import com.privategallery.app.domain.model.TrashItem
import java.util.concurrent.TimeUnit

@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onBack: () -> Unit
) {
    val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var itemPendingPermanentDelete by remember { mutableStateOf<TrashItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recently Deleted") },
                navigationIcon = {
                    GalleryIconButton(icon = GalleryIcons.Back, contentDescription = "Back", onClick = onBack)
                },
                actions = {
                    if (trashItems.isNotEmpty()) {
                        GalleryTextActionButton(text = "Empty", onClick = { showEmptyTrashConfirm = true })
                    }
                }
            )
        }
    ) { padding ->
        if (trashItems.isEmpty()) {
            GalleryEmptyState(
                icon = GalleryIcons.Delete,
                title = "Nothing in Recently Deleted",
                message = "Deleted photos and videos stay here temporarily before permanent removal.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(trashItems, key = { it.id }) { item ->
                    TrashRow(
                        item = item,
                        onRestore = { viewModel.restore(listOf(item.id)) },
                        onDeleteForever = { itemPendingPermanentDelete = item }
                    )
                }
            }
        }
    }

    if (showEmptyTrashConfirm) {
        GalleryConfirmDialog(
            title = "Empty Recently Deleted?",
            message = "All items will be permanently deleted. This can't be undone.",
            confirmLabel = "Empty",
            dismissLabel = "Cancel",
            isDestructive = true,
            onConfirm = { viewModel.emptyTrash(); showEmptyTrashConfirm = false },
            onDismiss = { showEmptyTrashConfirm = false }
        )
    }

    itemPendingPermanentDelete?.let { item ->
        GalleryConfirmDialog(
            title = "Delete forever?",
            message = "\"${item.displayName}\" will be permanently deleted. This can't be undone.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.permanentlyDelete(listOf(item.id))
                itemPendingPermanentDelete = null
            },
            onDismiss = { itemPendingPermanentDelete = null }
        )
    }
}

@Composable
private fun TrashRow(
    item: TrashItem,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpaceL, vertical = Dimens.SpaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clip(ThumbnailShape),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            AsyncImage(
                model = item.trashedUriString,
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Dimens.SpaceL)
        ) {
            Text(text = item.displayName, style = MaterialTheme.typography.titleSmall)
            Text(
                text = remainingTimeLabel(item.expiresAtMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            GalleryTextActionButton(text = "Restore", onClick = onRestore)
            GalleryTextActionButton(text = "Delete", onClick = onDeleteForever)
        }
    }
}

private fun remainingTimeLabel(expiresAtMillis: Long): String {
    val remainingMillis = expiresAtMillis - System.currentTimeMillis()
    if (remainingMillis <= 0) return "Expiring soon"
    val days = TimeUnit.MILLISECONDS.toDays(remainingMillis)
    return if (days >= 1) "$days days left" else "Less than a day left"
}
