# Blender render review — 2026-10-13

Snapshot: 2026-10-13 15:26:31 CEST. I read `tools/blender/POSES.md`. This is a visual pass over rendered Blender outputs, not a joint-data or source-pose validation. The contact sheets paired each available `blender/start.png` with motion samples near 25%, 50%, and 75% of the available frame sequence. I checked the six known-limit cases first where renders existed. The render was still progressing during this pass; IDs marked pending were not accepted as reviewed output.

At this snapshot there are 154 `joints.json` directories on disk. **100 IDs are OK, 6 need a fix and rerender, and 48 are pending** (five stale frame sets and 43 with no start/frames). The handoff doc listed 152 IDs; the live directory count differed, so the list below follows the directories actually present. Five fully written frame sets are older than their current joints and remain pending. Of the accepted known limits, only `scapular-pull` and `planche-adv-tuck` had current renders; `stretch-sleeper`, the four weighted variants, `stretch-thoracic`, and `stretch-wrist-flexor` remain pending with no start image. Contact sheets and sampled frames were temporary review aids under `/tmp/blender-review/` and are not committed evidence.

Severe visible defects: the six `fix / rerender pending` cases below have the wall or door hiding most or all of the model. The root task has assigned camera/prop corrections and will recheck the rerenders. These six are not marked OK.

- `arch-hang` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `arch-hold-y` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `arch-rocks` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `archer-row` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `barbell-rdl` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `barbell-squat` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `bridge-back` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `bridge-head` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `bridge-incline` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `bridge-single-leg` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `bridge-table` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `bridge-wall-walkdown` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `calf-raise` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `calf-raise-single` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `calf-raise-single-loaded` — **fix / rerender pending** — the wall covers virtually the whole figure; only a small body sliver is visible.
- `chair-assisted-pullup` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `chinup-full` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `copenhagen-side-plank` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `copenhagen-side-plank-short` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `dead-bug` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `dead-hang` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `dip-negative` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `dip-parallel` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `dip-support-hold` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `dumbbell-rdl` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `dumbbell-row` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `dumbbell-single-leg-rdl` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `flexed-arm-hang` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `front-lever-adv-tuck` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `front-lever-straddle` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `front-lever-tuck` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `german-hang` — **pending** — no Blender `start.png` or frames yet (0/72 frames).
- `glute-bridge` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `goblet-squat` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `hollow-hold` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `hollow-one-leg` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `hollow-rocks` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `hollow-tuck` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `hspu-wall` — **fix / rerender pending** — the wall fully occludes the figure in the start frame; only two small limb markers show.
- `hspu-wall-negative` — **fix / rerender pending** — the wall fully occludes the figure in the start frame; only two small limb markers show.
- `inverted-row` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `inverted-row-bent-knees` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `inverted-row-feet-elevated` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `inverted-row-wide` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `jefferson-curl` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `kettlebell-deadlift` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `kettlebell-one-arm-row` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `kettlebell-single-leg-rdl` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `kettlebell-suitcase-carry` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `kettlebell-swing` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `kettlebell-swing-one-arm` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `l-sit-advanced-tuck` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `l-sit-floor` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `l-sit-foot-supported` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `l-sit-one-foot` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `l-sit-one-leg` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `l-sit-tuck` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `leg-raise-lying` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `muscle-up-bar` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `nordic-banded` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `nordic-curl` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `nordic-negative-banded` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pallof-hold` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pallof-press` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pike-pushup` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pike-pushup-elevated` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `planche-adv-tuck` — **OK with known limit** — static advanced tuck hold without breathing; sampled frames show no major framing defect.
- `planche-lean` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `planche-tuck` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `plank` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `plank-shoulder-tap` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pullup-band-assisted` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pullup-chest-to-bar` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pullup-full` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pullup-negative` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pushup-archer` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pushup-diamond` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pushup-feet-elevated` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pushup-incline` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pushup-incline-high` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pushup-knee` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pushup-one-arm` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pushup-one-arm-negative` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pushup-standard` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `pushup-wall` — **fix / rerender pending** — the wall hides the torso and working arms; only the head and lower legs peek around it.
- `rdl-bodyweight` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `reverse-hyperextension` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `row-band` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `scapular-pull` — **OK with known limit** — the body rises only about 1 cm and elbows bend slightly; contact and framing read clearly.
- `side-plank` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `side-plank-leg-raise` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `single-leg-deadlift` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `slide-hamstring` — **pending** — frames predate the current `joints.json`; rerender and review the current output.
- `slide-negative` — **pending** — frames predate the current `joints.json`; rerender and review the current output.
- `slide-negative-single` — **pending** — frames predate the current `joints.json`; rerender and review the current output.
- `slide-single-leg` — **pending** — frames predate the current `joints.json`; rerender and review the current output.
- `split-squat` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `split-squat-bulgarian` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `squat-air` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `squat-pistol` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `squat-pistol-assisted` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `squat-pistol-box` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `squat-pistol-counterbalance` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `squat-shrimp` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `squat-shrimp-advanced` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `squat-shrimp-intermediate` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `stretch-90-90` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-ankle-mobility` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-back` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-butterfly` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-calf-wall` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-chest-door` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-cobra` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-frog` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-hamstring` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-hamstring-standing` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-hip-flexor` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-lat-wall` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-pancake` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-pigeon` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `stretch-pike-one-leg` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-quad-couch` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-quad-standing` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-rear-clasp` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-shoulder` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-shoulder-backbend` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-sleeper` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-supine-twist` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-thoracic` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-wrist-biceps` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-wrist-extensor` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `stretch-wrist-flexor` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `superman-hold` — **pending** — frames predate the current `joints.json`; rerender and review the current output.
- `tibialis-raise` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `tibialis-raise-single` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `towel-door-row` — **fix / rerender pending** — the door blocks most of the body; the start shows only a partial torso and legs.
- `v-sit-floor` — **pending** — no Blender `start.png` or frames yet (0/72 frames).
- `v-up` — **pending** — no Blender `start.png` or frames yet (0/72 frames).
- `walking-lunge` — **OK** — start plus sampled motion frames at roughly 25%, 50%, and 75%; no major motion or framing defect visible.
- `wall-handstand-hold` — **fix / rerender pending** — the wall fills nearly the entire frame and hides the handstand figure.
- `warmup-arch-hang` — **pending** — no Blender `start.png` or frames yet (0/72 frames).
- `warmup-dead-bug` — **pending** — no Blender `start.png` or frames yet (0/72 frames).
- `warmup-hinge-easier` — **pending** — no Blender `start.png` or frames yet (0/84 frames).
- `warmup-shoulder-band` — **pending** — no Blender `start.png` or frames yet (0/84 frames).
- `warmup-shoulder-towel` — **pending** — no Blender `start.png` or frames yet (0/84 frames).
- `warmup-squat-easier` — **pending** — no Blender `start.png` or frames yet (0/72 frames).
- `warmup-squat-sky-reach` — **pending** — no Blender `start.png` or frames yet (0/84 frames).
- `warmup-support-hold` — **pending** — no Blender `start.png` or frames yet (0/72 frames).
- `warmup-wrist-prep` — **pending** — no Blender `start.png` or frames yet (0/96 frames).
- `weighted-dip` — **pending** — no Blender `start.png` or frames yet (0/72 frames).
- `weighted-glute-bridge` — **pending** — no Blender `start.png` or frames yet (0/72 frames).
- `weighted-pullup` — **pending** — no Blender `start.png` or frames yet (0/72 frames).
- `weighted-pushup` — **pending** — no Blender `start.png` or frames yet (0/60 frames).
- `weighted-squat` — **pending** — no Blender `start.png` or frames yet (0/72 frames).

## Corrections prepared after the render snapshot

The continuation's Luna implementer corrected the six occlusion cases by moving their cameras to the subject side (`cam_az=-30`). The coordinator independently regenerated only the eight changed/new IDs and re-ran the geometry checks: all print OK. The coordinator inspected the German hang, Jefferson curl and corrected HSPU stick sheets; these checks accept pose drafts, not the still-pending Blender renders or training technique.

- `jefferson-curl`: new unloaded roll-down, planted feet, straight dangling arms; bottom upper trunk/head now curl below the pelvis rather than showing only a hinge.
- `german-hang`: new foot-supported hold, bar behind the torso; elbows 170–179 degrees at sampled moments. The preview helper was corrected to respect a bar's x offset, matching the renderer.
- `calf-raise-single-loaded`, `pushup-wall`, `towel-door-row`: camera correction, contact targets retained.
- `hspu-wall`, `hspu-wall-negative`: camera correction plus straight body/legs throughout descent (hips/knees about 168 degrees, elbows about 74/73 at bottom); toes slide along the wall rather than folding the legs into an inverted crouch.
- `wall-handstand-hold`: camera correction, toes meet the wall, near-straight body/limbs. Static hold: breathing movement omitted to preserve contact/reach geometry.

All eight require a new Blender render, visual recheck, seam/size checks and installation. The two new IDs were absent when the owner's current render enumerated its input list; rerun `--resume` after that process finishes. No shipped media or app aliases were changed.


## Addendum — newly completed motion-frame review (2026-10-13)

The reviewer initially miscounted frames by looking in `blender/`; the coordinator caught this and required a corrected review from `blender/frames/`. I sampled frames 0000, 25%, 50% and 75% from each current complete set below. Current complete sets had exactly the expected contiguous frame count and were newer than their joint file at inspection.

- `slide-hamstring` — **OK** — sampled loop shows both legs sliding from bent to extended and returning, with the bridge and hand support maintained.
- `slide-negative` — **OK** — sampled loop shows bilateral leg extension and return from the bent bridge position; motion reads clearly.
- `slide-negative-single` — **OK** — raised leg stays lifted while the working leg slides between bent and extended positions.
- `slide-single-leg` — **OK** — one leg remains raised while the planted leg performs the sliding extension and return.
- `stretch-90-90` — **OK** — seated 90/90 setup is clear; the torso leans forward through the sampled stretch and returns.
- `stretch-ankle-mobility` — **FIX / rerender pending** — all sampled frames retain the severe wall occlusion; the figure remains mostly hidden.
- `stretch-back` — **OK** — sampled quadruped loop shows a readable forward/back weight shift with hands and knees supporting the figure.
- `stretch-butterfly` — **OK** — seated soles-together position stays clear while the torso moves through a forward lean and returns.
- `stretch-calf-wall` — **FIX / rerender pending** — all sampled frames retain severe wall occlusion; the wall hides most of the body.
- `stretch-chest-door` — **pending** — the earlier full-frame door start remains the last visual finding; this ID is actively being corrected and was excluded from this motion review.

The original 100 IDs marked OK in the historical snapshot were sampled at distinct motion frames; a separate visual-sheet review confirmed that. This correction changes only the new-ID frame review above.

Known-limit sleeper/thoracic/wrist-flexor and the pending wall lat/wrist-biceps stretches have not yet been visually accepted.

## Stretch corrections prepared

The coordinator independently regenerated `stretch-ankle-mobility`, `stretch-calf-wall` and `stretch-chest-door` after Luna's fixes; all three printed OK. The two front-wall cameras now view the subject side. The doorway prop is a narrow frame with an opening instead of a solid panel; its hand-contact target is unchanged, and the camera views the opposite side of the former blocking plane. The coordinator inspected the doorway stick sheet; the figure and working arm are visible. These three remain **rerender and rendered review pending**, not accepted shipped clips.

The original 100 visually accepted current clips encoded and passed installer dry validation (100 prepared, zero problems; no shipped asset changes). See `blender-encode-2026-10-13.log` and `blender-dry-install-2026-10-13.log`. Maximum reported CRF30 size was 27 KB. The six largest seam outliers were checked in first/last frame sheets; no abrupt pose jump was evident. This is frame-sheet screening, not video playback or technique approval. Coordinator video-format spot checks confirmed H.264 Main, yuv420p, 480×360, 24 fps for the largest-size and largest-seam clips.


## Addendum — new stretch renders (2026-10-13)

Reviewed current `blender/frames/` sets at 0000, 25%, 50%, and 75%. Each reviewed set matched its joint frame count and the newest render files were newer than `joints.json` at inspection.

- `stretch-cobra` — **FIX / pose review needed** — the sampled torso stays essentially flat on the floor; the intended press-up chest lift with hips down does not read.
- `stretch-frog` — **OK with visibility limit** — wide-knee, low-hip kneeling stretch is readable in side view, but the camera angle makes the knee width hard to judge; sampled motion is subtle.
- `stretch-hamstring` — **OK** — seated straight-leg position and forward hip hinge are visible across the sampled loop.
- `stretch-hamstring-standing` — **FIX / pose review needed** — front heel is ahead, but the front knee bends substantially through the sampled hold and the hip hinge is modest, weakening the straight-leg hamstring stretch cue.

`stretch-hip-flexor` had only 20/96 frames at inspection and remains pending; `stretch-lat-wall` had no current start or frames and remains pending. Regenerated `stretch-ankle-mobility`, `stretch-calf-wall`, and `stretch-chest-door` were excluded because their existing renders are stale against the changed joints.

## Further form corrections prepared

`stretch-cobra` now lifts the chest clearly with pelvis z=0.11 and hands/legs grounded; the deepest sampled head height is 0.58 m. `stretch-hamstring-standing` keeps the working knee at 168–169 degrees with the ankle planted and a visible trunk hinge to 50 degrees. The coordinator independently regenerated only these two IDs, observed OK geometry checks, and inspected both stick sheets. Both require rerendering and rendered review. Including these two, 13 changed/new poses are awaiting that review.

## Addendum — review of the 32 renders from the 17:12 run, plus two changed ids (2026-10-13 evening)

Method: for each id, a sheet of `start.png` and the frames at 25 %, 50 % and 75 % of the sequence, checked to be newer than `joints.json` (all 34 were). Frame counts match the joint frame counts. This is a by-eye review of renders; nothing here is phone-tested.

- `stretch-hip-flexor` — **OK** — half-kneeling lunge, front shin vertical, back knee down; the motion is small but the position reads.
- `stretch-lat-wall` — **FIX** — the wall covers the whole body; only one arm shows. Camera changed from az 30 to az -30 (as for the calf-wall and ankle fixes); `poses.py stretch-lat-wall --describe` prints OK. Needs a re-render.
- `stretch-pancake` — **OK** — wide straddle, hips on the floor, trunk folding forward between the legs.
- `stretch-pike-one-leg` — **OK** — seated on a chair with one heel on the seat, trunk hinging toward the straight leg.
- `stretch-quad-couch` — **OK** — rear shin up the wall, knee on the floor; the figure is small but clear.
- `stretch-quad-standing` — **OK** — standing, heel pulled to the glute with the hand; knees stay together.
- `stretch-rear-clasp` — **OK** — hands behind the back; the clasp is hidden from the front-side camera but the arm line reads.
- `stretch-shoulder` — **OK** — cross-body arm pulled in by the other arm.
- `stretch-shoulder-backbend` — **OK** — arms overhead, slight lean back; subtle motion.
- `stretch-sleeper` — **OK (known limit)** — pelvis floats about 8 cm above the floor; the side-lying shape with the front arm bent reads.
- `stretch-supine-twist` — **OK** — on the back, knee crossing over, arms out; shoulders stay down.
- `stretch-thoracic` — **OK (known limit)** — the whole trunk rolls, and the raised arm reads as the rotation.
- `stretch-wrist-biceps` — **OK** — palm on the wall behind at shoulder height, body turning away.
- `stretch-wrist-extensor` — **OK** — quadruped, palms down, fingers toward the knees.
- `stretch-wrist-flexor` — **OK (known limit)** — nearly identical to the extensor clip (palms down, fingers toward the knees); the catalogue's "palms up" is not shown.
- `superman-hold` — **OK** — face down, arms and legs lifted off the floor; a small breathing motion.
- `v-sit-floor` — **OK** — hands on blocks, hips off the floor, legs raised straight at about 45 degrees.
- `v-up` — **OK** — lying flat, then arms and legs rising to the V and back.
- `warmup-arch-hang` — **OK** — hanging from the bar with the feet on a box; the arched shoulders read.
- `warmup-dead-bug` — **OK** — on the back, arms up, knees at 90 degrees, opposite arm and leg extending.
- `warmup-hinge-easier` — **OK** — standing, hands slide down the thighs, hinge with a slight knee bend.
- `warmup-shoulder-band` — **OK** — overhead, in front, behind pass with the band held wide.
- `warmup-shoulder-towel` — **OK** — same path as the band clip with a towel; the two look alike by design.
- `warmup-squat-easier` — **OK** — sits back to a chair and rises; the chair is behind the figure.
- `warmup-squat-sky-reach` — **OK** — arms overhead to a deep squat and back up.
- `warmup-support-hold` — **OK** — hands on bars at hip height, shoulders pushed down; small breathing motion.
- `warmup-wrist-prep` — **OK** — quadruped, hands rocking with the palms down.
- `weighted-dip` — **OK (known limit)** — dip on bars with a weight on the chest, no vest model.
- `weighted-glute-bridge` — **OK** — hips rise with a weight held on the hips.
- `weighted-pullup` — **OK (known limit)** — the pull-up reaches the bar with a weight on the chest.
- `weighted-pushup` — **OK (known limit)** — body line stays straight, with a weight on the back.
- `weighted-squat` — **OK (known limit)** — squat to depth with the weight at the chest.
- `towel-door-row` — **OK** — leaning back from the door edge, pulling to the chest; the door no longer hides the figure.
- `wall-handstand-hold` — **OK** — belly toward the wall, body vertical, hands on the floor; the wall stays behind the figure.
