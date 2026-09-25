package com.example

import com.example.splash.SplashMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test fun captionNeverOverlapsUnfolding() {
        for (time in 0..800) {
            assertEquals(0f, SplashMotion.progress(time.toFloat(), SplashMotion.CAPTION_START_MS,
                SplashMotion.CAPTION_MS), 0f)
        }
        assertEquals(1f, SplashMotion.progress(1450f, SplashMotion.CAPTION_START_MS,
            SplashMotion.CAPTION_MS), 0f)
        assertTrue(SplashMotion.EXIT_START_MS > 1450)
    }

    @Test fun assembledInkIsExactlySixtyPercentAndCenteredOnEveryScreen() {
        for (width in listOf(320f, 1080f, 1440f, 2560f)) {
            val scale = width * 0.6f / SplashMotion.INK_WIDTH
            val left = width / 2f - SplashMotion.CENTER_X * scale
            val inkLeft = left + SplashMotion.INK_LEFT * scale
            val inkRight = left + SplashMotion.INK_RIGHT * scale
            assertEquals(width * 0.6f, inkRight - inkLeft, 0.001f)
            assertEquals(width / 2f, (inkLeft + inkRight) / 2f, 0.001f)
            val initialByeCenter = left + (SplashMotion.BYE_CENTER_X + SplashMotion.TRAVEL) * scale
            assertEquals(width / 2f, initialByeCenter, 0.001f)
        }
    }

    @Test fun smoStartsFullyBehindTheMovingClipAndFinishesAtOriginalPosition() {
        assertEquals(SplashMotion.BYE_INK_RIGHT + SplashMotion.TRAVEL,
            SplashMotion.INK_RIGHT - SplashMotion.TRAVEL, 0f)
        assertEquals(0f, SplashMotion.progress(-100f, 0, SplashMotion.GROW_MS), 0f)
        assertEquals(1f, SplashMotion.progress(10000f, SplashMotion.EXIT_START_MS,
            SplashMotion.EXIT_MS), 0f)
    }
}
