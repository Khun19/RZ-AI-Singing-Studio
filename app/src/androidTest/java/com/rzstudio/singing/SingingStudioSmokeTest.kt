package com.rzstudio.singing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class SingingStudioSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsImportAndRecordingSections() {
        composeRule.onNodeWithText("Singing Studio").assertIsDisplayed()
        composeRule.onNodeWithText("Import song").assertIsDisplayed()
        composeRule.onNodeWithText("RECORDING STUDIO").assertIsDisplayed()
        composeRule.onNodeWithText("AI VOCAL SEPARATION").assertIsDisplayed()
    }
}
