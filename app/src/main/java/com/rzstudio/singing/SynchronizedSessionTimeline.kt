package com.rzstudio.singing

/**
 * Monotonic session timeline shared by backing playback and microphone capture.
 *
 * This class records timing intent and seek discontinuities. It does not itself
 * control an Android audio device and does not claim sample-accurate timestamps
 * from MediaPlayer or MediaRecorder.
 */
internal class SynchronizedSessionTimeline(
    private val nanoTime: () -> Long = System::nanoTime
) {
    enum class State { IDLE, RUNNING, PAUSED }

    data class PlaybackSegment(
        val sessionOffsetUs: Long,
        val backingTrackOffsetUs: Long
    )

    data class Snapshot(
        val state: State,
        val sessionElapsedUs: Long,
        val backingTrackPositionUs: Long,
        val microphoneCaptureStartOffsetUs: Long?
    )

    private var state: State = State.IDLE
    private var accumulatedElapsedUs: Long = 0L
    private var runningAnchorNanos: Long = 0L
    private var microphoneCaptureStartOffsetUs: Long? = null
    private val playbackSegments = mutableListOf<PlaybackSegment>()

    @Synchronized
    fun start(backingTrackPositionUs: Long = 0L) {
        require(backingTrackPositionUs >= 0L) { "Backing-track position cannot be negative" }
        check(state == State.IDLE) { "A session can only be started once" }
        runningAnchorNanos = nanoTime()
        playbackSegments += PlaybackSegment(
            sessionOffsetUs = 0L,
            backingTrackOffsetUs = backingTrackPositionUs
        )
        state = State.RUNNING
    }

    @Synchronized
    fun pause(): Snapshot {
        check(state == State.RUNNING) { "Only a running session can be paused" }
        accumulatedElapsedUs = currentSessionElapsedUs()
        state = State.PAUSED
        return snapshot()
    }

    @Synchronized
    fun resume() {
        check(state == State.PAUSED) { "Only a paused session can be resumed" }
        runningAnchorNanos = nanoTime()
        state = State.RUNNING
    }

    /**
     * Starts a new backing-track segment without resetting the monotonic
     * recording-session timeline. The seek is represented as a discontinuity.
     */
    @Synchronized
    fun seekBackingTrackTo(positionUs: Long): Snapshot {
        require(positionUs >= 0L) { "Backing-track position cannot be negative" }
        check(state != State.IDLE) { "A session must be started before seeking" }
        val sessionOffsetUs = currentSessionElapsedUs()
        playbackSegments += PlaybackSegment(
            sessionOffsetUs = sessionOffsetUs,
            backingTrackOffsetUs = positionUs
        )
        return snapshot()
    }

    /**
     * Records the requested microphone-start boundary relative to the shared
     * session clock. This is API-call timing, not a hardware sample timestamp.
     */
    @Synchronized
    fun markMicrophoneCaptureStarted(): Long {
        check(state != State.IDLE) { "A session must be started before capture" }
        check(microphoneCaptureStartOffsetUs == null) {
            "Microphone capture start has already been recorded"
        }
        return currentSessionElapsedUs().also { microphoneCaptureStartOffsetUs = it }
    }

    @Synchronized
    fun snapshot(): Snapshot {
        check(state != State.IDLE) { "A session has not been started" }
        val elapsedUs = currentSessionElapsedUs()
        val activeSegment = playbackSegments.last()
        val segmentElapsedUs = (elapsedUs - activeSegment.sessionOffsetUs).coerceAtLeast(0L)
        return Snapshot(
            state = state,
            sessionElapsedUs = elapsedUs,
            backingTrackPositionUs = activeSegment.backingTrackOffsetUs + segmentElapsedUs,
            microphoneCaptureStartOffsetUs = microphoneCaptureStartOffsetUs
        )
    }

    @Synchronized
    fun playbackSegments(): List<PlaybackSegment> = playbackSegments.toList()

    private fun currentSessionElapsedUs(): Long {
        if (state != State.RUNNING) return accumulatedElapsedUs
        val deltaNanos = (nanoTime() - runningAnchorNanos).coerceAtLeast(0L)
        return accumulatedElapsedUs + deltaNanos / 1_000L
    }
}
