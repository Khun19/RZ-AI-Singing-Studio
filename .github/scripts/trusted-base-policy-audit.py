#!/usr/bin/env python3
"""Fail-closed static audit of an untrusted candidate workflow; never execute it."""
from pathlib import Path
import re
import sys

text = Path(sys.argv[1]).read_text(encoding="utf-8")
checks = {
    "top-level read-only permissions": bool(re.search(r"(?m)^permissions:\s*\n\s+contents:\s*read\s*\n\s+pull-requests:\s*read\s*$", text)),
    "no write permissions": not re.search(r"(?m)^\s*(contents|pull-requests):\s*write\s*$", text),
    "no merge CLI invocation": not re.search(r"(?i)\bgh\s+pr\s+merge\b", text),
    "no merge endpoint invocation": not re.search(r"(?i)\bgh\s+api\b[^\n]*(?:/merge|merge\?)", text),
    "expected head/base env validation": all(x in text for x in ("EXPECTED_HEAD_SHA", "EXPECTED_BASE_SHA", "^[0-9a-f]{40}$")),
    "identity and SHA API verification": all(x in text for x in (".head.repo.full_name == $repo", ".head.sha == $head", ".base.sha == $base")),
    "main branch restriction": '.base.ref == "main"' in text,
    "draft exclusion": ".draft == false" in text,
}
for label, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + label)
failed = [label for label, ok in checks.items() if not ok]
print(f"RESULT: {len(checks)-len(failed)}/{len(checks)}")
sys.exit(1 if failed else 0)
