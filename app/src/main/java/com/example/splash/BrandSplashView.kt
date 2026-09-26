package com.example.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import androidx.core.content.res.ResourcesCompat
import com.example.R

data class SplashAssets(val bye: Bitmap, val smo: Bitmap, val typeface: Typeface) {
    companion object {
        /** Call off the UI thread, before releasing the system splash. */
        fun load(context: Context): SplashAssets {
            val requestedWidth = (context.resources.displayMetrics.widthPixels * 0.6f).coerceAtLeast(1f)
            var sample = 1
            while (SplashMotion.INK_WIDTH / (sample * 2) >= requestedWidth) sample *= 2
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            return SplashAssets(
                requireNotNull(BitmapFactory.decodeResource(context.resources, R.drawable.byesmo_bye_red, options))
                    .apply { prepareToDraw() },
                requireNotNull(BitmapFactory.decodeResource(context.resources, R.drawable.byesmo_smo_white, options))
                    .apply { prepareToDraw() },
                ResourcesCompat.getFont(context, R.font.byesmo_splash_font)
                    ?: Typeface.create("sans-serif", Typeface.NORMAL),
            )
        }
    }
}

/** One native, hardware-accelerated canvas; no bitmap decoding or layout per frame. */
class BrandSplashView(
    context: Context,
    private val assets: SplashAssets,
    private val tagline: String,
    private val animationsEnabled: Boolean,
    private val preview: Boolean = false,
    previewTimeMs: Long = SplashMotion.TOTAL_MS,
    initiallySettled: Boolean = false,
) : View(context) {
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = Color.WHITE
        typeface = assets.typeface
    }
    private val captionBounds = Rect()
    private val byeRect = RectF(0f, 0f, SplashMotion.SPLIT_X, SplashMotion.SOURCE_HEIGHT)
    private val smoRect = RectF(SplashMotion.SPLIT_X, 0f, SplashMotion.SOURCE_WIDTH, SplashMotion.SOURCE_HEIGHT)
    private val movement = PathInterpolator(0.22f, 1f, 0.36f, 1f)
    private val fade = PathInterpolator(0.42f, 0f, 0.58f, 1f)
    private var animator: ValueAnimator? = null
    private var playRequested = false
    private var finished = initiallySettled
    private var elapsedMs = when {
        preview -> previewTimeMs.toFloat()
        initiallySettled -> SplashMotion.TOTAL_MS.toFloat()
        else -> 0f
    }
    private val logoHitBounds = RectF()
    private var logoScale = 1f
    private var logoLeft = 0f
    private var logoTop = 0f
    private var captionX = 0f
    private var captionBaseline = 0f

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "byesmo. $tagline. Нажмите, чтобы повторить анимацию"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val insideLogo = logoHitBounds.contains(event.x, event.y)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!finished || !insideLogo) return false
                isPressed = true
            }
            MotionEvent.ACTION_MOVE -> if (!insideLogo) isPressed = false
            MotionEvent.ACTION_UP -> {
                val clicked = isPressed && insideLogo
                isPressed = false
                if (clicked) performClick()
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> isPressed = false
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        if (!preview && finished && animationsEnabled) {
            finished = false
            elapsedMs = 0f
            invalidate()
            play()
        }
        return true
    }

    fun play() {
        playRequested = true
        startIfReady()
    }

    private fun startIfReady() {
        if (preview || finished || !playRequested || !isAttachedToWindow || width == 0 ||
            windowVisibility != VISIBLE || animator != null) return
        if (!animationsEnabled) {
            finish()
            return
        }
        animator = ValueAnimator.ofFloat(0f, SplashMotion.TOTAL_MS.toFloat()).apply {
            duration = SplashMotion.TOTAL_MS
            interpolator = LinearInterpolator()
            addUpdateListener {
                elapsedMs = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = finish()
            })
            start()
        }
    }

    private fun finish() {
        if (finished) return
        finished = true
        elapsedMs = SplashMotion.TOTAL_MS.toFloat()
        animator = null
        invalidate()
    }

    fun dispose() {
        animator?.removeAllListeners()
        animator?.removeAllUpdateListeners()
        animator?.cancel()
        animator = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (windowVisibility == VISIBLE) animator?.resume()
        startIfReady()
    }

    override fun onDetachedFromWindow() {
        animator?.pause()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            animator?.resume()
            startIfReady()
        } else {
            animator?.pause()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        logoScale = w * 0.6f / SplashMotion.INK_WIDTH
        logoLeft = w / 2f - SplashMotion.CENTER_X * logoScale
        logoTop = h / 2f - (SplashMotion.INK_TOP + SplashMotion.INK_BOTTOM) / 2f * logoScale
        logoHitBounds.set(
            logoLeft + SplashMotion.INK_LEFT * logoScale,
            logoTop + SplashMotion.INK_TOP * logoScale,
            logoLeft + SplashMotion.INK_RIGHT * logoScale,
            logoTop + SplashMotion.INK_BOTTOM * logoScale,
        )
        // Fit actual glyph bounds uniformly to the visible brand width; never stretch text.
        captionPaint.textSize = 1000f
        captionPaint.getTextBounds(tagline, 0, tagline.length, captionBounds)
        captionPaint.textSize *= w * 0.6f / captionBounds.width().coerceAtLeast(1)
        captionPaint.getTextBounds(tagline, 0, tagline.length, captionBounds)
        captionX = (w - captionBounds.width()) / 2f - captionBounds.left
        captionBaseline = logoTop + SplashMotion.INK_BOTTOM * logoScale +
            24f * resources.displayMetrics.density - captionBounds.top
        startIfReady()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val grow = movement.getInterpolation(SplashMotion.progress(elapsedMs, 0, SplashMotion.GROW_MS))
        val unfold = movement.getInterpolation(SplashMotion.progress(elapsedMs, SplashMotion.GROW_MS, SplashMotion.UNFOLD_MS))
        val caption = fade.getInterpolation(SplashMotion.progress(elapsedMs, SplashMotion.CAPTION_START_MS, SplashMotion.CAPTION_MS))
        canvas.drawColor(Color.rgb(41, 45, 50))

        bitmapPaint.alpha = (255 * grow).toInt()
        val shift = SplashMotion.TRAVEL * (1f - unfold)
        val frame = canvas.save()
        // Growth is centered on the screen. Unfolding starts only after growth completes.
        canvas.scale(grow, grow, width / 2f, height / 2f)
        canvas.translate(logoLeft, logoTop)
        canvas.scale(logoScale, logoScale)

        if (unfold > 0f) {
            val reveal = canvas.save()
            // Moving hard clip hides smo even through the counters/holes in bye.
            canvas.clipRect(SplashMotion.BYE_INK_RIGHT + shift, 0f,
                SplashMotion.SOURCE_WIDTH, SplashMotion.SOURCE_HEIGHT)
            canvas.translate(-shift, 0f)
            canvas.drawBitmap(assets.smo, null, smoRect, bitmapPaint)
            canvas.restoreToCount(reveal)
        }
        canvas.translate(shift, 0f)
        canvas.drawBitmap(assets.bye, null, byeRect, bitmapPaint)
        canvas.restoreToCount(frame)

        if (caption > 0f) {
            captionPaint.alpha = (255 * caption).toInt()
            canvas.drawText(tagline, captionX, captionBaseline, captionPaint)
        }
    }
}
