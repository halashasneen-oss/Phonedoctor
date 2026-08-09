package com.phonedoctor.app.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.phonedoctor.app.R
import com.phonedoctor.app.domain.util.HealthScoreCalculator

/**
 * Circular progress ring used on Home to show the overall health score.
 * Pure canvas drawing, no dependency on any chart library.
 */
class HealthRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val strokeWidthPx = resources.getDimension(R.dimen.health_ring_stroke)
    private val bounds = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.color_surface_variant)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
    }

    private var animatedProgress = 0f
    private var animator: ValueAnimator? = null

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            progressPaint.color = colorForScore(field)
            animator?.cancel()
            animator = ValueAnimator.ofFloat(animatedProgress, field.toFloat()).apply {
                duration = 700
                addUpdateListener {
                    animatedProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

    private fun colorForScore(score: Int): Int {
        val status = HealthScoreCalculator.scoreToStatus(score)
        val colorRes = when (status) {
            com.phonedoctor.app.domain.model.TestStatus.EXCELLENT,
            com.phonedoctor.app.domain.model.TestStatus.GOOD -> R.color.color_success
            com.phonedoctor.app.domain.model.TestStatus.FAIR -> R.color.color_warning
            else -> R.color.color_danger
        }
        return ContextCompat.getColor(context, colorRes)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = strokeWidthPx / 2f
        bounds.set(inset, inset, w - inset, h - inset)
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(bounds, -90f, 360f, false, trackPaint)
        val sweep = 360f * (animatedProgress / 100f)
        canvas.drawArc(bounds, -90f, sweep, false, progressPaint)
    }
}
