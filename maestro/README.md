# UI screenshot flows (V00)

Maestro flows run by `.github/workflows/ui-screens.yml` through `scripts/ui_shots.sh`, on an API 34 emulator set to the
Galaxy S21's 1080×2400 at 420 dpi, in three variants: `light`, `dark`, `font13` (font scale 1.3).

- Flows run in file order per variant. `a_onboarding` starts from a fresh install (`clearState`); later flows continue
  from the state it leaves (onboarding done, Normal answers).
- R8 coverage is split across `l_r8_catalog` (Power route + alias search), `m_r8_programs_flow` (compact Programs list, authored details and flow workout), `n_r8_preview_rest_extras` (Train's extra pools and adjustable plain recovery), and `o_r8_progress_routes` (default pool, deliberate all-routes picker and collapsed alternate). `d_tabs` also selects the Planche route explicitly before its linked prerequisite detail.
- Progression routes that require equipment stay hidden until the selected profile has it. Route-picker flows should opt into All progressions and select a named route; do not treat a missing route chip under the default profile as a screen failure.
- Each flow gets the variable `VARIANT`. Name screenshots `<NN>-<step>-${VARIANT}` with no folder (Maestro adds
  `.png`; it refuses paths outside its output folder, and the script files them under the flow's name).
- Find controls by test tag (`id:`; the app exposes test tags as resource ids) or by visible text. Bottom sheets and
  dialogs are separate windows: there, use text.
- Every flow runs `../common/dismiss_system_dialogs.yaml` right after `launchApp` (emulator ANR dialogs). Keep it in new flows.
- A failing flow is retried once (logged as RETRY), then recorded; it does not stop the run; `results.txt` lists PASS/FAIL per flow and variant, and a `zz-failure-<variant>.png`
  shows the screen where it stopped. Maestro's own debug output is in the Actions artifact (`.maestro/tests`).

Why Maestro and not instrumented Compose tests: no new Gradle dependencies (the sandbox builds offline from a fixed
cache), the flows drive the real debug APK as a user would, and they are plain YAML the executor can extend without
compiling. Instrumented tests stay possible later for things Maestro cannot reach (e.g. the Room migration test).

Read the result: `git fetch origin ui-shots && git show origin/ui-shots:index.md`, or
`https://raw.githubusercontent.com/Nilscost/Calisthenics-app/ui-shots/<short-sha>/<flow>/<file>.png`.
