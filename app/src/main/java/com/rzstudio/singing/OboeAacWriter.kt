package com.rzstudio.singing

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Worker-thread PCM capture encoder. This is an opt-in engine integration component;
 * the existing MediaRecorder UI path is intentionally untouched.
 */
internal class OboeAacWriter(
    private val engine: AudioEngine,
    private val output: File,
    private val sampleRate: Int = 48_000,
    private val bitrate: Int = 192_000
) : AutoCloseable {
    private val running = AtomicBoolean(false)
    private val stopping = AtomicBoolean(false)
    private var worker: Thread? = null
    @Volatile private var failure: Throwable? = null

    fun start() {
        check(running.compareAndSet(false, true)) { "Writer already started" }
        check(!output.exists() || output.delete()) { "Output file already exists" }
        output.parentFile?.mkdirs()
        stopping.set(false)
        worker = Thread(::runEncoder, "rz-aac-writer").apply { start() }
    }

    fun stopAndJoin(timeoutMs: Long = 10_000): File {
        stopping.set(true)
        worker?.join(timeoutMs)
        check(worker?.isAlive != true) { "Encoder worker did not stop in time" }
        failure?.let { throw IllegalStateException("Recording failed", it) }
        check(output.isFile && output.length() > 0) { "No valid recording produced" }
        return output
    }

    private fun runEncoder() {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var track = -1
        var totalFrames = 0L
        var eosSent = false
        val pcm = FloatArray(2048)
        val pcmBytes = ByteBuffer.allocateDirect(pcm.size * 2).order(ByteOrder.nativeOrder())
        try {
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1)
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfoCompat.AAC_LC)
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, pcm.size * 2)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            engine.start()

            val info = MediaCodec.BufferInfo()
            while (!eosSent || !stopping.get()) {
                var drained = false
                val inputIndex = codec.dequeueInputBuffer(5_000)
                if (inputIndex >= 0) {
                    val input = codec.getInputBuffer(inputIndex) ?: error("Encoder input buffer unavailable")
                    input.clear()
                    pcmBytes.clear()
                    var samples = engine.readCapturedPcmDirect(pcmBytes, pcm.size)
                    if (samples <= 0 && stopping.get()) {
                        codec.queueInputBuffer(inputIndex, 0, 0, Pcm16Converter.presentationTimeUs(totalFrames, sampleRate), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        eosSent = true
                    } else if (samples <= 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, Pcm16Converter.presentationTimeUs(totalFrames, sampleRate), 0)
                    } else {
                        pcmBytes.flip()
                        input.put(pcmBytes)
                        codec.queueInputBuffer(inputIndex, 0, samples * 2, Pcm16Converter.presentationTimeUs(totalFrames, sampleRate), 0)
                        totalFrames += samples
                    }
                }

                var outputIndex = codec.dequeueOutputBuffer(info, 5_000)
                while (outputIndex >= 0) {
                    val encoded = codec.getOutputBuffer(outputIndex)
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0) {
                        if (!muxerStarted) {
                            track = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        encoded?.position(info.offset)
                        encoded?.limit(info.offset + info.size)
                        muxer.writeSampleData(track, encoded!!, info)
                        drained = true
                    }
                    val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (eos) break
                    outputIndex = codec.dequeueOutputBuffer(info, 0)
                }
                if (eosSent && !drained && stopping.get()) break
            }
            engine.stop()
        } catch (t: Throwable) {
            failure = t
            runCatching { engine.stop() }
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            if (muxerStarted) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            running.set(false)
            if (failure != null || !muxerStarted) output.delete()
        }
    }

    override fun close() {
        stopAndJoin()
    }
}

/** AAC-LC profile constant kept local to avoid extra compatibility dependency. */
private object MediaCodecInfoCompat {
    const val AAC_LC = 2
}
