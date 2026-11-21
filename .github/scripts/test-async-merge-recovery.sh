#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$BASH_SOURCE")" && pwd)"
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
export HEAD OLD_HEAD BASE MERGE UUID1 UUID2

cat > "$FAKE_CURL" <<'FAKE'
#!/usr/bin/env bash
set -euo pipefail
out="" url="" method="GET"
while (($#)); do
  case "$1" in
    --output) out="$2"; shift 2 ;;
    --write-out|--request|--data|--header) shift 2 ;;
    --silent|--show-error) shift ;;
    *) url="$1"; shift ;;
  esac
done
scenario="$FAKE_SCENARIO"
status=200
body='{}'

if [[ "$url" == */pulls/7/merge-async/* ]]; then
  uuid="\${url##*/}"
  case "$scenario:$uuid" in
    conflict_then_fail_retry_merged:$UUID1)
      body='{"status":"failed","details":{"message":"old expected head was cancelled"}}'
      ;;
    conflict_then_fail_retry_merged:$UUID2)
      polls=0
      [[ -f "$FAKE_CALLS.polls" ]] && polls="$(cat "$FAKE_CALLS.polls")"
      polls=$((polls + 1))
      printf '%s' "$polls" > "$FAKE_CALLS.polls"
      if [[ "$polls" == "1" ]]; then
        body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID2\",\"merge_method\":\"squash\",\"merge_action\":\"default\",\"expected_head_sha\":\"$HEAD\"}}"
      else
        body="{\"status\":\"merged\",\"details\":{\"sha\":\"$MERGE\"}}"
        touch "$FAKE_CALLS.merged"
      fi
      ;;
    expired_request:*)
      status=404
      body='{"message":"Not Found"}'
      ;;
    wrong_merged_head:*)
      body="{\"status\":\"merged\",\"details\":{\"sha\":\"$MERGE\"}}"
      touch "$FAKE_CALLS.merged"
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
    merged_head="$HEAD"
    [[ "$scenario" == "wrong_merged_head" ]] && merged_head="$OLD_HEAD"
    body="{\"state\":\"closed\",\"merged\":true,\"draft\":false,\"head\":{\"sha\":\"$merged_head\"},\"merge_commit_sha\":\"$MERGE\"}"
  else
    body="{\"state\":\"open\",\"merged\":false,\"draft\":false,\"base\":{\"ref\":\"main\",\"sha\":\"$BASE\"},\"head\":{\"repo\":{\"full_name\":\"owner/repo\"},\"sha\":\"$HEAD\"}}"
  fi
elif [[ "$url" == */pulls/7/merge-async ]]; then
  case "$scenario" in
    conflict_then_fail_retry_merged)
      puts=0
      [[ -f "$FAKE_CALLS.puts" ]] && puts="$(cat "$FAKE_CALLS.puts")"
      puts=$((puts + 1))
      printf '%s' "$puts" > "$FAKE_CALLS.puts"
      if [[ "$puts" == "1" ]]; then
        status=409
        body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID1\",\"merge_method\":\"squash\",\"merge_action\":\"default\"}}"
      else
        status=202
        body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID2\",\"merge_method\":\"squash\",\"merge_action\":\"default\",\"expected_head_sha\":\"$HEAD\"}}"
      fi
      ;;
    options_mismatch)
      status=409
      body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID1\",\"merge_method\":\"merge\",\"merge_action\":\"default\"}}"
      ;;
    malformed_uuid)
      status=409
      body='{"status":"pending","details":{"uuid":"not-a-uuid","merge_method":"squash","merge_action":"default"}}'
      ;;
    expired_request)
      status=409
      body="{\"status\":\"pending\",\"details\":{\"uuid\":\"$UUID1\",\"merge_method\":\"squash\",\"merge_action\":\"default\"}}"
      ;;
    enqueued)
      status=200
      body='{"status":"enqueued","details":{"message":"already queued"}}'
      ;;
    api_error)
      status=422
      body='{"message":"validation failed"}'
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
    bash "$SCRIPT_DIR/request-async-merge.sh" >"$TMP/out" 2>&1; then
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

run_case conflict_then_fail_retry_merged 0 "409 existing request reconciled, latest SHA retried, actual merge verified"
run_case unknown_status 1 "unknown async status fails closed"
run_case timeout 1 "pending request polling budget exhaustion fails closed"
run_case options_mismatch 1 "existing request with different merge options fails closed"
run_case malformed_uuid 1 "malformed conflict UUID fails closed"
run_case expired_request 1 "expired async request lookup fails closed"
run_case wrong_merged_head 1 "merge result for a different PR head fails closed"
run_case stale_pr 1 "changed PR head fails before submission"
run_case api_error 1 "non-409 API error fails closed"
run_case enqueued 0 "merge-queue enqueue is not reported as merged"

echo "All async merge recovery tests passed."
