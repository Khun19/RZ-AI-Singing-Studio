#!/usr/bin/env bash
set -euo pipefail

: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
: "${GH_TOKEN:?GH_TOKEN is required}"
: "${PR_NUMBER:?PR_NUMBER is required}"
: "${EXPECTED_HEAD_SHA:?EXPECTED_HEAD_SHA is required}"
: "${EXPECTED_BASE_SHA:?EXPECTED_BASE_SHA is required}"

[[ "$PR_NUMBER" =~ ^[1-9][0-9]*$ ]] || { echo "Invalid PR number." >&2; exit 2; }
[[ "$EXPECTED_HEAD_SHA" =~ ^[0-9a-f]{40}$ ]] || { echo "Invalid expected head SHA." >&2; exit 2; }
[[ "$EXPECTED_BASE_SHA" =~ ^[0-9a-f]{40}$ ]] || { echo "Invalid expected base SHA." >&2; exit 2; }

CURL_BIN="${CURL_BIN:-curl}"
SLEEP_BIN="${SLEEP_BIN:-sleep}"
MAX_POLLS="${MAX_POLLS:-12}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-5}"
MAX_SUBMISSIONS="${MAX_SUBMISSIONS:-2}"
API_BASE="${GITHUB_API_URL:-https://api.github.com}"
TMP_DIR="$(mktemp -d)"
BODY_FILE="$TMP_DIR/response.json"
trap 'rm -rf "$TMP_DIR"' EXIT

for value in "$MAX_POLLS" "$POLL_INTERVAL_SECONDS" "$MAX_SUBMISSIONS"; do
  [[ "$value" =~ ^[0-9]+$ ]] || { echo "Retry settings must be non-negative integers." >&2; exit 2; }
done
(( MAX_POLLS >= 1 && MAX_SUBMISSIONS >= 1 )) || { echo "MAX_POLLS and MAX_SUBMISSIONS must be at least 1." >&2; exit 2; }

api_request() {
  local method="$1" path="$2" payload="${3:-}" status
  local -a args
  args=(--silent --show-error --connect-timeout 10 --max-time 30
    --output "$BODY_FILE" --write-out '%{http_code}'
    --request "$method"
    --header "Authorization: Bearer $GH_TOKEN"
    --header "Accept: application/vnd.github+json"
    --header "X-GitHub-Api-Version: 2026-03-10")
  if [[ "$method" == "PUT" ]]; then
    args+=(--header "Content-Type: application/json" --data "$payload")
  fi
  if ! status="$("$CURL_BIN" "${args[@]}" "$API_BASE/$path")"; then
    echo "Transport/API client failure for $method $path." >&2
    return 90
  fi
  [[ "$status" =~ ^[0-9]{3}$ ]] || { echo "Missing HTTP status for $path." >&2; return 91; }
  HTTP_STATUS="$status"
}

reject_bypass_enabled() {
  if jq -e '.details.bypass_rules == true' "$BODY_FILE" >/dev/null; then
    echo "Async merge request has bypass_rules=true; refusing to continue." >&2
    return 1
  fi
}

require_bypass_disabled() {
  jq -e '.details.bypass_rules == false' "$BODY_FILE" >/dev/null || {
    echo "Async merge response did not explicitly confirm bypass_rules=false; failing closed." >&2
    return 1
  }
}

validate_current_pr() {
  api_request GET "repos/$GITHUB_REPOSITORY/pulls/$PR_NUMBER" || return
  [[ "$HTTP_STATUS" == "200" ]] || { echo "Unable to revalidate PR (HTTP $HTTP_STATUS)." >&2; return 1; }
  jq -e --arg repo "$GITHUB_REPOSITORY" --arg head "$EXPECTED_HEAD_SHA" --arg base "$EXPECTED_BASE_SHA" '
    .state == "open" and .merged == false and .draft == false
    and .base.ref == "main" and .head.repo.full_name == $repo
    and .head.sha == $head and .base.sha == $base
  ' "$BODY_FILE" >/dev/null || {
    echo "PR identity, state, head SHA, or base SHA changed; refusing to continue." >&2
    return 1
  }
}

verify_actual_merge() {
  local expected_merge_sha="${1:-}"
  api_request GET "repos/$GITHUB_REPOSITORY/pulls/$PR_NUMBER" || return
  [[ "$HTTP_STATUS" == "200" ]] || { echo "Cannot verify final PR state (HTTP $HTTP_STATUS)." >&2; return 1; }
  jq -e --arg head "$EXPECTED_HEAD_SHA" '.state == "closed" and .merged == true and .base.ref == "main" and .head.sha == $head' "$BODY_FILE" >/dev/null || {
    echo "Async result says merged, but GitHub does not confirm merged=true for the exact expected PR head." >&2
    return 1
  }
  if [[ -n "$expected_merge_sha" ]]; then
    jq -e --arg sha "$expected_merge_sha" '.merge_commit_sha == $sha' "$BODY_FILE" >/dev/null || {
      echo "Merge commit SHA does not match the asynchronous result." >&2
      return 1
    }
  fi
  echo "GitHub confirms actual merge: $(jq -r '.merge_commit_sha' "$BODY_FILE")"
}

poll_async_request() {
  local uuid="$1" request_head="$2" bypass_verified="${3:-false}" attempt status details_head details_uuid
  for ((attempt=1; attempt<=MAX_POLLS; attempt++)); do
    api_request GET "repos/$GITHUB_REPOSITORY/pulls/$PR_NUMBER/merge-async/$uuid" || return
    [[ "$HTTP_STATUS" == "200" ]] || {
      echo "Async request lookup failed (HTTP $HTTP_STATUS); state is unknown, failing closed." >&2
      return 1
    }

    status="$(jq -r '.status // empty' "$BODY_FILE")"
    case "$status" in
      pending)
        details_uuid="$(jq -r '.details.uuid // empty' "$BODY_FILE")"
        details_head="$(jq -r '.details.expected_head_sha // empty' "$BODY_FILE")"
        [[ "$details_uuid" == "$uuid" ]] || {
          echo "Pending async request UUID mismatch; failing closed." >&2
          return 1
        }
        [[ "$details_head" =~ ^[0-9a-f]{40}$ ]] || {
          echo "Pending async request omitted a valid expected-head SHA; failing closed." >&2
          return 1
        }
        if [[ -n "$request_head" && "$details_head" != "$request_head" ]]; then
          echo "Pending async request expected-head identity changed; failing closed." >&2
          return 1
        fi
        request_head="$details_head"
        require_bypass_disabled || return 1
        jq -e '.details.merge_method == "squash" and .details.merge_action == "default"' "$BODY_FILE" >/dev/null || {
          echo "Pending async request uses unexpected merge options; failing closed." >&2
          return 1
        }
        echo "Async request $uuid remains pending (poll $attempt/$MAX_POLLS)."
        if (( attempt < MAX_POLLS )); then "$SLEEP_BIN" "$POLL_INTERVAL_SECONDS"; fi
        ;;
      failed)
        [[ "$(jq -r '.details.uuid // empty' "$BODY_FILE")" == "" || "$(jq -r '.details.uuid' "$BODY_FILE")" == "$uuid" ]] || {
          echo "Terminal async request UUID mismatch; failing closed." >&2; return 1;
        }
        [[ "$(jq -r '.details.expected_head_sha // empty' "$BODY_FILE")" == "" || "$(jq -r '.details.expected_head_sha' "$BODY_FILE")" == "$request_head" ]] || {
          echo "Terminal async request head mismatch; failing closed." >&2; return 1;
        }
        echo "Async request $uuid failed: $(jq -r '.details.message // "no failure message supplied"' "$BODY_FILE")"
        return 10
        ;;
      enqueued)
        reject_bypass_enabled || return 1
        if jq -e '.details.bypass_rules == false' "$BODY_FILE" >/dev/null; then
          bypass_verified=true
        fi
        [[ "$bypass_verified" == "true" ]] || {
          echo "Enqueued async request has no verified bypass_rules=false state; failing closed." >&2
          return 1
        }
        if [[ -n "$request_head" && "$request_head" != "$EXPECTED_HEAD_SHA" ]]; then
          echo "Existing async request was enqueued for a different PR head; failing closed." >&2
          return 1
        fi
        validate_current_pr || return 1
        echo "Async request $uuid is enqueued. This is not a merge confirmation."
        return 11
        ;;
      merged)
        reject_bypass_enabled || return 1
        if jq -e '.details.bypass_rules == false' "$BODY_FILE" >/dev/null; then
          bypass_verified=true
        fi
        [[ "$bypass_verified" == "true" ]] || {
          echo "Merged async request has no verified bypass_rules=false state; failing closed." >&2
          return 1
        }
        [[ "$(jq -r '.details.uuid // empty' "$BODY_FILE")" == "" || "$(jq -r '.details.uuid' "$BODY_FILE")" == "$uuid" ]] || {
          echo "Terminal async request UUID mismatch; failing closed." >&2; return 1;
        }
        local merge_sha
        merge_sha="$(jq -r '.details.sha // empty' "$BODY_FILE")"
        [[ "$merge_sha" =~ ^[0-9a-f]{40}$ ]] || {
          echo "Merged result omitted a valid merge commit SHA; failing closed." >&2
          return 1
        }
        verify_actual_merge "$merge_sha"
        return
        ;;
      *)
        echo "Unknown async request status '$status'; failing closed." >&2
        return 1
        ;;
    esac
  done
  echo "Async request polling budget exhausted; leaving state unresolved and failing closed." >&2
  return 1
}

validate_current_pr

for ((submission=1; submission<=MAX_SUBMISSIONS; submission++)); do
  validate_current_pr
  payload="$(jq -cn --arg sha "$EXPECTED_HEAD_SHA" '{merge_method:"squash",merge_action:"default",bypass_rules:false,sha:$sha}')"
  api_request PUT "repos/$GITHUB_REPOSITORY/pulls/$PR_NUMBER/merge-async" "$payload" || exit $?

  reject_bypass_enabled || exit 1
  case "$HTTP_STATUS" in
    202)
      uuid="$(jq -r '.details.uuid // empty' "$BODY_FILE")"
      request_head="$(jq -r '.details.expected_head_sha // empty' "$BODY_FILE")"
      [[ "$uuid" =~ ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$ ]] || {
        echo "202 response did not include a valid async request UUID." >&2; exit 1;
      }
      if [[ -n "$request_head" && "$request_head" != "$EXPECTED_HEAD_SHA" ]]; then
        echo "202 response expected-head SHA does not match this event." >&2; exit 1;
      fi
      if poll_async_request "$uuid" "$request_head" true; then exit 0; else poll_result=$?; fi
      if [[ "$poll_result" -eq 11 ]]; then exit 0; fi
      if [[ "$poll_result" -ne 10 ]]; then
        echo "Async request did not reach a safely recoverable failed state." >&2
        exit 1
      fi
      ;;
    200)
      require_bypass_disabled || exit 1
      status="$(jq -r '.status // empty' "$BODY_FILE")"
      case "$status" in
        merged)
          merge_sha="$(jq -r '.details.sha // empty' "$BODY_FILE")"
          [[ "$merge_sha" =~ ^[0-9a-f]{40}$ ]] || { echo "200 merged response omitted merge SHA." >&2; exit 1; }
          verify_actual_merge "$merge_sha"
          exit $?
          ;;
        enqueued)
          validate_current_pr
          echo "PR is already in the merge queue. Enqueued is not merged."
          exit 0
          ;;
        *)
          echo "Unexpected HTTP 200 async merge status '$status'; failing closed." >&2
          exit 1
          ;;
      esac
      ;;
    409)
      uuid="$(jq -r '.details.uuid // empty' "$BODY_FILE")"
      request_head="$(jq -r '.details.expected_head_sha // empty' "$BODY_FILE")"
      [[ "$uuid" =~ ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$ ]] || {
        echo "409 response did not include a valid existing request UUID; failing closed." >&2; exit 1;
      }
      if [[ -n "$request_head" && ! "$request_head" =~ ^[0-9a-f]{40}$ ]]; then
        echo "409 response supplied an invalid existing request head SHA; failing closed." >&2
        exit 1
      fi
      jq -e '.details.merge_method == "squash" and .details.merge_action == "default"' "$BODY_FILE" >/dev/null || {
        echo "Existing async request uses unexpected merge options; failing closed." >&2; exit 1;
      }
      bypass_verified=false
      if jq -e '.details.bypass_rules == false' "$BODY_FILE" >/dev/null; then
        bypass_verified=true
      fi
      echo "409 conflict references existing request $uuid for head $request_head; reconciling it."
      if poll_async_request "$uuid" "$request_head" "$bypass_verified"; then exit 0; else poll_result=$?; fi
      if [[ "$poll_result" -eq 11 ]]; then exit 0; fi
      if [[ "$poll_result" -ne 10 ]]; then
        echo "Existing request did not reach a safely recoverable failed state." >&2
        exit 1
      fi
      ;;
    *)
      echo "Async merge request returned HTTP $HTTP_STATUS; failing closed." >&2
      cat "$BODY_FILE" >&2
      exit 1
      ;;
  esac

  if (( submission < MAX_SUBMISSIONS )); then
    validate_current_pr
    "$SLEEP_BIN" "$((submission * 5))"
  fi
done

echo "Async merge recovery submission budget exhausted." >&2
exit 1
