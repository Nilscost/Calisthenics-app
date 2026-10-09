# V09 — thumbnails and body figure in the lists: screenshot review
Run `b541951` (https://github.com/Nilscost/Calisthenics-app/actions/runs/37973328560), 15/15 PASS. Opened: `b_train_preview/02-preview-dark`.
| Screen | Variant | Score | Defects |
|---|---|---|---|
| Preview cards | dark (opened) | 4 | thumbnails (56 dp) and two small body figures per card replace the letter circle and the muscle names; names remain in the figure's description |
| Swap sheet, questionnaire cards, History detail, stretch list | light, dark, font13 | not opened one by one | tested by `ThumbTest`/`PreviewScreenTest`; the thumbnails come from the same files |
Open: no thumbnails in the Progress tree tiles until V13 (done there); stretch list thumbnails are 44 dp.
