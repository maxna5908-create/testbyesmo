package com.example

import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.byesmo.splashfix.configureByeSmoSplashWindow
import com.example.splash.SplashAssets
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var contentLaidOut = false
    private var systemSplashGone by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val systemSplash = installSplashScreen()
        super.onCreate(savedInstanceState)
        configureByeSmoSplashWindow()
        // Wait for assets AND the destination's first layout. No fixed loading delay.
        systemSplash.setKeepOnScreenCondition { !contentLaidOut }
        systemSplash.setOnExitAnimationListener { provider ->
            provider.remove()
            systemSplashGone = true
        }
        val animationsEnabled = if (Build.VERSION.SDK_INT >= 26) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }
        lifecycleScope.launch {
            val (assets, onboardingComplete) = withContext(Dispatchers.IO) {
                SplashAssets.load(this@MainActivity) to
                    getSharedPreferences(PREFERENCES, MODE_PRIVATE).getBoolean(ONBOARDING_COMPLETE, false)
            }
            // Recreation restores the destination instead of replaying the intro.
            var splashComplete by mutableStateOf(savedInstanceState != null || !animationsEnabled)
            setContent {
                MyApplicationTheme {
                    Box(Modifier.fillMaxSize().background(Color(0xFF292D32))
                        .onGloballyPositioned { contentLaidOut = true }) {
                        // Prepare the destination under the opaque overlay before animating.
                        Box(if (splashComplete) Modifier else Modifier.clearAndSetSemantics { }) {
                            LaunchDestination(
                                initiallyComplete = onboardingComplete,
                                onOnboardingComplete = {
                                    getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                                        .putBoolean(ONBOARDING_COMPLETE, true).apply()
                                },
                            )
                        }
                        if (!splashComplete) {
                            BackHandler { /* Wait for the short launch transition. */ }
                            SplashScreen(
                                assets = assets,
                                start = systemSplashGone,
                                animationsEnabled = animationsEnabled,
                                onFinished = { splashComplete = true },
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val PREFERENCES = "byesmo_launch"
        private const val ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
