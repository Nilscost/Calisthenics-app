# Release R1 — full screenshot review

Run: https://github.com/Nilscost/Calisthenics-app/actions/runs/37937080629 (commit `02146c7`), `results.txt` all 15 flow x variant PASS (5 flows x light/dark/font13); the `connected` job (Room tests on the emulator) is green too. Shots on branch `ui-shots` under `02146c7/`.

Version 0.4.0-r1 (versionCode 4). Tasks in this release: V00b, V01, V02 (domain), V03, V04a (Room 24, CI `connected` job), V04b.

Opened in this review (run 02146c7): c_session 04 (light, dark, font13), 05 (font13), a_onboarding 06-ready (light), d_tabs 01-progress (light), d_tabs 03-history (dark). Opened in the previous full run 1bceef1: c_session 04 (light, font13), 09 (light, font13), e_seeded 02 (font13), 03 (dark), 07 (dark), b_train_preview 02 (light). The remaining shots were not opened one by one; every flow passes and the screens they show were not changed in R1 (onboarding, Train, Preview, Settings).

| Screen | Variant | Score | Defects |
|---|---|---|---|
| Onboarding "You are ready" | light | 3 | step dots still shown and stars all empty (R7, V12) |
| Progress push tab | light | 4 | tree is horizontal (R23, V13) |
| History week | dark | 4 | "1 workouts" (pluralisation, open, will be fixed with the History text pass) |
| Session logger / stretch / next block | light, dark, font13 | 4 / 4 / 3 | see V01, V03 |
| End editor, History detail | light, dark, font13 | 4 | see V04b |
| Preview (b_train_preview 02) | light | 3 | letter circles instead of icons (R9, V16) |

Open defects: stretch clip framing (V06b/V10), empty band on the get-ready block (V10), clip frames are white in dark mode (V10), status-bar clock sometimes touches the left edge in light/dark emulator shots (emulator clock font, not the app).
