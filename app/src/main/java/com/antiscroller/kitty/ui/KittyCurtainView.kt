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
        ),
        ROLEX(
            key = "rolex",
            displayName = "Rolex",
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
        )
    }

    private var currentBreed: CatBreed = CatBreed.GINGER
    private var dropPercentage: Float = 0f
    private var animatedProgress: Float = 0f
    private var animator: ValueAnimator? = null

    // Tail wagging animation for Ginger & Tabby
    private var tailAngle: Float = 0f
    private var tailAnimator: ValueAnimator? = null

    // Body swaying animation for Ginger & Tabby (gradient starting midway down the hands)
    private var bodySwayOffset: Float = 0f
    private var swayAnimator: ValueAnimator? = null
    private val meshCols = 4
    private val meshRows = 12
    private val bodyMeshVerts = FloatArray((meshCols + 1) * (meshRows + 1) * 2)

    // Animated 8-frame sprite animation loops for Saturn & Rolex
    private var spriteFrameIndex: Int = 0
    private var spriteAnimator: ValueAnimator? = null
    private val saturnBitmaps = mutableListOf<Bitmap>()
    private var saturnFullBitmap: Bitmap? = null
    private val rolexBitmaps = mutableListOf<Bitmap>()
    private var rolexFullBitmap: Bitmap? = null

    // Loaf bitmap for full curtain lock (custom per-breed pixel-art cat loaf)
    private var loafBitmap: Bitmap? = null

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
            loafBitmap == null ||
            (newBreed == CatBreed.SATURN && saturnBitmaps.isEmpty()) ||
            (newBreed == CatBreed.ROLEX && rolexBitmaps.isEmpty()) ||
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
            saturnFullBitmap = BitmapFactory.decodeResource(resources, R.drawable.kitty_saturn, options)
        } else if (currentBreed == CatBreed.ROLEX) {
            rolexBitmaps.clear()
            val rolexResIds = intArrayOf(
                R.drawable.kitty_rolex_0,
                R.drawable.kitty_rolex_1,
                R.drawable.kitty_rolex_2,
                R.drawable.kitty_rolex_3,
                R.drawable.kitty_rolex_4,
                R.drawable.kitty_rolex_5,
                R.drawable.kitty_rolex_6,
                R.drawable.kitty_rolex_7
            )
            for (resId in rolexResIds) {
                val bmp = BitmapFactory.decodeResource(resources, resId, options)
                if (bmp != null) rolexBitmaps.add(bmp)
            }
            rolexFullBitmap = BitmapFactory.decodeResource(resources, R.drawable.kitty_rolex_full, options)
        } else {
            val isTabby = currentBreed == CatBreed.TABBY
            val bodyRes = if (isTabby) R.drawable.kitty_black_body else R.drawable.kitty_orange_body
            val tailRes = if (isTabby) R.drawable.kitty_black_tail else R.drawable.kitty_orange_tail

            bodyBitmap = BitmapFactory.decodeResource(resources, bodyRes, options)
            tailBitmap = BitmapFactory.decodeResource(resources, tailRes, options)
        }

        // Load custom per-breed pixel-art loaf icon for full lock
        val loafRes = when (currentBreed) {
            CatBreed.GINGER -> R.drawable.kitty_ginger_loaf
            CatBreed.TABBY -> R.drawable.kitty_tabby_loaf
            CatBreed.SATURN -> R.drawable.kitty_saturn_loaf
            CatBreed.ROLEX -> R.drawable.kitty_rolex_loaf
        }
        loafBitmap = BitmapFactory.decodeResource(resources, loafRes, options)
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

        // 2. Slow, gentle body swaying/blowing in the breeze for Ginger & Tabby
        swayAnimator?.cancel()
        swayAnimator = ValueAnimator.ofFloat(-9f, 9f).apply {
            duration = 2400
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                bodySwayOffset = it.animatedValue as Float
                if (currentBreed != CatBreed.SATURN && currentBreed != CatBreed.ROLEX) {
                    invalidate()
                }
            }
            start()
        }

        // 3. Fluid 8-frame cartoon animation loop for Saturn & Rolex (~120ms per frame)
        spriteAnimator?.cancel()
        spriteAnimator = ValueAnimator.ofInt(0, 7).apply {
            duration = 960
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener {
                val newIndex = it.animatedValue as Int
                if (newIndex != spriteFrameIndex) {
                    spriteFrameIndex = newIndex
                    if (currentBreed == CatBreed.SATURN || currentBreed == CatBreed.ROLEX) {
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
        val catW = if (isSaturn) (totalW * 0.28f).coerceIn(240f, 320f) else (totalW * 0.22f).coerceIn(210f, 270f)
        // Aspect ratio: Saturn is 588x587 (~1.0), Rolex is 280x475 (~0.589), Ginger/Tabby is 628x1162 (~0.540)
        val catH = when {
            isSaturn -> catW * (587f / 588f)
            isRolex -> (catW / 0.589f)
            else -> (catW / 0.540f)
        }

        // Stick offsets within the sprite coordinate space:
        // Saturn (pull-up): stick y=18..56 in 587 height space
        // Rolex (gangster): stick y=30..46 in 475 height space
        // Ginger/Tabby: stick y=30..72 in 1162 height space
        val stickTopRatio = when {
            isSaturn -> (18f / 587f)
            isRolex -> (30f / 475f)
            else -> (30f / 1162f)
        }
        val stickBotRatio = when {
            isSaturn -> (56f / 587f)
            isRolex -> (46f / 475f)
            else -> (72f / 1162f)
        }

        // The cat slides down smoothly from above the ceiling:
        // At progress = 0: cat is hidden above ceiling with its tail right at the top edge (catBottom = 0).
        // As scroll begins: tail emerges first, followed by legs, body, head, and paws.
        // Once stick reaches y=0, the screen-wide wooden pole and curtain unroll behind it.
        val startStickTop = -catH * (1f - stickTopRatio)
        val stickTop = startStickTop + progress * (totalH - startStickTop)
        val stickBottom = stickTop + (catH * (stickBotRatio - stickTopRatio))

        canvas.save()
        canvas.clipRect(0f, 0f, totalW, totalH)

        // 1. Draw solid curtain fabric from ceiling straight down to top edge of the stick
        if (stickTop > 0f) {
            drawCurtainBody(canvas, totalW, stickTop + 1.5f)
            // 2. Extend the stick seamlessly across the entire width of the curtain
            drawExtendedStick(canvas, totalW, stickTop, stickBottom)
        }

        // 3. Draw Hanging Cat sprite (stick stays rock-solid, animated paws/body/tail)
        val catTop = stickTop - (catH * stickTopRatio)
        drawHangingSpriteCat(canvas, centerX, catTop, catW, catH)

        canvas.restore()
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
            CatBreed.SATURN -> {
                val frame = if (saturnBitmaps.isNotEmpty()) {
                    val idx = spriteFrameIndex.coerceIn(0, saturnBitmaps.size - 1)
                    saturnBitmaps[idx]
                } else null
                if (frame != null) {
                    canvas.drawBitmap(frame, null, catRect, catPaint)
                }
            }
            CatBreed.ROLEX -> {
                val frame = if (rolexBitmaps.isNotEmpty()) {
                    val idx = spriteFrameIndex.coerceIn(0, rolexBitmaps.size - 1)
                    rolexBitmaps[idx]
                } else null
                if (frame != null) {
                    canvas.drawBitmap(frame, null, catRect, catPaint)
                }
            }
            else -> {
                val bodyBmp = bodyBitmap ?: return
                val tailBmp = tailBitmap ?: return

                val isTabby = currentBreed == CatBreed.TABBY
                val tailPivotXRatio = if (isTabby) 0.5207f else 0.5334f
                val tailPivotYRatio = 0.6885f

                // Hip displacement at y = 0.6885f (follows body sway gradient)
                val hipSwayFactor = Math.pow(((tailPivotYRatio - 0.086f) / (1f - 0.086f)).coerceIn(0f, 1f).toDouble(), 1.2).toFloat()
                val hipDx = bodySwayOffset * hipSwayFactor

                // 1. Lively wagging tail behind the body, swaying seamlessly with the hips
                canvas.save()
                val tailPivotX = catRect.left + catW * tailPivotXRatio + hipDx
                val tailPivotY = catRect.top + catH * tailPivotYRatio
                canvas.rotate(tailAngle, tailPivotX, tailPivotY)

                catRect.offset(hipDx, 0f)
                canvas.drawBitmap(tailBmp, null, catRect, catPaint)
                catRect.offset(-hipDx, 0f)
                canvas.restore()

                // 2. Body gentle sway: gradient displacement starts midway down the hands (yRatio > 0.086f).
                // The pole and paws gripping the pole stay 100% rock-solid and stationary (displacement = 0).
                var vertIdx = 0
                for (r in 0..meshRows) {
                    val yRatio = r.toFloat() / meshRows
                    val origY = catRect.top + catH * yRatio
                    val swayFactor = if (yRatio <= 0.086f) {
                        0f
                    } else {
                        Math.pow(((yRatio - 0.086f) / (1f - 0.086f)).toDouble(), 1.2).toFloat()
                    }
                    val dx = bodySwayOffset * swayFactor

                    for (c in 0..meshCols) {
                        val xRatio = c.toFloat() / meshCols
                        val origX = catRect.left + catW * xRatio
                        bodyMeshVerts[vertIdx++] = origX + dx
                        bodyMeshVerts[vertIdx++] = origY
                    }
                }
                canvas.drawBitmapMesh(bodyBmp, meshCols, meshRows, bodyMeshVerts, 0, null, 0, catPaint)
            }
        }
    }

    private fun drawFullSleepyCat(canvas: Canvas, totalH: Float, centerX: Float, totalW: Float) {
        if (currentBreed == CatBreed.ROLEX && rolexFullBitmap != null) {
            val bmp = rolexFullBitmap!!
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
        if (currentBreed == CatBreed.SATURN && saturnFullBitmap != null) {
            val bmp = saturnFullBitmap!!
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

        // For Ginger & Tabby: draw the custom pixel-art sleeping loaf cat!
        val bmp = loafBitmap
        if (bmp != null) {
            val bmpW = (totalW * 0.46f).coerceIn(240f, 360f)
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
    }

    private fun drawSpeechBubble(canvas: Canvas, hemY: Float, centerX: Float, progress: Float) {
        if (progress < 0.96f || hemY < 75f) return

        val message = "It's time to Clawse."

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
