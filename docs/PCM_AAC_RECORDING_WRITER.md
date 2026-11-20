# PCM → AAC/M4A Recording Writer

## Scope
The writer is an isolated recording component intended to drain mono float PCM from the Oboe engine on a worker thread and encode AAC-LC into an M4A container. It does not replace the current MediaRecorder UI path until integration and acceptance tests pass.

## Safety and audio-thread rules
- Audio callbacks must not perform encoding, file I/O, logging, allocations, or blocking synchronization.
- PCM is drained on a dedicated worker; MediaCodec and MediaMuxer are accessed from that worker.
- PCM is converted to native-order signed 16-bit samples with clipping and non-finite sanitization.
- Presentation timestamps derive from sample-frame count and the negotiated sample rate.
- Stop must terminate the capture stream, drain remaining queued PCM, signal encoder EOS, drain encoded output, and finalize the muxer.
- Failure handling must close/release codec and muxer and remove incomplete output.

## Acceptance checklist
- [ ] Unit tests for conversion, clipping, NaN/Infinity and frame-clock PTS.
- [ ] Instrumented test creates an AAC/M4A file and validates track metadata and decoded duration.
- [ ] API 31 and API 35 emulator acceptance.
- [ ] Redmi physical-device recording, playback, file integrity and duration acceptance.
- [ ] Queue overflow and stop/error recovery tests.
- [ ] Only after acceptance, consider replacing the existing user-facing MediaRecorder path.

## Current status
Design and PCM conversion utility are being introduced. End-to-end Oboe capture-to-M4A integration and device acceptance remain pending.
