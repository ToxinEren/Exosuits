#!/usr/bin/env bash
# Regenerate everything: models + atlases -> renders -> contact sheets.
set -euo pipefail
cd "$(dirname "$0")"
[ -d node_modules/three ] || npm install three@0.169.0
python3 build.py "$@"
node render.js "$@"
python3 sheets.py
