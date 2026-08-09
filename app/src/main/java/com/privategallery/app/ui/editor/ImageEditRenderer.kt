package com.privategallery.app.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path

/**
 * STEP 35/36/37/38 — CROP/TRANSFORM, ADJUSTMENT ENGINE, FILTER ENGINE,
 * DRAWING/MARKUP.
 *
 * Renders an EditState (STEP 34) onto a Bitmap. Used for both the live
 * preview and the final full-resolution render at Save time (STEP 40) —
 * same code path for both, so what the user previews is exactly what
 * gets saved. Never mutates the source bitmap in place; always produces
 * a new Bitmap, keeping the original untouched per STEP 34's contract.
 */
object ImageEditRenderer {

    fun render(source: Bitmap, state: EditState): Bitmap {
        val cropped = applyCrop(source, state.crop)
        val adjusted = applyAdjustments(cropped, state.adjustments)
        val filtered = applyFilter(adjusted, state.filter)
        return applyDrawing(filtered, state.drawingElements)
    }

    // STEP 35 — CROP AND TRANSFORM
    private fun applyCrop(source: Bitmap, crop: CropState): Bitmap {
        val matrix = Matrix()
        if (crop.rotationDegrees != 0) matrix.postRotate(crop.rotationDegrees.toFloat())
        if (crop.flippedHorizontally) matrix.postScale(-1f, 1f)
        if (crop.flippedVertically) matrix.postScale(1f, -1f)
        if (crop.straightenDegrees != 0f) matrix.postRotate(crop.straightenDegrees)

        val transformed = if (matrix.isIdentity) {
            source
        } else {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }

        val rect = crop.cropRectNormalized
        val left = (rect.left * transformed.width).toInt().coerceIn(0, transformed.width - 1)
        val top = (rect.top * transformed.height).toInt().coerceIn(0, transformed.height - 1)
        val right = (rect.right * transformed.width).toInt().coerceIn(left + 1, transformed.width)
        val bottom = (rect.bottom * transformed.height).toInt().coerceIn(top + 1, transformed.height)

        return if (left == 0 && top == 0 && right == transformed.width && bottom == transformed.height) {
            transformed
        } else {
            Bitmap.createBitmap(transformed, left, top, right - left, bottom - top)
        }
    }

    // STEP 36 — IMAGE ADJUSTMENT ENGINE
    private fun applyAdjustments(source: Bitmap, adjustments: AdjustmentState): Bitmap {
        if (adjustments == AdjustmentState()) return source

        val colorMatrix = ColorMatrix()

        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(1f + adjustments.saturation)
        colorMatrix.postConcat(satMatrix)

        val brightnessValue = (adjustments.brightness + adjustments.exposure) * 255f
        val brightnessMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightnessValue,
                0f, 1f, 0f, 0f, brightnessValue,
                0f, 0f, 1f, 0f, brightnessValue,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(brightnessMatrix)

        val contrastScale = 1f + adjustments.contrast
        val contrastTranslate = (1f - contrastScale) * 128f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrastScale, 0f, 0f, 0f, contrastTranslate,
                0f, contrastScale, 0f, 0f, contrastTranslate,
                0f, 0f, contrastScale, 0f, contrastTranslate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(contrastMatrix)

        if (adjustments.temperature != 0f || adjustments.tint != 0f) {
            val warmShift = adjustments.temperature * 40f
            val tintShift = adjustments.tint * 40f
            val tempMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, warmShift,
                    0f, 1f, 0f, 0f, tintShift,
                    0f, 0f, 1f, 0f, -warmShift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(tempMatrix)
        }

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Vignette/grain/fade are documented extension points: they need
        // a shader-based pass rather than a ColorMatrix and will be wired
        // once the renderer moves to RenderEffect/AGSL. Left out rather
        // than faked, per RULE E (no placeholder functionality).
        return result
    }

    // STEP 37 — FILTER ENGINE
    private fun applyFilter(source: Bitmap, filter: FilterState): Bitmap {
        val matrix = FilterCatalog.matrixFor(filter.filterId) ?: return source
        val identity = ColorMatrix()
        val blended = ColorMatrix()
        for (i in 0 until 20) {
            val filterValue = matrix.array[i]
            val identityValue = identity.array[i]
            blended.array[i] = identityValue + (filterValue - identityValue) * filter.intensity
        }
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(blended)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    // STEP 38 — DRAWING / MARKUP
    private fun applyDrawing(source: Bitmap, elements: List<DrawingElement>): Bitmap {
        if (elements.isEmpty()) return source
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        elements.forEach { element ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = element.colorArgb.toInt()
                strokeWidth = element.strokeWidth
                alpha = (element.opacity * 255).toInt()
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            when (element.tool) {
                DrawingTool.PEN, DrawingTool.HIGHLIGHTER, DrawingTool.ARROW -> drawPath(canvas, element, paint)
                DrawingTool.RECTANGLE -> drawRect(canvas, element, paint)
                DrawingTool.CIRCLE -> drawCircle(canvas, element, paint)
                DrawingTool.TEXT -> drawText(canvas, element, paint)
            }
        }
        return result
    }

    private fun drawPath(canvas: Canvas, element: DrawingElement, paint: Paint) {
        if (element.pathPoints.size < 2) return
        val path = Path()
        path.moveTo(element.pathPoints.first().first, element.pathPoints.first().second)
        element.pathPoints.drop(1).forEach { (x, y) -> path.lineTo(x, y) }
        canvas.drawPath(path, paint)
    }

    private fun drawRect(canvas: Canvas, element: DrawingElement, paint: Paint) {
        val points = element.pathPoints
        if (points.size < 2) return
        val (x1, y1) = points.first()
        val (x2, y2) = points.last()
        canvas.drawRect(minOf(x1, x2), minOf(y1, y2), maxOf(x1, x2), maxOf(y1, y2), paint)
    }

    private fun drawCircle(canvas: Canvas, element: DrawingElement, paint: Paint) {
        val points = element.pathPoints
        if (points.size < 2) return
        val (cx, cy) = points.first()
        val (ex, ey) = points.last()
        val radius = kotlin.math.hypot((ex - cx).toDouble(), (ey - cy).toDouble()).toFloat()
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun drawText(canvas: Canvas, element: DrawingElement, paint: Paint) {
        val text = element.text ?: return
        val point = element.pathPoints.firstOrNull() ?: return
        val textPaint = Paint(paint).apply {
            style = Paint.Style.FILL
            textSize = element.strokeWidth * 8f
        }
        canvas.drawText(text, point.first, point.second, textPaint)
    }
}

private object FilterCatalog {
    fun matrixFor(filterId: String?): ColorMatrix? = when (filterId) {
        "mono" -> ColorMatrix().apply { setSaturation(0f) }
        "warm" -> ColorMatrix(
            floatArrayOf(
                1.1f, 0f, 0f, 0f, 10f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 0.9f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        "cool" -> ColorMatrix(
            floatArrayOf(
                0.9f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1.1f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        "vivid" -> ColorMatrix().apply { setSaturation(1.4f) }
        else -> null
    }
}
