# Release 9.3: review and plan for the owner's 9.2 feedback (2026-11-12)

Written by the reviewer (read-only pass over `codex/kmp-port` at e257177a). Nothing was changed.
An Opus coordinator runs this plan with Sonnet executors, one worktree per lane, as for 9.1 and 9.2
([doc 42](42-feedback-2026-11-06.md)). All progress is replayed from history
(`ProgressionEngine.replay`), so every rule change must leave the owner's existing levels where they are
unless this plan says otherwise.

## 1. What the owner reported, and what the code does

| # | Report | Cause found |
| --- | --- | --- |
| 1 | Walking lunge and Russian twist do not say whether reps are per side or total. The owner has done the numbers per side. | Both are `sides = null`, `unilateral = false` (side mode NONE), so the target is shown bare ("8 reps"). Only 10 exercises are `ALTERNATING` (`ALTERNATING_IDS` in `tools/gen_r8_catalog.py`). About 30 more alternate sides inside one set and have the same gap (list in §2 L1). |
| 2 | Some clips are unreadable or wrong: the long-lever (RKC) plank looks like a normal plank; the single-leg glute bridge has odd limbs. | 516 of 533 clips are still the old format: two hand-typed poses joined by `rep(a, b)`, thin mannequin, no movement contract. Only 17 have a contract in `tools/blender/motion_contracts.json`. |
| 3 | The archer push-up looks better. Can that be generalised? | Yes, see §3. Codex's archer (`families/push_shoulders.py:89`) differs in four ways: a dense continuous trajectory (145 keys) built by small constraint solvers (planted toes, straight assisting arm); `anatomy_detail` torso mesh; a per-exercise contract checked by `motion_quality.py`; three filmed references in `motion_references.json`. Only the first needs per-exercise work. |
| 4 | Pull-ups and chin-ups are harder than other exercises; levels jump too far. | Pull-up levels are 1 / 3 / 5 / 8 / 10 reps. After three good sessions the target jumps to the next level (5 to 8 is +60 %). Nothing ramps inside a level. |
| 5 | In the Prepare phase the sheet shows the hold logger of the previous exercise ("Side Plank · set 2 · Seconds you held 40 s") instead of what comes next. | `loggerBlock()` in `SessionScreen.kt` returns the previous hold for TRANSITION blocks too, and when a logger is shown the upcoming target line (`sub`) is not rendered at all. |
| 6 | End screen: a mastered exercise (archer push-up) should show the next exercise in the same card, under a separator. | `ExerciseCard` shows only "Mastered". `SessionProgressSummary` does not carry the next exercise of the route. |
| 7 | End screen: two "Level 2 unlocked" cards, one says "1 of 3", the other "0 of 3". Expected "3 of 3" at level 1. | Two different level-up paths in the engine. `tryAdvance` (three good sessions) clears the streak: 0 of 3. The adoption path (typed reps met the next level's targets, `ProgressionEngine.kt` ~l. 303) sets `streakDays = listOf(day)`: 1 of 3. The card then shows progress toward the *next* level in both cases and never says why the level was reached. |
| 8 | "Too hard" / "Too easy" are missing from the end-screen cards. Inverted row is too hard and should go down a level. | The chips exist only inside "Edit what I logged" (`RoundEditor`). "Too hard" is a BELOW rating: it takes two in a row to produce a suggestion, shown in Progress, not here. |
| 9 | New: export a file from Settings that a chatbot can read (instructions + exercise library), and import routines the chatbot writes. | Nothing exists. Reusable parts: `BackupFileLauncher` (text file out / in on Android and web), `SavedRoutine` + `SavedRoutineStore`, `savedFromProgram`. |

Also seen in the screenshots, not reported:

- The archer push-up is "Mastered" and was still in the workout, although 9.2 made the half one-arm push-up the next step. Either the route did not move on (`activeInFamily`) because the five stars came from a hand-set level, or the planner ignores it. **Investigate first in lane L3; fix if it is a bug, report if it is intended.**
- The Prepare title reads "Prepare: Single-Leg Glute Bridge (per side) — Left side": the side is said twice.

## 2. Plan: app (release 9.3)

Branch `fix/feedback-2026-11-12` from `codex/kmp-port`. Five lanes, each a Sonnet executor in its own worktree; the coordinator merges. Shared `strings.xml` is append-only per lane to keep merges trivial. Every lane adds tests first for the behaviour named here and keeps `scripts/verify.sh` green.

### L1 · Per-side clarity (catalog)

Owns: `tools/gen_starter_catalog.py`, `tools/gen_r8_catalog.py`, generated catalogs (`app/src/main/assets/`, `content/starter/`), catalog tests.

Rule (owner's reading): **the stored number is per side; the screen shows "16 reps (8 per side)"**, which `ALTERNATING` already does everywhere (Preview, workout, logger, History).

- Add to `ALTERNATING_IDS`, numbers unchanged: `walking-lunge`, `russian-twist`.
- Review every exercise that works left and right in one continuous set. Starting list from the catalog scan (decide each one, record the decision in the evidence file): `dead-bug`, `dead-bug-single-leg`, `bird-dog`, `plank-arm-leg-reach`, `plank-shoulder-tap`, `mountain-climber`, `mountain-climber-slow`, `mountain-climber-cross-body`, `hanging-knee-raise-twisting`, `windshield-wiper-floor`, `windshield-wiper-bent-knee`, `hanging-windshield-wiper`, `hanging-windshield-wiper-half`, `heel-hook`, `pullup-typewriter`, `pullup-commando`, `bounding-alternate`, `box-push-off-alternating`, `jump-split`, `ankle-hop-side-to-side`, `ankle-hop-hip-twist`, `jump-lateral`, `cone-hops-lateral`, `lateral-shuffle`, `carioca`, `side-skip`, `high-knees`, `butt-kicks`, `skipping`, `power-skip`, `bear-crawl`, `crab-walk`, `squat-thrust-step-back`. Army-drill items (`routineOnly`) keep their source counts.
- `plank-shoulder-tap` is the one exception to "numbers unchanged": 9.2 set 10 to 20 as *total* taps. Make it ALTERNATING with 5 / 6 / 7 / 8 / 10 per side so the total stays the same. Its old logs were totals; use the policy version / `targetRevision` mechanism so they are not re-read as per-side numbers, and prove with a replay test that no level jumps.
- Guard: a catalog test that fails when a name or instruction says "alternat…", "each side", "side to side" or "per side" and the side mode is NONE, with an explicit allow-list.
- Small: on the workout screen do not print "(per side)" from the name when the side is already shown ("— Left side").

Acceptance: walking lunge shows "12 reps (6 per side)" in Preview, workout and History; the owner's replayed levels are identical before and after (fixture test on the evidence shape, not on his private data).

### L2 · Prepare phase shows what comes next

Owns: `SessionScreen.kt`, its tests and Maestro flows.

- In a TRANSITION block the sheet shows, in this order: the upcoming exercise name, its target and side in the headline style ("8 reps · Left side"), then, only if the block before was a hold, one compact secondary row "Side Plank held · − 40 s +" (same tags as today's logger so the hold can still be corrected).
- In rest and stretch blocks nothing changes (the hold logger stays the main element there).
- While here, split `LiveScreen` (about 170 lines, three interleaved branches) into `SessionHeader`, `SessionInfo` (name + target) and the button row. No behaviour change beyond the above.

### L3 · End screen: one card, complete

Owns: `SessionProgressSummary.kt`, `EndScreen.kt`, `RoundEditor.kt`, their tests and flows.

1. **Why the level was reached.** Add `levelUpBy: STREAK | DEMONSTRATED` to `ExerciseProgress`.
   - STREAK: the dots line is the finished one, all filled: "3 of 3 good sessions at Level 1" (or 4 of 4 on the weekly path). No next-level dots on this card today.
   - DEMONSTRATED: line "You did the Level 2 reps today", then the normal dots ("1 of 3 good sessions to Level 3").
2. **Too hard · Too easy · Pain on every card**, always visible, same chips and tags as `RoundEditor`. "Edit what I logged" becomes an expand control on the card that reveals the per-round steppers; the separate list of edit cards below is removed (one card per exercise, one place to correct).
3. **Acting on the rating, in place.** After "Too hard" on a level above 1 the card shows a button "Go down to Level N−1"; after "Too easy" the button for what `Suggestions.moveOn` already offers (two levels up, or the next exercise). One tap applies the existing `AcceptEasier` / `AcceptHarder` / `AcceptNextExercise` action; nothing changes without the tap. The Progress suggestion card stays and must not repeat an action already taken here.
4. **Mastered, then what.** When the exercise is mastered (now or before) and its route has a next step, the card shows a divider and "Next: Half One-Arm Push-Up · Level 1" with its thumb. If the next exercise has its own card in this session, show only the name.
5. Resolve the "mastered but still planned" question above.
6. Tidy: one `awaitSaved()` helper instead of the two copied polling loops; imports instead of inline fully-qualified names in the touched files.

### L4 · Gentler steps inside a level

Owns: `ProgressionEngine.kt` (target only), planner target lookup, tests.

One rule for every rep exercise, no per-exercise flag: **inside a level the prescribed target climbs toward the next level's target with each good session.**

`target = levelTarget + min(gap − 1, floor(goodSessions × gap / 3))`, where `gap = nextLevelTarget − levelTarget`.

| Exercise | Level | Today | With the ramp |
| --- | --- | --- | --- |
| Pull-up | 3 to 4 | 5, 5, 5 then 8 | 5, 6, 7 then 8 |
| Pull-up | 1 to 2 | 1, 1, 1 then 3 | 1, 1, 2 then 3 |
| Calf raise | 1 to 2 | 20, 20, 20 then 25 | 20, 21, 23 then 25 |
| Push-up (gap 1) | any | unchanged | unchanged |

Invariants, each with a test:
- The level-up gate is unchanged: three good sessions (or four in 30 days), where "good" still means every set at or above the **level's own** target. Missing the ramped target but meeting the level target is a good session, not "below target", and shows no "Level kept" chip.
- The existing rules keep priority: a reduced target after a shortfall, the level-5 top target, typed reps that meet the next level (adoption).
- Replaying any existing history gives the same levels and stars as before; only the next prescription differs.
- Holds: same formula in 5 s steps. Level 5 has no next level, so no ramp.
- Preview, workout, the exercise page and the end-screen "next target" chip all show the ramped number from the same function.

### L5 · Chatbot export and routine import

Owns: new `domain/.../routine/RoutineExchange.kt` (+ tests), a new settings screen, `SettingsScreen.kt` (one row), strings.

- **Settings → Data → "Routines from a chatbot"**, two actions with a caption each.
- **Export guide** writes `calisthenics-guide.md`, built at run time from the loaded catalog so it is never stale:
  1. what the app is and what to produce, in a dozen lines;
  2. the routine format with one example;
  3. the library as a table: id, name, reps or hold, side mode, equipment, body area, level 1 to 5 targets. `routineOnly` and stretches in separate tables.
- **Routine format v1** (JSON, the smallest thing that works):
  ```json
  {"calisthenicsRoutine": 1, "name": "Pull day", "sets": 3, "format": "CIRCUIT",
   "exercises": [{"id": "pullup-full"}, {"id": "plank-forearm", "seconds": 30}, {"id": "walking-lunge", "reps": 8}]}
  ```
  `reps` / `seconds` are optional: left out, the app uses the person's current level (progression works as usual); given, they are a fixed number (per side for sided exercises). A top-level array holds several routines.
- **Import routine**: pick a file *or paste text* (chat apps on a phone give text). The parser takes the first JSON object or fenced block, checks ids against the catalog (an unknown id is reported with the closest names), ranges and equipment, then shows a summary and "Save". Saved routines land in the existing `SavedRoutineStore` and appear under My routines on Train. No new storage, no backup-format change (saved routines are already in the backup).
- Parser and exporter are pure domain code with round-trip tests; the screen only wires `BackupFileLauncher` and the clipboard.

### Release

As 9.2: merge lanes, `scripts/verify.sh`, web smoke, `ui-screens`, screenshots of the end screen, Prepare and the new settings screen in light and dark. Version `0.12.3-r9`, code 22. APK to `Apps/builds/app-debug-R9.3.apk` with the same signature; push to `codex/kmp-port` (redeploys the web app). STATUS.md gets a 9.3 section with phone checks; `docs/user-feedback.md` gets the log. PR #4 stays unmerged.

## 3. Plan: demonstration clips (separate lane L6, not blocking 9.3)

Branch `video/reference-batch-a`. Owns `tools/blender/**`, `tests/test_*` for it, `work/` (untracked). Follows [doc 39](39-video-generation-and-review-procedure.md); this section only adds what is missing to make it repeatable for the 516 old clips.

**What generalises from the archer push-up**

| Part | Generalise how |
| --- | --- |
| `anatomy_detail` torso mesh, clearer contrast | Turn on for every re-rendered clip. Verify once that the native geometry gate passes for floor, hanging and inverted poses. |
| Continuous trajectory instead of two poses | Keep `rep(a, b)` for simple strict reps but sample it through `sample_continuous` with pauses at both ends; author phases (as the archer does) only where the movement has more than two. |
| Constraint solvers (planted feet/hands, straight limb) | Already reusable in `motion_constraints.py`; use them instead of typing joint positions. |
| Per-exercise contract + bespoke validator | Stop writing one validator per exercise. Add **movement classes** to `motion_contracts.json` (plank hold, bridge, push-up, pull, row, dip, squat, lunge, hinge, leg raise, twist, static hold, jump) with shared checks: planted contacts do not slide, segment lengths fixed, minimum range of the class's main joint, floor and prop clearance, loop seam, speed. An exercise then needs a class and three to six numbers. |
| Filmed references | New step below. |

**Using YouTube videos for correct form**

References are for local analysis only: they stay in ignored `work/references/`, are never committed, shipped or re-encoded into the app. The repo keeps only URL, creator, interval and file hash.

1. `motion_references.json` gets, per exercise, two or three coaching demonstrations with the exact interval of one clean repetition.
2. New `tools/blender/reference_fetch.py`: downloads only that interval at low resolution with `yt-dlp` (to be installed with Homebrew), caches it, records the hash.
3. New `tools/blender/reference_sheet.py`: extracts 6 to 8 evenly spaced frames per repetition and, where MediaPipe sees the body (`pose_compare.py extract-references`), a small table of joint angles per frame (elbow, shoulder, hip, knee, trunk angle). MediaPipe often fails on floor and inverted poses, so the frames are the primary evidence and the table is optional.
4. The executor writes the pose from the sheet, renders a low-sample preview with `preview_sheet.py`, and puts candidate frames **beside** the reference frames in one contact sheet. A reviewer (the coordinator, by looking at the sheet) accepts or sends it back with a concrete observation. This side-by-side look is the form check; green scripts alone are not.
5. Then doc 39 as written: contract, native geometry preflight, `render_batch.py`, encode, gallery for the owner.

**Batch A** (the owner's current workout first; the two named complaints lead):

1. Long-lever (RKC) plank: elbows clearly ahead of the shoulders (under the eyes), forearms converging with fists together, pelvis tucked, feet together. It must not be mistakable for the forearm plank next to it.
2. Single-leg glute bridge (`bridge-single-leg`): fix the limbs; shoulders on the floor, hip level at the top, free leg in line.
3. Walking lunge · 4. Russian twist · 5. Side plank · 6. Dip · 7. Inverted row · 8. Glute bridge · 9. Split squat · 10. Plank shoulder taps.
Controls (must stay byte-identical): `pushup-archer`, `pullup-full`.

Batch A is installed into the app only after the side-by-side review passes for each clip; clips that do not pass keep their current version. The owner gets the review gallery either way. If Batch A is ready before the 9.3 build it ships in it, otherwise as a media-only update.

**Backlog after Batch A**, batches of eight to ten with two accepted controls each: (1) every exercise in the onboarding ladders and ready-made routines, (2) the rest of the main routes family by family, (3) alternates, stretches and warm-ups last. That is roughly 50 batches; it is its own track across several sessions, not part of this run.

## 4. Code review: simplicity and consistency

Sizes: domain 14 000 lines (+ 8 000 tests), shared UI 8 400, data 1 800, Android shell 490, web 86. The KMP split is clean: the Android module is only the service and audio. The findings below are ordered by value; only those marked **do** are in this run.

| Finding | Action |
| --- | --- |
| Two level-up paths with different visible results (0 of 3 vs 1 of 3) and no reason given to the UI. | **do** (L3.1) |
| "Too easy" once gives a suggestion, "Too hard" needs twice, and both act on another screen. | **do** (L3.3) |
| End screen renders every exercise twice (card + edit card); the same save-polling loop is copied twice. | **do** (L3.2, L3.6) |
| `LiveScreen` is one 170-line composable; the bottom sheet has three branches that each re-decide name / target / logger. Source of bug 5. | **do** (L2) |
| Side mode is expressed three ways: `unilateral`, `sides`, and "(per side)" typed into names. | **do** the guard test and the duplicate side text (L1); do not rename exercises. |
| Legacy clip generators `tools/gen_demo_clips.py`, `gen_demo_clips_v3.py` (1 096 lines), `gen_clips_ai.py`, `gen_keyframe_clip.py` and `tests/test_clips_v3.py` predate Blender. | **do** in a cleanup lane (L7) after confirming no installed clip is produced by them: delete. |
| One-off validators per video batch (`generation_checks/next_*.py`, nine `check_*.py`, 28 test files). | Stop the growth with movement classes (L6); do not rewrite the existing ones. |
| 8 agent worktrees and about 12 merged branches are left over. | **do** (L7): remove only worktrees and branches fully merged into `codex/kmp-port`. Never touch `codex/video-review-media` or its worktree under `~/.codex`. |
| 452 lines over 200 characters, 219 inline fully-qualified names in shared UI, about 250 ticket tags (R7-T03, V10, W2) in comments that need the old docs to decode. | Fix only in files a lane touches: imports, wrap at about 160, keep the explanation and drop the tag. No repo-wide reformat. |
| `ProgressionEngine.replay` is one function of about 300 lines with the v0 and v1 evidence paths interleaved. | Leave. It is covered by tests and a split now would risk the owner's history. Revisit when v0 evidence can be dropped. |
| Twelve `screens/*Store.kt` wrappers of 15 to 20 lines that only add `ctx.settingsProvider()`. | Leave; harmless. |
| Android `res/values/strings.xml` (564 lines) mirrors the shared Compose strings (603) through `convert_strings.py --check`. | L7 investigates what still reads the Android copy (service notification, tests). Remove the mirror only if it is a handful of strings; otherwise leave and say so. |
| `catalog.json` (1 MB) is committed twice, byte-identical, by design (app asset + content pack). | Leave; add one `scripts/gen_catalog.sh` that runs the generator chain if none exists. |
| Package name `io.github.gonbei774.calisthenicsmemory` (upstream fork) beside `app.calisthenics`. | Leave; renaming buys nothing for users. |
| `work/` is 9 GB and `.git` 2.1 GB (rendered media history). | Owner's call; L7 only lists the largest directories. |

L7 runs last, after the five app lanes are merged, and may not change behaviour.

## 5. Coordination

- Order: L1, L2, L3, L4, L5 in parallel (disjoint files except append-only strings); L6 in parallel on its own branch; L7 after the merge; then the release steps.
- The coordinator reviews each lane's diff and test output before merging and looks at the screenshots itself. An executor's report is not proof.
- Decisions taken in this plan where the owner left room: per-side is the stored meaning (L1); the ramp applies to every rep exercise, not a hand-picked list (L4); reference videos are local analysis only (L6); only Batch A is rendered in this run.
- Stop and report instead of guessing if: a replay test shows an existing level would change; the "mastered but still planned" case turns out to be a planner design question; a Batch A clip cannot pass review after two rounds.
