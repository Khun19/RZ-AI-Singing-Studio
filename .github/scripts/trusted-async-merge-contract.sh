#!/usr/bin/env bash
set -euo pipefail
d="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec python3 "$d/trusted-async-merge-contract.py" "${1:?candidate script path required}"
