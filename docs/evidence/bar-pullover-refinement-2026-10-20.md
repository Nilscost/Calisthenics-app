# Bar pullover timing refinement — 2026-10-20

Owner feedback: the `pullup-pullover` video is not smooth and may take longer
than four seconds. The previous installed encode was 4.5 seconds. This update
is limited to that movement and its thumbnail; the jumping pullover is unchanged.

## Correction

The old trajectory eased to a stop at every quarter-turn, lifted the pelvis
roughly 60–66 cm over short phase transitions, then compressed most of the
return into less than a second. Its final scalar angle reset also made the loop
rotate rapidly through an unintended path. A frozen copy is a regression control.

The replacement lasts eleven seconds: 0.44 s initial hang, 4.4 s rise/rollover,
0.66 s support, 5.06 s controlled reverse and 0.44 s hang. Intermediate keys
retain velocity; full-turn angles stay unwrapped. Time follows the solved
body's average joint-path distance, with smooth speed ramps at the deliberate
start/stop holds. Positions around the bar are derived after interpolation,
so neither the body nor the wrist contact cuts through circular arcs.

The trunk stays tangent to the bar as the hips roll over it. Hands rotate around
the cylinder with the wrists, maintaining the modeled finger wrap; elbow poles
keep forearms outside the bar. This is an opt-in grip mode. Existing static
pull-up/chin-up grips and all other source trajectories retain their values.
The clip uses the continuous torso and featureless mannequin head.

## Automated verification

94 Python tests pass. Negative controls reject the actual old trajectory,
extending its duration without correcting the path, shortening the revised
cycle, a single-frame pop, a mid-turn pause, a sliding wrist, a missing rolling grip and a
body/bar intersection. The source passes reach and floor checks.

| Measure | Old | Revised |
|---|---:|---:|
| Peak tracked joint speed | 32.641 m/s | 2.079 m/s |
| Peak tracked joint acceleration | 729.784 m/s² | 5.969 m/s² |
| Peak torso angular speed | 1,731.031 °/s | 151.242 °/s |
| Peak torso angular acceleration | 34,219.507 °/s² | 414.334 °/s² |
| Loop gap at stored precision | discontinuous | 0 |
| Matched central descent/ascent duration | compressed return | 1.167× |

These are implementation screening measurements, not universal technique limits.
Peak linear metrics include elbows/knees as well as torso/head/ankles. The new
contract also rejects mid-turn stops, body/bar volume intersections, wrist
contact drift and missing full rotation/support endpoints.

Real Blender checks cover 3,965,808 actual body/hand mesh vertices across all
264 frames. Minimum sampled mesh/bar gap is 4.689 mm. A separate whole-finger
capsule bound clears the cylinder by 1.174 mm; body/limb screening envelopes
clear it by 9.98 mm. All actual body/hand vertices fit the output camera.
Vertex checks and volume bounds provide different evidence and are not a
physical wrist/contact simulation. Other body-to-body collisions are not covered.

All 532 other source trajectory hashes remain unchanged. The frozen baseline,
full reports and rendered phases are kept under ignored
`work/pullover-refinement/`; durable logs and summary accompany this report.

## Review limits

A bounded Luna reviewer inspected the old source/contact sheet and twelve actual
Cycles phases of the candidate. The phases read as hang, pull, rollover, support
and a broad reverse. The hand contact remains small at overview scale. This
candidate uses mostly extended legs rather than a pronounced knee tuck.
Selected images do not establish continuous playback or expert form approval.

The existing catalog instructions informed the movement sequence. No new
three-to-five-video numerical reference comparison was performed. No generation
credits were used. There is no Android behavior change, new APK or physical
phone verification in this media correction. Owner playback/form acceptance
remains open.

## Installed output and desktop playback

The installed silent H.264 Main/yuv420p MP4 is 480×360 at 24 fps, 264 frames,
eleven seconds and 42,035 bytes. Every frame decodes; the installed bytes match
the reviewed staged encode exactly. All 264 render frames postdate this run's
render start and the render provenance matches the current source. Installation
(dry run and actual) and structural content validation pass.

Desktop checks play the entire clip to its end at twice normal speed, replay it,
and verify frame stepping, rate selection, feedback retention, 320/736-pixel
layouts and absence of runtime errors. The review player stops at the end by
design; an initial QA assumption about automatic looping was corrected without
changing the player. Source loop endpoints have zero positional gap; the encoded
first/last mean pixel difference is 0.267 on a 0–255 scale. These measurements
and desktop playback do not establish phone smoothness or expert form quality.

The other 532 installed MP4 hashes match the pre-change baseline. The fresh
catalog screen has 109 heuristic review flags and 424 clips without those
flags. Four motion contracts pass; 529 movements remain unscoped.

[Summary and bounded review](bar-pullover-refinement-2026-10-20.json) ·
[Tests](bar-pullover-tests-2026-10-20.log) ·
[Blender geometry](bar-pullover-geometry-2026-10-20.log) ·
[Full render](bar-pullover-render-2026-10-20.log) ·
[Desktop review checks](bar-pullover-gallery-qa-2026-10-20.log) ·
[Revised video](../media/bar-pullover-smooth-2026-10-20.mp4).

Owner follow-up on 2026-10-21: “Much better.” This is positive playback feedback,
not professional form approval. The pullover media is preserved during the
subsequent Straddle Press foot correction.
