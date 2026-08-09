package com.privategallery.app.ui.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons

/**
 * STEP 24 — VIEWER ACTION PANEL
 * Primary actions inline; secondary actions go through a bottom sheet
 * (onMoreClick), reusing GalleryActionSheet like Albums (STEP 19) and
 * multi-select (STEP 32) rather than a separate menu system.
 */
@Composable
fun ViewerActionPanel(
    isFavorite: Boolean,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ViewerActionButton(GalleryIcons.Share, "Share", onShare, Modifier.weight(1f))
        ViewerActionButton(GalleryIcons.Edit, "Edit", onEdit, Modifier.weight(1f))
        ViewerActionButton(
            icon = if (isFavorite) GalleryIcons.Favorite else GalleryIcons.FavoriteBorder,
            label = "Favorite",
            onClick = onToggleFavorite,
            modifier = Modifier.weight(1f),
            tint = if (isFavorite) Color.Red else Color.White
        )
        ViewerActionButton(GalleryIcons.Delete, "Delete", onDelete, Modifier.weight(1f))
        ViewerActionButton(GalleryIcons.More, "More", onMoreClick, Modifier.weight(1f))
    }
}

@Composable
private fun RowScope.ViewerActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Text(text = label, color = tint, style = MaterialTheme.typography.labelSmall)
    }
}
