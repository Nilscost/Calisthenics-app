# 11 — Implementer handoff (paste this as the first message to the implementing model)

You are implementing the UX/UI overhaul of a personal calisthenics Android app (Kotlin, Jetpack Compose, Room). The owner is Nils; he tests by sideloading APKs on a Galaxy S21 (no USB, no adb). You cannot test on his phone; never claim device testing.

## Read first (in this order)
1. `docs/10-ux-overhaul-plan.md` — the contract: owner feedback F1–F18, owner decisions Q1–Q8 (answered), target experience §3, content §4, clips §5, design system §6, ordered tasks U01–U13 (+U01b) in §7.
2. `docs/07-decisions-and-blockers.md` and `docs/02-coding-specification.md` — older approved rules that still apply.
3. Inspect the repo before editing. File paths in the plan are starting points, not guarantees.

## Repo and build
- Project (git, branch `main`): `/Users/nils/Documents/hermes/Apps/calisthenics`.
- If you run in the Hermes Docker sandbox, build in `/workspace/cal` (has `local.properties` with `sdk.dir=/opt/android-sdk`) and sync to the Mac project with:
  `rsync -a --exclude .git --exclude build --exclude .gradle --exclude local.properties --exclude .kotlin --exclude '*.png' --exclude 'verify*.log' ./ /Users/nils/Documents/hermes/Apps/calisthenics/`
  Before starting, make `/workspace/cal` match the Mac repo HEAD (the Mac repo is the source of truth).
- Verify command (offline; do NOT run plain `./gradlew test`, it pulls release tasks that fail offline):
  ```
  export JAVA_TOOL_OPTIONS="-Dorg.sqlite.tmpdir=/root/sqlitetmp -Djava.io.tmpdir=/root/sqlitetmp"
  ./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline > verifyNN.log 2>&1; echo exit=$?
  ```
  Read the real exit code (not the exit code of a grep after a pipe). `/tmp` is noexec. No internet for Gradle: only use dependencies already in `/root/.gradle/caches/modules-2/files-2.1/` (check there before adding any library; Robolectric and material-icons-core are cached; navigation-compose and material-icons-extended may not be).
- Catalog is generated: edit `tools/gen_starter_catalog.py`, run it, then `cp content/starter/catalog.json app/src/main/assets/catalog.json`. Clips: `tools/gen_demo_clips.py` (v1) — v2 goes in a new script per plan §5.

## Rules (mandatory)
- One task at a time, in the plan's order (U01, U01b, U02 … U13). Tests first where logic is involved.
- After each task: full verify → copy log to `docs/evidence/uNN-verify-2026-MM-DD.log` → sync → commit on the Mac repo with
  `git -c user.name="Nils Costanzo" -c user.email="51085848+Nilscost@users.noreply.github.com" commit -m "UNN: <summary>"` → copy `app/build/outputs/apk/debug/app-debug.apk` to `/Users/nils/Documents/hermes/Apps/builds/app-debug-UNN.apk`.
- Append a short entry per task to `docs/evidence/progress-notes.md`: what changed, test result (real), anything NOT done, any choice not covered by the plan ("choices not in the plan" list).
- Data-model fields: append LAST with a default (tests construct data classes positionally). Room: additive migrations only, never destructive; keep old tables when deleting old features (U01b).
- All catalog numbers are DRAFT; never mark content reviewed. Never approve owner gates.
- Do not invent results. If something can't be done offline or blocks, write it in progress notes and move to the next independent task.
- **Stop points:** stop after U07 and report (owner phone test). U09 new progression chains need the owner's OK on plan §4.1 — if not recorded in the plan as approved, skip U09's new chains (muscles may proceed) and continue with tasks that don't depend on them.
- Keep secrets out of git: `Apps/keys/*` (release key) never gets copied into the repo.

## Report format at each stop
- Tasks done, with commit hashes and APK names.
- Real verify results (exit codes).
- What the owner should check on the phone (from the plan's "Owner check" column).
- Open issues and choices not in the plan.
