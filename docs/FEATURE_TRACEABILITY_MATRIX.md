# RZ AI Singing Studio — Feature Traceability Matrix

**Baseline:** User-selected v1 Requirements  
**Authority:** [Product Vision & Mission](PRODUCT_VISION_AND_MISSION.md) → [Requirements](KARAOKE_AI_SINGING_STUDIO_REQUIREMENTS.md) → [Architecture](ARCHITECTURE.md)  
**Purpose:** Prevent approved features, exclusions, acceptance criteria or implementation status from being lost during delivery.

> A row marked implemented in the MVP is not necessarily accepted. A planned row is not implemented. Test IDs below define required coverage; they are not claims that those tests already exist.

## 1. Status rules

| Status | Meaning |
|---|---|
| Implemented in MVP — not fully accepted | Source code exists for a limited MVP slice; feature-specific acceptance is incomplete. |
| In progress | Work is active in a branch/PR and not yet accepted. |
| Planned — not implemented | Approved scope exists, but no accepted implementation exists. |
| Experimental | A time-boxed feasibility experiment only; not a product commitment beyond the approved requirement. |
| Verified working | Feature-specific acceptance criteria passed on the declared API/device/route matrix with exact source and artifact evidence. |
| Blocked | A documented technical, legal, licensing, privacy, security or platform constraint prevents progress pending a decision. |

Only the final **Verified working** status means feature acceptance. CI pass, source presence, APK install/launch, or a demo screenshot alone cannot promote a feature to that status.

## 2. Approved feature map

| ID | Requirement area | Approved user-visible outcome | Current status | Required acceptance evidence |
|---|---|---|---|---|
| APP-001 | Platform and app shell | Standalone Android app, Android 12+ (API 31+), professional studio UI | Implemented in MVP — not fully accepted | API 31/35 install and launch; navigation and layout checks; representative physical-device compatibility |
| AUD-001 | Local audio import | Import user-selected local audio through supported Android document access | Implemented in MVP — not fully accepted | SAF permission and persisted access, supported format matrix, revoked/missing URI handling, large-file and error cases |
| AUD-002 | Online sources/catalog | Permitted online sources and licensed Myanmar music catalog only where provider terms and rights allow | Planned — not implemented | Provider terms, copyright and catalog rights; consent, availability, offline download behavior and failure handling |
| PLAY-001 | Imported-song preview | Preview selected song with reliable playback controls | Implemented in MVP — dedicated acceptance not verified | Test imported-source preview separately from take playback; start, pause/resume, completion, route change and decode errors. Do not claim seek until implemented and tested |
| TAKE-001 | Microphone capture | Record a microphone-only AAC/M4A take | Implemented in MVP — not fully accepted | Permission denial/grant, start/stop, valid codec/container, file integrity, interruption and physical-device checks |
| KARA-001 | Synchronized karaoke recording | Play backing track and capture microphone as aligned tracks; produce a correctly aligned mix | Planned — not implemented | Start offset, drift over full-song duration, dropped buffers/glitches, repeatability, mixed output and audio/video-independent playback validation |
| REC-002 | Recording modes | One-Take, Multi-Take and Punch-In | Planned — not implemented | Mode-specific session transitions, segment boundaries, take selection, punch range accuracy and non-destructive original preservation |
| REC-003 | Live voice enhancement | During singing, provide natural noise reduction, auto level, clarity and restrained pitch correction | Planned — not implemented | Live monitoring path, callback deadlines, latency, glitches, voice naturalness, bypass/fallback and capture continuity |
| MON-001 | Smart monitoring | Headphones enable monitoring; without headphones, reduce echo/feedback risk | Planned — not implemented | Wired/headphone route detection, speaker feedback-risk behavior, manual override, route changes and safe default |
| MON-002 | Bluetooth monitoring | Detect Bluetooth route, estimate/compensate latency and allow manual adjustment | Planned — not implemented | Measure route-specific latency and drift; reconnect/route-switch behavior; disclose residual latency; never claim zero latency |
| LIFE-001 | Background recording | Continue recording with screen off using compliant Android foreground microphone service | Planned — not implemented | API 31+ permission/service behavior, visible notification, background-start restrictions, screen-off continuity and process lifecycle |
| LIFE-002 | Call interruption | Automatically pause and preserve take on incoming call; require manual resume | Planned — not implemented | Simulated call/audio-focus interruption, saved segment integrity, no automatic resume, manual resume and repeated interruptions |
| LIFE-003 | Autosave and recovery | Segment autosave, recover drafts after crash/restart, preserve segments if merge fails | Planned — not implemented | Forced process death/restart, partial/corrupt segment handling, merge failure, low storage, draft continuation and original preservation |
| LYR-001 | Lyrics retrieval | Retrieve lyrics from authorized sources when available | Planned — not implemented | Source provenance and license, language coverage, missing/ambiguous result handling and offline behavior |
| LYR-002 | Myanmar lyrics recognition | Recognize/infer missing words with confidence and uncertainty labels; prioritize Burmese spelling and segmentation | Planned — not implemented | Burmese evaluation corpus with rights review, word/line accuracy, confidence calibration, uncertainty display and manual correction |
| LYR-003 | Synchronized lyrics | Previous/current/next lines, current-line highlight, automatic timing and manual adjustment | Planned — not implemented | Timing error against reference, line transitions, seek synchronization, manual offset persistence and long-song drift |
| VOCSEP-001 | Vocal separation | Separate vocal from an existing song to create karaoke backing | Planned — not implemented | Model/data license and provenance, vocal leakage, instrumental artifacts, quality, runtime, memory, thermal and low-tier fallback |
| POST-001 | AI post-recording fixes | Suggestions, automatic fix with A/B, per-change accept/reject and Undo/Redo | Planned — not implemented | Original preservation, A/B level-matched comparison, per-change state, undo/redo correctness and deterministic project history |
| FX-001 | Natural vocal effects | Reverb, soft echo, smart EQ and dynamic compression that preserve natural voice | Planned — not implemented | Listening evaluation, clipping/peak behavior, bypass, parameter persistence, latency and device performance |
| MIX-001 | Auto Mix and Auto Mastering | Balance vocal/music, align where possible, control peaks and master the final audio | Planned — not implemented | Loudness/peak targets declared in advance, clipping, alignment, codec validity, A/B preview and repeatable output |
| COACH-001 | Singing feedback and scoring | Non-interruptive pitch/timing/rhythm/volume/voice-quality feedback, overall and component scores, history and tips | Planned — not implemented | Feedback does not interrupt capture; score definitions and repeatability, history comparison, actionable tips and low-confidence handling |
| COVER-001 | AI cover modes | Original Melody Preservation and AI Creative Cover create a new arrangement/re-performance | Planned — not implemented | Prove new arrangement rather than stem extraction; melody/chord/tempo/structure behavior, rights and model provenance |
| COVER-002 | Cover controls | User-selected instruments, styles, tempo/BPM and key controls | Planned — not implemented | Every exposed option changes the generated result as specified; unsupported instrument/device cases disclosed |
| COVER-003 | Whole-song cover generation | Whole-song generation, Standard/High/Studio quality, final-only preview and local version history | Planned — not implemented | Full-song continuity, quality truthfulness, regeneration, settings lineage, playback and version deletion |
| EXP-001 | Audio export | Audio-only MP3/AAC/WAV with only supported bitrate/sample/bit-depth/channel controls | Planned — not implemented | External-player validity, codec metadata, sample/bit-depth truth, metadata/artwork compatibility and no fake upsampling |
| LIB-001 | Local project library | Organize by song/artist/date/album/genre/type/style; folders; title-only search | Implemented in MVP — limited take list only | Persistence, sort/filter, folder move, title-only search, artwork, metadata and local-only behavior |
| LIB-002 | File operations and sharing | Rename, play, trim, duplicate file, delete/restore where supported, storage warning, safe cleanup and sharing | Implemented in MVP — deletion only; remaining operations planned | Confirmed deletion, restore behavior, trim integrity, duplicate-file behavior, share targets, low-space warnings and no silent deletion |
| UI-001 | Interface and accessibility | Burmese/English, Dark Studio/Light Studio/Silver Chrome themes, rotation and manual orientation lock | Implemented in MVP — basic dark Compose shell only | Localization completeness, theme contrast, orientation changes, screen-size layout and accessibility semantics |
| PRIV-001 | Privacy and cloud consent | Offline-first; optional cloud AI only after explicit task-level consent and cost/data/retention disclosure | Planned — not implemented | No network upload before consent; cancellation, provider retention disclosure, deletion behavior, local alternative and no automatic cloud backup |
| RIGHTS-001 | Copyright and licensing | Use only authorized music and appropriately licensed models, datasets, libraries and assets | Policy defined; implementation review required | Per-provider/model/asset license inventory, commercial redistribution review, attribution/notice obligations and provenance |
| REL-001 | Build, provenance and release | Reproducible CI, exact APK validation, traceable artifact provenance and separately accepted physical-device release | Partially implemented — release gate incomplete | Exact head SHA, workflow/run/artifact IDs, APK SHA-256, package/SDK/signature, API 31/35 install-launch, physical-device evidence and release signing |

## 3. Test coverage map

These are stable test-plan identifiers. Link each ID to actual test files, CI job names and evidence artifacts as implementation is added.

| Test ID | Coverage | Minimum test layer |
|---|---|---|
| T-PLAT | Install/launch, API and layout compatibility | CI emulator + physical device |
| T-IMPORT | SAF import, permissions, URI persistence and format errors | Unit + Android integration + physical |
| T-PREVIEW | Imported-song preview behavior | Android instrumentation + physical audio route |
| T-TAKE | Microphone-only capture and valid AAC/M4A output | Android integration + physical |
| T-SYNC | Backing/microphone synchronization, drift, glitches and mix | Audio-engine integration + long-duration physical test |
| T-MODES | One-Take, Multi-Take and Punch-In state/segment behavior | Unit + Android integration |
| T-LIVE | Real-time DSP deadline, latency, fallback and voice quality | Audio benchmark + physical device |
| T-MONITOR | Headphone/speaker/Bluetooth route and feedback risk | Physical device and route matrix |
| T-LIFECYCLE | Background service, call pause, autosave, process death and recovery | Android integration + physical |
| T-LYRICS | Source rights, Burmese recognition, confidence, segmentation and timing | Evaluation corpus + integration |
| T-SEPARATION | Separation quality, artifacts, runtime, memory, license and fallback | Model evaluation + device benchmark |
| T-POST | A/B, per-change accept/reject, Undo/Redo and original preservation | Unit + integration |
| T-COACH | Feedback continuity, component scores, history and tips | Unit + evaluation + integration |
| T-COVER | New arrangement, controls, whole-song continuity, quality and rights | Model evaluation + device/cloud validation |
| T-MIX | Mix/master peak, loudness, alignment and preview | Audio analysis + listening evaluation |
| T-EXPORT | Codec/container, metadata, truthful format and external playback | Unit + integration + external player |
| T-LIBRARY | Persistence, organization, file operations, sharing and storage safety | Unit + Android integration |
| T-PRIVACY | Consent, upload boundary, cancellation, cost/retention disclosure | Unit + network inspection + provider review |
| T-RELEASE | Exact-source APK identity, install/launch, signature and physical acceptance | CI + artifact audit + physical device |

## 4. Current repository evidence snapshot

This snapshot reflects the source inspected during the documentation audit. Re-fetch source and test artifacts before changing a status.

| Capability | Observed evidence | Limitation |
|---|---|---|
| App shell | Kotlin + Jetpack Compose MainActivity | MainActivity currently owns most UI, player, recorder and storage behavior; modular boundaries are architectural direction, not implemented |
| Local import | Android OpenDocument picker and persisted read permission attempt | Imported-source preview has no dedicated instrumentation acceptance yet |
| Preview | MediaPlayer selected-source play/pause path | No verified selected-source seek workflow |
| Recording | MediaRecorder microphone AAC in MP4/M4A container | Mic-only; no backing-track synchronization or mixing |
| Takes | Local private recordings directory, list and delete; TakeStore path-boundary test | No full library, draft model, rename/trim/restore or recovery |
| Instrumentation | Recorded-take playback/seek/pause/resume/completion and activity recreation checks; smoke test | Recorded-take playback tests do not prove imported-song preview |
| CI | JVM tests, API 35 instrumentation, debug APK build, exact APK install/launch validation on API 31 and API 35 | API 31 functional instrumentation suite is not currently present; API 31 currently receives exact APK install/launch validation |
| APK evidence | Validator writes badging, signature, SHA-256, size, emulator API, install path, launch result, screenshot and crash log | A single provenance manifest binding source SHA, workflow run, artifact ID and APK SHA is not yet produced |
| Release | Debug APK only | Release signing and physical-device acceptance are not configured/verified |

## 5. Change control

- Never delete an approved row to make the roadmap appear complete. Move it to an explicit blocked/proposed-change state with a decision record.
- New requirements need user approval if they alter cost, privacy, data handling, workflow, platform, export scope or exclusions.
- Each implementation PR must cite feature IDs and test IDs.
- Update this matrix only when source, tests and evidence justify a status change.
- If a test does not exist, label it planned; do not invent a test result.
- Keep stem separation and AI cover arrangement separate.
- Keep imported-song preview and recorded-take playback separate.
- Keep CI, APK validation, emulator acceptance, physical-device acceptance and feature acceptance as separate gates.

## 6. Document links

- [Product Vision & Mission](PRODUCT_VISION_AND_MISSION.md)
- [Requirements](KARAOKE_AI_SINGING_STUDIO_REQUIREMENTS.md)
- [Architecture](ARCHITECTURE.md)
- [Development Workflow](DEVELOPMENT_WORKFLOW.md)
- [ADR template](adr/0000-template.md)
