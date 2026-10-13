# Release R7 screenshot review (0.10.0-r7) — preliminary

**Status: pending final review.** The newest complete screenshot set is not available yet. This preliminary visual spot-check uses the partial `ui-screens` run for `1f9dc5b` ([run 38400024043](https://github.com/Nilscost/Calisthenics-app/actions/runs/38400024043)); that run captured light shots for all ten flows and dark shots for only part of the run, but `b_train_preview` and `j_warmup` failed. The corrected flows were pushed in `ab77d74`; that run was canceled by the final tooling push. The latest run is for `89ade24` ([run 38406697056](https://github.com/Nilscost/Calisthenics-app/actions/runs/38406697056)) and is still in its emulator-flow stage. App and Maestro flow code are unchanged between `1f9dc5b` and `89ade24`; intervening commits only change tooling/evidence and the two flow fixes.

The scores below apply only to the named screenshots I could inspect. A dash means that variant or screen is still pending. These are provisional and do not establish that the flows pass.

## Preliminary spot-check

| Flow and inspected screens | Light | Dark | Font 1.3 | Notes |
|---|---:|---:|---:|---|
| a_onboarding: welcome | 5 | — | — | Clear type and action placement. Remaining questionnaire screens still need review. |
| b_train_preview: Train, Preview, Push category sheet | 4 | 4 | — | Content and contrast are clear. The old `01c` shot missed the cool-down controls; the flow now centers the switch and Stretching mode before capture. The old run failed to find the Preview cool-down caption. Verify the corrected run and inspect its font 1.3 screens. |
| c_session: rep work/logger, stretch caption | 4 | 4 | — | Work controls are legible; stretch caption includes the stretch name. Font 1.3 and the remaining screens are pending. |
| d_tabs: Progress tree, node panel, exercise detail, History | 4 | 4 | — | Links, stars, node states and History rows are readable. Some long names wrap; inspect font 1.3 before scoring. |
| e_seeded: History, chart, Progress, planche note, manual edit, stretch detail | 4 | — | — | Chart labels and exercise details are legible in the light shots reviewed. More seeded screens and variants are pending. |
| f_pairs: work logger | 4 | — | — | Logger controls are clear in the light shot. |
| g_routine: save routine dialog | 4 | — | — | Dialog actions remain visible above the keyboard. |
| h_ready: Recommended Routine Preview | 4 | — | — | Preview text and cards are readable. |
| i_stretch: stretch instruction and note | 4 | — | — | Instructions fit; caution text continues below the fold and is scrollable. |
| j_warmup: Train, raise and activate blocks | 4 | — | — | Warm-up headings and notes are legible. The old flow matched the first `30 s` hold (Front Plank) and then ended the workout early; it now waits for Dip Support Hold by name and confirms the `30 s` target. The hold, logger and editor screenshots are pending. |

## Pending before sign-off

- Wait for the current `ui-screens` run on `89ade24`; require PASS for every flow and variant.
- Open every changed screen in light, dark and font 1.3, including the corrected Train cool-down view, Preview cool-down, dip support hold, hold logger and end editor. Open at least one light screenshot from every other flow and inspect the remaining relevant light screens against the R7 plan.
- Replace this preliminary table with final scores and record any defects fixed or left open. No phone behavior is verified by screenshots.
