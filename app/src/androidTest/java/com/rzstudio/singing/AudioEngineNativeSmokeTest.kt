package com.rzstudio.singing

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioEngineNativeSmokeTest {
    @Test
    fun nativeLibraryLoadsAndReportsClosedEngine() {
        AudioEngine().use { engine ->
            val diagnostics = engine.diagnostics()
            assertTrue(diagnostics.contains("\"running\":false"))
            assertTrue(diagnostics.contains("\"actualBackend\":\"CLOSED\""))
        }
    }

    @Test
    fun playbackPcmQueueAcceptsDirectFloatBuffer() {
        AudioEngine().use { engine ->
            val pcm = ByteBuffer.allocateDirect(256 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            repeat(64) { pcm.put(0.125f) }
            pcm.flip()
            assertEquals(64, engine.queuePlaybackPcm(pcm))
        }
    }

    @Test
    fun playbackQueueHonorsBufferPositionAndLimit() {
        AudioEngine().use { engine ->
            val pcm = ByteBuffer.allocateDirect(256 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            repeat(32) { pcm.put(0.25f) }
            pcm.position(7)
            pcm.limit(23)

            assertEquals(16, engine.queuePlaybackPcm(pcm))
            assertEquals(23, pcm.position())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun playbackPcmQueueRejectsHeapBuffer() {
        AudioEngine().use { engine ->
            engine.queuePlaybackPcm(FloatBufferFactory.heap(16))
        }
    }

    private object FloatBufferFactory {
        fun heap(size: Int) = java.nio.FloatBuffer.allocate(size)
    }
}
