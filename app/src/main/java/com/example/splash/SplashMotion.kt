package com.example.splash

/** Original PNG pixel coordinates. The two crops reconstruct the source exactly. */
object SplashMotion {
    const val SOURCE_WIDTH = 4096f
    const val SOURCE_HEIGHT = 1090f
    const val SPLIT_X = 1914f
    const val INK_LEFT = 22f
    const val INK_RIGHT = 4073f
    const val INK_TOP = 10f
    const val INK_BOTTOM = 1076f
    const val BYE_INK_RIGHT = 1904f
    const val INK_WIDTH = INK_RIGHT - INK_LEFT
    const val CENTER_X = (INK_LEFT + INK_RIGHT) / 2f
    const val BYE_CENTER_X = (INK_LEFT + BYE_INK_RIGHT) / 2f
    const val TRAVEL = CENTER_X - BYE_CENTER_X

    const val GROW_MS = 300L
    const val UNFOLD_MS = 500L
    const val CAPTION_MS = 650L
    const val HOLD_MS = 200L
    const val EXIT_MS = 250L
    const val CAPTION_START_MS = GROW_MS + UNFOLD_MS
    const val EXIT_START_MS = CAPTION_START_MS + CAPTION_MS + HOLD_MS
    const val TOTAL_MS = EXIT_START_MS + EXIT_MS

    fun progress(timeMs: Float, startMs: Long, durationMs: Long): Float =
        ((timeMs - startMs) / durationMs).coerceIn(0f, 1f)
}
