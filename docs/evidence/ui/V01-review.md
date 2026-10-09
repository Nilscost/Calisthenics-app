# V01 — clip reloads per block: screenshot review

Run: https://github.com/Nilscost/Calisthenics-app/actions/runs/37937080629 (commit `02146c7`), `results.txt` all 15 flow x variant PASS (5 flows x light/dark/font13); the `connected` job (Room tests on the emulator) is green too. Shots on branch `ui-shots` under `02146c7/`.

K1 is visible in `c_session/04-stretch-block` (Chest Stretch clip) and `05-next-work-block` (Split Squat clip): the clip changes with the block in all three variants.

| Screen | Variant | Score | Defects |
|---|---|---|---|
| Stretch block with the logger (`c_session/04`) | light 4, dark 4, font13 3 | | font13: the clip is small (about 140 dp) because the long stretch title takes two lines; still readable |
| Next work block (`05`) | light, dark, font13 (opened light, font13) | 4 | large empty band above and below the clip (centred in the free space); fine |
| Status bar (all) | light, dark | 4 | **V00 defect fixed**: the bar now follows the theme (dark in dark mode) |

Defects found by looking and fixed in this task's follow-ups: (1) run 577e644: the V03 logger plus the long stretch title squeezed the clip to zero height, so the clip was drawn over the logger's Done button and the title (score 2); fix: the clip now fits the free space (`DemoPlayer(fit = true)`) and the ring is smaller while the logger shows; verified in run 02146c7. (2) The V00 status bar defect (fixed, see above).
Still open from the V00 baseline: the stretch clip is cut off at the bottom of the video itself (clip framing, V06b/V10); the get-ready block has an empty band under the logger (V10: full-screen video).
The clip clip is a white frame in dark mode (clips have a white background); V10 puts the video behind a scrim.
