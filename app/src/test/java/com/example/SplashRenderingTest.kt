package com.example

import android.content.Context
import android.app.Activity
import android.os.Looper
import android.view.ViewGroup
import android.view.MotionEvent
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
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SplashRenderingTest {
    @Test fun logoTapReplaysInTheSameViewAndBackgroundTapDoesNothing() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val activity = controller.get()
        val view = BrandSplashView(activity, SplashAssets.load(activity),
            activity.getString(R.string.byesmo_v3_tagline), true, initiallySettled = true)
        activity.setContentView(view, ViewGroup.LayoutParams(360, 780))
        shadowOf(Looper.getMainLooper()).idle()
        view.layout(0, 0, 360, 780)
        fun capture(): Bitmap = Bitmap.createBitmap(360, 780, Bitmap.Config.ARGB_8888)
            .also { view.draw(Canvas(it)) }
        fun tap(x: Float, y: Float) {
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(0, 0, action, x, y, 0)
                view.dispatchTouchEvent(event)
                event.recycle()
            }
        }
        val settled = capture()
        tap(10f, 10f)
        assertTrue(settled.sameAs(capture()))
        repeat(2) {
            tap(180f, 390f)
            assertEquals(0, whitePixels(capture(), 0, 780))
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
            assertTrue(settled.sameAs(capture()))
            assertTrue(view.isAttachedToWindow)
        }
        view.dispose()
        controller.pause().stop().destroy()
    }

    @Test fun disabledAnimationsImmediatelyShowTheCompleteBrand() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val activity = controller.get()
        val view = BrandSplashView(activity, SplashAssets.load(activity),
            activity.getString(R.string.byesmo_v3_tagline), false)
        activity.setContentView(view, ViewGroup.LayoutParams(360, 780))
        view.layout(0, 0, 360, 780)
        view.play()
        val bitmap = Bitmap.createBitmap(360, 780, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        assertTrue(whitePixels(bitmap, 340, 420) > 100)
        assertTrue(whitePixels(bitmap, 425, 600) > 30)
        view.dispose()
        controller.pause().stop().destroy()
    }

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

    @Test fun taglineAppearsOnlyAfterTheWholeWordAndFinalFrameStaysVisible() {
        assertTrue(whitePixels(frame(550), 340, 420) > 0)
        val assembled = frame(SplashMotion.CAPTION_START_MS)
        assertTrue(whitePixels(assembled, 340, 420) > 100)
        assertEquals(0, whitePixels(assembled, 425, 600))
        assertTrue(whitePixels(frame(1450), 425, 600) > 30)
        val settled = frame(SplashMotion.TOTAL_MS)
        assertTrue(whitePixels(settled, 340, 420) > 100)
        assertTrue(whitePixels(settled, 425, 600) > 30)
        assertEquals(Color.rgb(41, 45, 50), settled.getPixel(0, 0))
        assertTrue(settled.sameAs(frame(10000)))
    }
}
