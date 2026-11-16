# Architecture

## Product boundary
RZ AI Singing Studio is a standalone Android application. It is independent of RZ Builder and does not convert arbitrary ZIP files into APKs.

## Initial stack
- Kotlin and Jetpack Compose for the Android application.
- Android Storage Access Framework for user-selected audio files.
- Android media APIs for the initial import/preview path.
- A modular native audio engine is planned for low-latency recording and processing.
- AI separation and enhancement are isolated behind processing interfaces so model/runtime choices can be tested independently.

## Planned modules
- `audio-import`: authorized user-selected audio and metadata.
- `audio-engine`: playback, monitoring, recording and routing.
- `vocal-separation`: instrumental/vocal stem generation.
- `vocal-processing`: denoise, pitch correction, EQ, compression and effects.
- `producer`: analysis, mix recommendations and mastering.
- `export`: WAV/MP3/FLAC and mixed/stem output.

## Delivery principles
- Android 12 (API 31) minimum.
- Prefer on-device processing where performance and model size allow.
- Heavy AI workloads may use an explicitly disclosed optional cloud path.
- Never claim an AI operation is implemented until the model, runtime, tests and output validation exist.
- Process only audio the user is authorized to use.
