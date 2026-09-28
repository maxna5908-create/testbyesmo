package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-mdpi", sdk = [36])
class OnboardingTest {
    @get:Rule val compose = createComposeRule()
    @Test fun slides_and_completion() {
        compose.setContent { MyApplicationTheme { LaunchDestination(false, {}) } }
        compose.waitForIdle()
        compose.onNodeWithText("Твой путь без сигарет").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/reports/onboarding/slide-1.png")
        compose.onNodeWithText("Далее").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Замечай привычки").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/reports/onboarding/slide-2.png")
        compose.onNodeWithText("Далее").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Двигайся шаг за шагом").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/reports/onboarding/slide-3.png")
        compose.onNodeWithText("Начать").performClick()
        compose.onNodeWithText("Добро пожаловать").assertIsDisplayed()
    }
}
