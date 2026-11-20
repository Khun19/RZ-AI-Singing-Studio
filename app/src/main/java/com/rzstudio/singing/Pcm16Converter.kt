package com.rzstudio.singing

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Converts normalized float PCM to signed 16-bit PCM, clipping and sanitizing non-finite values. */
internal object Pcm16Converter {
    fun convert(samples: FloatArray, destination: ByteBuffer): Int =
        convert(samples, samples.size, destination)

    fun convert(samples: FloatArray, sampleCount: Int, destination: ByteBuffer): Int {
        require(sampleCount in 0..samples.size)
        require(destination.remaining() >= sampleCount * 2)
        destination.order(ByteOrder.nativeOrder())
        for (i in 0 until sampleCount) {
            val sample = samples[i]
            val safe = if (sample.isFinite()) sample.coerceIn(-1f, 1f) else 0f
            val value = if (safe <= -1f) Short.MIN_VALUE.toInt() else (safe * Short.MAX_VALUE).toInt()
            destination.putShort(value.toShort())
        }
        return sampleCount * 2
    }

    fun presentationTimeUs(frameCount: Long, sampleRate: Int): Long {
        require(frameCount >= 0 && sampleRate > 0)
        return frameCount * 1_000_000L / sampleRate
    }
}
