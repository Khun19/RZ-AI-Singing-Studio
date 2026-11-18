#!/usr/bin/env bash
set -Eeuo pipefail

apk_path="${1:?Usage: validate-built-apk.sh <apk-path>}"
evidence_dir="app/build/apk-validation"
mkdir -p "$evidence_dir"
apk_path="$(realpath "$apk_path")"

capture_failure() {
  local exit_code=$?
  trap - ERR
  {
    echo "exit_code=$exit_code"
    echo "apk_path=$apk_path"
    adb devices -l || true
    adb logcat -b all -d -t 5000 || true
    adb shell dumpsys activity activities || true
    adb shell dumpsys package com.rzstudio.singing || true
  } > "$evidence_dir/failure-diagnostics.txt" 2>&1
  exit "$exit_code"
}
trap capture_failure ERR

test -s "$apk_path"
aapt="$ANDROID_HOME/build-tools/35.0.0/aapt"
test -x "$aapt"

"$aapt" dump badging "$apk_path" > "$evidence_dir/apk-badging.txt"
grep -q "package: name='com.rzstudio.singing'" "$evidence_dir/apk-badging.txt"
grep -q "sdkVersion:'31'" "$evidence_dir/apk-badging.txt"
sha256sum "$apk_path" | tee "$evidence_dir/apk-sha256.txt"
stat --printf='apk_size_bytes=%s\n' "$apk_path" > "$evidence_dir/apk-size.txt"
adb shell getprop ro.build.version.sdk | tr -d '\r' > "$evidence_dir/emulator-api.txt"

adb install -r "$apk_path"
adb shell pm path com.rzstudio.singing | tee "$evidence_dir/installed-package-path.txt"
adb shell am force-stop com.rzstudio.singing
adb shell am start -W -n com.rzstudio.singing/.MainActivity | tee "$evidence_dir/launch-result.txt"

for attempt in $(seq 1 30); do
  if adb shell pidof com.rzstudio.singing >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
adb shell pidof com.rzstudio.singing | tee "$evidence_dir/app-process.txt"
adb shell dumpsys activity activities > "$evidence_dir/activity-state.txt"
grep -q "com.rzstudio.singing" "$evidence_dir/activity-state.txt"
adb shell screencap -p /sdcard/rz-ai-singing-studio.png
adb pull /sdcard/rz-ai-singing-studio.png "$evidence_dir/launch-screenshot.png"
adb logcat -b crash -d > "$evidence_dir/crash-log.txt"
if grep -E "FATAL EXCEPTION|Fatal signal" "$evidence_dir/crash-log.txt"; then
  echo "Crash log contains a fatal error" >&2
  exit 1
fi
echo "APK_INSTALL_LAUNCH_VALIDATION=PASS" | tee "$evidence_dir/result.txt"
