package com.antiscroller.kitty.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * 8-Bit Arcade Striped Awning with Scalloped Hem.
 * Recreates the iconic retro arcade canopy from the reference design.
 */
class PixelAwningView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val stripePaintYellow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F2B723")
        style = Paint.Style.FILL
    }

    private val stripePaintAmber = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DE9C18")
        style = Paint.Style.FILL
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33000000")
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density
        val targetStripeW = 20f * density
        val numStripes = (w / targetStripeW).toInt().coerceAtLeast(8)
        val stripeW = w / numStripes.toFloat()

        val scallopRadius = stripeW / 2f
        val bodyH = (h - scallopRadius).coerceAtLeast(10f)

        // Subtle drop shadow behind scallops
        for (i in 0 until numStripes) {
            val left = i * stripeW
            val cx = left + scallopRadius
            val cy = bodyH + (1.5f * density)
            canvas.drawArc(
                cx - scallopRadius,
                cy - scallopRadius,
                cx + scallopRadius,
                cy + scallopRadius,
                0f,
                180f,
                true,
                shadowPaint
            )
        }

        // Alternating Stripes with Scalloped bottom hem
        for (i in 0 until numStripes) {
            val left = i * stripeW
            val right = if (i == numStripes - 1) w else (i + 1) * stripeW
            val paint = if (i % 2 == 0) stripePaintYellow else stripePaintAmber

            // Vertical canopy body
            canvas.drawRect(left, 0f, right, bodyH, paint)

            // Scalloped bottom semicircle
            val cx = left + scallopRadius
            val cy = bodyH
            canvas.drawArc(
                cx - scallopRadius,
                cy - scallopRadius,
                cx + scallopRadius,
                cy + scallopRadius,
                0f,
                180f,
                true,
                paint
            )
        }
    }
}
