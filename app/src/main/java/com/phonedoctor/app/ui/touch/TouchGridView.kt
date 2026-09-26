package com.phonedoctor.app.ui.touch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.phonedoctor.app.R
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max

data class TouchStats(
    val currentPointers: Int,
    val maxSimultaneousPointers: Int,
    val coveragePercent: Int,
    val edgeCoveragePercent: Int,
    val missedCells: Int
)

/**
 * Full-screen touch coverage test. The screen is divided into a grid; every
 * path a finger travels through is marked, including historical MotionEvent
 * samples and interpolation between samples. Multi-touch statistics use real
 * pointer IDs/counts rather than counting raw MotionEvent callbacks.
 */
class TouchGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onStatsChanged: ((TouchStats) -> Unit)? = null

    private val cellSizePx = resources.displayMetrics.density * 32f
    private var columns = 1
    private var rows = 1
    private lateinit var visited: BooleanArray

    private var maxSimultaneousPointers = 0
    private val lastPointByPointerId = mutableMapOf<Int, PointF>()

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
        columns = max(1, (w / cellSizePx).toInt())
        rows = max(1, (h / cellSizePx).toInt())
        visited = BooleanArray(columns * rows)
        lastPointByPointerId.clear()
        maxSimultaneousPointers = 0
        notifyStats(0)
    }

    fun reset() {
        if (::visited.isInitialized) {
            visited.fill(false)
        }
        lastPointByPointerId.clear()
        maxSimultaneousPointers = 0
        invalidate()
        notifyStats(0)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!::visited.isInitialized) return true

        maxSimultaneousPointers = max(maxSimultaneousPointers, event.pointerCount)

        for (historyIndex in 0 until event.historySize) {
            for (pointerIndex in 0 until event.pointerCount) {
                val pointerId = event.getPointerId(pointerIndex)
                processPoint(
                    pointerId,
                    event.getHistoricalX(pointerIndex, historyIndex),
                    event.getHistoricalY(pointerIndex, historyIndex)
                )
            }
        }

        for (pointerIndex in 0 until event.pointerCount) {
            val pointerId = event.getPointerId(pointerIndex)
            processPoint(
                pointerId,
                event.getX(pointerIndex),
                event.getY(pointerIndex)
            )
        }

        val currentPointers = when (event.actionMasked) {
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> 0
            MotionEvent.ACTION_POINTER_UP -> (event.pointerCount - 1).coerceAtLeast(0)
            else -> event.pointerCount
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                lastPointByPointerId.remove(event.getPointerId(event.actionIndex))
                performClick()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                lastPointByPointerId.remove(event.getPointerId(event.actionIndex))
            }
            MotionEvent.ACTION_CANCEL -> lastPointByPointerId.clear()
        }

        invalidate()
        notifyStats(currentPointers)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun processPoint(pointerId: Int, x: Float, y: Float) {
        val previous = lastPointByPointerId[pointerId]
        if (previous != null) {
            markSegment(previous.x, previous.y, x, y)
        } else {
            markCell(x, y)
        }
        lastPointByPointerId[pointerId] = PointF(x, y)
    }

    private fun markSegment(x1: Float, y1: Float, x2: Float, y2: Float) {
        val distance = hypot(x2 - x1, y2 - y1)
        val stepSize = (cellSizePx / 2f).coerceAtLeast(1f)
        val steps = ceil(distance / stepSize).toInt().coerceAtLeast(1)

        for (step in 0..steps) {
            val fraction = step.toFloat() / steps.toFloat()
            markCell(
                x1 + (x2 - x1) * fraction,
                y1 + (y2 - y1) * fraction
            )
        }
    }

    private fun markCell(x: Float, y: Float) {
        val col = (x / cellSizePx).toInt().coerceIn(0, columns - 1)
        val row = (y / cellSizePx).toInt().coerceIn(0, rows - 1)
        val index = row * columns + col
        if (index in visited.indices) visited[index] = true
    }

    private fun notifyStats(currentPointers: Int) {
        if (!::visited.isInitialized) return

        val visitedCount = visited.count { it }
        val coverage = if (visited.isEmpty()) {
            0
        } else {
            visitedCount * 100 / visited.size
        }

        var edgeCells = 0
        var visitedEdgeCells = 0
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val isEdge = row == 0 || row == rows - 1 ||
                    col == 0 || col == columns - 1
                if (!isEdge) continue

                edgeCells++
                val index = row * columns + col
                if (index in visited.indices && visited[index]) {
                    visitedEdgeCells++
                }
            }
        }

        val edgeCoverage = if (edgeCells == 0) {
            0
        } else {
            visitedEdgeCells * 100 / edgeCells
        }

        onStatsChanged?.invoke(
            TouchStats(
                currentPointers = currentPointers,
                maxSimultaneousPointers = maxSimultaneousPointers,
                coveragePercent = coverage,
                edgeCoveragePercent = edgeCoverage,
                missedCells = (visited.size - visitedCount).coerceAtLeast(0)
            )
        )
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
                    canvas.drawRect(
                        left,
                        top,
                        left + cellSizePx,
                        top + cellSizePx,
                        visitedPaint
                    )
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
