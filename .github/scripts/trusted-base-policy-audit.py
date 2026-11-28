#!/usr/bin/env python3
"""Conservative lexical policy screen; not a YAML or shell semantic verifier."""
from pathlib import Path
import re
import sys

raw = Path(sys.argv[1]).read_text(encoding="utf-8")
lines = raw.splitlines()
normalized = raw.replace("\\\n", " ")
shell_flat = re.sub(r"[\'\"]", "", normalized)

yaml_features = {
    "no YAML anchors or aliases": not re.search(r"(?m)(?:^|[\s:[,{])[*&][A-Za-z0-9_-]+\b", raw),
    "no YAML merge keys": not re.search(r"(?m)^\s*<<\s*:", raw),
    "no nested permission declarations": not re.search(r"(?m)^\s{1,}permissions\s*:", raw),
}
shell_features = {
    "no dynamic shell evaluators": not re.search(
        r"(?i)\b(eval|exec)\s+|\b(?:bash|sh|dash|zsh)\s+-c\b|\bcommand\s+(?:gh|\$)", normalized
    ),
    "no shell command substitution": not re.search(r"\$\(|\$\{|\x60", normalized),
    "no command-name aliases for gh": not re.search(
        r"(?im)^\s*[A-Za-z_][A-Za-z0-9_]*\s*=\s*['\"]?gh['\"]?\s*(?:;|$)", normalized
    ) and not re.search(r"(?i)\$[A-Za-z_][A-Za-z0-9_]*\s+(?:pr|api)\b", normalized),
    "no encoded payload piped to a shell": not re.search(
        r"(?is)(?:base64|openssl\s+enc|printf|echo)[^\n|]*\|\s*(?:ba)?sh\b", normalized
    ),
    "no merge CLI invocation": not re.search(r"(?i)\bgh\s+pr\s+merge\b", normalized),
    "no merge API invocation": not re.search(r"(?i)\bgh\s+api\b[^\n]*(?:/merge|merge\?)", normalized),
    "no ambiguous shell command continuation": not re.search(r"(?m)\\\s*$", raw),
}
checks = {
    "top-level read-only permissions": any(
        lines[i:i + 3] == ["permissions:", "  contents: read", "  pull-requests: read"]
        for i in range(max(0, len(lines) - 2))
    ),
    "no write-all permission declaration": not re.search(r"(?im)^\s*permissions:\s*write-all\s*$", raw),
    "no explicit token write grants": not re.search(
        r"(?im)^\s*(contents|pull-requests|actions|issues|checks|statuses):\s*write\s*$", raw
    ),
    "expected head/base env validation": all(
        token in raw for token in ("EXPECTED_HEAD_SHA", "EXPECTED_BASE_SHA", "^[0-9a-f]{40}$")
    ),
    "identity and SHA API verification": all(
        token in raw for token in (".head.repo.full_name == $repo", ".head.sha == $head", ".base.sha == $base")
    ),
    "main branch restriction at job guard": bool(
        re.search(r"(?im)^\s*if:.*\.base\.ref\s*==\s*['\"]main['\"]", raw)
    ),
    "main branch restriction in API revalidation": bool(
        re.search(r"(?s)\.state\s*==\s*['\"]open['\"].*?\.base\.ref\s*==\s*['\"]main['\"]", raw)
    ),
    "draft exclusion at job guard": bool(
        re.search(r"(?im)^\s*if:.*\.draft\s*==\s*false", raw)
    ),
    "draft exclusion in API revalidation": bool(
        re.search(r"(?s)\.state\s*==\s*['\"]open['\"].*?\.draft\s*==\s*false", raw)
    ),
}
checks.update(yaml_features)
checks.update(shell_features)
for label, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + label)
failed = [label for label, ok in checks.items() if not ok]
print(f"RESULT: {len(checks) - len(failed)}/{len(checks)}")
sys.exit(1 if failed else 0)
