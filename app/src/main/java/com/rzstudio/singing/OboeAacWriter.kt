package com.rzstudio.singing

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/** Experimental worker-thread capture encoder. Not connected to public UI. */
internal class OboeAacWriter(
    private val engine: AudioEngine,
    private val output: File,
    private val sampleRate: Int = 48_000,
    private val bitrate: Int = 192_000
) : AutoCloseable {
    private val stopping = AtomicBoolean(false)
    private var worker: Thread? = null
    @Volatile private var failure: Throwable? = null

    fun start() {
        check(worker == null) { "Writer instance is single-use" }
        require(sampleRate > 0 && bitrate > 0)
        check(!output.exists() || output.delete()) { "Output file already exists" }
        output.parentFile?.mkdirs()
        stopping.set(false)
        worker = Thread(::runEncoder, "rz-aac-writer").apply { start() }
    }

    fun stopAndJoin(timeoutMs: Long = 15_000): File {
        val thread = worker ?: error("Writer has not started")
        stopping.set(true)
        thread.join(timeoutMs)
        check(!thread.isAlive) { "Encoder worker did not stop in time" }
        failure?.let { throw IllegalStateException("Recording failed", it) }
        check(output.isFile && output.length() > 0) { "No recording produced" }
        return output
    }

    private fun runEncoder() {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1
        var engineStarted = false
        var totalSamples = 0L
        var eosSent = false
        val samples = FloatArray(2048)
        val floatBuffer = ByteBuffer.allocateDirect(samples.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        val pcmBytes = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder())
        try {
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1)
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.AAC_PROFILE_LC)
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, pcmBytes.capacity())
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            check(engine.start() == 0) { "Audio engine failed to start" }
            engineStarted = true

            val info = MediaCodec.BufferInfo()
            var outputEos = false
            while (!outputEos) {
                if (!eosSent) {
                    val inputIndex = codec.dequeueInputBuffer(5_000)
                    if (inputIndex >= 0) {
                        val input = codec.getInputBuffer(inputIndex) ?: error("Encoder input buffer unavailable")
                        input.clear()
                        floatBuffer.clear()
                        val count = engine.readCapturedPcm(floatBuffer, samples.size)
                        if (count > 0) {
                            floatBuffer.flip()
                            for (i in 0 until count) samples[i] = floatBuffer.get()
                            pcmBytes.clear()
                            Pcm16Converter.convert(samples, count, pcmBytes)
                            pcmBytes.flip()
                            input.put(pcmBytes)
                            codec.queueInputBuffer(inputIndex, 0, count * 2,
                                Pcm16Converter.presentationTimeUs(totalSamples, sampleRate), 0)
                            totalSamples += count
                        } else if (stopping.get()) {
                            // Stop capture first, then drain any PCM still buffered in the engine.
                            if (engineStarted) {
                                engine.stop()
                                engineStarted = false
                            }
                            floatBuffer.clear()
                            val remaining = engine.readCapturedPcm(floatBuffer, samples.size)
                            if (remaining > 0) {
                                floatBuffer.flip()
                                for (i in 0 until remaining) samples[i] = floatBuffer.get()
                                pcmBytes.clear()
                                Pcm16Converter.convert(samples, remaining, pcmBytes)
                                pcmBytes.flip()
                                input.put(pcmBytes)
                                codec.queueInputBuffer(inputIndex, 0, remaining * 2,
                                    Pcm16Converter.presentationTimeUs(totalSamples, sampleRate), 0)
                                totalSamples += remaining
                            } else {
                                codec.queueInputBuffer(inputIndex, 0, 0,
                                    Pcm16Converter.presentationTimeUs(totalSamples, sampleRate),
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                eosSent = true
                            }
                        } else {
                            // No PCM yet. Return the input buffer without advancing the sample clock.
                            codec.queueInputBuffer(inputIndex, 0, 0,
                                Pcm16Converter.presentationTimeUs(totalSamples, sampleRate), 0)
                        }
                    }
                }
                var outIndex = codec.dequeueOutputBuffer(info, 10_000)
                while (outIndex >= 0) {
                    val out = codec.getOutputBuffer(outIndex)
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0) {
                        check(muxerStarted) { "Encoded data arrived before output format" }
                        requireNotNull(out)
                        out.position(info.offset)
                        out.limit(info.offset + info.size)
                        muxer.writeSampleData(trackIndex, out, info)
                    }
                    outputEos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    codec.releaseOutputBuffer(outIndex, false)
                    if (outputEos) break
                    outIndex = codec.dequeueOutputBuffer(info, 0)
                }
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    check(!muxerStarted) { "Encoder output format changed more than once" }
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
            }
        } catch (t: Throwable) {
            failure = t
            runCatching { if (engineStarted) engine.stop() }
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            if (muxerStarted) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            if (failure != null || !muxerStarted) output.delete()
        }
    }

    override fun close() { stopAndJoin() }
}
