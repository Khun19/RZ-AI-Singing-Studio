package com.rzstudio.singing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SynchronizedSessionTimelineTest {
    private class FakeMonotonicClock(var nowNanos: Long = 0L) {
        fun advanceMillis(millis: Long) {
            nowNanos += millis * 1_000_000L
        }
    }

    @Test
    fun elapsedAndBackingPositionAdvanceFromOneMonotonicClock() {
        val clock = FakeMonotonicClock()
        val timeline = SynchronizedSessionTimeline { clock.nowNanos }

        timeline.start(backingTrackPositionUs = 2_000_000L)
        clock.advanceMillis(1_500L)

        val snapshot = timeline.snapshot()
        assertEquals(1_500_000L, snapshot.sessionElapsedUs)
        assertEquals(3_500_000L, snapshot.backingTrackPositionUs)
        assertNull(snapshot.microphoneCaptureStartOffsetUs)
    }

    @Test
    fun pauseAndResumeExcludePausedWallTime() {
        val clock = FakeMonotonicClock()
        val timeline = SynchronizedSessionTimeline { clock.nowNanos }

        timeline.start()
        clock.advanceMillis(2_000L)
        timeline.pause()
        clock.advanceMillis(5_000L)

        assertEquals(2_000_000L, timeline.snapshot().sessionElapsedUs)
        assertEquals(2_000_000L, timeline.snapshot().backingTrackPositionUs)

        timeline.resume()
        clock.advanceMillis(750L)

        assertEquals(2_750_000L, timeline.snapshot().sessionElapsedUs)
        assertEquals(2_750_000L, timeline.snapshot().backingTrackPositionUs)
    }

    @Test
    fun seekCreatesNewBackingSegmentWithoutResettingSessionTime() {
        val clock = FakeMonotonicClock()
        val timeline = SynchronizedSessionTimeline { clock.nowNanos }

        timeline.start()
        clock.advanceMillis(3_000L)
        timeline.seekBackingTrackTo(10_000_000L)
        clock.advanceMillis(500L)

        val snapshot = timeline.snapshot()
        assertEquals(3_500_000L, snapshot.sessionElapsedUs)
        assertEquals(10_500_000L, snapshot.backingTrackPositionUs)
        assertEquals(
            listOf(
                SynchronizedSessionTimeline.PlaybackSegment(0L, 0L),
                SynchronizedSessionTimeline.PlaybackSegment(3_000_000L, 10_000_000L)
            ),
            timeline.playbackSegments()
        )
    }

    @Test
    fun microphoneStartOffsetIsRecordedOnlyOnce() {
        val clock = FakeMonotonicClock()
        val timeline = SynchronizedSessionTimeline { clock.nowNanos }

        timeline.start()
        clock.advanceMillis(125L)

        assertEquals(125_000L, timeline.markMicrophoneCaptureStarted())
        assertEquals(125_000L, timeline.snapshot().microphoneCaptureStartOffsetUs)
        assertThrows(IllegalStateException::class.java) {
            timeline.markMicrophoneCaptureStarted()
        }
    }

    @Test
    fun invalidTransitionsAndNegativePositionsAreRejected() {
        val timeline = SynchronizedSessionTimeline { 0L }

        assertThrows(IllegalStateException::class.java) { timeline.pause() }
        assertThrows(IllegalStateException::class.java) { timeline.resume() }
        assertThrows(IllegalStateException::class.java) { timeline.seekBackingTrackTo(0L) }
        assertThrows(IllegalArgumentException::class.java) { timeline.start(-1L) }

        timeline.start()
        assertThrows(IllegalStateException::class.java) { timeline.start() }
        assertThrows(IllegalArgumentException::class.java) { timeline.seekBackingTrackTo(-1L) }
    }
}
