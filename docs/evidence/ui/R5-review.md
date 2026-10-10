# Release R5 screenshot review (0.8.0-r5)

Evidence: ui-screens run https://github.com/Nilscost/Calisthenics-app/actions/runs/38058180879 (commit `4072272`, the R5 commit `f31fb2d` plus the bundled fonts `0629f5f` and the CI fixes): `results.txt` on the `ui-shots` branch, folder `4072272/`: **24 PASS, 0 FAIL** (8 flows × light / dark / font 1.3). Earlier R5 attempts: `f31fb2d` failed `h_ready` at font 1.3 (a scroll-until-visible wait), `0629f5f` failed the `verify` preflight (files over 400 KB: the catalog and the font); both fixed in `4072272`.
Per-task reviews with scores and defects: `V12-V27-review.md` (V12–V27), `L01-L10-review.md` (L01–L10). This file covers the release as a whole and the **new type** (Barlow Condensed and IBM Plex Sans, bundled in V08c).

## The new type (all screens of the run)
| Check | Result |
|---|---|
| Screen titles (TODAY'S WORKOUT, PROGRESS, HISTORY, ROUND PREVIEW), the big timer, ROUND 1 OF 4, the 8 reps / 0:55 digits, tile numbers | drawn in Barlow Condensed (narrow, heavy), also in dark and at font 1.3 |
| Body text, captions, dropdowns, chips, settings rows, dialogs | IBM Plex Sans; readable in all three variants |
| Clipping at font 1.3 | none in the titles and tiles; the Train screen scrolls (the warm-up row sits behind the bottom bar until scrolled); the Between sets control no longer cuts "Stretch" (the cards stack); History tile captions wrap to three lines |
Defects below 3: none.

## Screens (scores light / dark / font 1.3)
| Screen | Scores | Notes |
|---|---|---|
| Questionnaire (goal, equipment, level pages, ready) | 5 / 5 / 4 | |
| Train (objective, equipment, format, sets, warm-up, tiles, bar) | 5 / 5 / 4 | stacked cards at 1.3 |
| Round preview, Detailed edit, rule, add and swap sheets | 4 / 4 / 3 | break row spacing fixed; bottom bar buttons grow |
| Workout screen (header, clip, timer, DONE), end screen and editor | 4 / 4 / 4 | clip not full-bleed; get-ready of a stretch has an empty clip area (3, open) |
| Progress (suggestion card, By type, By skill, node panel, detail with Weight section, tabs Push, Pull, Row, Squat, Hips, Core, With weights, Stretches) | 4 / 4 / 3 | the Pull tab's connector lines are busy (3) |
| History (tiles, week strip, list, Progressions, chart) | 5 / 5 / 4 | |
| Settings (progression switch), Save as routine dialog, ready-made routines | 5 / 5 / 4 | |

## Open at R5 (not fixed)
The clip is a 4:3 figure centred on white, not full-bleed; the get-ready frame of a stretch shows no clip; the Pairs preview shows set tabs; the Pull tab connector lines. None blocks the owner's phone test.
