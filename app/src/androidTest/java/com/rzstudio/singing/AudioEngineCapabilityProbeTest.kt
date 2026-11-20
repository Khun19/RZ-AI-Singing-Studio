package com.rzstudio.singing

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Read-only capability discovery for physical audio-engine benchmark preparation.
 *
 * This test does not start microphone capture, play audio, request permissions,
 * change audio routing, or claim to measure real-world latency.
 */
@RunWith(AndroidJUnit4::class)
class AudioEngineCapabilityProbeTest {

    @Test
    fun reportsAudioPlatformCapabilitiesForBenchmark() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val report = buildString {
            appendLine("AUDIO_ENGINE_CAPABILITY_PROBE_V1")
            appendLine("manufacturer=${Build.MANUFACTURER}")
            appendLine("brand=${Build.BRAND}")
            appendLine("model=${Build.MODEL}")
            appendLine("device=${Build.DEVICE}")
            appendLine("product=${Build.PRODUCT}")
            appendLine("android_release=${Build.VERSION.RELEASE}")
            appendLine("api_level=${Build.VERSION.SDK_INT}")
            appendLine("supported_abis=${Build.SUPPORTED_ABIS.joinToString(",")}")
            appendLine("output_sample_rate_property=${audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) ?: "unavailable"}")
            appendLine("output_frames_per_buffer_property=${audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) ?: "unavailable"}")
            appendLine("audio_mode=${audioManager.mode}")
            appendLine("music_volume=${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}")
            appendLine("music_volume_max=${audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}")

            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
            appendLine("audio_device_count=${devices.size}")
            devices.forEachIndexed { index, device ->
                appendLine(
                    "audio_device_$index=" + listOf(
                        "id=${device.id}",
                        "type=${device.type}",
                        "product=${device.productName}",
                        "source=${device.isSource}",
                        "sink=${device.isSink}",
                        "sample_rates=${device.sampleRates.joinToString(",")}",
                        "channel_counts=${device.channelCounts.joinToString(",")}"
                    ).joinToString(";")
                )
            }

            val sampleRates = intArrayOf(44_100, 48_000)
            sampleRates.forEach { rate ->
                val recordMono = AudioRecord.getMinBufferSize(
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val recordStereo = AudioRecord.getMinBufferSize(
                    rate,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val trackMono = AudioTrack.getMinBufferSize(
                    rate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val trackStereo = AudioTrack.getMinBufferSize(
                    rate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                appendLine(
                    "pcm16_rate_$rate=" +
                        "record_mono_min_bytes:$recordMono;" +
                        "record_stereo_min_bytes:$recordStereo;" +
                        "track_mono_min_bytes:$trackMono;" +
                        "track_stereo_min_bytes:$trackStereo"
                )
            }
        }

        assertNotNull("Capability report must be generated", report)
        assertFalse("Capability report must not be empty", report.isBlank())
        report.lineSequence().forEach { line ->
            if (line.isNotBlank()) Log.i(TAG, line)
        }
    }

    private companion object {
        const val TAG = "AudioEngineProbe"
    }
}
