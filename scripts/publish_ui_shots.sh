#!/usr/bin/env bash
# V00: put this run's screenshots on the orphan branch `ui-shots` under <short-sha>/ and write index.md.
# The branch is rewritten as one commit each time and keeps only the newest KEEP runs, so it stays small
# (a full `git clone` of this public repo fetches every branch).
# Usage: bash scripts/publish_ui_shots.sh <shots-dir> <short-sha> <ref-name>   (needs GH_TOKEN and GITHUB_REPOSITORY)
set -euo pipefail
SRC="$1"; SHA="$2"; REF="$3"; KEEP=10
[ -d "$SRC" ] || { echo "no screenshots in $SRC"; exit 0; }
URL="https://x-access-token:${GH_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"
for attempt in 1 2 3; do
  W=$(mktemp -d)
  if git ls-remote --exit-code --heads "$URL" ui-shots > /dev/null; then
    git clone -q --depth 1 --branch ui-shots "$URL" "$W"
  else
    git init -q "$W"; git -C "$W" checkout -q --orphan ui-shots
  fi
  rm -rf "$W/$SHA"; mkdir -p "$W/$SHA"; cp -R "$SRC"/. "$W/$SHA/"
  # Branch copies are 720 px wide (the Actions artifact keeps the full 1080 px).
  python3 - "$W/$SHA" <<'PY' || echo "resize skipped"
import pathlib, sys
from PIL import Image
for p in pathlib.Path(sys.argv[1]).rglob("*.png"):
    im = Image.open(p)
    if im.width > 720:
        im = im.resize((720, round(im.height * 720 / im.width)), Image.LANCZOS)
    im.convert("RGB").save(p, optimize=True)
PY
  date -u +%Y-%m-%dT%H:%M:%SZ > "$W/$SHA/.stamp"; echo "$REF" > "$W/$SHA/.ref"
  # Keep the newest KEEP runs.
  ls -d "$W"/*/ 2>/dev/null | while read -r d; do echo "$(cat "$d/.stamp" 2>/dev/null || echo 0) $d"; done \
    | sort -r | tail -n +$((KEEP + 1)) | cut -d' ' -f2- | xargs -r rm -rf
  {
    echo "# UI screenshots (newest first)"; echo
    echo "Produced by .github/workflows/ui-screens.yml. Layout: \`<short-sha>/<flow>/<step>-<variant>.png\`; \`results.txt\` per run."; echo
    ls -d "$W"/*/ | while read -r d; do echo "$(cat "$d/.stamp") $(basename "$d") $(cat "$d/.ref")"; done | sort -r | while read -r stamp sha ref; do
      echo "## $sha ($ref, $stamp)"; echo
      [ -f "$W/$sha/results.txt" ] && { echo '```'; cat "$W/$sha/results.txt"; echo '```'; echo; }
      (cd "$W" && find "$sha" -name '*.png' | sort | sed 's|.*|- [&](&)|')
      echo
    done
  } > "$W/index.md"
  cd "$W"
  git checkout -q --orphan publish
  git add -A
  git -c user.name="ui-screens" -c user.email="41898282+github-actions[bot]@users.noreply.github.com" commit -q -m "ui-shots: $SHA ($REF)"
  if git push -q -f "$URL" publish:ui-shots; then echo "published $SHA"; exit 0; fi
  cd /; rm -rf "$W"; sleep $((attempt * 5))
done
echo "could not publish"; exit 1
