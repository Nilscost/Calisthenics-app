# 25 · R8 handoff: next steps (high level, for Codex)

Written 2026-10-14 by the Opus coordinator, who handed off at his usage limit. R8 adds 379 exercises, 8 fixed routines and a Pilates option. The app is a personal calisthenics app for Nils (Kotlin, Compose, Room; `:domain`, `:data`, `:app`). **All training content is an unreviewed DRAFT. Never claim phone testing and never approve owner gates.**

## Where things stand
- R7.2 (`0.10.2-r7`, versionCode 12) is released to the owner. His phone check is pending (`docs/evidence/owner-check-r7.2.md`).
- Research is done. The final report is `/Users/nils/Documents/hermes/Apps/reports/Bodyweight library gaps owner books.md`, with notes in `/Users/nils/Documents/hermes/Apps/research_notes/` (the owner's books are there too, for page checks).
- **The contract** is `docs/23-new-exercises.json`, summarised in `docs/23-exercise-additions.md`. It holds the exercises, new families, tabs and equipment, the aliases and options, the routines and the pose groups. Change it only to fix errors, and note every change in the progress notes.
- Pose instructions: `docs/24-pose-task-r8.md`.

## Working rules (unchanged)
`docs/19-r7-executor-handoff.md` §2–§3 still apply: the sandbox build and verify, the push command, the per-task loop, ui-screens review, progress notes, and appending new data-class fields last with defaults. Use the commit prefix `R8-TNN:`. Doc 17 is the visual system.

## Next steps
Two tracks. They can run in parallel; the clips can only be installed once T03 lands.

### Track A: clips (start now, so the owner can render tonight)
1. **T01 Poses, batch 1 first.** Follow doc 24: two pose groups at a time, check them, write `render-batch-1.txt`, commit. Then give the owner the one-line render command from doc 24.
2. **T02 Poses, batches 2 and 3.** Same loop. Each finished batch gets its own overnight render.
3. After each render: encode → install (needs T03) → full verify → commit.

### Track B: app (release R8)
1. **T03 Catalog import.** Write a small, tested generator (Python in `tools/`) that merges `docs/23-new-exercises.json` into `app/src/main/assets/catalog.json` (and `content/` if that is the source the app build uses; check first). It adds:
   - equipment, families and variations;
   - a progression policy per new id, copying the nearest sibling's policy;
   - `clip_meta.json` entries.

   Don't hand-edit 379 entries. Existing tests must stay green; add a catalog-integrity test that every routine item and alias resolves.
2. **T04 Tabs and tree.** Add the new tabs (Rings, Power, Conditioning, Prehab, Pilates) and extend existing ones in `SkillTree.kt` (`TreeTabs`). Hide routine-only families (`army-drill`) from browsing. Optionally do the renames the report suggests: Shoulders → "Handstand", German hang → "Back lever".
3. **T05 Equipment filter.** New equipment ids (rings, ab wheel, bench, pole, sliders and others) go into onboarding/settings, so exercises the owner lacks stay hidden.
4. **T06 Aliases and search.** Show "also known as" names on detail screens and match them in search, e.g. Teaser finds V-up.
5. **T07 Fixed routines with a flow option.** A read-only "Programs" list holding Army 4C, HSD, SSD, PD and RD, and the Pilates Beginner, Intermediate and 10-min core flow. Starting one runs the workout player with the given items. **Flow option:** per routine (default from the JSON, switchable): auto-advance after reps or `estSeconds`, no rest timer, no logging per set; one "done" at the end. 4C stays non-flow. The SSD rep count (10) is a flagged guess.
6. **T08 Options.** Store the option sets (grip, hand, stance, tempo) as data. Showing them as a picker on the logger can wait unless it is cheap.
7. **T09 Progression engine.** New ladders must work with the existing engine (Too easy → move on, D5/D7). For plyometrics and Explosive Calisthenics moves, cap at 3 reps per set and gate them behind `pushup-standard` / `squat-air` (see the notes). Depth jumps stay locked until box jumps are solid.
8. **T10 Screenshots and release.** ui-screens all PASS (add flows for one new tab, the Programs list and a flow-mode workout). Version `0.11.0-r8`. Then: APK into `/Users/nils/Documents/hermes/Apps/builds/`, update `STATUS.md`, and write `docs/evidence/owner-check-r8.md` (one walk-through step per new area, ending with scores).

## Open questions for the owner (list them in the release report)
- SSD reps (10 chosen).
- The stand-in enums: plyo and conditioning use SQUAT/LUNGE patterns, neck uses UPPER_BACK. Should we add proper enums?
- Kept despite a "skip or later" in the notes: Russian and L-sit dips, the shoulder roll, the kipping pull-up (with a caution).
- The human-flag posture descriptions are paraphrases. Check them against the CC2 photos.
- Which equipment he owns, which decides which tabs he sees first.
- Whether to add Army CD1–3 and CL1–2 as routines too (cheap: their moves already exist).

## Don't
- Run `poses.py` without ids.
- Delete or weaken tests to make them pass.
- Touch the R7.2 owner-check flow while the owner is testing it.
- Overwrite Claude's or anyone else's uncommitted work: check `git status` and `git log` first.
