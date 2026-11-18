package com.rzstudio.singing

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

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
                    writeTestWav(seededTake, durationSeconds = 6)
                    base.evaluate()
                }
            }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(seedTakeRule).around(composeRule)

    @Test
    fun playbackCanSeekPauseResumeAndComplete() {
        composeRule.onNodeWithText(seededTake.name).assertIsDisplayed()
        composeRule.onNodeWithText("Play").performClick()

        val slider = composeRule.onNodeWithTag("take_seek_slider")
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
        composeRule.onNodeWithText(seededTake.name).assertIsDisplayed()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText(seededTake.name).assertIsDisplayed()
        assert(TakeStore.list(context).any { it.name == seededTake.name })
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

    private fun writeTestWav(file: File, durationSeconds: Int) {
        val sampleRate = 44_100
        val channels = 1
        val bitsPerSample = 16
        val sampleCount = sampleRate * durationSeconds
        val dataSize = sampleCount * channels * bitsPerSample / 8
        BufferedOutputStream(FileOutputStream(file)).use { output ->
            output.write("RIFF".toByteArray(Charsets.US_ASCII))
            writeIntLe(output, 36 + dataSize)
            output.write("WAVE".toByteArray(Charsets.US_ASCII))
            output.write("fmt ".toByteArray(Charsets.US_ASCII))
            writeIntLe(output, 16)
            writeShortLe(output, 1)
            writeShortLe(output, channels)
            writeIntLe(output, sampleRate)
            writeIntLe(output, sampleRate * channels * bitsPerSample / 8)
            writeShortLe(output, channels * bitsPerSample / 8)
            writeShortLe(output, bitsPerSample)
            output.write("data".toByteArray(Charsets.US_ASCII))
            writeIntLe(output, dataSize)
            repeat(sampleCount) { index ->
                val sample = (sin(2.0 * PI * 440.0 * index / sampleRate) * 12_000).toInt()
                writeShortLe(output, sample)
            }
        }
    }

    private fun writeIntLe(output: BufferedOutputStream, value: Int) {
        output.write(value and 0xff)
        output.write((value shr 8) and 0xff)
        output.write((value shr 16) and 0xff)
        output.write((value shr 24) and 0xff)
    }

    private fun writeShortLe(output: BufferedOutputStream, value: Int) {
        output.write(value and 0xff)
        output.write((value shr 8) and 0xff)
    }
}
