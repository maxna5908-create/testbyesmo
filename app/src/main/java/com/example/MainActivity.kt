package com.example

import android.animation.ValueAnimator
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import com.byesmo.splashfix.ByeSmoSplashScreen
import com.byesmo.splashfix.configureByeSmoSplashWindow
import com.example.splash.SplashAlignmentMath
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var introReady by mutableStateOf(false)
    private var buttonSideDp by mutableStateOf(192f)
    private var targetButtonBounds: ComposeRect? = null
    private var pendingSplashProvider: SplashScreenViewProvider? = null
    private var animationsEnabled: Boolean = true
    private var fallbackRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val systemSplash = installSplashScreen() // MUST be called BEFORE super.onCreate
        super.onCreate(savedInstanceState)

        configureByeSmoSplashWindow() // BEFORE setContent / first app frame

        animationsEnabled = if (Build.VERSION.SDK_INT >= 26) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            Settings.Global.getFloat(
                contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
            ) > 0f
        }

        // On Activity recreation without an OS starting window, signal readiness immediately.
        if (savedInstanceState != null) {
            introReady = true
        } else {
            val fallback = Runnable {
                pendingSplashProvider?.let { provider ->
                    pendingSplashProvider = null
                    provider.remove()
                    configureByeSmoSplashWindow()
                    introReady = true
                }
            }
            fallbackRunnable = fallback
            window.decorView.postDelayed(fallback, 600L)

            systemSplash.setOnExitAnimationListener { provider ->
                handleSplashExit(provider)
            }
        }

        val captionTypeface = ResourcesCompat.getFont(this, R.font.byesmo_splash_font)
            ?: Typeface.create("sans-serif", Typeface.NORMAL)

        setContent {
            MyApplicationTheme {
                ByeSmoSplashScreen(
                    buttonResource = R.drawable.byesmo_button_v3,
                    wordmarkResource = R.drawable.byesmo_splash_wordmark,
                    tagline = stringResource(R.string.byesmo_v3_tagline),
                    captionTypeface = captionTypeface,
                    introReady = introReady,
                    animationsEnabled = animationsEnabled,
                    buttonSideDp = buttonSideDp,
                    onButtonPositioned = { bounds ->
                        onButtonPositioned(bounds)
                    },
                )
            }
        }
    }

    private fun handleSplashExit(provider: SplashScreenViewProvider) {
        if (isFinishing || isDestroyed) return
        fallbackRunnable?.let {
            window.decorView.removeCallbacks(it)
            fallbackRunnable = null
        }
        provider.remove()
        configureByeSmoSplashWindow()
        introReady = true
    }

    private fun onButtonPositioned(bounds: ComposeRect) {
        targetButtonBounds = bounds
        val provider = pendingSplashProvider
        if (provider != null) {
            pendingSplashProvider = null
            executeTransition(provider, bounds)
        }
    }

    private fun executeTransition(
        provider: SplashScreenViewProvider,
        targetBounds: ComposeRect,
    ) {
        fallbackRunnable?.let {
            window.decorView.removeCallbacks(it)
            fallbackRunnable = null
        }

        val iconView = provider.iconView
        if (iconView == null) {
            provider.remove()
            configureByeSmoSplashWindow()
            introReady = true
            return
        }

        (provider.view as? android.view.ViewGroup)?.clipChildren = false
        (provider.view as? android.view.ViewGroup)?.clipToPadding = false
        (iconView.parent as? android.view.ViewGroup)?.clipChildren = false
        (iconView.parent as? android.view.ViewGroup)?.clipToPadding = false

        if (iconView.width <= 0 || iconView.height <= 0) {
            iconView.post {
                executeTransition(provider, targetBounds)
            }
            return
        }

        val targetImageView = when (iconView) {
            is android.widget.ImageView -> iconView
            is android.view.ViewGroup -> {
                var found: android.widget.ImageView? = null
                for (i in 0 until iconView.childCount) {
                    val child = iconView.getChildAt(i)
                    if (child is android.widget.ImageView) {
                        found = child
                        break
                    }
                }
                found
            }
            else -> null
        }

        val drawable = targetImageView?.drawable ?: (iconView as? android.widget.ImageView)?.drawable
        val bounds = drawable?.bounds ?: android.graphics.Rect(0, 0, iconView.width, iconView.height)
        val matrix = targetImageView?.imageMatrix ?: (iconView as? android.widget.ImageView)?.imageMatrix ?: android.graphics.Matrix()

        val srcRect = android.graphics.RectF(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat()
        )
        if (srcRect.isEmpty) {
            srcRect.set(0f, 0f, iconView.width.toFloat(), iconView.height.toFloat())
        }
        val dstRect = android.graphics.RectF()
        matrix.mapRect(dstRect, srcRect)

        val renderedSystemCanvasSidePx = if (dstRect.width() > 0f && dstRect.height() > 0f) {
            minOf(dstRect.width(), dstRect.height())
        } else {
            minOf(iconView.width.toFloat(), iconView.height.toFloat())
        }

        val iconScreenLoc = IntArray(2)
        (targetImageView ?: iconView).getLocationOnScreen(iconScreenLoc)
        val systemCenterX = iconScreenLoc[0] + dstRect.centerX()
        val systemCenterY = iconScreenLoc[1] + dstRect.centerY()

        val windowScreenLoc = IntArray(2)
        window.decorView.getLocationOnScreen(windowScreenLoc)
        val targetCenterX = windowScreenLoc[0] + targetBounds.left + targetBounds.width / 2f
        val targetCenterY = windowScreenLoc[1] + targetBounds.top + targetBounds.height / 2f

        val targetMath = SplashAlignmentMath.target(
            renderedSystemCanvasSidePx = renderedSystemCanvasSidePx,
            systemButtonCenterXScreenPx = systemCenterX,
            systemButtonCenterYScreenPx = systemCenterY,
        )

        val density = resources.displayMetrics.density
        val measuredSideDp = SplashAlignmentMath.pixelsToDp(targetMath.imageSide, density)
        if (kotlin.math.abs(buttonSideDp - measuredSideDp) > 0.5f) {
            buttonSideDp = measuredSideDp
        }

        // Layout is confirmed ready and UI button center/size match the system button.
        // Remove system overlay immediately without any translation, scale or alpha animations.
        provider.remove()
        configureByeSmoSplashWindow()
        introReady = true
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun SplashScreenPreview() {
    MyApplicationTheme {
        SplashScreen()
    }
}
