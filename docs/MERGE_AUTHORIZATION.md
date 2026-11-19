# Explicit Merge Authorization

## Purpose

Merging is a two-stage decision: GitHub Actions verifies the exact PR revision and its complete evidence; a maintainer then separately authorizes one merge by manually dispatching Authorized Merge Gate.

A green CI run is not merge authorization. Comments, labels, bot reviews and previous authorizations do not grant merge permission.

## One-time authorization

After the PR is ready and all evidence exists:

1. Copy the PR's full 40-character Head SHA and current main Base SHA.
2. Open Actions → Authorized Merge Gate → Run workflow on the default branch.
3. Supply pr_number, expected_head_sha, expected_base_sha, and the phrase:
   MERGE-PR-<number>-<full-head-sha>
4. Dispatch. A successful run authorizes and merges only that exact revision.

Only Khun19 is permitted to dispatch, and the workflow refuses to run unless the selected workflow ref is the trusted `main` branch. The workflow checks the PR is open, non-draft, targets main, belongs to this repository, and still matches both supplied SHAs. It then checks the latest Android CI run for that PR/head, every job conclusion, and required unexpired APK/test evidence artifacts. Immediately before merge it rechecks the PR and uses GitHub's expected-head-SHA merge precondition.

## Evidence requirements

- JVM unit tests
- Android emulator instrumentation tests
- Debug APK build
- Exact APK install/launch on API 31 and API 35
- Non-empty, unexpired artifacts: instrumentation diagnostics, debug APK, API 31 validation, API 35 validation

The active GitHub ruleset remains authoritative. GitHub's merge API will reject a merge if the ruleset or other repository policy blocks it.

## Security boundaries

- This is an explicit, auditable workflow-dispatch authorization, not a fabricated GitHub APPROVE review. PR authors cannot approve their own PR.
- No pull_request_target is used.
- Verification has read-only permissions; write permissions are limited to the final merge job.
- The authorization phrase is bound to one PR and one exact head SHA.
- A changed PR head or base invalidates the authorization.
- CI success alone never triggers a merge.

## Bootstrap limitation

This workflow must first be merged into the default branch before it can be dispatched from Actions. It is proposed in a separate PR so it can be reviewed independently and does not alter PR #1.
