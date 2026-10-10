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

## V07 additions: the 11 new stretches (frame sequences in `frames-07/08-new-stretches-*.png`)
| Id | Verdict | Note |
|---|---|---|
| `stretch-pigeon` | acceptable | the crossed front shin is shown front-on; folds forward and back with the breath |
| `stretch-90-90` | acceptable | seated, one shin forward, the other leg to the side, leaning over the front shin |
| `stretch-frog` | good | wide knees, forearms down, hips rock back |
| `stretch-quad-couch` | good | back foot up the wall, hands on the front knee |
| `stretch-quad-standing` | weak | the small figure reads, but the held foot is hard to see |
| `stretch-hamstring-standing` | acceptable | front heel forward, hinge with the hands on the thigh |
| `stretch-lat-wall` | good | hands on the wall, hips back, chest sinking |
| `stretch-sleeper` | weak | side-lying with the arm up; the pressing hand is small |
| `stretch-wrist-flexor`, `stretch-wrist-extensor` | weak | the hand orientation (palms up / backs of the hands) is not drawn, so the two clips look alike; only the rocking differs |
| `stretch-thoracic` | good | the threaded arm travels from under the body to the ceiling |
All 18 stretches now colour the muscle that is stretched (primary) and a helper (secondary); the old 7 had none (K4). Still flat: the 7 older stretches keep their v1 poses (small sway only).

## V21b additions: 23 new clips for the RR pieces (frame sheets `frames-09..12-*.png`; `frames-11` is the corrected slide sheet)
| Id | Verdict | Note |
|---|---|---|
| `warmup-shoulder-band`, `warmup-shoulder-towel` | acceptable | straight arms travel over the head and back; the band or towel line between the hands is thin |
| `warmup-squat-sky-reach` | good | deep squat with hands together, then stand and reach up |
| `warmup-wrist-prep` | acceptable | quadruped rocking; the finger direction changes of the real prep are not drawn |
| `warmup-arch-hang` | weak | the figure hangs from the bar; the small shoulder-blade pull and arch are hard to see |
| `warmup-dead-bug`, `warmup-support-hold` | shared | same motion as `dead-bug` and `dip-support-hold` (declared in `SHARED_POSES`) |
| `dip-support-hold`, `dip-negative`, `dip-parallel` | good | parallel bars (new `pbars` scene), body hangs between them, lowers and presses; the negative is slow down, quick up |
| `rdl-bodyweight`, `single-leg-deadlift` | good | hip hinge with a flat back; the free leg rises behind |
| `nordic-negative-banded`, `nordic-banded`, `nordic-curl` | acceptable | straight body pivots at the knees, feet under an anchor block; the three differ only in range and speed; the band is not drawn |
| `slide-negative`, `slide-hamstring`, `slide-negative-single`, `slide-single-leg` | acceptable | bridge with the heels sliding out; the travel is short because the hip height limits the reach; single-leg versions hold one leg up |
| `pallof-press` | acceptable | band line anchored to the side, hands press out in front of the chest |
| `reverse-hyperextension` | good | prone over a table edge, legs swing up to body height |
| `plank-shoulder-tap` | acceptable | the lifted hand is small but alternates |
| `walking-lunge` | acceptable | steps forward, the back foot comes through |
Pose uniqueness check passes (`--check`: 95 poses; shared pairs declared).

## V25 additions: the C-E gap clips (`frames-13-c-e-gaps.png`)
| Id | Verdict | Note |
|---|---|---|
| `pullup-negative` | good | chin over the bar, then a slow lowering to straight arms |
| `scapular-pull`, `arch-hang` | weak | the movement is small by nature (shoulders rise a few centimetres with straight arms; the arch is slight); both read as a hang on the bar, the cue text carries the detail |
| `tuck-l-sit` (`l-sit-tuck`) | weak | seated with the knees tucked and a small hold sway; the lifted hips are hard to see from this angle |
| `bridge-incline` | acceptable | hands on a chair seat behind, hips pushed up, body in a diagonal line |

## V27 additions: dumbbell, barbell and vest clips (`frames-14-weights.png`)
| Id | Verdict | Note |
|---|---|---|
| `goblet-squat` | acceptable | dumbbell held at the chest; the dumbbell is a tiny prop |
| `dumbbell-rdl`, `dumbbell-single-leg-rdl` | acceptable | arms hang with the weights while the hips hinge; the dumbbells are tiny props |
| `dumbbell-row` | acceptable | hand and knee on a stable seat, the working elbow travels to the hip; the figure is large in the frame |
| `barbell-squat`, `barbell-rdl` | weak | the bar is a thin line across the hands/shoulders and hard to see |
| `weighted-glute-bridge` | weak | the weight on the hips is a tiny mark |
| `weighted-pushup`, `weighted-pullup`, `weighted-dip`, `weighted-squat` | shared | the same pose as the plain exercise (declared in `SHARED_POSES`); the vest is not drawn, the cue text carries it |

## L02 additions (`frames-15-pushup-steps.png`)
| Id | Verdict | Note |
|---|---|---|
| `pushup-wall` | good | standing against a wall, the chest travels toward it, straight body |
| `pushup-incline-high` | good | hands on a table-height block, the same body line as the low incline but a higher support |

## L03 additions (`frames-16-pullup-path.png`)
| Id | Verdict | Note |
|---|---|---|
| `dead-hang` | acceptable | straight-arm hang with a small sway; reads as a hang |
| `flexed-arm-hang` | acceptable | the top of a pull-up held; legs bent behind |
| `chair-assisted-pullup` | good | a chair under the feet, the body rises with the arms and the legs help |
| `chinup-full` | weak | the same movement as the pull-up with a little more lean; the underhand grip is not drawn (cue text carries it) |

## L04 additions (`frames-17-rows.png`, the towel row frames are from the corrected version `l04b`)
| Id | Verdict | Note |
|---|---|---|
| `inverted-row-wide` | good | the inverted row with the hands wide apart |
| `towel-door-row` | weak | leaning back from a door with a towel; the door slab hides part of the figure, the towel is a thin line |
| `kettlebell-one-arm-row` | acceptable | the same set-up as the dumbbell row with a bell; the bell is a short line |

## L05 additions (`frames-18-single-leg.png`)
| Id | Verdict | Note |
|---|---|---|
| `squat-shrimp-intermediate` | acceptable | the back knee goes down to the floor, the hand reaches back; close to the beginner version (the difference is range and the hold, in the cue text) |
| `squat-shrimp-advanced` | acceptable | deeper, the rear foot held behind |
| `squat-pistol-box` | acceptable | a box behind the figure, the free leg out in front |
| `squat-pistol-counterbalance` | acceptable | arms straight out with a small weight |

## L07 additions (`frames-19-lsit-steps.png`)
| Id | Verdict | Note |
|---|---|---|
| `l-sit-foot-supported`, `l-sit-one-foot` | weak | seated with the hips just off the floor; the heels on the floor and the one lifted leg are small differences from the side view |
| `l-sit-tuck`, `l-sit-advanced-tuck` | acceptable | the knees move from the chest to level thighs |
| `l-sit-one-leg` | acceptable | one leg straight, one tucked |

## L08 additions (`frames-20-core-planes.png`)
| Id | Verdict | Note |
|---|---|---|
| `hollow-tuck`, `hollow-one-leg` | acceptable | supine, shoulders lifted, knees over the hips; one leg extended in the second |
| `hollow-rocks` | weak | the rocking is a small sway of the whole body; reads as a hollow hold with movement |
| `pallof-hold` | acceptable | the band line to the anchor, arms pressed out, held |
| `copenhagen-side-plank-short` | acceptable | like the long lever with the knee on the chair; the figure is large in the frame |
| `kettlebell-suitcase-carry` | weak | walking in place with a bell at the side; the bell is a short line near the knee |

## L09 additions (`frames-21-back-bridge.png`)
| Id | Verdict | Note |
|---|---|---|
| `bridge-table` | good | reverse tabletop, hips rise and lower |
| `bridge-head` | acceptable | arched hold with the head down; the towel under the head is not drawn |
| `bridge-wall-walkdown` | weak | the figure leans on a wall and the hands walk down; a standing back bend against a wall is hard to read from the side |
