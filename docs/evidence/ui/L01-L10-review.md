# Screenshot review L01–L10 (one section per task)

Scores 1–5 (below 3 must be fixed). Variants: light / dark / font 1.3. Evidence: ui-screens run https://github.com/Nilscost/Calisthenics-app/actions/runs/38042340665 at commit `78d0fca` (the pushed HEAD that contains L01–L10): `results.txt` on the `ui-shots` branch, folder `78d0fca/`: **24 PASS, 0 FAIL**. (Runs `98570d7` failed in `e_seeded` because the next suggestion card pushed the tab row down, and `h_ready` once at font 1.3; both flows were fixed and rerun.) Compared with doc 17.
These are content tasks: their screens are the Progress tabs (new nodes and chains), the exercise detail and the questionnaire. Clip frames and their verdicts: `docs/evidence/clips-v3-review.md`.

## L01 — tier profiles: no visible change
Numbers behind the Pallof, reverse hyperextension, dip support hold (the cards read "8–12 reps", "10–30 s" in the Preview, see `h_ready`). Light 5, dark 5, font 1.3 4.

## L02 — push-up chain (e_seeded 04 and the Push tab)
Wall → high incline → incline ‖ knee → standard → diamond ‖ decline → archer is drawn as a branching tree; the locked dip chain now sits below the push-ups (V27b). Light 4, dark 4, font 1.3 3 (names wrap to three lines). Defects: none below 3.

## L03 — pull-up path (e_seeded 04b, a_onboarding family 3)
Pull tab: dead hang → scapular pull → arch hang → three options (chair-assisted, negative, flexed-arm hang) → pull-up, plus the band-assisted pull-up beside them. Light 3, dark 3, font 1.3 3: the connector lines cross and bend because the chains join from several sides, so the picture is busier than the others (readable, not wrong). Not fixed: the layout algorithm draws every prerequisite edge; a later pass could draw only the first-successor edges. The questionnaire pull page shows the longer ladder (band row, assisted pull-up, flexed-arm hang, pull-up) as a scrolling carousel: 4/4/3.

## L04 — rows (Row tab, not opened by a flow)
**Defect (process, 3):** no flow opens the Row tab, so the towel row, the wide row and the kettlebell row nodes were not seen in a screenshot; they use the same tile component as every other node, and the Row tab fits the width limit tested by `SkillTreeTest`. A Row-tab step is added to `e_seeded` in R5's follow-up (L11).

## L05 — single-leg squats (e_seeded 05)
Squat tab: split squat → Bulgarian → shrimp ‖ assisted pistol, with the further steps below the fold. Light 4, dark 4, font 1.3 3.

## L06 — hinge paths (e_seeded 06)
Hips tab with the slide, superman, arch and kettlebell chains. Light 4, dark 4, font 1.3 3. Text-only changes (Nordic cautions) are in the exercise detail, which uses the same layout as before.

## L07 — L-sit steps / L08 — core planes (e_seeded 07d, Core tab)
Dead bug → plank → plank shoulder taps ‖ tucked hollow → one-leg hollow → hollow → …; the chain reads top to bottom with the tile format of doc 17. Light 4, dark 4, font 1.3 3.

## L09 — back-bridge ladder (e_seeded 05/06, Hips tab)
Glute bridge → table bridge ‖ single-leg bridge, then the chair bridge (locked) below. Light 4, dark 4, font 1.3 3.

## L10 — dips (text only)
The dip texts appear in the exercise detail; the detail layout was reviewed in V12–V27 (4/4/4).

## Also seen in this run (V26, V27, V27b, V25)
- Kettlebell detail with the **Weight section** (12 kg NOW, 16 kg "NEEDS A 16 KG KETTLEBELL"): light 4, dark 4, font 1.3 4.
- **With weights** tab (dumbbell, goblet, weighted bridge, vest squat; wrench = equipment missing): 4/4/3.
- **History** with the three tiles, progressions list and the lowest-set chart with the dashed gate line: light 5, dark 5, font 1.3 4 (tile captions wrap to three lines).
- Suggestion card above By type / By skill: 5/5/4; the next card appears after a dismissal (a design choice: at most one at a time).

## Defects below 3
None. Defects at 3: the busy connector lines on the Pull tab (L03), long wrapped names at font 1.3, the missing Row tab shot (L04).
