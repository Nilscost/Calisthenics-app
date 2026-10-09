# Clips v3 review (V06b, 2026-10-09)

Method: for every one of the 61 ids a 6-frame sequence (frames 0, 1/6 … 5/6 of the 2 s loop) was rendered with `python3 tools/gen_demo_clips_v3.py --frames out.png <ids>` and looked at (the sheets are in `docs/evidence/clips-v3/frames-*.png`, ten ids per sheet). Looking at frames is not the same as watching the clip on the phone: smoothness and pace are an owner check. Thumbnails (`thumbnails-before-v06b.png`) were checked at 128 px.

## Fixed in V06b
| Id | Problem before | Now |
|---|---|---|
| `pushup-feet-elevated` (K2) | the standard push-up pose | feet on a box, body slopes head-down towards the bottom |
| `pushup-diamond` (K2) | the standard pose | hands brought together under the chest, wider camera yaw (62 deg) so it shows |
| `pushup-archer` (K2) | the standard pose | one arm bends, the other stays straight out to the side |
| `pullup-full` (K5) | tiny dead-hang-to-shoulder motion, head hidden behind the bar, straight legs | full range from straight arms to chin over the bar, knees bent behind |
| `pullup-chest-to-bar` | same motion as the pull-up with a bit more range | leans back, chest reaches the bar |
| `pullup-band-assisted` | the old pull-up | same new motion plus the band to the foot |
| `muscle-up-bar` | a pull-up that ends at the bar | pull leaning back, rotate over the bar, press out to straight arms above it |

`SHARED_POSES` now lists one group only: the one-arm push-up and its negative (same pose; the difference is the cue text). The assertion `--check` fails for any new unlisted sharing.

## Rejected or weak (not fixed in this task; the reason and where it goes)
| Id | Verdict | Reason | Next |
|---|---|---|---|
| `bridge-back` (Full Back Bridge) | **rejected** | reads as an A-frame / inverted V with the head under the arms, not an arched back bridge | V25 adds a middle step (C-E); redraw the pose then |
| `front-lever-tuck`, `front-lever-adv-tuck`, `front-lever-straddle` | weak | figure is tiny under a tall bar; the horizontal body is hard to see | V06 follow-up / clips pass |
| `v-sit-floor` | weak | arms cross the legs in an odd way | clips pass |
| `hspu-wall` and `hspu-wall-negative` | identical motion | only the cue differs (the negative should lower slowly and not press up) | clips pass |
| `wall-handstand-hold`, `hspu-wall*` | weak | thin figure edge-on to the wall (yaw 62 helps a little) | clips pass |
| `arch-hold-y` and `arch-rocks` | very similar | the rocks differ only by a small sway | accepted |
| all 7 stretches | flat (K4) | 2D poses lifted to 3D; the only motion is a small sway; several hold the same pose in all frames | V07 (3D stretches) |
| `superman-hold`, `dead-bug`, `planche-*` | acceptable | read as intended but small | accepted |

Everything else reads as intended in the frame sequences: the movement shows within one loop, contact points are plausible (feet and hands on the floor, bar or box in the right place), and what makes the variant different is visible.

Open for the owner (phone): clip smoothness, pace, and whether the new pull-up and push-up variants feel right.
