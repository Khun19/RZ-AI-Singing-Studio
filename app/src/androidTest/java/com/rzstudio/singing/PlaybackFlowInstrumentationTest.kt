package com.rzstudio.singing

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class PlaybackFlowInstrumentationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val composeRule = createAndroidComposeRule<MainActivity>()
    private lateinit var seededTake: File

    private val seedTakeRule = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement =
            object : Statement() {
                override fun evaluate() {
                    TakeStore.list(context).forEach { TakeStore.delete(it) }
                    seededTake = TakeStore.newTakeFile(context)
                    writeTestM4aFixture(seededTake)
                    base.evaluate()
                }
            }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(seedTakeRule).around(composeRule)

    @Test
    fun playbackCanSeekPauseResumeAndComplete() {
        composeRule.onNodeWithText(seededTake.name).performScrollTo()
        composeRule.onNodeWithText(seededTake.name).assertIsDisplayed()
        composeRule.onNodeWithText("Play").performClick()

        val slider = composeRule.onNodeWithTag("take_seek_slider")
        slider.performScrollTo()
        slider.assertIsDisplayed()

        // Confirm the real MediaPlayer advances before any user seek action.
        composeRule.waitUntil(3_000) {
            playbackPositionMs(slider) in 500f..1_800f
        }

        slider.performTouchInput {
            swipe(
                start = Offset(2f, centerY),
                end = Offset(width * 0.7f, centerY),
                durationMillis = 300
            )
        }

        // The displayed position is sourced from MediaPlayer.currentPosition after seekTo().
        composeRule.waitUntil(2_000) {
            playbackPositionMs(slider) > 3_000f
        }

        composeRule.onNodeWithText("Pause").performClick()
        composeRule.onNodeWithText("Play").assertIsDisplayed()
        val pausedPosition = playbackPositionMs(slider)

        composeRule.waitUntil(1_500) {
            val current = playbackPositionMs(slider)
            abs(current - pausedPosition) < 250f
        }

        composeRule.onNodeWithText("Play").performClick()
        composeRule.waitUntil(2_000) {
            playbackPositionMs(slider) > pausedPosition + 400f
        }

        composeRule.waitUntil(6_000) {
            composeRule.onAllNodesWithText("Take playback completed")
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithText("Take playback completed").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun takeRemainsAvailableAfterActivityRecreation() {
        composeRule.onNodeWithText(seededTake.name).performScrollTo()
        composeRule.onNodeWithText(seededTake.name).assertIsDisplayed()
        assertTrue("Take must remain in private storage before recreation", seededTake.exists())
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(seededTake.name)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(seededTake.name).performScrollTo()
        composeRule.onNodeWithText(seededTake.name).assertIsDisplayed()
        assertTrue("Take must remain in private storage after recreation",
            TakeStore.list(context).any { it.name == seededTake.name })
    }

    @Test
    fun deletingTakeRemovesItFromUiAndPrivateStorage() {
        composeRule.onNodeWithText(seededTake.name).assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()
        assertTrue(composeRule.onAllNodesWithText(seededTake.name).fetchSemanticsNodes().isEmpty())
        assert(TakeStore.list(context).none { it.name == seededTake.name })
    }

    private fun playbackPositionMs(
        interaction: androidx.compose.ui.test.SemanticsNodeInteraction
    ): Float = interaction.fetchSemanticsNode().config[
        androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo
    ].current

    private fun writeTestM4aFixture(file: File) {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        testContext.assets.open("playback_fixture.m4a").use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
    }

}
