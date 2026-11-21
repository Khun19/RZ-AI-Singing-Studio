#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
FAKE_CURL="$TMP/fake-curl"
CALLS="$TMP/calls"
HEAD="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
OLD_HEAD="bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
BASE="cccccccccccccccccccccccccccccccccccccccc"
MERGE="dddddddddddddddddddddddddddddddddddddddd"
UUID1="11111111-1111-4111-8111-111111111111"
UUID2="22222222-2222-4222-8222-222222222222"

cat > "$FAKE_CURL" <<'FAKE'
#!/usr/bin/env bash
set -euo pipefail
out="" method="GET" url="" payload=""
while (($#)); do
  case "$1" in
    --output) out="$2"; shift 2 ;;
    --write-out) shift 2 ;;
    --request) method="$2"; shift 2 ;;
    --data) payload="$2"; shift 2 ;;
    --header|--connect-timeout|--max-time) shift 2 ;;
    --silent|--show-error) shift ;;
    *) url="$1"; shift ;;
  esac
done
scenario="$FAKE_SCENARIO"
count=0
[[ -f "$FAKE_CALLS" ]] && count="$(cat "$FAKE_CALLS")"
count=$((count + 1))
printf '%s' "$count" > "$FAKE_CALLS"
status=200
body='{}'
if [[ "$url" == */pulls/7/merge-async/* ]]; then
  uuid="${url##*/}"
  case "$scenario:$uuid" in
    conflict_then_fail_retry_merged:$UUID1)
      body='{"status":"failed","details":{"message":"old expected head was cancelled"}}'
      ;;
    conflict_then_fail_retry_merged:$UUID2)
      polls="$(grep -c "poll2" "$FAKE_CALLS.poll" 2>/dev/null || echo 0)"
      printf 'poll2\n' >> "$FAKE_CALLS.poll"
      if [[ "$polls" == "0" ]]; then
        body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID2\",\"merge_method\":\"squash\",\"merge_action\":\"default\",\"expected_head_sha\":\"$HEAD\"}}"
      else
        body="{\"status\":\"merged\",\"details\":{\"sha\":\"$MERGE\"}}"
        touch "$FAKE_CALLS.merged"
      fi
      ;;
    unknown_status:*)
      body='{"status":"waiting_forever","details":{}}'
      ;;
    timeout:*)
      body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID2\",\"merge_method\":\"squash\",\"merge_action\":\"default\",\"expected_head_sha\":\"$HEAD\"}}"
      ;;
    *)
      body='{"status":"failed","details":{"message":"unexpected request"}}'
      ;;
  esac
elif [[ "$url" == */pulls/7 ]]; then
  if [[ "$scenario" == "stale_pr" ]]; then
    body="{\"state\":\"open\",\"merged\":false,\"draft\":false,\"base\":{\"ref\":\"main\",\"sha\":\"$BASE\"},\"head\":{\"repo\":{\"full_name\":\"owner/repo\"},\"sha\":\"$OLD_HEAD\"}}"
  elif [[ -f "$FAKE_CALLS.merged" ]]; then
    body="{\"state\":\"closed\",\"merged\":true,\"draft\":false,\"merge_commit_sha\":\"$MERGE\"}"
  else
    body="{\"state\":\"open\",\"merged\":false,\"draft\":false,\"base\":{\"ref\":\"main\",\"sha\":\"$BASE\"},\"head\":{\"repo\":{\"full_name\":\"owner/repo\"},\"sha\":\"$HEAD\"}}"
  fi
elif [[ "$url" == */pulls/7/merge-async ]]; then
  case "$scenario" in
    conflict_then_fail_retry_merged)
      puts="$(grep -c '^put)"
      printf 'put\n' >> "$FAKE_CALLS.methods"
      if [[ "$puts" == "0" ]]; then
        status=409
        body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID1\",\"merge_method\":\"squash\",\"merge_action\":\"default\",\"expected_head_sha\":\"$OLD_HEAD\"}}"
      else
        status=202
        body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID2\",\"merge_method\":\"squash\",\"merge_action\":\"default\",\"expected_head_sha\":\"$HEAD\"}}"
      fi
      ;;
    options_mismatch)
      status=409
      body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID1\",\"merge_method\":\"merge\",\"merge_action\":\"default\",\"expected_head_sha\":\"$HEAD\"}}"
      ;;
    enqueued)
      status=200
      body='{"status":"enqueued","details":{"message":"already queued"}}'
      ;;
    *)
      status=202
      body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID2\",\"merge_method\":\"squash\",\"merge_action\":\"default\",\"expected_head_sha\":\"$HEAD\"}}"
      ;;
  esac
fi
printf '%s' "$body" > "$out"
printf '%s' "$status"
FAKE
chmod +x "$FAKE_CURL"

run_case() {
  local scenario="$1" expected="$2" label="$3" rc=0
  rm -f "$CALLS" "$CALLS".* 
  if env GITHUB_REPOSITORY=owner/repo GH_TOKEN=test PR_NUMBER=7 \
    EXPECTED_HEAD_SHA="$HEAD" EXPECTED_BASE_SHA="$BASE" \
    CURL_BIN="$FAKE_CURL" SLEEP_BIN=true MAX_POLLS=2 POLL_INTERVAL_SECONDS=0 \
    MAX_SUBMISSIONS=2 FAKE_SCENARIO="$scenario" FAKE_CALLS="$CALLS" \
    bash "$ROOT/.github/scripts/request-async-merge.sh" >"$TMP/out" 2>&1; then
    rc=0
  else
    rc=$?
  fi
  if [[ "$rc" -ne "$expected" ]]; then
    echo "FAIL: $label (expected exit $expected, got $rc)"
    cat "$TMP/out"
    return 1
  fi
  echo "PASS: $label"
}

run_case conflict_then_fail_retry_merged 0 "409 stale request reconciled, latest exact head retried, actual merge verified"
run_case unknown_status 1 "unknown async status fails closed"
run_case timeout 1 "pending request poll exhaustion fails closed"
run_case options_mismatch 1 "existing request with different merge options fails closed"
run_case stale_pr 1 "changed PR head fails before submission"
run_case enqueued 0 "merge-queue enqueue is accepted but not reported as merged"

echo "All async merge recovery tests passed."
 "$FAKE_CALLS.methods" 2>/dev/null || echo 0)"
      printf 'put\n' >> "$FAKE_CALLS.methods"
      if [[ "$puts" == "0" ]]; then
        status=409
        body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID1\",\"merge_method\":\"squash\",\"merge_action\":\"default\",\"expected_head_sha\":\"$OLD_HEAD\"}}"
      else
        status=202
        body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID2\",\"merge_method\":\"squash\",\"merge_action\":\"default\",\"expected_head_sha\":\"$HEAD\"}}"
      fi
      ;;
    options_mismatch)
      status=409
      body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID1\",\"merge_method\":\"merge\",\"merge_action\":\"default\",\"expected_head_sha\":\"$HEAD\"}}"
      ;;
    enqueued)
      status=200
      body='{"status":"enqueued","details":{"message":"already queued"}}'
      ;;
    *)
      status=202
      body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID2\",\"merge_method\":\"squash\",\"merge_action\":\"default\",\"expected_head_sha\":\"$HEAD\"}}"
      ;;
  esac
fi
printf '%s' "$body" > "$out"
printf '%s' "$status"
FAKE
chmod +x "$FAKE_CURL"

run_case() {
  local scenario="$1" expected="$2" label="$3" rc=0
  rm -f "$CALLS" "$CALLS".* 
  if env GITHUB_REPOSITORY=owner/repo GH_TOKEN=test PR_NUMBER=7 \
    EXPECTED_HEAD_SHA="$HEAD" EXPECTED_BASE_SHA="$BASE" \
    CURL_BIN="$FAKE_CURL" SLEEP_BIN=true MAX_POLLS=2 POLL_INTERVAL_SECONDS=0 \
    MAX_SUBMISSIONS=2 FAKE_SCENARIO="$scenario" FAKE_CALLS="$CALLS" \
    bash "$ROOT/.github/scripts/request-async-merge.sh" >"$TMP/out" 2>&1; then
    rc=0
  else
    rc=$?
  fi
  if [[ "$rc" -ne "$expected" ]]; then
    echo "FAIL: $label (expected exit $expected, got $rc)"
    cat "$TMP/out"
    return 1
  fi
  echo "PASS: $label"
}

run_case conflict_then_fail_retry_merged 0 "409 stale request reconciled, latest exact head retried, actual merge verified"
run_case unknown_status 1 "unknown async status fails closed"
run_case timeout 1 "pending request poll exhaustion fails closed"
run_case options_mismatch 1 "existing request with different merge options fails closed"
run_case stale_pr 1 "changed PR head fails before submission"
run_case enqueued 0 "merge-queue enqueue is accepted but not reported as merged"

echo "All async merge recovery tests passed."
