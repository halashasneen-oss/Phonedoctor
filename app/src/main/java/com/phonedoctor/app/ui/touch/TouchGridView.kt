package com.phonedoctor.app.ui.touch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.phonedoctor.app.R

/**
 * Full-screen touch coverage test. The screen is divided into a grid; any
 * cell a finger passes over is marked visited. Real multi-touch support via
 * MotionEvent's pointer index/id APIs, not simulated.
 */
class TouchGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onStatsChanged: ((touchPoints: Int, coveragePercent: Int) -> Unit)? = null

    private val cellSizePx = resources.displayMetrics.density * 32f
    private var columns = 1
    private var rows = 1
    private lateinit var visited: BooleanArray

    private var touchPointCount = 0

    private val visitedPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.color_primary)
        alpha = 140
    }
    private val gridPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.color_outline)
        strokeWidth = 1f
        alpha = 60
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = maxOf(1, (w / cellSizePx).toInt())
        rows = maxOf(1, (h / cellSizePx).toInt())
        visited = BooleanArray(columns * rows)
    }

    fun reset() {
        visited.fill(false)
        touchPointCount = 0
        invalidate()
        notifyStats()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        for (i in 0 until event.pointerCount) {
            markCell(event.getX(i), event.getY(i))
        }
        touchPointCount++
        invalidate()
        notifyStats()
        return true
    }

    private fun markCell(x: Float, y: Float) {
        val col = (x / cellSizePx).toInt().coerceIn(0, columns - 1)
        val row = (y / cellSizePx).toInt().coerceIn(0, rows - 1)
        val index = row * columns + col
        if (index in visited.indices) visited[index] = true
    }

    private fun notifyStats() {
        val visitedCount = visited.count { it }
        val coverage = if (visited.isEmpty()) 0 else (visitedCount * 100 / visited.size)
        onStatsChanged?.invoke(touchPointCount, coverage)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.TRANSPARENT)
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val index = row * columns + col
                if (index < visited.size && visited[index]) {
                    val left = col * cellSizePx
                    val top = row * cellSizePx
                    canvas.drawRect(left, top, left + cellSizePx, top + cellSizePx, visitedPaint)
                }
            }
        }
        for (col in 0..columns) {
            val x = col * cellSizePx
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
        }
        for (row in 0..rows) {
            val y = row * cellSizePx
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
    }
}
