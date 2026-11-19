# RZ AI Singing Studio

RZ AI Singing Studio is a standalone Android karaoke, vocal-recording and AI cover-arrangement application. It is a separate project from RZ Builder / Universal-ZIP-to-APK-Builder.

The product goal is to let users sing along with music, create karaoke arrangements with newly generated or re-performed instruments, improve and mix their own vocal recordings, and manage audio projects locally.

> **Project status:** Early Android MVP. The feature set below is the approved product direction, not a claim that every feature has been implemented or validated.
>
> **Product north star:** [Product Vision & Mission](docs/PRODUCT_VISION_AND_MISSION.md) defines the product goal, user outcome, scope boundaries and change-control rules. The [Requirements](docs/KARAOKE_AI_SINGING_STUDIO_REQUIREMENTS.md) define approved feature behavior; the [Architecture](docs/ARCHITECTURE.md) defines the technical design. The [Feature Traceability Matrix](docs/FEATURE_TRACEABILITY_MATRIX.md) maps every approved feature to status and acceptance evidence; the [Development Workflow](docs/DEVELOPMENT_WORKFLOW.md) defines gated delivery; and the [ADR template](docs/adr/0000-template.md) standardizes technical decisions.

## Product principles

- **Android 12+** support, with testing across device capability tiers rather than only one phone.
- **Offline-first AI:** prefer on-device processing whenever technically practical.
- **Optional cloud AI:** never upload audio silently. Ask for explicit consent for each cloud task and disclose expected cost before processing.
- **Local alternatives:** provide an on-device alternative wherever feasible and explain any quality, speed or capability differences.
- **Free-first:** no mandatory subscription for core functionality and no forced export watermark.
- **Privacy:** recordings and project data stay local by default. No automatic cloud backup.
- **Licensed technology:** review model, library, dataset, music and artwork licenses for the intended commercial distribution before use. Open-source status alone does not prove commercial-use permission.
- **Honest capability reporting:** distinguish implemented, experimental, planned and validated features. CI success alone is not proof of runtime acceptance.

## Current implementation status

This repository currently contains an early Android MVP. The existing implementation includes:

- Import a user-selected audio document through Android Storage Access Framework.
- Basic audio preview using Android MediaPlayer.
- Microphone recording to AAC/M4A files using Android MediaRecorder.
- Local take listing and deletion inside the app's private storage.

The current recorder captures microphone audio only. It does **not yet** synchronize and mix the selected backing track with the microphone recording.

The following product capabilities are **not yet implemented or accepted**:

- Karaoke vocal removal / vocal-instrumental separation.
- AI cover arrangement or instrument re-performance/generation.
- Synchronized backing-track playback and vocal recording as a complete karaoke workflow.
- Live voice monitoring, Bluetooth latency compensation and background/screen-off recording.
- Call-interruption handling, autosave, drafts and crash/restart recovery.
- Lyrics retrieval, Myanmar speech recognition, lyric timing and verification.
- Natural vocal enhancement, denoise, pitch correction, AI effects, mixing or mastering.
- One-take, multi-take and punch-in workflows.
- Singing feedback, performance scoring and history.
- MP3/WAV/AAC export controls, metadata and cover artwork generation.
- Advanced local library, sharing and storage management.
- Release signing and verified physical-device acceptance.

Do not interpret this roadmap as evidence that these features already work. Each feature must pass its own tests and acceptance criteria before being marked complete.

## Approved feature scope

The complete user-selected baseline is documented in [Karaoke & AI Singing Studio Requirements](docs/KARAOKE_AI_SINGING_STUDIO_REQUIREMENTS.md).

### Karaoke, lyrics and recording

- Import local audio and support online sources only where platform terms and copyright permissions allow.
- Add a licensed music catalog only when suitable distribution and usage rights are available.
- Display previous, current and upcoming lyric lines, highlighting the current line.
- Automatically retrieve and synchronize lyrics where possible, allow manual timing adjustment, and use AI verification/correction.
- Prioritize Myanmar-language recognition, spelling and line segmentation. Mark uncertain AI-inferred words instead of presenting guesses as verified lyrics.
- Support one-take, multi-take and punch-in recording.
- Provide post-recording AI suggestions and user-controlled fixes while preserving the original take.

### AI vocal processing and monitoring

- Preserve the user's natural vocal identity.
- Research and implement natural noise reduction, level control, clarity enhancement and restrained pitch correction.
- Provide natural AI vocal effects and AI-assisted mixing/mastering.
- Smart headphone monitoring, with feedback-risk reduction when headphones are not connected.
- Detect Bluetooth routes and attempt latency estimation/compensation, while acknowledging that wireless latency varies and cannot be guaranteed to reach zero.
- Support background and screen-off recording using Android-compliant microphone foreground services and notifications.
- Pause safely on incoming calls; require the user to resume.
- Autosave, draft management and crash/restart recovery must protect unfinished recordings where storage remains intact.

### AI cover arrangement and instrument generation

AI cover creation means creating a **new instrumental arrangement or re-performance**, not merely extracting the guitar or another instrument stem from the original recording. Vocal/instrumental separation is a separate capability for karaoke backing-track creation.

- Original Melody Preservation and AI Creative Cover modes.
- Natural instrument and polished studio sound options.
- User-selected instruments, including supported guitars, piano/keyboard, strings, percussion, bass, winds and traditional Myanmar instruments.
- Selectable cover styles, tempo and key/pitch controls.
- Whole-song generation, selectable quality and full local version history.
- AI Auto Mix and AI Auto Mastering.

Model feasibility, device requirements, copyright considerations and commercial-use licenses must be researched before implementation.

### Export and local library

- Audio-only output; karaoke video export is not in the selected scope.
- Target formats: MP3, WAV and AAC, with controls exposed only where the actual encoder and processing path support them.
- Standard, High and Studio quality choices. Do not fake quality through upsampling or claim detail not present in the source.
- Metadata, automatic/manual file naming and artwork options.
- Local-only library and no automatic cloud backup.
- Organize by song, artist, date, album, genre, recording type and cover style; allow user-created folders.
- Song-title search only in the selected scope.
- Storage warnings and cleanup of disposable temporary files. Never delete source recordings, drafts or final files without confirmation.
- Share/export audio through supported Android sharing targets.

### Interface

- Professional studio interface.
- Burmese and English UI.
- Dark Studio, Light Studio and Silver Chrome Premium Tech themes.
- Smart portrait/landscape rotation with manual orientation lock.
- Responsive layout for lyrics, timeline, recording controls, audio levels and feedback.
- No recording touch-lock feature.

## AI cost and privacy policy

The approved policy is **Offline-First + Optional Paid Cloud AI + Local Alternative**.

1. Prefer on-device AI where feasible.
2. Cloud AI is optional and must not be invoked without explicit task-level consent.
3. Before a cloud task, disclose what data will be uploaded and any expected charge.
4. Provide a local alternative wherever technically feasible.
5. No surprise charges or mandatory subscription for core functionality.
6. Do not upload recordings for cloud backup.
7. Delete temporary uploaded audio after processing to the extent supported by the provider. Disclose provider-side backups, logs or retention that cannot be immediately deleted.
8. Evaluate model hosting, GPU, bandwidth, storage and maintenance costs; "open source" does not mean zero cost.

## Technology and implementation direction

Technology choices are not final until the feasibility and license review is complete. Evaluate candidate Android audio engines, DSP libraries, inference runtimes and AI models against:

**Audio engine selection is currently open.** The [Audio Engine Benchmark Preparation](docs/AUDIO_ENGINE_BENCHMARK_PLAN.md) defines the physical-device test protocol; [ADR-0001](docs/adr/0001-audio-engine-selection.md) records why no engine has been selected yet.

- Android API and ABI compatibility.
- Commercial-use license and redistribution obligations.
- Model/package size and download requirements.
- RAM, CPU, GPU/NPU, battery and thermal impact.
- Audio latency, synchronization, quality and stability.
- Offline behavior and graceful fallback on low-end devices.
- Cloud pricing, privacy terms and data-retention behavior.

Keep audio playback/recording, lyrics, vocal processing, cover generation, mixing/mastering, export and local project management as independently testable capabilities. Do not equate stem separation with generative instrument arrangement.

## Build and test

Requirements for the current Android MVP:

- Android 12 (API 31) or later.
- Java 17.
- Gradle 8.9 through the committed Gradle Wrapper.
- Android SDK platform 35 and build tools 35.0.0 for local builds.

Use the committed Gradle Wrapper:

```bash
./gradlew --version
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

GitHub Actions runs the workflows configured in this repository. Debug APK artifacts are development builds, not signed production releases. Check the actual workflow run and artifact for the exact commit being evaluated; do not assume a workflow passed merely because a commit exists.

For Android audio features, CI tests must be supplemented by emulator and physical-device tests. Validate at least API 31 and API 35, and cover different device capability tiers for latency, background execution, microphone routing, Bluetooth, interruptions, recovery and storage behavior.

## Evidence and release gates

For every feature, record:

1. Technical feasibility and alternatives.
2. Model/library/dataset/asset license and commercial-use review.
3. Security, privacy, copyright and platform-policy review.
4. Android API/ABI support and device capability requirements.
5. Model size, RAM/CPU/GPU/NPU use, latency, battery and storage measurements.
6. Offline behavior, cloud cost, consent, failure and fallback behavior.
7. Reproducible unit, integration, emulator and physical-device test plans.
8. Explicit acceptance criteria and test evidence.

Use the evidence sequence **Root Cause → Fix → Test → Evidence**.

Keep these states separate:
- Source/CI checks passed.
- APK built and structurally validated.
- Exact APK installed and launched on emulator.
- Exact APK accepted on physical device.
- Feature-specific acceptance passed.

A CI pass does not imply APK validation, provenance validation or device acceptance. No feature is complete until its own acceptance criteria and evidence pass.

## Initial implementation sequence

1. Audit the current Android MVP, repository/PR state, CI and existing test evidence.
2. Maintain the approved requirements baseline and feature-to-test traceability.
3. Prove synchronized backing-track playback and microphone recording with reliable timing before layering on AI.
4. Implement recording lifecycle, Android foreground service, call interruption, autosave, drafts and recovery.
5. Research and benchmark vocal separation and on-device inference candidates.
6. Add lyrics, vocal processing, mixing and mastering incrementally.
7. Research AI music arrangement and instrument generation as a distinct, compute-intensive workstream.
8. Complete export, library and UI capabilities.
9. Validate exact APKs on API 31 and API 35 emulators, then perform physical-device acceptance.

## Audio and copyright

Only import, process, arrange or distribute music that the user owns or is authorized to use. Online links, platform integrations, lyrics sources, model training data and licensed catalog content must comply with applicable copyright, platform terms and provider licenses.

## License

The project is distributed under Apache License 2.0. See [LICENSE](LICENSE).
