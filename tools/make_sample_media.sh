#!/usr/bin/env bash
# Generate the starter pack's sample demo media (T03 scaffold, spec §8).
#
# Produces light, muted, seamlessly-loopable MP4 loops for demo purposes:
#   content/starter/media/pushup-demo.mp4 — bar oscillating at 0.5 Hz (push-up cadence)
#   content/starter/media/plank-demo.mp4  — static hold with a fixed "breathing" marker
#
# These are SYNTHETIC placeholder loops (ffmpeg lavfi, no footage, no audio,
# no trackers). They are NOT reviewed training content — the starter pack
# ships exercises.json with reviewState=DRAFT until the review pipeline
# (T09/T10) replaces these with reviewed sources.
#
# Loop seam: push-up motion = sin(2*PI*t/2) over 4 s = exactly 2 full
# cycles, so frame 0 and frame 120 match (seamless).
#
# Deterministic: -fflags +bitexact -flags:v +bitexact keeps hashes stable
# across re-runs (validate_content.py pins sha256).
set -euo pipefail
cd "$(dirname "$0")/.."

OUT=content/starter/media
mkdir -p "$OUT"

# 640x360, 30 fps, 4 s, h264 yuv420p, muted (no audio stream), faststart.
COMMON=(-c:v libx264 -preset veryfast -crf 28 -pix_fmt yuv420p
        -fflags +bitexact -flags:v +bitexact -an -movflags +faststart)

# pushup: dark slate background + a "body" bar oscillating at 0.5 Hz
ffmpeg -hide_banner -loglevel error -y \
  -f lavfi -i "color=c=0x0f172a:s=640x360:d=4:r=30" \
  -vf "drawbox=x=160:y='236-44*sin(2*PI*t/2)':w=320:h=28:color=0xe2e8f9:t=fill,\
drawbox=x=200:y='252-44*sin(2*PI*t/2)':w=240:h=10:color=0x94a3b8:t=fill,\
drawtext=text='sample loop (not reviewed)':fontcolor=0x64748b@0.7:x=16:y=330:fontsize=18" \
  -t 4 -r 30 "${COMMON[@]}" "$OUT/pushup-demo.mp4"

# plank: dark green background + static hold bar + fixed breathing marker
ffmpeg -hide_banner -loglevel error -y \
  -f lavfi -i "color=c=0x0f172a:s=640x360:d=4:r=30" \
  -vf "drawbox=x=0:y=0:w=640:h=360:color=0x14532d@0.25:t=fill,\
drawbox=x=160:y=236:w=320:h=28:color=0xe2e8f9:t=fill,\
drawbox=x=200:y=252:w=240:h=10:color=0x86efac:t=fill,\
drawbox=x=300:y=244:w=40:h=12:color=white@0.8:t=fill,\
drawtext=text='sample loop (not reviewed)':fontcolor=0x64748b@0.7:x=16:y=330:fontsize=18" \
  -t 4 -r 30 "${COMMON[@]}" "$OUT/plank-demo.mp4"

echo "wrote:"
ls -la "$OUT"
for f in "$OUT"/*.mp4; do
  echo "$f  sha256=$(sha256sum "$f" | cut -d' ' -f1)  bytes=$(stat -c%s "$f")"
done
