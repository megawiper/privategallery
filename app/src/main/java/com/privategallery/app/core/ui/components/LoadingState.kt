package com.privategallery.app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.privategallery.app.core.ui.Dimens

@Composable
fun GalleryFullScreenLoading(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            if (message != null) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(Dimens.SpaceS))
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun GalleryScanProgress(
    progress: Float?,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(Dimens.SpaceL)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = Dimens.SpaceS))
        if (progress != null) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
