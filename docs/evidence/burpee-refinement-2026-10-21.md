# Push-Up Burpee refinement — 2026-10-21

Rendered review candidate; owner acceptance and qualified form assessment remain open.
No app video is replaced and no APK is packaged in this follow-up.

The owner reported knees popping behind the body during descent. The previous
10.5-second review candidate used mirrored lateral knee guides, producing
411 mm of out-of-plane knee displacement. The correction uses sagittal bend
planes, lower bilateral hop-back feet, a staged transition into the planted
high–low–high push-up, and a hip-raised bilateral feet-in return. Standing arms
relax by the sides. The finish extends the legs before a short gravity-shaped
jump and soft landing instead of holding the body at the apex.

## Verification

- 179 Python tests pass, including the actual old-pose regression and independent
  lateral-knee, branch-flip, acceleration, held-apex and bent-launch mutations.
- The original 30 m/s² knee-transition bounds are retained: approach 28.019,
  return 11.024. A proposed relaxation to 60 was rejected during integration.
  These are authored engineering screens, not universal biomechanical limits.
- All-frame native Blender geometry passes: 252 frames, 3,822,336 body vertices,
  zero framing exclusions, minimum body height +3.000 mm and foot +4.013 mm.
- Full encode/decode and actual desktop playback, replay, pause, stepping, speed,
  saved-note restoration and narrow-layout checks pass for the eight-video gallery.
- Only the burpee source trajectory changes; the other 532 trajectories and all
  1,066 installed demo/thumbnail assets match this task's starting hashes.
- Ten scoped motion contracts pass; 523 exercises remain unscoped. Content
  validation checks structure/rights fields only. Whitespace checks pass.

The common joint-speed cap changes intentionally from 3.5 to 8 m/s for the short
arm drive. Phase-specific knee and jump guards remain active; the old 19 m/s
trajectory remains rejected. A minimum knee angle near 30 degrees during the
feet-in tuck remains an explicit visual review point rather than an automatic
anatomical verdict.

## Filmed references and automated extraction

Three independent creator videos were downloaded for local analysis and decoded:
[Peak Physio](https://www.youtube.com/watch?v=AuoDVuBK1uI),
[CrossFit](https://www.youtube.com/watch?v=TU8QYVW0gDU), and
[Rogue Fitness](https://www.youtube.com/watch?v=SxDmxON_pZk).
Peak Physio is the closest strict push-up-and-jump variation. CrossFit and Rogue
use chest-and-thigh floor contact; that distinction is preserved, not copied into
the catalog variant. Reviewed sheets and interval notes remain under
`work/burpee-refinement-2026-10-21/reference-review/`.

The existing pose-extraction script processes 56, 24 and 36 sampled frames.
Body coverage is 92.9–100%, but hand coverage is only 8.3–41.7%. The CrossFit
interval is edited and the Rogue selection is a partial fast cycle. Exploratory
alignment uses the actual rendered camera projection; all three comparisons are
**inconclusive** because of view, variation and cycle limitations. No numerical
score establishes realism, correct contacts or human form. No paid generation
credits were used (USD 0).

## Review output

[Corrected burpee video](../media/burpee-refined-2026-10-21.mp4).
The existing eight-video review gallery substitutes this candidate while retaining
the corrected Depth Jump and the six other completed candidates, plus saved notes.
The batch manifest, source hashes, full-decode result, native geometry, reference
analysis and preservation checks are recorded in the
[structured evidence](burpee-refinement-2026-10-21.json).

Phone playback/readability and expert form approval are not verified by the
geometry checks, sampled images or automated desktop playback. The review-first
workflow keeps this candidate out of app assets pending owner acceptance.
