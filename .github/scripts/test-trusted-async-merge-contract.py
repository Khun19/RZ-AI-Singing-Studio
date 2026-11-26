#!/usr/bin/env python3
import importlib.util,tempfile,unittest
from pathlib import Path
D=Path(__file__).resolve().parent
S=importlib.util.spec_from_file_location("checker",D/"trusted-async-merge-contract.py");C=importlib.util.module_from_spec(S);S.loader.exec_module(C)
SAFE=r'''#!/bin/bash
EXPECTED_HEAD_SHA="${EXPECTED_HEAD_SHA:?}"
[[ "$EXPECTED_HEAD_SHA" =~ ^[0-9a-f]{40}$ ]]
[[ "$EXPECTED_BASE_SHA" =~ ^[0-9a-f]{40}$ ]]
validate_current_pr(){ jq -e --arg repo "$GITHUB_REPOSITORY" --arg head "$EXPECTED_HEAD_SHA" --arg base "$EXPECTED_BASE_SHA" '.state == "open" and .merged == false and .base.ref == "main" and .head.repo.full_name == $repo and .head.sha == $head and .base.sha == $base'; }
verify_actual_merge(){ jq -e --arg head "$EXPECTED_HEAD_SHA" '.state == "closed" and .merged == true and .head.sha == $head'; jq -e --arg sha "$expected_merge_sha" '.merge_commit_sha == $sha'; }
reject(){ jq -e '.details.bypass_rules == true'; }
require(){ jq -e '.details.bypass_rules == false'; }
MAX_POLLS=12
MAX_SUBMISSIONS=2
for ((attempt=1; attempt<=MAX_POLLS; attempt++)); do :; done
for ((submission=1; submission<=MAX_SUBMISSIONS; submission++)); do :; done
curl --write-out '%{http_code}'
case "$HTTP_STATUS" in
409)
 poll
 ;;
esac
case "$status" in
pending) :;; enqueued) :;; merged) :;; failed) :;;
esac
payload="$(jq -cn --arg sha "$EXPECTED_HEAD_SHA" '{merge_method:"squash",merge_action:"default",bypass_rules:false,sha:$sha}')"
'''
class T(unittest.TestCase):
 def check(self,x):
  with tempfile.TemporaryDirectory() as d:
   p=Path(d)/"candidate.sh";p.write_text(x);return C.main([str(p)])
 def test_valid(self):self.assertEqual(self.check(SAFE),0)
 def test_comments_cannot_pass(self):self.assertNotEqual(self.check("\n".join("# "+x for x in SAFE.splitlines())),0)
 def test_bypass_false_missing(self):self.assertNotEqual(self.check(SAFE.replace(".details.bypass_rules == false","unknown")),0)
 def test_base_sha_missing(self):self.assertNotEqual(self.check(SAFE.replace("and .base.sha == $base","")),0)
 def test_merge_confirmation_missing(self):self.assertNotEqual(self.check(SAFE.replace(".merged == true",".merged == false")),0)
 def test_poll_unbounded(self):self.assertNotEqual(self.check(SAFE.replace("attempt<=MAX_POLLS","attempt<=99999")),0)
 def test_409_missing(self):self.assertNotEqual(self.check(SAFE.replace("409)","418)")),0)
if __name__=="__main__":unittest.main(verbosity=2)
