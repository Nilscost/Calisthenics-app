# R8 light-theme visual review (final)

**Run reviewed:** GitHub `ui-screens` run 38973320244 on commit `95fa4fc` (R8-T14, all 25 repaired demonstrations installed, zero demonstration holds). Result: 17 flows PASS in this theme (light had one RETRY on `b_train_preview` that passed on the retry), 51 of 51 flow results PASS over the three themes, connected job 9 tests, 0 failures. This replaces the earlier partial review. Nothing was tested on a phone; this is not owner acceptance.

I opened contact sheets of all 111 screenshots of this theme (flows a to o, all 17 flows, 15 images per sheet) and the larger previews where text was small. Scores run from 1 (needs correction) to 5 (clear and complete) and assess the captured screen at phone size, not accessibility or interaction.

## Changed screens in this release

| Screen (flow) | Score | Finding |
| --- | ---: | --- |
| Session work/stretch/warm-up blocks with the repaired clips (c_session, i_stretch, j_warmup, f_pairs) | 4 | The mannequin is visible and centred in every block; the clip panel is white on the app canvas, a deliberate media panel. Timer, DONE, Skip and Pause stay distinct. |
| Stretch sheet in Progress, Thoracic Rotation (e_seeded 10) | 4 | The repaired clip shows the quadruped rotation with the written steps below it. |
| Exercise detail with clip (d_tabs 02b, e_seeded 07b/07g, l_r8_catalog 02 V-up) | 4 | Clip, levels, unlocks and cautions are readable; no held-demonstration text remains. |
| Fixed programs under Routine and program flow (m_r8_programs_flow) | 4 | Compact summaries, expandable details, flow switch and flow session read clearly. |
| Preview rest edit and manual power add (n_r8_preview_rest_extras) | 4 | Rest stepper beside +, totals update, specialist exercises listed with thumbnails. |
| Progress routes and other ways (o_r8_progress_routes) | 4 | Main path, locked alternatives, needs and missing-equipment lines are legible. |
| Train, Preview, sheets, History, Settings, onboarding | 4 | Unchanged layout; no overlaps or clipped controls. |

## Remaining low-severity items (not release blockers)

- `d_tabs/04-history-detail` and the end screens show "1 rounds" for a one-round workout. Copy polish; not changed in R8.
- Long lists continue below the viewport; the sheets do not show every bottom row. Scrolling is covered by the flows.
