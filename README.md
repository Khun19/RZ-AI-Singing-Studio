# RZ AI Singing Studio

RZ AI Singing Studio is an Android application for creating karaoke backing tracks and recording vocal performances. It is a separate project from RZ Builder.

## Current status

This repository is an early MVP. Implemented on the current development branch:
- Import a user-selected audio document through Android Storage Access Framework.
- Basic audio preview using Android MediaPlayer.
- Microphone recording to AAC/M4A files using Android MediaRecorder.
- Local take listing and deletion inside the app's private storage.

Not implemented yet:
- AI vocal/instrumental separation (no model or inference runtime is shipped).
- Live monitoring, latency compensation, synchronized multitrack recording or take alignment.
- Pitch correction, denoise, EQ, compression, de-essing, reverb, delay, mixing or mastering.
- MP3/FLAC export, karaoke video rendering or 4K video export.
- Release signing and verified physical-device acceptance.

A successful CI build is not evidence that these future features work.

## Requirements

- Android 12 (API 31) or later.
- Android Studio/Android SDK for local development, or GitHub Actions for CI.
- Java 17.
- Gradle 8.9 (the current CI currently invokes the installed Gradle distribution; Gradle Wrapper setup is still pending).

## Build and test

The CI workflow runs JVM unit tests, Android instrumentation tests on an emulator, and assembles a debug APK. For local builds after the wrapper is added, the intended commands are:

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

Until a Gradle Wrapper is committed, use a compatible Gradle 8.9 installation with the same tasks.

## Audio and privacy

- Only import and process audio that you own or are authorized to use.
- Microphone access is requested only when the user starts recording.
- Recorded takes are stored in the app's private internal storage.
- The current recorder captures microphone audio only; it does not yet mix the selected backing track into the recording.
- No cloud upload or subscription requirement is part of this MVP.

## Architecture direction

The planned modules are audio import/playback, recording/take management, vocal separation, vocal processing, producer/mix analysis, and export. AI model selection will be based on license compatibility, Android ABI/runtime support, model size, latency, memory use and measured separation quality. Model availability alone will not be treated as proof of a working feature.

## License

The project is distributed under Apache License 2.0. See [LICENSE](LICENSE).
