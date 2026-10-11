# Release R6 screenshot review (0.9.0-r6)

Evidence: ui-screens run https://github.com/Nilscost/Calisthenics-app/actions/runs/38080776533 at commit `9b28604`; `results.txt` on the `ui-shots` branch, folder `9b28604/`: **30 PASS, 0 FAIL** (10 flows a to j, each in light, dark and font 1.3); verify green; full local verify exit 0 (`docs/evidence/r6-verify-2026-10-10.log`). Every flow's screens were opened as three-variant contact sheets (81 steps) and compared with doc 17; the per-task reviews (`V12-V27-review.md`, `L01-L10-review.md`, `L11-review.md`, `L11b-review.md`, `L12-review.md`, `L13-review.md`, `R5-review.md`) hold the detail. Type: Barlow Condensed for titles, timers and numbers, IBM Plex Sans for text, in all variants.

## Screens (scores light / dark / font 1.3)
| Flow and screens | Scores | Defects |
|---|---|---|
| a_onboarding: welcome, goal (Body part / Skill / Routine), equipment, six level pages (long ladders incl. flexed-arm hang), style, ready with stars | 5 / 5 / 4 | none open; the carousel cards are cut at the edge by design |
| b_train_preview: Train, round preview, set tab, add and swap sheets, Detailed edit, Rep range rule, bottom, timeline | 5 / 5 / 4 (preview 4 / 4 / 3) | open (3): the break row and "Save as routine" are tight at font 1.3 (fixed in V27b to growing heights; still two lines) |
| c_session: first block, work, logger, stretch, next work, paused, end confirm, end screen, end editor | 4 / 4 / 4 | open (3): the clip is a centred 4:3 figure, not full-bleed; the get-ready frame of a stretch shows no clip yet |
| d_tabs: Progress, node panel, exercise detail, By skill, History, History detail, Settings, progression | 4 / 4 / 3 to 4 | open (3): long node names wrap to three lines at font 1.3 |
| e_seeded (17 steps): History tiles, Progressions list and chart, detail, Push, Pull, Row, Squat, Hips, Core, With weights, Lower legs, kettlebell panel, detail and Weight section, Stretches list, stretch sheet | 4 / 4 / 3 to 4 | open (3): busy connector lines on the Pull tab; weak thumbnails in Lower legs |
| f_pairs: Train in Pairs, PAIR 1 header, logger, end editor with "Set n" | 4 / 4 / 4 | none |
| g_routine: Save as routine dialog, saved note, Routine objective | 5 / 5 / 4 | none |
| h_ready: Recommended and Minimalist routines (preview of the core triplet) | 5 / 5 / 4 | none |
| i_stretch: Starting To Stretch (Train, preview list, bump and hold blocks with notes) | 4 / 4 / 4 | none |
| j_warmup: warm-up blocks "raise" and "activate" in the Recommended Routine | 4 / 4 / 4 | none |

## Defects below 3
None. **Fixed during the releases:** raw equipment ids in panels, dip chain drawn first, "Stretch" cut and clipped bottom-bar buttons at font 1.3, the break-row spacing, History without tiles and chart (V27b), the missing flow shots for Pull, Row, Core, With weights, Lower legs, weights and warm-up. **Open (at 3):** the items in the table above and the weak clips listed in `clips-v3-review.md`.
