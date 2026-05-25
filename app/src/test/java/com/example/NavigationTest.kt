package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.lang.Exception

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class NavigationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun runAndLogExceptions() {
        ShadowLog.stream = System.out
        try {
            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.mainClock.advanceTimeBy(5000)
            composeTestRule.waitForIdle()
        } catch(e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
