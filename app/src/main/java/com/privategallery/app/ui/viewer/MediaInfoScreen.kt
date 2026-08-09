package com.privategallery.app.ui.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryFullScreenLoading
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.model.MediaType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun MediaInfoScreen(
    viewModel: MediaInfoViewModel,
    onBack: () -> Unit
) {
    val item by viewModel.item.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Info") },
                navigationIcon = {
                    GalleryIconButton(
                        icon = GalleryIcons.Back,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                }
            )
        }
    ) { padding ->
        val current = item
        if (current == null) {
            GalleryFullScreenLoading(modifier = Modifier.padding(padding))
        } else {
            MediaInfoContent(current, Modifier.padding(padding))
        }
    }
}

@Composable
private fun MediaInfoContent(item: MediaItem, modifier: Modifier = Modifier) {
    val dateFormatter = remember { SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.SpaceL)
    ) {
        InfoRow("Name", item.displayName)
        InfoRow("Date", dateFormatter.format(Date(item.dateTakenMillis ?: item.dateAddedMillis)))
        InfoRow("Size", formatFileSize(item.sizeBytes))
        InfoRow("Resolution", "${item.width} × ${item.height}")
        InfoRow("Format", item.mimeType)
        if (item.type == MediaType.VIDEO) {
            item.durationMillis?.let { InfoRow("Duration", formatDuration(it)) }
        }
        InfoRow("Favorite", if (item.isFavorite) "Yes" else "No")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.SpaceM)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(120.dp)
            )
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider()
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
