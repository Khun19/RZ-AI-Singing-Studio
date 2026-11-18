#!/usr/bin/env bash
set -Eeuo pipefail

diagnostics_dir="app/build/ci-diagnostics"
mkdir -p "$diagnostics_dir"

capture_failure() {
  local exit_code=$?
  trap - ERR
  adb devices -l > "$diagnostics_dir/adb-devices.txt" 2>&1 || true
  adb shell getprop > "$diagnostics_dir/emulator-getprop.txt" 2>&1 || true
  adb logcat -b all -d -t 5000 > "$diagnostics_dir/logcat.txt" 2>&1 || true
  adb shell dumpsys activity processes > "$diagnostics_dir/activity-processes.txt" 2>&1 || true
  exit "$exit_code"
}
trap capture_failure ERR

timeout 90s adb wait-for-device

for attempt in $(seq 1 60); do
  device_state="$(adb get-state 2>/dev/null || true)"
  boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  if [[ "$device_state" == "device" && "$boot_completed" == "1" ]]; then
    break
  fi
  sleep 2
done

[[ "$(adb get-state)" == "device" ]]
[[ "$(adb shell getprop sys.boot_completed | tr -d '\r')" == "1" ]]

for attempt in $(seq 1 30); do
  if adb shell cmd package list packages >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
adb shell cmd package list packages >/dev/null

# Generate a real AAC/M4A fixture. The app stores recordings as AAC in an MP4 container;
# a WAV file renamed to .m4a would test the wrong format and make MediaPlayer reject it.
if ! command -v ffmpeg >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y ffmpeg
fi
mkdir -p app/src/androidTest/assets
ffmpeg -y -hide_banner -loglevel error \
  -f lavfi -i "sine=frequency=440:sample_rate=44100:duration=8" \
  -c:a aac -b:a 96k -movflags +faststart \
  app/src/androidTest/assets/playback_fixture.m4a
test -s app/src/androidTest/assets/playback_fixture.m4a
ffprobe -v error -select_streams a:0 -show_entries stream=codec_name -of csv=p=0 \
  app/src/androidTest/assets/playback_fixture.m4a | grep -qx aac

timeout 15m ./gradlew connectedDebugAndroidTest --stacktrace --console=plain
