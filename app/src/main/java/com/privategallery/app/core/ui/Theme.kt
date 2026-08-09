package com.privategallery.app.core.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class GalleryThemeMode { LIGHT, DARK, SYSTEM }

private val LightColors = lightColorScheme(
    primary = GalleryPrimaryLight,
    onPrimary = GalleryOnPrimaryLight,
    primaryContainer = GalleryPrimaryContainerLight,
    onPrimaryContainer = GalleryOnPrimaryContainerLight,
    secondary = GallerySecondaryLight,
    onSecondary = GalleryOnSecondaryLight,
    secondaryContainer = GallerySecondaryContainerLight,
    onSecondaryContainer = GalleryOnSecondaryContainerLight,
    background = GalleryBackgroundLight,
    onBackground = GalleryOnBackgroundLight,
    surface = GallerySurfaceLight,
    onSurface = GalleryOnSurfaceLight,
    surfaceVariant = GallerySurfaceVariantLight,
    onSurfaceVariant = GalleryOnSurfaceVariantLight,
    error = GalleryErrorLight,
    onError = GalleryOnErrorLight,
    errorContainer = GalleryErrorContainerLight,
    onErrorContainer = GalleryOnErrorContainerLight,
    outline = GalleryOutlineLight
)

private val DarkColors = darkColorScheme(
    primary = GalleryPrimaryDark,
    onPrimary = GalleryOnPrimaryDark,
    primaryContainer = GalleryPrimaryContainerDark,
    onPrimaryContainer = GalleryOnPrimaryContainerDark,
    secondary = GallerySecondaryDark,
    onSecondary = GalleryOnSecondaryDark,
    secondaryContainer = GallerySecondaryContainerDark,
    onSecondaryContainer = GalleryOnSecondaryContainerDark,
    background = GalleryBackgroundDark,
    onBackground = GalleryOnBackgroundDark,
    surface = GallerySurfaceDark,
    onSurface = GalleryOnSurfaceDark,
    surfaceVariant = GallerySurfaceVariantDark,
    onSurfaceVariant = GalleryOnSurfaceVariantDark,
    error = GalleryErrorDark,
    onError = GalleryOnErrorDark,
    errorContainer = GalleryErrorContainerDark,
    onErrorContainer = GalleryOnErrorContainerDark,
    outline = GalleryOutlineDark
)

@Composable
fun PrivateGalleryTheme(
    themeMode: GalleryThemeMode = GalleryThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        GalleryThemeMode.LIGHT -> false
        GalleryThemeMode.DARK -> true
        GalleryThemeMode.SYSTEM -> systemDark
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GalleryTypography,
        shapes = GalleryShapes,
        content = content
    )
}
