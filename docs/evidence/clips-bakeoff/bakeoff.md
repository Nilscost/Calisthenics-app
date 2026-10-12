# Clip bake-off: Blender vs Seedance 2.0 vs Kling v3 (2026-10-12)

Plan: `docs/16-video-clips-plan.md`. Comparison videos (2x2, 10 s loop): `<id>-compare.mp4` in this folder. Raw files and frames: `work/clips-bakeoff/` (not in git). Attempt log: `manifest.csv`.

Same start frame for all routes: the Blender start pose (rendered on the owner's Mac, Blender 5.2.2 LTS, Cycles), sent to the AI models as first AND last frame. One attempt per model and exercise, no retries yet. Scores are the agent's review of 12-frame contact sheets (1 = unusable, 5 = ship as is); the owner decides.

| Exercise | Blender | Seedance 2.0 | Kling v3 std |
|---|---|---|---|
| Pull-up | 4: clean hang to chin at the bar, hands fixed | 4: full rep, chin to bar, hands fixed | 3: shorter range, arms stay bent at the top |
| Feet-elevated push-up | 4: feet stay on the chair, body straight, chest low; elbows a little wide | 4: real rep, feet stay on the chair | **1**: legs leave the chair and kick around |
| Pistol squat | 3: full depth, standing heel down; the free foot hangs low at the top | 3: free leg forward, but only about parallel depth | 2: shallow, both feet near the floor (reads as a squat) |
| Pigeon stretch | 3: reads as pigeon from a high camera; long arms when folded | 4: clear fold over the front leg, legs stay put | — refused by Kling's content filter (not charged) |

Size with the shipping settings (480 wide, CRF 30): Blender 11–22 KB, Seedance 16–25 KB, Kling 14–25 KB, today's clips 19–31 KB. All far below the 120 KB clip cap.

Cost and time:
- Seedance 2.0: **$0.348 per 5 s clip** (measured; output 752x560), 2–4 min per clip. Kling v3 std: **$0.42**, 1–2 min. Failed jobs were not charged. Spend on the bake-off key: **$2.65** (4 Seedance + 3 Kling).
- Blender: $0. Rendering ~3–5 min per exercise on the Mac (Cycles, GPU). Agent time: writing the mannequin, the IK pose code and the four exercises took several hours of iteration in the first session; a further exercise is mostly keying 2–4 poses and checking previews (estimate, not measured per clip).
- Full library (~140 clips, about 2 tries each): Seedance ≈ $100, Kling ≈ $120 (plus rejections), Blender $0 plus agent time.

Not done yet: retries; the rights check of Seedance/Kling output terms (plan step 3.5); the optional test APK.

Observations:
- Both AI models kept the Blender start frame's look (same mannequin, chair, bar), so an AI route still needs the Blender (or another) start pose per exercise.
- Seedance followed the motion text best; Kling broke the anatomy on the hardest contact case (feet on the chair) and refused the floor stretch.
- Blender is exact where the pose code is right and wrong where it is wrong (pistol free foot); fixes are a number edit and a re-render.
