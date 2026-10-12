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

## Owner test: Seedance text-only, real person (2026-10-12)
`pullup-full-seedance-human.mp4`: no start image; the prompt asks for a real athlete in plain sports clothes on a white background. Cost $0.348, 151 s. Agent review: photoreal and convincing; overhand grip, hands stay on the bar, a full rep from dead hang to the chin at the bar and back, fixed camera, no visible anatomy errors at this size. Defects: background light grey rather than white, the figure is small in the frame, the feet hang close to the floor (low bar). Loop: starts and ends in a dead hang, but not frame-identical (no first/last frame was sent). Score 4. Without a start image, every exercise would come out with a different person, bar and framing unless the prompt pins them down; consistency across ~140 clips is the open risk.

## Owner test: photoreal start images + video model (2026-10-12)
Pipeline: Blender start pose → image model `google/gemini-3.1-flash-image` makes a photoreal man in the same pose on white (≈ $0.07 per image; the pistol image got the pull-up image as a reference, and the same person came out) → video model with that image as first and last frame.
- Pull-up, Seedance 2.0 (`pullup-full-seedance-photostart.mp4`, $0.35): full rep from dead hang to chin over the bar and back, hands fixed, same person throughout, white background, loops. Score 4–5; the best clip of the bake-off.
- Pistol squat, Seedance 2.0: **refused twice** before generating (`InputImageSensitiveContentDetected.PrivacyInformation`: "may contain real person"; not charged). The pull-up image passed, so the filter is inconsistent: a photoreal route on Seedance cannot be relied on for every exercise.
- Pistol squat, Kling v3 std (`squat-pistol-kling-photostart.mp4`, $0.42): accepted the photo, but the man only bends the standing knee a little (quarter squat) with the leg lifted. Not a pistol. Score 2.
- Consequence: photoreal start images give the nicest clips where the video model gets the movement right, but (a) Seedance's real-person filter blocks some inputs at random, (b) hard moves still fail. Possible mitigations, untested: a slightly stylised person (e.g. "3D-rendered athlete") to pass the filter; several retries; Blender for the moves AI cannot do.
