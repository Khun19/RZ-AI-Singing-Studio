#!/usr/bin/env python3
"""YAML-aware fail-closed policy checker."""
from pathlib import Path
import re
import sys
import textwrap
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
require(permissions == {"contents": "write", "pull-requests": "write"},
        "candidate has only the exact permissions required for native auto-merge")
jobs = data.get("jobs")
require(isinstance(jobs, dict) and len(jobs) == 1, "exactly one job is permitted")
for name, job in jobs.items():
    require(isinstance(job, dict), f"job {name} is a mapping")
    require("permissions" not in job, f"job {name} has no permissions override")
for path, key, value in walk(data):
    if str(value).lower() == "write-all": reject(f"write-all permission at {path}.{key}")

trigger = data.get("on")
require(isinstance(trigger, dict) and set(trigger.keys()) == {"pull_request"},
        "only pull_request trigger is permitted")
require(isinstance(trigger.get("pull_request"), dict), "pull_request trigger exists")
branches = trigger["pull_request"].get("branches", [])
require(isinstance(branches, list) and "main" in branches, "trigger targets main")
scripts = []
for name, job in jobs.items():
    guard = " ".join(str(job.get("if", "")).split())
    expected_guard = ("github.event.pull_request.base.ref == 'main' && "
                      "github.event.pull_request.head.repo.full_name == github.repository && "
                      "github.event.pull_request.draft == false")
    require(guard == expected_guard, f"{name}: exact fail-closed PR guard")
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
require(set(data.keys()) == {"name", "on", "permissions", "concurrency", "jobs"},
        "workflow uses only the approved top-level structure")
job = next(iter(jobs.values()))
require(set(job.keys()) == {"name", "if", "runs-on", "steps"},
        "job uses only the approved execution structure")
steps = job["steps"]
require(len(steps) == 2, "exactly two approved steps are permitted")
expected_envs = [
    {
        "GH_TOKEN": "$" + "{{ github.token }}",
        "PR_NUMBER": "$" + "{{ github.event.pull_request.number }}",
        "EXPECTED_HEAD_SHA": "$" + "{{ github.event.pull_request.head.sha }}",
        "EXPECTED_BASE_SHA": "$" + "{{ github.event.pull_request.base.sha }}",
    },
    {
        "GH_TOKEN": "$" + "{{ github.token }}",
        "PR_NUMBER": "$" + "{{ github.event.pull_request.number }}",
        "EXPECTED_HEAD_SHA": "$" + "{{ github.event.pull_request.head.sha }}",
    },
]
expected_scripts = [
r"""set -euo pipefail
[[ "$PR_NUMBER" =~ ^[1-9][0-9]*$ ]]
[[ "$EXPECTED_HEAD_SHA" =~ ^[0-9a-f]{40}$ ]]
[[ "$EXPECTED_BASE_SHA" =~ ^[0-9a-f]{40}$ ]]
pr="$(gh api "repos/$GITHUB_REPOSITORY/pulls/$PR_NUMBER")"
jq -e --arg repo "$GITHUB_REPOSITORY" --arg head "$EXPECTED_HEAD_SHA" --arg base "$EXPECTED_BASE_SHA" '
  .state == "open" and .merged == false and .draft == false
  and .base.ref == "main"
  and .head.repo.full_name == $repo
  and .head.sha == $head
  and .base.sha == $base
' <<<"$pr" >/dev/null
echo "PR identity and exact event SHAs verified." """,
r"""set -euo pipefail
gh pr merge "$PR_NUMBER" --repo "$GITHUB_REPOSITORY" --auto --squash --match-head-commit "$EXPECTED_HEAD_SHA"
echo "GitHub native auto-merge enabled. GitHub will merge only after all active ruleset requirements pass." """
]
for index, step in enumerate(steps):
    require(set(step.keys()) == {"name", "env", "shell", "run"},
            f"step {index} uses only approved keys")
    require(step.get("shell") == "bash", f"step {index} uses bash explicitly")
    require(step.get("env") == expected_envs[index], f"step {index} has exact event-bound environment")
    actual_script = textwrap.dedent(str(step.get("run", ""))).strip()
    require(actual_script == textwrap.dedent(expected_scripts[index]).strip(),
            f"step {index} matches the reviewed command script exactly")

combined = "\n".join(scripts)
shell_flat = re.sub(r"['\"]", "", combined)
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
 "unapproved command substitution": r"\$\((?!\s*gh\s+api\b)|\$\{|\x60",
 "base64-to-shell pipe": r"(?is)(?:base64|openssl\s+enc)[^|]*\|\s*(?:ba)?sh\b",
 "merge API": r"(?i)\bgh\s+api\b[^\n]*(?:/merge|merge\?)",
 "shell continuation": r"(?m)\\\s*$",
 "indirect gh alias": r"(?im)^\s*[A-Za-z_]\w*\s*=\s*['\"]?gh['\"]?\s*(?:;|$)|\$\w+\s+(?:pr|api)\b",
}
for label, pattern in patterns.items():
    require(not re.search(pattern, shell_flat), "no " + label)
merge_lines = [line.strip() for line in shell_flat.splitlines()
               if re.search(r"(?i)\bgh\s+pr\s+merge\b", line)]
safe_merge = "gh pr merge $PR_NUMBER --repo $GITHUB_REPOSITORY --auto --squash --match-head-commit $EXPECTED_HEAD_SHA"
require(merge_lines == [safe_merge],
        "only exact-head native squash auto-merge command is permitted")
print("RESULT: YAML-aware policy validation PASS")
