# 10 — UX/UI overhaul plan (owner feedback 2026-10-05)

Status: **APPROVED TO START (owner answers 2026-10-05 in §2). Owner OK on the §4.1 chains given in chat 2026-10-05, "OK for the moment" (provisional; draft content, may be changed).**
Implementer: Sonnet (or any model). Read this whole file, then `docs/07-decisions-and-blockers.md`, then inspect the repo. Do not rely on chat history.

---

## 0. Owner feedback (verbatim summary, numbered for traceability)

| ID | Owner said |
|---|---|
| F1 | "Today's workout" and "S'entraîner" must be one thing. |
| F2 | Duration and the rounds/mode controls side by side. |
| F3 | Goal as a dropdown, not chips. |
| F4 | Timed rounds = a toggle between *rep-based* and *timed*. |
| F5 | Home/Travel shown as **profiles**, with a **+** that opens a profile-creation page (add equipment). |
| F6 | One clear **Start** button. |
| F7 | Starting level = **one-time questionnaire at install**, per **family**, not per exercise. Pick the variation you do (e.g. feet-elevated push-up), your reps and rounds. "I don't know" → choose easy / medium / hard. |
| F8 | After setting today's parameters: show a **draft of the workout**, then start. |
| F9 | Animation: the coloured part must be **the muscle being worked**, not a random limb. |
| F10 | A button to log **how many reps** you did **during** the workout. |
| F11 | Stretch on → **no separate rest**. |
| F12 | Clips need **perspective** so the movement is understandable. |
| F13 | Block duration must match the hold: a 15 s plank lasts 15 s, not 45 s. |
| F14 | Feedback **during** the session, not at the end. |
| F15 | Library: band row and superman hold have **no progression**. |
| F16 | Graphical view → a real **tree with icons** and **up to 5 stars** per mastered exercise. |
| F17 | Remove "Enregistrer l'entraînement", "À faire", "S'entraîner" (the old forked app's screens). |
| F18 | Overall: think much harder about UX/UI; efficient, beautiful, highest standard. |

---

## 1. Review: what is wrong today (found by reading the code, not on device)

1. **Two apps in one.** `HomeScreen.kt` shows 4 new buttons ("Today's workout (new)", "Exercise library (new)", "Weekly history (new)", "Backup (new)") **plus** the 5 old fork buttons (To Do, Record, Workout, Create, dashboard). The old ones run a different data model (`exercises`/`training_records`) that the new planner ignores. → F1, F17.
2. **Today screen is one long scroll** of mixed controls: length text, goal chips, skill tree, rounds chips, timed chip, high-bar chip, slider, profile chips, focus chips, stretch checkbox, level button, recovery card, the full block list, swap chips for every slot, two save buttons, Start. No hierarchy, no preview step. → F2–F6, F8.
3. **"High bar" is a loose toggle** instead of equipment in a profile; profiles are hard-coded (`SeedProfiles`), not editable. → F5.
4. **Onboarding lists all 43 exercises** with a text field + slider each. → F7.
5. **Hold blocks are padded:** `hold_tiers` sets `workWindowSeconds = target + 25`. In timed mode every work block is 60 s regardless of a hold target. → F13.
6. **Rest vs stretch:** with stretch on, recovery *is* a stretch block, but the 5 s `TRANSITION` blocks are labelled "Rest" in `SessionScreen`/`WorkoutSessionService.label()`, and side-switch/transition naming makes it look like rest still exists. → F11.
7. **Feedback only after the workout** (`FeedbackForm` on the completed screen); the session screen has no logging control. → F10, F14.
8. **Clips:** 2-D side-view stick figures; the blue colour marks the *arms* in every clip, not the working muscle. No depth. → F9, F12.
9. **Dead-end families:** `row` (band row), `superman`, `dead-bug`, `side-plank`, `deadlift` have one exercise and no `nextVariationIds`. → F15.
10. **Skill tree** is a vertical list of cards for one goal chain; Library is a flat text list grouped by raw family id (`"pushup"`, `"hspu"`). Stars exist in the domain (`VariationProgress.earnedStars()`) but are not shown. → F16.
11. **Visual design:** no design system on new screens (raw `Text`, default `Card`, emoji icons, inconsistent spacing, English-only strings while the phone runs French). → F18.

---

## 2. Owner decisions (answered 2026-10-05)

| ID | Decision |
|---|---|
| Q1 | "10-12 sets of 4 rounds" = **reps per set** + **rounds**. |
| Q2 | "I don't know" offers **Easy / Normal / Hard**. Calibration anchor (owner): **Normal = the family's middle exercise at its middle step** — e.g. standard push-up, step 3 = 8 reps. **Easy = one exercise easier, step 3. Hard = one exercise harder, step 3.** Each family declares its anchor in the catalog (`anchorVariationId`): push-up → standard push-up; squat → split squat; pull → band-assisted pull-up (no bar: band row); core → plank; hinge → glute bridge; shoulders → pike push-up. If no easier/harder exercise exists, use step 1 / step 5 of the anchor. Hard skills (planche, lever, muscle-up, pistol, HSPU, one-arm push-up) are never reachable via "I don't know". |
| Q3 | **English only for now.** Still put every string in `res/values/strings.xml` (no hard-coded text) so French can be added later. |
| Q4 | **Holds last exactly the target (+3 s get-ready), then the session moves straight on.** Timed mode does not pad a hold to 60 s. The next block is the recovery — **and if stretch is on, the stretch IS the recovery: there is never a separate rest block or "Rest" cue when stretch is on.** |
| Q5 | Bottom tabs **Train · Progress · History · Settings**; Backup inside Settings. |
| Q6 | **Delete the old fork features** (To Do, Record, Create, old Workout/S'entraîner, programs, intervals, community share, CSV, old backup, old dashboard) to make the build clean. Keep Room migrations so existing installs upgrade; old tables may stay unused (no destructive migration). Delete code before redesign work (U01b) so later tasks don't touch dead screens. |
| Q7 | Clips: 3/4 view, solid grey body, working muscles orange-red (default accepted). |
| Q8 | **Name: later.** Use the placeholder "Calisthenics" in one string resource (`app_name`). |

## 3. Target experience (the contract)

### 3.1 Navigation (F1, F17, Q5)
- One activity, Compose `NavHost` (androidx.navigation.compose, already allowed: check the gradle cache offline first; if absent, keep the existing `Screen` sealed class but make the new screens the only routes).
- **Bottom bar:** Train · Progress · History · Settings. No old fork buttons anywhere.
- Start destination: Onboarding if not completed (`OnboardingStore.done == false`), else Train.

### 3.2 First run: onboarding questionnaire (F7, Q1, Q2) — one time
Pager, one question per page, progress dots, Back/Next, can't skip required pages:
1. **Welcome**: what the app does, all on the phone, no account.
2. **Goal** (dropdown, same list as `Goals.all`, grouped "Skills" / "Body focus").
3. **Equipment**: create the first profile (see 3.4). Defaults to "Home".
4. **One page per family** in this order: Push-up, Squat, Pull/Row, Core (plank line), Hinge (bridge), Shoulders (pike/HSPU, only if a push-up answer ≥ standard). For each:
   - Horizontal carousel of the family's **entry-level exercises** (icon + name + 1-line description; tap = play the clip in a bottom sheet).
   - "I do this" → two steppers: **reps per set** (or seconds for holds) and **rounds**.
   - **"I don't know"** → three cards Easy / Normal / Hard (Q2 anchor mapping; each card names the exercise and target it will start you at, e.g. "Normal: Standard push-up, 8 reps").
   - Mapping: `Intake.suggestStartingStep(tiers, reps)` picks the step; rounds are stored as a preference (`Preferences.defaultRounds`).
5. **Workout style**: rep-based / timed toggle, default rounds, stretch on/off.
6. **Summary**: list of starting exercises with stars at 0; "Start training".
- Store `OnboardingStore.done = true` and the levels (existing `LevelStore` / `UserAction.SelfAssessment`).
- Re-run later from Settings → "Redo the starting questionnaire" (overwrites levels after a confirm dialog).

### 3.3 Train tab = today's setup (F2–F6)
Single screen, no scrolling on a 6.2" phone at default font size:
```
[ Goal ▾  Handstand push-up           ]      ← ExposedDropdownMenuBox
[ Profile:  (Home) (Travel)  (+) ]           ← FilterChips; + opens Profile editor
┌ Duration ───────────┐ ┌ Style ───────────┐
│  − 4 rounds +       │ │ (Reps)(Timed)    │  ← SegmentedButton
│  ≈ 44 min           │ │                  │
└─────────────────────┘ └──────────────────┘
[ Focus: Full · Upper · Lower · Core ]       ← SegmentedButton (single or multi)
[ Stretch between exercises   ( on ) ]       ← Switch
                                   [ Preview workout → ]   ← primary FilledButton, full width
```
- Rounds stepper replaces the minutes slider. The estimated minutes are computed live from the planner and shown under it. (Rounds are the length mechanism — owner decision.)
- An unfinished-session banner sits at the top when a checkpoint exists.

### 3.4 Profiles (F5)
- `ProfileStore` (JSON in app-private prefs) replaces hard-coded `SeedProfiles` at runtime; seeds are created on first run.
- Profile editor: name, then an equipment checklist with icons: Pull-up bar, **High bar (feet clear)**, Low bar / sturdy table (for rows), Resistance band, Kettlebell (weight field), Dumbbells (weight field), Chair (stable), Wall, Mat, Parallettes/dip bars (future). Delete profile (not the last one).
- Remove the loose "I have a high bar" toggle and `ModeStore.highBar`.

### 3.5 Preview = the draft workout (F8)
- Screen after "Preview workout": header "4 rounds · 44 min · Reps".
- One card per exercise in the circuit (not per block): icon, name, target ("10 reps" / "15 s"), muscles worked chips, swap button (bottom sheet with same-pattern alternatives available in this profile; today only, "Keep for next time" checkbox).
- Collapsible "Full timeline" for the block-by-block view.
- Warnings (draft content, cautions for hard skills) as one compact info row.
- Sticky bottom **Start** button (F6).

### 3.6 Session screen (F10, F11, F13, F14)
- **Work block:** big clip (top 45 %), exercise name, target, ring timer around the time (or for reps: a count-up with the 60 s ring in timed mode), "Next: …".
- **Logging during the session (F10/F14):** when a work block ends (or the user taps **Done**), the following rest/stretch screen shows a compact logger *at the top*:
  `Push-up  − 10 +  reps   [Too hard] [Pain]` — prefilled with the target, saved immediately as a feedback revision (existing `reviseFeedback(..., actualReps)`). If the user doesn't touch it, it's "as planned". The per-round value is stored; evidence uses the lowest round (existing rule).
- **Rest vs stretch (F11):** with stretch on, the recovery block *is* the stretch: show "Stretch: Chest doorway · 0:55" with its clip. No block or voice cue says "Rest". The 5 s transition is shown as "Get ready: <next exercise>", never "Rest".
- **Hold duration (F13):** a hold block lasts `target + 3 s` setup countdown, not `target + 25`. Rep blocks keep their work window (rep mode) or 60 s (timed mode).
- Controls bottom: Pause · Skip · Done (early completion) · End workout (in overflow, with confirm).
- End screen: short summary (time, rounds, exercises), stars earned/progress events, "Edit what I logged" link. No long form.

### 3.7 Progress tab = skill tree (F15, F16)
- One horizontal-scrollable **tree per family**, drawn with Compose `Canvas` + nodes:
  - Node = circular icon (family pictogram) + short name + **0–5 stars** (`earnedStars()`, filled = earned).
  - Edges = curved lines from `nextVariationIds` and `prerequisite` relations; multiple prerequisites (e.g. HSPU needs negative + hold) shown as merging lines.
  - States: mastered (filled, accent), current (ring + "training now"), available (outlined), locked (greyed, lock badge), equipment-locked (greyed, equipment icon badge).
  - Tap a node → bottom sheet: clip, how-to, cautions, the 5 steps with targets, equipment, sources.
- Family picker at the top (chips with icons). The selected goal's chain is highlighted.
- The Library becomes this screen (no separate text list); stretches in a "Stretches" family tab.

### 3.8 History tab
- Week calendar strip + list of sessions with duration, rounds, exercise count; tap → session detail with per-round logged reps, editable (new revision).

### 3.9 Settings tab
- Profiles, workout defaults, redo questionnaire, voice cues, backup/export/restore, licences, privacy policy, about.

---

## 4. Content changes

### 4.1 New progressions (F15) — draft, flagged unreviewed (owner OK 2026-10-05, provisional)
| Family | Chain (easier → harder) | Equipment |
|---|---|---|
| Row / horizontal pull | band row → inverted row bent knees → inverted row straight legs → feet-elevated inverted row → archer row | band; then low bar or sturdy table |
| Back extension | superman hold → arch hold arms overhead (Y) → arch rocks | none |
| Anti-rotation core | dead bug → hollow-hold chain hand-off (dead bug top → hollow hold) | none |
| Side plank | side plank → side plank with top-leg raise → Copenhagen side plank (bench/chair) | chair for the last |
| Hinge (kettlebell) | KB deadlift → KB single-leg RDL → KB swing | kettlebell |
Every family must end in either a hard skill or a `nextVariationIds` hand-off; extend `StarterCatalogTest.plankAndEveryChainTopLeadsSomewhere` to fail on any dead end that is not an explicit "top" (`isTopOfLadder` list).

### 4.2 Muscles (F9)
- Add `primaryMuscles: List<Muscle>` and `secondaryMuscles` to `ExerciseVariation` (append last with default `emptyList()`), enum `Muscle { CHEST, FRONT_DELTS, SIDE_DELTS, REAR_DELTS, TRICEPS, BICEPS, FOREARMS, LATS, UPPER_BACK, LOWER_BACK, ABS, OBLIQUES, GLUTES, QUADS, HAMSTRINGS, CALVES, HIP_FLEXORS, ADDUCTORS }`.
- Fill for every exercise in `gen_starter_catalog.py`; test: every strength exercise has ≥1 primary muscle.
- Used by clips (colour), preview chips, and the tree sheet.

### 4.3 Hold timing (F13, Q4)
- `hold_tiers`: `workWindowSeconds = target + 3`. Timed mode: hold blocks use `target + 3` too, then move straight to the recovery (stretch if on). No padding to 60 s.

### 4.4 Family anchors (Q2)
- Add `anchorVariationId` per family (catalog `families` list or a map in the generator) and a test that each onboarding family has one and that Normal for push-up resolves to standard push-up step 3 = 8 reps.
- Re-check `StarterPlanTest` length bands after the change (4 rounds 40–50 min) and update the numbers table `docs/review/draft-numbers.md`.

---

## 5. Clips v2 (F9, F12, Q7)
- Rewrite `tools/gen_demo_clips.py` → `tools/gen_demo_clips_v2.py` (keep v1 until v2 passes review):
  - 3-D skeleton: joints as (x, y, z); keyframes per exercise (reuse v1 poses as the sagittal plane, add z offsets for left/right limbs).
  - Camera: 30° yaw, 15° pitch orthographic projection; painter's algorithm by depth.
  - Body drawn as capsules (thick rounded limbs, torso as a tapered capsule, head sphere), light grey with a darker far side; floor grid + soft shadow ellipse.
  - Muscles: each `Muscle` maps to segments/regions (e.g. CHEST → front of upper torso, TRICEPS → back of upper arm). Primary = saturated orange-red, secondary = light orange. **Nothing else coloured.**
  - Equipment drawn in 3-D (bar, wall, chair, band).
  - 2 s loop, 24 fps, 480×360, H.264, < 120 KB each.
- Review: contact sheet + vision check per batch of 8 for (a) correct movement, (b) only the listed muscles coloured, (c) readable depth. Record review notes in `docs/evidence/clips-v2-review.md`.

---

## 6. Design system (F18)
- Material 3 theme in `ui/theme/` for the new screens: dynamic colour on Android 12+ (S21 has it), fallback palette (primary deep teal, accent orange used for muscles/stars), dark theme.
- Typography scale only from `MaterialTheme.typography`; no raw `fontSize` except the session timer (`displayLarge`).
- Spacing tokens 4/8/12/16/24; corner radius 16 on cards, 28 on primary buttons.
- Icons: Material Symbols (bundled `material-icons-extended` is large; prefer vector drawables copied for the ~20 icons used, licence Apache-2.0 → add to `THIRD_PARTY_NOTICES.md`). Family pictograms as vector drawables generated from the clip skeleton (one key pose, single colour).
- Motion: shared-axis transitions between Train → Preview → Session; haptic tick on stepper +/−.
- Accessibility: every icon has a contentDescription; touch targets ≥ 48 dp; works at font scale 1.3 without clipping (test with `@Preview(fontScale = 1.3f)`).
- Strings: all in `res/values/strings.xml`, English only for now (Q3).

---

## 7. Ordered tasks for the implementer

Conventions (mandatory): see skill `calisthenics-app-build-loop` / repo `docs/02-coding-specification.md`. One task at a time, tests first, full verify command, evidence log `docs/evidence/u<NN>-verify-<date>.log`, commit as "Nils Costanzo <51085848+Nilscost@users.noreply.github.com>", APK `Apps/builds/app-debug-U<NN>.apk`. Data-model fields are appended last with defaults. No destructive DB migration.

| # | Task | Needs | Files (start points) | Automated check | Owner check on phone |
|---|---|---|---|---|---|
| U01 | Navigation shell: bottom bar (Train/Progress/History/Settings), remove old fork entries from Home/nav | Q5 |
| U01b | Delete old fork code (screens, viewmodels, DAOs only they use, their strings/resources, unused dependencies). Keep `AppDatabase` entities + migrations so upgrades never lose data | Q6 | `MainActivity.kt`, new `ui/nav/AppNav.kt`, `HomeScreen.kt` (retire) | Compose UI test (Robolectric is cached: verify offline) that the 4 tabs exist and "S'entraîner"/"À faire"/"Enregistrer" strings do not | Open app: only 4 tabs |
| U02 | Design system: theme, spacing, English strings in resources | Q3 | `ui/theme/*`, `res/values*/strings.xml` | lint: no hard-coded strings in new screens | Looks consistent light/dark |
| U03 | Domain: hold timing `target+3`; timed mode never pads holds; with stretch on the recovery is the stretch and no block/cue is "Rest"; transitions read "Get ready: <next>" | Q4 | `gen_starter_catalog.py`, `Planner.kt`, `WorkoutSessionService.label()`, `SessionScreen.kt` | planner tests: 15 s plank block = 18 s; timed hold leftover added to recovery; no cue text "Rest" when stretchOn | 15 s plank lasts 15 s |
| U04 | Profiles: `ProfileStore`, editor screen, equipment checklist incl. high bar / low bar; delete high-bar toggle | — | new `ProfileStore.kt`, `ProfileEditScreen.kt`, `Equipment.kt` | unit: CRUD + muscle-up locked without high bar | Create "Gym" profile, see muscle-up unlock |
| U05 | Train tab redesign per §3.3 | U01–U04 | `TodayScreen.kt` → `TrainScreen.kt` | UI test: Preview button enabled; rounds stepper updates minutes | One screen, no scroll |
| U06 | Preview screen per §3.5 with swap sheet | U05 | new `PreviewScreen.kt` | UI test: swap changes card, Start launches service intent | Draft looks right, Start works |
| U07 | Session redesign + in-session logger per §3.6 | U03, U06 | `SessionScreen.kt`, `WorkoutSessionService.kt`, `HistoryDao` | reducer/unit: per-round reps stored; untouched = assumed met; evidence uses lowest round | Log reps during rest |
| U08 | Onboarding questionnaire per §3.2 | Q1, Q2, U04, family anchors in catalog | `OnboardingScreen.kt` rewrite, new `OnboardingStore.kt`, `Intake.kt` (`levelFor(family, answer)`) | unit: Normal for push-up = standard push-up step 3 (8 reps); Easy/Hard = neighbour exercise step 3; edge families fall back to step 1/5; per-family answer → tier | Fresh install flows through 6–9 pages |
| U09 | Content: new chains (§4.1) + muscles (§4.2) | owner OK on §4.1 list | `gen_starter_catalog.py`, `Model.kt`, tests | no dead-end test; every strength exercise has primary muscles | Band row now leads somewhere |
| U10 | Progress tab tree per §3.7 with stars | U09 | new `ui/progress/SkillTreeCanvas.kt`, retire `LibraryScreen.kt`, `SkillTree.kt` | unit: layout positions deterministic, no overlapping nodes; stars from `earnedStars()` | Tree readable, stars visible |
| U11 | Clips v2 per §5 (all exercises) | Q7, U09 | `tools/gen_demo_clips_v2.py`, `assets/demos/*.mp4` | script asserts every catalog id has a pose and every primary muscle maps to a region; vision review log | Movement understandable, right muscles glow |
| U12 | History tab + Settings tab per §3.8–3.9 | U01 | `HistoryScreen.kt`, new `SettingsScreen.kt` | UI test: redo-questionnaire confirm | Edit a logged value |
| U13 | Polish pass: motion, haptics, a11y at font 1.3, dark mode screenshots | all | — | Robolectric screenshot set saved to `docs/evidence/ui/` | Owner rates each screen 1–5 |

Gate after U07 and after U13: **owner device test** (the owner approves; a model never does).

---

## 8. Verification matrix (short)
| Feedback | Tasks | Test |
|---|---|---|
| F1, F17 | U01, U01b | nav UI test; grep shows no old screen classes |
| F2–F6 | U04, U05 | Train UI test + owner |
| F7 | U08 | Intake mapping unit tests |
| F8 | U06 | Preview UI test |
| F9, F12 | U09, U11 | muscle-region assertion + vision review |
| F10, F14 | U07 | per-round feedback unit test |
| F11, F13 | U03 | planner/cue tests |
| F15 | U09 | no-dead-end test |
| F16 | U10 | tree layout test |
| F18 | U02, U13 | lint + screenshots + owner rating |

## 9. Known limits
- No device testing from the sandbox; Robolectric screenshots approximate the phone.
- All catalog numbers remain the assistant's DRAFT until the owner reviews them.
- Clips stay generated drawings; real AI video can replace any `<id>.mp4` later.
