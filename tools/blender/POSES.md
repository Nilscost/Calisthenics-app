# Writing exercise poses for the Blender clips

Every exercise clip in the app is rendered from **joint motion that you write in Python**. You write a few key poses per exercise. The toolkit (`tools/blender/poses.py`) interpolates them, solves arms and legs with two-bone IK so hands and feet stay exactly on their targets, and writes `work/clips-bakeoff/<id>/joints.json`. Later, Blender (`tools/blender/render.py`, run on the owner's Mac overnight) turns that file into frames. No Blender is needed while you write: you check poses with stick-figure sheets.

## Files
- **Your module:** `tools/blender/families/<group>.py`. It ends with `EXERCISES = {"<catalog id>": function, ...}`. Edit only your module.
- **Do not edit** `poses.py`, `render.py`, `preview_sheet.py`, `encode.py`, other people's modules, or anything in `app/`. If you need a helper, write it inside your module. If you need a prop kind that doesn't exist, approximate it with the existing ones (box/rod) and note it in your report.
- Exercise facts: `work/clips-bakeoff/catalog-brief.json` (name, instructions, form cues, unilateral, equipment, clipKind rep/hold). **Read the instructions of every id before posing it.** The clip must show what makes this variant different from its siblings.
- Reference examples: `pullup()`, `pushup_fe()`, `pistol()`, `pigeon()` at the bottom of `poses.py`.

## Commands (sandbox)
```
cd <repo-root>
source /root/toolchain/env.sh
export PYTHONDONTWRITEBYTECODE=1
python3 tools/blender/poses.py <id> [<id> ...] --preview /workspace/pv-<group>   # writes joints.json + a PNG sheet per id
python3 tools/blender/poses.py --list                                          # every registered id
```
The command prints `OK`, or a problem for each id:
- `target … out of reach`: a hand or foot can't reach its target, so the contact is broken. Move the pelvis or the target.
- `… sinks into the floor`: a joint's body surface is more than 2 cm below the floor.

**Every id must print OK.** Then open its sheet with `vision_analyze` and judge the anatomy (see the checklist below). Iterate until it's right.

The sheet has 6 rows (moments in the loop) × 3 columns (side view from the figure's right, front view, the render camera's direction). Orange = the figure's right limbs, dark = left limbs, blue = props, red dots = hands and feet.

## World and figure
- Z is up, the floor is z = 0, units are metres. The figure **faces +X**, and **+Y is its left**. The camera sits on the figure's **right** side (−Y), turned toward the front by `cam_az` degrees (0 = pure side view, 90 = straight from the front), and raised by `cam_el` degrees.
- **Unilateral exercises** (per side): the **right (near) side works**, so the camera sees it.
- Body: about 1.75 m tall. Standing pelvis height 0.955. Segment lengths are in `L` in `poses.py`: thigh 0.44, shin 0.43, foot 0.20, upper arm 0.30, forearm 0.27, hand 0.17, shoulder half-width 0.18, hip half-width 0.09.
  - Arm reach (shoulder → wrist) is ≤ 0.57.
  - Leg reach (hip → ankle) is ≤ 0.87.
  - The hip joint is 3 cm below the pelvis centre along the trunk. The shoulder is 3 cm below the chest point.
- Contact heights: an ankle with the foot flat on the floor sits at z ≈ 0.08. A wrist with the hand flat on the floor sits at z ≈ 0.035. A knee resting on the floor sits at about 0.05. The figure lying flat has its chest point at about 0.11.

## A pose (one key)
Start from `base_pose(...)` (a neutral standing pose with every field) and override what changes. **Every key of one exercise must have the same fields:** don't mix `wrist_l` (world) in one key with `wrist_l_rel` in another.

| Field | Meaning |
|---|---|
| `pelvis` | [x, y, z] of the pelvis centre |
| `p1`, `p2` | pitch of the lower and upper trunk (deg): 0 upright, +90 lying face down with the head toward +X, −90 on the back with the head toward −X, ~40 leaning forward |
| `yaw` | turn about Z (deg) |
| `roll` | about the body's long axis: with p ≈ 90, roll +90 turns the left side up (lying on the right side) and −90 turns the right side up (side plank on the left arm, seen from the near side) |
| `hp` | head pitch relative to the upper trunk |
| `ankle_l` / `ankle_r` | world target, OR `ankle_l_rel`: offset from the hip |
| `knee_l` | knee pole: the direction the knee points (e.g. [1,0,0] forward, [0,0,-1] down) |
| `foot_l` | direction ankle → toe ([1,0,0] flat forward; [0,0,-1] toes pointing down, as on a push-up on the toes) |
| `wrist_l` | world target, OR `wrist_l_rel`: offset from the shoulder |
| `elbow_l` | elbow pole |
| `hand_l` | direction wrist → fingertips |
| `palm_l` | palm normal: the way the palm faces, e.g. [0,0,-1] flat on the floor, [1,0,0] facing a bar in front |
| `grip_l` | finger curl, 0 = flat, 0.95 = wrapped round a bar |

Helpers:
- `both(wrist=[x,y,z], grip=0.9)` mirrors a field to both sides (y is flipped for the right side).
- `rep(top, bottom)` gives the usual rep timing.
- `dense(fn, keyed, steps)` keys a function of a few parameters densely. Use it when interpolating positions would cut a curve, e.g. a body pivoting on its feet.
- `pt("wrist_r", (0,0,0))` is a prop point attached to a joint.
- `sample(keys, t)` gives the interpolated pose at time t.

Your function returns:
```python
dict(seconds=3.0, keys=[(t, pose), ...], cam_az=30, cam_el=10, props=[...])
```
- Keys go from t = 0 to < 1, and the loop returns to the first key. The first key is the start frame, shown as the still image too, so make it the clearest pose.
- Typical lengths: reps 2.5–4 s, holds 3 s (with a small breathing or settling motion so the loop isn't frozen), stretches 4 s (ease in and out).

## Props (kinds render.py knows)
```python
dict(kind="bar", z=2.3, half=0.65, r=0.017, x=0)           # pull-up bar with two posts; use z≈1.0 for a low bar / table edge
dict(kind="chair", x=-1.5, seat=0.45)                      # chair, back on its -X side
dict(kind="box", center=[x,y,z], size=[sx,sy,sz])          # step, bench, box, wall (thin box), mat (very thin), door (thin)
dict(kind="rod", a=[x,y,z] or pt(...), b=..., r=0.017)     # parallel bars, dip bars, band, towel (r 0.02), barbell, stick
dict(kind="weight", type="dumbbell"|"kettlebell", joint="wrist_r", off=[0,0,0])   # held in that hand
dict(kind="plate", center=[x,y,z], r=0.12, normal=[0,0,1]) # slider / disc under a hand or foot
```
- A wall is a box behind or in front of the figure, e.g. `center=[-0.35, 0, 1.2], size=[0.05, 1.6, 2.4]`.
- Dip bars are two rods at y = ±0.25, z ≈ 1.0, along X.
- A band is a thin rod (r 0.008) from a fixed anchor to `pt("wrist_r")`.

## Anatomy checklist (judge every sheet; fix before reporting)
1. Contacts are exact. Hands on the bar, floor or bench; feet flat where the instructions say. **Nothing floats** above a surface it should touch, and nothing sinks into it. Remember the planche/feet-off-the-floor kind of rule: if the cue says feet off the floor, they must be off.
2. No joint bends the wrong way. Knees and elbows bend toward their pole. A straight limb really is straight (≥ 0.98 of reach) when the cue says straight.
3. The movement is the right one and reads within one loop: full range as instructed (depth, chin over the bar, chest to the floor…), with the right tempo shape.
4. **The variant's difference is visible:** feet elevated vs. standard, archer vs. wide, tuck vs. straddle, assisted (band, chair, box) vs. free. Don't reuse a sibling's pose for a different variant.
5. Balance is plausible: the centre of mass sits over the support for standing and one-leg moves.
6. The framing works. Pick `cam_az`/`cam_el` so the working limbs aren't hidden behind the body. For floor exercises, a slightly higher `cam_el` (15–30) helps.
7. The loop is seamless: the last key returns smoothly to the first.

## Report (your final message)
For each id:
- `OK`, with a one-line description and your 1–5 self-score after checking the sheet, **or** `PROBLEM`, with what is wrong.

Then list any prop or helper you missed. Keep the report short.
