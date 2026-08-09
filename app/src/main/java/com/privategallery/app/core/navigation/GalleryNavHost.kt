package com.privategallery.app.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.privategallery.app.core.di.AppContainer
import com.privategallery.app.core.di.SimpleViewModelFactory
import com.privategallery.app.ui.albums.AlbumsScreen
import com.privategallery.app.ui.albums.AlbumsViewModel
import com.privategallery.app.ui.favorites.FavoritesScreen
import com.privategallery.app.ui.favorites.FavoritesViewModel
import com.privategallery.app.ui.home.HomeScreen
import com.privategallery.app.ui.home.HomeViewModel
import com.privategallery.app.ui.onboarding.WelcomeScreen
import com.privategallery.app.ui.permission.PermissionScreen
import com.privategallery.app.ui.search.SearchScreen
import com.privategallery.app.ui.search.SearchViewModel
import com.privategallery.app.ui.splash.SplashScreen
import com.privategallery.app.ui.video.VideoPlayerScreen
import com.privategallery.app.ui.video.VideoPlayerViewModel
import com.privategallery.app.ui.viewer.ImageViewerScreen
import com.privategallery.app.ui.viewer.ImageViewerViewModel

/**
 * STEP 4 — NAVIGATION ARCHITECTURE
 *
 * The full destination graph is declared here up front, per the roadmap.
 * Each composable body is a temporary scaffold screen until its owning
 * step (5, 6, 7, 11, ...) is implemented. This lets navigation between
 * every screen be verified before feature logic exists, and later
 * steps only need to replace the scaffold body — not touch this graph.
 */
@Composable
fun GalleryNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destination.Splash.route
) {
    // STEP 49/61 — on every resume, check whether Private Gallery's
    // auto-lock window has elapsed while backgrounded; if so, force
    // back to the Lock screen rather than leaving private content
    // reachable. Normal Gallery is checked the same way if it has a
    // configured lock (STEP 46).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val currentRoute = navController.currentDestination?.route.orEmpty()
                val isOnPrivateScreen = currentRoute.startsWith("private_") ||
                    currentRoute == Destination.PrivateGalleryHome.route
                if (isOnPrivateScreen) {
                    val scope = com.privategallery.app.data.preferences.SecurityScope.PRIVATE_GALLERY
                    val relockNeeded = com.privategallery.app.core.security.AuthSessionManager
                        .shouldRelockOnForeground(
                            scope = scope,
                            nowMillis = System.currentTimeMillis(),
                            autoLockMillis = 0L // Immediately — Private Gallery's default (STEP 49)
                        )
                    if (relockNeeded) {
                        com.privategallery.app.core.security.AuthSessionManager.lock(scope)
                        navController.navigate(Destination.Lock.createRoute(isPrivate = true)) {
                            popUpTo(Destination.PrivateGalleryHome.route) { inclusive = true }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) }
    ) {
        composable(Destination.Splash.route) {
            val splashViewModel: SplashViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = SimpleViewModelFactory {
                    SplashViewModel(
                        appPreferences = appContainer.appPreferences,
                        securityPreferences = appContainer.securityPreferences
                    )
                }
            )
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateFirstTime = {
                    navController.navigate(Destination.Welcome.route) {
                        popUpTo(Destination.Splash.route) { inclusive = true }
                    }
                },
                onNavigateLocked = {
                    navController.navigate(Destination.Lock.createRoute(isPrivate = false)) {
                        popUpTo(Destination.Splash.route) { inclusive = true }
                    }
                },
                onNavigateReturning = {
                    navController.navigate(Destination.Home.route) {
                        popUpTo(Destination.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.Welcome.route) {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            WelcomeScreen(
                onContinue = {
                    scope.launch { appContainer.appPreferences.setOnboardingComplete(true) }
                    navController.navigate(Destination.Permission.route)
                },
                onSkipSetup = {
                    scope.launch { appContainer.appPreferences.setOnboardingComplete(true) }
                    navController.navigate(Destination.Home.route) {
                        popUpTo(Destination.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.Permission.route) {
            PermissionScreen(
                onAccessGranted = {
                    navController.navigate(Destination.Home.route) {
                        popUpTo(Destination.Permission.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.Home.route) {
            val homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = SimpleViewModelFactory { HomeViewModel(appContainer.mediaRepository) }
            )
            HomeScreen(
                viewModel = homeViewModel,
                onOpenViewer = { sourceCollection, startIndex ->
                    navController.navigate(
                        Destination.ImageViewer.createRoute(sourceCollection, startIndex)
                    )
                },
                onOpenAlbums = { navController.navigate(Destination.Albums.route) },
                onOpenSearch = { navController.navigate(Destination.Search.route) },
                onOpenPrivateGallery = {
                    navController.navigate(Destination.Lock.createRoute(isPrivate = true))
                },
                onOpenSettings = { navController.navigate(Destination.Settings.route) }
            )
        }
        composable(Destination.Albums.route) {
            val albumsViewModel: AlbumsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = SimpleViewModelFactory { AlbumsViewModel(appContainer.mediaRepository) }
            )
            AlbumsScreen(
                viewModel = albumsViewModel,
                onOpenAutomaticAlbum = { key ->
                    navController.navigate(Destination.AlbumDetails.createRoute(key))
                },
                onOpenCustomAlbum = { albumId ->
                    navController.navigate(Destination.AlbumDetails.createRoute(albumId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Destination.AlbumDetails.route,
            arguments = listOf(navArgument(NavArgs.ALBUM_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString(NavArgs.ALBUM_ID).orEmpty()
            ScaffoldScreen("Album Details: $albumId")
        }
        composable(Destination.Favorites.route) {
            val favoritesViewModel: FavoritesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = SimpleViewModelFactory { FavoritesViewModel(appContainer.mediaRepository) }
            )
            FavoritesScreen(
                viewModel = favoritesViewModel,
                onOpenViewer = { startIndex ->
                    navController.navigate(Destination.ImageViewer.createRoute("favorites", startIndex))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Destination.Search.route) {
            val searchViewModel: SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = SimpleViewModelFactory { SearchViewModel(appContainer.mediaRepository) }
            )
            SearchScreen(
                viewModel = searchViewModel,
                onOpenViewer = { startIndex ->
                    navController.navigate(Destination.ImageViewer.createRoute("home", startIndex))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Destination.ImageViewer.route,
            arguments = listOf(
                navArgument(NavArgs.SOURCE_COLLECTION) { type = NavType.StringType },
                navArgument(NavArgs.VIEWER_START_INDEX) { type = NavType.IntType },
                navArgument(NavArgs.IS_PRIVATE) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val sourceCollection = backStackEntry.arguments?.getString(NavArgs.SOURCE_COLLECTION).orEmpty()
            val startIndex = backStackEntry.arguments?.getInt(NavArgs.VIEWER_START_INDEX) ?: 0
            val viewerViewModel: ImageViewerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = SimpleViewModelFactory {
                    ImageViewerViewModel(
                        mediaRepository = appContainer.mediaRepository,
                        getMediaCollectionUseCase = appContainer.getMediaCollectionUseCase,
                        sourceCollection = sourceCollection,
                        startIndex = startIndex
                    )
                }
            )
            ImageViewerScreen(
                viewModel = viewerViewModel,
                onBack = { navController.popBackStack() },
                onOpenEditor = { mediaId ->
                    navController.navigate(Destination.Editor.createRoute(mediaId.toString()))
                },
                onOpenInfo = { mediaId ->
                    navController.navigate(Destination.MediaInfo.createRoute(mediaId.toString()))
                },
                onShare = { mediaId ->
                    val item = viewerViewModel.uiState.value.items.firstOrNull { it.id == mediaId }
                    if (item != null) {
                        com.privategallery.app.core.storage.ShareHelper.shareSingle(
                            context = navController.context,
                            uri = android.net.Uri.parse(item.uriString),
                            mimeType = item.mimeType
                        )
                    }
                },
                onAddToAlbum = { }
            )
        }
        composable(
            route = Destination.VideoViewer.route,
            arguments = listOf(
                navArgument(NavArgs.MEDIA_ID) { type = NavType.StringType },
                navArgument(NavArgs.IS_PRIVATE) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val backStackEntry = navController.currentBackStackEntry
            val mediaId = backStackEntry?.arguments?.getString(NavArgs.MEDIA_ID)?.toLongOrNull() ?: 0L
            val videoViewModel: VideoPlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = SimpleViewModelFactory {
                    VideoPlayerViewModel(
                        context = context,
                        mediaRepository = appContainer.mediaRepository,
                        getMediaCollectionUseCase = appContainer.getMediaCollectionUseCase,
                        sourceCollection = "home",
                        startMediaId = mediaId
                    )
                }
            )
            VideoPlayerScreen(
                viewModel = videoViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Destination.Editor.route,
            arguments = listOf(navArgument(NavArgs.MEDIA_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val context = androidx.compose.ui.platform.LocalContext.current
            val mediaId = backStackEntry.arguments?.getString(NavArgs.MEDIA_ID)?.toLongOrNull() ?: 0L
            val editorViewModel: com.privategallery.app.ui.editor.EditorViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = SimpleViewModelFactory {
                        com.privategallery.app.ui.editor.EditorViewModel(
                            context = context,
                            mediaRepository = appContainer.mediaRepository,
                            saveEditedImageUseCase = appContainer.saveEditedImageUseCase,
                            mediaId = mediaId
                        )
                    }
                )
            com.privategallery.app.ui.editor.EditorScreen(
                viewModel = editorViewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            route = Destination.MediaInfo.route,
            arguments = listOf(
                navArgument(NavArgs.MEDIA_ID) { type = NavType.StringType },
                navArgument(NavArgs.IS_PRIVATE) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString(NavArgs.MEDIA_ID)?.toLongOrNull() ?: 0L
            val infoViewModel: com.privategallery.app.ui.viewer.MediaInfoViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = SimpleViewModelFactory {
                        com.privategallery.app.ui.viewer.MediaInfoViewModel(
                            mediaRepository = appContainer.mediaRepository,
                            mediaId = mediaId
                        )
                    }
                )
            com.privategallery.app.ui.viewer.MediaInfoScreen(
                viewModel = infoViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Destination.Trash.route) {
            val trashViewModel: com.privategallery.app.ui.trash.TrashViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = SimpleViewModelFactory {
                        com.privategallery.app.ui.trash.TrashViewModel(appContainer.mediaRepository)
                    }
                )
            com.privategallery.app.ui.trash.TrashScreen(
                viewModel = trashViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Destination.Duplicates.route) {
            val duplicatesViewModel: com.privategallery.app.ui.duplicates.DuplicatesViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = SimpleViewModelFactory {
                        com.privategallery.app.ui.duplicates.DuplicatesViewModel(
                            mediaRepository = appContainer.mediaRepository,
                            duplicateDetectionEngine = appContainer.duplicateDetectionEngine
                        )
                    }
                )
            com.privategallery.app.ui.duplicates.DuplicatesScreen(
                viewModel = duplicatesViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Destination.Settings.route) {
            val settingsViewModel: com.privategallery.app.ui.settings.SettingsViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = SimpleViewModelFactory {
                        com.privategallery.app.ui.settings.SettingsViewModel(
                            appPreferences = appContainer.appPreferences,
                            securityPreferences = appContainer.securityPreferences,
                            mediaRepository = appContainer.mediaRepository
                        )
                    }
                )
            com.privategallery.app.ui.settings.SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onOpenSecuritySettings = { navController.navigate(Destination.SecuritySettings.route) }
            )
        }
        composable(Destination.SecuritySettings.route) {
            val settingsViewModel: com.privategallery.app.ui.settings.SettingsViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = SimpleViewModelFactory {
                        com.privategallery.app.ui.settings.SettingsViewModel(
                            appPreferences = appContainer.appPreferences,
                            securityPreferences = appContainer.securityPreferences,
                            mediaRepository = appContainer.mediaRepository
                        )
                    }
                )
            com.privategallery.app.ui.settings.SecuritySettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Destination.Lock.route,
            arguments = listOf(
                navArgument(NavArgs.IS_PRIVATE) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val isPrivate = backStackEntry.arguments?.getBoolean(NavArgs.IS_PRIVATE) ?: false
            val scope = if (isPrivate) {
                com.privategallery.app.data.preferences.SecurityScope.PRIVATE_GALLERY
            } else {
                com.privategallery.app.data.preferences.SecurityScope.NORMAL_GALLERY
            }
            val lockViewModel: com.privategallery.app.ui.lock.LockViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = SimpleViewModelFactory {
                        com.privategallery.app.ui.lock.LockViewModel(
                            securityPreferences = appContainer.securityPreferences,
                            scope = scope
                        )
                    }
                )
            com.privategallery.app.ui.lock.LockScreen(
                viewModel = lockViewModel,
                isPrivate = isPrivate,
                onUnlocked = {
                    val destination = if (isPrivate) {
                        Destination.PrivateGalleryHome.route
                    } else {
                        Destination.Home.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Destination.Lock.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.PrivateGalleryHome.route) {
            PrivateGalleryRoot(
                appContainer = appContainer,
                onOpenViewer = { startIndex, collection ->
                    val isVideoCollection = collection == "private_videos"
                    if (isVideoCollection) {
                        navController.navigate("private_video_viewer/$startIndex")
                    } else {
                        navController.navigate("private_image_viewer/$collection/$startIndex")
                    }
                },
                onOpenImport = { navController.navigate("private_import") },
                onOpenSearch = { navController.navigate(Destination.PrivateSearch.route) },
                onOpenAlbum = { albumId ->
                    navController.navigate(Destination.PrivateAlbumDetails.createRoute(albumId))
                },
                onLockNow = {
                    navController.navigate(Destination.Home.route) {
                        popUpTo(Destination.PrivateGalleryHome.route) { inclusive = true }
                    }
                },
                onSwitchToNormalGallery = {
                    navController.navigate(Destination.Home.route) {
                        popUpTo(Destination.PrivateGalleryHome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "private_image_viewer/{collection}/{startIndex}",
            arguments = listOf(
                navArgument("collection") { type = NavType.StringType },
                navArgument("startIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val collection = backStackEntry.arguments?.getString("collection") ?: "private_images"
            val startIndex = backStackEntry.arguments?.getInt("startIndex") ?: 0
            PrivateViewerRoot(
                appContainer = appContainer,
                collection = collection,
                startIndex = startIndex,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "private_video_viewer/{startIndex}",
            arguments = listOf(navArgument("startIndex") { type = NavType.IntType })
        ) { backStackEntry ->
            val startIndex = backStackEntry.arguments?.getInt("startIndex") ?: 0
            PrivateVideoViewerRoot(
                appContainer = appContainer,
                startIndex = startIndex,
                onBack = { navController.popBackStack() }
            )
        }
        composable("private_import") {
            PrivateImportRoot(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
                onImportComplete = { navController.popBackStack() }
            )
        }
        composable(
            route = Destination.PrivateAlbumDetails.route,
            arguments = listOf(navArgument(NavArgs.ALBUM_ID) { type = NavType.StringType })
        ) {
            ScaffoldScreen("Private Album Details")
        }
        composable(Destination.PrivateSearch.route) {
            PrivateSearchRoot(
                appContainer = appContainer,
                onOpenViewer = { startIndex ->
                    navController.navigate("private_image_viewer/private_images/$startIndex")
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun ScaffoldScreen(label: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = label)
        }
    }
}
