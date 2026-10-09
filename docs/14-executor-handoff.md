# 14 — Executor handoff, version 2 (paste as the first message to the Sonnet executor)

You implement version 2 of a personal calisthenics Android app (Kotlin, Jetpack Compose, Room). The owner is Nils. He tests by sideloading APKs on a Galaxy S21; you cannot reach his phone. **Never claim phone testing, never approve owner gates, and keep all training content DRAFT.**

## 1. Read first, in this order
1. `docs/12-v2-plan.md`: the contract. Owner feedback R1–R29, decisions D1–D15, answers O1–O6, target experience §5, content §6, tasks §8. **§11 (reviewer changes) overrides §8 where they differ**, and the task order is in §4 below.
2. `docs/11-implementer-handoff.md` "Rules": they still apply, with the updates in plan §10.
3. `maestro/README.md`: how the UI screenshots work.
4. Inspect the code before editing. Paths in the plan are starting points.

## 2. Environment and commands
- **Repo (source of truth):** `/Users/nils/Documents/hermes/Apps/calisthenics`, branch `main`, public at github.com/Nilscost/Calisthenics-app.
- **Build** in the Hermes sandbox (aarch64 Linux, no emulator). From the Mac, prefix commands with `docker exec <hermes container> bash -lc '…'`; the container name changes, so find it with `docker ps`. The Mac folder `/Users/nils/Documents/hermes` is mounted at the same path inside.
  ```
  source /root/toolchain/env.sh
  cd /workspace/review
  # once per session: make the build tree equal to the Mac repo
  git fetch -q /Users/nils/Documents/hermes/Apps/calisthenics main && git reset -q --hard FETCH_HEAD && git clean -fdq -e local.properties
  # after editing on the Mac (or edit here and rsync the other way; never both):
  rsync -a --exclude .git --exclude build --exclude .gradle --exclude local.properties --exclude .kotlin /Users/nils/Documents/hermes/Apps/calisthenics/ ./
  ./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --console=plain > /workspace/verify-VNN.log 2>&1; echo exit=$?
  ```
  - Read the real exit code.
  - Gradle is offline: a new library must already be in `/root/.gradle/caches/modules-2/files-2.1/`. If it isn't, stop and ask.
  - Catalog: edit `tools/gen_starter_catalog.py`, run it, then `cp content/starter/catalog.json app/src/main/assets/catalog.json`.
  - Clips: `tools/gen_demo_clips_v2.py` (v3 from V06a on) needs `PYTHONPATH` from `env.sh`.
  - Review table: `python3 tools/gen_review_table.py`.
- **Commit** on the Mac repo:
  `git -c user.name="Nils Costanzo" -c user.email="51085848+Nilscost@users.noreply.github.com" commit -m "VNN: <summary>"`
  
  End the message with `Co-Authored-By: Claude Sonnet <noreply@anthropic.com>`. Never commit keys, personal data or the owner's Gmail. `Apps/keys/` stays outside git.
- **Push** (never print the token; never write it into git config or files):
  ```
  GIT_TERMINAL_PROMPT=0 git -c credential.helper= -c credential.helper='!f() { echo username=x-access-token; printf "password=%s\n" "$(tr -d "\r\n" < /Users/nils/Documents/hermes/.secrets/github_token)"; }; f' push origin main
  ```
  - The token covers Contents, Actions and Workflows for this repo only.
  - On 401/403, stop and ask the owner. Don't work around it (e.g. with another login).
- **GitHub API** (run status), with the token inline only:
  ```
  curl -s -H "Authorization: Bearer $(tr -d '\r\n' < /Users/nils/Documents/hermes/.secrets/github_token)" "https://api.github.com/repos/Nilscost/Calisthenics-app/actions/runs?head_sha=$(git rev-parse HEAD)" | python3 -c 'import json,sys; [print(r["name"], r["status"], r["conclusion"], r["html_url"]) for r in json.load(sys.stdin)["workflow_runs"]]'
  ```
  If `gh` is available and logged in on the Mac, `gh run list` / `gh run watch` give the same information.

## 3. UI screenshots (V00, already working)
- Every push to `main` that touches more than docs runs `.github/workflows/ui-screens.yml`, as does every push to a `ui-check/**` branch or a manual dispatch. It runs flows `maestro/flows/*.yaml` on an API 34 emulator at the S21's 1080×2400 / 420 dpi, in variants `light`, `dark` and `font13`.
- Output, about 15 minutes after the push:
  - the branch `ui-shots`: `<short-sha>/<flow>/<NN>-<step>-<variant>.png` (720 px wide; only the newest 5 runs are kept), `<short-sha>/results.txt` (PASS/FAIL per flow and variant) and `index.md`;
  - the Actions artifact `ui-shots-<sha>` (full size, plus Maestro debug output).
- Fetch:
  ```
  git fetch -q origin ui-shots && git show origin/ui-shots:<short-sha>/results.txt
  rm -rf /tmp/shots && mkdir -p /tmp/shots && git archive origin/ui-shots <short-sha> | tar -x -C /tmp/shots
  ```
  Then open the PNGs with your file-reading tool, which shows images. Look at them; don't only list them.
- **Add or extend a flow in the same commit as the UI it shows.** Tag new controls with `Modifier.testTag("…")`, which is visible to Maestro as `id:`. Bottom sheets and dialogs are separate windows: select by text there.
- A FAIL in `results.txt` blocks the task until it is fixed. That includes a flow broken by your UI change: fix the flow or the UI.

**Screenshot review, required for every task with a "Screenshot check" in plan §8:**
1. Push and wait for `ui-screens` to complete. Fetch the shots for that commit.
2. Open **every** screenshot of the screens the task changed, in all three variants. Open at least the `light` shot of every other flow, to catch regressions.
3. Write `docs/evidence/ui/VNN-review.md`:
   - the run URL;
   - a table with screen · variant · score 1–5 · defects;
   - defects fixed in this task;
   - defects left open, and why.
   
   Check for:
   - text cut off or overlapping (font13 especially);
   - contrast, and dark-mode colours that stayed light;
   - touch targets;
   - wrong or missing content against plan §5;
   - empty or placeholder states;
   - anything that looks broken.
4. Fix what you can and re-run. A task is not done while a screen you changed scores below 3.
5. Commit the review file with the task (or as `VNN: screenshot review` right after it).

Known open defects from the V00 baseline (`docs/evidence/ui/V00-review.md`):
- the status bar is not themed in dark mode;
- the stretch clip is cropped in its frame;
- an empty area and repeated text on the get-ready block.

Fix them in the first task that touches those screens (V10 at the latest) and say so in its review.

The screenshots do not replace the owner's phone test. Haptics, sound, voice cues, screen-off behaviour and the real-workout feel stay owner checks.

## 4. Task order
One task = one commit (or a commit plus its review commit). Tests first for logic. Plan §8 has the start files and checks; §11.3 has the changes.

| Order | Task | Notes |
|---|---|---|
| 1 | **V00b** debug seed | `src/debug` receiver + fixture backup in debug assets + flow `e_seeded.yaml` (History list, History detail, Progress with stars, a kettlebell sheet). No release-build code. |
| 2 | V01 | K1 clip fix. Add a flow step that screenshots a stretch block and the next work block. |
| 3 | V02 | domain only |
| 4 | V03 | logger redesign |
| 5 | **V04a** | Room 23 → 24 (`feedback_revisions` + nullable `blockId`, `actualHoldSeconds`). Write the migration and its test in `HistoryAndMigrationTest`. Add a job `connected` to `ui-screens.yml` that runs `./gradlew :data:connectedDebugAndroidTest` on the same emulator setup. That job must be green; if it can't be made to run, stop and report. |
| 6 | **V04b** | end-screen per-round editor + History per-round edit + "Too easy" |
| 7 | **V05 Release R1** | **STOP** (§6) |
| 8 | **V06a** | clip pipeline v3: thumbnails, muscle data, pose-uniqueness assertion; poses unchanged |
| 9 | **V06b** | own pose per variant (K2), pull-up (K5), full review `docs/evidence/clips-v3-review.md` |
| 10 | V07 | stretches to about 18 |
| 11 | V08 | body map |
| 12 | V09 | thumbnails and body map everywhere |
| 13 | V10 | full-screen workout video |
| 14 | **V11 Release R2** | **STOP** |
| 15 | V12 | questionnaire, plus the shared three-type goal control on Train; hide "Ready-made routine" until presets exist |
| 16 | V13 | vertical tree, Types/Skills tabs, stars below current |
| 17 | V14 | Automatic progression switch |
| 18 | **V15 Release R3** | **STOP** |
| 19 | V16 | Preview editor |
| 20 | V17 | Detailed edit |
| 21 | V19 | formats in the planner |
| 22 | V20 | workout screen for sets + Train Format control |
| 23 | V21 | progression rule per routine. Unlogged sets under Rep range count as the same numbers as last time (owner answer, §11.5 Q1). |
| 24 | V18 | saved routines (stores format, rule, rests, warm-up, stretch picks, overrides) |
| 25 | **V21b** | preset content C-B, C-C, C-D. Re-check the RR hinge and dip lists at the source and record the URL. Bodyweight-only RR dip slot = chair dips with a shoulder caution; support hold moves on at 3 × 30 s (owner answers, §11.5 Q2–Q3). |
| 26 | V22 | RR + Minimalist presets; show "Ready-made routine" in the questionnaire and on Train |
| 27 | V23 | warm-up switch for any workout (plumbing exists: `BlockType.WARMUP`, `Preferences.warmupOn`, `Planner.template`) |
| 28 | **V24 Release R4** | **STOP** |
| 29 | V25 | C-E gaps only |
| 30 | V26 | weight tracks |
| 31 | V27 | suggestion card, barbell and vest options |
| 32 | **V28 Release R5** | **STOP** |

## 5. Definition of done, per task
1. Tests for the logic written first, then the code.
2. Full verify exit 0. Copy the log to `docs/evidence/vNN-verify-2026-MM-DD.log`.
3. Data rules:
   - new fields go last, with defaults;
   - Room migrations are additive only, with a migration test;
   - old plans, evidence and backups still decode (add a decode test with a fixture from before the change).
4. UI tasks: flows updated, `ui-screens` green (all PASS), screenshot review written (§3).
5. A `progress-notes.md` entry: what changed, real results, what was NOT done, and "choices not in the plan".
6. Committed and pushed. CI `verify` green on that commit.
7. The APK copied to `/Users/nils/Documents/hermes/Apps/builds/app-debug-VNN.apk`.

## 6. Releases and stop points
A release task (V05, V11, V15, V24, V28):
1. Raise `versionCode` and set `versionName` (e.g. `0.4.0-r1`).
2. Run the full verify and get CI green.
3. Get a complete `ui-shots` run all PASS and review every screen (`docs/evidence/ui/R<n>-review.md`).
4. Write `docs/evidence/owner-check-r<n>.md` from the plan's "Owner check" column, short.
5. Copy the APK to `Apps/builds/app-debug-R<n>.apk`.
6. Update `STATUS.md`.
7. **Stop and report.**

Also stop when:
- an open owner question (plan §11.5, §4 O4) blocks the next task. Continue with tasks that don't depend on it, and list what you skipped;
- push or API access fails (401/403);
- a new library isn't in the offline cache;
- a Room change can't be done additively.

**Report at each stop:**
- tasks done, with commit hashes and APK names;
- verify exit codes;
- `ui-shots` run URLs and the review files;
- what the owner should check on the phone;
- open issues and choices not in the plan.
