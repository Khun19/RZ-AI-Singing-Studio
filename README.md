# RZ AI Singing Studio

RZ AI Singing Studio is a standalone Android karaoke, vocal-recording and AI cover-arrangement application. It is a separate project from RZ Builder / Universal-ZIP-to-APK-Builder.

The product goal is to let users sing along with music, create karaoke arrangements with newly generated or re-performed instruments, improve and mix their own vocal recordings, and manage audio projects locally.

> **Project status:** Early Android MVP with the first Oboe native audio-engine foundation under implementation. This is not yet a fully accepted synchronized recording product.

## Product principles

- **Android 12+** support, with testing across device capability tiers rather than only one phone.
- **Offline-first AI:** prefer on-device processing whenever technically practical.
- **Optional cloud AI:** never upload audio silently. Ask for explicit consent for each cloud task and disclose expected cost before processing.
- **Local alternatives:** provide an on-device alternative wherever feasible and explain any quality, speed or capability differences.
- **Free-first:** no mandatory subscription for core functionality and no forced export watermark.
- **Privacy:** recordings and project data stay local by default. No automatic cloud backup.
- **Licensed technology:** review model, library, dataset, music and artwork licenses for intended commercial distribution.
- **Honest capability reporting:** distinguish implemented, experimental, planned and validated features. CI success alone is not proof of runtime acceptance.

## Current implementation status

The existing MVP includes:
- Local audio import through Android Storage Access Framework.
- Basic imported-song preview using Android MediaPlayer.
- User-facing microphone-only AAC/M4A recording using Android MediaRecorder.
- Local take listing and deletion.

The feature branch adds an initial native Oboe full-duplex engine foundation:
- Oboe 1.11.0 through Google Maven Prefab, CMake and C++17.
- Kotlin/JNI engine lifecycle API.
- Oboe full-duplex input/output stream setup with Exclusive-first and Shared retry.
- Preallocated SPSC PCM queues for captured mono PCM and queued stereo playback PCM.
- Optional microphone monitoring, disabled by default.
- Actual stream/backend diagnostics.

**Not yet validated:** native compilation/CI, emulator native smoke tests, Redmi physical Oboe stream startup, actual backend selection, loopback latency, synchronization drift, route changes and endurance.

The current user-facing recorder remains MediaRecorder mic-only. The native engine is not yet wired into durable AAC/M4A recording or the imported backing-track decoder. Therefore synchronized backing-plus-mic recording, final track alignment, live monitoring acceptance, Bluetooth compensation, background recording, crash recovery, lyrics, vocal separation, AI cover arrangement, effects, mix/master and export remain unimplemented or unaccepted.

Do not interpret native source presence as production feature acceptance. Each capability must pass its own tests and device acceptance criteria.

## Approved feature scope

The full product requirements are documented in [Karaoke & AI Singing Studio Requirements](docs/KARAOKE_AI_SINGING_STUDIO_REQUIREMENTS.md). Key areas include karaoke playback, licensed lyrics and Myanmar lyric timing, multi-take recording, natural vocal enhancement, AI cover arrangement, audio-only export and a local library.

## AI cost and privacy policy

The approved policy is **Offline-First + Optional Paid Cloud AI + Local Alternative**. Cloud processing requires explicit task-level consent and disclosure of uploaded data, expected charges and provider retention. No silent uploads or automatic cloud backup.

## Audio engine decision

[ADR-0001](docs/adr/0001-audio-engine-selection.md) selects Oboe for implementation. Production requests Oboe's AudioApi::Unspecified and records the actual backend; AAudio is preferred where recommended. Explicit OpenSL ES selection is reserved for compatibility benchmarking, not assumed to be a reliable API 31+ fallback. Android AudioRecord/AudioTrack remains the benchmark baseline.

See the [Audio Engine Benchmark Plan](docs/AUDIO_ENGINE_BENCHMARK_PLAN.md) for physical validation gates. Emulator CI cannot establish physical latency or synchronization acceptance.

## Build and test

Requirements:
- Android 12 / API 31 or later.
- Java 17.
- Gradle 8.9 via the committed Gradle Wrapper.
- Android SDK platform 35 and build tools 35.0.0.
- Android NDK 27.2.12479018 and CMake 3.22.1 for native audio builds.

Run:
```bash
./gradlew --version
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

GitHub Actions builds and validates the exact debug APK on API 31 and API 35 emulators. Debug APKs are not signed production releases. Verify the exact workflow run, commit and artifact.

## Evidence and release gates

Use **Root Cause → Fix → Test → Evidence**. Keep source/CI, APK validation, emulator install/launch, physical-device acceptance and feature-specific acceptance separate. No feature is complete until its own acceptance criteria and evidence pass.

## Audio and copyright

Only import, process, arrange or distribute music the user owns or is authorized to use. Online sources, lyrics, models, datasets and assets must comply with applicable rights and provider terms.

## License

Apache License 2.0. See [LICENSE](LICENSE).
