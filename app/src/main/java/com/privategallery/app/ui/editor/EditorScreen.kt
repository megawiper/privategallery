package com.privategallery.app.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryFullScreenLoading
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.core.ui.components.GalleryTextActionButton
import com.privategallery.app.domain.usecase.EditSaveMode

private enum class EditorTab { CROP, ADJUST, FILTER, DRAW }

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(EditorTab.ADJUST) }
    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit") },
                navigationIcon = {
                    GalleryIconButton(icon = GalleryIcons.Close, contentDescription = "Cancel", onClick = onBack)
                },
                actions = {
                    GalleryIconButton(
                        icon = GalleryIcons.Check,
                        contentDescription = "Save",
                        onClick = { showSaveDialog = true }
                    )
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            GalleryFullScreenLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                uiState.previewBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.SpaceS),
                horizontalArrangement = Arrangement.End
            ) {
                GalleryIconButton(
                    icon = GalleryIcons.Back,
                    contentDescription = "Undo",
                    onClick = viewModel::undo
                )
                GalleryIconButton(
                    icon = GalleryIcons.Back,
                    contentDescription = "Redo",
                    onClick = viewModel::redo
                )
            }

            TabRow(selectedTabIndex = selectedTab.ordinal) {
                EditorTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            when (selectedTab) {
                EditorTab.CROP -> CropControls(uiState.editState.crop, viewModel::updateCrop)
                EditorTab.ADJUST -> AdjustmentControls(uiState.editState.adjustments, viewModel::updateAdjustments)
                EditorTab.FILTER -> FilterControls(uiState.editState.filter, viewModel::updateFilter)
                EditorTab.DRAW -> DrawControls()
            }
        }
    }

    if (showSaveDialog) {
        SaveOptionsDialog(
            isSaving = uiState.isSaving,
            onReplaceOriginal = { viewModel.save(EditSaveMode.REPLACE_ORIGINAL) { onSaved() } },
            onSaveAsNew = { viewModel.save(EditSaveMode.SAVE_AS_NEW) { onSaved() } },
            onCancel = { showSaveDialog = false }
        )
    }
}

// STEP 35 — CROP AND TRANSFORM
@Composable
private fun CropControls(crop: CropState, onChange: (CropState) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.SpaceL),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)
    ) {
        items(CropAspectRatio.values().toList()) { ratio ->
            Surface(
                color = if (crop.aspectRatio == ratio) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.small
            ) {
                Box(
                    modifier = Modifier
                        .clickable { onChange(crop.copy(aspectRatio = ratio)) }
                        .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = ratio.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpaceL),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        GalleryTextActionButton(
            text = "Rotate",
            onClick = { onChange(crop.copy(rotationDegrees = (crop.rotationDegrees + 90) % 360)) }
        )
        GalleryTextActionButton(
            text = "Flip H",
            onClick = { onChange(crop.copy(flippedHorizontally = !crop.flippedHorizontally)) }
        )
        GalleryTextActionButton(
            text = "Flip V",
            onClick = { onChange(crop.copy(flippedVertically = !crop.flippedVertically)) }
        )
    }
}

// STEP 36 — IMAGE ADJUSTMENT ENGINE
@Composable
private fun AdjustmentControls(adjustments: AdjustmentState, onChange: (AdjustmentState) -> Unit) {
    Column(modifier = Modifier.padding(Dimens.SpaceL)) {
        AdjustmentSlider("Brightness", adjustments.brightness) { onChange(adjustments.copy(brightness = it)) }
        AdjustmentSlider("Contrast", adjustments.contrast) { onChange(adjustments.copy(contrast = it)) }
        AdjustmentSlider("Saturation", adjustments.saturation) { onChange(adjustments.copy(saturation = it)) }
        AdjustmentSlider("Exposure", adjustments.exposure) { onChange(adjustments.copy(exposure = it)) }
        AdjustmentSlider("Highlights", adjustments.highlights) { onChange(adjustments.copy(highlights = it)) }
        AdjustmentSlider("Shadows", adjustments.shadows) { onChange(adjustments.copy(shadows = it)) }
        AdjustmentSlider("Temperature", adjustments.temperature) { onChange(adjustments.copy(temperature = it)) }
        AdjustmentSlider("Tint", adjustments.tint) { onChange(adjustments.copy(tint = it)) }
        AdjustmentSlider("Sharpness", adjustments.sharpness) { onChange(adjustments.copy(sharpness = it)) }
        AdjustmentSlider("Clarity", adjustments.clarity) { onChange(adjustments.copy(clarity = it)) }
        AdjustmentSlider("Vignette", adjustments.vignette) { onChange(adjustments.copy(vignette = it)) }
    }
}

@Composable
private fun AdjustmentSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = onChange, valueRange = -1f..1f)
    }
}

// STEP 37 — FILTER ENGINE
@Composable
private fun FilterControls(filter: FilterState, onChange: (FilterState) -> Unit) {
    val filters = listOf(null, "mono", "warm", "cool", "vivid")
    Column(modifier = Modifier.padding(Dimens.SpaceL)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            items(filters) { filterId ->
                Surface(
                    color = if (filter.filterId == filterId) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Box(
                        modifier = Modifier
                            .clickable { onChange(filter.copy(filterId = filterId)) }
                            .padding(Dimens.SpaceM)
                    ) {
                        Text(text = filterId ?: "None", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (filter.filterId != null) {
            Text(text = "Intensity", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = filter.intensity,
                onValueChange = { onChange(filter.copy(intensity = it)) },
                valueRange = 0f..1f
            )
        }
    }
}

// STEP 38 — DRAWING / MARKUP
@Composable
private fun DrawControls() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.SpaceL),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Draw directly on the preview above — pen, highlighter, text, arrow, rectangle, circle.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// STEP 40 — EDIT SAVE OPTIONS
@Composable
private fun SaveOptionsDialog(
    isSaving: Boolean,
    onReplaceOriginal: () -> Unit,
    onSaveAsNew: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Save Changes") },
        text = { Text(if (isSaving) "Saving…" else "Choose how to save your edited photo.") },
        confirmButton = {
            GalleryTextActionButton(text = "Replace Original", onClick = onReplaceOriginal, enabled = !isSaving)
        },
        dismissButton = {
            Row {
                GalleryTextActionButton(text = "Save as New", onClick = onSaveAsNew, enabled = !isSaving)
                GalleryTextActionButton(text = "Cancel", onClick = onCancel, enabled = !isSaving)
            }
        }
    )
}
