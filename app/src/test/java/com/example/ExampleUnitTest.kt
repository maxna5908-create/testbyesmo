package com.example

import com.example.splash.SplashAlignmentMath
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testCenteredButtonGroupAnchor() {
    val windowHeight = 800f
    val scale = 1.0f
    val systemButtonCenterY = windowHeight / 2f // 400f
    val buttonSlotSize = 232f * scale
    val buttonCenterYInsideGroup = buttonSlotSize / 2f // 116f

    val groupTop = SplashAlignmentMath.groupTop(
      systemButtonCenterYInParent = systemButtonCenterY,
      buttonCenterYInsideGroup = buttonCenterYInsideGroup,
    )

    // Verify groupTop places the button center exactly at systemButtonCenterY
    val actualButtonCenterY = groupTop + buttonCenterYInsideGroup
    assertEquals(systemButtonCenterY, actualButtonCenterY, 0.0001f)

    // Test compositionShiftY formula:
    val oldButtonCenterY = 345f // previous higher position
    val shiftY = SplashAlignmentMath.compositionShiftY(systemButtonCenterY, oldButtonCenterY)
    assertEquals(55f, shiftY, 0.0001f)
    assertEquals(systemButtonCenterY, oldButtonCenterY + shiftY, 0.0001f)
  }

  @Test
  fun testSplashButtonTarget() {
    val renderedCanvasSide = 576f // e.g. 288dp * 2 density
    val systemCenterX = 540f
    val systemCenterY = 1200f

    val target = SplashAlignmentMath.target(
      renderedSystemCanvasSidePx = renderedCanvasSide,
      systemButtonCenterXScreenPx = systemCenterX,
      systemButtonCenterYScreenPx = systemCenterY,
    )

    assertEquals(systemCenterX, target.centerX, 0.0001f)
    assertEquals(systemCenterY, target.centerY, 0.0001f)
    // side = 576 * 192 / 288 = 384
    assertEquals(384f, target.imageSide, 0.0001f)
    // bodyDiameter = 384 * 856 / 1024 = 321
    assertEquals(321f, target.bodyDiameter, 0.0001f)
    assertEquals(target.centerX - target.imageSide / 2f, target.imageLeft, 0.0001f)
    assertEquals(target.centerY - target.imageSide / 2f, target.imageTop, 0.0001f)
  }
}
