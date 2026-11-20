package com.rzstudio.singing

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Converts normalized float PCM to signed 16-bit PCM, clipping and sanitizing non-finite values. */
internal object Pcm16Converter {
    fun convert(samples: FloatArray, destination: ByteBuffer): Int {
        require(destination.remaining() >= samples.size * 2)
        destination.order(ByteOrder.nativeOrder())
        for (sample in samples) {
            val safe = if (sample.isFinite()) sample.coerceIn(-1f, 1f) else 0f
            val value = if (safe <= -1f) Short.MIN_VALUE.toInt() else (safe * Short.MAX_VALUE).toInt()
            destination.putShort(value.toShort())
        }
        return samples.size * 2
    }

    fun presentationTimeUs(frameCount: Long, sampleRate: Int): Long {
        require(frameCount >= 0 && sampleRate > 0)
        return frameCount * 1_000_000L / sampleRate
    }
}
