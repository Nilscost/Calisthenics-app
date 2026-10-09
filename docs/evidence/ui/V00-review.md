# V00 — UI screenshot pipeline: acceptance evidence (Opus reviewer, 2026-10-09)

## What was built
- `.github/workflows/ui-screens.yml`:
  - builds the debug APK;
  - starts an API 34 x86_64 emulator (`pixel_6` profile, set to 1080×2400 / 420 dpi);
  - runs `scripts/ui_shots.sh` (every `maestro/flows/*.yaml` × light / dark / font 1.3);
  - uploads an Actions artifact;
  - publishes to the orphan branch `ui-shots` (`scripts/publish_ui_shots.sh`: 720 px copies, newest 5 runs, one rewritten commit).
- Flows: `a_onboarding` (fresh install, every questionnaire page), `b_train_preview` (Train, Preview, timeline, swap sheet), `c_session` (first block, Done, logger, pause, end with confirm, end screen), `d_tabs` (Progress tree + node sheet, History + detail, Settings).
- Maestro rather than instrumented tests: see `maestro/README.md`.
- App change: `MainActivity` exposes Compose test tags as resource ids.

## Runs
| Run | Commit | Result |
|---|---|---|
| https://github.com/Nilscost/Calisthenics-app/actions/runs/37913487822 | 0dc89b4 | all FAIL: Maestro refuses absolute screenshot paths → fixed (relative names, collected per flow) |
| https://github.com/Nilscost/Calisthenics-app/actions/runs/37914511147 | c1b97c9 | A, B pass; C fails (end-confirm dialog is its own window → select by text); D fails as a consequence (no workout in History) |
| https://github.com/Nilscost/Calisthenics-app/actions/runs/37916455847 | 72320f2 | all FAIL: emulator "Pixel Launcher isn't responding" dialog over the app → boot wait, `hide_error_dialogs`, dismiss sub-flow |
| https://github.com/Nilscost/Calisthenics-app/actions/runs/37917550425 | ebb4dc7 | **12/12 PASS** (4 flows × 3 variants), 102 screenshots |
| https://github.com/Nilscost/Calisthenics-app/actions/runs/37917561183 | fb6930d (branch `ui-check/v00-planted`) | **12/12 PASS**, with the planted defect |
| https://github.com/Nilscost/Calisthenics-app/actions/runs/37919210076 | 48b8963 (main) | light 4/4 PASS, dark and font13 FAIL: a blank emulator screen after the theme switch (flake) → settle step after each switch + one retry per flow |
| https://github.com/Nilscost/Calisthenics-app/actions/runs/37920253931 | 5f2019a (main) | **12/12 PASS**, no retries |

## Planted-defect test
- **Defect:** on `ui-check/v00-planted` (commit fb6930d, never merged), the Preview's Start label was given the button's own colour (`colorScheme.primary` on a primary button), which makes it invisible. All flows still pass, because the button works by its tag.
- **Retrieval:** I fetched the shots the way the executor will:
  ```
  git fetch origin ui-shots && git archive origin/ui-shots fb6930d | tar -x
  ```
  Then I opened `fb6930d/b_train_preview/02-preview-light.png` with vision.
- **Found:** the full-width primary button at the bottom of "Your workout" shows **no label**, just an empty blue bar. The same screen from the clean run `ebb4dc7` shows "Start".
- **Detected by looking**, not by a test.

## Baseline observations from the clean run (for the executor; not fixed in V00)
| Screen | Variant | Score | Note |
|---|---|---|---|
| Preview | dark | 4 | readable; letter circles instead of icons (R9, planned) |
| Train | font13 | 4 | fits on one screen at 1.3; no "Profile" label above the chips (R8, planned) |
| Session logger (get-ready block) | dark | 3 | big empty area between logger and timer; "Get ready: Chest Stretch" and "Next: Stretch: Chest Stretch" repeat each other |
| all screens | dark | — | **the status bar stays light grey in dark mode** (not themed): a real defect, not in the plan; fix with V10 or the first UI task that touches the theme |
| Session stretch block | light | 3 | the stretch clip is cut off at the top and right of its frame (seen in run c1b97c9) |

## Limits
- The emulator is not the S21: there is no dynamic colour, the One UI font differs, and haptics, sound, voice cues and screen-off behaviour are untested. The owner phone checks stay.
- Flows A–D start from a fresh install, so Progress has no stars yet; task V00b adds seeded data.
- Two runs publishing at the same moment can overwrite each other's folder on `ui-shots` (force push). Re-run the workflow if a folder is missing; the Actions artifact always has the full set.
