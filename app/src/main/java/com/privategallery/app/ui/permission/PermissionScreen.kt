package com.privategallery.app.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privategallery.app.core.permissions.MediaPermissionState
import com.privategallery.app.core.permissions.MediaPermissionStatus
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.components.GalleryPrimaryButton
import com.privategallery.app.core.ui.components.GalleryTextActionButton

@Composable
fun PermissionScreen(
    onAccessGranted: (MediaPermissionStatus) -> Unit,
    viewModel: PermissionViewModel = viewModel()
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsStateWithLifecycle()
    val hasRequestedOnce by viewModel.hasRequestedOnce.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val resolved = MediaPermissionState.currentStatus(context)
        viewModel.onPermissionResult(resolved)
        if (resolved != MediaPermissionStatus.DENIED) {
            onAccessGranted(resolved)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.SpaceXL),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.padding(top = Dimens.SpaceL))
            Text(
                text = "Access your photos and videos",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.padding(top = Dimens.SpaceS))
            Text(
                text = "Private Gallery needs permission to show and organize the " +
                    "photos and videos already on your device. Nothing is uploaded anywhere.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hasRequestedOnce && status == MediaPermissionStatus.DENIED) {
                Spacer(modifier = Modifier.padding(top = Dimens.SpaceL))
                Text(
                    text = "Access was denied. You can grant it from system settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.padding(top = Dimens.SpaceXXL))

            if (hasRequestedOnce && status == MediaPermissionStatus.DENIED) {
                GalleryPrimaryButton(
                    text = "Open Settings",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                GalleryPrimaryButton(
                    text = "Grant Access",
                    onClick = { launcher.launch(MediaPermissionState.requiredPermissions()) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.padding(top = Dimens.SpaceS))
            GalleryTextActionButton(
                text = "Not now",
                onClick = { onAccessGranted(MediaPermissionStatus.DENIED) }
            )
        }
    }
}
