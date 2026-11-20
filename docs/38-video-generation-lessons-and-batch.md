# Lessons for scripted exercise demonstrations

2026-10-21. Engineering guidance from owner review and bounded Luna implementation,
not professional form approval. Preserve existing accepted assets until each new
encoded candidate is reviewed. No paid video-generation credits are used.

## What the review established

A plausible silhouette and low collision error do not establish correct movement.
The original archer push-up lacked the required lateral movement; the pull-up had
unclear grip and an unwanted speed surge. Later review caught invisible rings,
a sudden crossover, an unnatural ring wrist, and world-fixed Straddle Press feet.
Each concern needs its own observable rule and regression case.

Reviewers must inspect the actual baseline. The previous burpee already contained
a high–low–high push-up and landing; the previous pistol already returned upward.
Frozen old-pose fixtures remain honest passes for those features. Improvements
must be described as controlled transfer, preload, timing and explicit phases,
not repairs for motions the baseline already contained.

## Rules that now affect generation

| Lesson | Executable check / evidence | Remaining judgment |
| --- | --- | --- |
| Define the exact catalog variation before posing | Per-exercise motion contracts; missing-phase and wrong-variant mutations | Whether the demonstration teaches the intended technique |
| A depth jump must step into air, then land on both feet and rebound | Bilateral flight, no early floor support with rear foot on box, short first ground interval, second landing and recovery checks | Load absorption and real-world pace |
| Do not stop at every interpolation key | Continuous authored trajectories; speed, acceleration, angular continuity and cycle-seam checks where scoped | Smoothness at actual playback speed |
| Inversion needs body-relative feet | Straddle rotation and pointed finish; full-wiper shin-relative toes; noncollinear roll-over feet; heel-frame continuity mutations | Ankle posture and toe readability |
| Review the entire forearm–wrist–hand chain under load | Ring wrist extrema and frame changes, fixed contact errors; frozen old wrist rejected | Plausible grip, flexion/deviation and loading |
| Implement orientation and hand orientation need independent frames | Fixed ring plane with moving hand contact; independent ring-plane/contact regression tests | Finger wrap and pressure during turnover |
| Joint endpoints can clear the floor while the rendered foot penetrates it | Native batch preflight checks every actual body vertex against the floor and final encoded framing; first batch exposed burpee foot penetration of 38 mm | Surface contact and force balance still need review |
| Collision checking needs volumes and actual meshes | Ring conservative arm/finger volumes plus native all-frame mesh scan; 192 frames and 1,267,200 hand vertices checked | Real anatomy and forces are outside this model |
| Measure planted phases separately from approach/absorption | Phase-specific slip gates in lower/floor/conditioning validators | Convincing transfer between supports |
| Preserve the whole movement and a physical return | Phase/range/seam tests, complete encoded repetition review | Returning must not obscure the exercise or create unsafe imitation |
| Never approve from selected stills or a generic green score | Unknown exercises remain `unscoped`; articulation reports always request human review | Full playback, phone readability and qualified form assessment |
| Freeze source and render provenance | Isolated batch staging, source/data hashes, renderer/validator stamps, stale-source rejection | Owner acceptance before promotion |

The first depth-jump rewrite passed its initial tests but was a step-down: one
foot reached the floor while the rear remained on the box. Root rejected it.
The replacement negative control explicitly rejects that error. Tests must defend
movement identity, not merely reproduce the first implementation's numbers.

Thresholds are engineering bounds for these authored variants, not universal
physiological limits. Position metrics exclude orientation vectors. Nonfinite or
degenerate geometry fails rather than quietly becoming a successful measurement.
The shared contract file records the effective scoped metric bounds; changes need
an intentional review, not silent validator drift.

## Ring wrist refinement

The old frozen hand direction produced a 115.668-degree peak forearm-to-fingertip
bend. The candidate rotates the hand around a fixed ring contact while preserving
the ring plane, true hoop, straps and modest body crossover. The peak is now
47.421 degrees and the largest per-frame angle change is 7.449 degrees in an
eight-second cycle. These are simplified axes, not measured human wrist bones.

Native conservative minimum clearances: arm 9.276 mm, fingers 1.360 mm; actual
hand mesh/ring 4.515 mm and body vertex/ring 11.347 mm. Independent Luna inspection
of selected Cycles poses found a visibly improved connection, but retained the
need for full playback and phone-size review. Clearance is not form approval.

## Next batch

Use `tools/blender/render_batch.py` to prepare and run an isolated review batch.
The planned set is ring-muscle-up, depth-jump, burpee-pushup, squat-pistol,
pilates-roll-over and hanging-windshield-wiper, plus pullup-pullover and
handstand-press-straddle as accepted regression controls. This applies the lessons
to six refinements and checks that shared renderer changes preserve two recently
accepted movements. It does not introduce an unchosen mannequin style.

Outputs live under `work/render-batches/2026-10-21-lessons-b/`. The manifest and
state distinguish preparation, active rendering, encoding, completion and failure.
Before rendering, `check_batch_geometry.py` examines every actual body vertex
for floor penetration (2 mm tolerance) and final encoded framing. The runner
requires its matching source and staged-data hashes. A complete item then needs
fresh native frames, matching source/stamps, full decode,
expected frame count/duration, silent 480×360 24 fps H.264 and the 120 KiB cap.
The initial batch was stopped after native inspection found burpee foot-floor
penetration and a pre-existing 5 mm roll-over chest-floor intersection. Corrected sources
were prepared as fresh batch B rather than treating interrupted frames as current.
No batch command installs app assets. Rendered candidates need continuous review
and owner acceptance before promotion. A running process and growing frame count,
not a manifest alone, establish that the batch has started.

## Efficient supervision and scope

Root owns shared models, contracts, provenance, integration and verification.
Luna lanes own bounded family functions, dedicated validators/tests and compact
reports. Root checks source instructions, actual baselines and counterexamples.
Cheap scripts screen every frame and report the worst frame/time; review then
focuses on those frames plus full playback. Scripts cannot guarantee human form.

The 3–5-video reference comparison workflow remains available in FORM_REVIEW.md.
Existing camera/landmark limitations make numerical reference comparisons
inconclusive. No new reference-video extraction or quantitative human comparison
was completed for this batch. A small pose error is not an approval score.

Further mannequin detail should improve readable joints, thumb/finger direction,
sole/heel orientation and material separation. Anatomy, clothing, lighting and
surface refinement can follow after movement acceptance; more polygons cannot
repair an incorrect trajectory. Keep style experiments separate from motion fixes.

## Evidence

- `work/video-generation-2026-10-21/shared-contracts.md`: bounded ownership.
- `work/video-generation-2026-10-21/{lower,floor,conditioning}/`: lane reports and proposed contracts.
- `work/video-generation-2026-10-21/integrated-audit.json`: current scoped audit.
- `work/video-generation-2026-10-21/ring-geometry.log`: actual native geometry.
- `work/video-generation-2026-10-21/ring/wrist-comparison.png`: selected before/after poses.
- `work/video-generation-2026-10-21/ring/review.json`: independent selected-pose review and its limits.
- `work/video-generation-2026-10-21/tests.log`: combined automated tests.

No phone behavior, expert form approval, new APK, or full-library approval is
claimed by these source and geometry checks. Batch state is the authority for
render progress; this document is not proof that a render has completed.

The geometry findings also gained source regressions: the burpee validator mirrors
the renderer's heel/tip and tapered foot construction, rejecting the -38.453 mm
candidate and measuring +4.018 mm after correction. Roll-Over now constrains the
rotating chest ellipsoid's support height with a 1.1 mm margin while keeping its
hand anchors fixed. The old chest intersection existed in the frozen baseline;
it is not attributed to the new trunk curl. Both fixes are checked again on the
actual native meshes before the corrected batch renders.

The corrected batch B passed its native preflight: all eight candidates,
21,335,472 actual body vertices, zero outside the final body frame, and all
body-surface floor minima above -2 mm. Evidence is saved in
`docs/evidence/video-generation-native-preflight-2026-10-21.{json,log}`.
The batch then began full Cycles frames; this does not claim completed encodes
or form acceptance for all eight.

The first candidate, Ring Muscle-Up, completed all 192 frames and its eight-second
encode (37,724 bytes, H.264 Main, 480×360, 24 fps, yuv420p, silent). Full decode,
play-to-ended, replay, pause and eight seeks passed using the actual MP4 in an
isolated desktop browser. The encoded phase sheet was inspected; no professional
form or phone approval follows from it. Candidate:
[Ring wrist refinement](media/ring-wrist-refined-2026-10-21.mp4).
The runner then moved on to Depth Jump with growing fresh frames. See
`docs/evidence/video-generation-batch-start-2026-10-21.json` for the live snapshot
and `docs/evidence/ring-wrist-playback-2026-10-21.json` for playback evidence.
Seven further batch encodes were pending at that snapshot; all installed app
videos and thumbnails remain unchanged. No new APK was packaged for this media
review batch.

## Depth Jump follow-up and review set completion

Owner feedback caught unrealistic arms and floating flight. The correction now
uses a half-second ballistic rebound, coordinated arm drive, forearm-relative
hands, full takeoff leg extension and a continuous ground-to-flight boundary.
The first revised source still had a launch-height override bug: a good parabola
inside flight did not guarantee a good takeoff. Actual rendered phases caught it;
launch-knee and boundary-continuity regressions now reject it. Validate the joins
between physical phases as well as each phase in isolation.

169 Python tests, all-frame native geometry and encoded desktop playback pass.
Three filmed references informed sequencing; quantitative comparison remains
inconclusive, with poor hand detection and camera/variant differences retained.
Only the corrected Depth Jump app assets are installed by that follow-up.
The original eight-render batch is now complete and the review gallery displays
its candidates with the corrected Depth Jump substituted. Prioritize the five
owner-review items named in the [follow-up evidence](evidence/depth-jump-refinement-2026-10-21.md).


## Burpee follow-up: anatomy and phase joins

Owner review caught lateral knees despite passing foot-floor checks. The actual
frozen review candidate now supplies a regression: 411 mm out-of-plane knee
travel, poor transition continuity and a held jump apex. A sagittal knee plane,
continuous flexion branch, relative-knee acceleration and a small physical jump
are separately screened. Preserve the original transition bounds (30 m/s² here)
while improving the path; a proposed 60 m/s² relaxation was rejected. Native pose
review also caught an excessive heel curl during hop-back, so foot lift was
reduced rather than accepting the first mathematical pass.

Jump checks include fitted gravity, apex dwell and takeoff knee extension. A
parabolic pelvis alone is insufficient if the legs remain squatted throughout
launch. Ten independent tests protect the old-pose regression and mutations;
179 total Python tests pass. The final encode and all eight gallery playback
controls pass; existing app assets stay untouched pending owner review.

Three filmed burpees informed sequencing, but only Peak Physio closely matches
the strict catalog push-up variant. CrossFit/Rogue use chest-and-thigh contact,
and sampled intervals/views remain uncertain. Automated projected-angle
alignment is inconclusive, with weak hand detection. Do not turn three available
videos into three compatible ground-truth demonstrations merely to fill a quota.
[Follow-up evidence](evidence/burpee-refinement-2026-10-21.md).

## Moving-ring and depth-return follow-up

The newer Ring Muscle-Up source uses moving hoop centres, planes and grip arcs
with fixed suspension anchors, plus a forearm-relative false-grip frame. This
supersedes the static-ring candidate described above. A heel-over-wood/down-palm
identity screen caught an underhook that absolute wrist angles had missed.

Native verification also exposed fixed-length rendered finger meshes following
variable-length authored segments. Updating the mesh endpoints fixes that
representation mismatch; it does not make the segments anatomical. An explicit
proportion report still flags changing proximal finger lengths and a skin/wood
gap. Keep this candidate blocked from app promotion. Future acceptance needs
consistent phalanx lengths, credible contact and full encoded hand review as well
as collision clearance. The extra search candidates did not establish that all
of those constraints have a satisfactory solution in the current hand rig.

The Depth Jump return exposed a different blind spot: correct airborne physics
can coexist with a crab walk during the reset. Return checks now inspect facing
relative to travel, planted-sole slip and heading, a real turn toward the box,
and native solid-box clearance. The stronger rebound remains ballistic; its
height is an authored variant, not a calibrated athlete measurement.

The two follow-ups preserve the other 531 trajectories and all installed media.
205 Python tests pass. The new candidates need owner/phone/expert review; no
new APK is produced for this review-only step. Root continued the remaining
implementation after the existing Luna executors reached their usage limit.
