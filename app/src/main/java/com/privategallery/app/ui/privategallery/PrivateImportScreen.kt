package com.privategallery.app.ui.privategallery

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryEmptyState
import com.privategallery.app.core.ui.components.GalleryIconButton

@Composable
fun PrivateImportScreen(
    viewModel: PrivateImportViewModel,
    onBack: () -> Unit,
    onImportComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val candidates = uris.map { uri -> uri.toImportCandidate(context) }
            viewModel.importCandidates(candidates)
        }
    }

    LaunchedEffect(uiState.isImporting, uiState.totalCount) {
        if (!uiState.isImporting && uiState.totalCount > 0) {
            onImportComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import") },
                navigationIcon = {
                    GalleryIconButton(icon = GalleryIcons.Back, contentDescription = "Back", onClick = onBack)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isImporting) {
                Column(modifier = Modifier.padding(Dimens.SpaceXL)) {
                    Text(
                        text = "Importing ${uiState.importedCount + uiState.failedCount} of ${uiState.totalCount}…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.padding(top = Dimens.SpaceS))
                    LinearProgressIndicator(
                        progress = {
                            (uiState.importedCount + uiState.failedCount).toFloat() /
                                uiState.totalCount.coerceAtLeast(1)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                GalleryEmptyState(
                    icon = GalleryIcons.Add,
                    title = "Import photos and videos",
                    message = "Securely copy media into your Private Gallery. Nothing leaves your device.",
                    actionLabel = "Choose Media",
                    onAction = {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                )
            }
        }
    }
}

private fun Uri.toImportCandidate(context: Context): ImportCandidate {
    val mimeType = context.contentResolver.getType(this) ?: "image/*"
    val isVideo = mimeType.startsWith("video")
    val displayName = queryDisplayName(context) ?: "imported_${System.currentTimeMillis()}"
    return ImportCandidate(uri = this, displayName = displayName, mimeType = mimeType, isVideo = isVideo)
}

private fun Uri.queryDisplayName(context: Context): String? {
    return try {
        context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameCol >= 0) cursor.getString(nameCol) else null
        }
    } catch (e: Exception) {
        null
    }
}
