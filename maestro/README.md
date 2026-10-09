# UI screenshot flows (V00)

Maestro flows run by `.github/workflows/ui-screens.yml` through `scripts/ui_shots.sh`, on an API 34 emulator set to the
Galaxy S21's 1080×2400 at 420 dpi, in three variants: `light`, `dark`, `font13` (font scale 1.3).

- Flows run in file order per variant. `a_onboarding` starts from a fresh install (`clearState`); later flows continue
  from the state it leaves (onboarding done, Normal answers).
- Each flow gets two variables: `VARIANT` and `SHOTS` (its output folder). Name screenshots
  `${SHOTS}/<NN>-<step>-${VARIANT}` (Maestro adds `.png`).
- Find controls by test tag (`id:`; the app exposes test tags as resource ids) or by visible text. Bottom sheets and
  dialogs are separate windows: there, use text.
- A failing flow does not stop the run; `results.txt` lists PASS/FAIL per flow and variant, and a `zz-failure-<variant>.png`
  shows the screen where it stopped. Maestro's own debug output is in the Actions artifact (`.maestro/tests`).

Why Maestro and not instrumented Compose tests: no new Gradle dependencies (the sandbox builds offline from a fixed
cache), the flows drive the real debug APK as a user would, and they are plain YAML the executor can extend without
compiling. Instrumented tests stay possible later for things Maestro cannot reach (e.g. the Room migration test).

Read the result: `git fetch origin ui-shots && git show origin/ui-shots:index.md`, or
`https://raw.githubusercontent.com/Nilscost/Calisthenics-app/ui-shots/<short-sha>/<flow>/<file>.png`.
