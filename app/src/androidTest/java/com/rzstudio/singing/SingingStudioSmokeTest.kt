package com.rzstudio.singing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class SingingStudioSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsImplementedStudioFeatures() {
        composeRule.onNodeWithText("Singing Studio").assertIsDisplayed()
        composeRule.onNodeWithText("Import song").assertIsDisplayed()
        composeRule.onNodeWithText("RECORDING STUDIO").assertIsDisplayed()
        composeRule.onNodeWithText("VOCAL RECORDING").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Record vocal take").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("VOCAL TAKES").performScrollTo().assertIsDisplayed()
    }
}
