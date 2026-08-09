package com.privategallery.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.ThumbnailShape
import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.model.MediaType
import java.util.concurrent.TimeUnit

/**
 * STEP 13/14 — reusable grid thumbnail. Used by Home (STEP 11), Albums
 * (STEP 17), Favorites (STEP 20), Search results (STEP 28), and
 * multi-select (STEP 31) so thumbnail behavior stays consistent
 * everywhere instead of being reimplemented per screen.
 */
@Composable
fun MediaThumbnail(
    item: MediaItem,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    selectionModeActive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(ThumbnailShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = item.uriString,
            contentDescription = item.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (item.type == MediaType.VIDEO) {
            Icon(
                imageVector = GalleryIcons.Play,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
            item.durationMillis?.let { duration ->
                Text(
                    text = formatDuration(duration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                )
            }
        }

        if (item.isFavorite) {
            Icon(
                imageVector = GalleryIcons.Favorite,
                contentDescription = "Favorite",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }

        if (selectionModeActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) Color.Black.copy(alpha = 0.35f) else Color.Transparent)
            )
            Icon(
                imageVector = if (isSelected) GalleryIcons.Check else GalleryIcons.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp)
            )
        }
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
