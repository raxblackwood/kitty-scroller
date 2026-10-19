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

        // 100% full screen lock: curtain covers entire screen, sleepy cat loaf in center
        if (progress >= 0.96f) {
            drawCurtainBody(canvas, totalW, totalH)
            drawFullSleepyCat(canvas, totalH, centerX)
            drawSpeechBubble(canvas, totalH * 0.45f - 40f, centerX, progress)
            return
        }

        // Hanging cat total height from hands down to tail tip is ~195f
        val catTotalHeight = 195f

        // hemY is where the curtain ends and Kitty's hands grip:
        // At progress 0: hemY = -catTotalHeight (entire cat is above screen, tail at 0)
        // At progress ~0.15: hemY = 0 (tail, legs, body, head, and hands have fully emerged)
        // At progress >0.15: curtain unrolls from 0 down to hemY behind/attached to hands
        // At progress 1.0: hemY = totalH (curtain covers full screen)
        val hemY = -catTotalHeight + (progress * (totalH + catTotalHeight))

        // 1. Draw solid curtain fabric from ceiling down to hands hem (only once hands enter screen)
        if (hemY > 0f) {
            drawCurtainBody(canvas, totalW, hemY)
        }

        // 2. Draw Hanging Cat: tail -> legs -> body -> head -> hands attached to hem
        drawHangingCat(canvas, centerX, hemY, progress)

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

    private fun drawHangingCat(canvas: Canvas, centerX: Float, hemY: Float, progress: Float) {
        val headCenterY = hemY + 45f
        val headRadius = 40f
        val bodyTop = headCenterY + 22f
        val bodyBottom = bodyTop + 65f

        // 1. Animated Wagging Tail (at the bottom)
        canvas.save()
        val tailBaseX = centerX + 12f
        val tailBaseY = bodyBottom - 8f
        canvas.rotate(tailAngle, tailBaseX, tailBaseY)
        val tailPath = Path().apply {
            moveTo(tailBaseX, tailBaseY)
            quadTo(tailBaseX + 28f, tailBaseY + 24f, tailBaseX + 18f, tailBaseY + 50f)
        }
        val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = currentBreed.furColor
            style = Paint.Style.STROKE
            strokeWidth = 14f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(tailPath, tailPaint)
        canvas.restore()

        // 2. Back Legs & Paws (dangling below body)
        val legWidth = 22f
        val legHeight = 36f
        val leftLegRect = RectF(centerX - 30f, bodyBottom - 14f, centerX - 30f + legWidth, bodyBottom - 14f + legHeight)
        val rightLegRect = RectF(centerX + 8f, bodyBottom - 14f, centerX + 8f + legWidth, bodyBottom - 14f + legHeight)
        canvas.drawRoundRect(leftLegRect, 11f, 11f, pawPaint)
        canvas.drawRoundRect(rightLegRect, 11f, 11f, pawPaint)

        // Cute paw pads on back feet
        val padPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (currentBreed == CatBreed.BLACK) Color.parseColor("#FF80AB") else Color.parseColor("#FFAB91")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(leftLegRect.centerX(), leftLegRect.bottom - 8f, 5f, padPaint)
        canvas.drawCircle(rightLegRect.centerX(), rightLegRect.bottom - 8f, 5f, padPaint)

        // 3. Body (torso hanging below head)
        val bodyRect = RectF(centerX - 34f, bodyTop, centerX + 34f, bodyBottom)
        canvas.drawRoundRect(bodyRect, 28f, 28f, furPaint)

        // Tabby stripes on body (subtle)
        if (currentBreed == CatBreed.ORANGE) {
            val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = currentBreed.furDarkColor
                strokeWidth = 3f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(centerX - 22f, bodyTop + 20f, centerX - 8f, bodyTop + 20f, stripePaint)
            canvas.drawLine(centerX + 8f, bodyTop + 20f, centerX + 22f, bodyTop + 20f, stripePaint)
            canvas.drawLine(centerX - 24f, bodyTop + 36f, centerX - 10f, bodyTop + 36f, stripePaint)
            canvas.drawLine(centerX + 10f, bodyTop + 36f, centerX + 24f, bodyTop + 36f, stripePaint)
        }

        // 4. Ears (pointing up towards hands/curtain)
        val earSize = 32f
        val leftEar = Path().apply {
            moveTo(centerX - 34f, headCenterY - 14f)
            lineTo(centerX - 42f, headCenterY - 14f - earSize)
            lineTo(centerX - 14f, headCenterY - 30f)
            close()
        }
        canvas.drawPath(leftEar, furPaint)
        val leftInnerEar = Path().apply {
            moveTo(centerX - 32f, headCenterY - 16f)
            lineTo(centerX - 38f, headCenterY - 16f - earSize * 0.65f)
            lineTo(centerX - 18f, headCenterY - 26f)
            close()
        }
        canvas.drawPath(leftInnerEar, innerEarPaint)

        val rightEar = Path().apply {
            moveTo(centerX + 34f, headCenterY - 14f)
            lineTo(centerX + 42f, headCenterY - 14f - earSize)
            lineTo(centerX + 14f, headCenterY - 30f)
            close()
        }
        canvas.drawPath(rightEar, furPaint)
        val rightInnerEar = Path().apply {
            moveTo(centerX + 32f, headCenterY - 16f)
            lineTo(centerX + 38f, headCenterY - 16f - earSize * 0.65f)
            lineTo(centerX + 18f, headCenterY - 26f)
            close()
        }
        canvas.drawPath(rightInnerEar, innerEarPaint)

        // 5. Head
        canvas.drawCircle(centerX, headCenterY, headRadius, furPaint)

        // Forehead markings
        if (currentBreed == CatBreed.ORANGE) {
            val fStripe = Path().apply {
                moveTo(centerX - 6f, headCenterY - 32f)
                lineTo(centerX, headCenterY - 22f)
                lineTo(centerX + 6f, headCenterY - 32f)
            }
            canvas.drawPath(fStripe, furDarkPaint)
        }

        // 6. Eyes
        val eyeOffsetY = headCenterY - 4f
        val eyeSpacing = 18f
        if (progress > 0.85f) {
            val eyePath = Path().apply {
                moveTo(centerX - eyeSpacing - 9f, eyeOffsetY)
                quadTo(centerX - eyeSpacing, eyeOffsetY - 7f, centerX - eyeSpacing + 9f, eyeOffsetY)
                moveTo(centerX + eyeSpacing - 9f, eyeOffsetY)
                quadTo(centerX + eyeSpacing, eyeOffsetY - 7f, centerX + eyeSpacing + 9f, eyeOffsetY)
            }
            canvas.drawPath(eyePath, strokePaint)
        } else {
            canvas.drawCircle(centerX - eyeSpacing, eyeOffsetY, 8.5f, eyePaint)
            canvas.drawCircle(centerX + eyeSpacing, eyeOffsetY, 8.5f, eyePaint)
            if (currentBreed == CatBreed.BLACK) {
                val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#111111")
                    style = Paint.Style.FILL
                }
                canvas.drawOval(RectF(centerX - eyeSpacing - 2f, eyeOffsetY - 6f, centerX - eyeSpacing + 2f, eyeOffsetY + 6f), pupilPaint)
                canvas.drawOval(RectF(centerX + eyeSpacing - 2f, eyeOffsetY - 6f, centerX + eyeSpacing + 2f, eyeOffsetY + 6f), pupilPaint)
            }
            canvas.drawCircle(centerX - eyeSpacing - 2.5f, eyeOffsetY - 2.5f, 2.5f, eyeHighlightPaint)
            canvas.drawCircle(centerX + eyeSpacing - 2.5f, eyeOffsetY - 2.5f, 2.5f, eyeHighlightPaint)
        }

        // 7. Nose & Mouth
        val noseY = headCenterY + 10f
        val nosePath = Path().apply {
            moveTo(centerX, noseY + 4f)
            lineTo(centerX - 5f, noseY)
            lineTo(centerX + 5f, noseY)
            close()
        }
        canvas.drawPath(nosePath, nosePaint)

        val mouthPath = Path().apply {
            moveTo(centerX, noseY + 4f)
            lineTo(centerX, noseY + 10f)
            moveTo(centerX, noseY + 10f)
            quadTo(centerX - 8f, noseY + 15f, centerX - 12f, noseY + 11f)
            moveTo(centerX, noseY + 10f)
            quadTo(centerX + 8f, noseY + 15f, centerX + 12f, noseY + 11f)
        }
        canvas.drawPath(mouthPath, strokePaint)

        // 8. Whiskers
        val whiskerPaint = if (currentBreed == CatBreed.BLACK) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#B0BEC5")
                strokeWidth = 2f
            }
        } else strokePaint

        canvas.drawLine(centerX - 20f, noseY + 3f, centerX - 48f, noseY - 1f, whiskerPaint)
        canvas.drawLine(centerX - 20f, noseY + 8f, centerX - 48f, noseY + 10f, whiskerPaint)
        canvas.drawLine(centerX + 20f, noseY + 3f, centerX + 48f, noseY - 1f, whiskerPaint)
        canvas.drawLine(centerX + 20f, noseY + 8f, centerX + 48f, noseY + 10f, whiskerPaint)

        // 9. Hands (Front Paws) gripping the bottom hem of the curtain
        val handTop = hemY - 12f
        val handWidth = 26f
        val handHeight = 26f
        val leftHandRect = RectF(centerX - 46f, handTop, centerX - 46f + handWidth, handTop + handHeight)
        val rightHandRect = RectF(centerX + 20f, handTop, centerX + 20f + handWidth, handTop + handHeight)
        canvas.drawRoundRect(leftHandRect, 10f, 10f, pawPaint)
        canvas.drawRoundRect(rightHandRect, 10f, 10f, pawPaint)

        // Little claw lines gripping the curtain hem
        val clawPaint = if (currentBreed == CatBreed.BLACK) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF80AB")
                strokeWidth = 2.5f
                strokeCap = Paint.Cap.ROUND
            }
        } else strokePaint

        canvas.drawLine(centerX - 40f, handTop + 6f, centerX - 40f, handTop + 14f, clawPaint)
        canvas.drawLine(centerX - 32f, handTop + 6f, centerX - 32f, handTop + 14f, clawPaint)
        canvas.drawLine(centerX + 26f, handTop + 6f, centerX + 26f, handTop + 14f, clawPaint)
        canvas.drawLine(centerX + 34f, handTop + 6f, centerX + 34f, handTop + 14f, clawPaint)
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
