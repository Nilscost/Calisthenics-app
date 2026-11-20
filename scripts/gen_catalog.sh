#!/usr/bin/env bash
# Regenerate the exercise catalog and check the two committed copies agree.
# Run with: bash scripts/gen_catalog.sh
#
# Chain: tools/gen_r8_catalog.py runs tools/gen_starter_catalog.py (base catalog), imports
# docs/23-new-exercises.json, and writes content/starter/catalog.json,
# app/src/main/assets/catalog.json (+ catalog_meta.json copies, clip_meta.json, review reports).
# validate_content.py then checks the content pack. On a clean tree this leaves no git diff.
set -Eeuo pipefail

cd "$(dirname "$0")/.."

python3 tools/gen_r8_catalog.py
python3 tools/validate_content.py content/starter

for name in catalog.json catalog_meta.json; do
  if ! cmp -s "content/starter/$name" "app/src/main/assets/$name"; then
    echo "ERROR: content/starter/$name and app/src/main/assets/$name differ" >&2
    exit 1
  fi
done
echo "catalog copies are byte-identical (catalog.json, catalog_meta.json)"
