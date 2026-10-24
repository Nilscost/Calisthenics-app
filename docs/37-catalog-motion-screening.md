# Catalog screening and complex-movement review set

## Scope and results — 2026-10-20

The owner authorized a catalog-wide screening pass supervised across three Luna
review lanes, and requested 5–10 complex videos to refine the generation process.
The review set contains eight varied demanding movement patterns, selected for
transitions, inversion, rotation, asymmetry, contacts and landing phases. It is
not an objective ranking of exercise difficulty or an approval of their form.

All **533 current source trajectories** were built and all **48,310 installed
video frames** decoded. The run took **12.01 seconds** on this machine. There
were no decode/format/byte-cap errors, no reach/floor warnings from the existing
source checks, and no source-versus-staged-joint mismatch. **111 clips have
heuristic timing flags**: 99 exceed the generic 3 m/s joint-speed prompt and 34
have an endpoint step over 4 cm; categories overlap. These are review prompts,
not physical/medical limits or automatic failures. Ballistic movements may be
legitimately fast. The other 422 have no flags from these generic checks; that
is not form approval. Exercise-specific motion contracts remain 2 pass and
531 unscoped.

All 533 installed MP4 hashes were rechecked and remain unchanged. No clips,
thumbs, app behavior, catalog review states or APK were replaced by this pass.

## Supervision and evidence

Three bounded Luna lanes reviewed upper/inversion, lower/ballistic/balance and
floor/core/Pilates families. Each wrote only its own report and optional image
outputs. They first inspected source and catalog instructions, then reviewed
selected frames from actual encoded clips. All eight selected clips received
selected-frame review. No lane claims full playback, expert form approval or
new reference-video comparison. Their findings and uncertainties are retained
in the evidence JSON; root reviewed the handstand/pullover evidence and relevant
source definitions before selecting the review prompts.

One concrete source/encoded issue is the straddle press: it reaches inversion
without an authored lowering phase, with a source endpoint step around 2 m at
the loop. The pullover's rapid transitions/bar clearance need investigation.
The roll-over's limited trunk articulation is another useful process test.
These findings justify per-movement phase/contact rules beyond generic checks.
No speculative findings were used to rewrite the animations before owner review.

## Repeatable workflow

```sh
# PY is the isolated environment from FORM_REVIEW.md.
"$PY" tools/blender/catalog_screen.py --output work/catalog-screening/screen.json
"$PY" tools/blender/build_review_gallery.py \
  --screen work/catalog-screening/screen.json --output /absolute/task-owned/review.html
"$PY" -m unittest discover -s tests
```

The screening command is read-only with respect to installed media and source
poses. It reports source geometry separately from encoded media. Matching staged
joints does not prove the installed video was generated with today's renderer.
It does not assess full mesh collisions, per-exercise intended contact anchors,
reference agreement for the remaining movements, muscle activation or expert
form acceptance. The gallery builder refuses videos whose bytes changed after
screening. It embeds eight MP4s without re-encoding and stays under 1 MB.
The versioned `complex_review_set.json` controls the selection.

The gallery provides normal/half/quarter playback, individual frame stepping,
per-movement review questions and optional feedback. A temporary local headless
browser verified actual play/pause time advancement for all eight videos,
metadata, stepping, playback rate, feedback retention, 320/736 px layouts and
absence of runtime errors. This is desktop artifact QA, not phone verification.
77 Python tests pass, including negative controls for nonfinite joints, source
jumps, missing media and stale gallery/video evidence.

## Appearance concepts

The owner now allows richer detail and appearance; the former smooth styling
preference is not a fixed constraint. Three real Blender stills show the exact
same approved pull-up pose: current renderer, revised material/torso smoothing,
and a fused continuous-body surface concept. None was installed. The third is
a still-only baked surface; animation would require a properly skinned body rig,
retargeting and testing across poses. It is not an anatomical muscle simulation.

Highest-value future detail is continuous shoulder/hip contours, clearer wrist
and foot articulation, useful anatomical landmarks and consistent lighting.
Texture, clothing and face detail can change appearance but do not correct pose
paths. Additional geometry must extend collision/framing checks as well.

```sh
blender -b --python-exit-code 1 -P tools/blender/appearance_preview.py -- \
  --output work/catalog-screening/appearance
```

No paid generation APIs or video credits were used. Luna usage was not measured
in dollars or tokens; no numerical savings claim is made.

[Summary, lane reports and selected clips](evidence/catalog-screening-2026-10-20.json)
· [Detailed 533-clip report](evidence/catalog-screening-detail-2026-10-20.json)
· [Tests](evidence/catalog-screening-tests-2026-10-20.log)
· [Gallery QA](evidence/catalog-review-gallery-qa-2026-10-20.log)
· [Appearance preview](media/mannequin-appearance-options-2026-10-20.png).

## Subsequent owner feedback refinements

The gallery feedback has produced two scoped replacements: [Ring Muscle-Up](evidence/ring-muscle-up-refinement-2026-10-20.md) and [Bar Pullover](evidence/bar-pullover-refinement-2026-10-20.md). The latter adds explicit angular/linear acceleration, mid-turn pause, contact-circle and slower-return checks. Extending an old duration alone must fail regression controls. New source/media snapshots are recorded separately from the original read-only catalog audit above; four movements now have contracts and 529 remain unscoped. These checks do not approve the rest of the library.

### Straddle Press foot-frame follow-up (2026-10-21)

Owner review found feet fixed in world space during inversion. The scoped
[Straddle Press correction](evidence/straddle-foot-refinement-2026-10-21.md)
adds continuous toe rotation, stable heel frames and a complete matching descent.
A first candidate was rejected after Luna's selected-frame review; the final
pointed finish was reviewed again. Five movement contracts now pass and 528
remain unscoped. The four-contract snapshot above remains historical.

The ring wrist/hand concern remains unresolved. Articulation review now includes
forearm–wrist–hand alignment and loaded-contact questions, with compact per-side
angle/frame reports. Those reports always require human review and cannot
convert collision clearance into form approval.
