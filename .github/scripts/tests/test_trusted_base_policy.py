#!/usr/bin/env python3
"""Independent mutation regression suite for the trusted policy checker."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
CHECKER = ROOT / ".github/scripts/trusted-base-policy-audit.py"
SAFE = """name: Safe candidate
permissions:
  contents: read
  pull-requests: read
jobs:
  check:
    if: github.event.pull_request.base.ref == 'main' && github.event.pull_request.draft == false
    steps:
      - env:
          EXPECTED_HEAD_SHA: abc
          EXPECTED_BASE_SHA: def
        run: |
          [[ "$EXPECTED_HEAD_SHA" =~ ^[0-9a-f]{40}$ ]]
          [[ "$EXPECTED_BASE_SHA" =~ ^[0-9a-f]{40}$ ]]
          jq -e '.head.repo.full_name == $repo and .head.sha == $head and .base.sha == $base and .base.ref == "main" and .draft == false'
"""

class PolicyMutationTests(unittest.TestCase):
    def run_checker(self, text):
        with tempfile.TemporaryDirectory() as td:
            f = Path(td) / "candidate.yml"
            f.write_text(text, encoding="utf-8")
            return subprocess.run(["python3", str(CHECKER), str(f)],capture_output=True,text=True)

    def test_safe_baseline_passes(self):
        self.assertEqual(self.run_checker(SAFE).returncode, 0)
    def test_write_all_rejected(self):
        self.assertNotEqual(self.run_checker(SAFE.replace("permissions:\n", "permissions: write-all\n",1)).returncode,0)
    def test_job_permissions_override_rejected(self):
        self.assertNotEqual(self.run_checker(SAFE.replace("  check:\n", "  check:\n    permissions:\n      contents: write\n",1)).returncode,0)
    def test_explicit_write_grant_rejected(self):
        self.assertNotEqual(self.run_checker(SAFE.replace("contents: read", "contents: write",1)).returncode,0)
    def test_merge_cli_rejected(self):
        self.assertNotEqual(self.run_checker(SAFE.replace("jq -e", "gh pr merge 12 --auto\n          jq -e",1)).returncode,0)
    def test_continued_merge_cli_rejected(self):
        mutation=SAFE.replace("jq -e", "gh \\\n            pr merge 12 --auto\n          jq -e",1)
        self.assertNotEqual(self.run_checker(mutation).returncode,0)
    def test_merge_api_rejected(self):
        self.assertNotEqual(self.run_checker(SAFE.replace("jq -e", "gh api repos/x/pulls/12/merge\n          jq -e",1)).returncode,0)
    def test_missing_head_validation_rejected(self):
        self.assertNotEqual(self.run_checker(SAFE.replace('EXPECTED_HEAD_SHA', 'HEAD_SHA',2)).returncode,0)
    def test_identity_verification_removed_rejected(self):
        self.assertNotEqual(self.run_checker(SAFE.replace(".head.repo.full_name == $repo", "true",1)).returncode,0)
    def test_main_restriction_removed_rejected(self):
        self.assertNotEqual(self.run_checker(SAFE.replace('.base.ref == "main"', "true",1)).returncode,0)
    def test_draft_restriction_removed_rejected(self):
        self.assertNotEqual(self.run_checker(SAFE.replace(".draft == false", "true",1)).returncode,0)

if __name__ == "__main__":
    unittest.main(verbosity=2)
