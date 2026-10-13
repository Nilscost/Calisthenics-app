# Task for a small model: write Blender poses for ONE group

Follow these steps exactly. Don't explore the repo beyond the files named here. **Don't look at images.** You check poses with text output only.

## 0. Your group
You get a group name (e.g. `core`) and its list of ids from `work/clips-bakeoff/pose-groups.json`. You write **one file**: `tools/blender/families/<group>.py`. If the file already exists, keep the ids that print `OK` and add the missing ones.

**Never edit any other file.** Never run `git`.

## 1. Read (once)
1. `tools/blender/POSES.md`, sections "World and figure", "A pose", "Props".
2. `tools/blender/families/squat_lowerlegs.py`: the worked example (`squat_air`). Copy its structure.
3. For each of your ids, only its entry in `work/clips-bakeoff/catalog-brief.json` (`instructions`, `formCues`, `unilateral`, `equipment`, `clipKind`). Fetch one entry with:
   ```
   python3 -c "import json;print(json.dumps(json.load(open('work/clips-bakeoff/catalog-brief.json'))['<id>'],indent=1))"
   ```

## 2. Write each exercise (one function per id)
```python
def my_exercise():
    a = base_pose(...)      # start pose (also the still image): the clearest moment
    b = base_pose(...)      # the other end of the movement
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[...])
```
Register every id at the bottom of the file: `EXERCISES = {"<id>": my_exercise, ...}`.

Rules:
- **Facing and sides.** The figure faces +X and +Y is its left. For per-side exercises the **right** side (`_r`) does the work, because it faces the camera.
- **Same fields in every key.** Build every key from `base_pose(...)` with the same overrides. If a limb uses `wrist_l` (world) in one key, it uses `wrist_l` in all keys, never `wrist_l_rel`.
- **Contacts:** a hand that touches something gets a world target (`wrist_l=[x,y,z]`). A free-moving limb gets a relative target (`wrist_l_rel=[...]`). Heights:

  | Contact | z (m) |
  |---|---|
  | foot flat (ankle) | 0.08 |
  | hand flat (wrist) | 0.035 |
  | knee on the floor (ankle with the shin on the floor) | ≈ 0.06–0.08 |
  | sitting (pelvis) | ≈ 0.10–0.12 |
  | lying on the back or front (pelvis) | ≈ 0.11 |
- **Reach:**
  - Shoulder → wrist ≤ 0.57.
  - Hip → ankle ≤ 0.87. The hip is 0.09 to the side of the pelvis centre and 0.03 down along the trunk.
  - If the tool says "out of reach", move the pelvis closer, or move the target.
- **Trunk pitch** (`p1` lower, `p2` upper):
  - 0 upright, +40 leaning forward;
  - +90 lying face down with the head toward +X;
  - −90 lying on the back with the head toward −X;
  - ≈ 180 upside down (handstand).
- **Side-lying** (side plank): `p1 = p2 = 90` and `roll = -90`. The right side is then up, and the figure is supported on the left forearm.
- **Holds** (`clipKind` "hold"): two keys that differ only slightly (a breath: pelvis ±1–2 cm). Use `seconds=3`.
- **Stretches:** key a = the start position, key b = deeper into the stretch. Use `seconds=4`.
- **Props** (equipment the instructions mention) go in `props` as listed in POSES.md:
  - wall: a thin box;
  - bench, step, chair: a box or `chair`;
  - bar: `bar`;
  - band, towel, dip bars: `rod`;
  - dumbbell or kettlebell: `weight`.

## 3. Check: text only
```
cd /Users/nils/Documents/hermes/Apps/calisthenics && source /root/toolchain/env.sh && export PYTHONDONTWRITEBYTECODE=1
python3 tools/blender/poses.py <id> [<id> ...] --describe
```
For each id it prints `OK` or problems, then 4 moments (t = 0, 0.25, 0.5, 0.75). Each moment lists:
- pelvis height and x;
- head height;
- trunk angle from vertical;
- knee and elbow angles (180 = straight, 90 = right angle);
- the hip angle between thigh and trunk (180 = straight body);
- right wrist and ankle heights;
- **which body parts touch the floor**.

Compare the text with the exercise's instructions:

| Exercise type | What the text must show |
|---|---|
| Push-up (any) | top: elbows ≥ 165; bottom: elbows 60–100, chest/head z ≤ 0.30; hip angle ≥ 160 at all times (straight body); hands on the floor (wrist on floor) unless the hands are on a box or wall |
| Squat | top: knees ≥ 165; bottom: knees ≤ 70 for full depth; ankles stay at 0.08 (heels down) |
| Lunge / split squat | front knee ≈ 90 at the bottom; rear knee near the floor (knee_r or knee_l z ≤ 0.15) |
| Plank (forearm) | trunk 85–95; hip angle ≥ 165; elbows ≈ 90; elbows, wrists and toes on the floor |
| Hollow / lying | pelvis z ≈ 0.11, trunk ≈ 90 (lying); shoulders and legs off the floor where instructed |
| Hangs (bar) | wrists at the bar height; nothing on the floor; elbows ≥ 165 for a dead hang |
| Bridge | feet (and hands/head as instructed) on the floor; pelvis well above 0.3 at the top |
| L-sit | hands on the floor/box; pelvis off the floor (z > 0.12); knees per level (tuck ≤ 60, straight ≥ 165) |
| Stretch | the parts the instructions put on the floor are listed under "on floor" |

**Fix and re-run until every id prints OK and its text matches the instructions.** Change one or two numbers at a time.

## 4. Variants must differ
Each id must differ visibly from its siblings, the way its instructions say: assisted vs. free, tuck vs. straight, feet elevated, one arm or one leg, wide vs. narrow. Never register the same function for two ids.

## 5. Report (final message, at most 20 lines)
One line per id: `<id>: OK | <one-line description> | key numbers (e.g. bottom elbows 75, chest 0.22)`, or `<id>: PROBLEM <what>`. Then any equipment you could not show.
