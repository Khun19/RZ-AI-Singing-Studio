# ADR-0001: Android audio engine selection

- **Status:** Accepted for implementation; primary physical-device test user-confirmed PASS; detailed metric evidence pending
- **Date:** 2026-11-20
- **Owners:** RZ AI Singing Studio
- **Related feature IDs:** Synchronized karaoke recording; live monitoring; recording lifecycle and recovery
- **Related requirements:** [Architecture — synchronized karaoke recording](../ARCHITECTURE.md#42-synchronized-karaoke-recording)
- **Supersedes / superseded by:** Supersedes the deferred decision recorded in the research draft

## 1. Context

The MVP currently uses Android MediaPlayer for imported-song preview and MediaRecorder for microphone-only AAC/M4A recording. It does not yet provide the complete synchronized backing-track and microphone recording workflow.

The user has selected Oboe as the engine family, preferring AAudio on the supported Android target while retaining OpenSL ES as a legacy compatibility/benchmark backend to evaluate. This is an implementation decision, not a claim that the Redmi device has passed production latency or stability acceptance.

## 2. Decision drivers and constraints

- Android 12+ support; minimum API 31 and target API 35.
- Full-duplex capture/playback with a shared audio-frame timing model.
- Low and repeatable wired monitoring latency.
- Bounded drift and no audible dropouts during long sessions.
- Safe route changes, stream disconnection handling and lifecycle recovery.
- CPU, RAM, battery, thermal and package-size impact.
- Kotlin/Compose integration with a narrow JNI boundary.
- ABI support, dependency security and commercial redistribution licenses.
- No cloud upload; local-first capture and preservation of original takes.
- Native AudioRecord/AudioTrack remains a benchmark baseline.

## 3. Options considered

| Option | Benefits | Costs/risks | License/platform constraints | Evidence |
|---|---|---|---|---|
| Oboe | Native callback model, full-duplex helper, AAudio/OpenSL ES abstraction, device quirks handling | C++/NDK/JNI complexity; route and backend behavior still require physical testing | Oboe 1.11.0; Apache-2.0; verify packaged artifact and ABI in CI | User-confirmed physical test PASS; detailed benchmark metrics pending |
| Android AudioRecord + AudioTrack | Platform APIs; direct PCM control; Kotlin integration; benchmark reference | Thread, buffering, synchronization and route behavior must be engineered | Android platform APIs | Existing Termux system-level tests only; app-level prototype NOT RUN |
| MediaPlayer + MediaRecorder | Minimal MVP path | Does not provide synchronized PCM engine and precise session timeline | Existing Android APIs | Existing MVP source |

## 4. Evidence and experiments

- Device baseline: Redmi M2007J22C, Android 12 / API 31, MediaTek MT6853.
- Termux system-level evidence: simultaneous mic capture and speaker playback observed; this does not prove app-level synchronization or latency.
- Oboe upstream release: https://github.com/google/oboe/releases/tag/1.11.0.
- Integration: com.google.oboe:oboe:1.11.0, Prefab/CMake, C++17 and JNI.
- Production stream policy: request AudioApi::Unspecified; record the actual backend after opening. Oboe normally selects AAudio when recommended and may select OpenSL ES otherwise.
- Explicit OpenSL ES selection is diagnostic/compatibility-only; it is deprecated on modern Android and is not assumed to be a reliable fallback.
- Current implementation: native full-duplex capture/output engine foundation with preallocated SPSC PCM queues, optional monitoring, playback PCM queue, capture PCM drain API and stream diagnostics.
- User confirmed the Oboe physical-device test passed. Detailed raw benchmark evidence (latency percentiles, drift, xrun counts, route matrix and endurance logs) has not been supplied.
- Physical loopback, sync drift and endurance numeric results: NOT independently inspected; user has confirmed the physical-device test passed.
- Exact APK acceptance: NOT RUN. Primary physical-device test: user-confirmed PASS; raw metric evidence pending.

## 5. Decision

**Select Oboe for the production audio-engine implementation.**

- Production requests Oboe AudioApi::Unspecified so Oboe selects the backend recommended for the current Android device/API. The actual input/output backend must be logged and checked.
- AAudio is the preferred expected backend on supported modern Android devices.
- OpenSL ES remains available for explicit compatibility benchmarking and only for a separately justified legacy case. Do not force it as the default on API 31+.
- Native AudioRecord/AudioTrack remains the comparison baseline.
- Start with 48 kHz Float PCM, mono input and stereo output. Query and report actual stream properties rather than assuming the requested values were accepted.
- Request Exclusive low-latency streams first and retry Shared when Exclusive open fails.
- Keep live monitoring disabled by default to avoid speaker feedback; enable only through an explicit user action and route policy.
- Keep disk I/O, encoding, logging, locks, JNI and allocations out of real-time callbacks.
- Do not replace the existing user-facing recorder until capture persistence, synchronization, recovery and physical acceptance are complete.

This decision authorizes implementation, but does not waive the physical-device release gates.

## 6. Consequences

### Positive
- Establishes a single native engine boundary for synchronized capture/playback and future DSP.
- Uses Oboe's stream abstraction and full-duplex synchronization helper.
- Keeps the engine replaceable behind a Kotlin API and preserves native baseline comparisons.

### Negative / trade-offs
- Adds C++, NDK, CMake, Prefab and ABI build complexity.
- Oboe backend selection does not guarantee low latency on every device or route.
- PCM queues and current engine foundation do not yet provide a durable AAC/M4A recording workflow or decoded backing-track integration.

### Operational and user impact
- Existing MVP recording behavior remains unchanged until the new engine passes acceptance.
- No audio is uploaded by the native engine.
- Monitoring remains off unless explicitly enabled.

## 7. Fallback and rollback

- If Oboe stream initialization fails, report the actual error and do not claim the engine is running.
- Retry Shared mode after Exclusive open failure.
- Use native AudioRecord/AudioTrack only as a separately benchmarked alternative; do not silently switch engines without recording the active implementation.
- Preserve the current MediaRecorder MVP path until Oboe recording persistence and device acceptance pass.
- Roll back the native integration by reverting the feature branch; existing takes and app data remain untouched.

## 8. Acceptance and follow-up

- [x] User selected Oboe for implementation.
- [x] Oboe 1.11.0 and Apache-2.0 source release identified.
- [x] Native engine foundation and JNI boundary added on feature branch.
- [ ] Exact-head Android CI, native compilation and APK validation pass.
- [ ] Native engine instrumentation smoke tests pass.
- [x] Primary physical-device test: user-confirmed PASS; detailed backend/stream diagnostics remain to be attached.
- [ ] Physical full-duplex capture/playback and wired loopback measurement records attached.
- [ ] Drift, xruns, route changes and 30-minute endurance records attached and reviewed.
- [ ] PCM-to-file recording, crash recovery and synchronized backing decoder integration accepted.
- [ ] Feature Traceability Matrix and implementation status updated with exact evidence.

## 9. Review triggers

Revisit this ADR if Oboe's backend behavior, Android audio policy, supported API/ABI range, license terms, physical benchmark results or device support tiers change materially.
