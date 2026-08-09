package com.privategallery.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.components.GalleryPrimaryButton
import com.privategallery.app.core.ui.components.GalleryTextActionButton

private data class WelcomeHighlight(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val highlights = listOf(
    WelcomeHighlight(
        icon = Icons.Filled.PhotoLibrary,
        title = "One gallery for everything",
        description = "Browse your photos and videos, organize albums, and find what you need fast."
    ),
    WelcomeHighlight(
        icon = Icons.Filled.Lock,
        title = "A private space, separately locked",
        description = "Move sensitive photos and videos into a Private Gallery with its own authentication."
    ),
    WelcomeHighlight(
        icon = Icons.Filled.Security,
        title = "Secured on your device",
        description = "PIN, password, pattern, or biometric protection — your credentials never leave your device."
    )
)

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onSkipSetup: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.SpaceXL),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.padding(top = Dimens.SpaceXXL))
                Text(
                    text = "Welcome to Private Gallery",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.padding(top = Dimens.SpaceXXL))
                highlights.forEach { highlight ->
                    WelcomeHighlightRow(highlight)
                    Spacer(modifier = Modifier.padding(top = Dimens.SpaceXL))
                }
            }

            Column {
                GalleryPrimaryButton(
                    text = "Continue",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.padding(top = Dimens.SpaceS))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GalleryTextActionButton(
                        text = "Set up later",
                        onClick = onSkipSetup
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeHighlightRow(highlight: WelcomeHighlight) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = highlight.icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(Dimens.SpaceL))
        Column {
            Text(text = highlight.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.padding(top = Dimens.SpaceXXS))
            Text(
                text = highlight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
