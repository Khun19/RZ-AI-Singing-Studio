#!/usr/bin/env bash
set -euo pipefail
candidate="${1:?candidate script path required}"
[[ -f "$candidate" ]] || { echo "Candidate script missing"; exit 1; }
# Static contract audit only: never execute PR-controlled code.
grep -Fq 'bypass_rules:false' "$candidate" || grep -Fq 'bypass_rules\":false' "$candidate" || { echo "Missing explicit bypass=false request"; exit 1; }
grep -Fq 'EXPECTED_HEAD_SHA' "$candidate" || { echo "Missing expected-head binding"; exit 1; }
grep -Fq 'EXPECTED_BASE_SHA' "$candidate" || { echo "Missing expected-base binding"; exit 1; }
grep -Fq 'validate_current_pr' "$candidate" || { echo "Missing PR revalidation"; exit 1; }
grep -Fq 'verify_actual_merge' "$candidate" || { echo "Missing final merge verification"; exit 1; }
grep -Fq 'MAX_POLLS' "$candidate" || { echo "Missing bounded polling"; exit 1; }
grep -Fq 'MAX_SUBMISSIONS' "$candidate" || { echo "Missing bounded submissions"; exit 1; }
grep -Fq 'HTTP_STATUS' "$candidate" || { echo "Missing HTTP status handling"; exit 1; }
grep -Fq '409)' "$candidate" || { echo "Missing 409 reconciliation path"; exit 1; }
grep -Fq 'pending)' "$candidate" || { echo "Missing pending state handling"; exit 1; }
grep -Fq 'enqueued)' "$candidate" || { echo "Missing enqueued state handling"; exit 1; }
grep -Fq 'merged)' "$candidate" || { echo "Missing merged state handling"; exit 1; }
grep -Fq 'failed)' "$candidate" || { echo "Missing failed state handling"; exit 1; }
grep -Fq 'bypass_rules == true' "$candidate" || { echo "Missing bypass-enabled rejection"; exit 1; }
grep -Fq 'bypass_rules == false' "$candidate" || { echo "Missing explicit bypass-disabled verification"; exit 1; }
bash -n "$candidate"
echo "Trusted static contract checks passed; candidate was not executed."
