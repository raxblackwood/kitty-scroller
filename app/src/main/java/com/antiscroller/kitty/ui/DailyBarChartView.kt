package com.antiscroller.kitty.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.antiscroller.kitty.R
import com.antiscroller.kitty.data.ScrollDataManager
import kotlin.math.max

/**
 * Custom hardware-accelerated vertical bar chart component.
 * Displays 7-day reel consumption columns rising vertically against a horizontal daily limit line.
 */
class DailyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var historyItems: List<ScrollDataManager.DailyHistoryItem> = emptyList()
    private var selectedIndex: Int = -1
    private var animProgress: Float = 0f

    var onDaySelectedListener: ((ScrollDataManager.DailyHistoryItem) -> Unit)? = null

    // Colors
    private val colorNormal = context.getColor(R.color.accent_green)
    private val colorWarning = context.getColor(R.color.kitty_orange)
    private val colorLimitExceeded = context.getColor(R.color.accent_red)
    private val colorLimitLine = Color.parseColor("#888888")
    private val colorTextPrimary = context.getColor(R.color.text_primary)
    private val colorTextSecondary = context.getColor(R.color.text_secondary)
    private val colorBarBg = Color.parseColor("#252525")
    private val colorSelectedGlow = Color.parseColor("#44FFFFFF")

    // Paints
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorBarBg
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
        color = Color.WHITE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val limitLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorLimitLine
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

    private val density = resources.displayMetrics.density
    private val barRect = RectF()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setData(items: List<ScrollDataManager.DailyHistoryItem>) {
        // Take past 7 days in chronological order (oldest to newest/today on right)
        val sevenDays = items.take(7).reversed()
        this.historyItems = sevenDays
        this.selectedIndex = sevenDays.indexOfLast { it.isToday }.coerceAtLeast(sevenDays.size - 1)

        startEntranceAnimation()
    }

    private fun startEntranceAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener {
                animProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (historyItems.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        val paddingBottom = 32f * density
        val paddingTop = 28f * density
        val chartHeight = h - paddingTop - paddingBottom
        val bottomY = h - paddingBottom

        val count = historyItems.size
        val slotWidth = w / count
        val barWidth = (slotWidth * 0.44f).coerceIn(16f * density, 28f * density)
        val cornerRadius = barWidth / 2f

        val limit = historyItems.firstOrNull()?.limit ?: 50
        val maxReels = historyItems.maxOfOrNull { it.count } ?: 0
        val ceiling = max(max(maxReels.toFloat(), limit.toFloat()) * 1.25f, 10f)

        // 1. Draw dashed limit line
        val limitRatio = (limit.toFloat() / ceiling).coerceIn(0f, 1f)
        val limitY = bottomY - (chartHeight * limitRatio)

        canvas.drawLine(16f * density, limitY, w - 16f * density, limitY, limitLinePaint)

        // Limit label
        textPaint.color = colorTextSecondary
        textPaint.textSize = 10f * density
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Limit (${limit})", w - 16f * density, limitY - (4f * density), textPaint)
        textPaint.textAlign = Paint.Align.CENTER

        // 2. Draw each day bar
        for (i in 0 until count) {
            val item = historyItems[i]
            val centerX = (i * slotWidth) + (slotWidth / 2f)
            val left = centerX - (barWidth / 2f)
            val right = centerX + (barWidth / 2f)

            // Background slot capsule (shows full scale height softly)
            barRect.set(left, paddingTop, right, bottomY)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barBgPaint)

            // Active bar
            val valueRatio = (item.count.toFloat() / ceiling).coerceIn(0f, 1f)
            val barH = chartHeight * valueRatio * animProgress
            val barTop = if (item.count == 0) bottomY - (4f * density) else (bottomY - barH).coerceAtMost(bottomY - 4f * density)

            barRect.set(left, barTop, right, bottomY)

            // Determine bar color
            barPaint.color = when {
                item.count == 0 -> Color.parseColor("#383838")
                item.count >= item.limit -> colorLimitExceeded
                item.count >= (item.limit * 0.85f) -> colorWarning
                else -> colorNormal
            }

            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)

            // Selected day border highlight
            if (i == selectedIndex) {
                barRect.set(left - (2f * density), paddingTop - (2f * density), right + (2f * density), bottomY + (2f * density))
                canvas.drawRoundRect(barRect, cornerRadius + (2f * density), cornerRadius + (2f * density), selectedPaint)
            }

            // Value text above the bar
            if (animProgress > 0.6f) {
                textPaint.textSize = 11f * density
                textPaint.color = if (i == selectedIndex) Color.WHITE else colorTextPrimary
                textPaint.isFakeBoldText = (i == selectedIndex || item.isToday)
                val textY = if (item.count == 0) bottomY - (10f * density) else (barTop - (6f * density)).coerceAtLeast(paddingTop - 4f * density)
                canvas.drawText(item.count.toString(), centerX, textY, textPaint)
            }

            // Day label below the bar
            textPaint.textSize = 11f * density
            textPaint.color = if (item.isToday) colorWarning else colorTextSecondary
            textPaint.isFakeBoldText = item.isToday

            val dayLabel = when {
                item.isToday -> "Today"
                item.displayDate == "Yesterday" -> "Yest"
                else -> item.dayOfWeek.take(3)
            }
            canvas.drawText(dayLabel, centerX, h - (10f * density), textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && historyItems.isNotEmpty()) {
            val slotWidth = width.toFloat() / historyItems.size
            val clickedIndex = (event.x / slotWidth).toInt().coerceIn(0, historyItems.size - 1)
            selectedIndex = clickedIndex
            invalidate()
            onDaySelectedListener?.invoke(historyItems[clickedIndex])
            return true
        }
        return true
    }
}
