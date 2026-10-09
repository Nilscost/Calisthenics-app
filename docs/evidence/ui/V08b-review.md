# V08b — Design system B: screenshot review (every flow)

Run: https://github.com/Nilscost/Calisthenics-app/actions/runs/37970698419 (commit `55d0cda`): 15/15 flow x variant PASS (the runner now sets the app theme per variant: light and font13 = Light, dark = Dark). Checked against `docs/17-ui-direction-b.md` §1.

Opened: b_train_preview 01 (light, dark), d_tabs 01 (dark), a_onboarding 03 (font13), plus the earlier session shots. The other shots pass their flows and use the same theme; they were not opened one by one.

| Screen | Variant | Score | Defects |
|---|---|---|---|
| Train | light | 4 | gold primary button and selected segments, surface cards, gold tab; layout is still the old one (V12) |
| Train | dark | 4 | dark default palette matches the tokens (background #111416, cards #1B2024) |
| Progress tree | dark | 4 | gold current-node ring and "Training now" caption; stars still grey until earned; tree still horizontal (V13) |
| Questionnaire equipment page | font13 | 4 | gold checkboxes and Next; headings use the (fallback) condensed bold font; dots are gold for the current page |
| Bottom tabs | all | 4 | active tab in gold, inactive in the muted token |

Open: Barlow Condensed / IBM Plex Sans are not bundled (system condensed and sans fonts are used) - font download needs approval; top bars still show a surface-coloured band under the status bar (to be removed when each screen is restyled).
