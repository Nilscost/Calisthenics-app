# Release R7.1 screenshot review (0.10.1-r7, versionCode 11) — final

Source: `ui-screens` run [38442012588](https://github.com/Nilscost/Calisthenics-app/actions/runs/38442012588) on `fc183ad`: **all 10 flows PASS in all 3 variants (30 of 30), no retries**; the `connected` job also passed. Shots: branch `ui-shots`, folder `fc183ad/`. The run is one job per variant (light, dark, font 1.3). I opened every shot of the changed flows (b, c, d, e, j) in light, dark and font 1.3 as contact sheets of the three variants side by side, and every light shot of a, f, g, h and i. Scores: 1 to 5, nothing below 4 was found. Screenshots do not verify phone behaviour; nothing was tested on a phone.

| Flow and screens | Light | Dark | Font 1.3 | Notes |
|---|---:|---:|---:|---|
| a_onboarding: welcome, goal, equipment, level pages | 4 | – | – | Clear; the unselected "Next" is visibly disabled. |
| b_train_preview: Train, exercises per round, cool-down switch with Stretching/Timed, Preview, level stepper, add sheet, rule picker, cool-down list, timeline, swap sheet | 4 | 4 | 4 | Cool-down switch and mode are visible. Labels wrap at 1.3 but nothing is cut off. The timeline is long at 1.3 (hence the longer flow scroll). |
| c_session: work block, rep logger, stretch block and caption, paused, end confirm, end screen, end editor | 4 | 4 | 4 | The new Blender clips show (push-up, doorway stretch). Captions "Next: ..." wrap at 1.3 without overlap. |
| d_tabs: Progress tree, node panel, exercise detail, planche note, edit levels, by skill, History, History detail, Settings | 4 | 4 | 4 | Needs/Unlocks links and stars readable. Settings footer shows 0.10.1-r7. |
| e_seeded: History list, Progressions chart, detail, Progress tabs (push, pull, squat, hips, core, lower legs, weighted), kettlebell weights, stretch list and sheet | 4 | 4 | 4 | Chart numbers legible. Tab strip scrolls horizontally at 1.3 and cuts the last tab at the edge, which is expected for a scrolling row. Thumbnails of the new clips are small but recognisable. |
| f_pairs: work logger | 4 | – | – | |
| g_routine: save routine dialog | 4 | – | – | |
| h_ready: Recommended Routine Preview | 4 | – | – | |
| i_stretch: instruction and note | 4 | – | – | |
| j_warmup: Train (warm-up on), raise and activate blocks, first hold block, hold logger, end editor | 4 | 4 | 4 | Warm-up headers and notes legible. |

## Defects fixed in this review round (flows, not the app)
- `j_warmup` waited for "Dip Support Hold", but the Recommended Routine of the seeded start no longer contains it (the first pair shows the pull-up and split squat, the second the wall handstand push-up and the hinge); the workout then ended after all the skips. The flow now skips to the first hold block (any `session_target` like `30 s`). The warm-up switch is scrolled to (it is below the fold at font 1.3).
- `b_train_preview`: the cool-down switch tap is idempotent (a retry started with the switch already on and turned it off), and the scroll to the set-2 badge got a longer timeout for the long timeline at 1.3.
- CI: one job per variant (the single job ran past 60 minutes); the `ui-shots` publisher now merges parallel jobs with a lease instead of overwriting.

## Open
- Nothing below 4. Phone behaviour, sound, haptics, and the technique of every clip stay owner checks; all training content is DRAFT.
