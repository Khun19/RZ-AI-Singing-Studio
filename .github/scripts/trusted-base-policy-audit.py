#!/usr/bin/env python3
"""YAML-aware fail-closed policy checker."""
from pathlib import Path
import re
import sys
try:
    import yaml
    from yaml.tokens import AliasToken, AnchorToken
except ImportError as exc:
    print(f"FAIL: PyYAML unavailable: {exc}")
    sys.exit(2)

def reject(message):
    print("FAIL: " + message)
    sys.exit(1)
def require(ok, message):
    if not ok: reject(message)
    print("PASS: " + message)
def walk(value, path="$"):
    if isinstance(value, dict):
        for key, child in value.items():
            yield path, key, child
            yield from walk(child, path + "." + str(key))
    elif isinstance(value, list):
        for i, child in enumerate(value):
            yield from walk(child, path + f"[{i}]")

source = Path(sys.argv[1]).read_text(encoding="utf-8")
try:
    tokens = list(yaml.scan(source, Loader=yaml.BaseLoader))
    documents = list(yaml.load_all(source, Loader=yaml.BaseLoader))
except yaml.YAMLError as exc:
    reject("YAML parse error: " + str(exc))
require(len(documents) == 1 and isinstance(documents[0], dict), "single YAML mapping document")
require(not any(isinstance(t, (AnchorToken, AliasToken)) for t in tokens),
        "anchors and aliases prohibited")
data = documents[0]
require(not any(str(k) == "<<" for _, k, _ in walk(data)), "YAML merge keys prohibited")
permissions = data.get("permissions")
require(isinstance(permissions, dict), "top-level permissions mapping exists")
require(permissions == {"contents": "read", "pull-requests": "read"},
        "top-level permissions are read-only")
jobs = data.get("jobs")
require(isinstance(jobs, dict) and bool(jobs), "jobs mapping exists")
for name, job in jobs.items():
    require(isinstance(job, dict), f"job {name} is a mapping")
    require("permissions" not in job, f"job {name} has no permissions override")
for path, key, value in walk(data):
    if str(value).lower() == "write-all": reject(f"write-all permission at {path}.{key}")
    if str(key) in {"contents", "pull-requests", "actions", "issues", "checks", "statuses"}:
        require(value != "write", f"no write grant at {path}.{key}")

trigger = data.get("on")
require(isinstance(trigger, dict) and isinstance(trigger.get("pull_request"), dict),
        "pull_request trigger exists")
branches = trigger["pull_request"].get("branches", [])
require(isinstance(branches, list) and "main" in branches, "trigger targets main")
scripts = []
for name, job in jobs.items():
    guard = str(job.get("if", ""))
    require("github.event.pull_request.base.ref == 'main'" in guard, f"{name}: main guard")
    require("github.event.pull_request.head.repo.full_name == github.repository" in guard,
            f"{name}: same-repository guard")
    require("github.event.pull_request.draft == false" in guard, f"{name}: draft guard")
    steps = job.get("steps", [])
    require(isinstance(steps, list) and bool(steps), f"{name}: steps exist")
    for index, step in enumerate(steps):
        require(isinstance(step, dict), f"{name}: step {index} is mapping")
        if "run" in step: scripts.append(str(step["run"]))
        env = step.get("env", {})
        if isinstance(env, dict):
            for key in ("EXPECTED_HEAD_SHA", "EXPECTED_BASE_SHA"):
                if key in env:
                    require("github.event.pull_request." in str(env[key]), f"{key}: event-bound")
combined = "\n".join(scripts)
require("EXPECTED_HEAD_SHA" in combined and "EXPECTED_BASE_SHA" in combined,
        "expected head/base SHA values used")
require(bool(re.search(r"\[\[.*EXPECTED_HEAD_SHA.*\[0-9a-f\].*40", combined, re.S)),
        "head SHA format validated")
require(bool(re.search(r"\[\[.*EXPECTED_BASE_SHA.*\[0-9a-f\].*40", combined, re.S)),
        "base SHA format validated")
for token in (".state == \"open\"", ".base.ref == \"main\"", ".draft == false",
              ".head.repo.full_name == $repo", ".head.sha == $head", ".base.sha == $base"):
    require(token in combined, "API revalidation contains " + token)

patterns = {
 "dynamic evaluator": r"(?i)\b(eval|exec)\b",
 "shell -c": r"(?i)\b(?:bash|sh|dash|zsh)\s+-c\b",
 "command substitution": r"\$\(|\$\{|\x60",
 "base64-to-shell pipe": r"(?is)(?:base64|openssl\s+enc)[^|]*\|\s*(?:ba)?sh\b",
 "merge CLI": r"(?i)\bgh\s+pr\s+merge\b",
 "merge API": r"(?i)\bgh\s+api\b[^\n]*(?:/merge|merge\?)",
 "shell continuation": r"(?m)\\\s*$",
 "indirect gh alias": r"(?im)^\s*[A-Za-z_]\w*\s*=\s*['\"]?gh['\"]?\s*(?:;|$)|\$\w+\s+(?:pr|api)\b",
}
for label, pattern in patterns.items():
    require(not re.search(pattern, combined), "no " + label)
print("RESULT: YAML-aware policy validation PASS")
