package com.privategallery.app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.privategallery.app.core.ui.Dimens

@Composable
fun GalleryEmptyState(
    icon: ImageVector,
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Dimens.SpaceXXL)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = Dimens.SpaceL))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (message != null) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = Dimens.SpaceXS))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            if (actionLabel != null && onAction != null) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = Dimens.SpaceL))
                GalleryPrimaryButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun GalleryErrorState(
    title: String,
    message: String? = null,
    retryLabel: String = "Retry",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Dimens.SpaceXXL)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            if (message != null) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = Dimens.SpaceXS))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onRetry != null) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = Dimens.SpaceL))
                GallerySecondaryButton(text = retryLabel, onClick = onRetry)
            }
        }
    }
}
