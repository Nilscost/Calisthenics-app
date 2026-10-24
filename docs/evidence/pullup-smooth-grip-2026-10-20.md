# Pull-up smoothness and bar-contact correction — 2026-10-20

Follow-up to the motion pilot, limited to the pull-up. The approved archer MP4
and thumbnail remain byte-for-byte unchanged. This supersedes the earlier
pull-up timing/grip acceptance; earlier reference comparisons describe their
recorded candidate and are not new comparisons of this revision.

## Correction

The previous cubic easing reset velocity to zero at every intermediate key,
including during the pull and descent. Pull-up alone now uses shape-preserving
cubic Hermite interpolation with continuous velocity through those keys. The
intentional endpoint holds, shoulder initiation, mild arch, fixed wrists,
4-second duration and 24 fps remain. The interpolator does not overshoot the
keyed reach envelope.

The generic curled fingers intersected the bar because their straight segments
ignored its cylinder. Pull-up alone now uses three segments per finger and an
opposed two-segment thumb, wrapping around the bar. Clearance checks include
capsule thickness and the entire chord between joints. The body and synchronized
grip inset use exactly the same geometry.

## Verification

- 69 Python tests pass. Regression tests reproduce the old mid-travel stops,
  check continuous derivatives at internal keys, prevent trajectory overshoot,
  cover both hands in every frame and detect capsule intersections even when
  both endpoints lie outside the bar.
- Lowest pelvis speed in the middle 15–85% of travel rises from **0.036 m/s**
  to **0.299 m/s**; the new screening threshold is 0.12 m/s. Intended top/bottom
  holds are excluded. This is a diagnostic for this authored motion.
- Conservative minimum finger/thumb capsule clearance: **1.174 mm**.
- Actual Blender mesh checks and encoded-output results are recorded in the
  accompanying evidence JSON and logs. Analytic clearance is the conservative
  collision check; mesh-vertex checks alone cannot prove face clearance.
- Both scoped motion contracts pass; 531 exercises remain explicitly unscoped.
- No paid generation, Android code change, new APK or physical phone check.

Reproduction: run `motion_review.py pullup-full --write`, the Blender
`check_pullup_grip.py` and `check_motion_render_geometry.py` scripts, then render,
encode and explicitly install only `pullup-full` through the existing workflow.
The render stamp includes the new grip helper; stale cached renders are rejected.

Remaining acceptance: watch this replacement loop on the phone for playback
smoothness and grip readability. These checks do not certify exercise form.

Evidence: [tests](pullup-smooth-grip-tests-2026-10-20.log),
[actual hand geometry](pullup-smooth-grip-geometry-2026-10-20.log),
[before/after grip image](../media/pullup-smooth-grip-2026-10-20.png).
The old generic grip reproduced **25.610 mm** of conservative capsule
penetration; the actual replacement meshes have at least **4.694 mm vertex
clearance** across 633,600 sampled hand-mesh vertices in all 96 frames.
The conservative whole-capsule clearance remains the stricter **1.174 mm**.
Torso/framing checks cover 76,800 vertices across both pilot trajectories.

## Installed output

Only `app/src/main/assets/demos/pullup-full.mp4` and its matching thumbnail were
installed. The MP4 is **22,656 bytes**, 96 frames / 4 seconds, silent H.264 Main,
yuv420p, 480×360 at 24 fps. Every frame decoded successfully. Fourteen selected
encoded phases, including the largest decoded frame difference (frame 42), were
visually inspected; timing continuity is verified by trajectory tests, not a
claim of watching playback on a phone. Both dry-run and actual installation pass,
and installed bytes match the reviewed encode exactly. Current source, exact
fresh frame sequence, grip frames and renderer provenance are accepted.
Archer MP4 and thumbnail SHA-256 hashes match their pre-change values.

Full measurements and installed hashes:
[machine-readable evidence](pullup-smooth-grip-2026-10-20.json).
[Render log](pullup-smooth-grip-render-2026-10-20.log) ·
[Torso/framing check](pullup-smooth-grip-framing-2026-10-20.log).
The source MP4 and thumbnail are ready; the existing R8.4 APK remains unchanged.
