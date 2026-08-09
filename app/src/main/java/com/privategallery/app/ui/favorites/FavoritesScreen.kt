package com.privategallery.app.ui.favorites

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryEmptyState
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.core.ui.components.GridColumns
import com.privategallery.app.core.ui.components.MediaGrid

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onOpenViewer: (startIndex: Int) -> Unit,
    onBack: () -> Unit
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
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
        if (favorites.isEmpty()) {
            GalleryEmptyState(
                icon = GalleryIcons.FavoriteBorder,
                title = "No favorites yet",
                message = "Tap the heart icon on any photo or video to add it here.",
                modifier = Modifier.padding(padding)
            )
        } else {
            MediaGrid(
                items = favorites,
                columns = GridColumns.THREE,
                modifier = Modifier.padding(padding),
                onItemClick = { item -> onOpenViewer(favorites.indexOf(item)) },
                onItemLongClick = { }
            )
        }
    }
}
