package com.privategallery.app.ui.editor

/**
 * STEP 34 — IMAGE EDITOR FOUNDATION
 *
 * The pipeline: Original -> temporary EditState -> preview -> Save.
 * Every field here is non-destructive — nothing is written back to the
 * original file until STEP 40's Save flow runs and verifies the result.
 * This single state object is what STEP 35 (crop), STEP 36 (adjustments),
 * STEP 37 (filters), and STEP 38 (drawing) all mutate, so undo/redo
 * (STEP 39) only needs to snapshot one object.
 */
data class CropState(
    val aspectRatio: CropAspectRatio = CropAspectRatio.FREE,
    val rotationDegrees: Int = 0,
    val flippedHorizontally: Boolean = false,
    val flippedVertically: Boolean = false,
    val straightenDegrees: Float = 0f,
    val cropRectNormalized: EditorRectF = EditorRectF(0f, 0f, 1f, 1f)
)

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    ORIGINAL("Original", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    THREE_FOUR("3:4", 3f / 4f),
    SIXTEEN_NINE("16:9", 16f / 9f),
    NINE_SIXTEEN("9:16", 9f / 16f)
}

data class EditorRectF(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class AdjustmentState(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val exposure: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val sharpness: Float = 0f,
    val clarity: Float = 0f,
    val fade: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f
)

data class FilterState(
    val filterId: String? = null,
    val intensity: Float = 1f
)

enum class DrawingTool { PEN, HIGHLIGHTER, TEXT, ARROW, RECTANGLE, CIRCLE }

data class DrawingElement(
    val id: String,
    val tool: DrawingTool,
    val pathPoints: List<Pair<Float, Float>> = emptyList(),
    val text: String? = null,
    val colorArgb: Long,
    val strokeWidth: Float,
    val opacity: Float = 1f
)

data class EditState(
    val crop: CropState = CropState(),
    val adjustments: AdjustmentState = AdjustmentState(),
    val filter: FilterState = FilterState(),
    val drawingElements: List<DrawingElement> = emptyList()
) {
    val isModified: Boolean
        get() = crop != CropState() ||
            adjustments != AdjustmentState() ||
            filter != FilterState() ||
            drawingElements.isNotEmpty()
}
