# RZ AI Singing Studio — Development Workflow

**Purpose:** Deliver the approved Product Vision and Requirements through small, testable, evidence-backed increments without losing features, introducing unapproved scope or destabilizing the audio foundation.

## 1. Authority and change control

Follow this hierarchy:

1. [Product Vision & Mission](PRODUCT_VISION_AND_MISSION.md)
2. [Requirements](KARAOKE_AI_SINGING_STUDIO_REQUIREMENTS.md)
3. [Architecture](ARCHITECTURE.md)
4. [Feature Traceability Matrix](FEATURE_TRACEABILITY_MATRIX.md)
5. [Architecture Decision Records](adr/0000-template.md)
6. Roadmap, issues, branches and pull requests

If two documents conflict, stop the affected work and reconcile them before coding. Legal, licensing, security, privacy, Android platform or measured technical constraints can block a path, but must be documented with feasible alternatives. Do not silently remove or reinterpret approved features.

## 2. Delivery rules

- GitHub Actions on Ubuntu is the canonical build and test environment. Local/Termux builds are optional diagnostics, not release evidence.
- Work from the latest main branch. Do not commit directly to main.
- One PR should deliver one coherent feature slice or one narrowly scoped documentation/engineering change.
- Every PR references feature IDs and test IDs from the traceability matrix.
- No unrelated refactors, model additions, provider integrations or dependency upgrades in a feature PR.
- Do not add paid services, cloud uploads, model downloads, new permissions or new data collection without the required review and explicit product approval where policy changes.
- Preserve originals and user data. No destructive clean/reset/sync operation as a troubleshooting shortcut.
- Never mark a feature complete because a screen, placeholder, interface or model file exists.
- Never merge a failed, pending, stale or mismatched exact-head check. GitHub native auto-merge may be used only when the repository ruleset remains authoritative and every required gate passes.

## 3. Feature readiness — Definition of Ready

A feature may enter implementation only when all applicable items are true:

- [ ] Approved requirement and stable feature ID exist.
- [ ] Dependencies and upstream feature gates are identified.
- [ ] User-visible behavior and explicit non-goals are clear.
- [ ] Technical alternatives and feasibility questions are recorded.
- [ ] Android API/ABI and device capability range are defined.
- [ ] Model, dataset, library, music and asset license review is complete or has an explicit research gate.
- [ ] Privacy, security, copyright and platform-policy implications are reviewed.
- [ ] Offline behavior, cloud consent, cost and fallback are specified where relevant.
- [ ] Acceptance criteria and test IDs are defined before implementation.
- [ ] Performance budgets or measurement methodology are declared before performance claims are made.

If any critical item is missing, keep the feature in research/planned status rather than coding around the gap.

## 4. Ordered implementation roadmap

### Stage 0 — Product and repository baseline

- Keep Vision, Requirements, Architecture and this matrix consistent.
- Audit current source, tests, dependencies, Gradle Wrapper, CI, permissions and release configuration.
- Record the exact baseline commit and current MVP limitations.
- No AI expansion during baseline reconciliation.

**Exit gate:** all approved features have IDs, honest status, acceptance coverage and known dependencies.

### Stage 1 — Audio-engine research and decision

Compare Android native AudioRecord/AudioTrack with Oboe and other viable approaches where appropriate. Do not select an engine by popularity alone.

Benchmark on representative Android 12+ device tiers:
- Input/output and round-trip latency.
- Capture/playback synchronization and long-session drift.
- Buffer underruns, glitches and callback deadline misses.
- Wired, speaker and Bluetooth route behavior.
- CPU, memory, battery, thermal behavior and stability.
- API/ABI compatibility, licensing, maintainability and fallback behavior.

Record measurements, reproducible harness, alternatives and rollback plan in an ADR.

**Exit gate:** evidence supports an audio-engine decision and a fallback path. No AI model or complex effect is allowed to hide an unstable audio foundation.

### Stage 2 — Synchronized karaoke vertical slice

Implement only the minimum end-to-end path:
1. Select and decode a supported local backing track.
2. Capture microphone audio.
3. Start playback and capture against a shared monotonic timing model.
4. Preserve separate source assets and timing metadata.
5. Render or mix an aligned output.
6. Validate a short test, then a full-song duration test.

Measure start offset, drift, glitches, dropped buffers and repeatability. Do not move to dependent recording features while synchronization fails.

**Exit gate:** synchronized playback/capture/mix passes defined unit, integration, emulator and physical-device acceptance.

### Stage 3 — Recording lifecycle and data safety

Implement One-Take, Multi-Take and Punch-In on shared session primitives; background/screen-off foreground service; call interruption; autosave; drafts; process-death recovery; segment merge and low-storage handling.

**Exit gate:** interruptions and recovery preserve original audio and recoverable work without silent deletion.

### Stage 4 — Live monitoring and natural vocal processing

Implement headphone detection, speaker feedback-risk reduction, route changes, latency estimation/manual adjustment and bounded live DSP. Keep post-recording processing separate from the real-time monitoring chain.

AI inference must not block the audio callback. Use a measured, deterministic fallback or bypass when the device cannot meet the real-time budget.

**Exit gate:** recording remains stable while monitoring/effects are active; latency, quality, power and thermal criteria pass.

### Stage 5 — Lyrics pipeline

Build authorized source lookup, audio recognition, missing-word inference, Burmese spelling/segmentation, confidence labels, line timing, manual correction and playback synchronization.

**Exit gate:** a rights-cleared evaluation set demonstrates declared language and timing performance; uncertain words remain visibly uncertain.

### Stage 6 — Vocal separation for karaoke backing

Evaluate model provenance, commercial license, quality, artifacts, runtime, memory, storage, thermal impact and low-tier fallbacks. Keep separation output distinct from generated cover arrangements.

**Exit gate:** model and data rights are cleared and output-quality/performance acceptance passes on declared device tiers.

### Stage 7 — Post-recording production

Add reversible effects, denoise, restrained pitch correction, AI suggestions, A/B, per-change accept/reject, Undo/Redo, Auto Mix and Auto Mastering. Preserve original takes and project lineage.

**Exit gate:** audio-quality, clipping, alignment, repeatability and non-destructive editing tests pass.

### Stage 8 — AI cover arrangement and instrument generation

Treat as a separate high-compute R&D stream. Validate music rights, training/data provenance, instrument coverage, full-song structure, melody-preservation behavior, compute options and transparent cloud costs before product integration.

**Exit gate:** the output is demonstrably a new arrangement/re-performance, not mislabeled stem extraction; selected controls work and licensing is acceptable.

### Stage 9 — Export, local library and interface completion

Implement MP3/AAC/WAV only where the actual encoder supports the selected settings; metadata/artwork; local folders/search; rename/trim/share/storage; Burmese/English; themes and orientation.

**Exit gate:** external playback, metadata, persistence, low-storage safety and accessibility tests pass. Never fake sample rate or bit depth.

### Stage 10 — Release readiness

- Exact source commit and build provenance are recorded.
- APK package, min/target SDK, signature, size and SHA-256 are validated.
- Exact APK install/launch passes on API 31 and API 35 emulators.
- Feature-specific workflows pass on emulators where meaningful.
- Physical-device acceptance covers real microphone/audio routes, background execution, interruptions, recovery, battery and thermals.
- Release signing, privacy/copyright/license review, release notes and rollback plan are complete.

**Exit gate:** every release-blocking feature is accepted with evidence; debug CI success is not a production release.

## 5. Pull request lifecycle

1. Start from current main and create a focused branch: `feat/<feature-id>-short-name`, `fix/<feature-id>-short-name`, `docs/<topic>` or `adr/<decision-topic>`.
2. Read the target feature row, Architecture and relevant ADRs before editing.
3. Implement the smallest vertical slice with tests.
4. Run the relevant local checks only as a fast diagnostic; canonical validation remains GitHub Actions.
5. Open a PR with:
   - Feature IDs and test IDs.
   - User-visible behavior and non-goals.
   - Root cause (for fixes), implementation summary and alternatives.
   - License/privacy/security/platform/performance impact.
   - Test commands and results.
   - Exact source SHA, workflow run and artifact evidence when available.
   - Remaining limitations and follow-up work.
6. Wait for required CI and automated review. Resolve all actionable review threads.
7. Re-fetch the PR and confirm the tested head SHA still equals the current PR head SHA.
8. Allow ruleset-gated GitHub native squash auto-merge only when all required checks and review-thread gates pass.
9. After merge, verify main contains the intended change and update the matrix/documentation from verified evidence.

Never force-push, bypass rulesets, merge stale checks or infer success from an earlier commit.

## 6. Required test layers

| Layer | Purpose | When required |
|---|---|---|
| Unit | Domain state machines, timing math, storage rules, metadata, policy and deterministic DSP logic | Every feature with testable domain behavior |
| Android integration | Permissions, media APIs, services, storage, lifecycle, routing and Compose behavior | Every platform-facing feature |
| Emulator | Repeatable install/launch and supported app workflows | Every PR/build; API 31 and API 35 exact APK validation |
| Physical device | Real microphones, wired/Bluetooth routes, latency, thermals, background limits and OEM behavior | Audio, lifecycle, AI performance and release acceptance |
| Model/audio evaluation | Quality, confidence, artifacts, runtime, memory and rights provenance | Every AI/audio model or effect |
| Release audit | Provenance, signing, permissions, licenses, privacy and rollback | Every release candidate |

Current CI runs Android instrumentation on API 35 and exact APK install/launch validation on API 31 and API 35. Until a dedicated API 31 functional instrumentation suite is added, do not claim full API 31 workflow coverage.

## 7. Evidence and status transitions

Use **Root Cause → Fix → Test → Evidence → Documentation Update**.

For each accepted feature, retain:
- Feature ID, requirement revision and acceptance criteria.
- Source commit SHA and PR head/base SHA.
- Workflow name/run ID and required job conclusions.
- Artifact name/ID, APK SHA-256, package name, SDK levels and signing identity.
- Emulator API/device profile and install/launch result.
- Test reports, screenshots, logs and benchmark data as applicable.
- Physical-device model/API/audio route and observed result.
- Known limitations, fallback behavior and rollback path.

Current APK validation captures badging, signature, SHA-256, size, emulator API, install path, launch result, screenshot and crash log. A single machine-readable provenance manifest binding source SHA, workflow run, artifact ID and APK SHA is still a follow-up engineering task; do not imply that the current evidence script already provides the complete binding.

Keep these states separate:
1. Source/CI checks passed.
2. APK built and structurally validated.
3. Exact APK installed and launched on emulator.
4. Exact APK accepted on physical device.
5. Feature-specific acceptance passed.
6. Release candidate signed and audited.

A later state requires all applicable earlier states, but an earlier state does not imply a later one.

## 8. Stop conditions and risk controls

Stop the affected feature and record the blocker when:
- Audio drift, glitches or callback overruns exceed predeclared limits.
- A model, dataset, music source or dependency lacks suitable license/provenance.
- A cloud path starts before explicit task-level consent or hides data/cost/retention.
- An Android permission, foreground-service or background-start requirement is unresolved.
- A device cannot meet performance/thermal limits and no tested fallback exists.
- Original recordings, drafts or final exports could be lost or silently overwritten.
- CI is stale, failing, missing or tied to a different head SHA.
- Requirements, Architecture and implementation disagree.

Known high-risk areas to manage explicitly:
- Audio synchronization and Bluetooth latency.
- Real-time DSP deadlines and AI model resource use.
- Android 12+ background microphone restrictions and target SDK 35 behavior.
- Burmese lyric recognition and copyrighted lyric sources.
- Music-generation rights and model/data provenance.
- Cloud GPU cost, provider retention and user consent.
- Low-storage recovery and non-destructive project history.
- Complete build-to-artifact provenance and release signing.

## 9. Definition of Done

A feature is done only when:
- Approved behavior is implemented, not represented by a placeholder.
- Acceptance criteria are measurable and all required tests pass.
- Exact tested source and artifact identity are recorded.
- Device/API/route limitations and fallback are documented.
- Privacy, security, copyright and license review is complete.
- Original user data is preserved and failure recovery is tested.
- Feature Traceability Matrix, Architecture and README status are updated.
- No unresolved blocking review comments or failed required checks remain.

## 10. Related documents

- [Product Vision & Mission](PRODUCT_VISION_AND_MISSION.md)
- [Requirements](KARAOKE_AI_SINGING_STUDIO_REQUIREMENTS.md)
- [Architecture](ARCHITECTURE.md)
- [Feature Traceability Matrix](FEATURE_TRACEABILITY_MATRIX.md)
- [ADR template](adr/0000-template.md)
