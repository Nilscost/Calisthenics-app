# V07 — stretch library: screenshot review

Run: https://github.com/Nilscost/Calisthenics-app/actions/runs/37940542581 (commit `62c070b`): 15/15 flow x variant PASS (the `e_seeded` flow now also shows the Stretches tab and a stretch sheet).

| Screen | Variant | Score | Defects |
|---|---|---|---|
| Progress > Stretches list, bottom (`e_seeded/09`) | light (opened) | 3 | all 18 stretches present and readable; every row has the same generic stretch icon (V09 uses thumbnails); the list is unsorted by area |
| Stretch sheet "Thoracic Rotation" (`10`) | dark (opened) | 4 | clip, how-to, cues, own caution plus the general one, no equipment; no muscle chips yet on stretches (V08 adds the body map to the sheet) |
| same sheet | light, font13 | not opened one by one | passes the flow; text wraps in the same layout as the opened dark shot |

Open: generic icons (V09); clip frames are white in dark mode (V10).
