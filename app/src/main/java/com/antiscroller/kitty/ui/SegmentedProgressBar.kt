package com.antiscroller.kitty.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 8-Bit Chunky Segmented Progress Tally Bar.
 * Draws 18 distinct blocky vertical ticks representing scroll limit progress.
 */
class SegmentedProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val totalSegments = 18

    private var progressRatio = 0f // 0.0 to 1.0

    private val filledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E1C2E")
        style = Paint.Style.FILL
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C5BEB3")
        style = Paint.Style.FILL
    }

    private val blockRect = RectF()

    fun setProgress(progress: Float) {
        progressRatio = progress.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density
        val spacing = 3.5f * density
        val totalSpacing = spacing * (totalSegments - 1)
        val blockW = ((w - totalSpacing) / totalSegments).coerceAtLeast(1f)
        val cornerRadius = 2f * density

        val filledCount = (progressRatio * totalSegments).toInt().coerceIn(0, totalSegments)

        for (i in 0 until totalSegments) {
            val left = i * (blockW + spacing)
            val right = left + blockW
            blockRect.set(left, 0f, right, h)

            val paint = if (i < filledCount) filledPaint else emptyPaint
            canvas.drawRoundRect(blockRect, cornerRadius, cornerRadius, paint)
        }
    }
}
