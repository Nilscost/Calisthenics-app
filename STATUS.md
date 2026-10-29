# Actual project status

## iPhone and web port resumed (2026-10-29)

Work continues on `codex/kmp-port` under [the multiplatform plan](docs/41-multiplatform-port-plan.md), with Terra executors and coordinator review. Phase 0's toolchain upgrade, 616 domain tests, 290 app tests, 10 connected database tests and seeded install-over comparison passed. The latest full screenshot run passed dark, enlarged text and connected tests, but failed light Train/Preview after missed sheet-opening taps. The flow corrections and a setup-hook failure fix are being verified; Phase 0 is not approved or tagged, and Phases 1–5 remain pending. The iPhone deliverable is an installable web app; native iOS remains optional Phase 6. No new release APK or web URL is ready for owner testing.

[Execution evidence](docs/evidence/kmp-port.md) · [Latest screenshot review](docs/evidence/ui/KMP-phase0-latest-review.md).

## Clip fixes installed (2026-10-26 · no new APK)

Eleven demonstrations were fixed at source, rendered in one isolated batch and installed.

- **The seven doc-40 candidates:**
  - Flag kick press, installed as rendered.
  - Toes to bar, now with the legs together.
  - Heel hook, with a readable hook-up.
  - Dragon-flag negative, with a tucked recovery.
  - Wall walk, with alternating planted hand steps.
  - Pilates Saw, with level arms and no slide.
  - Russian dip, kept as installed.
- **Five installed clips:**
  - Archer push-up, reframed with the feet raised above the floor.
  - Straddle press, with a wider straddle and a front-oblique view.
  - Windshield wiper, with a readable sweep.
  - Pistol squat, with the free leg held up and full extension.
  - Ring muscle-up, with a camera view that keeps the straps off the torso.

All eleven passed native geometry and the install gates. 594 domain and 265 app tests pass. Owner real-speed form review and phone checks remain open.
[Evidence](docs/evidence/clip-fixes-2026-10-26.md).

## Burpee foot contact and fluidity review (2026-10-25 · no new APK)

The revised review candidate uses raised heels/toe support, a gradual feet-in
approach and grounded settling crouch, then a continuous rise into jump preload.
242 Python tests, native body/floor/framing and actual foot-skin contact checks
pass. The other 532 trajectories and all installed app build inputs are unchanged.
Owner naturalness/form and phone checks remain open.
[Evidence](docs/evidence/burpee-fluidity-2026-10-25.md).

## R8.5 session editing and video candidate (2026-10-25 · 0.11.5-r8, 19)

Changing the exercise count rebuilds the automatic selection without shared-route
alternatives; manual choices survive other navigation/settings until count changes.
Deleting the only exercise leaves Add/Undo available; final-row deletion preserves
remaining work. The exact six-to-eight transition agrees across both Start paths.
539 domain, 256 app and 238 Python tests pass, alongside lint, packaging and content
checks. Five eligible updated videos/thumbnails are installed; the rigid ring hand
experiment is isolated because full arm/wrist checks still fail. Phone audio/music,
manual-lock/timeout, upgrade/recovery, screenshots and form acceptance remain open.
[Release evidence](docs/evidence/r8.5-implementation-2026-10-25.md) ·
[Phone checks](docs/evidence/owner-check-r8.5.md) ·
[Video procedure](docs/39-video-generation-and-review-procedure.md).

## Depth Jump return and moving-ring review (2026-10-21 · no new APK)

The Depth Jump review candidate now has a higher gravity-shaped rebound, planted
turn steps and a direct sequential step back onto the box. The moving-ring
candidate couples wrists/hands and apparatus, with explicit false-grip identity
and native mesh checks. Its procedural proximal finger lengths and skin/wood
gap remain unresolved; **ring app promotion is blocked** despite collision
clearance. 205 Python tests and all-frame native geometry checks pass.
The other 531 trajectories and all 1,066 installed media files are unchanged.
Both renders and full desktop playback/control checks across the eight-clip
gallery completed on 2026-10-22. Owner, phone-size and expert form acceptance
remain open.
[Depth evidence](docs/evidence/depth-return-refinement-2026-10-21.md) ·
[Ring evidence](docs/evidence/ring-articulation-refinement-2026-10-21.md).

## Push-Up Burpee knee correction (2026-10-21 · review candidate, no new APK)

The burpee review candidate now uses sagittal knee bends, lower bilateral hop-back
feet, smooth planted push-up transitions, bilateral feet-in travel and a short
jump with extended takeoff. 179 Python tests, all-frame native geometry, full
decode and actual desktop playback/control checks pass. Three filmed references
were reviewed and extracted; numerical comparisons remain inconclusive because
of camera, variation and cycle differences. The updated eight-video gallery
preserves earlier candidates and feedback. All installed app media is unchanged;
phone readability and owner/form approval remain open.
[Evidence](docs/evidence/burpee-refinement-2026-10-21.md).

## Depth Jump realism and eight-candidate review (2026-10-21 · no new APK)

Depth Jump now has a 0.50-second gravity-shaped rebound, coordinated arm drive,
neutral wrists and proper leg extension at takeoff. Only this video and thumbnail
are replaced. The other 532 trajectories and videos are unchanged by this follow-up.
169 Python tests, actual mesh/floor/framing checks, full decode and complete desktop
playback/replay of all eight review clips pass. The eight-render batch is complete;
its other clips are review candidates. Three reference comparisons remain
inconclusive; actual phone and expert form approval remain open.
[Evidence and review priorities](docs/evidence/depth-jump-refinement-2026-10-21.md).


## Scripted video refinement and render batch (2026-10-21 · review candidates)

Six source motions are refined: ring wrist/hand contact, actual depth-jump flight
and rebound, controlled burpee phases, explicit pistol phases, progressive
roll-over curl and shin-relative full-wiper feet. The other 527 trajectories and
all 533 installed videos and thumbnails are unchanged from this batch's baseline.
162 Python tests and ten scoped contracts pass; 523 trajectories remain unscoped.
Scripts do not certify exercise form or phone behavior.

The first eight-item isolated native Blender batch was started, then stopped
after native mesh checks exposed burpee foot-floor penetration and a smaller
pre-existing roll-over chest-floor contact issue. The corrected batch passed native preflight and subsequently completed. All 21,335,472
body vertices fit the final app frame and pass the 2 mm floor tolerance. It includes those six candidates plus accepted
Pullover and Straddle Press regression controls. Its manifest is the authority
for current completion/failure, and nothing is automatically installed. Full
encoded review, phone-size readability and owner/form acceptance remain open.
All eight candidates have completed encode/decode and desktop playback mechanics
checks. The newer Depth Jump correction above supersedes the original batch draft.
[Lessons and batch workflow](docs/38-video-generation-lessons-and-batch.md).


## Straddle Press foot direction (2026-10-21 · source assets, no new APK)

Feet now turn continuously with the Straddle Press and finish pointed along the
legs. A full descent completes an eight-second loop. 105 Python tests, actual
Blender foot/floor/orientation and framing checks, full decoding, installation
and structural content validation pass. Desktop full playback/replay and review
controls also pass. The other 532 trajectories and installed
videos are unchanged, including the Pullover that the owner called “Much better.”
Five scoped motion contracts pass; 528 remain unscoped. Wrist/hand realism is now
an explicit review step with compact articulation reports; the ring wrist
concern remains open. No phone or expert form approval is claimed.
[Evidence](docs/evidence/straddle-foot-refinement-2026-10-21.md).

## Bar pullover smoothing (2026-10-20 · source assets, no new APK)

The owner-reported abrupt Bar Pullover is replaced with an eleven-second loop,
continuous rotation, a deliberate slower reverse and wrists rolling around the
bar. 94 Python tests pass, including frozen old-motion and mid-turn pause/pop
regressions. All 264 frames decode; actual Blender mesh/bar and framing checks
pass. The other 532 source trajectories and installed videos are unchanged.
Four movement contracts pass; 529 remain unscoped. Phone playback and expert
form acceptance remain open. [Evidence](docs/evidence/bar-pullover-refinement-2026-10-20.md).

## Ring muscle-up refinement (2026-10-20 · source assets, no new APK)

Owner feedback addressed with visible wooden rings, suspension straps, an isolated
grip close-up and a smaller, slower crossover with controlled return. Whole-arm
and finger/ring volume checks cover all 144 frames. 85 Python tests, installation,
content checks and desktop review playback controls pass. Only this clip and its
thumbnail are replaced; the other 532 installed videos are unchanged. Three
motion contracts pass; 530 remain unscoped. Expert form and phone acceptance
remain open. [Evidence](docs/evidence/ring-muscle-up-refinement-2026-10-20.md).

## Catalog screening and eight-video review set (2026-10-20)

A read-only pass builds all 533 trajectories and decodes 48,310 installed frames;
zero media-format/decode errors, 111 clips with heuristic speed/loop review flags.
Only two exercise-specific contracts exist; 531 remain unscoped. Three bounded
Luna lanes reviewed source and selected encoded frames for a diverse eight-clip
owner review set. 77 Python tests and desktop review-control QA pass. Optional
same-pose appearance concepts are previews only. All 533 MP4 hashes are unchanged;
no source media replacement, APK or phone/form approval. [Workflow/evidence](docs/37-catalog-motion-screening.md).

## Smooth mannequin heads (2026-10-20 · source assets, no new APK)

The renderer now uses featureless heads, removing nose/chin geometry while
retaining head articulation. Pull-up and chin-up clips are regenerated with
exactly the same motion data and approved pull-up pacing. Archer assets remain
unchanged. [Evidence](docs/evidence/smooth-heads-2026-10-20.md).

## Pull-up pacing correction (2026-10-20 · source assets, no new APK)

A further owner-reported mid-movement surge is corrected with steady central
vertical travel and smooth endpoint ramps, retaining the grip and authored pose
path. 72 Python tests pass; a frozen previous trajectory fails the new surge
gates. Central vertical speed ratio is 1.00036 in both directions. The approved
archer assets are unchanged. [Evidence](docs/evidence/pullup-steady-pace-2026-10-20.md).
Phone playback acceptance remains open; no generation credits spent.

## Pull-up follow-up (2026-10-20 · source assets, no new APK)

The pull-up now moves continuously through intermediate keys and wraps modeled
fingers/thumbs around the bar with conservative collision clearance. The approved
archer clip and thumbnail are unchanged. 69 Python tests pass; Blender mesh,
encoded-output and installation evidence are recorded in the
[follow-up report](docs/evidence/pullup-smooth-grip-2026-10-20.md).
Phone playback/readability acceptance remains open. No generation credits spent.

## Motion-quality pilot (2026-10-20 · source assets, no new APK)

The archer push-up and pull-up now have exercise-specific motion gates, corrected trajectories and a continuous torso mesh. Archer: alternating sides, assisting elbow about 170°, about 19 cm chest travel toward each hand and planted anchors. Pull-up: controlled mild arch, about 17° torso inclination at the top and shoulder initiation, preserving the grip inset. Exact old poses and disguised normal push-ups are rejected by regression tests. 64 Python tests, real hand/torso/framing checks and media validation pass. Six real reference analyses replay from cache; approximate camera comparisons remain screening evidence with material residual differences. Only two exercises have motion contracts; 531 are explicitly unscoped. Phone readability/form acceptance remains open. [Evidence](docs/evidence/motion-quality-pilot-2026-10-20.md) · [Workflow](docs/36-motion-quality-pilot.md). No generation credits spent.

## Blender grip and reference-comparison pilot (2026-10-20)

Implements readable hand direction for the chin-up/pull-up pilot: anatomical thumb rotation, distinct palm/back surfaces, corrected underhand bar contact and synchronized enlarged grip views. Local automated body/hand extraction and timing-aligned comparisons ran against five actual YouTube videos, three per exercise. Camera differences and variable hand visibility keep numerical results inconclusive; no automated form approval or phone-readability claim. Both 72-frame loops and thumbnails are installed in source assets after decoded-output review; 51 Python tests and content validation pass. Existing R8.4 APK is unchanged. No generation credits spent. [Implementation and reference evidence](docs/evidence/blender-grip-pilot-2026-10-20.md) · [Reproduction](tools/blender/FORM_REVIEW.md).

## R8.4 phone-feedback candidate (2026-10-20 · 0.11.4-r8, versionCode 18)

Implements document 35 with Sol integration and three bounded Luna lanes, preserving the substantial dirty/untracked R8.3 baseline. Flow/Rest/Stretch controls are synchronized on Train and both Preview forms; real compatible stretches, safe explained fallback, preparation, dormant rest overlays, saved edits and both Start paths agree. Ordinary contexts use App levels through scoped, versioned migration; saved/preset Rep range and Custom remain intact. Explicit full higher-level performance adopts N as current and success 1/3; two more qualifying distinct days within 14 days permit advancement. Compatible untried same-rung choices may start at demonstrated N−1 without transferred achievements. Historical replay retains its original semantics; new plans snapshot effective policies. Warning beeps use media-classified playback, per-sample/load/play diagnostics and saved app gain independently of voice, without audio-focus requests. An Activity lifecycle binding keeps visible unfinished RUNNING/PAUSED sessions awake and never wakes/unlocks the phone.

Fresh local checkout plus the final working-source overlay: **539 domain and 252 app tests pass**, data unit-test task has no sources; lint, debug APK and content validation pass. Preflight and migration SQL checks pass. All **1,444** recorded source/build/test/content/flow files match the shared workspace. No Room schema change. [Implementation report](docs/evidence/r8.4-implementation-2026-10-20.md) · [build/source/certificate record](docs/evidence/r8.4-build-2026-10-20.json) · [full verification log](docs/evidence/r8.4-fresh-verify-2026-10-20.log).

APK: `/Users/nils/Documents/hermes/Apps/builds/app-debug-R8.4.apk` (also `app/build/outputs/apk/debug/app-debug-R8.4.apk`); SHA-256 `b5c4dd20b1bb66648bb0b3527263b045da1e960651f9e51d90428980d7cfd851`. Certificate matches the actual R8.3 and R7.2 APKs; R8.3 is retained unchanged. No commit, push, publication or CI run occurred.

**Phone audibility/music/Bluetooth, manual-lock/timeout, actual upgrade/recovery acceptance, screenshots and owner acceptance remain open.** No connected device/emulator/Maestro was available; automated sink/window tests are not physical verification. [Owner checklist](docs/evidence/owner-check-r8.4.md) · [visual gate](docs/evidence/ui/R8.4-review.md). Training content remains DRAFT. Document 34’s old “implementation not started” wording does not describe this workspace.

## R8.3 implementation candidate (2026-10-20 · 0.11.3-r8, versionCode 17)

The approved feedback in `docs/34-user-feedback-executor-handoff.md` is implemented: app-only voice/beep volume (30% default) without audio-focus requests; Train's exclusive Flow / Rest / Stretch modes; persisted 1–30 s preparation with upcoming silent demonstrations; personal five-level targets and above-level-5 rep/hold extensions with level-5 evidence; static Pilates/Conditioning cards; ultramarine/blanc cassé Light default with yellow achievement stars and saved Dark support. Routines, drafts, backups and recovery carry the new fields; no Room schema change was needed. Historical achievements remain, while revised personal targets start compatible future streaks.

Fresh local checkout plus the recorded working-source overlay: **513 domain and 229 app tests pass**, data unit-test task has no sources, lint, debug APK and content validation pass (`docs/evidence/r8.3-fresh-verify-2026-10-20.log`). Preflight and migration SQL checks pass. Build/source/certificate details: `docs/evidence/r8.3-build-2026-10-20.json`; implementation and changed-file manifest: `docs/evidence/r8.3-implementation-2026-10-20.md`. Local APK: `/Users/nils/Documents/hermes/Apps/builds/app-debug-R8.3.apk`, SHA-256 `cec3959ff4256c6c3cc5d7e7ca945003369a71de164edf8ee1fac1eaca7b281a`; its signing certificate matches the actual R7.2 and R8.2 APKs.

**Screenshot execution/review, phone music/Bluetooth checks and owner acceptance remain open.** No emulator/Maestro or compatible native screenshot renderer is available locally; no earlier screenshots substitute for this candidate (`docs/evidence/ui/R8.3-review.md`). No CI, push, publication or deployment occurred. Content stays DRAFT. Walkthrough: [owner-check-r8.3.md](docs/evidence/owner-check-r8.3.md).

## R8.2 release (2026-10-18 · 0.11.2-r8, versionCode 16)

R8.2 (`docs/33-r8.2-plan.md`, entries `R8.2-T01` to `T05` in `docs/evidence/progress-notes.md`): the Power tab is gone and its exercises sit in Squat and Push with their routes (gates unchanged); Prehab and the warm-up items are one Warm-up category by body area, and the automatic warm-up adds 2 to 4 items matched to the session within its usual time; Train has one Include row (Warm-up, Conditioning, Cool-down) with a 5 to 10 minute conditioning finisher, shown as blocks in the Preview; the duplicate exercises-per-round tile is removed. **Nothing was tested on a phone; content stays DRAFT.** Fresh clone of `ce593e8` exit 0 (495 domain, 211 app tests, lint, assembleDebug, content); GitHub verify [39160885835](https://github.com/Nilscost/Calisthenics-app/actions/runs/39160885835) and ui-screens [39160885932](https://github.com/Nilscost/Calisthenics-app/actions/runs/39160885932) success (51 of 51, reviewed in `docs/evidence/ui/R8.2-review.md`). APK `Apps/builds/app-debug-R8.2.apk`, SHA-256 `8309858a1603faeca268ffcddf80b74124070ec8cf15bed9fd74f99de8ced886`, certificate identical to R7.2 (`docs/evidence/r8.2-build-2026-10-18.json`). Walk-through: [owner-check-r8.2.md](docs/evidence/owner-check-r8.2.md).

## R8.1 release (2026-10-17 · 0.11.1-r8, versionCode 15)

R8.1 answers the owner's phone feedback on R8 (`docs/31-r8.1-plan.md`, entries `R8.1-T01` to `T13` in `docs/evidence/progress-notes.md`). Engine: the planner no longer prescribes below the rung a person has shown (rotation, route position, missing equipment); one exercise per logical route per workout; logged reps stay the actual reps (carry-over to the next set, skip keeps a typed number); a level rises after 3 qualifying sessions within 14 days (same-rung alternates count together). Preview: Straight sets / Pairs / Circuit and sets restored, edits survive navigation, Flow as an option for every workout, each between-set stretch at most twice. Train: one dropdown with Programs and My routines; programs open in the Preview as a starting point and save as new routines; merged Army PRT (about 37 min) and Pilates mat (about 33 min) sessions; no draft bubbles; equipment in movement-area order with icons; "1 round". Progress: search above By type / By skill, no suggestions, same-rung alternates drawn beside their rung, and (owner bug R8.1-T10b) every by-type category shows its exercises. **Nothing was tested on a phone; content stays DRAFT.** Verification: Docker fresh clone of `187521f` exit 0 (483 domain, 208 app tests, lint, assembleDebug, content), GitHub verify [39072032905](https://github.com/Nilscost/Calisthenics-app/actions/runs/39072032905) and ui-screens [39072032888](https://github.com/Nilscost/Calisthenics-app/actions/runs/39072032888) success (51 of 51 flow results, reviewed in `docs/evidence/ui/R8.1-review.md`). APK `Apps/builds/app-debug-R8.1.apk`, SHA-256 `b59c6b6f48fdf248b21aecd60cabc529ea0b2ae7516825f4f1771e2d02d1e656`, signing certificate identical to R7.2 (`docs/evidence/r8.1-build-2026-10-17.json`). Walk-through: [owner-check-r8.1.md](docs/evidence/owner-check-r8.1.md).

## R8 final release (2026-10-17 · 0.11.0-r8, versionCode 14)

The owner's final scope is implemented: all 25 unclear videos were redone, fixed programs sit under Routine, and Extras with its four pool switches is removed. Specialist exercises remain manually addable in Preview and available in fixed routines; Flow stays. The APK in `Apps/builds/app-debug-R8.apk` is the final code-14 build.

R8 adds 379 library entries, eight fixed programs and 53 explicit progression routes while keeping the default workout pool small. Main-path mastery controls advancement; route alternatives rotate across completed workouts and keep independent results. Untried comparable bodyweight alternatives receive a conservative starting estimate using the associated main rung's history, across both rep targets and matching timed holds. Equipment, movement, side structure, existing history and manual-target safeguards remain in force.

The owner's workout feedback is incorporated: avoid duplicate progressions in generated workouts, slower voice, named sides, five seconds between sides of holds, editable Preview rests and the rest/stretch card beside +. Programs under Routine, aliases, manual specialist additions and compact Progress routes are included. All 533 catalog entries have bundled clip and thumbnail files and there are no demonstration holds: the 379-pose Blender batch was audited and encoded, six camera views were corrected, and the 25 previously unclear demonstrations were re-rendered, reviewed frame by frame on decoded MP4s and installed (`docs/evidence/clips-r8/repair-final-acceptance-2026-10-17.md`). The mannequin stays schematic; a few clips show subtle actions (scapular push-ups, chin nod, short foot, manual neck resistance).

Verification: 439 domain tests, 191 app tests, lint and assembleDebug pass; Python 41 tests; content and migration checks pass (`docs/evidence/r8-final-verify-2026-10-17.log`). GitHub verify run 38973320301 and ui-screens run 38973320244 on `95fa4fc` pass: 51 of 51 flow results (17 flows, light, dark, font 1.3) and the 9-test connected job; reviews in `docs/evidence/ui/R8-*-review.md`. A fresh Docker clone builds the APK: SHA-256 `cfa9f42c5305e6d3f195614f0d13001a80b52804d56714bc1aa32c1f153982ea`, signing certificate identical to R7.2 (`docs/evidence/r8-build-2026-10-17.json`). **R8 has not been tested on the owner's phone.** Walk-through: [owner-check-r8.md](docs/evidence/owner-check-r8.md). Content remains DRAFT and owner acceptance remains open.

## Current state (2026-10-13, Release R7.2 = 0.10.2-r7, versionCode 12)
R7.2 answers the owner's phone test of 0.10.1-r7 (`docs/21-r7.2-plan.md`, tasks `docs/22-r7.2-executor-handoff.md`, entries `R7.2-T01` to `T11` in `docs/evidence/progress-notes.md`): the Preview round view has one "All sets · N min each" header instead of set tabs (F1); the swap sheet's "Other types" scrolls into view and shows the exercise's own tab first in a "Same family" box (F2, F3); breaks are outlined cards with the swap icon and the + on its own line (F4); cool-down stretches can be removed (long press or x) and added (+), saved with routines and backups (F5); Resume is the gold filled button on one line (F6); **one "Too easy" now offers "Move on to ..." (two levels, or the next exercise from level 4; skipped levels count as done; changes D7/B9, owner decision O1)** (F7); units next to logged numbers ("30 s", "8 x") (F12); dates under the progression chart's dots (F8). F9 (planche lean note) is by design; only the check wording changed. The shoulder pass-through and Pallof clips (F10, F11) are re-posed, rendered on the owner's Mac and installed (R7.2-T09b): **rendered, owner to judge**; from the contact sheets the pass-through's behind-the-back phase reads poorly (`docs/evidence/clips-r7.2/review.md`). **Nothing was tested on a phone; owner gates stay open.** Verify: fresh-clone full verify exit 0 (`docs/evidence/r7.2-t11-verify-2026-10-13.log`); ui-screens run [38480367268](https://github.com/Nilscost/Calisthenics-app/actions/runs/38480367268) on `f1f7993`: 39 of 39 PASS (light, dark, font 1.3), review in `docs/evidence/ui/R7.2-review.md`. Owner walk-through: `docs/evidence/owner-check-r7.2.md`. APK: `Apps/builds/app-debug-R7.2.apk`.

## Previous state (2026-10-13, Release R7.1 = 0.10.1-r7, versionCode 11)
R7 answers the owner's phone feedback on 0.9.0-r6 (`docs/18-r7-plan.md`, tasks `docs/19-r7-executor-handoff.md`, tasks R7-T01 to T14): hold targets show and start at their top, DONE works in breaks, readable block captions and WARM-UP / COOL-DOWN headers, rep logging with "Too easy" on the exercise screen, a questionnaire answer above the top step completes the ladder, "Too easy" at the top level suggests the next exercise, a Progressions chart with the numbers actually done, a clip cache that follows the app version, an "Exercises per round" stepper (the default aims for at least 30 minutes when the equipment-limited exercise pool is large enough), skill sessions built from the exercises that make the skill possible, the full timeline as the Preview editor with a category add sheet and a level stepper that steps the chain under a rep range, Progress with an Edit mode, tappable Needs and Unlocks, History that opens the exercise and the weighted exercises inside their movement tabs, deep stretches between sets with their own time, Jefferson curl and German hang with light-red cautions, and a cool-down switch (stretching or timed). **Nothing was tested on a phone.** All exercise content stays an unreviewed DRAFT. The owner walks through `docs/evidence/owner-check-r7.md` (it maps every B and D item to a step); the screenshot review and current verification status are in `docs/evidence/ui/R7-review.md`; per-task details, **NOT done** items and choices not in the plan in `docs/evidence/progress-notes.md` (entries `R7-T01` to `R7-T14`). Content: 119 strength exercises, 25 stretches and 11 warm-up items; numbers in `docs/review/draft-numbers.md`. Jefferson curl and German hang have their own clips now (R7.1). The final owner report (`docs/evidence/r7-owner-report.md`) records implementation limits: Preview number/stretch edits are per set, while level/swap/removal affect all sets; equipment-limited plans can be shorter than 30 minutes. **R7.1 (0.10.1-r7, versionCode 11)** is the same R7 feature set plus the Blender clip library: all 154 clips were reviewed by eye on contact sheets (frames at 0, 25, 50 and 75 %) and all 154 are installed in `app/src/main/assets/demos` and `thumbs` (largest 27 KB, cap 120 KB, loop seams checked); the `ClipAlias` stand-ins and the T12 `SHARED_POSES` placeholders are gone. The version bump makes the clip cache refresh (clear the app cache if a clip still looks old). Known clip limits are listed in `docs/evidence/clips-bakeoff/blender-review.md` (weighted variants without a vest prop, sleeper pelvis 8 cm off the floor, thoracic rotates the whole trunk, wrist flexor and extensor look alike, German hang shows the shallow foot-supported version). Verify exit 0 (`docs/evidence/r71-verify-2026-10-13.log`); ui-screens run [38442012588](https://github.com/Nilscost/Calisthenics-app/actions/runs/38442012588) on `fc183ad`: all 10 flows PASS in light, dark and font 1.3 (30 of 30), connected job passed; review in `docs/evidence/ui/R7-review.md`. **Nothing was tested on a phone; owner gates stay open.** APK: `Apps/builds/app-debug-R7.1.apk`.

## Previous state (2026-10-10, version 2: Release R6 = 0.9.0-r6)
Version 2 (owner feedback R1-R29, plan `docs/12-v2-plan.md`, tasks `docs/14-executor-handoff.md`, library batch `docs/archive/15-library-expansion-tasks.md`) is done: R1 (V00-V04b), R2 (V06-V10, design direction B), R3 (V12-V14), V16-V23 (preview editor, formats, rules, saved routines, ready-made routines, warm-up; **R4 was folded into R5**), V21b/V25 (RR content, C-E gaps), V26 weight tracks, V27 suggestion card and extra equipment, V27b History, V08c fonts (Barlow Condensed, IBM Plex Sans), R5, and the library batch L01-L13 (tier profiles, push-up, pull-up, rows, single-leg squat, hinge, L-sit, core, back bridge, dips, 24 stretches, the Starting To Stretch session, RAMP warm-up, lower legs), then R6. **Nothing was tested on a phone.** All exercise content stays an unreviewed DRAFT. The owner walks through `docs/evidence/owner-check-final.md` (it merges R1-R6; per-release lists `owner-check-r1..r6.md`); screenshot reviews are in `docs/evidence/ui/` (`R5-review.md`, `R6-review.md`, `V12-V27-review.md`, `L*-review.md`); per-task details in `docs/evidence/progress-notes.md`. Content: 117 strength exercises, 24 stretches and 11 warm-up items; numbers in `docs/review/draft-numbers.md`; clips in `docs/evidence/archive/clips-v3-review.md`.

## Previous state (2026-10-10, R5 = 0.8.0-r5)
R5 held everything up to L10 and the fonts (its checklist: `owner-check-r5.md`).

## Previous state (2026-10-09, R3 = 0.6.0-r3)
Version 2 follows `docs/12-v2-plan.md` (owner feedback R1-R29) with the task list in `docs/14-executor-handoff.md`. Done so far (R1: V00-V04b; R2: V06-V10 visual system, design direction B of `docs/17-ui-direction-b.md`): V00 UI screenshot pipeline (GitHub Actions + Maestro, light/dark/font 1.3), V00b debug seed, V01 clip reloads per block, V02 "too easy" rule in the domain, V03 logger redesign, V04a Room 24 and per-round corrections, V04b per-round editors, V06 clip pipeline v3 and own poses, V07 18 stretches, V08 body figure, V08b design system B, V09 thumbnails, V10 workout screen; R3: V12 questionnaire and Train, V13 Progress tree and detail, V14 automatic-progression switch. Owner checklists: `docs/evidence/archive/owner-check-r3.md` (R2: `-r2.md`, R1: `-r1.md`). **Nothing was tested on a phone.** All exercise content stays an unreviewed DRAFT. Details: `docs/evidence/progress-notes.md`; owner checklist: `docs/evidence/archive/owner-check-r1.md`; screenshot reviews: `docs/evidence/ui/`.

## Previous state (2026-10-05, UX overhaul U01-U13)
The app is now one product with four tabs (Train · Progress · History · Settings): first-run questionnaire, equipment profiles, one-screen Train setup, draft Preview, redesigned live session with in-workout rep logging, skill tree with stars, history with corrections, settings, 3/4-view demo clips with muscle colouring, 10 new exercises. The old fork screens are deleted (data tables kept). Everything was verified with the offline Gradle gate (see `docs/evidence/uNN-verify-*.log`) and Robolectric UI tests; **nothing was tested on a phone**. The owner device test is the open gate; all exercise content stays an unreviewed DRAFT. Details per task: `docs/evidence/progress-notes.md`; plan: `docs/10-ux-overhaul-plan.md`; owner checklist: `docs/evidence/archive/owner-check-u13.md`.

Older history (first build T00-T24, gates, 2026-09-25 to 2026-10-01): `docs/archive/status-history-2026-09.md`.
