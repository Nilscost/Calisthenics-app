# Scripted motion-quality pilot: archer push-up and pull-up

Scope: `pushup-archer` and `pullup-full`. The remaining 531 exercises have no
movement-identity contract yet. No paid generation or model API calls are used.
The renderer remains a stylized teaching mannequin, not a human simulation.

## What changed

The archer uses fixed wide hands, an extended assisting arm, and a continuous
plank pivoting through planted toes. A small scalar constraint solver keeps the
assisting arm extended through the descent. A leg/foot intersection constraint
keeps toe anchors planted without stretching limbs or switching ankle branches.
It shows a right and left repetition, matching the catalog's alternating-side
instructions. Fingers turn outward. The camera is raised toward the front so
the sideways movement and two different elbow angles can be read.

The pull-up uses a controlled mildly arched style: shoulder initiation, an
intermediate pull, upper-back extension toward the top, and controlled return.
The torso is no longer almost vertical throughout. The overhand grip close-up
from the preceding pilot is preserved. These checks describe this authored
style; they do not reject all legitimate hollow-body pull-ups as incorrect.

Both pilots use an optional continuous torso mesh with hip, waist, ribcage and
shoulder contours, matching joint material, clearer body contrast and face
orientation. It is our own script-generated geometry. Other exercises retain
their current render style and installed assets.

## Cheap checks before rendering

`tools/blender/motion_contracts.json` stores engineering screening tolerances.
`motion_quality.py` measures the entire source timeline: elbow asymmetry,
sideways chest travel, body line, fixed hands/toes, torso inclination, shoulder
movement, grip, loop closure and excessive joint speed. Reach and floor checks
remain separate. Bounds are not clinical limits or an exact population model.

`motion_review.py` is the batch entry point. It emits a compact summary, detailed
JSON for machines, and optional **failure-only** multi-view contact sheets. It
stages source joints only for passing scoped exercises, and does not rewrite
identical data. A whole-catalog scan reports `pass: 2, unscoped: 531` rather than
pretending every exercise has been checked for movement identity.

```sh
# PY is the isolated Python from tools/blender/FORM_REVIEW.md.
"$PY" tools/blender/motion_review.py --all \
  --output work/motion-quality-pilot/batch-audit.json \
  --failure-sheets work/motion-failures --write
```

The exact pre-fix poses are retained as compressed test fixtures. The tests
reject the actual old archer and old vertical pull-up, a standard push-up
relabelled as an archer, a bent assisting elbow, sliding hands, wrong grip,
teleporting joints and nonfinite geometry. Installation independently reruns the
motion checks, checks current pose source equality, and requires current render
provenance. Passing a stale render or old pose through is not sufficient.

## Reference extraction and comparisons

`tools/blender/motion_references.json` records three videos per exercise,
selected demonstration intervals, candidate repetition intervals, approximate
camera categories and source attribution. Archer references: FitnessFAQs,
Minus The Gym and Calisthenicmovement. Pull-up references: FitnessFAQs,
Calisthenicmovement and Jeff Nippard. Source videos remain in ignored `work/`.

```sh
"$PY" tools/blender/pose_compare.py extract-references \
  --manifest tools/blender/motion_references.json \
  --pose-model work/form-reference-pilot/pose_landmarker_full.task \
  --hand-model work/form-reference-pilot/hand_landmarker.task
blender -b --python-exit-code 1 -P tools/blender/reference_projection.py -- \
  --manifest tools/blender/motion_references.json
"$PY" tools/blender/pose_compare.py compare \
  --manifest tools/blender/motion_references.json --exercise pushup-archer \
  --joints work/clips-bakeoff/pushup-archer/joints.json \
  --candidate work/motion-quality-pilot/G8BvsXteJ2s-archer-projection.json \
  --output work/motion-quality-pilot/pushup-archer-reference-report.json
```

Repeat comparison for `pullup-full` with its joints and
`GBcUcATb8RQ-pullup-projection.json`. The projection command produces diagnostic
camera views numerically, without rendering extra videos. Cameras are manually
estimated categories, **not calibrated camera reconstructions**. Measurements
remain screening evidence; individual anatomy, perspective, occlusion and style
still matter. Each reference gets a separate result, without averaging them
into an automatic correct-form grade. Candidate cycles are trimmed to match
one versus two repetitions. Stale projection hashes are rejected.

Extraction caches depend on source-video bytes, model bytes, selected interval,
sampling rate, confidence, extraction code and dependency versions. A second
unchanged run produces six cache hits and performs no landmark inference. Keep
these cached measurements outside model context; send only failures, outliers
and selected comparison images for review. There is no measured token-saving
percentage claim.

## Render, inspect and promote

```sh
blender -b --python-exit-code 1 -P tools/blender/render.py -- \
  pushup-archer pullup-full --engine CYCLES
"$PY" tools/blender/encode.py pushup-archer pullup-full
blender -b --python-exit-code 1 -P tools/blender/check_motion_render_geometry.py
# Inspect encoded key phases, motion outliers and the loop at actual app size.
"$PY" tools/blender/install_clips.py pushup-archer pullup-full --dry
# After local encoded-output review:
"$PY" tools/blender/install_clips.py pushup-archer pullup-full
```

The real torso check evaluates all frames for closed mesh topology, floor
penetration, app framing and grip-inset clearance. `check_hand_geometry.py`
separately checks anatomical hand axes/finger ordering across the library.
Decoded media checks cover frame count, dimensions, codec, audio absence, byte
cap and continuity. Visual inspection is of selected encoded phases/outliers;
it is not a claim that every rendered pixel was manually inspected.

Owner phone readability and exercise-form acceptance remain open. A successful
scripted check cannot determine muscle activation, force distribution or
whether a particular technique is appropriate for a person. This pilot does
not bulk-approve or replace the other 531 videos. Existing APKs are separate
artifacts and are not rebuilt by this media workflow.

## Pull-up follow-up: velocity and bar contact

The subsequent pull-up correction adds continuous, shape-preserving interpolation
through intermediate keys and an opt-in cylinder grip. Other exercises retain
their original interpolation/hand geometry. In addition to the existing checks:

```sh
python -m unittest discover -s tests
/Applications/Blender.app/Contents/MacOS/Blender -b --python-exit-code 1 \
  -P tools/blender/check_pullup_grip.py
```

The motion gate rejects mid-travel stops and insufficient conservative capsule
clearance. The Blender check reproduces the previous intersection, verifies
rendered finger positions/orientations and checks actual mesh vertices. Endpoint
checks alone are insufficient: the chord between two clear joints can cross the
bar. Render and install only `pullup-full` for this correction, preserving the
approved archer assets. See the
[follow-up evidence](evidence/pullup-smooth-grip-2026-10-20.md).


The subsequent [steady-pacing correction](evidence/pullup-steady-pace-2026-10-20.md)
adds maximum/minimum speed checks for each direction, alongside the minimum
speed check. Continuous interpolation alone is insufficient: uneven key spacing
can still create a mid-travel surge. The original pose curve is now retimed by
height with constant central vertical velocity and smooth endpoint ramps.
