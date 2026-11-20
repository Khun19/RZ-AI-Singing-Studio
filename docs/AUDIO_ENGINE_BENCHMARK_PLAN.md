# Audio Engine Benchmark Plan

- **Status:** Oboe implementation started; physical-device benchmark pending
- **Owner:** RZ AI Singing Studio
- **Baseline branch:** main
- **Scope:** Android 12+ (API 31+) audio capability discovery, Oboe integration and reproducible engine comparison
- **Related architecture:** [Architecture — synchronized karaoke recording](ARCHITECTURE.md#42-synchronized-karaoke-recording)
- **Decision record:** [ADR-0001 — Audio engine selection](adr/0001-audio-engine-selection.md)

## 1. Objective

Implement Oboe as the production engine family while retaining Android AudioRecord + AudioTrack as the benchmark baseline. Production requests Oboe AudioApi::Unspecified and records the actual backend; AAudio is preferred where recommended by Oboe. Explicit OpenSL ES mode is reserved for compatibility benchmarking and must not be assumed to be a reliable modern-device fallback.

## 2. Current source and device evidence

- Kotlin + Jetpack Compose app; minimum API 31 and target API 35.
- Existing imported-song preview uses MediaPlayer.
- Existing user-facing microphone-only AAC/M4A capture uses MediaRecorder.
- Native Oboe engine foundation is being added behind a Kotlin/JNI API.
- The native engine requests 48 kHz Float PCM, mono input and stereo output; actual values/backend are queried from opened streams.
- The engine uses Oboe FullDuplexStream, preallocated single-producer/single-consumer PCM rings, optional microphone monitoring, playback PCM queueing and capture PCM draining.
- Termux on Redmi M2007J22C / API 31 previously proved system-level simultaneous microphone capture and speaker playback. This is not app-level synchronization or latency acceptance.
- User has confirmed the physical-device test passed. Detailed app-level latency percentiles, drift, xrun counts, route matrix and endurance records have not been supplied for independent review.

## 3. Benchmark phases

### Phase 0 — Build and native smoke verification
- Build Oboe 1.11.0 through Google Maven Prefab, CMake and the pinned Android NDK.
- Verify arm64-v8a and x86_64 packaging at minimum.
- Verify native library load, engine object lifecycle, direct-buffer validation and PCM queue operations on API 31/API 35 emulators.
- Treat emulator success as compilation/integration evidence only.

### Phase 1 — Physical Oboe stream validation

**User-reported status:** PASS. Treat this as user-confirmed device acceptance for the tested configuration; do not infer unprovided latency, drift, backend or endurance measurements.
- On Redmi M2007J22C / API 31, grant microphone permission explicitly and start the engine.
- Record actual input/output backend, sample rate, channel count, sharing mode, frames per burst, buffer size/capacity and errors.
- Confirm simultaneous input and output callbacks are active.
- Keep monitoring disabled for initial tests; enable only with headphones and explicit user action.
- Save complete diagnostics and logcat; do not infer latency from nominal buffer duration.

### Phase 2 — Native AudioRecord/AudioTrack baseline
- Build a disposable native baseline using matching PCM formats and equivalent workloads.
- Use preallocated buffers and a dedicated high-priority audio thread.
- Record monotonic timestamps, frame positions, buffer sizes and underrun/overrun/error counts.
- Compare under identical device, route, sample rate, duration and workload.

### Phase 3 — Physical loopback and synchronization
Measure separately:
- Input-to-output round-trip latency using a repeatable electrical/acoustic loopback method.
- Wired live monitoring latency.
- Capture/playback frame drift over 5, 15 and 30 minutes.
- Pause/resume alignment and discontinuities.
- Speaker, wired, USB-C and Bluetooth route behavior where available.
- XRun, underrun, overrun and dropped-frame counts.

Do not infer end-to-end latency from buffer duration or emulator results.

### Phase 4 — Endurance and resource use
- 30-minute continuous capture/playback stability run.
- 60-minute battery/thermal observation where practical.
- CPU and memory sampling at idle, typical and peak workloads.
- Screen-on/off, route change, disconnect/reconnect and interruption tests.
- Safe stop/restart and process-death recovery after durable recording segments are implemented.

## 4. Device and route matrix

| Tier | Device / environment | Purpose |
|---|---|---|
| Primary physical | Redmi M2007J22C, Android 12 / API 31, arm64 | User's primary physical baseline |
| Modern physical | Android 14 / API 34 device | Current route and lifecycle behavior |
| Modern physical | Android 15 / API 35 device | Target-SDK compatibility |
| Lower capability | Representative low-RAM Android 12+ device | Resource and fallback behavior |
| CI emulator | API 31 and API 35 | Functional regression only; not latency acceptance |

## 5. Provisional acceptance gates

| Metric | Evidence | Provisional target |
|---|---|---|
| Wired round-trip latency | At least 20 loopback measurements; median and p95 | Prefer median <=20 ms; investigate 20–30 ms; >30 ms unsuitable for default live monitoring |
| Sync drift | Frame/timestamp comparison over 30 minutes | <=10 ms with no unexplained discontinuity |
| Glitches/xruns | Counters plus recorded loopback | Zero audible dropouts in 30-minute target workload |
| Stream lifecycle | Repeated start/stop and interruption cycles | No crash, leaked stream or corrupted take |
| Route changes | Input/output route and recovery logs | No silent wrong-route capture |
| CPU/memory | Time-series under identical workload | Record average, p95 and peak; set device-tier budgets from baseline |
| Battery/thermal | Controlled 60-minute observation | No thermal shutdown or sustained throttling |
| Bluetooth | Per-device/codec measurements | Characterize and compensate where feasible; never promise zero latency |

These are provisional engineering targets, not current results or guarantees.

## 6. Execution controls

Every run must record:
- Git commit SHA, APK SHA-256, workflow run ID and harness version.
- Device model, build fingerprint, API, ABI, battery, thermal state and free storage.
- Audio route/accessory/codec where observable.
- Actual backend, stream properties and exact test duration/repetitions.
- Raw diagnostics, logcat, measurement files, calculations and exclusions.
- Failed attempts and root cause; do not discard outliers without a documented rule.

Keep CI, emulator install/launch, physical stream capability, loopback latency, synchronization and endurance as separate acceptance states.

## 7. Safety and product constraints

- No audio upload or cloud processing.
- No background recording claim until foreground-service and lifecycle work is implemented.
- Do not enable live monitoring by default; avoid speaker feedback.
- Keep file encoding and disk I/O off audio callbacks.
- Preserve original recordings and use disposable fixtures for recovery tests.
- Do not claim support for a route or format that was not tested.

## 8. Exit criteria

The Oboe engine is not production-accepted until exact-head CI, native smoke tests, physical Redmi stream validation, loopback latency, drift, route changes, 30-minute stability, resource measurements, durable recording integration and fallback behavior all pass with inspectable evidence.
