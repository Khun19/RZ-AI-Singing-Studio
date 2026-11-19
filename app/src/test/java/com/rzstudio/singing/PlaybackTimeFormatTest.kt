package com.rzstudio.singing

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTimeFormatTest {
    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatPlaybackTime(0))
        assertEquals("0:09", formatPlaybackTime(9_999))
        assertEquals("1:01", formatPlaybackTime(61_999))
        assertEquals("10:00", formatPlaybackTime(600_000))
    }

    @Test
    fun clampsNegativePositionsToZero() {
        assertEquals("0:00", formatPlaybackTime(-1))
    }
}
