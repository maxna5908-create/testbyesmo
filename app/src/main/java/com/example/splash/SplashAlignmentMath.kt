// Change this package to the existing Android project's package + .splash.
// Pure geometry only. Read ALIGNMENT_RU.md for measurement and handoff lifecycle.
package com.example.splash

data class SplashButtonTargetPx(
    val centerX: Float,
    val centerY: Float,
    val imageSide: Float,
    val bodyDiameter: Float,
) {
    val imageLeft: Float get() = centerX - imageSide / 2f
    val imageTop: Float get() = centerY - imageSide / 2f
}

object SplashAlignmentMath {
    // Geometry of the supplied padded system PNG and the unpadded UI PNG.
    const val SYSTEM_CANVAS = 288f
    const val SYSTEM_INNER_IMAGE = 192f
    const val UI_CANVAS = 1024f
    const val UI_BODY_DIAMETER = 856f

    // renderedSystemCanvasSidePx is the mapped drawable side, not necessarily
    // iconView.width. Measure AFTER entry/layout and BEFORE exit transforms.
    fun target(
        renderedSystemCanvasSidePx: Float,
        systemButtonCenterXScreenPx: Float,
        systemButtonCenterYScreenPx: Float,
    ): SplashButtonTargetPx {
        require(renderedSystemCanvasSidePx.isFinite() && renderedSystemCanvasSidePx > 0f)
        require(systemButtonCenterXScreenPx.isFinite() && systemButtonCenterYScreenPx.isFinite())
        val side = renderedSystemCanvasSidePx * SYSTEM_INNER_IMAGE / SYSTEM_CANVAS
        return SplashButtonTargetPx(
            centerX = systemButtonCenterXScreenPx,
            centerY = systemButtonCenterYScreenPx,
            imageSide = side,
            bodyDiameter = side * UI_BODY_DIAMETER / UI_CANVAS,
        )
    }

    // Move all group members by this same amount. Measure both centers in
    // one coordinate space. This is static layout, never an exit animation.
    fun compositionShiftY(
        systemButtonCenterY: Float,
        currentSplashButtonCenterY: Float,
    ): Float {
        require(systemButtonCenterY.isFinite() && currentSplashButtonCenterY.isFinite())
        return systemButtonCenterY - currentSplashButtonCenterY
    }

    // Alternative direct placement. buttonCenterYInsideGroup is measured from
    // the group's top edge, with the current sizes and internal gaps preserved.
    fun groupTop(
        systemButtonCenterYInParent: Float,
        buttonCenterYInsideGroup: Float,
    ): Float {
        require(systemButtonCenterYInParent.isFinite() && buttonCenterYInsideGroup.isFinite())
        return systemButtonCenterYInParent - buttonCenterYInsideGroup
    }

    fun pixelsToDp(pixels: Float, density: Float): Float {
        require(pixels.isFinite() && density.isFinite() && density > 0f)
        return pixels / density
    }
}
