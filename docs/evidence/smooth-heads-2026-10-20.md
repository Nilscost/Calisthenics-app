# Smooth mannequin heads — 2026-10-20

Owner preference: no nose or facial characteristics; retain a smooth mannequin.
Removed the renderer's nose/chin geometry and its placement logic. Head size,
orientation and articulation remain. Future renders use a featureless ellipsoid
head even when older pose metadata contains `head_detail`.

Only the pull-up and chin-up source clips/thumbnails are regenerated in this
pass. Both staged joint files are byte-for-byte unchanged, preserving every
original motion frame, the approved pull-up pacing and existing hand geometry.
The approved archer video and thumbnail are unchanged.

Verification: 72 Python tests pass. A real Blender check constructs both actual
figures, confirms no facial objects, the original head dimensions and identical
96-frame pull-up / 72-frame chin-up motion data. Encoded-media and installation
results are recorded below after completion. No paid generation, Android code
change, APK rebuild or actual-device verification.

Checks: [Python tests](smooth-heads-tests-2026-10-20.log) ·
[Actual Blender geometry](smooth-heads-geometry-2026-10-20.log).
The original `head_detail` metadata remains for orientation compatibility; it
no longer creates facial geometry. Existing clips beyond these two are retained
until explicitly regenerated, including the approved archer clip.

## Completed installation

Both full Cycles renders completed, including synchronized grip insets. Every
encoded frame decoded: pull-up 96 frames / 4 seconds / 22,699 bytes; chin-up
72 frames / 3 seconds / 20,820 bytes. Both are silent H.264 Main, yuv420p,
480×360 at 24 fps and below the 120 KiB cap. First, midpoint and final encoded
frames were visually reviewed for each clip. Dry-run and actual installation
pass; installed video bytes equal the reviewed encodes. Fresh exact sequences,
source poses and renderer stamps are accepted. Matching thumbnails were updated.
Both motion-file hashes and archer MP4/thumbnail hashes match the pre-change
record. No new APK or device verification.

[Installed hashes and media results](smooth-heads-2026-10-20.json) ·
[Smooth pull-up preview](../media/pullup-smooth-head-2026-10-20.mp4).
