package com.antiscroller.kitty.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.antiscroller.kitty.R

/**
 * Custom View that renders the adorable Kitty Curtain dropping down from the ceiling.
 * Supports multiple Kitty Breeds (Ginger Tabby, Midnight Black Cat).
 * Stays persistently visible on screen as the user scrolls.
 */
class KittyCurtainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class CatBreed(
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
        val speechTextColor: Int
    ) {
        ORANGE(
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
            speechTextColor = Color.parseColor("#212121")
        ),
        BLACK(
            furColor = Color.parseColor("#262626"),
            furDarkColor = Color.parseColor("#141414"),
            innerEarColor = Color.parseColor("#FF80AB"),
            pawColor = Color.parseColor("#1F1F1F"),
            pawPadColor = Color.parseColor("#FF80AB"),
            curtainBodyColor = Color.parseColor("#2A2D34"),
            curtainStripeColor = Color.parseColor("#33FFD600"),
            eyeColor = Color.parseColor("#FFD600"), // Iconic luminous golden/yellow eyes
            eyeHighlightColor = Color.WHITE,
            noseColor = Color.parseColor("#FF4081"),
            strokeColor = Color.parseColor("#111111"),
            speechBubbleBg = Color.parseColor("#1E1E1E"),
            speechTextColor = Color.parseColor("#FFD600")
        )
    }

    private var currentBreed: CatBreed = CatBreed.ORANGE
    private var dropPercentage: Float = 0f
    private var animatedProgress: Float = 0f
    private var animator: ValueAnimator? = null

    // Tail wagging animation (more active/lively)
    private var tailAngle: Float = 0f
    private var tailAnimator: ValueAnimator? = null

    // Gentle hanging body sway (pendulum swing)
    private var swayAngle: Float = 0f
    private var swayAnimator: ValueAnimator? = null

    // Pixel-art sprite bitmaps
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

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        applyTheme("orange")
        startAnimations()
    }

    fun applyTheme(themeName: String) {
        currentBreed = if (themeName.equals("black", ignoreCase = true)) {
            CatBreed.BLACK
        } else {
            CatBreed.ORANGE
        }

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

        loadCatBitmaps(themeName)
        invalidate()
    }

    private fun loadCatBitmaps(theme: String) {
        val isBlack = theme.equals("black", ignoreCase = true)
        val bodyRes = if (isBlack) R.drawable.kitty_black_body else R.drawable.kitty_orange_body
        val tailRes = if (isBlack) R.drawable.kitty_black_tail else R.drawable.kitty_orange_tail

        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        bodyBitmap = BitmapFactory.decodeResource(resources, bodyRes, options)
        tailBitmap = BitmapFactory.decodeResource(resources, tailRes, options)
    }

    private fun startAnimations() {
        // 1. Lively tail wagging (user requested: 'I think the tail will move a bit more')
        tailAnimator?.cancel()
        tailAnimator = ValueAnimator.ofFloat(-22f, 22f).apply {
            duration = 850
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                tailAngle = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        // 2. Gentle hanging body sway (pendulum swing)
        swayAnimator?.cancel()
        swayAnimator = ValueAnimator.ofFloat(-2.2f, 2.2f).apply {
            duration = 1750
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                swayAngle = it.animatedValue as Float
                invalidate()
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

        animator?.cancel()
        animator = ValueAnimator.ofFloat(animatedProgress, clamped).apply {
            duration = 420
            interpolator = OvershootInterpolator(0.7f)
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
        // At 0 scrolls: curtain and Kitty remain strictly hidden at ceiling (0px)
        if (progress <= 0.0001f) return

        val totalW = width.toFloat()
        val totalH = height.toFloat()
        if (totalW <= 0f || totalH <= 0f) return

        val centerX = totalW / 2f

        // 100% full screen lock: curtain covers entire screen, sleepy cat loaf in center
        if (progress >= 0.96f) {
            drawCurtainBody(canvas, totalW, totalH)
            drawFullSleepyCat(canvas, totalH, centerX)
            drawSpeechBubble(canvas, totalH * 0.45f - 40f, centerX, progress)
            return
        }

        // Cat display sizing based on screen width
        val catW = (totalW * 0.50f).coerceIn(400f, 620f)
        val catH = catW / 0.54f // 628 / 1162 ≈ 0.54f

        // hemY is where the curtain ends and Kitty's paws/bar grip:
        // At progress 0: hemY = -catH (entire cat is above screen, tail tip at 0)
        // At progress ~0.20: hemY = 0 (tail, legs, body, head, and hands have fully emerged)
        // At progress >0.20: curtain unrolls from 0 down to hemY behind/attached to hands
        // At progress 1.0: hemY = totalH (curtain covers full screen)
        val hemY = -catH + (progress * (totalH + catH))

        // 1. Draw solid curtain fabric from ceiling down to hands hem (only once hands enter screen)
        if (hemY > 0f) {
            drawCurtainBody(canvas, totalW, hemY)
        }

        // 2. Draw Hanging Cat sprite (body with gentle sway, tail with active wagging)
        drawHangingSpriteCat(canvas, centerX, hemY, catW, catH)

        // 3. Draw speech bubble on curtain above Kitty's hands
        drawSpeechBubble(canvas, hemY, centerX, progress)
    }

    private fun drawCurtainBody(canvas: Canvas, w: Float, h: Float) {
        if (h <= 0f) return

        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            val scallops = 6
            val step = w / scallops
            lineTo(w, h)
            for (i in scallops downTo 1) {
                val startX = i * step
                val endX = (i - 1) * step
                val midX = (startX + endX) / 2f
                val waveDepth = 12f
                quadTo(midX, h + waveDepth, endX, h)
            }
            close()
        }
        canvas.drawPath(path, curtainBodyPaint)

        // Vertical decorative pleats / stripes
        val pleats = 5
        for (i in 1..pleats) {
            val px = w * (i.toFloat() / (pleats + 1))
            canvas.drawLine(px, 0f, px, h - 4f, stripePaint)
        }
    }

    private fun drawHangingSpriteCat(canvas: Canvas, centerX: Float, hemY: Float, catW: Float, catH: Float) {
        val bodyBmp = bodyBitmap
        val tailBmp = tailBitmap
        if (bodyBmp == null || tailBmp == null) return

        val catLeft = centerX - (catW / 2f)
        // Align paws/bar with hemY
        val catTop = hemY - (catH * 0.045f)
        val catRect = RectF(catLeft, catTop, catLeft + catW, catTop + catH)

        canvas.save()
        // 1. Gentle body sway around paws holding the bar
        val pawsPivotX = catRect.left + catW * 0.50f
        val pawsPivotY = catRect.top + catH * 0.045f
        canvas.rotate(swayAngle, pawsPivotX, pawsPivotY)

        // 2. Animated lively wagging tail
        canvas.save()
        val isBlack = currentBreed == CatBreed.BLACK
        val tailPivotXRatio = if (isBlack) 0.570f else 0.589f
        val tailPivotYRatio = 0.7186f
        val tailPivotX = catRect.left + catW * tailPivotXRatio
        val tailPivotY = catRect.top + catH * tailPivotYRatio

        canvas.rotate(tailAngle, tailPivotX, tailPivotY)
        canvas.drawBitmap(tailBmp, null, catRect, catPaint)
        canvas.restore()

        // 3. Draw cat body (head, arms, torso, back feet) on top of tail
        canvas.drawBitmap(bodyBmp, null, catRect, catPaint)

        canvas.restore()
    }

    private fun drawFullSleepyCat(canvas: Canvas, totalH: Float, centerX: Float) {
        val catY = totalH * 0.45f
        val loafRect = RectF(centerX - 180f, catY - 100f, centerX + 180f, catY + 110f)
        canvas.drawRoundRect(loafRect, 90f, 90f, furPaint)

        val sleepEyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (currentBreed == CatBreed.BLACK) Color.parseColor("#FFD600") else Color.parseColor("#4E342E")
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
            color = if (currentBreed == CatBreed.BLACK) Color.parseColor("#FFD600") else Color.parseColor("#FF6E40")
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
        if (progress <= 0.001f || hemY < 75f) return

        val message = when {
            progress < 0.25f -> if (currentBreed == CatBreed.BLACK) "Void cat is watching you... 🐈‍⬛" else "Peek-a-boo! Scrolling already? 🐾"
            progress < 0.50f -> "Sliding down... maybe read a book? 📖"
            progress < 0.75f -> "Half the screen is Kitty now! 😼"
            progress < 0.95f -> "Curtain closing! Final scrolls remaining! ⚠️"
            else -> "STOPPED! Daily scroll limit reached! 💤"
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

        // Position text strictly ON the curtain, above Kitty's hands (hemY)
        val bubbleBottom = (hemY - 16f).coerceAtLeast(36f + bubbleH)
        val bubbleTop = bubbleBottom - bubbleH

        val bubbleRect = RectF(
            centerX - (bubbleW / 2f),
            bubbleTop,
            centerX + (bubbleW / 2f),
            bubbleBottom
        )

        // Triangular pointer pointing DOWN towards Kitty's hands at hemY
        val pointerPath = Path().apply {
            moveTo(centerX - 10f, bubbleBottom)
            lineTo(centerX, (bubbleBottom + 9f).coerceAtMost(hemY))
            lineTo(centerX + 10f, bubbleBottom)
            close()
        }

        canvas.drawRoundRect(bubbleRect, 18f, 18f, speechBubblePaint)
        canvas.drawPath(pointerPath, speechBubblePaint)

        val fontMetrics = textPaint.fontMetrics
        val textY = bubbleRect.centerY() - (fontMetrics.top + fontMetrics.bottom) / 2f
        canvas.drawText(message, centerX, textY, textPaint)
    }
}
