package com.antiscroller.kitty.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.antiscroller.kitty.R

/**
 * Custom View that renders the adorable Kitty Curtain dropping down from the ceiling.
 * Supports multiple Kitty Breeds:
 * - Ginger (Orange Cat)
 * - Tabby (Black Cat)
 * - Saturn (Gangster Cat with 8-frame animated sprite)
 *
 * Features:
 * - Unified horizontal pole stretching continuously across the screen at the bottom of the curtain.
 * - Straight flush curtain hem connecting directly to the top edge of the stick (no separate curtain stick below).
 * - Rock-solid, non-wiggling stick anchored across the curtain.
 * - Stays persistently visible on screen as the user scrolls.
 */
class KittyCurtainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class CatBreed(
        val key: String,
        val displayName: String,
        val furColor: Int,
        val furDarkColor: Int,
        val innerEarColor: Int,
        val pawColor: Int,
        val pawPadColor: Int,
        val curtainBodyColor: Int,
        val curtainStripeColor: Int,
        val eyeColor: Int,
        val eyeHighlightColor: Int,
        val noseColor: Int,
        val strokeColor: Int,
        val speechBubbleBg: Int,
        val speechTextColor: Int,
        val poleWoodColor: Int,
        val poleOutlineColor: Int
    ) {
        GINGER(
            key = "ginger",
            displayName = "Ginger",
            furColor = Color.parseColor("#FFA726"),
            furDarkColor = Color.parseColor("#FB8C00"),
            innerEarColor = Color.parseColor("#FF8A80"),
            pawColor = Color.parseColor("#FFF3E0"),
            pawPadColor = Color.parseColor("#FF8A80"),
            curtainBodyColor = Color.parseColor("#FFE0B2"),
            curtainStripeColor = Color.parseColor("#33FFA726"),
            eyeColor = Color.parseColor("#212121"),
            eyeHighlightColor = Color.WHITE,
            noseColor = Color.parseColor("#FF5252"),
            strokeColor = Color.parseColor("#4E342E"),
            speechBubbleBg = Color.parseColor("#F5F5F5"),
            speechTextColor = Color.parseColor("#212121"),
            poleWoodColor = Color.parseColor("#936043"),
            poleOutlineColor = Color.parseColor("#120A05")
        ),
        TABBY(
            key = "tabby",
            displayName = "Tabby",
            furColor = Color.parseColor("#262626"),
            furDarkColor = Color.parseColor("#141414"),
            innerEarColor = Color.parseColor("#FF80AB"),
            pawColor = Color.parseColor("#1F1F1F"),
            pawPadColor = Color.parseColor("#FF80AB"),
            curtainBodyColor = Color.parseColor("#2A2D34"),
            curtainStripeColor = Color.parseColor("#33FFD600"),
            eyeColor = Color.parseColor("#FFD600"),
            eyeHighlightColor = Color.WHITE,
            noseColor = Color.parseColor("#FF4081"),
            strokeColor = Color.parseColor("#111111"),
            speechBubbleBg = Color.parseColor("#1E1E1E"),
            speechTextColor = Color.parseColor("#FFD600"),
            poleWoodColor = Color.parseColor("#724A39"),
            poleOutlineColor = Color.parseColor("#0E0704")
        ),
        SATURN(
            key = "saturn",
            displayName = "Saturn",
            furColor = Color.parseColor("#E0D3C1"),
            furDarkColor = Color.parseColor("#4A2E1B"),
            innerEarColor = Color.parseColor("#8A5040"),
            pawColor = Color.parseColor("#3D2415"),
            pawPadColor = Color.parseColor("#2D1B10"),
            curtainBodyColor = Color.parseColor("#222026"),
            curtainStripeColor = Color.parseColor("#33FFD700"),
            eyeColor = Color.parseColor("#FFB300"),
            eyeHighlightColor = Color.WHITE,
            noseColor = Color.parseColor("#22150E"),
            strokeColor = Color.parseColor("#150E0A"),
            speechBubbleBg = Color.parseColor("#18161C"),
            speechTextColor = Color.parseColor("#FFD700"),
            poleWoodColor = Color.parseColor("#76513A"),
            poleOutlineColor = Color.parseColor("#140C08")
        ),
        ROLEX(
            key = "rolex",
            displayName = "Rolex",
            furColor = Color.parseColor("#C8B8A6"),
            furDarkColor = Color.parseColor("#263238"),
            innerEarColor = Color.parseColor("#37474F"),
            pawColor = Color.parseColor("#37474F"),
            pawPadColor = Color.parseColor("#263238"),
            curtainBodyColor = Color.parseColor("#2B303A"),
            curtainStripeColor = Color.parseColor("#3378909C"),
            eyeColor = Color.parseColor("#FFFFFF"),
            eyeHighlightColor = Color.parseColor("#263238"),
            noseColor = Color.parseColor("#263238"),
            strokeColor = Color.parseColor("#12171A"),
            speechBubbleBg = Color.parseColor("#1E222A"),
            speechTextColor = Color.parseColor("#B0BEC5"),
            poleWoodColor = Color.parseColor("#4A382A"),
            poleOutlineColor = Color.parseColor("#14100D")
        )
    }

    private var currentBreed: CatBreed = CatBreed.GINGER
    private var dropPercentage: Float = 0f
    private var animatedProgress: Float = 0f
    private var animator: ValueAnimator? = null

    // Tail wagging animation for Ginger & Tabby
    private var tailAngle: Float = 0f
    private var tailAnimator: ValueAnimator? = null

    // Saturn 8-frame cartoon animation loop
    private var saturnFrameIndex: Int = 0
    private var saturnAnimator: ValueAnimator? = null
    private val saturnBitmaps = mutableListOf<Bitmap>()
    private var saturnFullBitmap: Bitmap? = null

    // Rolex sprite bitmap
    private var rolexBitmap: Bitmap? = null

    // Pixel-art sprite bitmaps for Ginger & Tabby
    private var bodyBitmap: Bitmap? = null
    private var tailBitmap: Bitmap? = null
    private val catPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    // Paint objects
    private val furPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val furDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val innerEarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val eyeHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }
    private val nosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val speechBubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 4f, Color.parseColor("#40000000"))
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val curtainBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    // Horizontal pole paints (matching pixel-art sprite bar)
    private val poleOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#120A05")
    }
    private val poleBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#8F5E40")
    }

    // Reusable geometry objects to prevent GC churn during fast scrolling
    private val curtainPath = Path()
    private val catRect = RectF()
    private val bubbleRect = RectF()
    private val pointerPath = Path()

    init {
        applyTheme("ginger")
        startAnimations()
    }

    fun applyTheme(themeName: String) {
        val newBreed = when (themeName.lowercase()) {
            "tabby", "black" -> CatBreed.TABBY
            "saturn" -> CatBreed.SATURN
            "rolex" -> CatBreed.ROLEX
            else -> CatBreed.GINGER
        }

        val needsBitmapReload = (currentBreed != newBreed) ||
            (newBreed == CatBreed.SATURN && saturnBitmaps.isEmpty()) ||
            (newBreed == CatBreed.ROLEX && rolexBitmap == null) ||
            (newBreed != CatBreed.SATURN && newBreed != CatBreed.ROLEX && bodyBitmap == null)

        currentBreed = newBreed

        furPaint.color = currentBreed.furColor
        furDarkPaint.color = currentBreed.furDarkColor
        innerEarPaint.color = currentBreed.innerEarColor
        pawPaint.color = currentBreed.pawColor
        eyePaint.color = currentBreed.eyeColor
        eyeHighlightPaint.color = currentBreed.eyeHighlightColor
        strokePaint.color = currentBreed.strokeColor
        nosePaint.color = currentBreed.noseColor
        speechBubblePaint.color = currentBreed.speechBubbleBg
        textPaint.color = currentBreed.speechTextColor
        curtainBodyPaint.color = currentBreed.curtainBodyColor
        stripePaint.color = currentBreed.curtainStripeColor
        poleBodyPaint.color = currentBreed.poleWoodColor
        poleOutlinePaint.color = currentBreed.poleOutlineColor

        if (needsBitmapReload) {
            loadCatBitmaps()
            invalidate()
        }
    }

    private fun loadCatBitmaps() {
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        if (currentBreed == CatBreed.SATURN) {
            saturnBitmaps.clear()
            val saturnResIds = intArrayOf(
                R.drawable.kitty_saturn_0,
                R.drawable.kitty_saturn_1,
                R.drawable.kitty_saturn_2,
                R.drawable.kitty_saturn_3,
                R.drawable.kitty_saturn_4,
                R.drawable.kitty_saturn_5,
                R.drawable.kitty_saturn_6,
                R.drawable.kitty_saturn_7
            )
            for (resId in saturnResIds) {
                val bmp = BitmapFactory.decodeResource(resources, resId, options)
                if (bmp != null) saturnBitmaps.add(bmp)
            }
            saturnFullBitmap = BitmapFactory.decodeResource(resources, R.drawable.kitty_saturn_full, options)
        } else if (currentBreed == CatBreed.ROLEX) {
            rolexBitmap = BitmapFactory.decodeResource(resources, R.drawable.kitty_rolex, options)
        } else {
            val isTabby = currentBreed == CatBreed.TABBY
            val bodyRes = if (isTabby) R.drawable.kitty_black_body else R.drawable.kitty_orange_body
            val tailRes = if (isTabby) R.drawable.kitty_black_tail else R.drawable.kitty_orange_tail

            bodyBitmap = BitmapFactory.decodeResource(resources, bodyRes, options)
            tailBitmap = BitmapFactory.decodeResource(resources, tailRes, options)
        }
    }

    private fun startAnimations() {
        // 1. Lively tail wagging behind body for Ginger & Tabby
        tailAnimator?.cancel()
        tailAnimator = ValueAnimator.ofFloat(-12f, 12f).apply {
            duration = 850
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                tailAngle = it.animatedValue as Float
                if (currentBreed != CatBreed.SATURN && currentBreed != CatBreed.ROLEX) {
                    invalidate()
                }
            }
            start()
        }

        // 2. Fluid 8-frame cartoon animation loop for Saturn (~130ms per frame)
        saturnAnimator?.cancel()
        saturnAnimator = ValueAnimator.ofInt(0, 7).apply {
            duration = 1040
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener {
                val newIndex = it.animatedValue as Int
                if (newIndex != saturnFrameIndex) {
                    saturnFrameIndex = newIndex
                    if (currentBreed == CatBreed.SATURN) {
                        invalidate()
                    }
                }
            }
            start()
        }
    }

    fun setDropPercentage(target: Float, animate: Boolean = true) {
        val clamped = target.coerceIn(0f, 1f)
        dropPercentage = clamped

        if (!animate || height == 0) {
            animator?.cancel()
            animatedProgress = clamped
            invalidate()
            return
        }

        if (Math.abs(clamped - animatedProgress) < 0.002f) {
            return
        }

        animator?.cancel()
        animator = ValueAnimator.ofFloat(animatedProgress, clamped).apply {
            duration = 200
            interpolator = DecelerateInterpolator(1.2f)
            addUpdateListener {
                animatedProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val progress = animatedProgress
        if (progress <= 0.0001f) return

        val totalW = width.toFloat()
        val totalH = height.toFloat()
        if (totalW <= 0f || totalH <= 0f) return

        val centerX = totalW / 2f

        // 100% full screen lock: curtain covers entire screen, cat loaf in center
        if (progress >= 0.96f) {
            drawCurtainBody(canvas, totalW, totalH)
            drawFullSleepyCat(canvas, totalH, centerX, totalW)
            drawSpeechBubble(canvas, totalH * 0.45f - 40f, centerX, progress)
            return
        }

        // Cat display sizing
        val isSaturn = currentBreed == CatBreed.SATURN
        val isRolex = currentBreed == CatBreed.ROLEX
        val catW = if (isRolex) (totalW * 0.28f).coerceIn(240f, 320f) else (totalW * 0.22f).coerceIn(210f, 270f)
        // Aspect ratio: Rolex is 588x587 (~1.0), Saturn is 280x475 (~0.589), Ginger/Tabby is 628x1162 (~0.540)
        val catH = when {
            isRolex -> catW * (587f / 588f)
            isSaturn -> (catW / 0.589f)
            else -> (catW / 0.540f)
        }

        // Stick offsets within the sprite coordinate space:
        // Rolex: stick y=18..56 in 587 height space
        // Saturn: stick y=30..46 in 475 height space
        // Ginger/Tabby: stick y=30..72 in 1162 height space
        val stickTopRatio = when {
            isRolex -> (18f / 587f)
            isSaturn -> (30f / 475f)
            else -> (30f / 1162f)
        }
        val stickBotRatio = when {
            isRolex -> (56f / 587f)
            isSaturn -> (46f / 475f)
            else -> (72f / 1162f)
        }

        // Curtain bottom edge moves down proportionally with progress
        val curtainBottom = progress * totalH
        val stickTop = curtainBottom
        val stickBottom = stickTop + (catH * (stickBotRatio - stickTopRatio))

        // 1. Draw solid curtain fabric from ceiling straight down to top edge of the stick
        if (stickTop > 0f) {
            drawCurtainBody(canvas, totalW, stickTop + 1.5f)
            // 2. Extend the stick seamlessly across the entire width of the curtain
            drawExtendedStick(canvas, totalW, stickTop, stickBottom)
        }

        // 3. Draw Hanging Cat sprite (stick stays rock-solid, animated paws/body/tail)
        val catTop = stickTop - (catH * stickTopRatio)
        drawHangingSpriteCat(canvas, centerX, catTop, catW, catH)
    }

    private fun drawCurtainBody(canvas: Canvas, w: Float, bottomY: Float) {
        if (bottomY <= 0f) return

        curtainPath.reset()
        curtainPath.moveTo(0f, 0f)
        curtainPath.lineTo(w, 0f)
        curtainPath.lineTo(w, bottomY)
        curtainPath.lineTo(0f, bottomY)
        curtainPath.close()
        canvas.drawPath(curtainPath, curtainBodyPaint)

        // Vertical decorative pleats / stripes
        val pleats = 5
        for (i in 1..pleats) {
            val px = w * (i.toFloat() / (pleats + 1))
            canvas.drawLine(px, 0f, px, bottomY, stripePaint)
        }
    }

    private fun drawExtendedStick(canvas: Canvas, w: Float, stickTop: Float, stickBottom: Float) {
        val stickH = stickBottom - stickTop
        if (stickH <= 0f) return
        val borderThick = (stickH * 0.13f).coerceAtLeast(2f)

        // 1. Pole top black border matching pixel art outline
        canvas.drawRect(0f, stickTop, w, stickTop + borderThick, poleOutlinePaint)
        // 2. Pole main wooden body matching sprite wood tone
        canvas.drawRect(0f, stickTop + borderThick, w, stickBottom - borderThick, poleBodyPaint)
        // 3. Pole bottom black border matching pixel art outline
        canvas.drawRect(0f, stickBottom - borderThick, w, stickBottom, poleOutlinePaint)
    }

    private fun drawHangingSpriteCat(canvas: Canvas, centerX: Float, catTop: Float, catW: Float, catH: Float) {
        val catLeft = centerX - (catW / 2f)
        catRect.set(catLeft, catTop, catLeft + catW, catTop + catH)

        when (currentBreed) {
            CatBreed.ROLEX -> {
                val bmp = rolexBitmap ?: return
                canvas.drawBitmap(bmp, null, catRect, catPaint)
            }
            CatBreed.SATURN -> {
                val frame = if (saturnBitmaps.isNotEmpty()) {
                    val idx = saturnFrameIndex.coerceIn(0, saturnBitmaps.size - 1)
                    saturnBitmaps[idx]
                } else null
                if (frame != null) {
                    canvas.drawBitmap(frame, null, catRect, catPaint)
                }
            }
            else -> {
                val bodyBmp = bodyBitmap ?: return
                val tailBmp = tailBitmap ?: return

                // 1. Lively wagging tail behind the body
                canvas.save()
                val isTabby = currentBreed == CatBreed.TABBY
                val tailPivotXRatio = if (isTabby) 0.5207f else 0.5334f
                val tailPivotYRatio = 0.6885f
                val tailPivotX = catRect.left + catW * tailPivotXRatio
                val tailPivotY = catRect.top + catH * tailPivotYRatio

                canvas.rotate(tailAngle, tailPivotX, tailPivotY)
                canvas.drawBitmap(tailBmp, null, catRect, catPaint)
                canvas.restore()

                // 2. Body sits firmly on the stick (no wiggling of the stick)
                canvas.drawBitmap(bodyBmp, null, catRect, catPaint)
            }
        }
    }

    private fun drawFullSleepyCat(canvas: Canvas, totalH: Float, centerX: Float, totalW: Float) {
        if (currentBreed == CatBreed.SATURN && saturnFullBitmap != null) {
            val bmp = saturnFullBitmap!!
            val bmpW = (totalW * 0.48f).coerceIn(240f, 320f)
            val bmpH = bmpW * (bmp.height.toFloat() / bmp.width.toFloat())
            val dstRect = RectF(
                centerX - (bmpW / 2f),
                (totalH * 0.45f) - (bmpH / 2f),
                centerX + (bmpW / 2f),
                (totalH * 0.45f) + (bmpH / 2f)
            )
            canvas.drawBitmap(bmp, null, dstRect, catPaint)
            return
        }
        if (currentBreed == CatBreed.ROLEX && rolexBitmap != null) {
            val bmp = rolexBitmap!!
            val bmpW = (totalW * 0.44f).coerceIn(240f, 320f)
            val bmpH = bmpW * (bmp.height.toFloat() / bmp.width.toFloat())
            val dstRect = RectF(
                centerX - (bmpW / 2f),
                (totalH * 0.45f) - (bmpH / 2f),
                centerX + (bmpW / 2f),
                (totalH * 0.45f) + (bmpH / 2f)
            )
            canvas.drawBitmap(bmp, null, dstRect, catPaint)
            return
        }

        val catY = totalH * 0.45f
        val loafRect = RectF(centerX - 180f, catY - 100f, centerX + 180f, catY + 110f)
        canvas.drawRoundRect(loafRect, 90f, 90f, furPaint)

        val sleepEyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (currentBreed == CatBreed.TABBY) Color.parseColor("#FFD600") else Color.parseColor("#4E342E")
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
        }
        val leftEye = Path().apply {
            moveTo(centerX - 70f, catY - 10f)
            quadTo(centerX - 50f, catY - 30f, centerX - 30f, catY - 10f)
        }
        val rightEye = Path().apply {
            moveTo(centerX + 30f, catY - 10f)
            quadTo(centerX + 50f, catY - 30f, centerX + 70f, catY - 10f)
        }
        canvas.drawPath(leftEye, sleepEyePaint)
        canvas.drawPath(rightEye, sleepEyePaint)

        val nosePath = Path().apply {
            moveTo(centerX, catY + 5f)
            lineTo(centerX - 9f, catY - 4f)
            lineTo(centerX + 9f, catY - 4f)
            close()
        }
        canvas.drawPath(nosePath, nosePaint)

        val smilePath = Path().apply {
            moveTo(centerX, catY + 5f)
            lineTo(centerX, catY + 16f)
            quadTo(centerX - 15f, catY + 26f, centerX - 24f, catY + 16f)
            moveTo(centerX, catY + 16f)
            quadTo(centerX + 15f, catY + 26f, centerX + 24f, catY + 16f)
        }
        canvas.drawPath(smilePath, sleepEyePaint)

        val zPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (currentBreed == CatBreed.TABBY) Color.parseColor("#FFD600") else Color.parseColor("#FF6E40")
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("Z", centerX + 100f, catY - 50f, zPaint)
        zPaint.textSize = 32f
        canvas.drawText("z", centerX + 130f, catY - 80f, zPaint)
        zPaint.textSize = 24f
        canvas.drawText("z", centerX + 155f, catY - 105f, zPaint)
    }

    private fun drawSpeechBubble(canvas: Canvas, hemY: Float, centerX: Float, progress: Float) {
        if (progress < 0.96f || hemY < 75f) return

        val message = when (currentBreed) {
            CatBreed.SATURN -> "Time's up, boss. Take a break 🚬"
            CatBreed.ROLEX -> "Workout complete. Step away ⏱️"
            else -> "Daily scroll limit reached 💤"
        }

        textPaint.textSize = 30f
        var textWidth = textPaint.measureText(message)
        val maxAvailableW = width.toFloat() - 60f
        if (textWidth + 60f > maxAvailableW) {
            textPaint.textSize = 26f
            textWidth = textPaint.measureText(message)
        }

        val bubbleW = (textWidth + 60f).coerceAtMost(maxAvailableW)
        val bubbleH = 54f

        val bubbleBottom = (hemY - 16f).coerceAtLeast(36f + bubbleH)
        val bubbleTop = bubbleBottom - bubbleH

        bubbleRect.set(
            centerX - (bubbleW / 2f),
            bubbleTop,
            centerX + (bubbleW / 2f),
            bubbleBottom
        )

        pointerPath.reset()
        pointerPath.moveTo(centerX - 10f, bubbleBottom)
        pointerPath.lineTo(centerX, (bubbleBottom + 9f).coerceAtMost(hemY))
        pointerPath.lineTo(centerX + 10f, bubbleBottom)
        pointerPath.close()

        canvas.drawRoundRect(bubbleRect, 18f, 18f, speechBubblePaint)
        canvas.drawPath(pointerPath, speechBubblePaint)

        val fontMetrics = textPaint.fontMetrics
        val textY = bubbleRect.centerY() - (fontMetrics.top + fontMetrics.bottom) / 2f
        canvas.drawText(message, centerX, textY, textPaint)
    }
}
