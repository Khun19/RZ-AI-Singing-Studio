# RZ AI Singing Studio — Architecture

**Status:** Architecture baseline aligned to the user-selected product requirements (v1).  
**Platform:** Standalone Android application, Android 12+ (API 31+).  
**Repository:** `Khun19/RZ-AI-Singing-Studio`  
**Product vision:** [Product Vision & Mission](PRODUCT_VISION_AND_MISSION.md)  
**Product requirements:** [Karaoke & AI Singing Studio Requirements](KARAOKE_AI_SINGING_STUDIO_REQUIREMENTS.md)

> This document describes the intended system architecture. A module, interface, or data flow described here is not evidence that it has been implemented. See [Implementation status](#implementation-status) and verify feature-specific tests before claiming functionality.

## 1. Product and system boundaries

RZ AI Singing Studio is a standalone Android karaoke, vocal-recording and AI cover-arrangement studio. It is independent of RZ Builder / Universal-ZIP-to-APK-Builder.

The application must:
- Support Android 12 (API 31) and later, with capability-aware behavior across device tiers.
- Prefer offline/on-device processing. Optional cloud processing requires task-level explicit consent, disclosure of uploaded data and expected charges, and a local alternative wherever feasible.
- Keep recordings and project data local by default; do not perform automatic cloud backup.
- Preserve the user's natural vocal identity and original recordings.
- Process only music and assets the user is authorized to use.
- Use models, libraries, datasets and assets only after license, security, privacy and commercial-distribution review.
- Expose honest capability and quality limits; do not claim universal device support, zero Bluetooth latency or unsupported export quality.

## 2. Architectural principles

1. **Modular boundaries:** playback/recording, lyrics, separation, vocal processing, cover generation, production, export and library are independently testable.
2. **Ports and adapters:** UI and workflows depend on stable domain interfaces, not directly on a particular DSP library, AI model or cloud vendor.
3. **Local-first data:** source audio, takes, drafts, generated versions and final exports remain in app-managed local storage unless the user explicitly exports or consents to a cloud task.
4. **Non-destructive processing:** retain original takes and source assets; generated/fixed versions are separate artifacts with settings and lineage.
5. **Capability-aware execution:** choose local model/quality paths based on API, ABI, RAM, compute, thermal and storage capability; fail gracefully on unsupported devices.
6. **Evidence-driven status:** source presence, CI, APK validation, device acceptance and feature acceptance are separate states.
7. **No premature technology lock-in:** engine, codecs, model runtimes and providers remain replaceable until benchmarks and license reviews support a decision.

## 3. High-level component architecture

| Component | Responsibility | Status |
|---|---|---|
| Android UI / presentation | Studio screens, navigation, themes, accessibility and user controls | Initial Compose app exists; full studio UI planned |
| Project & workflow coordinator | Song session, recording workflow, processing jobs and state transitions | Planned |
| Audio import & metadata | SAF-based local import, permission-aware online sources, metadata | Basic local SAF import exists |
| Playback engine | Backing-track decode, transport, seek and playback state | Basic MediaPlayer preview exists |
| Audio engine | PCM capture/output, synchronized backing + microphone, routing, monitoring and track alignment | Planned; technology selection pending benchmarks |
| Recording manager | One-take, multi-take, punch-in, take lifecycle and autosave | Basic mic-only recording exists; advanced workflows planned |
| Lyrics pipeline | Licensed/authorized lyric lookup, speech recognition, line segmentation, timing and uncertainty labels | Planned |
| Vocal separation | Vocal/instrumental separation for karaoke backing creation | Planned; distinct from cover generation |
| Vocal processing | Denoise, level/clarity, restrained pitch correction, EQ, compression, reverb and echo | Planned |
| Cover arrangement | New instrument arrangement/re-performance, style, tempo and key controls | Planned; distinct from stem extraction |
| Producer | AI mix recommendations, Auto Mix, mastering, A/B and per-change acceptance | Planned |
| Performance analysis | Non-interrupting pitch/timing/rhythm/volume/voice-quality analysis and history | Planned |
| Export | Audio-only MP3, AAC and WAV output with truthful capability-gated quality controls | Planned |
| Local library & storage | Projects, takes, drafts, versions, artwork, folders, storage and sharing | Basic take list/delete exists; full library planned |
| Cloud processing adapter | Optional consent-gated remote inference with cost/retention disclosure | Not implemented; provider not selected |

### Dependency direction

- Presentation calls application use cases.
- Use cases coordinate domain workflows and depend on interfaces (ports).
- Infrastructure adapters implement storage, media playback/codec, audio I/O, DSP, AI inference and optional cloud providers.
- Domain models and acceptance rules must not depend on Android UI classes or a specific AI vendor.
- Long-running processing must expose progress, cancellation, failure and recoverable state.

## 4. Core data and audio flows

### 4.1 Import and karaoke playback

1. User selects a local audio document through Android Storage Access Framework.
2. Import adapter validates readability and supported media format, then stores a durable app reference or managed copy as appropriate.
3. Metadata and playback preparation are performed without modifying the source.
4. Playback engine decodes the selected backing track and exposes transport/timing information.
5. Lyrics pipeline independently resolves authorized lyrics and timing; uncertain AI-inferred words are marked as uncertain.
6. UI displays previous/current/next lyric lines and highlights the current line.

Online links and catalog integrations are allowed only where provider terms and copyright permissions permit. The product must not promise that every YouTube URL can be downloaded or processed.

### 4.2 Synchronized karaoke recording

Target flow:
1. Load and prepare backing-track audio.
2. Prepare microphone capture and determine the active input/output route.
3. Start playback and capture against a shared monotonic timing/frame reference.
4. Record microphone and backing audio as logically separate tracks or source assets.
5. Track frame positions/timestamps and route latency; do not rely only on wall-clock UI timers.
6. Align tracks after capture where possible and preserve the original microphone take.
7. Allow monitoring only under the selected route policy; headphones enable monitoring by default, while speaker/no-headphone routes reduce echo/feedback risk.
8. Save a recoverable session and create a mixed preview/export through a non-destructive processing step.

Bluetooth latency varies by device, codec and route. Estimate/compensate where possible and expose manual adjustment; do not guarantee zero latency. Final track alignment is distinct from the live monitoring delay.

**Engine decision is not final.** Benchmark native Android audio APIs (AudioRecord/AudioTrack and supported low-latency paths) against Oboe on representative API 31+ devices and routes. Compare round-trip latency, synchronization drift, glitches, CPU, battery, thermal behavior, route changes and recovery. Select an implementation only after reproducible measurements. Keep a compatible fallback for devices/routes where the preferred path is unavailable.

### 4.3 Recording lifecycle and recovery

Recording manager owns explicit states such as idle, preparing, recording, paused, interrupted, saving, recoverable-draft and completed. State transitions must be testable.

- Support One-Take, Multi-Take and Punch-In as separate workflows over the same capture/session primitives.
- Use Android-compliant microphone foreground service and visible notification for background/screen-off recording.
- On incoming calls, pause safely and preserve the take; resume only after explicit user action.
- Periodically persist session metadata and recoverable audio segments.
- On process death/restart, offer recovery when data is intact. If segment merge fails, preserve the original segments.
- Save automatically on stop. Distinguish source takes, unfinished drafts, processed versions and final exports.
- Never delete originals, drafts or final files without user confirmation.

### 4.4 Lyrics pipeline

Lyrics resolution is a staged pipeline: authorized source lookup → audio speech recognition when permitted → AI inference for missing segments → line segmentation → timing/alignment → verification and confidence labeling. Burmese recognition, spelling and line segmentation are priority requirements. Guessed text must never be displayed as verified lyrics. Manual timing correction remains available.

### 4.5 Vocal separation and AI cover arrangement

These are separate bounded contexts:

- **Vocal separation** separates vocal and instrumental content from an existing source to create a karaoke backing track.
- **AI cover arrangement** creates a new arrangement or re-performs instruments (for example, guitar or piano) based on the source song. It does not mean extracting those instrument stems from the original.

Cover workflow supports Original Melody Preservation and AI Creative Cover modes, manual instrument selection, selected cover styles, tempo and key controls, whole-song generation, Standard/High/Studio quality targets, final-result preview and full local version history. Actual instrument/model support depends on feasibility, device capability and license review.

### 4.6 Vocal processing and production

Processing stages are non-destructive and independently testable:
- Noise reduction, automatic level control and clarity enhancement.
- Restrained pitch correction that preserves natural timbre.
- Natural reverb, soft echo, smart EQ and dynamic compression.
- AI-assisted balance, timing alignment where possible, peak/clipping control, Auto Mix and Auto Mastering.
- Preview and A/B comparison; allow per-change accept/reject and Undo/Redo while preserving the original take.

**Live enhancement and monitoring path:** microphone capture feeds a bounded, low-latency processing chain before the monitoring output. The capture/audio callback must do only predictable, deadline-bounded work: no disk/network I/O, blocking locks, UI work, unbounded allocations or model inference with unknown latency. Expensive AI analysis runs off the real-time callback; it may publish parameters or frames to the audio engine through bounded, thread-safe handoff. The audio engine applies only validated parameters at safe buffer boundaries. The live chain must cover the approved live requirements (noise reduction, automatic level control, clarity enhancement and restrained pitch correction) where device benchmarks prove the latency and stability budget. Post-recording enhancement is a separate path and does not satisfy the live requirement by itself.

When a device or model cannot meet the measured real-time budget, bypass the unsupported live effect or use a tested lighter DSP fallback while keeping recording stable; disclose the limitation and retain the unprocessed original. Never let an AI stall or monitoring route failure interrupt microphone capture. Define and measure callback deadline misses, end-to-end monitoring latency, glitches, CPU, memory, battery and thermal behavior on representative API 31+ devices before accepting a live effect.

Live singing feedback must not interrupt capture. Analyze pitch, timing, rhythm, volume and voice quality asynchronously or through bounded real-time work that cannot block the audio callback.

### 4.7 Export and local library

- Export audio only; karaoke video export is out of scope.
- Target formats are **MP3, AAC and WAV**. FLAC and stem export are not in the approved v1 export scope.
- Expose bitrate, sample rate, bit depth and channel controls only when the selected encoder and processing path support them.
- Studio output may reach 24-bit/48 kHz only when the source and processing path genuinely support it. Never fake quality through upsampling.
- Keep source, take, draft, generated version and final export lineage/settings.
- Support local organization by song, artist, date, album, genre, recording type and cover style; user-created folders; song-title search only.
- Duplicate-audio detection is not in the selected scope.
- Artwork may be AI-generated, user-provided or selected/edited from the gallery, subject to format metadata support and asset rights.
- Provide storage usage and low-space warnings. Cleanup may remove disposable temporary files only; user content requires confirmation.
- Sharing uses supported Android targets. No automatic cloud backup, PIN vault or biometric vault is in scope.

## 5. Local and cloud processing policy

### Local path
Prefer on-device processing where feasible. Select model/runtime variants based on API/ABI, RAM, CPU/GPU/NPU, model size, battery, thermal limits, latency and available storage. Low-capability devices must receive a clear fallback or unsupported-state explanation rather than a misleading quality claim.

### Optional cloud path
Cloud processing is not a default fallback and must not start silently. Before each task:
- Explain which audio or other data will be uploaded and why.
- Obtain explicit task-level consent.
- Disclose expected charges before processing.
- Explain provider retention/logging limits and deletion behavior.
- Offer a local alternative wherever feasible and describe quality/speed differences.

No provider, model or pricing assumption is final until reviewed. Open-source licensing does not automatically grant commercial redistribution rights or remove hosting/GPU/bandwidth costs.

## 6. Android platform and security considerations

- Minimum supported OS: Android 12 / API 31.
- Validate API 31 and API 35 at minimum, plus representative device capability tiers and audio routes.
- Request microphone and media/document access only when needed and explain the purpose.
- Background microphone capture must comply with the target SDK's foreground-service type, permission, notification and background-start restrictions.
- Treat imported media, metadata, lyrics, model files and provider responses as untrusted input.
- Use scoped app storage/SAF; avoid broad storage permissions unless a separately justified platform requirement exists.
- Keep secrets and provider credentials out of the APK and repository.
- Do not upload audio or telemetry without the required consent and disclosure.
- Review dependencies, model weights, datasets, artwork and music rights before distribution.

## 7. Implementation status

This status reflects the known early MVP baseline, not a live claim about the latest repository build. Re-check source and exact-commit test evidence before changing a status.

| Capability | Status | Evidence/qualification |
|---|---|---|
| Android Kotlin/Jetpack Compose app shell | Implemented in MVP; full studio UI not verified | Existing Android application source |
| Local audio selection via SAF | Implemented in MVP | Existing import path; verify on supported API/device |
| Imported-song preview | Implemented in MVP; dedicated preview acceptance not yet verified | MediaPlayer play/pause path exists; current playback instrumentation tests recorded-take playback, not imported-song preview. The current imported-song preview has no verified seek control. |
| Recorded-take playback/seek/pause | Implemented in MVP | PlaybackFlowInstrumentationTest exercises a seeded recorded M4A take, including playback, seek, pause/resume and completion |
| Microphone AAC/M4A recording | Implemented in MVP | MediaRecorder captures mic only |
| Local take list and deletion | Implemented in MVP | TakeStore/local storage tests |
| Synchronized backing + mic recording/mixing | Not implemented | Requires audio-engine design and feature acceptance |
| Lyrics and Myanmar lyric timing | Not implemented | No accepted pipeline yet |
| Vocal separation | Not implemented | Model/runtime/license/quality work pending |
| AI cover arrangement/instrument generation | Not implemented | Separate R&D and license review pending |
| Vocal effects, Auto Mix, Auto Mastering | Not implemented | DSP/model and acceptance work pending |
| Live monitoring/Bluetooth compensation | Not implemented | Route/latency benchmarks pending |
| Background recording/call handling/recovery | Not implemented | Foreground service and lifecycle work pending |
| Performance scoring/history | Not implemented | Analysis pipeline pending |
| MP3/AAC/WAV export controls | Not implemented | Current mic recording file is not the full export workflow |
| Full local library, artwork, folders and sharing | Not implemented | Current take list is only a minimal subset |
| Optional cloud AI | Not implemented | Provider and privacy/cost review pending |

“Implemented in MVP” does not mean every device/API combination has passed physical acceptance. A source file or passing CI job alone does not prove end-to-end feature acceptance.

## 8. Verification and requirements traceability

Every feature must have a traceable requirement, test plan and evidence record. At minimum:

| Requirement area | Required verification |
|---|---|
| Import and imported-song preview | SAF permission/persistence, supported formats, selected-source preview start/pause/resume, completion, error handling and route changes; test preview separately from recorded-take playback and do not claim seek until implemented and tested |
| Recorded-take playback | Recorded M4A playback, seek, pause/resume, completion, persistence after activity recreation and deletion; identify the exact take fixture and test artifact |
| Synchronized recording | Track start alignment, drift over full-song duration, dropped frames/glitches, final alignment, mixed output and repeatability |
| Recording lifecycle | One-take/multi-take/punch-in, background/screen-off, call interruption, manual resume, process death, recovery and low-storage behavior |
| Lyrics | Source/license checks, Burmese recognition quality, line segmentation, timing error, uncertainty labels and manual correction |
| Vocal separation | Licensed model provenance, vocal/instrumental quality, artifacts, runtime, memory, latency and low-tier fallback |
| Cover generation | New arrangement rather than stem extraction, melody/structure preservation mode, instrument/style controls, quality and rights review |
| Vocal processing/producer | Naturalness, clipping/peak behavior, A/B output, per-change acceptance, Undo/Redo and original preservation; separately verify real-time effects on the live monitoring path and post-recording effects |
| Live enhancement/audio engine | Callback deadline misses, processing-buffer underruns, end-to-end monitoring latency, capture continuity, effect bypass/fallback, CPU/memory/battery/thermal behavior and voice quality across representative device tiers |
| Monitoring | Wired and Bluetooth route detection, latency, feedback risk, route switching and manual offset |
| Singing feedback and performance scoring | Non-interrupting feedback; overall, pitch, timing, rhythm and voice-quality scores; score consistency/repeatability; history comparison; actionable tips; handling of low-confidence or unavailable analysis |
| Export | Format/codec validity, metadata, sample/bit-depth truthfulness, no fake upsampling and audio playback in external players |
| Library/storage | Persistence, folder/move/rename, title search, sharing, low-space warning and confirmed deletion |
| Cloud path | Consent before upload, accurate data/cost/retention disclosure, cancellation, failure behavior and local alternative |

Test layers:
1. Domain/unit tests for state machines, timing calculations, metadata, policy and project persistence.
2. Android integration tests for media, permissions, foreground service, storage and lifecycle.
3. Emulator tests on API 31 and API 35 for exact APK install/launch and supported workflows.
4. Physical-device tests across representative capability tiers and audio routes, especially latency, Bluetooth, thermal behavior, interruptions and recovery.
5. Feature-specific acceptance tests with reproducible evidence and exact source/APK identity.

Keep these gates separate:
- Source and CI checks passed.
- APK built and structurally validated.
- Exact APK installed and launched on emulator.
- Exact APK accepted on physical device.
- Feature-specific acceptance passed.

Use **Root Cause → Fix → Test → Evidence**. A CI pass does not imply APK validation, provenance validation, device acceptance or feature acceptance.

## 9. Technology decision records

Do not treat a candidate as selected until evidence is recorded.

| Decision | Current position | Required decision evidence |
|---|---|---|
| Audio I/O engine | Open; compare Android native APIs and Oboe | API 31+ device benchmarks, route coverage, drift/glitch/latency, CPU/battery/thermal and fallback |
| Media decoding/encoding | Use Android-supported codecs initially where suitable; abstraction required | Format support matrix, codec behavior, quality and licensing |
| DSP/effects | Open | License, performance, audio quality, ABI support and maintainability |
| AI inference runtime/models | Open | Commercial-use license, model provenance, size, memory, quality, speed and offline behavior |
| Vocal separation model | Open | Rights, separation quality, artifacts, device-tier performance |
| Cover generation models | Open; separate R&D workstream | Music rights, training/data provenance, instrument quality, compute/cost and device/cloud options |
| Cloud provider | Not selected | Privacy/retention, consent APIs, cost, region, reliability and deletion terms |

Record each final decision as an ADR with date, alternatives, evidence, trade-offs and rollback/fallback plan.

## 10. Delivery sequence

1. Audit the current MVP, repository/PR state, CI and exact-APK evidence.
2. Keep this architecture aligned with the approved requirements and maintain feature-to-test traceability.
3. Design and benchmark the synchronized audio engine; prove backing playback plus microphone capture and alignment before adding AI.
4. Implement recording lifecycle, foreground service, call interruption, autosave, drafts and recovery.
5. Research and benchmark vocal separation and on-device inference candidates.
6. Add lyrics, vocal processing, monitoring, mixing and mastering incrementally.
7. Research AI cover arrangement/instrument generation as a distinct, compute-intensive workstream.
8. Complete export, library, artwork and UI capabilities.
9. Validate exact APKs on API 31 and API 35 emulators, then perform physical-device acceptance.

Do not skip a failed acceptance stage to begin dependent work. Do not mark a feature complete until its own acceptance criteria and evidence pass.

## 11. Explicit scope exclusions and constraints

- No dependency on RZ Builder or arbitrary ZIP-to-APK conversion.
- No karaoke video export.
- No FLAC or stem export in the selected v1 export scope.
- No duplicate-audio detection requirement.
- No singing-training curriculum/exercise system.
- No recording touch-lock.
- No PIN/biometric vault.
- No automatic cloud backup.
- No silent cloud upload or mandatory cloud processing.
- No claim that every online video/music URL can be imported.
- No guarantee of zero Bluetooth latency or full offline AI parity on every Android device.

## 12. License

The project is distributed under Apache License 2.0. See [LICENSE](../LICENSE).
