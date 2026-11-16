package com.rzstudio.singing

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class SingingStudioSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsImportAndRecordingSections() {
        composeRule.onNodeWithText("Singing Studio").assertExists()
        composeRule.onNodeWithText("Import song").assertExists()
        composeRule.onNodeWithText("RECORDING STUDIO").assertExists()
        composeRule.onNodeWithText("AI VOCAL SEPARATION").assertExists()
    }
}
