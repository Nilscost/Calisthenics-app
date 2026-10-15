# 24 · Pose task for the R8 exercises (pick-up instructions)

Goal: a Blender pose for every new id in `docs/23-new-exercises.json`, so the owner can render them overnight on his Mac. Written 2026-10-14.

## What you write
- One file per pose group: `tools/blender/families/<group>.py`. The groups and their ids are in `docs/23-new-exercises.json` → `poseGroups` (24 groups, all new file names). `poses.py` loads every module in `families/` automatically.
- **Follow `tools/blender/HAIKU-TASK.md` steps 1–5 exactly**, with one change: the exercise facts come from `docs/23-new-exercises.json` → `exercises` (the new ids are not in `work/clips-bakeoff/catalog-brief.json` yet). Fetch one entry with:
  ```
  python3 -c "import json;d=json.load(open('docs/23-new-exercises.json'));print(json.dumps(next(e for e in d['exercises'] if e['id']=='<id>'),indent=1))"
  ```
  Use its `instructions`, `formCues`, `unilateral`, `equipment`, `clipKind` and `props`.
- Also read `tools/blender/POSES.md` (world, pose fields, props, anatomy checklist).

## Order and pace
- Do the groups that hold **batch 1** ids first (`renderBatch` in each exercise), then batch 2, then batch 3. Inside a group, pose every id anyway.
- Run **two groups at a time** (two Haiku agents in parallel, one group each). A coordinator (Opus or Sonnet) re-checks each group before the next pair starts.

## Props for new equipment (use existing prop kinds only; don't edit `render.py`)
| Equipment | Prop |
|---|---|
| rings | two `rod`s (r 0.015) hanging from a high point (a `bar` at z 2.3) down to `pt("wrist_l")` / `pt("wrist_r")` |
| ab wheel | `plate` (r 0.09, `normal=[0,1,0]`) at the hands' midpoint, centre z 0.09 |
| pole / stall bars (flag) | vertical `rod` (r 0.025) from z 0 to 2.4 |
| box, bench, step | `box` (or `chair`) |
| band | thin `rod` (r 0.008) from the anchor to the hand |
| slider | `plate` under the hand or foot |
| parallettes | two short `rod`s at y ±0.25, z ≈ 0.12 |
| cones / hurdles | small `box`es; leave them out if they hide the feet |

If something truly can't be shown, pose the body correctly without it and say so in the report.

## Hard rules
- **Never run `poses.py` without ids.** It rewrites every `joints.json` and forces a full re-render. Always pass the ids of your group.
- Never edit `poses.py`, `render.py`, `preview_sheet.py`, `encode.py`, another group's file, or anything in `app/`. Never run git (the coordinator commits).
- Every id prints `OK` with `--describe`, and its text matches the instructions (HAIKU-TASK.md §3 table). Siblings must differ visibly (§4).
- Per-side exercises: the right side works (faces the camera).
- Pilates and Army drill moves follow the same rules. Pilates rolls (rolling like a ball, open-leg rocker, seal) need `dense(...)` keys so the spine rolls instead of teleporting.

## Safe checks while Blender is running

Use `python3 tools/blender/check_new_poses.py --group <group> --describe` for read-only validation. Add `--write` only for an unaccepted new group once all checks pass. This wrapper rejects missing/unknown IDs, refuses writes to IDs already in the render manifests, validates the whole group before writing, and preserves identical JSON timestamps. Do not invoke the historical `poses.py --help`: it is unsupported and regenerates every cache. The coordinator handles preview sheets and acceptance. Core pose/render code remains unchanged.

## Coordinator check, per group
1. `python3 tools/blender/poses.py <ids…> --describe`: all `OK`.
2. `python3 tools/blender/poses.py <ids…> --preview /workspace/pv-<group>`: look at the sheets for 3–4 ids per group (always the hardest ones: inversions, flags, levers, rolls) against the anatomy checklist in POSES.md. Send fixes back.
3. Append the group's OK ids to `work/clips-bakeoff/render-batch-<n>.txt` (one id per line, `n` = the id's `renderBatch`).
4. Commit the group file: `R8-poses: <group> (<n> ids)`.

## The owner's overnight render (one line, no comments)
```
cd /Users/nils/Documents/hermes/Apps/calisthenics && /Applications/Blender.app/Contents/MacOS/Blender -b -P tools/blender/render.py -- $(cat work/clips-bakeoff/render-batch-1.txt)
```
Always pass the ids explicitly like this. Don't use `-- --resume` alone: most older clips no longer have their frames on disk, so it would re-render all of them. To continue an interrupted night, add `--resume` after the ids. Rough time: about 2 minutes per clip, so batch 1 (121 ids) ≈ 4 h, batch 3 (192) ≈ 6–7 h.

## After the render
`python3 tools/blender/encode.py <ids…>`, then `python3 tools/blender/install_clips.py <ids…> --dry`, then without `--dry`. `install_clips.py` only accepts ids that exist in `catalog.json`, so the catalog entries (doc 25, step 2) must land first.
