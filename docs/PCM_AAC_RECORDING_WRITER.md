# PCM → AAC/M4A Recording Writer

## Scope
Experimental worker-thread capture encoder draining mono Float PCM from Oboe and encoding AAC-LC into M4A. It remains isolated from the public UI and does not replace MediaRecorder until integration and acceptance pass.

## Capture and encoder policy
- Start the Oboe engine and obtain the negotiated capture sample rate from engine diagnostics before configuring MediaCodec.
- Reject unavailable or mismatched capture sample-rate values rather than silently producing pitch/duration errors.
- PCM capture, conversion, MediaCodec and MediaMuxer work only on the writer worker thread.\n- Wait for capture PCM without queuing artificial empty input buffers; a bounded starvation timeout fails the recording instead of silently ending it.
- Negative PCM reads are treated as errors, not as silence or EOS.
- PCM conversion clips normalized float values and sanitizes NaN/Infinity.
- PTS are based on accumulated captured sample count and the validated actual sample rate.
- On stop, stop the audio stream and repeatedly drain the capture ring until empty, then signal encoder EOS and drain encoded output.
- Failures release codec/muxer and delete incomplete output. Stop timeout remains bounded by worker polling and must be tested.

## Acceptance checklist
- [ ] Converter unit tests: clipping, non-finite values, sample-count overload and frame-clock PTS.
- [ ] Writer instrumented tests: startup failure, read failure, stop/drain, EOS, format change, timeout and incomplete-file cleanup.
- [ ] AAC/M4A integrity: track metadata, decodability, duration, sample rate and file size.
- [ ] API 31 and API 35 emulator acceptance.
- [ ] Redmi physical recording, playback, file integrity, duration, route and endurance acceptance.
- [ ] Queue overflow and recovery behavior measured.
- [ ] Only after acceptance, consider public UI integration.

## Current status
Lifecycle and stop/drain hardening committed for review. Runtime and device validation remain pending; this writer is not production-ready.
