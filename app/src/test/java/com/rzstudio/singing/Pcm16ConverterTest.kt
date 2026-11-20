package com.rzstudio.singing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Pcm16ConverterTest {
    @Test fun conversionClipsAndSanitizes() {
        val values = floatArrayOf(-2f, -1f, -0.5f, 0f, 0.5f, 1f, 2f, Float.NaN, Float.POSITIVE_INFINITY)
        val buffer = ByteBuffer.allocate(values.size * 2).order(ByteOrder.nativeOrder())
        Pcm16Converter.convert(values, buffer)
        buffer.flip()
        assertEquals(Short.MIN_VALUE, buffer.getShort().toInt())
        assertEquals(Short.MIN_VALUE, buffer.getShort().toInt())
        assertEquals(-16383, buffer.getShort().toInt())
        assertEquals(0, buffer.getShort().toInt())
        assertEquals(16383, buffer.getShort().toInt())
        assertEquals(Short.MAX_VALUE.toInt(), buffer.getShort().toInt())
        assertEquals(Short.MAX_VALUE.toInt(), buffer.getShort().toInt())
        assertEquals(0, buffer.getShort().toInt())
        assertEquals(0, buffer.getShort().toInt())
    }

    @Test fun overloadConvertsOnlyRequestedSamples() {
        val values = floatArrayOf(-1f, 0.25f, 1f)
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder())
        assertEquals(4, Pcm16Converter.convert(values, 2, buffer))
        buffer.flip()
        assertEquals(Short.MIN_VALUE, buffer.getShort().toInt())
        assertEquals(8191, buffer.getShort().toInt())
    }

    @Test fun sampleCountAndCapacityAreValidated() {
        val values = floatArrayOf(0f, 1f)
        assertThrows(IllegalArgumentException::class.java) {
            Pcm16Converter.convert(values, 3, ByteBuffer.allocate(6))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Pcm16Converter.convert(values, 2, ByteBuffer.allocate(2))
        }
    }

    @Test fun ptsUsesSampleFrameClock() {
        assertEquals(1_000_000L, Pcm16Converter.presentationTimeUs(48_000, 48_000))
        assertEquals(500_000L, Pcm16Converter.presentationTimeUs(22_050, 44_100))
        assertEquals(0L, Pcm16Converter.presentationTimeUs(0, 48_000))
    }
}
