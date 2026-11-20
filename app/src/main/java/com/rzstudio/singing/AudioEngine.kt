package com.rzstudio.singing

import java.io.Closeable
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Oboe-backed full-duplex audio engine.
 *
 * PCM buffers passed to this class must be direct FloatBuffers. Playback is interleaved
 * stereo at the engine's actual sample rate; captured PCM is mono. Call PCM transfer
 * methods from worker/IO threads, never from the Android main thread.
 */
class AudioEngine : Closeable {
    private var handle: Long = nativeCreate()

    @Synchronized
    fun start(): Int = nativeStart(requireHandle())

    @Synchronized
    fun stop(): Int = if (handle == 0L) 0 else nativeStop(handle)

    fun setMonitoring(enabled: Boolean) {
        val current = requireHandle()
        nativeSetMonitoring(current, enabled)
    }

    fun readCapturedPcm(destination: FloatBuffer, maxSamples: Int = destination.remaining()): Int {
        requireDirectNativeBuffer(destination)
        require(maxSamples >= 0 && maxSamples <= destination.remaining())
        return nativeReadCapturedPcm(requireHandle(), destination, maxSamples)
    }

    fun queuePlaybackPcm(source: FloatBuffer, samples: Int = source.remaining()): Int {
        requireDirectNativeBuffer(source)
        require(samples >= 0 && samples <= source.remaining())
        return nativeQueuePlaybackPcm(requireHandle(), source, samples)
    }

    fun diagnostics(): String = nativeDiagnostics(requireHandle())

    @Synchronized
    override fun close() {
        if (handle != 0L) {
            nativeStop(handle)
            nativeDestroy(handle)
            handle = 0L
        }
    }

    private fun requireHandle(): Long {
        check(handle != 0L) { "AudioEngine is closed" }
        return handle
    }

    private fun requireDirectNativeBuffer(buffer: FloatBuffer) {
        require(buffer.isDirect) { "Audio PCM buffers must be direct" }
        require(buffer.order() == ByteOrder.nativeOrder()) {
            "Audio PCM buffers must use native byte order"
        }
    }

    private external fun nativeCreate(): Long
    private external fun nativeStart(handle: Long): Int
    private external fun nativeStop(handle: Long): Int
    private external fun nativeSetMonitoring(handle: Long, enabled: Boolean)
    private external fun nativeReadCapturedPcm(handle: Long, destination: FloatBuffer, maxSamples: Int): Int
    private external fun nativeQueuePlaybackPcm(handle: Long, source: FloatBuffer, samples: Int): Int
    private external fun nativeDiagnostics(handle: Long): String
    private external fun nativeDestroy(handle: Long)

    companion object {
        init {
            System.loadLibrary("rz_audio_engine")
        }
    }
}
