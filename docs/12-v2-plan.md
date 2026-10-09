# 12 — Version 2 plan: owner phone test of 0.3.1 (2026-10-08)

Status: **draft for review.** Workflow:
1. An Opus reviewer reviews this plan and makes the UI-check capability (task V00) work end to end.
2. Only then does it hand over to a Sonnet executor.

The reviewer may reorder or split tasks, and must list every change it makes in §11 ("Reviewer changes"). It must not change owner decisions (§2); it raises them as questions instead.

Sources: owner phone test on Galaxy S21 (APK `app-debug-KB1.apk`, 0.3.1-kb) and two question rounds in chat on 2026-10-08. Earlier plan: `docs/10-ux-overhaul-plan.md` (F1–F18, U01–U13). Its rules still apply unless this document overrides them.

---

## 0. Code facts checked while writing this plan
| # | Owner observation | Cause found in code |
|---|---|---|
| K1 | Clip did not change from a stretch to Split Squat (C4) | `DemoPlayer` (`ui/screens/DemoClips.kt`) builds the `VideoView` once in `AndroidView(factory=…)`, with no `update` and no `key(file)`, so the first file keeps playing. |
| K2 | Feet-elevated push-up shows a standard push-up (C1, D3) | `tools/gen_demo_clips.py`: `pushup-diamond`, `pushup-feet-elevated` and `pushup-archer` all map to `_STD` (the standard push-up pose). v2 reuses those poses. |
| K3 | Parallettes "not used yet" | True: `EquipmentOption("parallettes", future = true)` exists, but no catalog exercise needs parallettes. |
| K4 | Stretch clips look flat | The 7 stretch clips come from v1 poses lifted into v2; there are only 7 stretches in total. |
| K5 | Pull-up clip poor | `pullup_v(...)` pose in v1; judged poor by the owner. Not otherwise analysed. |

## 1. Owner feedback (numbered for traceability)
**Questionnaire**
- R1. The goal choice has three top-level types, chosen with buttons: **Body focus** (full / upper / lower / core…), **Skill**, **Ready-made routine** (RR, Minimalist, and later the owner's saved routines).
- R2. Why are parallettes "not used yet"? (K3)
- R3. When choosing your level, show **all** levels of the progression, not only the entry ladder.
- R4. Every exercise gets a picture: a small icon, or a mini animation if feasible.
- R5. Muscle focus is shown as a **body figure coloured where it works**, not as muscle names.
- R6. The header shows the parts **Goal · Equipment · Level** (owner answer to Q2, three parts).
- R7. The "You are ready" page has no step dots, and its stars match the levels chosen.

**Train / Preview / routine editing**
- R8. Label "Profile" above the profile chips on Train.
- R9. Preview cards show the exercise icon instead of a letter in a circle, and a body figure instead of muscle names.
- R10. Swap sheet: shows the **whole progression** of the current exercise (with levels and icons); at the bottom an **"Other types"** button opens the other movement types (squat, pull-up…), each with its full progression.
- R11. You can see and change the stretches and breaks between exercises.
- R12. The full timeline uses the same card format, round by round. In the normal Preview you edit one round's stretches and the stretched body parts; per-round editing is in a **Detailed edit** mode.
- R13. You can save a workout as a routine; it then appears under "Ready-made routine" on Train and in the questionnaire.
- R14. You can add or remove blocks: a "−" per exercise, and a "+" between blocks to add an exercise or a stretch.
- R15. Reps are adjustable in the Preview (see D3).

**During the workout**
- R16. The video is full screen, with the controls on top of it.
- R17. The rep logger uses the same layout as the exercise screen: big number in the middle, − left, + right, **Done** below.
- R18. Stretch clips redone in 3D; more stretches in the library.
- R19. Review every clip for quality (pull-up and feet-elevated push-up are wrong).
- R20. Bug: the clip didn't change from a stretch to Split Squat (K1).

**After the workout / History**
- R21. The end screen allows editing **per round**, and has a **"Too easy"** button.
- R22. In History, each round has its own number that can be corrected.

**Progress**
- R23. The tree runs **vertically**.
- R24. Two views: **by exercise type** and **by skill**, the latter showing which exercises lead to a skill.
- R25. Family and exercise icons are more precise (same pipeline as R4).
- R26. Exercises below your current level show 5 stars.

**Weights and equipment**
- R27. Kettlebell weights inside an exercise: show heavier weights as locked steps until the profile has that bell; harder exercises at the same weight stay open.
- R28. Use other equipment (the owner wrote "barbells"; see O3).
- R29. A **suggestion card** in Progress: "at your level, consider X". It must stay fully usable with **bodyweight only**.

## 2. Owner decisions (2026-10-08)
- D1 (R10) Swap sheet = the current exercise's full progression, then an "Other types" button listing other types with their full progression. A swap to another movement type keeps the existing "not the same movement" warning.
- D2 (R6) Questionnaire header parts: Goal · Equipment · Level.
- D3 (R15) Preview reps:
  - the − / + control is a **level stepper** (it moves between the exercise's levels, e.g. 8 → 10 reps);
  - in **Detailed edit**, the same − / + still steps levels, but the number can also be typed freely; a free number does **not** count towards progression.
- D4 Global switch **"Automatic progression"** (Settings, default on):
  - **On:** levels rise automatically, and after level 5 the routine moves to the next exercise in the progression.
  - **Off:** everything freezes (levels and exercises); the owner changes things by hand.
- D5 Editing a saved routine offers **"Save as new"** or **"Update this routine"**.
- D6 (R12) Normal Preview: edit one round's stretches and the stretched body parts. Per-round stretches only in Detailed edit; stretches otherwise rotate automatically between rounds.
- D7 "Too easy": two sessions in a row marked too easy **suggest** one level up; the owner confirms; never automatic.
- D8 One kind of star (earned and assumed look the same). **This replaces** the old rule (spec ADR 0002 C5) to show self-reported levels separately.
- D9 No extra equipment owned now. Weights and equipment beyond bodyweight appear as **suggestions**; the app must be complete with bodyweight only.
- D10 Ready-made routines must be **faithful** to their sources.
- D11 Train gets a **workout format** toggle. The owner asked for rounds vs pairs; the assistant recommends three options: **Circuit · Pairs (supersets) · Straight sets**. See O1.
- D12 RR's **warm-up** is included. A warm-up can also be added to any workout; warm-up content must be created.
- D13 All changes are in scope (R1–R29). Delivery is in releases with an owner phone test after each (§9).
- D14 Workflow: plan → Opus review (must make UI self-checking work) → Sonnet executor.
- D15 The owner agrees to automatic UI screenshots on GitHub (V00).

## 3. Corrections to earlier statements (assistant, checked against sources)
- The **Minimalist routine is a circuit**: Walking Lunges → Push-ups → Rows → Plank Shoulder Taps, 2–6 circuits, little rest, no fixed rep range. Its rule for making an exercise harder is "about 3 sets of 8–10 reps first". Source: the r/bodyweightfitness wiki Minimalist routine (derived from u/m092's post), summarised at aegirlab.com/guides/minimalist-routine.html. The earlier chat claim that it uses pairs with 90 s rest was wrong.
- The **RR** (redditbwf.github.io/wiki/recommended_routine.html):
  - **Warm-up**, 5–10 min: shoulder band warm-up 5–10, squat sky reaches 5–10, wrist prep 10+, dead bugs 30 s, arch hangs 10 (after negative pull-ups), support hold 30 s (after negative dips), easier squat 10, easier hinge 10.
  - **Pairs**, 3 × 5–8 reps with 90 s between every set (up to 3 min allowed): pull-up + squat; dip + hinge; row + push-up.
  - **Core triplet**, 3 × 8–12 reps with 60 s rests: anti-extension, anti-rotation, extension.
  - **Progression rule:** when all 3 sets reach 8 reps, move to the next exercise and restart at 3 × 5. Holds go 10–30 s; move on at 3 × 30 s.
  - **Tempo** 10X0, sets stop one rep short of failure, 3 sessions a week.
  - It recommends **barbells** for the squat and hinge if available.
- Reproduce structure and numbers, but **write our own instruction text and credit both sources** in the app and in `THIRD_PARTY_NOTICES.md`. The reviewer checks the wiki's licence before shipping any copied wording.

## 4. Decisions answered in round 3 (2026-10-08) and what is still open
- **O1 Formats: ANSWERED.**
  - Three formats: Circuit · Pairs · Straight sets.
  - "Stretch during recovery" vs "plain rest timer" is a single switch that applies to **every** format, Circuit included.
  - Default: stretch on (today's behaviour); the RR preset sets plain rest.
- **O2 Progression rules: ANSWERED (accepted).** The rule is a property of the routine:
  1. **App levels** (default, today's): 5 targets; up after 3 qualifying sessions spread over 7+ days.
  2. **Rep range:** sets × from–to (RR preset 3 × 5–8, holds 10–30 s); a session where every set reaches the top moves you to the next exercise, starting again at the bottom of the range.
  3. **Custom:** the same as option 2, with sets, range and qualifying sessions editable.
  
  D4 (the global switch off) freezes all three.
- **O3 Extra equipment: ANSWERED.**
  - "Barbells" meant the owner's **dumbbells** (2 × 2.5 kg today, weight editable in the profile).
  - **Barbell** (with or without rack) and **weighted vest** are new profile options.
  - Content: dumbbell variants where a light load genuinely helps (e.g. goblet squat, dumbbell RDL / single-leg RDL, weighted glute bridge, rows); weight tracks like the kettlebell (V26); the RR's barbell squat and hinge alternatives shown when a barbell is in the profile; the weighted vest as a load option for push-ups, pull-ups, dips and squats.
  - Everything beyond bodyweight also feeds the suggestion card (R29). Bodyweight-only must stay complete (D9).
- **O4 Ready-made routines: PARTLY OPEN.** The RR and Minimalist are confirmed. The owner asked which other well-known routines could be added. Candidates the assistant proposed (owner to pick; the reviewer verifies each source and its licence; structure and numbers only, our own wording, credited):
  - the r/bodyweightfitness **beginner / "simple" routine** (free wiki);
  - **Antranik's beginner bodyweight routine** (free, antranik.org);
  - **Convict Conditioning "Big Six"**: its programmes (e.g. "New Blood") as structure only; the book text and illustrations are copyrighted;
  - formats rather than routines: **EMOM / AMRAP / Tabata** and **Grease the Groove**.
- **O5 Kettlebell weight steps: ANSWERED (accepted).** Ladder 8 / 12 / 16 / 20 / 24 kg, shown from the owner's current bell upward.
- **O6 Agent push access: ANSWERED, a token.** The owner creates a fine-grained GitHub token (only repo Nilscost/Calisthenics-app; Contents read/write, Actions read/write, Metadata read). He stores it in `~/.hermes/.env` as `GITHUB_TOKEN` and forwards it with `terminal.docker_forward_env`. The agent checks it exists with `[ -n "$GITHUB_TOKEN" ]`, pushes with `git push https://x-access-token:$GITHUB_TOKEN@github.com/Nilscost/Calisthenics-app.git main`, and must never print it or store it in git config or files.

## 5. Target experience
### 5.1 Visual system (R4, R5, R9, R16, R18, R19, R25) — do this once, use it everywhere
- **One drawing pipeline:** `tools/gen_demo_clips_v2.py` produces three outputs per exercise and stretch:
  - the clip (`assets/demos/<id>.mp4`);
  - a still **thumbnail** for icons (`assets/thumbs/<id>.webp`, a key frame with the body cropped, transparent or theme-neutral background);
  - the muscle data the body figure needs.
- **No mini-animations in lists** (battery, visual noise). Only the focused item animates (Preview card when expanded, swap sheet when an item is selected, the workout screen).
- **Body figure** (`ui/components/BodyMap.kt`): front and back silhouette; primary muscles in the accent colour, secondary muscles light. It is drawn in Compose from vector paths (no bitmaps), driven by the same `Muscle` enum. Muscle names stay in the screen-reader description and are shown on tap.
- **Every clip gets its own pose.** A variant never reuses another exercise's pose (K2). Quality bar per clip:
  - the movement reads within one loop;
  - the anatomy is plausible: contact points right, feet off the floor where they should be, the bar or box in the right place;
  - what makes this variant different is visible.
- Review: contact sheets plus actually watching frames in sequence. The reviewer lists rejected clips in `docs/evidence/clips-v3-review.md`.
- **Stretch clips** are rebuilt in 3D like the strength clips.
- **Full-screen workout video:** the clip fills the screen behind the controls. Controls sit on a dark band (scrim) at the top and bottom; text contrast stays ≥ 4.5:1 over any frame; portrait only.

### 5.2 Questionnaire (R1, R3, R6, R7)
- Header: three labelled parts, Goal · Equipment · Level, with the current part highlighted. The step dots are gone on the final page.
- Goal page: three big buttons (Body focus / Skill / Ready-made routine), then the choices inside the picked type.
- Level pages:
  - show the full progression of the family (all exercises, each with thumbnail and 5 levels);
  - picking a harder exercise without its prerequisites stays allowed (self-assessment);
  - the "I don't know" path stays.
- "You are ready": a summary per family with the stars for the chosen levels (D8), and no dots.

### 5.3 Train and Preview (R8–R15)
- Train: the "Profile" label above the chips. The goal control has the same three types as the questionnaire. New **Format** control (Circuit / Pairs / Straight sets; O1). The rounds stepper becomes "sets" in Pairs and Straight sets.
- Preview card: thumbnail, name, level stepper with target (D3), body figure, swap button, "−" to remove.
- Between cards: a slim **break row** showing the stretch (thumbnail and name) or the rest time; tap it to change. A "+" between blocks adds an exercise or a stretch.
- Swap sheet: D1.
- Full timeline: the same card format, one section per round.
- **Detailed edit:** per-round stretches (D6) and free rep numbers (D3).
- Saving: "Save routine" asks for a name; editing a saved routine asks "Save as new" or "Update" (D5).

### 5.4 Workout screen (R16, R17, R20)
- Full-screen clip with an overlay: round, name, target, ring timer, and Pause · Skip · Done.
- Logger during the recovery: the same layout as the work screen (− · big number · +, Done below), plus Too hard · Too easy · Pain chips.
- K1 fixed: the clip always matches the current block.

### 5.5 End screen and History (R21, R22)
- End screen: a per-round editor per exercise, with "Too easy" next to "Too hard" and "Pain".
- History detail: every round's value is editable. Corrections are saved as new revisions; the history itself is append-only.

### 5.6 Progress (R23–R27, R29)
- **Vertical** tree: easier at the top, harder below, scrolls vertically; chains side by side only when they branch.
- Two tabs at the top: **Types** (today's families) and **Skills** (each skill with the exercises that lead to it, and which are done).
- Lower exercises in a chain you have passed show 5 stars (D8).
- Kettlebell exercise sheet: a weight track (e.g. 12 kg ★★★★★ → 16 kg 🔒 → 20 kg 🔒); locked steps say what's missing; same-weight harder exercises are unaffected.
- **Suggestion card** (R29): at most one at a time, dismissible, never blocking. Examples: "You've mastered the band row — a low bar or rings opens inverted rows", "A 16 kg kettlebell opens the next weight step", "Dip bars or parallettes open the dip progression".

## 6. Content
- **C-A Stretches:** from 7 to about 18, covering hips (pigeon, 90/90, frog), hamstrings, quads (couch stretch), chest, lats, shoulders (sleeper / cross-body), wrists (flexor / extensor), spine (child's pose, thoracic rotation), calves and ankles. Each needs areas, seconds, a unilateral flag, cues, cautions, a 3D clip and a thumbnail.
- **C-B Warm-up library** (D12): RR warm-up items — shoulder band warm-up (or the stick/towel alternative), squat sky reaches, wrist prep, dead bugs, arch hangs, support hold — plus "easier squat" and "easier hinge" as references to existing exercises. Warm-up is a block type (`WARMUP` already exists in `BlockType`), and any workout can switch it on.
- **C-C RR content** needed to be faithful:
  - dip progression: parallel-bar support hold, negative dips, dips — needs parallettes / dip bars, which fixes K3; with no equipment, chair dips are the documented alternative;
  - hinge progression in RR style (the RR's main path uses bands and Nordic curls); the reviewer checks the current RR hinge list at the source;
  - anti-rotation (band anti-rotation hold) and extension (reverse hyperextension) progressions.
  
  Every new exercise gets: equipment, muscles, cautions, tiers, clip, thumbnail.
- **C-D Minimalist content:** walking lunges, plank shoulder taps, and rows (already present via the row chain).
- **C-E Gaps from the earlier review:**
  - pull-up path without a band (negatives, scapular pulls, dead hang);
  - tuck L-sit, or L-sit on parallettes, before the floor L-sit;
  - squat order (assisted pistol before shrimp, check sources);
  - a middle step before the Full Back Bridge.
- All new numbers are DRAFT; regenerate `docs/review/draft-numbers.md` with `tools/gen_review_table.py`.

## 7. UI self-check capability — task V00 (must work before any UI task)
Facts:
- The Hermes sandbox is aarch64 Linux without KVM: no Android emulator runs there.
- Robolectric cannot render pixels on linux-aarch64.
- The sandbox **can** reach GitHub, and GitHub's ubuntu runners support the Android emulator with KVM.

Build:
1. A workflow `.github/workflows/ui-screens.yml`, triggered on push to `main` and by hand:
   - build the debug APK;
   - start an emulator with `reactivecircus/android-emulator-runner` (API 34 x86_64, 1080×2400, density 420 ≈ S21);
   - run scripted flows;
   - save screenshots.
2. Flows with **Maestro** (YAML, one flow per checklist area A–D from the owner's test plan) or instrumented Compose tests. The reviewer chooses and records why. Each flow:
   - starts from `pm clear` (fresh install) or a seeded state (a backup file imported through the app's restore flow, so History and Progress have data);
   - takes named screenshots per step.
3. Variants for every flow: light, dark (`cmd uimode night yes`) and font scale 1.3 (`settings put system font_scale 1.3`).
4. Output:
   - an Actions artifact;
   - a commit to an orphan branch `ui-shots` under `<short-sha>/<flow>/<step>-<variant>.png`, plus `index.md`, using the workflow's own `GITHUB_TOKEN` with `contents: write`.
   
   The agent reads them via `git fetch origin ui-shots` or `raw.githubusercontent.com`.
5. A **UI review step** is required in every UI task: fetch the screenshots for the commit, look at each one with vision, and write `docs/evidence/ui/<task>-review.md`. That file holds a 1–5 score per screen, defects found, and fixed vs open.
6. Acceptance for V00 (reviewer):
   - the agent itself triggered a run and fetched the PNGs;
   - it opened at least one in vision;
   - it detected a planted defect (e.g. a temporarily mis-coloured button on a branch).
   
   Only then hand over to the executor.

Limits that stay: haptics, sound, voice cues, screen-off behaviour and real-workout feel need the owner's phone.

## 8. Tasks (ordered; one commit each; tests first for logic)
Columns: id · what · needs · start files · automated check · screenshot check (V00) · owner phone check.

| # | Task | Needs | Start files | Automated check | Screenshot check | Owner check |
|---|---|---|---|---|---|---|
| V00 | UI screenshot pipeline (§7) | token (O6) | `.github/workflows/`, new `maestro/` or `app/src/androidTest/` | workflow green; PNGs on `ui-shots` | planted defect found | — |
| V01 | Fix K1: `DemoPlayer` reloads on file change (`key(file)` + `update`) | — | `ui/screens/DemoClips.kt` | Robolectric: changing the block changes the video path | stretch → exercise transition frames | clip changes every block |
| V02 | "Too easy" rating + D7 suggestion (domain) | — | `feedback/Feedback.kt`, `ProgressionEngine.kt` | unit: 2× too easy → suggestion event, no automatic change | — | — |
| V03 | Logger redesign (R17) incl. Too easy chip | V02 | `ui/session/SessionScreen.kt` | UI test: − / + / Done send the right actions | logger frames light/dark/1.3 | logging feels like the work screen |
| V04 | Per-round editing on the end screen + History (R21, R22) | V02 | `ui/session/FeedbackEditor.kt`, `ui/history/*`, `history/SessionDetail.kt`, `HistoryDao` | unit + UI: per-round correction = new revision; evidence uses the corrected values | history detail | correct round 2 only |
| V05 | **Release R1** (bugs + logging) | V01–V04 | — | full verify | full flow set | owner phone test R1 |
| V06 | Clip pipeline v3: own pose per exercise; thumbnails; full review; rejected list (§5.1) | — | `tools/gen_demo_clips_v2.py` → v3 | script asserts: every id has its own pose (no shared function object without a declared variant difference), a thumbnail, size limits | contact sheets + frame sequences reviewed | clips understandable |
| V07 | Stretch library to about 18 (C-A) with 3D clips | V06 | `tools/gen_starter_catalog.py` | catalog tests: count, areas, cautions | stretch sheets | — |
| V08 | Body figure component (R5) | — | new `ui/components/BodyMap.kt` | unit: every `Muscle` maps to a region; a11y description lists muscles | body map light/dark | readable at card size |
| V09 | Thumbnails + body map in every list (Preview, swap sheet, questionnaire, Progress, History) (R4, R9, R25) | V06, V08 | `ui/*` | string/a11y tests | all screens | looks "graphical" |
| V10 | Full-screen workout video with overlay controls (R16) | V06 | `ui/session/SessionScreen.kt` | UI test: controls reachable, 48 dp, contrast tokens | workout frames on several clips | readable from 2 m |
| V11 | **Release R2** (visual system) | V06–V10 | | full verify | full flow set | owner phone test R2 |
| V12 | Questionnaire: three goal types, header Goal·Equipment·Level, full progression on level pages, ready page without dots and with stars (R1, R3, R6, R7, D2, D8) | V09 | `ui/onboarding/*`, `goals/Goals.kt`, `intake/Intake.kt` | onboarding tests updated | every questionnaire page | — |
| V13 | Progress: vertical tree + Types/Skills tabs + stars below current (R23, R24, R26, D8) | V09 | `tree/SkillTree.kt`, `ui/progress/*` | layout unit tests (vertical, no overlaps, deterministic); star test | trees per tab | tree readable |
| V14 | Global "Automatic progression" switch (D4) | — | `SettingsScreen.kt`, planner/engine | unit: off → no tier change and no successor change from evidence | settings | — |
| V15 | **Release R3** | V12–V14 | | | | owner phone test R3 |
| V16 | Preview editor: level stepper (D3), remove "−", "+" between blocks, break rows, swap sheet D1, "Profile" label (R8) | V09 | `ui/train/*`, `planner/TrainPlan.kt`, `routine/Routine.kt` | unit: stepper moves within levels; add/remove yields valid plans; swap lists the full chain | preview + swap sheet | — |
| V17 | Detailed edit: per-round stretches, free reps that don't count (D3, D6); timeline in card format per round (R12) | V16 | same + `feedback` (non-counting flag) | unit: a free number is excluded from progression evidence | detailed edit | — |
| V18 | Saved routines: save / save as new / update; listed under Ready-made routines (R13, D5) | V16 | `ui/screens/RoutineStore.kt` → domain routine store, backup | unit: save, update keeps history; backup round-trip | | save + reuse |
| V19 | Workout formats in the planner: Circuit / Pairs / Straight sets, with the stretch-or-rest switch across all formats (D11, O1) | — | `planner/Planner.kt`, `model/Model.kt` (append-only fields) | planner tests per format: block order, rest lengths, durations | timeline per format | — |
| V20 | Workout screen + History for sets ("Set 2 of 3"), plain-rest timer | V19 | `ui/session/*`, `history/*` | reducer/UI tests | workout frames per format | Pairs workout feels right |
| V21 | Progression rule per routine: App levels / Rep range / Custom (O2) | V19 | `progression/*` | engine tests per rule incl. holds (10–30 s) | rule picker | — |
| V22 | Ready-made routines RR + Minimalist, plus any O4 picks (D10, C-B, C-C, C-D) | V19–V21 | catalog + new `routine/Presets.kt` | tests: structure matches §3 (pairs, sets, rests, warm-up, core triplet; Minimalist circuit 2–6) | routine preview | runs like the RR |
| V23 | Warm-up block for any workout (D12, C-B) | V22 | planner, Train/Preview | planner: warm-up first, its duration counted | | — |
| V24 | **Release R4** (editor, formats, routines) | V16–V23 | | | | owner phone test R4 |
| V25 | Content gaps C-E + parallettes/dip content (C-C; fixes K3/R2) | V06 | catalog, clips | no-dead-end + equipment tests | new clips | — |
| V26 | Weight tracks for kettlebell and dumbbells; weighted-vest load option (R27, O3, O5) | V25 | `load/Load.kt`, `ui/progress/*` | unit: locked steps from the profile; same-weight successors unaffected | progress sheet | — |
| V27 | Suggestion card (R29, D9); barbell + vest profile options; dumbbell/barbell content (O3) | V26 | new `progress/Suggestions.kt` | unit: max 1, dismissible, never blocks; the bodyweight-only profile always has a complete plan | progress | — |
| V28 | **Release R5**; regenerate the review table; STATUS/README | all | | full verify + CI green | full flow set | owner phone test R5 |

## 9. Releases and stop points
Each release ends with:
- the full verify;
- CI green;
- a full screenshot set reviewed by the agent;
- an APK `Apps/builds/app-debug-R<n>.apk` with a raised `versionCode`;
- a short owner checklist in `docs/evidence/owner-check-r<n>.md`.

**Stop and wait for the owner** after each release, and when an open decision (§4) blocks the next task. Tasks that don't depend on it may continue.

## 10. Rules (carried over + new)
- Everything in `docs/11-implementer-handoff.md` "Rules" still applies, with these updates:
  - commit identity `Nils Costanzo <51085848+Nilscost@users.noreply.github.com>`;
  - the repo is **public** on GitHub, so never commit personal data, keys or the owner's Gmail.
- Toolchain (sandbox): `source /root/toolchain/env.sh` before any build. `bash /root/toolchain/check.sh` shows what's present. The build tree is `/workspace/review` (a clone of the Mac repo; reset it to Mac HEAD before starting). Verify command:
  `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline`. Read the real exit code.
- Data model: append fields last with defaults. Room: additive migrations only. Old plans and evidence must still decode.
- Every UI task includes the V00 screenshot review; "tests pass" alone is not done for UI.
- Never claim device testing; never approve owner gates; content stays DRAFT.

## 11. Reviewer changes
(The Opus reviewer fills this in: tasks split, merged or reordered, and why; V00 evidence; questions raised to the owner.)
