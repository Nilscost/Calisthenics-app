# Ring muscle-up refinement — 2026-10-20

Owner feedback from the eight-clip gallery: rings cannot be seen and the forward
movement looks unnatural. This refinement is limited to `ring-muscle-up`.
The other 532 source trajectories retain their previous hashes.

## Correction

The old source defined only wrist-attached rods, with no actual ring mesh.
The revised clip has two wooden torus rings, dark suspension straps and a
synchronized close-up isolating the right ring/hand. The main view retains both
rings throughout. The neutral ring grip sits on the rear/lower arc of the hoop; its contact is derived
from the wrist and the same geometry drives finger placement and mesh checks.
A continuous torso is used; the head remains featureless.

The movement is now a six-second pull, close crossover, press, top hold and
controlled reverse to the original hanging pose. The previous tucked-leg stages
are removed. The elbows move behind the torso through the transition rather
than inheriting one unchanged pole target for the entire cycle. Position and
velocity pass continuously through the authored phase keys, with smooth timing
at the start and end of each direction.

Measured source changes:

| Measure | Previous | Revised |
|---|---:|---:|
| Chest fore/aft range | about 41.6 cm | 22.16 cm |
| Peak forward chest speed | 1.564 m/s | 0.631 m/s |
| Peak chest path speed | 3.787 m/s | 0.907 m/s |
| Revised pelvis fore/aft range | — | 10.25 cm |
| Revised loop endpoint gap | — | 0 at stored precision |

These are authored-motion checks, not universal biomechanics limits.

## Reference basis and limits

Primary written guidance consulted:
[GMB's strict ring muscle-up breakdown](https://gmb.io/muscle-up/),
[Pamela Gagnon's CrossFit breakdown](https://www.crossfit.com/essentials/crossfit-ring-muscle-up-rx-plan),
and [the original CrossFit movement sequence](https://www.crossfit.com/essentials/the-muscle-up-journal-classic).
Only the grip/pull/transition/press sequence, close-ring geometry and modest
weight transfer were used; this is not a prescribed training plan.
No new numerical YouTube landmark comparison was performed for this correction.

A bounded Luna reviewer inspected the old encoded phases, source definitions
and the revised rendered phase previews independently. The revised rings are
readable and crossover/tuck behavior is visibly improved in those samples.
Exact false-grip mechanics and contact readability at support remain form-review
questions. Hands are a simplified procedural grip, not a biomechanical wrist
simulation. Selected stills do not establish continuous playback or expert form
approval. Owner acceptance of the revised loop remains open.

## Verification

85 Python tests pass, including the frozen actual old pose as a negative control,
removing the ring geometry, introducing a forward lurch and testing finger
capsules against the actual torus rather than an axis-aligned bounding box.
The old missing rings, excess crossover and leg-tuck change are rejected, as is
an earlier candidate whose grip placement allowed a ring to cross an arm.
Three scoped motion contracts pass; 530 other movements remain unscoped.

Real Blender checks cover all 144 revised frames and 950,400 actual hand-mesh
vertices. Minimum sampled hand/ring vertex clearance is 7.525 mm; conservative
whole-finger capsule clearance is at least 1.497 mm, including a bound for gaps
between samples. Body mesh vertices clear by at least 11.338 mm in all frames.
A stronger whole-arm volume check, including enlarged joint envelopes, clears
by at least 2.240 mm and is enforced by both the pre-render and installation
motion gates. This catches rings crossing long arm segments even when their
mesh vertices lie outside the wood. The source tests reproduce that failure.
The neutral rings have projected mesh width at least 20.2 pixels at 480×360;
the isolated close-up provides a larger view. These projection/mesh checks
are distinct from encoded readability or physical phone verification.

The renderer, shared ring helper and contracts participate in render provenance;
the installer checks source equality, complete fresh full/grip frame sequences,
motion gates and hashes. No changes to Android behavior, catalog review state or
APK are part of this media refinement. No video-generation credits were used.

## Installed media and review controls

The final 480×360 H.264 Main/yuv420p encode contains 144 decoded frames at
24 fps, six seconds, silent, 29,539 bytes. All 288 full/grip render frames
postdate the final render start. The installed MP4 exactly matches the reviewed
encode; installation and content validation pass. The other 532 installed MP4
hashes are unchanged. The eight-video review set now contains this encode.
Desktop playback, pause, frame stepping, speed, feedback retention, narrow/wide
layout and runtime checks pass. This is desktop verification, not phone testing.

[Machine-readable evidence](ring-muscle-up-refinement-2026-10-20.json),
[tests](ring-muscle-up-tests-2026-10-20.log),
[actual Blender geometry checks](ring-muscle-up-geometry-2026-10-20.log),
[final render](ring-muscle-up-render-2026-10-20.log),
[desktop review checks](ring-muscle-up-gallery-qa-2026-10-20.log).

The new complete technical screening snapshot has 110 heuristic review flags
and 423 clips without those flags; these categories do not approve exercise
form. Only three exercise-specific motion contracts are implemented.
