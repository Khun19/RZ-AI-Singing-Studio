#!/usr/bin/env python3
"""Trusted-base static policy audit. Candidate content is treated only as data."""
from pathlib import Path
import sys

candidate = Path(sys.argv[1]).read_text(encoding="utf-8")
checks = {
    "read-only contents permission": "contents: read" in candidate and "contents: write" not in candidate,
    "read-only pull request permission": "pull-requests: read" in candidate and "pull-requests: write" not in candidate,
    "no merge command": "gh pr merge" not in candidate,
    "no merge API call": "gh api" not in candidate or "/merge" not in candidate,
    "head SHA validation": "EXPECTED_HEAD_SHA" in candidate and "^[0-9a-f]{40}$" in candidate,
    "base SHA validation": "EXPECTED_BASE_SHA" in candidate and "^[0-9a-f]{40}$" in candidate,
    "API identity/head/base verification": ".head.repo.full_name == $repo" in candidate and ".head.sha == $head" in candidate and ".base.sha == $base" in candidate,
    "main base branch restriction": '.base.ref == "main"' in candidate,
    "draft PR exclusion": ".draft == false" in candidate,
}
for label, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + label)
failed = [label for label, ok in checks.items() if not ok]
print(f"RESULT: {len(checks)-len(failed)}/{len(checks)}")
sys.exit(1 if failed else 0)
