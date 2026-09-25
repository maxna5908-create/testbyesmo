package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.splash.BrandSplashView
import com.example.splash.SplashAssets
import com.example.splash.SplashMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SplashRenderingTest {
    private fun frame(timeMs: Long): Bitmap {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = BrandSplashView(context, SplashAssets.load(context),
            context.getString(R.string.byesmo_v3_tagline), false, true, timeMs)
        view.layout(0, 0, 360, 780)
        return Bitmap.createBitmap(360, 780, Bitmap.Config.ARGB_8888).also { bitmap ->
            view.draw(Canvas(bitmap))
            val directory = File("build/reports/splash").apply { mkdirs() }
            File(directory, "frame-$timeMs.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun whitePixels(bitmap: Bitmap, top: Int, bottom: Int): Int {
        var count = 0
        for (y in top until bottom) for (x in 0 until bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            if (Color.alpha(pixel) > 200 && Color.red(pixel) > 200 &&
                Color.green(pixel) > 200 && Color.blue(pixel) > 200) count++
        }
        return count
    }

    @Test fun firstFrameIsOnlyGraphiteAndSmoDoesNotLeakDuringGrowth() {
        val initial = frame(0)
        for (y in 0 until 780) for (x in 0 until 360) {
            assertEquals(Color.rgb(41, 45, 50), initial.getPixel(x, y))
        }
        assertEquals(0, whitePixels(frame(150), 0, 780))
        assertEquals(0, whitePixels(frame(SplashMotion.GROW_MS), 0, 780))
    }

    @Test fun taglineAppearsOnlyAfterTheWholeWordAndOverlayFinallyClears() {
        assertTrue(whitePixels(frame(550), 340, 420) > 0)
        val assembled = frame(SplashMotion.CAPTION_START_MS)
        assertTrue(whitePixels(assembled, 340, 420) > 100)
        assertEquals(0, whitePixels(assembled, 425, 600))
        assertTrue(whitePixels(frame(1450), 425, 600) > 30)
        val exit = frame(SplashMotion.TOTAL_MS)
        assertEquals(0, Color.alpha(exit.getPixel(180, 390)))
        assertEquals(0, Color.alpha(exit.getPixel(0, 0)))
    }
}
