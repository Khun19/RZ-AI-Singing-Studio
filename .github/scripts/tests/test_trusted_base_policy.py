#!/usr/bin/env python3
"""Independent mutation tests for YAML-aware policy validation."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
CHECKER = ROOT / ".github/scripts/trusted-base-policy-audit.py"
SAFE = """name: Safe candidate
on:
  pull_request:
    branches: [main]
permissions:
  contents: read
  pull-requests: read
jobs:
  merge:
    if: github.event.pull_request.base.ref == 'main' && github.event.pull_request.head.repo.full_name == github.repository && github.event.pull_request.draft == false
    steps:
      - env:
          EXPECTED_HEAD_SHA: github.event.pull_request.head.sha
          EXPECTED_BASE_SHA: github.event.pull_request.base.sha
        run: |
          set -euo pipefail
          [[ "$EXPECTED_HEAD_SHA" =~ ^[0-9a-f]{40}$ ]]
          [[ "$EXPECTED_BASE_SHA" =~ ^[0-9a-f]{40}$ ]]
          jq -e '.state == "open" and .base.ref == "main" and .draft == false and .head.repo.full_name == $repo and .head.sha == $head and .base.sha == $base'
"""
class Tests(unittest.TestCase):
    def check(self, text):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "candidate.yml"
            p.write_text(text, encoding="utf-8")
            return subprocess.run(["python3", str(CHECKER), str(p)], capture_output=True, text=True)
    def reject(self, text):
        r = self.check(text)
        self.assertNotEqual(r.returncode, 0, r.stdout + r.stderr)
    def test_safe_baseline(self):
        r = self.check(SAFE)
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
    def test_invalid_yaml(self): self.reject(SAFE + "\ninvalid: [\n")
    def test_multiple_documents(self): self.reject(SAFE + "\n---\nname: second\n")
    def test_nested_permissions(self): self.reject(SAFE.replace("    steps:", "    permissions:\n      contents: write\n    steps:", 1))
    def test_write_all(self): self.reject(SAFE.replace("contents: read", "contents: write-all"))
    def test_write_grant(self): self.reject(SAFE.replace("contents: read", "contents: write"))
    def test_anchor(self): self.reject(SAFE.replace("permissions:", "permissions: &p", 1))
    def test_alias(self): self.reject(SAFE + "\nextra: *p\n")
    def test_merge_key(self): self.reject(SAFE + "\nextra:\n  <<: {x: y}\n")
    def test_wrong_branch(self): self.reject(SAFE.replace("branches: [main]", "branches: [dev]"))
    def test_missing_job_guard(self): self.reject(SAFE.replace("    if: github.event.pull_request.base.ref == 'main' && github.event.pull_request.head.repo.full_name == github.repository && github.event.pull_request.draft == false\n", ""))
    def test_missing_draft_api_check(self): self.reject(SAFE.replace(' and .draft == false', ""))
    def test_missing_head_sha_api_check(self): self.reject(SAFE.replace(" and .head.sha == $head", ""))
    def test_missing_base_sha_api_check(self): self.reject(SAFE.replace(" and .base.sha == $base", ""))
    def test_missing_identity_api_check(self): self.reject(SAFE.replace(" and .head.repo.full_name == $repo", ""))
    def test_missing_head_sha_format(self): self.reject(SAFE.replace('[[ "$EXPECTED_HEAD_SHA" =~ ^[0-9a-f]{40}$ ]]', "echo skipped"))
    def test_merge_cli(self): self.reject(SAFE.replace("set -euo pipefail", "gh pr merge 2 --auto"))
    def test_merge_api(self): self.reject(SAFE.replace("set -euo pipefail", "gh api repos/x/pulls/2/merge"))
    def test_eval(self): self.reject(SAFE.replace("set -euo pipefail", 'eval "gh pr merge 2"'))
    def test_shell_c(self): self.reject(SAFE.replace("set -euo pipefail", 'bash -c "gh pr merge 2"'))
    def test_command_substitution(self): self.reject(SAFE.replace("set -euo pipefail", "X=$(printf gh)"))
    def test_base64_pipe(self): self.reject(SAFE.replace("set -euo pipefail", "base64 -d payload | sh"))
    def test_variable_alias(self): self.reject(SAFE.replace("set -euo pipefail", "GH=gh; $GH pr merge 2"))
    def test_quoted_fragment(self): self.reject(SAFE.replace("set -euo pipefail", "g''h pr merge 2"))
    def test_shell_continuation(self): self.reject(SAFE.replace("set -euo pipefail", "gh \\\n pr merge 2"))
if __name__ == "__main__":
    unittest.main(verbosity=2)
