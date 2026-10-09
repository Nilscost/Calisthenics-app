# V00b — debug seed: screenshot review

Run: https://github.com/Nilscost/Calisthenics-app/actions/runs/37937080629 (commit `02146c7`), `results.txt` all 15 flow x variant PASS (5 flows x light/dark/font13); the `connected` job (Room tests on the emulator) is green too. Shots on branch `ui-shots` under `02146c7/`.

First runs of `e_seeded` failed on flow steps, not on the seed (hook output: `result=1`): the tab chips scroll sideways and the bottom sheet is its own window (runs 37923076641, 37925099345). Fixed in the flow (swipe until visible, find the sheet by text); then all 3 variants PASS.

| Screen | Variant | Score | Defects |
|---|---|---|---|
| History list with 2 workouts this week (`e_seeded/01`) | light, dark, font13 (opened light) | 4 | summary reads "2 workouts"; with one workout it says "1 workouts" (see d_tabs, open) |
| History detail with data (`02`, `03`) | light, dark, font13 (opened dark, font13) | 4 | long page, scrolls; editors are V04b |
| Progress push / squat / hips with stars (`04`-`06`) | light, dark, font13 (opened light, font13) | 4 | stars and "Training now" appear as seeded; nodes cut at the right edge are scrollable |
| Kettlebell deadlift sheet (`07`) | light, dark (opened dark) | 4 | shows "Kettlebell: 12 kg" line; no weight track yet (V26) |

Open: none from V00b itself. The seeded History uses the current week because the receiver rebases the fixture times.
