package com.example

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.res.ResourcesCompat
import com.byesmo.splashfix.ByeSmoSplashScreen

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    introReady: Boolean = true,
    animationsEnabled: Boolean = false,
    onButtonPositioned: ((ComposeRect) -> Unit)? = null,
) {
    val context = LocalContext.current
    val captionTypeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.byesmo_splash_font)
            ?: Typeface.create("sans-serif", Typeface.NORMAL)
    }

    ByeSmoSplashScreen(
        buttonResource = R.drawable.byesmo_button_v3,
        wordmarkResource = R.drawable.byesmo_splash_icon,
        tagline = stringResource(R.string.byesmo_v3_tagline),
        captionTypeface = captionTypeface,
        introReady = introReady,
        animationsEnabled = animationsEnabled,
        modifier = modifier,
        onButtonPositioned = onButtonPositioned,
    )
}
