# RZ AI Singing Studio

RZ AI Singing Studio is an Android application for creating karaoke backing tracks and recording vocal performances. It is a separate project from RZ Builder.

## Current status

This repository is an early Android MVP. Implemented on the development branch:

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

A successful CI build is not evidence that these future features work. Recording, take persistence and deletion still require successful emulator and physical-device acceptance.

## Requirements

- Android 12 (API 31) or later.
- Java 17.
- Gradle 8.9 through the committed Gradle Wrapper.
- Android SDK platform 35 and build tools 35.0.0 for local builds.

## Build and test

Use the committed Gradle Wrapper:

```bash
./gradlew --version
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

GitHub Actions runs JVM unit tests and Android emulator instrumentation tests. The debug APK is assembled and uploaded only after both test jobs pass. CI artifacts are development builds, not signed production releases.

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
