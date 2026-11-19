# Audio Engine Benchmark Preparation

- **Status:** Research preparation; no engine selected
- **Owner:** RZ AI Singing Studio
- **Baseline branch:** `main`
- **Scope:** Android 12+ (API 31+) audio capability discovery and reproducible engine comparison
- **Related architecture:** [Architecture — synchronized karaoke recording](ARCHITECTURE.md#42-synchronized-karaoke-recording)
- **Decision record:** [ADR-0001 — Audio engine selection](adr/0001-audio-engine-selection.md)

## 1. Objective

Select an audio capture/playback engine for synchronized karaoke recording only after reproducible measurements on representative physical Android devices. The candidates are:

1. Android `AudioRecord` + `AudioTrack` as the native baseline/fallback.
2. Oboe (using AAudio where available and its supported fallback behavior) as the low-latency candidate.
3. Keep the current `MediaPlayer` + `MediaRecorder` path only as the existing MVP reference; it is not considered a professional synchronized PCM engine.

This preparation does not add recording, mixing, monitoring, background-service or AI features to the product. It does not select Oboe in advance.

## 2. Current verified source baseline

The inspected `main` source at the start of this research shows:

- Kotlin + Jetpack Compose application.
- `minSdk = 31`, `targetSdk = 35`, `compileSdk = 35`.
- Imported-song preview uses `MediaPlayer`.
- Microphone-only AAC/M4A capture uses `MediaRecorder` at 44.1 kHz / 192 kbps.
- No synchronized backing-track + microphone capture, PCM mixer, live monitoring, route-latency compensation or dedicated audio-engine abstraction is present.
- The existing manifest declares `RECORD_AUDIO`; it does not yet declare a microphone foreground service.

This is a source inspection baseline, not a physical-device performance result.

## 3. Benchmark phases

### Phase 0 — Capability discovery

Run `AudioEngineCapabilityProbeTest` on a physical device and retain the complete instrumentation output. Record:

- Manufacturer, model, build fingerprint, Android release and API level.
- ABI(s), available RAM and storage headroom.
- Native output sample-rate and frames-per-buffer properties when exposed.
- Available input/output audio devices, device types, channel counts and advertised sample rates.
- `AudioRecord.getMinBufferSize` and `AudioTrack.getMinBufferSize` results for 44.1/48 kHz, mono/stereo, PCM 16-bit.
- Permission state, audio mode, audio focus state and active route during each later test.

A reported minimum buffer size is a platform capability hint, not a latency measurement. A missing property or unsupported format must be recorded as unavailable, not silently replaced with an assumed value.

### Phase 1 — Native baseline prototype

Build a disposable, isolated prototype using `AudioRecord` and `AudioTrack` with matching PCM formats. It must:

- Use a dedicated high-priority audio thread and bounded preallocated buffers.
- Keep file I/O, logging, locks, allocations and other blocking work out of the real-time audio callback/loop.
- Record monotonic timestamps and audio frame positions.
- Count buffer underruns/overruns and capture errors.
- Expose requested and actual sample rates, channel masks, buffer sizes and performance mode.
- Stop and release resources safely on route changes, permission loss and lifecycle interruption.

Do not wire this prototype into the production UI or replace the MVP recorder in this phase.

### Phase 2 — Oboe prototype

Repeat the same workload, PCM format, device, route, duration and measurement procedure with Oboe. Record Oboe version, NDK version, ABI, API path/backend, sharing/performance modes and fallback behavior. Do not compare different workloads or different routes as if they were equivalent.

### Phase 3 — Physical loopback and session synchronization

Use a repeatable electrical/acoustic loopback arrangement and an established tool such as OboeTester where applicable. Record the exact method, signal, output/input route, measurement count and raw results. Measure separately:

- Input-to-output round-trip latency (not merely buffer size).
- Monitoring latency on each route.
- Playback/capture timestamp drift over 5, 15 and 30 minutes.
- Start alignment and discontinuity after pause/resume.
- Route-change behavior and recovery time.
- Glitches, xrun/underrun/overrun counts and lost frames.

Do not infer physical latency from emulator tests or from the nominal buffer duration.

### Phase 4 — Endurance and resource use

For each candidate and route:

- 30-minute continuous capture/playback stability run.
- 60-minute battery/thermal observation where practical.
- CPU and memory sampling at idle, typical workload and peak workload.
- Screen-on and screen-off lifecycle tests.
- Incoming-call/audio-focus interruption and explicit user-resume test.
- Wired disconnect/reconnect and Bluetooth connect/disconnect tests.
- Low-storage and process-death recovery tests only after a safe disposable prototype exists.

Do not use a real user's only copy of a recording for destructive or recovery experiments.

## 4. Required device and route matrix

| Tier | Device / environment | Purpose |
|---|---|---|
| Primary physical | Redmi M2007J22C, Android 12 / API 31, arm64 | User's minimum supported physical baseline |
| Modern physical | Android 14 / API 34 device | Current route and foreground-service behavior |
| Modern physical | Android 15 / API 35 device | Target-SDK behavior and compatibility |
| Lower capability | Representative low-RAM / lower-tier Android 12+ device | Resource and fallback behavior |
| CI emulator | API 31 and API 35 | Functional regression only; not physical latency acceptance |

Test routes independently: built-in speaker + microphone, wired headset, USB-C audio where available, and Bluetooth headset. Record exact accessory model, codec/profile when observable, and route state. Bluetooth results are characterization data; they must not be presented as zero-latency or equivalent to wired monitoring.

## 5. Metrics and provisional acceptance gates

All thresholds below are **provisional engineering targets**, to be reviewed against actual user experience and device data before becoming release gates.

| Metric | Required evidence | Provisional target / interpretation |
|---|---|---|
| Wired monitoring round-trip latency | At least 20 repeated loopback measurements; median and p95 | Prefer median <= 20 ms; investigate 20–30 ms; > 30 ms is unsuitable for default live monitoring |
| Session sync drift | Frame/timestamp comparison over 5/15/30 min | <= 10 ms drift over 30 min, with no unexplained discontinuity |
| Glitches / xruns | Counters plus recorded loopback | Zero audible dropouts in 30-min target workload |
| Start/stop reliability | Repeated start/stop and interruption cycles | No leaked audio resources, crash or corrupted saved take |
| Route changes | Input/output route event log and recovery timing | No silent wrong-route capture; safe stop/reconfigure or explicit recoverable state |
| CPU / memory | Time-series under same workload | Report average, p95 and peak; set device-tier budgets after baseline |
| Battery / thermal | 60-min controlled observation | No thermal shutdown or sustained throttling; report battery delta and temperature trend |
| Bluetooth | Per-device/codec latency distribution | Characterize and compensate where feasible; never promise zero latency |

A candidate passes only when raw evidence, device details, test version and repeatability are available. Missing measurements are **NOT RUN**, not PASS.

## 6. Test execution controls

For every run, record:

- Git commit SHA, APK SHA-256, build/run ID and test harness version.
- Device model, build fingerprint, API, ABI, kernel/build details where available.
- Battery percentage, thermal state, free storage and background workload.
- Audio route, accessory model, codec if observable, sample rate, channel count and buffer size.
- Exact test start/end time, duration, number of repetitions and environmental setup.
- Raw output/logcat, measurement files, calculations and any exclusions.
- Any failed attempt and its root cause; do not discard outliers without a documented rule.

Keep source CI, emulator functional tests, physical capability discovery, physical loopback latency acceptance and long-duration stability as separate statuses.

## 7. Safety and product constraints

- No production engine switch or manifest/service change in this preparation phase.
- Request microphone permission only for tests that actually capture audio; the capability probe does not need to record microphone data.
- No audio upload or cloud processing.
- Do not record or retain bystanders; use a controlled test signal and test environment.
- Preserve original recordings and use disposable fixtures for recovery tests.
- Do not claim support for a route or format that was not exercised.
- Any new native dependency must pass ABI, size, license, security and maintenance review before adoption.

## 8. Exit criteria for engine decision

The team may prepare an Accepted engine ADR only after:

1. Capability discovery completed on the primary physical device and at least one additional Android 12+ physical device.
2. Native baseline and Oboe prototypes run under the same repeatable workload.
3. Wired round-trip latency, sync drift and glitch evidence are captured.
4. Route changes, pause/resume and 30-minute stability are tested.
5. CPU, memory, battery and thermal results are reported.
6. Oboe/NDK and any transitive dependencies pass license, ABI, security and maintenance review.
7. A fallback and rollback path is demonstrated.
8. The chosen engine satisfies the feature acceptance criteria and the decision is recorded in ADR-0001.

Until these criteria are met, engine selection remains **OPEN / NOT DECIDED**.
