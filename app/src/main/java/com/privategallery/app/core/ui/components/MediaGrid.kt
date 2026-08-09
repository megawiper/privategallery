package com.privategallery.app.core.ui.components

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.domain.model.MediaItem

@Composable
fun MediaGrid(
    items: List<MediaItem>,
    columns: GridColumns,
    modifier: Modifier = Modifier,
    selectedIds: Set<Long> = emptySet(),
    selectionModeActive: Boolean = false,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    header: (LazyGridScope.() -> Unit)? = null
) {
    val spacing = when (columns) {
        GridColumns.TWO -> Dimens.GridSpacing2Col
        GridColumns.THREE -> Dimens.GridSpacing3Col
        GridColumns.FOUR -> Dimens.GridSpacing4Col
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns.count),
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing)
    ) {
        header?.invoke(this)
        items(items = items, key = { it.id }) { item ->
            MediaThumbnail(
                item = item,
                isSelected = selectedIds.contains(item.id),
                selectionModeActive = selectionModeActive,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
        }
    }
}
