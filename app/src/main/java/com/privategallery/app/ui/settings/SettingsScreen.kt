package com.privategallery.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.data.preferences.SecurityScope

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenSecuritySettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val normalAuthMethod by viewModel.normalAuthMethod.collectAsStateWithLifecycle()
    val privateAuthMethod by viewModel.privateAuthMethod.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    GalleryIconButton(icon = GalleryIcons.Back, contentDescription = "Back", onClick = onBack)
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { SectionHeader("Gallery") }
            item {
                ListItem(
                    headlineContent = { Text("Grid size") },
                    supportingContent = { Text(uiState.gridSize.name) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Sort order") },
                    supportingContent = { Text(uiState.sortOrder.name) }
                )
            }
            item {
                SwitchRow(
                    title = "Show file names",
                    checked = uiState.showFileNames,
                    onCheckedChange = viewModel::setShowFileNames
                )
            }
            item {
                SwitchRow(
                    title = "Show video duration",
                    checked = uiState.showVideoDuration,
                    onCheckedChange = viewModel::setShowVideoDuration
                )
            }
            item {
                SwitchRow(
                    title = "Animations",
                    checked = uiState.animationsEnabled,
                    onCheckedChange = viewModel::setAnimationsEnabled
                )
            }

            item { SectionHeader("Security") }
            item {
                ListItem(
                    headlineContent = { Text("Normal Gallery Lock") },
                    supportingContent = { Text(normalAuthMethod.name) },
                    modifier = Modifier.clickable(onClick = onOpenSecuritySettings)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Private Gallery Lock") },
                    supportingContent = { Text(privateAuthMethod.name) },
                    modifier = Modifier.clickable(onClick = onOpenSecuritySettings)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Lock Now") },
                    modifier = Modifier.clickable { viewModel.lockNow(SecurityScope.PRIVATE_GALLERY) }
                )
            }
            item {
                SwitchRow(
                    title = "Screenshot protection",
                    checked = uiState.screenshotProtectionEnabled,
                    onCheckedChange = viewModel::setScreenshotProtectionEnabled
                )
            }

            item { SectionHeader("Media") }
            item {
                ListItem(
                    headlineContent = { Text("Scan / Refresh") },
                    modifier = Modifier.clickable { viewModel.refreshMediaLibrary() }
                )
            }

            item { SectionHeader("Trash") }
            item {
                ListItem(
                    headlineContent = { Text("Retention duration") },
                    supportingContent = { Text("${uiState.trashRetentionDays} days") }
                )
            }

            item { SectionHeader("Editing") }
            item {
                SwitchRow(
                    title = "Replace original by default",
                    checked = uiState.replaceOriginalOnEdit,
                    onCheckedChange = viewModel::setReplaceOriginalOnEdit
                )
            }
            item {
                SwitchRow(
                    title = "Preserve metadata",
                    checked = uiState.preserveMetadata,
                    onCheckedChange = viewModel::setPreserveMetadata
                )
            }

            item { SectionHeader("Appearance") }
            item {
                ListItem(
                    headlineContent = { Text("Theme") },
                    supportingContent = { Text(uiState.theme.name) }
                )
            }

            item { SectionHeader("About") }
            item {
                ListItem(
                    headlineContent = { Text("App version") },
                    supportingContent = { Text("1.0") }
                )
            }
            item { ListItem(headlineContent = { Text("Privacy information") }) }
            item { ListItem(headlineContent = { Text("Open-source licenses") }) }
            item { ListItem(headlineContent = { Text("Feedback") }) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpaceL, vertical = Dimens.SpaceS)
    )
    HorizontalDivider()
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}
