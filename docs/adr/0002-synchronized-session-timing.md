# ADR-0002: Synchronized Karaoke Session Timing

- **Status:** Provisional implementation contract; audio-engine choice remains open.
- **Date:** 2026-11-28
- **Related feature:** KARA-001
- **Related tests:** T-SYNC, T-TAKE, T-LIFECYCLE

## Context

The current MVP uses MediaPlayer for imported-song preview and MediaRecorder for microphone-only AAC/M4A takes. That path is already user-visible and must remain intact as a regression baseline.

Synchronized karaoke recording introduces two independently progressing media paths: backing-track playback and microphone capture. Their start boundaries, pause/resume transitions, seek discontinuities and recovery state must be represented on one monotonic session timeline. A wall-clock timestamp is unsuitable because wall time can be adjusted.

MediaPlayer and MediaRecorder do not expose a shared sample-accurate hardware clock to this app. Recording the time at which their start methods are called is useful diagnostic metadata, but it is not proof of the first rendered output frame or first captured microphone frame. Therefore, a timeline model alone must never be treated as synchronization acceptance.

## Provisional design

1. Use a monotonic time source (System.nanoTime() for the domain clock; audio-device timestamps for measured stream alignment).
2. Keep session elapsed time independent from backing-track position.
3. Represent every backing-track seek as a new segment mapping a session offset to a backing-track offset. Do not reset the recording-session timeline on seek.
4. Pause freezes session elapsed time; resume re-anchors the monotonic clock and excludes paused wall time.
5. Record the requested microphone-start boundary as diagnostic metadata. Label it as an API-call boundary unless a hardware capture timestamp is available.
6. Preserve original backing audio and original microphone capture. Any aligned mix is a separate derived output with source references and timing metadata.
7. Keep the existing MediaRecorder microphone-only flow unchanged. Synchronized capture is a separate implementation path until feature acceptance and a migration decision are approved.
8. Benchmark the existing MediaPlayer/MediaRecorder combination and candidate AudioTrack/AudioRecord and Oboe/AAudio paths independently. No engine is promoted based on API availability or a single device demonstration.

## Synchronization evidence required

- Capture and playback timestamps in a common monotonic timebase where the selected APIs expose them.
- Measured start offset and long-session drift over short and full-song runs.
- Buffer underrun/overrun, dropped-frame and glitch counts.
- Repeatability across multiple runs and after pause/resume/seek.
- Route-specific results for speaker, wired headset and Bluetooth where available.
- CPU, memory, thermal and battery observations on representative hardware.
- A documented fallback when timestamp quality or device capability is insufficient.

## Decision gates

- Unit tests of timeline transitions are necessary but insufficient.
- API 31 and API 35 emulator tests establish functional behavior, not physical audio latency acceptance.
- Physical-device acceptance is required on Redmi M2007J22C/API 31 and must retain raw measurement logs.
- AI vocal separation and AI processing remain blocked until synchronized capture, alignment, persistence and recovery pass their feature-specific gates.

## Consequences

The timeline model can be developed and tested without changing the current recorder. Actual synchronized audio capture still requires an audio I/O implementation that can provide reliable timing evidence. The app must not claim sample-accurate alignment from MediaRecorder/MediaPlayer start-call timestamps.
