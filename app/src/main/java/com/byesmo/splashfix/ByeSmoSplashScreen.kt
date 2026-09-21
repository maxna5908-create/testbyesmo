package com.byesmo.splashfix

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.splash.SplashAlignmentMath
import kotlinx.coroutines.delay

val ByeSmoSplashBackground = Color(0xFF292D32)

// Scale all RGB channels equally: increase brightness without changing hue
// or saturation. The bottom remains the original graphite shade.
private const val BackgroundTopBrightness = 1.8f
private val ByeSmoSplashBackgroundTop = ByeSmoSplashBackground.copy(
    red = ByeSmoSplashBackground.red * BackgroundTopBrightness,
    green = ByeSmoSplashBackground.green * BackgroundTopBrightness,
    blue = ByeSmoSplashBackground.blue * BackgroundTopBrightness,
)
private val ByeSmoSplashBackgroundGradient = Brush.verticalGradient(
    colors = listOf(ByeSmoSplashBackgroundTop, ByeSmoSplashBackground),
)

private const val WordmarkScale = 0.8f

/**
 * Full-window first screen, drawn inside the real launcher Activity.
 * The background is a full-height procedural graphite gradient:
 * lighter at the top, with the original #292D32 at the bottom.
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
    val captionAlpha = remember { Animatable(if (introReady) 1f else 0f) }

    LaunchedEffect(introReady, animationsEnabled) {
        if (!introReady) return@LaunchedEffect
        if (!animationsEnabled) {
            captionAlpha.snapTo(1f)
        } else {
            if (captionAlpha.value < 1f) {
                delay(40)
            }
            captionAlpha.animateTo(1f, tween(180))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ByeSmoSplashBackground),
    ) {
        // Brand logo placed in the exact center (vertical and horizontal),
        // matching the system splash screen position with zero transformation.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(288.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(wordmarkResource),
                contentDescription = "byesmo",
                modifier = Modifier.size(288.dp),
            )
        }

        // Tagline positioned directly below the centered brand logo.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 45.5.dp)
                .width(146.dp)
                .height(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            FittedTagline(
                value = tagline,
                typeface = captionTypeface,
                alpha = captionAlpha.value,
                modifier = Modifier.fillMaxSize(),
            )
        }
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
            .semantics { text = AnnotatedString(value) },
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
