#!/usr/bin/env python3
"""Conservative lexical policy screen; not a YAML or shell semantic verifier."""
from pathlib import Path
import re
import sys

raw = Path(sys.argv[1]).read_text(encoding="utf-8")
lines = raw.splitlines()
# Join shell continuations so split commands cannot evade simple command checks.
normalized = raw.replace("\\\n", " ")
checks = {
    "top-level read-only permissions": any(
        lines[i:i + 3] == ["permissions:", "  contents: read", "  pull-requests: read"]
        for i in range(max(0, len(lines) - 2))
    ),
    "no write-all permission declaration": not re.search(r"(?im)^\s*permissions:\s*write-all\s*$", raw),
    "no job-level permissions overrides": not re.search(r"(?m)^\s{4,}permissions:\s*$", raw),
    "no explicit token write grants": not re.search(
        r"(?im)^\s*(contents|pull-requests|actions|issues|checks|statuses):\s*write\s*$", raw
    ),
    "no merge CLI invocation": not re.search(r"(?i)\bgh\s+pr\s+merge\b", normalized),
    "no merge API invocation": not re.search(r"(?i)\bgh\s+api\b[^\n]*(?:/merge|merge\?)", normalized),
    "no ambiguous shell command continuation": not re.search(r"(?m)\\\s*$", raw),
    "expected head/base env validation": all(
        token in raw for token in ("EXPECTED_HEAD_SHA", "EXPECTED_BASE_SHA", "^[0-9a-f]{40}$")
    ),
    "identity and SHA API verification": all(
        token in raw for token in (".head.repo.full_name == $repo", ".head.sha == $head", ".base.sha == $base")
    ),
    "main branch restriction": '.base.ref == "main"' in raw,
    "draft exclusion": ".draft == false" in raw,
}
for label, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + label)
failed = [label for label, ok in checks.items() if not ok]
print(f"RESULT: {len(checks) - len(failed)}/{len(checks)}")
sys.exit(1 if failed else 0)
