#include <jni.h>
#include <android/log.h>

#include <oboe/Oboe.h>
#include <oboe/FullDuplexStream.h>

#include <algorithm>
#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>

namespace {
constexpr char kTag[] = "RZAudioEngine";
constexpr int32_t kRequestedSampleRate = 48000;
constexpr int32_t kInputChannels = 1;
constexpr int32_t kOutputChannels = 2;
constexpr uint32_t kCaptureCapacity = 1u << 19;   // 10.9 seconds at 48 kHz.
constexpr uint32_t kPlaybackCapacity = 1u << 20;  // 10.9 seconds, stereo frames.

class FloatSpscRing {
public:
    explicit FloatSpscRing(uint32_t capacity)
        : mData(capacity), mCapacity(capacity), mMask(capacity - 1) {}

    uint32_t write(const float *source, uint32_t count) noexcept {
        const uint32_t writeIndex = mWrite.load(std::memory_order_relaxed);
        const uint32_t readIndex = mRead.load(std::memory_order_acquire);
        const uint32_t available = mCapacity - (writeIndex - readIndex);
        const uint32_t accepted = std::min(count, available);
        for (uint32_t i = 0; i < accepted; ++i) {
            mData[(writeIndex + i) & mMask] = source[i];
        }
        mWrite.store(writeIndex + accepted, std::memory_order_release);
        return accepted;
    }

    uint32_t read(float *destination, uint32_t count) noexcept {
        const uint32_t readIndex = mRead.load(std::memory_order_relaxed);
        const uint32_t writeIndex = mWrite.load(std::memory_order_acquire);
        const uint32_t available = writeIndex - readIndex;
        const uint32_t taken = std::min(count, available);
        for (uint32_t i = 0; i < taken; ++i) {
            destination[i] = mData[(readIndex + i) & mMask];
        }
        mRead.store(readIndex + taken, std::memory_order_release);
        return taken;
    }

    uint32_t availableToRead() const noexcept {
        return mWrite.load(std::memory_order_acquire) -
               mRead.load(std::memory_order_acquire);
    }

private:
    std::vector<float> mData;
    const uint32_t mCapacity;
    const uint32_t mMask;
    alignas(64) std::atomic<uint32_t> mWrite{0};
    alignas(64) std::atomic<uint32_t> mRead{0};
};

class AudioEngine;

class StreamErrorCallback final : public oboe::AudioStreamErrorCallback {
public:
    explicit StreamErrorCallback(AudioEngine *engine) : mEngine(engine) {}
    void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result error) override;
private:
    AudioEngine *mEngine;
};

class AudioEngine final : public oboe::FullDuplexStream {
public:
    AudioEngine()
        : mCaptureRing(kCaptureCapacity),
          mPlaybackRing(kPlaybackCapacity),
          mErrorCallback(this) {}

    ~AudioEngine() override { stop(); }

    int32_t start() {
        std::lock_guard<std::mutex> guard(mLifecycleMutex);
        if (mRunning.load(std::memory_order_acquire)) return 0;
        closeStreamsLocked();
        mLastError.store(0, std::memory_order_relaxed);
        mCaptureDropped.store(0, std::memory_order_relaxed);
        mPlaybackUnderrun.store(0, std::memory_order_relaxed);
        mCallbackCount.store(0, std::memory_order_relaxed);
        mCapturedFrames.store(0, std::memory_order_relaxed);
        mPlayedFrames.store(0, std::memory_order_relaxed);

        auto result = openStreamsLocked(oboe::SharingMode::Exclusive);
        if (result != oboe::Result::OK) {
            closeStreamsLocked();
            result = openStreamsLocked(oboe::SharingMode::Shared);
        }
        if (result != oboe::Result::OK) {
            closeStreamsLocked();
            mLastError.store(static_cast<int32_t>(result), std::memory_order_release);
            return static_cast<int32_t>(result);
        }

        setSharedInputStream(mInputStream);
        setSharedOutputStream(mOutputStream);
        setNumInputBurstsCushion(1);
        setMinimumFramesBeforeRead(0);
        result = oboe::FullDuplexStream::start();
        if (result != oboe::Result::OK) {
            closeStreamsLocked();
            mLastError.store(static_cast<int32_t>(result), std::memory_order_release);
            return static_cast<int32_t>(result);
        }
        mRunning.store(true, std::memory_order_release);
        return 0;
    }

    int32_t stop() {
        std::lock_guard<std::mutex> guard(mLifecycleMutex);
        mRunning.store(false, std::memory_order_release);
        if (mInputStream && mOutputStream) {
            (void) oboe::FullDuplexStream::stop();
        }
        closeStreamsLocked();
        return 0;
    }

    void setMonitoring(bool enabled) noexcept {
        mMonitoring.store(enabled, std::memory_order_release);
    }

    uint32_t readCaptured(float *destination, uint32_t count) noexcept {
        return mCaptureRing.read(destination, count);
    }

    uint32_t queuePlayback(const float *source, uint32_t samples) noexcept {
        return mPlaybackRing.write(source, samples);
    }

    oboe::DataCallbackResult onBothStreamsReady(
            const void *inputData,
            int32_t numInputFrames,
            void *outputData,
            int32_t numOutputFrames) override {
        auto *output = static_cast<float *>(outputData);
        const auto *input = static_cast<const float *>(inputData);
        const int32_t outputSamples = numOutputFrames * kOutputChannels;
        std::fill(output, output + outputSamples, 0.0f);

        if (input != nullptr && numInputFrames > 0) {
            const uint32_t captured = mCaptureRing.write(
                input, static_cast<uint32_t>(numInputFrames));
            mCapturedFrames.fetch_add(static_cast<uint64_t>(numInputFrames),
                                      std::memory_order_relaxed);
            if (captured < static_cast<uint32_t>(numInputFrames)) {
                mCaptureDropped.fetch_add(
                    static_cast<uint64_t>(numInputFrames) - captured,
                    std::memory_order_relaxed);
            }
        }

        const uint32_t playbackSamples = static_cast<uint32_t>(outputSamples);
        const uint32_t playbackRead = mPlaybackRing.read(output, playbackSamples);
        if (playbackRead < playbackSamples) {
            mPlaybackUnderrun.fetch_add(playbackSamples - playbackRead,
                                        std::memory_order_relaxed);
        }

        if (input != nullptr && numInputFrames > 0 &&
            mMonitoring.load(std::memory_order_acquire)) {
            const float gain = mMonitorGain.load(std::memory_order_relaxed);
            const int32_t frames = std::min(numInputFrames, numOutputFrames);
            for (int32_t frame = 0; frame < frames; ++frame) {
                const float sample = input[frame] * gain;
                output[frame * 2] += sample;
                output[frame * 2 + 1] += sample;
            }
        }

        for (int32_t i = 0; i < outputSamples; ++i) {
            output[i] = std::max(-1.0f, std::min(1.0f, output[i]));
        }
        mPlayedFrames.fetch_add(static_cast<uint64_t>(numOutputFrames),
                                 std::memory_order_relaxed);
        mCallbackCount.fetch_add(1, std::memory_order_relaxed);
        return oboe::DataCallbackResult::Continue;
    }

    void onOutputErrorAfterClose(oboe::Result error) {
        mRunning.store(false, std::memory_order_release);
        mLastError.store(static_cast<int32_t>(error), std::memory_order_release);
        std::lock_guard<std::mutex> guard(mLifecycleMutex);
        if (mInputStream) {
            (void) mInputStream->requestStop();
            (void) mInputStream->close();
            mInputStream.reset();
        }
        mOutputStream.reset();
    }

    std::string diagnostics() {
        std::lock_guard<std::mutex> guard(mLifecycleMutex);
        std::ostringstream out;
        out << "{"
            << "\"running\":" << (mRunning.load() ? "true" : "false")
            << ",\"sampleRate\":" << (mOutputStream ? mOutputStream->getSampleRate() : 0)
            << ",\"inputSampleRate\":" << (mInputStream ? mInputStream->getSampleRate() : 0)
            << ",\"inputChannels\":" << (mInputStream ? mInputStream->getChannelCount() : 0)
            << ",\"outputChannels\":" << (mOutputStream ? mOutputStream->getChannelCount() : 0)
            << ",\"framesPerBurst\":" << (mOutputStream ? mOutputStream->getFramesPerBurst() : 0)
            << ",\"bufferSizeFrames\":" << (mOutputStream ? mOutputStream->getBufferSizeInFrames() : 0)
            << ",\"bufferCapacityFrames\":" << (mOutputStream ? mOutputStream->getBufferCapacityInFrames() : 0)
            << ",\"actualBackend\":\"" << backendName(mOutputStream) << "\""
            << ",\"inputBackend\":\"" << backendName(mInputStream) << "\""
            << ",\"sharingMode\":\"" << sharingName(mOutputStream) << "\""
            << ",\"callbacks\":" << mCallbackCount.load()
            << ",\"capturedFrames\":" << mCapturedFrames.load()
            << ",\"playedFrames\":" << mPlayedFrames.load()
            << ",\"captureDroppedFrames\":" << mCaptureDropped.load()
            << ",\"playbackUnderrunSamples\":" << mPlaybackUnderrun.load()
            << ",\"captureQueueSamples\":" << mCaptureRing.availableToRead()
            << ",\"lastError\":" << mLastError.load()
            << "}";
        return out.str();
    }

private:
    oboe::Result openStreamsLocked(oboe::SharingMode sharingMode) {
        oboe::AudioStreamBuilder outputBuilder;
        outputBuilder.setDirection(oboe::Direction::Output)
            ->setAudioApi(oboe::AudioApi::Unspecified)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(sharingMode)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(kOutputChannels)
            ->setSampleRate(kRequestedSampleRate)
            ->setUsage(oboe::Usage::Media)
            ->setContentType(oboe::ContentType::Music)
            ->setDataCallback(this)
            ->setErrorCallback(&mErrorCallback);
        auto result = outputBuilder.openStream(mOutputStream);
        if (result != oboe::Result::OK) return result;

        oboe::AudioStreamBuilder inputBuilder;
        inputBuilder.setDirection(oboe::Direction::Input)
            ->setAudioApi(oboe::AudioApi::Unspecified)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(sharingMode)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(kInputChannels)
            ->setSampleRate(mOutputStream->getSampleRate())
            ->setBufferCapacityInFrames(mOutputStream->getBufferCapacityInFrames() * 2);
        result = inputBuilder.openStream(mInputStream);
        if (result != oboe::Result::OK) return result;
        return oboe::Result::OK;
    }

    void closeStreamsLocked() {
        if (mOutputStream) {
            if (mOutputStream->getState() == oboe::StreamState::Started) {
                (void) mOutputStream->requestStop();
            }
            (void) mOutputStream->close();
            mOutputStream.reset();
        }
        if (mInputStream) {
            if (mInputStream->getState() == oboe::StreamState::Started) {
                (void) mInputStream->requestStop();
            }
            (void) mInputStream->close();
            mInputStream.reset();
        }
    }

    static const char *backendName(const std::shared_ptr<oboe::AudioStream> &stream) {
        if (!stream) return "CLOSED";
        switch (stream->getAudioApi()) {
            case oboe::AudioApi::AAudio: return "AAUDIO";
            case oboe::AudioApi::OpenSLES: return "OPENSL_ES";
            default: return "UNSPECIFIED";
        }
    }

    static const char *sharingName(const std::shared_ptr<oboe::AudioStream> &stream) {
        if (!stream) return "CLOSED";
        return stream->getSharingMode() == oboe::SharingMode::Exclusive
            ? "EXCLUSIVE" : "SHARED";
    }

    std::mutex mLifecycleMutex;
    std::shared_ptr<oboe::AudioStream> mInputStream;
    std::shared_ptr<oboe::AudioStream> mOutputStream;
    FloatSpscRing mCaptureRing;
    FloatSpscRing mPlaybackRing;
    StreamErrorCallback mErrorCallback;
    std::atomic<bool> mRunning{false};
    std::atomic<bool> mMonitoring{false};
    std::atomic<float> mMonitorGain{0.8f};
    std::atomic<int32_t> mLastError{0};
    std::atomic<uint64_t> mCallbackCount{0};
    std::atomic<uint64_t> mCapturedFrames{0};
    std::atomic<uint64_t> mPlayedFrames{0};
    std::atomic<uint64_t> mCaptureDropped{0};
    std::atomic<uint64_t> mPlaybackUnderrun{0};
};

void StreamErrorCallback::onErrorAfterClose(
        oboe::AudioStream * /*stream*/, oboe::Result error) {
    mEngine->onOutputErrorAfterClose(error);
}

AudioEngine *fromHandle(jlong handle) {
    return reinterpret_cast<AudioEngine *>(static_cast<intptr_t>(handle));
}
} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_rzstudio_singing_AudioEngine_nativeCreate(JNIEnv *, jobject) {
    auto *engine = new AudioEngine();
    return static_cast<jlong>(reinterpret_cast<intptr_t>(engine));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_rzstudio_singing_AudioEngine_nativeStart(JNIEnv *, jobject, jlong handle) {
    auto *engine = fromHandle(handle);
    return engine == nullptr ? static_cast<jint>(oboe::Result::ErrorNull) : engine->start();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_rzstudio_singing_AudioEngine_nativeStop(JNIEnv *, jobject, jlong handle) {
    auto *engine = fromHandle(handle);
    return engine == nullptr ? static_cast<jint>(oboe::Result::ErrorNull) : engine->stop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_rzstudio_singing_AudioEngine_nativeSetMonitoring(
        JNIEnv *, jobject, jlong handle, jboolean enabled) {
    if (auto *engine = fromHandle(handle)) engine->setMonitoring(enabled == JNI_TRUE);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_rzstudio_singing_AudioEngine_nativeReadCapturedPcm(
        JNIEnv *env, jobject, jlong handle, jobject destination, jint maxSamples) {
    auto *engine = fromHandle(handle);
    auto *address = static_cast<float *>(env->GetDirectBufferAddress(destination));
    const jlong capacity = env->GetDirectBufferCapacity(destination);
    if (engine == nullptr || address == nullptr || maxSamples < 0 || capacity < maxSamples) return -1;
    return static_cast<jint>(engine->readCaptured(address, static_cast<uint32_t>(maxSamples)));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_rzstudio_singing_AudioEngine_nativeQueuePlaybackPcm(
        JNIEnv *env, jobject, jlong handle, jobject source, jint samples) {
    auto *engine = fromHandle(handle);
    auto *address = static_cast<const float *>(env->GetDirectBufferAddress(source));
    const jlong capacity = env->GetDirectBufferCapacity(source);
    if (engine == nullptr || address == nullptr || samples < 0 || capacity < samples) return -1;
    return static_cast<jint>(engine->queuePlayback(address, static_cast<uint32_t>(samples)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_rzstudio_singing_AudioEngine_nativeDiagnostics(
        JNIEnv *env, jobject, jlong handle) {
    auto *engine = fromHandle(handle);
    const std::string value = engine == nullptr ? "{}" : engine->diagnostics();
    return env->NewStringUTF(value.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_rzstudio_singing_AudioEngine_nativeDestroy(JNIEnv *, jobject, jlong handle) {
    delete fromHandle(handle);
}
