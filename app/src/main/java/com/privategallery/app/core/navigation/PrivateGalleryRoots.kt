package com.privategallery.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privategallery.app.core.di.AppContainer
import com.privategallery.app.core.di.SimpleViewModelFactory
import com.privategallery.app.core.ui.components.GalleryFullScreenLoading
import com.privategallery.app.domain.repository.PrivateGalleryRepository
import com.privategallery.app.ui.privategallery.PrivateHomeScreen
import com.privategallery.app.ui.privategallery.PrivateHomeViewModel
import com.privategallery.app.ui.privategallery.PrivateImportScreen
import com.privategallery.app.ui.privategallery.PrivateImportViewModel
import com.privategallery.app.ui.privategallery.PrivateSearchScreen
import com.privategallery.app.ui.privategallery.PrivateSearchViewModel
import com.privategallery.app.ui.privategallery.PrivateVideoPlayerScreen
import com.privategallery.app.ui.privategallery.PrivateVideoPlayerViewModel
import com.privategallery.app.ui.privategallery.PrivateViewerScreen
import com.privategallery.app.ui.privategallery.PrivateViewerViewModel

/**
 * PrivateGalleryEncryptedDatabase.getInstance (STEP 52) is suspend
 * because opening it requires a DataStore-backed passphrase read.
 * SimpleViewModelFactory's create() is synchronous, so every private-
 * gallery destination resolves the repository here first and shows a
 * brief loading state, then builds its real ViewModel once the
 * (cached-after-first-call) repository is available.
 */
@Composable
private fun rememberPrivateRepositoryOrNull(appContainer: AppContainer): PrivateGalleryRepository? {
    var repository by remember { mutableStateOf<PrivateGalleryRepository?>(null) }
    LaunchedEffect(Unit) {
        repository = appContainer.getPrivateGalleryRepository()
    }
    return repository
}

@Composable
fun PrivateGalleryRoot(
    appContainer: AppContainer,
    onOpenViewer: (startIndex: Int, collection: String) -> Unit,
    onOpenImport: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAlbum: (albumId: String) -> Unit,
    onLockNow: () -> Unit,
    onSwitchToNormalGallery: () -> Unit
) {
    val repository = rememberPrivateRepositoryOrNull(appContainer) ?: run {
        GalleryFullScreenLoading()
        return
    }
    val viewModel: PrivateHomeViewModel = viewModel(
        factory = SimpleViewModelFactory { PrivateHomeViewModel(repository) }
    )
    PrivateHomeScreen(
        viewModel = viewModel,
        onOpenViewer = onOpenViewer,
        onOpenImport = onOpenImport,
        onOpenSearch = onOpenSearch,
        onOpenAlbum = onOpenAlbum,
        onLockNow = onLockNow,
        onSwitchToNormalGallery = onSwitchToNormalGallery
    )
}

@Composable
fun PrivateViewerRoot(
    appContainer: AppContainer,
    collection: String,
    startIndex: Int,
    onBack: () -> Unit
) {
    val repository = rememberPrivateRepositoryOrNull(appContainer) ?: run {
        GalleryFullScreenLoading()
        return
    }
    val viewModel: PrivateViewerViewModel = viewModel(
        factory = SimpleViewModelFactory { PrivateViewerViewModel(repository, collection, startIndex) }
    )
    PrivateViewerScreen(viewModel = viewModel, onBack = onBack)
}

/**
 * startSecureId is resolved by index rather than passed directly through
 * navigation, keeping with STEP 4's rule against passing large/complex
 * objects through nav args — the video list itself is re-fetched here
 * from the repository, and the caller's chosen index picks the start
 * item, mirroring how ImageViewer resolves its source collection
 * (STEP 23) rather than carrying a secureId string through the graph.
 */
@Composable
fun PrivateVideoViewerRoot(
    appContainer: AppContainer,
    startIndex: Int,
    onBack: () -> Unit
) {
    val repository = rememberPrivateRepositoryOrNull(appContainer) ?: run {
        GalleryFullScreenLoading()
        return
    }
    val context = LocalContext.current
    var resolvedStartId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository) {
        val videos = kotlinx.coroutines.flow.first(repository.observeVideos())
        resolvedStartId = videos.getOrNull(startIndex)?.secureId ?: videos.firstOrNull()?.secureId
    }

    val startId = resolvedStartId ?: run {
        GalleryFullScreenLoading()
        return
    }

    val viewModel: PrivateVideoPlayerViewModel = viewModel(
        factory = SimpleViewModelFactory {
            PrivateVideoPlayerViewModel(
                context = context,
                repository = repository,
                storageManager = appContainer.privateStorageManagerForImageLoading(),
                startSecureId = startId
            )
        }
    )
    PrivateVideoPlayerScreen(viewModel = viewModel, onBack = onBack)
}

@Composable
fun PrivateImportRoot(
    appContainer: AppContainer,
    onBack: () -> Unit,
    onImportComplete: () -> Unit
) {
    val repository = rememberPrivateRepositoryOrNull(appContainer) ?: run {
        GalleryFullScreenLoading()
        return
    }
    val viewModel: PrivateImportViewModel = viewModel(
        factory = SimpleViewModelFactory { PrivateImportViewModel(repository) }
    )
    PrivateImportScreen(viewModel = viewModel, onBack = onBack, onImportComplete = onImportComplete)
}

@Composable
fun PrivateSearchRoot(
    appContainer: AppContainer,
    onOpenViewer: (startIndex: Int) -> Unit,
    onBack: () -> Unit
) {
    val repository = rememberPrivateRepositoryOrNull(appContainer) ?: run {
        GalleryFullScreenLoading()
        return
    }
    val viewModel: PrivateSearchViewModel = viewModel(
        factory = SimpleViewModelFactory { PrivateSearchViewModel(repository) }
    )
    PrivateSearchScreen(viewModel = viewModel, onOpenViewer = onOpenViewer, onBack = onBack)
}
