# Release R2 — full screenshot review (V06-V11)

Run: https://github.com/Nilscost/Calisthenics-app/actions/runs/37976176269 (commit `55123b3`, version 0.5.0-r2): 15/15 flow x variant PASS, `connected` job green; CI `verify` green (run 37976176217). Checked against `docs/17-ui-direction-b.md`.

Opened in this run: c_session 04 (font13), 05 (light). Opened in the previous run (`b541951`, same screens before the colour fix): c_session 02 and 04 (dark), 04 (font13), b_train_preview 02 (dark). Everything else passes its flow and shares the same theme; not every shot was opened.

| Screen | Variant | Score | Defects |
|---|---|---|---|
| Workout, work block (`c_session/02`, `05`) | dark, light | 4 | matches doc 17 §2.3: big ROUND line, set markers, elapsed time, clip on white, dark sheet with gold timer and bar, exercise and set, Pause · DONE · Skip with DONE wide in the middle; the "Next:" line repeats the name for the second side |
| Workout, stretch block with logger (`04`) | dark, font13 | 4 | - number + readable at font 1.3 (fixed in this release: in the Light theme the number was dark on the dark sheet); a long stretch title wraps to two lines and the clip gets smaller |
| Preview (`b_train_preview/02`) | dark | 4 | thumbnail, name, target, and the front/back body figure per card (V09); figures are small (24 dp per view) but readable; top bar band still the old style (V16) |
| Progress sheet with body figure (`e_seeded/07`) | dark | 3 | figure small beside the chips (see V08-review); clip frame is white in the dark sheet |
| Train, Progress, onboarding | dark, light, font13 | 4 | new palette and gold tab; layouts are still the pre-doc-17 ones (V12 and V13) |

Open: clip frame is a white rectangle on the dark sheets (doc 16/17: clips are on white by design; the owner is asked in `owner-check-r2.md`); decoded clip white is 253, not 255 (a faint edge); fonts are the system fallbacks until the font download is approved by the owner.
