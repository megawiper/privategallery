package com.privategallery.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryEmptyState
import com.privategallery.app.core.ui.components.GalleryFullScreenLoading
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.core.ui.components.GridColumns
import com.privategallery.app.core.ui.components.MediaGrid

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenViewer: (startIndex: Int) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChanged,
                        placeholder = { Text("Search photos and videos…") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.onSearchSubmitted() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterRow(
                filters = uiState.filters,
                onFiltersChanged = viewModel::onFiltersChanged
            )

            when {
                uiState.isSearching -> GalleryFullScreenLoading()
                uiState.query.isBlank() && uiState.results.isEmpty() -> {
                    RecentSearchesSection(
                        recentSearches = uiState.recentSearches,
                        onSelect = viewModel::onQueryChanged
                    )
                }
                uiState.results.isEmpty() -> {
                    GalleryEmptyState(
                        icon = GalleryIcons.Search,
                        title = "No results",
                        message = "Try a different search term or filter."
                    )
                }
                else -> {
                    MediaGrid(
                        items = uiState.results,
                        columns = GridColumns.THREE,
                        onItemClick = { item -> onOpenViewer(uiState.results.indexOf(item)) },
                        onItemLongClick = { }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    filters: SearchFilters,
    onFiltersChanged: (SearchFilters) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        contentPadding = PaddingValues(horizontal = Dimens.SpaceL, vertical = Dimens.SpaceS)
    ) {
        item {
            FilterChip(
                selected = filters.imagesOnly,
                onClick = { onFiltersChanged(filters.copy(imagesOnly = !filters.imagesOnly, videosOnly = false)) },
                label = { Text("Images") }
            )
        }
        item {
            FilterChip(
                selected = filters.videosOnly,
                onClick = { onFiltersChanged(filters.copy(videosOnly = !filters.videosOnly, imagesOnly = false)) },
                label = { Text("Videos") }
            )
        }
        item {
            FilterChip(
                selected = filters.favoritesOnly,
                onClick = { onFiltersChanged(filters.copy(favoritesOnly = !filters.favoritesOnly)) },
                label = { Text("Favorites") }
            )
        }
    }
}

@Composable
private fun RecentSearchesSection(
    recentSearches: List<String>,
    onSelect: (String) -> Unit
) {
    if (recentSearches.isEmpty()) {
        GalleryEmptyState(
            icon = GalleryIcons.Search,
            title = "Search your gallery",
            message = "Find photos and videos by name, date, month, year, or type."
        )
        return
    }
    Column(modifier = Modifier.padding(Dimens.SpaceL)) {
        Text(text = "Recent searches", style = MaterialTheme.typography.titleSmall)
        recentSearches.forEach { term ->
            Text(
                text = term,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(term) }
                    .padding(vertical = Dimens.SpaceS)
            )
        }
    }
}
