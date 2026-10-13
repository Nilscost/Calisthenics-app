# R7.2-T09: clip re-pose, stick-sheet review (F10, F11)

Status: **poses written and checked on stick sheets; NOT rendered.** The Blender render runs on the owner's Mac, so the installed clips
`app/src/main/assets/demos/{warmup-shoulder-band,pallof-press,pallof-hold}.mp4` are still the R7.1 ones.
Frames only; nothing here was reviewed as video.

Sheets: `warmup-shoulder-band.png`, `pallof-press.png`, `pallof-hold.png` (six moments x side / front / render camera; made with
`poses.py <ids> --preview`). `poses.py <ids> --describe` prints OK for all three.

| id | What changed | Check | Score |
|---|---|---|---|
| warmup-shoulder-band | Arms stay straight (elbow >= 168 deg in every frame; the old clip moved the wrists in a straight line between two keys, so the arms bent half-way). The wrists travel on a dense arc about the shoulder axis: hanging in front of the thighs, forward, overhead, down behind the back (about 40 deg behind plumb), and back along the same path. Wide overhand grip (0.88 m between the hands, was 0.60). Band rod radius 0.012 (was 0.008). Camera three-quarter (az 55, el 10; was az 35). Same 3.5 s / 84 frames as before. | Wrist-to-head distance never below 0.52 m. Hands pass 0.44 m either side of the midline. | 4 |
| pallof-press | Side-on to the anchor (the band runs straight to the figure's right), wide stance, both hands together on the midline at chest height. Hands start at the sternum (0.20 m in front of the chest, elbows bent about 60 deg), press straight forward to straight arms (0.53 m, elbow about 160-165 deg), pause, return. Camera az 58 (was 30) so the band's sideways pull and the press both read. Band radius 0.010. Same 3.5 s / 84 frames. | Reach OK (no "out of reach"). The band stays horizontal and at a right angle to the press. | 4 |
| pallof-hold | Shares the pose family: arms straight out, hands together, a small sideways give toward the anchor (1.2 cm) and a breath. Camera az 58. Same 3.0 s / 72 frames. | Reach OK. | 4 |

Known limits (not fixed here):
- The stick sheet cannot show whether the band reads as a band at 640x480; it is a thin rod. If the render shows it too faint, raise `r` (0.012 / 0.010) in `warmup_weights.shoulder_band` and `core.PALLOF_BAND`.
- `warmup-shoulder-towel` still uses the old `_shoulder_loop` pose (bent arms, straight-line interpolation). It has the same problem as F10; it was outside this release's scope.
- The standing legs of the Pallof clips keep the base stance (knees about 157 deg), as in the other standing core clips.

## Owner render (one line)
```
cd /Users/nils/Documents/hermes/Apps/calisthenics && /Applications/Blender.app/Contents/MacOS/Blender -b -P tools/blender/render.py -- warmup-shoulder-band pallof-press pallof-hold
```
Frame counts are unchanged (84 / 84 / 72), so no stale frames are left behind. Then, in the sandbox: `python3 tools/blender/encode.py warmup-shoulder-band pallof-press pallof-hold`,
`python3 tools/blender/install_clips.py warmup-shoulder-band pallof-press pallof-hold --dry`, then without `--dry`, then the full verify.

Only the three ids' `joints.json` are newer than their frames. (During this task `poses.py` was once run without ids in the sandbox, which touched every
`joints.json`; their modification times were set back to just before their frames so `--resume` is unaffected. The contents are identical, since the other poses did not change.)
