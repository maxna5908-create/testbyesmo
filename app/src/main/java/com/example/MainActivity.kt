package com.example

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.RectF
import android.graphics.Typeface
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.ImageView
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
import androidx.core.view.doOnLayout
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import com.byesmo.splashfix.ByeSmoSplashScreen
import com.byesmo.splashfix.configureByeSmoSplashWindow
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var introReady by mutableStateOf(false)
    private var targetWordmarkBounds: ComposeRect? = null
    private var pendingSplashProvider: SplashScreenViewProvider? = null
    private var animationsEnabled = true
    private var transitionAnimator: ValueAnimator? = null
    private var fallbackRunnable: Runnable? = null
    private var fadeWaitRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val systemSplash = installSplashScreen()
        super.onCreate(savedInstanceState)
        configureByeSmoSplashWindow()

        animationsEnabled = if (Build.VERSION.SDK_INT >= 26) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            Settings.Global.getFloat(
                contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
            ) > 0f
        }
        // Do not replay the intro when Android recreates this Activity.
        introReady = savedInstanceState != null
        systemSplash.setOnExitAnimationListener(::handleSplashExit)

        val captionTypeface = ResourcesCompat.getFont(this, R.font.byesmo_splash_font)
            ?: Typeface.create("sans-serif", Typeface.NORMAL)

        setContent {
            MyApplicationTheme {
                ByeSmoSplashScreen(
                    wordmarkResource = R.drawable.byesmo_splash_icon,
                    tagline = stringResource(R.string.byesmo_v3_tagline),
                    captionTypeface = captionTypeface,
                    introReady = introReady,
                    animationsEnabled = animationsEnabled,
                    onWordmarkPositioned = { bounds ->
                        targetWordmarkBounds = bounds
                        tryStartTransition()
                    },
                )
            }
        }
    }

    private fun handleSplashExit(provider: SplashScreenViewProvider) {
        if (isFinishing || isDestroyed) {
            provider.remove()
            return
        }
        pendingSplashProvider = provider
        if (introReady || !animationsEnabled) {
            finishSplash()
            return
        }

        // Start the watchdog only after the OS hands us the overlay.
        // It bounds a missing-layout/OEM failure, not the app's loading time.
        val fallback = Runnable { finishSplash() }
        fallbackRunnable = fallback
        window.decorView.postDelayed(fallback, 1000L)
        // On a fast startup, let the system AVD finish its 200ms fade before resizing.
        val remainingFade = if (Build.VERSION.SDK_INT >= 31 && provider.iconAnimationStartMillis > 0L) {
            (provider.iconAnimationStartMillis + provider.iconAnimationDurationMillis -
                System.currentTimeMillis()).coerceIn(0L, 200L)
        } else {
            0L
        }
        if (remainingFade > 0L) {
            val resume = Runnable {
                fadeWaitRunnable = null
                tryStartTransition()
            }
            fadeWaitRunnable = resume
            window.decorView.postDelayed(resume, remainingFade)
        }
        provider.iconView.doOnLayout { tryStartTransition() }
        tryStartTransition()
    }

    private fun tryStartTransition() {
        val provider = pendingSplashProvider ?: return
        val target = targetWordmarkBounds ?: return
        if (transitionAnimator != null || fadeWaitRunnable != null || isFinishing || isDestroyed) return
        val icon = provider.iconView
        if (icon.width <= 0 || icon.height <= 0 || target.width <= 0f) return

        // iconView is an ImageView on standard Android. Respect its actual
        // drawable matrix and padding instead of assuming a fixed pixel size.
        val canvas = RectF(0f, 0f, icon.width.toFloat(), icon.height.toFloat())
        if (icon is ImageView) {
            val drawable = icon.drawable
            if (drawable != null && !drawable.bounds.isEmpty) {
                canvas.set(drawable.bounds)
                icon.imageMatrix.mapRect(canvas)
                canvas.offset(icon.paddingLeft.toFloat(), icon.paddingTop.toFloat())
            }
        }
        val canvasSide = minOf(canvas.width(), canvas.height())
        if (canvasSide <= 0f) {
            finishSplash()
            return
        }

        // Animate the system view itself. Unclip its ancestors so the enlarged
        // wordmark is not cut off by the original icon slot.
        icon.clipToOutline = false
        var ancestor = icon.parent
        while (ancestor is ViewGroup) {
            ancestor.clipChildren = false
            ancestor.clipToPadding = false
            ancestor.clipToOutline = false
            if (ancestor === provider.view) break
            ancestor = ancestor.parent
        }

        val iconLocation = IntArray(2)
        icon.getLocationOnScreen(iconLocation)
        val windowLocation = IntArray(2)
        window.decorView.getLocationOnScreen(windowLocation)
        val dx = windowLocation[0] + target.center.x -
            (iconLocation[0] + canvas.centerX())
        val dy = windowLocation[1] + target.center.y -
            (iconLocation[1] + canvas.centerY())
        val endScale = target.width / canvasSide

        icon.pivotX = canvas.centerX()
        icon.pivotY = canvas.centerY()
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260L
            interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val scale = 1f + (endScale - 1f) * progress
                icon.scaleX = scale
                icon.scaleY = scale
                icon.translationX = dx * progress
                icon.translationY = dy * progress
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Keep the final system frame visible until it is removed.
                    // Compose already has the exact same final logo underneath.
                    finishSplash()
                }
            })
        }
        transitionAnimator = animator
        animator.start()
    }

    private fun finishSplash() {
        val provider = pendingSplashProvider ?: return
        pendingSplashProvider = null
        fallbackRunnable?.let { window.decorView.removeCallbacks(it) }
        fallbackRunnable = null
        fadeWaitRunnable?.let { window.decorView.removeCallbacks(it) }
        fadeWaitRunnable = null
        transitionAnimator?.apply {
            removeAllListeners()
            removeAllUpdateListeners()
            cancel()
        }
        transitionAnimator = null
        provider.remove()
        if (!isFinishing && !isDestroyed) {
            configureByeSmoSplashWindow()
            introReady = true
        }
    }

    override fun onDestroy() {
        // Remove listeners before cancelling, so teardown cannot start the caption.
        transitionAnimator?.apply {
            removeAllListeners()
            removeAllUpdateListeners()
            cancel()
        }
        transitionAnimator = null
        fallbackRunnable?.let { window.decorView.removeCallbacks(it) }
        fallbackRunnable = null
        fadeWaitRunnable?.let { window.decorView.removeCallbacks(it) }
        fadeWaitRunnable = null
        pendingSplashProvider?.remove()
        pendingSplashProvider = null
        super.onDestroy()
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun SplashScreenPreview() {
    MyApplicationTheme {
        SplashScreen()
    }
}
