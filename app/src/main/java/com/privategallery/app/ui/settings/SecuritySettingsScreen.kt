package com.privategallery.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryIconButton
import com.privategallery.app.core.ui.components.GalleryTextActionButton
import com.privategallery.app.data.preferences.AuthMethod
import com.privategallery.app.data.preferences.AutoLockDuration
import com.privategallery.app.data.preferences.SecurityScope

@Composable
fun SecuritySettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val normalAuthMethod by viewModel.normalAuthMethod.collectAsStateWithLifecycle()
    val privateAuthMethod by viewModel.privateAuthMethod.collectAsStateWithLifecycle()
    val normalAutoLock by viewModel.normalAutoLock.collectAsStateWithLifecycle()
    val privateAutoLock by viewModel.privateAutoLock.collectAsStateWithLifecycle()

    var editingScope by remember { mutableStateOf<SecurityScope?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security") },
                navigationIcon = {
                    GalleryIconButton(icon = GalleryIcons.Back, contentDescription = "Back", onClick = onBack)
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { SectionLabel("Normal Gallery") }
            item {
                ListItem(
                    headlineContent = { Text("Lock method") },
                    supportingContent = { Text(normalAuthMethod.name) }
                )
            }
            item {
                GalleryTextActionButton(
                    text = "Change Normal Gallery Lock",
                    onClick = { editingScope = SecurityScope.NORMAL_GALLERY },
                    modifier = Modifier.padding(horizontal = Dimens.SpaceL)
                )
            }
            item {
                AutoLockRow(
                    current = normalAutoLock,
                    onSelect = { viewModel.setAutoLockDuration(SecurityScope.NORMAL_GALLERY, it) }
                )
            }

            item { SectionLabel("Private Gallery") }
            item {
                ListItem(
                    headlineContent = { Text("Lock method") },
                    supportingContent = { Text(privateAuthMethod.name) }
                )
            }
            item {
                GalleryTextActionButton(
                    text = "Change Private Gallery Lock",
                    onClick = { editingScope = SecurityScope.PRIVATE_GALLERY },
                    modifier = Modifier.padding(horizontal = Dimens.SpaceL)
                )
            }
            item {
                AutoLockRow(
                    current = privateAutoLock,
                    onSelect = { viewModel.setAutoLockDuration(SecurityScope.PRIVATE_GALLERY, it) }
                )
            }
        }
    }

    editingScope?.let { scope ->
        SetCredentialDialog(
            scope = scope,
            onDismiss = { editingScope = null },
            onConfirm = { method, credential ->
                viewModel.setCredential(scope, method, credential)
                editingScope = null
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpaceL, vertical = Dimens.SpaceS)
    )
}

@Composable
private fun AutoLockRow(
    current: AutoLockDuration,
    onSelect: (AutoLockDuration) -> Unit
) {
    ListItem(
        headlineContent = { Text("Auto-lock") },
        supportingContent = { Text(current.name) }
    )
    Column(modifier = Modifier.padding(horizontal = Dimens.SpaceL)) {
        AutoLockDuration.values().forEach { duration ->
            GalleryTextActionButton(text = duration.name, onClick = { onSelect(duration) })
        }
    }
}

@Composable
private fun SetCredentialDialog(
    scope: SecurityScope,
    onDismiss: () -> Unit,
    onConfirm: (AuthMethod, CharArray) -> Unit
) {
    var method by remember { mutableStateOf(AuthMethod.PIN) }
    var credential by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (scope == SecurityScope.PRIVATE_GALLERY) "Private Gallery Lock" else "Normal Gallery Lock")
        },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(AuthMethod.PIN, AuthMethod.PASSWORD, AuthMethod.PATTERN).forEach { option ->
                        GalleryTextActionButton(text = option.name, onClick = { method = option })
                    }
                }
                OutlinedTextField(
                    value = credential,
                    onValueChange = { credential = it },
                    label = { Text(method.name) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            GalleryTextActionButton(
                text = "Save",
                onClick = { if (credential.isNotBlank()) onConfirm(method, credential.toCharArray()) }
            )
        },
        dismissButton = {
            GalleryTextActionButton(text = "Cancel", onClick = onDismiss)
        }
    )
}
