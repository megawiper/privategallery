package com.privategallery.app.ui.duplicates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privategallery.app.core.media.DuplicateGroup
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryConfirmDialog
import com.privategallery.app.core.ui.components.GalleryEmptyState
import com.privategallery.app.core.ui.components.GalleryFullScreenLoading
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.core.ui.components.GalleryTextActionButton
import com.privategallery.app.core.ui.components.MediaThumbnail

@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicates") },
                navigationIcon = {
                    GalleryIconButton(icon = GalleryIcons.Back, contentDescription = "Back", onClick = onBack)
                }
            )
        }
    ) { padding ->
        when {
            uiState.isScanning -> GalleryFullScreenLoading(
                modifier = Modifier.padding(padding),
                message = "Scanning for duplicates…"
            )
            !uiState.hasScanned -> GalleryEmptyState(
                icon = GalleryIcons.Search,
                title = "Find duplicate photos",
                message = "Scan your gallery for exact and similar-looking duplicates.",
                actionLabel = "Scan Now",
                onAction = viewModel::scanForDuplicates,
                modifier = Modifier.padding(padding)
            )
            uiState.groups.isEmpty() -> GalleryEmptyState(
                icon = GalleryIcons.Check,
                title = "No duplicates found",
                message = "Your gallery is already tidy.",
                modifier = Modifier.padding(padding)
            )
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(uiState.groups, key = { it.id }) { group ->
                        DuplicateGroupCard(
                            group = group,
                            onDeleteSelected = { ids -> viewModel.deleteSelected(ids) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    onDeleteSelected: (List<Long>) -> Unit
) {
    var selectedIds by remember(group.id) { mutableStateOf(setOf<Long>()) }
    var showConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(Dimens.SpaceL)) {
        Text(
            text = if (group.isExactMatch) {
                "${group.items.size} identical photos"
            } else {
                "${group.items.size} similar photos"
            },
            style = MaterialTheme.typography.titleSmall
        )
        if (!group.isExactMatch) {
            Text(
                text = "These look alike but may not be true duplicates — review before deleting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.SpaceS),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)
        ) {
            items(group.items, key = { it.id }) { item ->
                MediaThumbnail(
                    item = item,
                    modifier = Modifier.size(96.dp),
                    isSelected = selectedIds.contains(item.id),
                    selectionModeActive = true,
                    onClick = {
                        selectedIds = if (selectedIds.contains(item.id)) {
                            selectedIds - item.id
                        } else {
                            selectedIds + item.id
                        }
                    },
                    onLongClick = { }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            GalleryTextActionButton(
                text = "Keep All",
                onClick = { selectedIds = emptySet() }
            )
            GalleryTextActionButton(
                text = "Delete Selected (${selectedIds.size})",
                onClick = { if (selectedIds.isNotEmpty()) showConfirm = true },
                enabled = selectedIds.isNotEmpty()
            )
        }
    }

    if (showConfirm) {
        GalleryConfirmDialog(
            title = "Delete ${selectedIds.size} photo(s)?",
            message = "They'll be moved to Recently Deleted.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteSelected(selectedIds.toList())
                selectedIds = emptySet()
                showConfirm = false
            },
            onDismiss = { showConfirm = false }
        )
    }
}
