package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.example.splash.BrandSplashView
import com.example.splash.SplashAssets

/** The only splash implementation. Preview uses the same view at its settled frame. */
@Composable
fun SplashScreen(
    assets: SplashAssets,
    start: Boolean,
    animationsEnabled: Boolean = true,
    preview: Boolean = false,
    onFinished: () -> Unit = {},
) {
    val onComplete by rememberUpdatedState(onFinished)
    val tagline = stringResource(R.string.byesmo_v3_tagline)
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            BrandSplashView(context, assets, tagline, animationsEnabled, preview)
                .apply { this.onFinished = { onComplete() } }
        },
        onRelease = { it.dispose() },
        update = { if (start) it.play() },
    )
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
fun SplashScreenPreview() {
    val context = LocalContext.current
    val assets = remember(context) { SplashAssets.load(context) }
    SplashScreen(assets = assets, start = false, preview = true)
}
