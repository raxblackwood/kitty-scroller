package com.antiscroller.kitty.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.antiscroller.kitty.R

/**
 * 8-Bit Arcade Striped Awning with Scalloped Hem and Peeking Cat.
 * Recreates the retro arcade canopy from the Pixel Theme reference design.
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

    private val catPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private var catBitmap: Bitmap? = null
    private val catSrcRect = Rect()
    private val catDstRect = RectF()

    init {
        loadCatBitmap(R.drawable.kitty_casper_loaf)
    }

    fun setCatBitmapRes(resId: Int) {
        loadCatBitmap(resId)
        invalidate()
    }

    private fun loadCatBitmap(resId: Int) {
        try {
            catBitmap = BitmapFactory.decodeResource(resources, resId)
        } catch (_: Exception) {}
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // 1. Calculate stripe width and count (aim for ~22-26dp per stripe)
        val density = resources.displayMetrics.density
        val targetStripeW = 22f * density
        val numStripes = (w / targetStripeW).toInt().coerceAtLeast(8)
        val stripeW = w / numStripes.toFloat()

        val scallopRadius = stripeW / 2f
        val bodyH = h - scallopRadius

        // 2. Draw Peeking Cat on the right side behind the awning
        catBitmap?.let { bmp ->
            val catW = 74f * density
            val catH = catW * (bmp.height.toFloat() / bmp.width.toFloat())
            // Position on right: ~78% across width, peeking out from under bodyH
            val catRight = w - 24f * density
            val catLeft = catRight - catW
            val catTop = bodyH - (catH * 0.45f)
            val catBottom = catTop + catH

            catSrcRect.set(0, 0, bmp.width, bmp.height)
            catDstRect.set(catLeft, catTop, catRight, catBottom)
            canvas.drawBitmap(bmp, catSrcRect, catDstRect, catPaint)
        }

        // 3. Draw Alternating Stripes
        for (i in 0 until numStripes) {
            val left = i * stripeW
            val right = if (i == numStripes - 1) w else (i + 1) * stripeW
            val paint = if (i % 2 == 0) stripePaintYellow else stripePaintAmber

            // Stripe vertical body
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
