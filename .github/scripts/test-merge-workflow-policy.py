#!/usr/bin/env python3
"""Independent policy regression checks for the PR-triggered merge workflow.

This harness checks workflow policy only; it never executes candidate scripts,
calls merge APIs, or uses a write token.
"""
from pathlib import Path
import sys

path = Path(".github/workflows/automatic-pr-merge.yml")
text = path.read_text(encoding="utf-8")

checks = {
    "contents permission is read-only": "contents: read" in text and "contents: write" not in text,
    "pull-request permission is read-only": "pull-requests: read" in text and "pull-requests: write" not in text,
    "no native merge command": "gh pr merge" not in text,
    "no merge API request": "merge_action" not in text and "merge_method" not in text,
    "event head SHA is checked": 'EXPECTED_HEAD_SHA' in text and '^[0-9a-f]{40}$' in text,
    "event base SHA is checked": 'EXPECTED_BASE_SHA' in text and '^[0-9a-f]{40}$' in text,
    "PR identity is revalidated": '.head.repo.full_name == $repo' in text and '.head.sha == $head' in text,
    "base ref is constrained": '.base.ref == "main"' in text,
    "draft PR is excluded": '.draft == false' in text,
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + name)
print(f"RESULT: {len(checks)-len(failed)}/{len(checks)} checks passed")
sys.exit(1 if failed else 0)
