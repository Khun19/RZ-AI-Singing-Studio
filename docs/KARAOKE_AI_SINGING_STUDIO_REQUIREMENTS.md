# RZ AI Singing Studio — Karaoke & AI Singing Studio Requirements

**Status:** User-selected product baseline (v1)  
**Platform:** Standalone Android application, Android 12+ (API 31+)  
**Repository:** `Khun19/RZ-AI-Singing-Studio`  
**Architecture direction:** Offline-first, optional cloud acceleration  
**Important:** This document records product choices. It does not claim that every feature is technically feasible on every device or that any AI model/library has passed license review.

## 1. Product boundaries and principles

- Keep this application fully separate from RZ Builder / Universal-ZIP-to-APK-Builder.
- Build a professional karaoke and AI singing studio, not a generic audio player.
- Preserve the user's natural voice; avoid excessive autotune or artificial voice replacement.
- Prefer on-device processing. Cloud processing is optional and requires explicit, informed consent.
- Before cloud upload, disclose the task, data being uploaded, expected cost (if any), and retention policy. Do not upload recordings automatically.
- Delete temporary uploaded audio after processing where provider capabilities and terms allow; disclose any provider-side backup/log retention that cannot be immediately purged.
- No mandatory subscription for core use and no forced export watermark.
- Use only models, libraries, datasets, music and artwork assets whose licenses permit the intended commercial distribution.
- Process only music the user owns or is authorized to use. YouTube/online links and catalog access must comply with platform terms, copyright and catalog licenses.
- Support Android 12+ broadly, including low-RAM devices. Use capability tiers and honest fallbacks rather than assuming every device can run large models.
- Research, implementation, tests and acceptance must be tracked separately. A CI pass alone does not prove a feature works.

## 2. Karaoke playback, lyrics and recording

### Music input and sources
- Import local audio files.
- Support permitted online music links where platform terms allow processing.
- Provide a licensed music catalog and Myanmar music discovery only when suitable rights and provider terms are available.
- Support offline use for downloaded/available local tracks, lyrics and compatible local AI processing.
- Do not promise that every YouTube URL can be downloaded or processed.

### Lyrics
- Automatically retrieve lyrics using online lookup and audio speech recognition where lawful and technically available.
- Use AI to infer missing words, but label uncertain text with confidence/uncertainty; never present guesses as verified lyrics.
- Prioritize Myanmar-language recognition, spelling and line segmentation.
- Show three lines: previous, current and next; visually highlight the current line.
- Synchronize automatically, allow manual timing adjustment, and provide AI verification/correction.
- Display one focused lyric line at a time while previewing the upcoming line.

### Recording modes
- One-take, multi-take and punch-in recording.
- AI post-recording error detection and suggestions.
- Provide suggestion-only, automatic-fix-with-A/B-compare and per-change accept/reject workflows.
- Preserve the original take; support Undo/Redo.
- Recording output is audio-only.

### Live voice processing and monitoring
- AI live voice enhancement: noise reduction, automatic level control, clarity enhancement and restrained natural pitch correction while preserving voice identity.
- Natural voice tuning; no aggressive pitch or timbre alteration.
- Smart monitoring: enable live monitoring when headphones are connected; reduce echo/acoustic feedback risk when headphones are absent.
- Detect Bluetooth headphones and attempt latency estimation/compensation; provide manual latency adjustment and honest fallback because Bluetooth latency cannot be guaranteed to zero.
- Distinguish monitoring latency from final recorded-track alignment; align/compensate tracks after recording when possible.
- The user did not request touch lock during recording.
- Background recording is required, including while the screen is off, subject to Android foreground-service, microphone permission and notification requirements.
- On incoming phone calls, automatically pause recording and preserve the current take. After the call, the user manually resumes; do not auto-resume without user action.

### Recovery and save behavior
- Periodically auto-save recording state/audio segments.
- Recover unfinished recordings after app crash or device restart where data remains intact.
- Preserve original segments if recovery/merge fails.
- Automatically save when recording stops.
- Keep unfinished work as drafts that can be reopened and continued.
- Clearly separate drafts, source takes and final recordings.

## 3. AI cover arrangement and instrumental generation

This product requirement is **new arrangement / re-performance**, not extraction of the original guitar or other instrument stem. Stem separation and instrument generation are distinct capabilities.

### Arrangement modes and sound
- Original Melody Preservation: retain the song's melody, chords, tempo and structure while changing the instrumental arrangement.
- AI Creative Cover: generate a creative new arrangement based on the source song.
- Support both natural-sounding instrument mode and polished studio-sound mode.
- User manually selects instruments. Instrument choices should include guitar (acoustic/classical/electric), piano/keyboard, strings, drums/percussion, bass, flute/saxophone, traditional Myanmar instruments and other supported instruments.
- Cover styles: Original, Acoustic/Unplugged, Romantic/Soft, Pop/Modern, Rock/Alternative, Jazz/Blues, Classical, Cinematic/Orchestral, Lo-fi/Chill, Ambient, Traditional Myanmar and Custom.
- Tempo controls: original tempo, slow/normal/fast and manual BPM.
- Key/pitch controls: original key, manual key and AI recommendation.
- Generate the whole song in one pass.
- Quality selection: Standard, High and Studio.
- Show the completed result only; no live generation preview is required.
- Regeneration applies to the whole cover.
- Keep full local version history, including playback, generation settings, selection and deletion controls.

### Vocal/instrumental mix and mastering
- AI Auto Mix: automatically balance vocal and instrumental levels, align timing where possible, apply natural effects and control peaks/clipping.
- AI Auto Mastering.
- Provide preview before saving/exporting.

## 4. Vocal effects, coaching and scoring

- AI Natural Vocal Effects: intelligently set natural reverb, soft echo, EQ and dynamic compression without over-processing.
- Smart Singing Coach feedback for pitch, timing, rhythm, volume and voice quality; feedback must not interrupt singing.
- Performance scoring: overall and component scores for pitch, timing, rhythm and voice quality.
- Compare performance history and provide improvement tips.
- A separate singing-training curriculum/exercise system is not required.

## 5. Export, library and local data

### Export
- Audio-only export; karaoke video export is not part of the selected scope.
- MP3, WAV and AAC, with available bitrate, sample-rate, bit-depth and stereo/mono controls.
- Standard, High and Studio quality choices. Do not fake quality by upsampling or claim detail that was not captured in the source/recording path.
- Studio export may offer up to 24-bit/48 kHz only when the processing and source path genuinely support it.
- Automatic filename using song metadata, with manual rename.
- Metadata: title, artist, album, artwork, date, AI style, instruments, tempo, key and audio quality.
- Artwork options: AI-generated artwork, user's own photo, gallery selection and custom editing. Artwork is attached to audio metadata where the chosen format supports it.

### Local library
- Local storage only; no automatic cloud backup.
- Organize by song, artist, date, album, genre, recording type and cover style.
- Allow user-created folders and file movement.
- Rename, play, trim, duplicate and delete/restore where technically supported.
- Search by song title only.
- No duplicate-audio detection requirement was selected; do not add it to the committed scope without a later decision.
- Standard local storage only: no app PIN, biometric lock or private vault.
- Show storage usage and warn when space is low.
- Automatically remove only disposable temporary files. Never delete source recordings, drafts or final files without user confirmation.
- Share audio to other apps and supported Bluetooth/Nearby Share targets; provide export-folder management.

## 6. UI and accessibility direction

- Professional studio interface.
- Burmese and English UI.
- Dark Studio, Light Studio and Silver Chrome Premium Tech themes, with selectable theme behavior.
- Smart portrait/landscape rotation with manual orientation lock.
- Adapt lyrics, timeline, recording controls, audio levels, monitoring status and AI feedback to available screen space.
- Do not add a recording touch-lock feature.

## 7. AI cost and privacy policy

The selected policy is **D: Offline-First + Optional Paid Cloud AI + Local Alternative**.

- Prefer local/on-device AI whenever feasible.
- Cloud AI is optional and must never be silently invoked.
- Obtain explicit consent for each cloud-processing task before uploading audio or other personal content.
- Display any expected cloud charge before use; no surprise charges.
- Provide a local alternative wherever technically feasible, and disclose quality, speed or feature differences.
- No mandatory subscription for core functionality.
- Cloud temporary-file deletion is required after processing to the extent supported by the provider; document exceptions in provider terms.
- Do not claim that open-source automatically means commercial-use permission or zero operating cost.

## 8. Engineering and release gates

For every feature, record:

1. Technical feasibility and alternative implementations.
2. Model/library/dataset/asset license and commercial-use review.
3. Security, privacy, copyright and platform-policy review.
4. Android API/ABI support and device capability requirements.
5. Model size, RAM/CPU/GPU/NPU use, latency, battery and storage measurements.
6. Offline behavior, cloud cost, consent, failure and fallback behavior.
7. Reproducible unit, integration, emulator and physical-device test plans.
8. Acceptance criteria and evidence.

Use the evidence sequence **Root Cause → Fix → Test → Evidence**. Keep feature implementation, CI status, APK validation and physical-device acceptance as separate gates.

## 9. Initial implementation sequence

1. Audit the existing Android MVP, branch/PR state, CI and current test evidence.
2. Freeze this requirements baseline and create a feature-to-test traceability matrix.
3. Establish audio-engine architecture and prove stable synchronized backing-track playback plus microphone recording before adding AI.
4. Implement recording lifecycle, foreground service, call interruption, draft/autosave and crash recovery.
5. Research and benchmark vocal separation and on-device inference candidates; do not ship an unlicensed or unbenchmarked model.
6. Add vocal processing, mix/master and lyrics pipeline incrementally.
7. Research music arrangement/generation separately; this is a high-cost/high-compute feature and must not be conflated with stem separation.
8. Add export/library/UI capabilities and run API 31 and API 35 exact-APK install/launch tests, then physical-device acceptance.

**No feature is considered complete until its own acceptance criteria and evidence pass.**
