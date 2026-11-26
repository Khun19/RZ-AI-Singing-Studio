#!/usr/bin/env python3
import re,sys,subprocess
from pathlib import Path
R={
"payload bypass false":r"\{\s*merge_method\s*:\s*['\"]squash['\"].*?merge_action\s*:\s*['\"]default['\"].*?bypass_rules\s*:\s*false.*?sha\s*:\s*\$sha\s*\}",
"head SHA format":r"EXPECTED_HEAD_SHA.*?\[\[\s*\"\$EXPECTED_HEAD_SHA\"\s*=~\s*\^\[0-9a-f\]\{40\}\$",
"base SHA format":r"EXPECTED_BASE_SHA.*?\[\[\s*\"\$EXPECTED_BASE_SHA\"\s*=~\s*\^\[0-9a-f\]\{40\}\$",
"exact PR head and base":r"\.head\.sha\s*==\s*\$head.*?\.base\.sha\s*==\s*\$base",
"open and unmerged":r"\.state\s*==\s*['\"]open['\"].*?\.merged\s*==\s*false",
"main and same repo":r"\.base\.ref\s*==\s*['\"]main['\"].*?\.head\.repo\.full_name\s*==\s*\$repo",
"actual merge confirmation":r"\.state\s*==\s*['\"]closed['\"].*?\.merged\s*==\s*true.*?\.head\.sha\s*==\s*\$head",
"merge SHA":r"\.merge_commit_sha\s*==\s*\$sha",
"reject bypass true":r"\.details\.bypass_rules\s*==\s*true",
"require bypass false":r"\.details\.bypass_rules\s*==\s*false",
"bounded polls":r"for\s*\(\(\s*attempt\s*=\s*1\s*;\s*attempt\s*<=\s*MAX_POLLS",
"bounded submissions":r"for\s*\(\(\s*submission\s*=\s*1\s*;\s*submission\s*<=\s*MAX_SUBMISSIONS",
"HTTP status":r"--write-out\s+['\"]%\{http_code\}['\"]",
"409 handling":r"409\)\s*\n","pending":r"pending\)","enqueued":r"enqueued\)","merged":r"merged\)","failed":r"failed\)"}
def main(argv=None):
 args=sys.argv[1:] if argv is None else argv
 if len(args)!=1:return 2
 p=Path(args[0])
 if not p.is_file():print("candidate missing",file=sys.stderr);return 2
 s=p.read_text();s="\n".join(x for x in s.splitlines() if not x.lstrip().startswith("#"))
 bad=[k for k,v in R.items() if not re.search(v,s,re.S)]
 if bad:
  [print("FAIL "+x,file=sys.stderr) for x in bad];return 1
 q=subprocess.run(["bash","-n",str(p)],capture_output=True,text=True)
 if q.returncode:print(q.stderr,file=sys.stderr);return 1
 print("PASS: %d assertions; candidate not executed"%len(R));return 0
if __name__=="__main__":raise SystemExit(main())
