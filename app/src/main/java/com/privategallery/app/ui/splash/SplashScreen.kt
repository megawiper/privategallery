package com.privategallery.app.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons

@Composable
fun SplashScreen(
    onNavigateFirstTime: () -> Unit,
    onNavigateLocked: () -> Unit,
    onNavigateReturning: () -> Unit,
    viewModel: SplashViewModel = viewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        when (destination) {
            is SplashDestination.FirstTime -> onNavigateFirstTime()
            is SplashDestination.Locked -> onNavigateLocked()
            is SplashDestination.Returning -> onNavigateReturning()
            null -> Unit
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = GalleryIcons.Albums,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Box(modifier = Modifier.padding(top = Dimens.SpaceL)) {
                    Text(
                        text = "Private Gallery",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Box(modifier = Modifier.padding(top = Dimens.SpaceXXL)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}
