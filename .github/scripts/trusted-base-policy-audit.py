#!/usr/bin/env python3
"""Conservative static policy screen; rejects ambiguous permission/merge constructs."""
from pathlib import Path
import re
import sys

raw = Path(sys.argv[1]).read_text(encoding="utf-8")
# Normalize shell continuation lines before command screening.
text = re.sub(r"\\\s*\n\s*", " ", raw)
checks = {
    "top-level read-only permissions": any(raw.splitlines()[i:i+3] == ["permissions:", "  contents: read", "  pull-requests: read"] for i in range(len(raw.splitlines())),
    "no write-all permission declaration": not re.search(r"(?im)^\s*permissions:\s*write-all\s*$", raw),
    "no job-level permissions overrides": not re.search(r"(?m)^\s{4,}permissions:\s*$", raw),
    "no explicit token write grants": not re.search(r"(?im)^\s*(contents|pull-requests|actions|issues|checks|statuses):\s*write\s*$", raw),
    "no merge CLI invocation": not re.search(r"(?i)\bgh\s+pr\s+merge\b", text),
    "no merge endpoint invocation": not re.search(r"(?i)\bgh\s+api\b[^\n]*(?:/merge|merge\?)", text),
    "no ambiguous shell command continuation": not re.search(r"(?m)\\\s*$", raw),
    "expected head/base env validation": all(x in raw for x in ("EXPECTED_HEAD_SHA", "EXPECTED_BASE_SHA", "^[0-9a-f]{40}$")),
    "identity and SHA API verification": all(x in raw for x in (".head.repo.full_name == $repo", ".head.sha == $head", ".base.sha == $base")),
    "main branch restriction": '.base.ref == "main"' in raw,
    "draft exclusion": ".draft == false" in raw,
}
for label, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + label)
failed = [label for label, ok in checks.items() if not ok]
print(f"RESULT: {len(checks)-len(failed)}/{len(checks)}")
sys.exit(1 if failed else 0)
