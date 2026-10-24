# Reference comparison and readable grips

The next stage adds motion-identity gates and a continuous torso for archer
push-ups and pull-ups: [motion-quality workflow](../../docs/36-motion-quality-pilot.md).

The first pilot covers `pullup-full` and `chinup-full`. Five source videos provide
three independent videos per exercise. `form_references.json` records URLs,
creators, selected intervals, variation limitations and camera-review state.
References remain in ignored `work/form-reference-pilot/`, outside app assets.
No paid generation service is used. Generation-credit spend: **USD 0**.

## Detail contract

The existing rig has four articulated fingers and a thumb on each hand. The new
hand axes preserve anatomical handedness through supination, rather than always
pointing the thumb toward the body midline. Finger ordering keeps the index
next to the thumb and the little finger on the opposite side. Palm pads are blue; the back of the
hand is light grey. The mannequins use smooth, featureless heads, plus a
synchronized enlarged right-hand view and plain grip captions at 480×360.
Chin-up wrists are on the opposite side of the bar from overhand pull-up wrists;
the body position follows so the hands stay reachable. Other exercise pose data
is unchanged. Renderer geometry improves on future renders; existing bundled
clips do not change until explicitly installed.

Both views must render every frame. Installation requires contiguous, fresh
close-up frames and a matching hash of the renderer and hand-axis implementation.
The encoder refuses to silently omit a required grip inset. A start-only preview
cannot pass full-render installation checks. Do not promote old cached renders.

## Reproduce the local pilot

Use Python 3.12 in an isolated virtual environment. Install
`requirements-form-review.txt`. Set `PY` to that environment's Python executable.
Download the two official MediaPipe task bundles linked from Google's pose/hand
landmarker guides into `work/form-reference-pilot/` as
`pose_landmarker_full.task` and `hand_landmarker.task`. Obtain the reference files
at the paths in the manifest; use creator-provided or otherwise permitted files.
The videos are analysis references, not replacement footage to ship.

```sh
"$PY" tools/blender/pose_compare.py extract-references \
  --manifest tools/blender/form_references.json \
  --pose-model work/form-reference-pilot/pose_landmarker_full.task \
  --hand-model work/form-reference-pilot/hand_landmarker.task
"$PY" tools/blender/poses.py pullup-full chinup-full
/Applications/Blender.app/Contents/MacOS/Blender -b -P tools/blender/render.py -- pullup-full chinup-full
"$PY" tools/blender/encode.py pullup-full chinup-full
"$PY" tools/blender/pose_compare.py project \
  work/clips-bakeoff/pullup-full/blender/camera-projection.json \
  --output work/form-reference-pilot/pullup-full-candidate.json
"$PY" tools/blender/pose_compare.py compare \
  --exercise pullup-full --manifest tools/blender/form_references.json \
  --candidate work/form-reference-pilot/pullup-full-candidate.json \
  --output work/form-reference-pilot/pullup-full-report.json
```

Repeat project/compare for `chinup-full`. Camera projections come from Blender's
actual full-body camera; they are not world angles compared with image angles.
Reference extraction records source/model hashes, timestamps, threshold, body
measurements and all 21 landmarks for each detected hand. Dimensions are
converted to pixels before measuring angles to avoid aspect-ratio distortion.
Constrained dynamic time warping aligns movement timing, retains missing frames
as penalized gaps and reports joint errors and measurement coverage. Three to
five distinct video IDs are required per exercise. Stale intervals are rejected.

Run `blender -b --python-exit-code 1 -P tools/blender/check_hand_geometry.py`
to check the real hand geometry across all 533 poses and the renderer resume
cache path. This does not rerender or approve the full library.

## Interpretation and promotion

Results are screening measurements, never an automatic form certificate.
Different perspectives, missing joints, cropped bodies, scene cuts and uncertain
hand detections must stay visible. Current reference camera views differ from
the app camera, so the pilot reports **inconclusive** numerical comparisons.
A weighted chin-up reference can inform grip/upper-arm movement but cannot
establish the unweighted clip's leg motion or balance. Shared names alone do not
make variations equivalent. Scapular, neck and finger mechanics need close review.

Body pose cannot prove palm direction. Hand landmarks are retained for visual
review; detection coverage is not a grip classification. Do not label grip
correct from the body score, hand detector handedness, or a blue patch alone.
Inspect actual thumb/finger wrap, wrist position relative to the bar, palm normal
and the complete encoded loop at the displayed size. Mark source segments with
wrong-form demonstrations, edits or incomplete reps unusable before comparison.

Before extending the library, review these two encoded pilots at phone size,
including the inset's readability and whether it hides important body motion.
Only the two reviewed candidates should be installed; no bulk replacement of
533 clips based on numerical scores. Owner phone readability and professional
form review remain distinct from automated geometry/media checks.

## Articulation realism: mandatory review questions

Owner feedback identified an unnatural wrist/hand relationship in the ring
muscle-up after the apparatus and timing were corrected. That concern remains
open; collision clearance does not approve a wrist pose under load. Review the
forearm–wrist–hand chain, palm direction, flexion/deviation, grip pressure and
contact continuity at hang, turnover and support. A hand axis is a simplified
fingertip direction, not a precise skeletal palm orientation.

Run `articulation_review.py <ids> --output <report.json>` for compact per-side
wrist-angle extrema, their frames/times and largest frame-to-frame angle change.
Its status always requires human review; it does not supply anatomical approval
or a universal safe threshold. Select those frames for visual review, then watch
the complete encode. No ring asset is replaced by merely adding this report.

For feet, review toe direction relative to shin and body throughout inversion,
ankle articulation, heel/sole frame continuity, ground contact and the complete
return. A constant world-space toe direction is insufficient for an inverted
movement. Nonzero frame axes and low collision errors alone do not prove form.

For loaded apparatus, check the wrist's position relative to the wood as well
as its angle: a false-grip heel belongs over the ring. A mathematically exact
declared contact point can coexist with skin intersecting another arc of the
hoop. The moving-ring source gate samples palm/pad/dorsum surfaces and bounds
whole finger/forearm capsules; actual native mesh and encoded close-up review
remain distinct gates. The ring plane must be independent of the palm plane,
but it must move plausibly with the gripped hand and constrained suspension.

Keep reference identities explicit. Assisted strict transitions, a continuous
negative return, an edited tutorial close-up and a visibly swinging/kipping
sequence offer different evidence. Three available URLs do not establish
three compatible complete strict cycles or calibrated human joint angles.

For reset walking, inspect facing relative to travel, planted sole position and
heading, swing-foot lift, solid apparatus clearance, weight transfer and the
loop seam. Checking only the active repetition misses moonwalking and sliding
resets. A higher rebound must retain gravity-shaped acceleration, an immediate
load-to-launch transition and a soft second landing, rather than add an apex
hold. Treat rebound height as an authored variant unless calibrated footage
supports a numerical comparison.
