# RZ AI Singing Studio — Product Vision & Mission

**Document type:** Product north-star and governance baseline  
**Status:** User-approved direction for product planning; technical feasibility and feature acceptance remain separate  
**Platform:** Standalone Android application, Android 12+ (API 31+)  
**Repository:** `Khun19/RZ-AI-Singing-Studio`

## 1. Vision

Make professional-quality karaoke singing, vocal recording and creative music production accessible to everyday Android users through one coherent, privacy-respecting studio — including users who do not own professional recording equipment or paid desktop software.

RZ AI Singing Studio should help a person move from choosing a song to singing, creating a new arrangement, refining their vocal performance and keeping a finished audio recording, without needing several disconnected apps.

## 2. Mission

Build a dependable Android singing studio that combines:

- Karaoke playback with synchronized, trustworthy lyrics, prioritizing Burmese-language recognition and timing.
- Vocal removal for creating karaoke backing tracks.
- Reliable synchronized backing-track playback and microphone recording.
- Natural vocal enhancement, effects, mixing, mastering and non-interruptive performance feedback.
- AI-created new cover arrangements and instrument re-performance, clearly distinct from extracting stems from an existing recording.
- Safe recording lifecycle, recovery, local project management and audio-only export.

Deliver these capabilities incrementally, with evidence that each feature works on its supported devices before expanding the product.

## 3. Intended user outcome

A user should be able to:

1. Select a song they are authorized to use.
2. Prepare a karaoke backing track when vocal separation is available and supported.
3. Follow synchronized lyrics, including clear uncertainty labels for inferred words.
4. Record their voice with the backing track aligned, monitor safely where the audio route permits, and recover unfinished work after interruptions where data remains intact.
5. Improve the take while retaining the original and controlling each proposed change.
6. Optionally create a new instrumental cover arrangement with selected instruments, style, tempo and key.
7. Preview, compare, organize and export the resulting audio without misleading quality claims.

The intended outcome is a trustworthy creative workflow, not a promise that every AI task will run offline or at identical quality on every Android device.

## 4. Product pillars

### 4.1 Singing-first studio workflow
The core experience is singing with music and recording a usable performance. Playback, lyrics, recording, alignment and recovery are foundational; advanced AI must not undermine recording reliability.

### 4.2 Natural voice, user control
Preserve the singer's identity. Enhancement should be restrained and reversible. Keep original takes, provide A/B comparison, per-change accept/reject and Undo/Redo.

### 4.3 Creative AI, not misleading labels
Vocal/instrumental separation creates karaoke backing from an existing recording. AI cover arrangement creates a new instrumental performance or arrangement. These are separate capabilities with separate feasibility, rights, quality and acceptance tests.

### 4.4 Offline-first and privacy by default
Prefer on-device processing when technically practical. Never upload audio silently. Every cloud task requires explicit task-level consent, clear data/retention disclosure and expected-cost disclosure before upload. Provide a local alternative wherever feasible. No automatic cloud backup.

### 4.5 Accessible core product
No mandatory subscription for core functionality and no forced export watermark. Compute-intensive cloud services may have disclosed costs; do not disguise those costs or promise unlimited free cloud processing.

### 4.6 Honest compatibility and quality
Support Android 12+ with capability-aware paths across device tiers. Clearly disclose unavailable features, fallback quality and device limitations. Do not promise zero Bluetooth latency, universal offline AI parity, unsupported formats or fake upsampling.

### 4.7 Rights-respecting creation
Process music the user owns or is authorized to use. Use models, datasets, libraries, lyrics sources and assets only after license, privacy, security and commercial-distribution review.

## 5. North-star outcome and success criteria

The product is successful when the complete user journey is dependable and verifiable:

- A user can import and play supported local audio.
- Lyrics are synchronized accurately enough for the declared support level, and uncertain inferred words are clearly marked.
- Backing audio and microphone capture remain synchronized over a full recording, with measured drift and route limitations documented.
- Recording interruption, pause, save and recovery behavior protects user work within stated platform/storage limits.
- Vocal processing improves intelligibility while preserving natural identity and the untouched original.
- AI cover output is genuinely a new arrangement/re-performance, not mislabeled stem extraction.
- Exported files are valid, playable and accurately described by their actual codec/sample format.
- Local projects, takes, drafts and final exports remain manageable and are not silently deleted or uploaded.
- Each released capability has reproducible tests and acceptance evidence on its declared API/device/route matrix.
- Privacy, copyright, model licensing, security, cost and platform requirements are satisfied before release.

No arbitrary quality score or device-coverage percentage is declared as passed in advance. Numerical thresholds must be set in feature-specific acceptance criteria before implementation or evaluation, then supported by measured evidence.

## 6. Product scope boundaries

### In scope
- Standalone Android application, Android 12+.
- Karaoke playback, permitted audio import, lyrics and vocal separation.
- Synchronized vocal recording, monitoring, recording modes and recovery.
- Natural vocal processing, effects, Auto Mix, Auto Mastering and performance feedback.
- AI cover arrangement and supported instrument generation.
- Audio-only MP3, AAC and WAV export, subject to actual encoder/source capabilities.
- Local recording/project library, organization, artwork, storage management and sharing.
- Burmese and English interface; Dark Studio, Light Studio and Silver Chrome Premium Tech themes.
- Offline-first processing and optional, consent-gated cloud AI.

### Explicitly out of scope unless the user approves a later change
- Dependency on RZ Builder or arbitrary ZIP-to-APK conversion.
- Karaoke video export.
- FLAC and stem export in the approved v1 export scope.
- A separate singing-training curriculum/exercise system.
- Duplicate-audio detection.
- Recording touch-lock.
- PIN/biometric vault.
- Automatic cloud backup.
- Silent cloud uploads, mandatory cloud processing or surprise charges.
- A promise to process every YouTube or online music URL.
- A guarantee of zero Bluetooth latency or full offline AI parity on all devices.

The detailed feature-level choices are maintained in [Karaoke & AI Singing Studio Requirements](KARAOKE_AI_SINGING_STUDIO_REQUIREMENTS.md).

## 7. Product decision hierarchy

Use these documents in this order:

1. **Product Vision & Mission (this document):** defines why the product exists, the intended user outcome, enduring principles and scope boundaries.
2. **Requirements:** defines the user-selected feature behavior and approved product scope.
3. **Architecture:** defines technical boundaries, component responsibilities, data flows and verification strategy.
4. **Feature Traceability Matrix:** maps each approved feature to implementation status, acceptance criteria and required evidence.
5. **Architecture Decision Records (ADRs):** record evidence-backed choices of engines, libraries, models, providers and trade-offs using the [ADR template](adr/0000-template.md).
6. **Development Workflow:** defines readiness, implementation sequence, PR lifecycle, test layers, stop conditions and Definition of Done.
7. **Roadmap / Issues / Pull Requests:** define the current execution order and individual work items.

A lower-level document or implementation must not silently redefine a higher-level product decision. If documents conflict, stop the affected work, identify the conflict and reconcile the documents before implementation.

Safety, law, platform policy, security and verified technical constraints can block or limit a proposed feature; they do not authorize silently changing the product goal. Explain the constraint and present feasible alternatives for an explicit product decision.

## 8. Change-control rules

Before adding or changing a feature:

1. Link it to a user requirement and the product outcome it serves.
2. Classify it as approved scope, implementation detail, experiment or proposed scope change.
3. For experiments, state the hypothesis, limits, data/license constraints, cost and exit criteria. Experiments do not become committed product scope automatically.
4. Review feasibility, alternatives, Android/API/ABI support, device tiers, performance, storage, battery, thermal behavior, privacy, security, copyright, licenses and cost.
5. Define acceptance criteria and tests before implementation.
6. Implement in a focused branch/PR; avoid unrelated scope expansion.
7. Verify the exact source commit and build artifact, then run relevant unit, integration, API 31/API 35 emulator and physical-device tests.
8. Update implementation status, Architecture, Requirements and README wherever the verified change affects them.
9. Mark a feature complete only after its own acceptance evidence passes.

Any feature that changes cost policy, data handling, core workflow, export scope, supported platform, product boundaries or explicit exclusions requires the user's approval before it becomes committed scope.

## 9. Delivery principles and release gates

- Build foundations before dependent AI features. Reliable synchronized audio capture/playback precedes advanced live effects and mix workflows.
- Do not select an audio engine, model, dataset or cloud provider solely by popularity; benchmark and review its license and operating constraints.
- Keep source takes immutable; processing is non-destructive.
- Keep implementation, CI, APK validation, provenance, emulator acceptance, physical-device acceptance and feature acceptance as separate gates.
- Never skip a failed gate to begin dependent work.
- Use **Root Cause → Fix → Test → Evidence → Documentation Update**.
- A feature's status must use the controlled vocabulary defined in the Feature Traceability Matrix: Implemented in MVP — not fully accepted, In progress, Planned — not implemented, Experimental, Verified working, or Blocked.
- Documentation must never claim more than the code and evidence prove.

## 10. Current product maturity

The repository is an early MVP, not the completed studio. Basic local audio import, preview, microphone-only recording and a minimal local take list exist in the known baseline. Synchronized backing-plus-microphone recording and the advanced AI capabilities remain future work until implementation and feature-specific acceptance prove otherwise.

This document establishes direction; it is not a technical feasibility report, release promise or claim that the full vision has already been achieved.

## 11. Document ownership and review

The user is the product decision authority. The implementation team/assistant may research, recommend, design and execute approved work, but must not silently change user-selected scope or policy.

Review this document whenever the user explicitly changes product goals, core priorities, privacy/cost policy or scope boundaries. Routine implementation decisions should remain consistent with it and be recorded in the appropriate lower-level document.
