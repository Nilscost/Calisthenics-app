# Clips v2 review (U11, 2026-10-05)

Script: `tools/gen_demo_clips_v2.py` (v1 `gen_demo_clips.py` is kept; v2 reuses its sagittal poses and adds the ten exercises from U09).
Output: `app/src/main/assets/demos/<id>.mp4`, 60 clips (53 strength, 7 stretch/mobility), 480x360, 24 fps, 2 s loop, H.264, 13–38 KB each (1.25 MB total; budget 120 KB each, enforced by the script and by `ClipAssetsTest`).
Contact sheets (frames at 0, 25 and 50 % of the loop, 8 exercises per sheet): `docs/evidence/clips-v2/sheet1.png` … `sheet8.png`.

What the drawing does: 3-D skeleton (left/right limbs get a depth offset), orthographic camera at 30° yaw and 15° pitch, capsule limbs and torso, sphere head, painter's algorithm by depth; light-grey body with a darker far side; floor grid and a soft shadow; bars, boxes (bench, chair) and walls drawn in 3-D. Primary muscles saturated orange-red, secondary muscles light orange, nothing else coloured (stretches show no muscles, they have none in the catalog). A muscle on the side turned away from the camera (e.g. the chest in a push-up seen from above) is drawn as a faint x-ray tint instead of being hidden.

Automatic checks (script `--check`, also `tests/test_clips_v2.py` where Pillow exists): every catalog id has a pose; every muscle used by an exercise has a body region and all 18 muscles have one; every strength exercise has a primary muscle. `ClipAssetsTest`: every catalog id has a clip, no clip without an exercise, each < 120 KB.

## Visual review
Done by the implementing model from the eight contact sheets (3 frames per clip), NOT by a human; the owner's phone check is the real review.

(a) Correct movement — judged by eye on the three frames:
- Read as intended: push-up family (incline bench, knee, standard, feet-elevated, diamond, archer, one-arm), pike push-ups, pull-ups and chest-to-bar, muscle-up, squat family (air, split, Bulgarian, shrimp, pistols), glute bridges, bridge, planks, side planks, leg raise, V-up, L/V-sit, planche lean/tuck, kettlebell deadlift, single-leg RDL and swing, inverted rows (all four), Copenhagen, arch hold/rocks, stretches.
- Weakest: wall handstand clips (still thin even with a 62° yaw, the pose is right), cat-cow (the spine curve is crude), arch rocks (small movement), dead bug (limbs overlap at some angles). The handstand and front-lever clips use a wider yaw (62° / 55°) because at 30° they were almost edge-on; that improved the lever clips clearly and the handstand clips somewhat.

(b) Only the listed muscles coloured: checked on every sheet — no colour on the head, hands, feet, equipment or floor. Orange appears on the regions of the catalog muscles only (arms for triceps/biceps/delts, torso bands for chest/lats/abs/obliques, thighs for quads/hamstrings/adductors, shins for calves). Known approximation: a muscle is a band on one side of a capsule, not an anatomical shape; secondary muscles of the far limb are drawn too.

(c) Readable depth: the near/far limb split, the floor grid and the shadow give the 3/4 impression on all clips; the handstands remain the least readable.

## Open items
- Handstand clips could use a closer, hand-tuned camera; for the owner's rating.
- No real video was used; any `<id>.mp4` can be replaced later.
