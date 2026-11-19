# ADR-0001: Android audio engine selection

- **Status:** Proposed — decision deferred pending physical-device evidence
- **Date:** 2026-11-20
- **Owners:** RZ AI Singing Studio
- **Related feature IDs:** Synchronized karaoke recording; live monitoring; recording lifecycle and recovery
- **Related requirements:** [Architecture — synchronized karaoke recording](../ARCHITECTURE.md#42-synchronized-karaoke-recording)
- **Supersedes / superseded by:** None

## 1. Context

The current MVP uses Android `MediaPlayer` for imported-song preview and `MediaRecorder` for microphone-only AAC/M4A recording. It does not yet provide synchronized backing-track playback and microphone capture, PCM mixing, live monitoring, timestamp alignment or route-latency compensation.

A production karaoke workflow requires predictable capture/playback timing, separate preservation of source vocal audio, robust route handling and recoverable recording sessions. Selecting an engine before measuring the user's Android 12 physical baseline would be premature.

## 2. Decision drivers and constraints

- Android 12+ support; minimum API 31 and target API 35.
- Synchronized playback and capture against audio frame/timestamp references.
- Low and repeatable wired monitoring latency.
- Bounded drift and no audible dropouts during long sessions.
- Stable input/output route changes and safe lifecycle recovery.
- CPU, RAM, battery, thermal and package-size impact across device tiers.
- Kotlin integration and maintainable native boundaries.
- ABI support, dependency security and commercial redistribution licenses.
- No cloud upload; local-first capture and preservation of original takes.
- A tested fallback and rollback path.

## 3. Options considered

| Option | Benefits | Costs/risks | License/platform constraints | Evidence |
|---|---|---|---|---|
| Android AudioRecord + AudioTrack | Platform APIs; direct PCM control; Kotlin integration; useful baseline and fallback | Buffer/thread/timestamp/routing behavior must be engineered and measured | Android platform APIs; verify behavior per API/device | Not yet benchmarked on physical target |
| Oboe (AAudio where supported, supported fallback as applicable) | Designed for low-latency Android audio; native callback model; cross-version abstraction | C++/NDK/JNI complexity; ABI and binary-size cost; device/backend behavior still varies | Review exact Oboe version, NDK, transitive components and license before adoption | Not yet benchmarked on physical target |
| Keep MediaPlayer + MediaRecorder | Minimal change; adequate for current MVP preview and mic-only recording | Does not provide the required synchronized PCM engine and precise session timeline | Existing Android APIs | Current source confirms only basic preview and mic-only capture |
| Defer selection | Avoids unsupported claims and premature dependency adoption | Delays production audio-engine implementation | No new dependency | Selected interim position |

## 4. Evidence and experiments

- Source baseline: current `main` application source, `app/build.gradle.kts` and `app/src/main/AndroidManifest.xml`, inspected 2026-11-20.
- Reproducible plan: [Audio Engine Benchmark Preparation](../AUDIO_ENGINE_BENCHMARK_PLAN.md).
- Capability probe: `AudioEngineCapabilityProbeTest` in Android instrumentation tests.
- Required devices: Redmi M2007J22C / API 31; additional API 34 and API 35 physical devices; lower-tier Android 12+ device.
- Required metrics: physical round-trip latency, sync drift, glitches/xruns, route changes, CPU, memory, battery, thermal and recovery.
- Raw benchmark results: **NOT RUN**.
- Physical loopback results: **NOT RUN**.
- Oboe dependency/license review: **NOT RUN**.
- Confidence: insufficient to select a production engine.

## 5. Decision

**Defer the final engine selection.** Use `AudioRecord` + `AudioTrack` as the native benchmark baseline and Oboe as the primary low-latency candidate. Keep the existing MediaPlayer/MediaRecorder implementation unchanged while the prototypes and physical-device tests are prepared.

No production engine, dependency, manifest permission or foreground service is approved by this ADR.

## 6. Consequences

### Positive
- Avoids choosing an engine based on assumptions or emulator-only evidence.
- Establishes comparable measurements and a clear evidence trail.
- Keeps the current MVP stable while R&D proceeds.

### Negative / trade-offs
- Production synchronized recording is delayed until the benchmark and decision are complete.
- Both candidates require prototype and physical-device work.

### Operational and user impact
- Current MVP behavior remains unchanged.
- No new audio is collected or uploaded by the capability probe.

## 7. Fallback and rollback

The probe is test-only and can be removed without changing production behavior. Prototype code must remain isolated from the production UI and recorder. If Oboe is not viable, continue with the native baseline only if it meets the measured acceptance criteria; otherwise defer the feature rather than ship an unreliable path.

## 8. Acceptance and follow-up

- [x] Current source baseline inspected.
- [x] Candidate options and evidence gaps documented.
- [x] Benchmark protocol and provisional metrics defined.
- [ ] Capability probe run on Redmi M2007J22C / API 31.
- [ ] Native AudioRecord/AudioTrack prototype benchmarked.
- [ ] Oboe prototype benchmarked under equivalent conditions.
- [ ] Physical loopback, sync drift and endurance evidence reviewed.
- [ ] License, ABI, security and maintenance review complete.
- [ ] Fallback demonstrated.
- [ ] Final engine decision reviewed and ADR status updated.

## 9. Review triggers

Revisit this ADR when physical benchmark data is available, Android audio API behavior changes materially, supported device tiers change, an Oboe/NDK dependency changes, or the provisional latency/stability targets are revised.
