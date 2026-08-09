package com.privategallery.app.ui.viewer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage

/**
 * STEP 22 — pinch zoom, double-tap zoom, and single-tap-to-toggle-controls,
 * all in one gesture-aware image composable so the viewer screen doesn't
 * need its own gesture math.
 */
@Composable
fun ZoomableImage(
    model: Any?,
    contentDescription: String?,
    onSingleTap: () -> Unit,
    onSwipeDownToClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var verticalDragAccumulated by remember { mutableFloatStateOf(0f) }

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offset = if (scale == 1f) Offset.Zero else offset + pan
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            }
            .pointerInput(scale) {
                if (scale == 1f) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (verticalDragAccumulated > SWIPE_DOWN_CLOSE_THRESHOLD) {
                                onSwipeDownToClose()
                            }
                            verticalDragAccumulated = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            if (dragAmount > 0) verticalDragAccumulated += dragAmount
                        }
                    )
                }
            }
            .scale(scale)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(offset.x.toInt(), offset.y.toInt())
                }
            }
    )
}

private const val SWIPE_DOWN_CLOSE_THRESHOLD = 300f
