package com.antiscroller.kitty.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator

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

    // Tail wagging animation offset
    private var tailAngle: Float = 0f
    private var tailAnimator: ValueAnimator? = null

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
        startTailAnimation()
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

        invalidate()
    }

    private fun startTailAnimation() {
        tailAnimator = ValueAnimator.ofFloat(-14f, 14f).apply {
            duration = 1300
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                tailAngle = it.animatedValue as Float
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

        // Kitty vertical center: descends steadily as a warning indicator (from 160px down to ~82% of screen)
        val minKittyY = 160f
        val maxKittyY = totalH * 0.82f
        val kittyY = minKittyY + (progress * (maxKittyY - minKittyY))

        // Solid blocking curtain: unrolls strictly in the final 20% before limit lock (progress > 0.80)
        val curtainH = if (progress > 0.80f) {
            val curtainProgress = ((progress - 0.80f) / 0.20f).coerceIn(0f, 1f)
            curtainProgress * totalH
        } else {
            0f
        }

        // Draw solid curtain fabric from ceiling down if in final 20%
        if (curtainH > 0f) {
            drawCurtainBody(canvas, totalW, curtainH)
        }

        // Draw Kitty Face & Paws (transition to Sleepy Cat loaf at 96%+)
        if (progress >= 0.96f) {
            drawFullSleepyCat(canvas, totalH, centerX)
        } else {
            drawSlidingKitty(canvas, kittyY, centerX, curtainH, progress)
        }

        // Draw Speech Bubble above Kitty's ears
        drawSpeechBubble(canvas, kittyY, centerX, progress)
    }

    private fun drawCurtainBody(canvas: Canvas, w: Float, h: Float) {
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
                val waveDepth = 15f
                quadTo(midX, h + waveDepth, endX, h)
            }
            close()
        }
        canvas.drawPath(path, curtainBodyPaint)

        // Vertical decorative pleats / stripes
        val pleats = 5
        for (i in 1..pleats) {
            val px = w * (i.toFloat() / (pleats + 1))
            canvas.drawLine(px, 0f, px, h - 5f, stripePaint)
        }
    }

    private fun drawSlidingKitty(canvas: Canvas, kittyY: Float, centerX: Float, curtainH: Float, progress: Float) {
        val headRadius = 75f
        val headCenterY = kittyY

        // 1. Paws holding shade cords (0-80%) or bottom curtain hem (80-100%)
        if (curtainH > 0f) {
            drawCurtainPaws(canvas, centerX, curtainH)
        } else {
            drawPullCordsAndPaws(canvas, centerX, kittyY)
        }

        // 2. Ears
        val earSize = 45f
        val leftEar = Path().apply {
            moveTo(centerX - 60f, headCenterY - 30f)
            lineTo(centerX - 80f, headCenterY - 30f - earSize)
            lineTo(centerX - 25f, headCenterY - 55f)
            close()
        }
        canvas.drawPath(leftEar, furPaint)
        val leftInnerEar = Path().apply {
            moveTo(centerX - 55f, headCenterY - 32f)
            lineTo(centerX - 72f, headCenterY - 32f - earSize * 0.7f)
            lineTo(centerX - 32f, headCenterY - 50f)
            close()
        }
        canvas.drawPath(leftInnerEar, innerEarPaint)

        val rightEar = Path().apply {
            moveTo(centerX + 60f, headCenterY - 30f)
            lineTo(centerX + 80f, headCenterY - 30f - earSize)
            lineTo(centerX + 25f, headCenterY - 55f)
            close()
        }
        canvas.drawPath(rightEar, furPaint)
        val rightInnerEar = Path().apply {
            moveTo(centerX + 55f, headCenterY - 32f)
            lineTo(centerX + 72f, headCenterY - 32f - earSize * 0.7f)
            lineTo(centerX + 32f, headCenterY - 50f)
            close()
        }
        canvas.drawPath(rightInnerEar, innerEarPaint)

        // 3. Head shape
        canvas.drawCircle(centerX, headCenterY, headRadius, furPaint)

        // Forehead stripes (subtle on black cat)
        val foreheadStripe = Path().apply {
            moveTo(centerX - 10f, headCenterY - 65f)
            lineTo(centerX, headCenterY - 45f)
            lineTo(centerX + 10f, headCenterY - 65f)
        }
        canvas.drawPath(foreheadStripe, furDarkPaint)

        // 4. Eyes (Glowing yellow/gold on black cat, dark on ginger)
        val eyeOffsetY = headCenterY - 8f
        val eyeSpacing = 32f
        if (dropPercentage > 0.75f) {
            val eyePath = Path().apply {
                moveTo(centerX - eyeSpacing - 14f, eyeOffsetY)
                quadTo(centerX - eyeSpacing, eyeOffsetY - 10f, centerX - eyeSpacing + 14f, eyeOffsetY)
                moveTo(centerX + eyeSpacing - 14f, eyeOffsetY)
                quadTo(centerX + eyeSpacing, eyeOffsetY - 10f, centerX + eyeSpacing + 14f, eyeOffsetY)
            }
            canvas.drawPath(eyePath, strokePaint)
        } else {
            canvas.drawCircle(centerX - eyeSpacing, eyeOffsetY, 13f, eyePaint)
            canvas.drawCircle(centerX + eyeSpacing, eyeOffsetY, 13f, eyePaint)
            // Pupils on black cat
            if (currentBreed == CatBreed.BLACK) {
                val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#111111")
                    style = Paint.Style.FILL
                }
                canvas.drawOval(RectF(centerX - eyeSpacing - 3f, eyeOffsetY - 8f, centerX - eyeSpacing + 3f, eyeOffsetY + 8f), pupilPaint)
                canvas.drawOval(RectF(centerX + eyeSpacing - 3f, eyeOffsetY - 8f, centerX + eyeSpacing + 3f, eyeOffsetY + 8f), pupilPaint)
            }
            // Highlights
            canvas.drawCircle(centerX - eyeSpacing - 4f, eyeOffsetY - 4f, 4f, eyeHighlightPaint)
            canvas.drawCircle(centerX + eyeSpacing - 4f, eyeOffsetY - 4f, 4f, eyeHighlightPaint)
        }

        // 5. Nose & Mouth
        val noseY = headCenterY + 12f
        val nosePath = Path().apply {
            moveTo(centerX, noseY + 6f)
            lineTo(centerX - 7f, noseY)
            lineTo(centerX + 7f, noseY)
            close()
        }
        canvas.drawPath(nosePath, nosePaint)

        val mouthPath = Path().apply {
            moveTo(centerX, noseY + 6f)
            lineTo(centerX, noseY + 14f)
            moveTo(centerX, noseY + 14f)
            quadTo(centerX - 12f, noseY + 22f, centerX - 18f, noseY + 16f)
            moveTo(centerX, noseY + 14f)
            quadTo(centerX + 12f, noseY + 22f, centerX + 18f, noseY + 16f)
        }
        canvas.drawPath(mouthPath, strokePaint)

        // 6. Whiskers (Light on black cat, dark on ginger)
        val whiskerPaint = if (currentBreed == CatBreed.BLACK) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#B0BEC5")
                strokeWidth = 2.5f
            }
        } else strokePaint

        canvas.drawLine(centerX - 35f, noseY + 5f, centerX - 75f, noseY - 2f, whiskerPaint)
        canvas.drawLine(centerX - 35f, noseY + 12f, centerX - 75f, noseY + 15f, whiskerPaint)
        canvas.drawLine(centerX + 35f, noseY + 5f, centerX + 75f, noseY - 2f, whiskerPaint)
        canvas.drawLine(centerX + 35f, noseY + 12f, centerX + 75f, noseY + 15f, whiskerPaint)

        // 7. Dangling Bottom Paws & Tail
        drawBottomPawsAndTail(canvas, centerX, headCenterY + headRadius)
    }

    private fun drawPullCordsAndPaws(canvas: Canvas, centerX: Float, kittyY: Float) {
        val cordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (currentBreed == CatBreed.BLACK) Color.parseColor("#80B0BEC5") else Color.parseColor("#908D6E63")
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        val pawTop = kittyY - 78f
        // Pull cords extending from ceiling down to paws
        canvas.drawLine(centerX - 68f, 0f, centerX - 68f, pawTop, cordPaint)
        canvas.drawLine(centerX + 68f, 0f, centerX + 68f, pawTop, cordPaint)

        // Little bead on cords
        val beadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (currentBreed == CatBreed.BLACK) Color.parseColor("#FFD600") else Color.parseColor("#FF8A65")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX - 68f, pawTop - 8f, 6f, beadPaint)
        canvas.drawCircle(centerX + 68f, pawTop - 8f, 6f, beadPaint)

        // Paws gripping the cords
        val leftPawRect = RectF(centerX - 82f, pawTop, centerX - 54f, pawTop + 24f)
        val rightPawRect = RectF(centerX + 54f, pawTop, centerX + 82f, pawTop + 24f)
        canvas.drawRoundRect(leftPawRect, 12f, 12f, pawPaint)
        canvas.drawRoundRect(rightPawRect, 12f, 12f, pawPaint)
    }

    private fun drawCurtainPaws(canvas: Canvas, centerX: Float, curtainH: Float) {
        val pawTop = (curtainH - 12f).coerceAtLeast(0f)
        val leftPawRect = RectF(centerX - 85f, pawTop, centerX - 55f, pawTop + 26f)
        val rightPawRect = RectF(centerX + 55f, pawTop, centerX + 85f, pawTop + 26f)
        canvas.drawRoundRect(leftPawRect, 12f, 12f, pawPaint)
        canvas.drawRoundRect(rightPawRect, 12f, 12f, pawPaint)
    }

    private fun drawBottomPawsAndTail(canvas: Canvas, centerX: Float, bottomY: Float) {
        canvas.drawOval(RectF(centerX - 50f, bottomY - 10f, centerX - 20f, bottomY + 22f), pawPaint)
        canvas.drawOval(RectF(centerX + 20f, bottomY - 10f, centerX + 50f, bottomY + 22f), pawPaint)

        // Animated Wagging Tail
        canvas.save()
        canvas.rotate(tailAngle, centerX + 55f, bottomY)
        val tailPath = Path().apply {
            moveTo(centerX + 45f, bottomY)
            quadTo(centerX + 85f, bottomY + 25f, centerX + 75f, bottomY + 55f)
        }
        val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = currentBreed.furColor
            style = Paint.Style.STROKE
            strokeWidth = 14f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(tailPath, tailPaint)
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

    private fun drawSpeechBubble(canvas: Canvas, kittyY: Float, centerX: Float, progress: Float) {
        if (progress <= 0.001f) return

        val message = when {
            progress < 0.25f -> if (currentBreed == CatBreed.BLACK) "Void cat is watching you... 🐈‍⬛" else "Peek-a-boo! Scrolling already? 🐾"
            progress < 0.50f -> "Sliding down... maybe read a book? 📖"
            progress < 0.75f -> "Halfway there! Take a breath! 😼"
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
        val bubbleH = 56f

        // Position text strictly above Kitty's head & ears
        val earTipY = if (progress >= 0.96f) {
            height.toFloat() * 0.45f - 100f // Above sleepy cat loaf
        } else {
            val headRadius = 75f
            val earSize = 45f
            kittyY - headRadius - earSize // Top tip of ears
        }

        if (earTipY < 40f) {
            return
        }

        val bubbleBottom = (earTipY - 14f)
        val bubbleY = (bubbleBottom - bubbleH).coerceAtLeast(34f)

        val bubbleRect = RectF(
            centerX - (bubbleW / 2f),
            bubbleY,
            centerX + (bubbleW / 2f),
            bubbleY + bubbleH
        )

        // Triangular pointer pointing DOWN towards Kitty's head
        val pointerPath = Path().apply {
            moveTo(centerX - 12f, bubbleY + bubbleH)
            lineTo(centerX, (bubbleY + bubbleH + 11f).coerceAtMost(earTipY))
            lineTo(centerX + 12f, bubbleY + bubbleH)
            close()
        }

        canvas.drawRoundRect(bubbleRect, 18f, 18f, speechBubblePaint)
        canvas.drawPath(pointerPath, speechBubblePaint)

        val fontMetrics = textPaint.fontMetrics
        val textY = bubbleRect.centerY() - (fontMetrics.top + fontMetrics.bottom) / 2f
        canvas.drawText(message, centerX, textY, textPaint)
    }
}
