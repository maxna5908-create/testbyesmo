package com.byesmo.splashfix

import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

/**
 * Call after super.onCreate and before setContent, on the first frame of startup.
 * Starting-window theme items are supplied separately for the pre-Activity screen.
 */
@Suppress("DEPRECATION")
fun ComponentActivity.configureByeSmoSplashWindow() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )

    if (Build.VERSION.SDK_INT >= 29) {
        // In particular, removes the system scrim behind three-button navigation.
        window.isNavigationBarContrastEnforced = false
        window.isStatusBarContrastEnforced = false
    }
    if (Build.VERSION.SDK_INT >= 28) {
        window.navigationBarDividerColor = Color.TRANSPARENT
    }
    if (Build.VERSION.SDK_INT < 35) {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }
    WindowCompat.getInsetsController(window, window.decorView).apply {
        // false means white/light system icons, suitable for our navy background.
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = false
    }
}
