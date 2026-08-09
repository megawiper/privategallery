package com.privategallery.app.ui.privategallery

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privategallery.app.core.security.SecureScreenEffect
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryEmptyState
import com.privategallery.app.core.ui.components.GalleryFullScreenLoading
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.core.ui.components.GridColumns
import com.privategallery.app.core.ui.components.MediaGrid

@Composable
fun PrivateSearchScreen(
    viewModel: PrivateSearchViewModel,
    onOpenViewer: (startIndex: Int) -> Unit,
    onBack: () -> Unit
) {
    SecureScreenEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChanged,
                        placeholder = { Text("Search private photos and videos…") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { }),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    GalleryIconButton(icon = GalleryIcons.Back, contentDescription = "Back", onClick = onBack)
                }
            )
        }
    ) { padding ->
        when {
            uiState.isSearching -> GalleryFullScreenLoading(modifier = Modifier.fillMaxSize())
            uiState.query.isBlank() -> GalleryEmptyState(
                icon = GalleryIcons.Search,
                title = "Search your Private Gallery",
                message = "This never searches your normal gallery.",
                modifier = Modifier.fillMaxSize()
            )
            uiState.results.isEmpty() -> GalleryEmptyState(
                icon = GalleryIcons.Search,
                title = "No results",
                modifier = Modifier.fillMaxSize()
            )
            else -> {
                val displayItems = uiState.results.map { it.toDisplayMediaItem() }
                MediaGrid(
                    items = displayItems,
                    columns = GridColumns.THREE,
                    onItemClick = { item -> onOpenViewer(displayItems.indexOf(item)) },
                    onItemLongClick = { }
                )
            }
        }
    }
}
