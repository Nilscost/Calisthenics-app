# Screenshot review V12–V27 (one section per task)

Scores 1–5 (5 = matches doc 17 and reads well; 3 = usable with a visible flaw; below 3 = must be fixed). Variants: light / dark / font 1.3.
Evidence source: ui-screens run https://github.com/Nilscost/Calisthenics-app/actions/runs/38022677910 at commit `adcbd3f` (the pushed HEAD at V23 plus the h_ready flow fix). `results.txt` on the `ui-shots` branch, folder `adcbd3f/`: **24 PASS, 0 FAIL** (8 flows × 3 variants). Earlier runs: `7da03aa` failed in `e_seeded` (fixed in `4d27077`), `88d8707` failed in `h_ready` (fixed in `adcbd3f`).
Reviewed by opening every changed screen of the flows in the three variants (contact sheets of the three variants side by side) and comparing with doc 17. Nothing was seen on a phone.
Sections for V25, V26 and V27 are added after their own green runs (see the end of the file).

## V12 — questionnaire (a_onboarding)
| Screen | Light | Dark | Font 1.3 | Notes |
|---|---|---|---|---|
| Goal page (GOAL · EQUIPMENT · LEVEL header, Body part / Skill / Routine, dropdown) | 5 | 5 | 4 | header highlights the current part; the objective control is the same component as on Train |
| Level page "Pull and row" (full progression carousel, I know / I don't know) | 4 | 4 | 3 | the carousel peeks the next card by design; at font 1.3 the second card's text is cut at the right edge (it scrolls) |
| "You are ready" (stars below the chosen level, no dots) | 5 | 5 | 5 | D8 stars shown (2 stars for step 3) |
Defects: none below 3. Routine type in the questionnaire now lists the ready-made routines (V22).

## V13 — Progress vertical tree (d_tabs, e_seeded)
| Screen | Light | Dark | Font 1.3 | Notes |
|---|---|---|---|---|
| By type tree (tiles with level number, picture, stars; chains side by side only at branches) | 4 | 4 | 3 | tile text wraps to 3 lines and is cut at the bottom edge at font 1.3 ("Standard") |
| Node panel above the tab bar, OPEN EXERCISE DETAIL | 4 | 4 | 4 | |
| Exercise detail (clip on white, body figure, five levels, unlocks, cautions) | 4 | 4 | 4 | clip stays a white block in dark (doc 17 says so) |
| By skill chips + chain | 4 | 4 | 3 | |
Defects: (1) **3** the Push tab lists the equipment-locked dip chain (wrench / lock tiles) above the push-ups the person actually trains: a chain with a low difficulty rank is drawn first. (2) **3** the node panel says "Missing in this profile: dip-support" with a raw equipment id instead of the label. Both are fixed in V27b.

## V14 — automatic progression switch (d_tabs 05b)
Settings → Progression card with the switch and a two-line explanation. Light 5, dark 5, font 1.3 4. No defects.

## V15 / R3 — release R3 (combined with V12–V14 above)
Train (doc 17 §2.1), questionnaire, Progress, switch all reviewed above. The Train screen at font 1.3 scores 3 (see V16: "Stretc" cut, bottom-bar buttons clipped).

## V16 — Preview editor (b_train_preview)
| Screen | Light | Dark | Font 1.3 | Notes |
|---|---|---|---|---|
| Train (objective, equipment, format, sets/between sets, warm-up, tiles, bottom bar) | 5 | 5 | 3 | font 1.3: **"Stretc"** is cut in the BETWEEN SETS control, the TIME/EXERCISES tiles sit under the bottom bar and "Workout preview" is clipped to one and a half lines |
| Round preview (set tabs, cards with − / + level stepper, break rows with change and +) | 4 | 4 | 3 | the break row reads "Stretch · Chest Stretch (doorway / wall) · 1:00change": **no space before "change"**; at font 1.3 "Save as routine" in the bottom bar is clipped |
| Add sheet, Swap sheet (chain + Other types, Keep for next time) | 4 | 4 | 4 | |
Defects (all 3): "Stretc" cut; clipped bottom-bar buttons at font 1.3; missing space before "change". Fixed in V27b.

## V17 — Detailed edit (b_train_preview 02d)
SET 1 header, per-set cards, explanation line in capitals ("A typed number does not count towards your progress"). Light 4, dark 4, font 1.3 3 (same break-row spacing defect as V16). The typed-number dialog is not driven by a screenshot flow (text field, see progress notes).

## V18 — saved routines (g_routine)
Save dialog with a Name field (light 5, dark 5, font 1.3 4); Train with the Routine objective "Push day · 6 exercises · Circuit" (5 / 5 / 3, same Train font-1.3 flaws). No new defects.

## V20 — workout and History for sets (f_pairs, e_seeded)
| Screen | Light | Dark | Font 1.3 | Notes |
|---|---|---|---|---|
| Workout header "PAIR 1 · SET 1 OF 4", dark bottom sheet with timer, DONE | 4 | 4 | 4 | the clip is a small centred figure, not full-bleed (known, plan §5.1 not reached) |
| Logger / get-ready sheet | 4 | 4 | 4 | in the get-ready frame of a stretch the clip area is empty white: **3** (the stretch clip has not appeared yet at that moment) |
| End screen and end editor ("Set 1", Too hard / Too easy / Pain chips) | 4 | 4 | 4 | |
| History detail "Round n · Left side" corrections | 4 | 4 | 4 | |
Defects: empty clip area during get-ready of a stretch (3). Noted; fixing it needs a design decision (show the next exercise's clip during get-ready); logged in V27b notes.

## V21 — progression rule (b_train_preview 02e)
Rule control (App levels · Rep range · Custom), explanatory sentence, ranges "5–8 reps" on the cards. Light 5, dark 5, font 1.3 4.

## V21b — Recommended Routine content (clips and Progress)
Content task; its screens are the Progress Push tab with the dip chain (wrench / lock tiles, thumbnails readable at tile size: 4/4/4) and the clip frames in `docs/evidence/clips-v3-review.md`. Defects as in V13 (ordering, raw id).

## V22 — ready-made routines (h_ready)
| Screen | Light | Dark | Font 1.3 | Notes |
|---|---|---|---|---|
| Train with the Recommended Routine ("Ready-made · 9 exercises · Pairs", Rest selected, warm-up on, ≈ 73 min) | 5 | 5 | 3 | |
| Preview of the core triplet (Rest · 1:00 between, Pallof press "8–12 reps per side", reverse hyperextension) | 4 | 4 | 3 | |
| Train with the Minimalist Routine (Circuit, 4 exercises, ≈ 13 min) | 5 | 5 | 3 | |
Defects: Train font-1.3 flaws as above.

## V23 — warm-up switch
"Warm-up first" row with a one-line explanation and a switch on Train (visible in all Train shots): light 5, dark 5, font 1.3 4.

## Defects below 3
None. Defects at 3 are listed per task and fixed or logged in V27b.
