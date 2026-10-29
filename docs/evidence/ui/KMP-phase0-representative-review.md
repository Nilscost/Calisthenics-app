# KMP Phase 0: representative screenshot review

Reviewer: Sol coordinator. This is partial visual evidence, **not Phase 0 gate approval**.

Source: isolated CI [ui-screens 40635897471](https://github.com/Nilscost/Calisthenics-app/actions/runs/40635897471), commit `9e07c954`. Its connected tests passed; the overall run failed Preview in light and Session in all variants. Those failed flows are excluded below and require fresh corrected-flow evidence.

Opened 12 labelled contact sheets containing the following 45 screenshots (15 per light, dark and font 1.3). Also opened the font 1.3 routine-save dialog and dark pair-workout end editor individually at full size to check text, keyboard clearance, contrast and controls. The labelled sheets are saved under `docs/evidence/ui/kmp-phase0-representatives/{light,dark,font13}-{1,2,3,4}.jpg`; their exact artifact-relative source manifest is `sources.txt` in that directory. Original images remain under `/tmp/kmp-ui-ci-artifacts/ui-shots-<variant>-9e07c954be55a87d60c007f3ef1b2a66a9281132/work/_temp/shots/`.

Scores use 1–5; 3 is the minimum acceptable. A score of 4 means readable and usable with no blocking defect observed in this screenshot, rather than a claim of complete interaction or phone testing.

| Flow / screenshot | Light | Dark | Font 1.3 | Findings |
|---|---:|---:|---:|---|
| a_onboarding / 06-ready | 4 | 4 | 4 | Names, targets, stars and navigation readable; no overlap. |
| d_tabs / 01-progress | 4 | 4 | 4 | Search, route chips, nodes and bottom navigation readable. Horizontal chip strips continue beyond the viewport as expected. |
| e_seeded / 02-history-detail | 4 | 4 | 4 | Existing workout and per-round corrections visible; no overlapping controls. |
| f_pairs / 04-pairs-end-editor | 4 | 4 | 4 | Saved-history confirmation, mastery card, editing controls and Done readable. Inactive mastery dots remain distinguishable in dark mode. |
| g_routine / 01-save-dialog | 4 | 4 | 4 | Dialog text and actions fit above keyboard, including enlarged font. |
| h_ready / 04-train-minimalist | 4 | 4 | 4 | Controls reflow into separate rows at enlarged font; fixed Start/Preview/navigation remain clear. |
| i_stretch / 04-stretch-hold | 4 | 4 | 4 | Long title/instructions wrap and timer/actions fit. White clip canvas is intentional existing media presentation. |
| j_warmup / 05-dip-hold-logger | 4 | 4 | 4 | Screenshot shows rest with a prior Front Plank hold logger; numeric controls and actions fit. Filename is not proof that this image covers a dip. |
| k1_chart / 01-chart-1-sessions | 4 | 4 | 4 | Single point and summary legible; lower chart content can require scrolling at font 1.3. |
| k2_chart / 01-chart-2-sessions | 4 | 4 | 4 | Two-point line and values visible; no unintended overlap. |
| k7_chart / 01-chart-7-sessions | 4 | 4 | 4 | Seven-point progression and values remain readable. |
| l_r8_catalog / 01-power-progression | 4 | 4 | 4 | Jump route, equipment locks and stars visible; horizontal chips remain scrollable. |
| m_r8_programs_flow / 03-flow-session | 4 | 4 | 4 | Flow title, next exercise, timer and pause button readable. |
| n_r8_preview_rest_extras / 03-preview-recovery-adjusted | 4 | 4 | 4 | Adjusted rest, exercise targets, body map and fixed footer fit; top content is intentionally scrolled. |
| o_r8_progress_routes / 03-push-up-other-ways | 4 | 4 | 4 | Equipment requirement panel readable, including warning and exercise-detail action. Tree content continues behind its existing detail panel. |

No blocking visual defect was found in this representative sample. No application fixes were requested from these images. This review does not cover every screenshot, prove baseline equivalence across all screens, approve the failed flows, or replace the final owner phone checks.

## Latest-APK comparison (light mode)

Sol compared six matching Train/Preview captures from the owner-selected latest APK with the upgraded APK, both on the same emulator and with the same onboarding/selected settings. The paired images are saved under `docs/evidence/ui/kmp-phase0-baseline-comparison/`; the original APK is on the left and upgraded APK on the right. Sources are `/tmp/kmp-baseline-train/2026-10-28_200219/z_comparison_tmp/takeScreenshot/` and `/tmp/kmp-phase0-full/b_train_preview/`.

- `02-preview-light.png`: pixel-identical below the system status bar.
- `01-train-light.png`, `01a-between-flow-light.png`, `01b-train-exercises-light.png`, `01c-train-cooldown-light.png`: content, text and control positions match. The selected Train navigation indicator is slightly narrower in the upgraded Material 3 rendering (approximately 168 to 147 physical pixels); its icon, label and navigation cell remain in place. This is a library rendering difference, not an intentional design change.
- `01d-train-include-light.png`: same content/controls with the navigation indicator difference above and a different captured Conditioning ripple animation frame.

The system clock differs between captures. The cropped images make the navigation indicator and transient ripple differences explicit. No clipping, overlapping text, missing control or meaningful touch-layout drift was observed in these pairs. This is representative light-mode equivalence evidence only; full three-variant regression and final phone checks remain separate gates.
