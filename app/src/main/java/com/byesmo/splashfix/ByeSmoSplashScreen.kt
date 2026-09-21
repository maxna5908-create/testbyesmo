package com.byesmo.splashfix

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

val ByeSmoSplashBackground = Color(0xFF292D32)

// Scale all RGB channels equally: increase brightness without changing hue
// or saturation. The bottom remains the original graphite shade.
private const val EnableSplashGradient = false
private const val BackgroundTopBrightness = 1.8f
private val ByeSmoSplashBackgroundTop = ByeSmoSplashBackground.copy(
    red = ByeSmoSplashBackground.red * BackgroundTopBrightness,
    green = ByeSmoSplashBackground.green * BackgroundTopBrightness,
    blue = ByeSmoSplashBackground.blue * BackgroundTopBrightness,
)
private val ByeSmoSplashBackgroundGradient = Brush.verticalGradient(
    colors = listOf(ByeSmoSplashBackgroundTop, ByeSmoSplashBackground),
)

// Visible path bounds in byesmo_splash_icon.xml's 288 x 288 viewport.
// Transparent padding is excluded when calculating the requested 60% width.
private const val SystemWordmarkCanvasDp = 288f
private const val WordmarkVisibleWidthDp = 145.16869f
private const val WordmarkVisibleCenterXDp = 143.86113f
private const val WordmarkVisibleBottomDp = 163.75929f
private const val BrandResizeDurationMs = 260
private const val CaptionRevealDurationMs = 450

/**
 * Full-window first screen, drawn inside the real launcher Activity.
 * Solid graphite background; the optional procedural gradient is disabled for now.
 */
@Composable
fun ByeSmoSplashScreen(
    @DrawableRes buttonResource: Int = 0,
    @DrawableRes wordmarkResource: Int,
    tagline: String,
    captionTypeface: Typeface,
    introReady: Boolean,
    animationsEnabled: Boolean = true,
    buttonSideDp: Float = 192f,
    modifier: Modifier = Modifier,
    onButtonPositioned: ((ComposeRect) -> Unit)? = null,
) {
    // Recreation and previews start in the final state. A cold/warm launch
    // waits for MainActivity to remove the system splash and set introReady.
    val brandProgress = remember { Animatable(if (introReady) 1f else 0f) }
    val revealProgress = remember { Animatable(if (introReady) 1f else 0f) }

    LaunchedEffect(introReady, animationsEnabled) {
        if (!introReady) return@LaunchedEffect
        if (!animationsEnabled) {
            brandProgress.snapTo(1f)
            revealProgress.snapTo(1f)
        } else {
            // Sequential: finish resizing, then gently reveal the caption.
            brandProgress.animateTo(
                1f,
                tween(BrandResizeDurationMs, easing = FastOutSlowInEasing),
            )
            revealProgress.animateTo(
                1f,
                tween(
                    CaptionRevealDurationMs,
                    easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f),
                ),
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ByeSmoSplashBackground),
    ) {
        val brandWidth = maxWidth * 0.6f
        val targetScale = brandWidth.value / WordmarkVisibleWidthDp
        val brandScale = 1f + (targetScale - 1f) * brandProgress.value
        val captionHeight = brandWidth * 0.16f
        val captionCenterX = ((WordmarkVisibleCenterXDp - SystemWordmarkCanvasDp / 2f) * targetScale).dp
        val captionCenterY = ((WordmarkVisibleBottomDp - SystemWordmarkCanvasDp / 2f) * targetScale).dp +
            12.dp + captionHeight / 2f

        // Retain the gradient for later experiments, but do not draw it while disabled.
        if (EnableSplashGradient) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = revealProgress.value }
                    .background(ByeSmoSplashBackgroundGradient),
            )
        }

        // Keep the system icon's original canvas and center for the first frame.
        // Render-layer scaling avoids constraining its transparent padding to screen width.
        Image(
            painter = painterResource(wordmarkResource),
            contentDescription = "byesmo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .requiredSize(SystemWordmarkCanvasDp.dp)
                .graphicsLayer {
                    scaleX = brandScale
                    scaleY = brandScale
                },
        )

        FittedTagline(
            value = tagline,
            typeface = captionTypeface,
            alpha = revealProgress.value,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = captionCenterX, y = captionCenterY)
                .width(brandWidth)
                .height(captionHeight),
        )
    }
}

/**
 * Native glyph rendering ensures actual ink bounds match the visible logo.
 * Typeface size changes uniformly without horizontal stretching.
 * TalkBack receives the full phrase through semantics.
 */
@Composable
fun FittedTagline(
    value: String,
    typeface: Typeface,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val paint = remember(typeface) {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.LEFT
            color = android.graphics.Color.WHITE
            shader = null
        }
    }
    val bounds = remember { Rect() }

    Canvas(
        modifier = modifier
            .alpha(alpha)
            .semantics { if (alpha > 0f) text = AnnotatedString(value) },
    ) {
        if (value.isNotEmpty() && size.width > 0f && size.height > 0f) {
            paint.textSize = 1000f
            paint.getTextBounds(value, 0, value.length, bounds)
            val fittingFactor = minOf(
                size.width / bounds.width().coerceAtLeast(1),
                size.height / bounds.height().coerceAtLeast(1),
            )
            paint.textSize = 1000f * fittingFactor
            paint.getTextBounds(value, 0, value.length, bounds)
            val x = (size.width - bounds.width()) / 2f - bounds.left
            val baseline = (size.height - bounds.height()) / 2f - bounds.top
            drawContext.canvas.nativeCanvas.drawText(value, x, baseline, paint)
        }
    }
}
