package com.screenlens.app.ui.crop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.screenlens.app.R
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a scrim over the displayed image with a draggable, resizable clear
 * rectangle representing the crop selection. All rects are in *view* coordinates;
 * callers map to bitmap pixel coordinates via [imageBounds].
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** The bounds (in view coordinates) where the bitmap is actually drawn, set by the host. */
    var imageBounds: RectF = RectF()
        set(value) {
            field = value
            selection = RectF(value)
            invalidate()
        }

    private var selection = RectF()
    val selectionRect: RectF get() = RectF(selection)

    private val handleTouchRadius = 48f
    private var activeHandle: Handle = Handle.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private enum class Handle { NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private val scrimPaint = Paint().apply { color = Color.parseColor("#B3000000") }
    private val borderPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.brand_primary)
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val handlePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.brand_primary)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun reset() {
        selection = RectF(imageBounds)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (imageBounds.isEmpty) return

        val path = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addRect(selection, Path.Direction.CW)
        }
        canvas.drawPath(path, scrimPaint)
        canvas.drawRect(selection, borderPaint)

        val r = 14f
        canvas.drawCircle(selection.left, selection.top, r, handlePaint)
        canvas.drawCircle(selection.right, selection.top, r, handlePaint)
        canvas.drawCircle(selection.left, selection.bottom, r, handlePaint)
        canvas.drawCircle(selection.right, selection.bottom, r, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeHandle = hitTest(event.x, event.y)
                lastTouchX = event.x
                lastTouchY = event.y
                return activeHandle != Handle.NONE
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                lastTouchX = event.x
                lastTouchY = event.y
                applyDrag(dx, dy)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = Handle.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun hitTest(x: Float, y: Float): Handle {
        fun near(px: Float, py: Float) = (x - px) * (x - px) + (y - py) * (y - py) <= handleTouchRadius * handleTouchRadius
        return when {
            near(selection.left, selection.top) -> Handle.TOP_LEFT
            near(selection.right, selection.top) -> Handle.TOP_RIGHT
            near(selection.left, selection.bottom) -> Handle.BOTTOM_LEFT
            near(selection.right, selection.bottom) -> Handle.BOTTOM_RIGHT
            selection.contains(x, y) -> Handle.MOVE
            else -> Handle.NONE
        }
    }

    private fun applyDrag(dx: Float, dy: Float) {
        val minSize = 64f
        when (activeHandle) {
            Handle.MOVE -> {
                val moved = RectF(selection)
                moved.offset(dx, dy)
                if (moved.left >= imageBounds.left && moved.right <= imageBounds.right) {
                    selection.left = moved.left; selection.right = moved.right
                }
                if (moved.top >= imageBounds.top && moved.bottom <= imageBounds.bottom) {
                    selection.top = moved.top; selection.bottom = moved.bottom
                }
            }
            Handle.TOP_LEFT -> {
                selection.left = clamp(selection.left + dx, imageBounds.left, selection.right - minSize)
                selection.top = clamp(selection.top + dy, imageBounds.top, selection.bottom - minSize)
            }
            Handle.TOP_RIGHT -> {
                selection.right = clamp(selection.right + dx, selection.left + minSize, imageBounds.right)
                selection.top = clamp(selection.top + dy, imageBounds.top, selection.bottom - minSize)
            }
            Handle.BOTTOM_LEFT -> {
                selection.left = clamp(selection.left + dx, imageBounds.left, selection.right - minSize)
                selection.bottom = clamp(selection.bottom + dy, selection.top + minSize, imageBounds.bottom)
            }
            Handle.BOTTOM_RIGHT -> {
                selection.right = clamp(selection.right + dx, selection.left + minSize, imageBounds.right)
                selection.bottom = clamp(selection.bottom + dy, selection.top + minSize, imageBounds.bottom)
            }
            Handle.NONE -> {}
        }
    }

    private fun clamp(value: Float, lower: Float, upper: Float) = max(lower, min(upper, value))
}
