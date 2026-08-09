package com.privategallery.app.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val GalleryShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Purpose-specific shapes used across grids, sheets, and viewers
val ThumbnailShape = RoundedCornerShape(6.dp)
val AlbumCoverShape = RoundedCornerShape(10.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
val ChipShape = RoundedCornerShape(percent = 50)
