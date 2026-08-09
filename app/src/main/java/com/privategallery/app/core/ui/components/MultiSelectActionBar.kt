package com.privategallery.app.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons

/**
 * STEP 31/32 — MULTI-SELECT SYSTEM / MULTI-SELECT ACTIONS
 *
 * Rendered by any screen that supports selection (Home STEP 11, Albums
 * STEP 17, Favorites STEP 20, Search STEP 28) once selectionModeActive
 * is true. Actions here call back into that screen's ViewModel, which
 * routes through the shared MediaRepository — no separate multi-select
 * data path.
 */
@Composable
fun MultiSelectActionBar(
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onAddToAlbum: () -> Unit,
    onMoveToPrivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.SpaceS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MultiSelectAction(GalleryIcons.Share, "Share", onShare, Modifier.weight(1f))
            MultiSelectAction(GalleryIcons.Favorite, "Favorite", onFavorite, Modifier.weight(1f))
            MultiSelectAction(GalleryIcons.Delete, "Delete", onDelete, Modifier.weight(1f))
            MultiSelectAction(GalleryIcons.Albums, "Album", onAddToAlbum, Modifier.weight(1f))
            MultiSelectAction(GalleryIcons.Lock, "Private", onMoveToPrivate, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RowScope.MultiSelectAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(Dimens.SpaceXS),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label)
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
