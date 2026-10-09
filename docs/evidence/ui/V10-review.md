# V10 — full-screen workout video: screenshot review
Runs `b541951` and `55123b3` (see R2-review), 15/15 PASS each. Opened: c_session 02, 04 (dark), 04 (font13), 05 (light).
| Screen | Variant | Score | Defects |
|---|---|---|---|
| Work block | dark, light | 4 | doc 17 §2.3 layout; clip is 4:3, centred on white (not full-bleed) |
| Stretch block with logger | dark | 4 | - 9 + readable; DONE confirms, Pause and Skip outlined |
| Stretch block with logger | font13 | 2 then 4 | in the first run (`b541951`) the number between - and + was missing in the Light theme (dark text on the dark sheet); fixed by setting the content colour of the header and the sheet; verified in `55123b3` |
Open: very long titles wrap; clip white edge (253).
