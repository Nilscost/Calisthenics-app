# Pull-up steady pacing — 2026-10-20

Further correction after the owner spotted acceleration remaining in the middle
of the previously smoothed pull-up. The previous fix removed intermediate stops
but retained uneven key spacing. Its minimum-speed gate did not detect surges.

The pull-up now traverses its existing pose path using a separate height-based
clock. An integrated cosine velocity ramp occupies the first and last 12% of
each moving phase; the central 76% runs at constant vertical speed. Position,
velocity and acceleration join continuously at the ramp boundaries. The mild
arch, shoulder articulation, corrected bar grip, endpoint holds, 4-second loop
and 24 fps remain. Only pull-up opts into this timing; archer assets are retained.

## Automated verification

- 72 Python tests pass. A frozen copy of the actual preceding clip's trajectory
  reproduces the surge and fails the new checks. Tests cover steady central
  speed in both directions and smooth ramp joins in addition to prior grip,
  motion, stale-cache and installer regressions.
- In the middle 15–85% of vertical travel, maximum/minimum vertical speed ratio
  changes from **1.64677 to 1.00036 upward**, and **1.47603 to 1.00036 downward**.
  A ratio of 1 means constant speed. The small residual is coordinate rounding.
- Whole chest speed ratio, including its horizontal movement, changes from
  **1.69208 to 1.06668 upward** and **1.51610 to 1.06744 downward**. Horizontal
  articulation remains part of the authored path; the torso is not frozen.
- New motion gates require vertical ratio ≤1.03 and chest ratio ≤1.12 in each
  direction, excluding endpoint easing. These engineering bounds apply to this
  authored controlled pull-up, not every valid human technique.
- Both scoped motion contracts pass; 531 exercises remain explicitly unscoped.

No Android code change, paid generation, new APK or physical-device verification.
Phone playback remains an owner check. The earlier smooth-grip report describes
its previous candidate; this report supersedes its timing result.

## Final media and installation

Cycles rendered all 96 full-body frames and synchronized grip views. Every
encoded frame decodes successfully: **22,883 bytes**, H.264 Main, yuv420p,
480×360, 24 fps, silent, 4 seconds. Fourteen selected encoded phases, including
the largest decoded frame difference (frame 62), were visually inspected.
This is selected-frame review plus numerical timing verification, not a claim of
watching phone playback. The installer dry run and actual pull-up-only install
pass; installed MP4 bytes equal the reviewed encode. The fresh exact frame
sequence, current source data, motion gates and renderer stamps are accepted.
The pull-up thumbnail is updated; archer MP4 and thumbnail hashes are unchanged.

Blender checks pass for 633,600 actual hand-mesh vertices and 76,800 torso
vertices over both pilot trajectories. Conservative capsule/bar clearance is
1.174 mm; no framing/floor issue was found. Content structure/rights validation
and diff whitespace checks pass. Source assets only; the R8.4 APK is unchanged.

[Measurements and installed hashes](pullup-steady-pace-2026-10-20.json) ·
[Tests](pullup-steady-pace-tests-2026-10-20.log) ·
[Grip](pullup-steady-pace-grip-2026-10-20.log) ·
[Framing](pullup-steady-pace-framing-2026-10-20.log) ·
[Render](pullup-steady-pace-render-2026-10-20.log) ·
[Replacement preview](../media/pullup-steady-pace-2026-10-20.mp4).
