# User feedback

## Workout feedback · 2026-10-15

1. **Avoid two exercises from the same progression in one generated workout.** A general-strength workout included two types of push-up. Multiple exercises from the wider push category are fine; two steps or variants of the same push-up progression should be avoided.
2. **Slow the voice by about 25%.**
3. **Announce the working side.** Say left and right when an exercise is split into two sides.
4. **Allow five seconds to change sides for holds.** Add a get-ready interval between left and right holds, such as side planks. Do not shorten either hold.
5. **Critically review progression before implementing new ladder changes.** Write down how the new exercises fit together and consider a round of debate before deciding. Unapproved structural ladder changes are proposals for review.
6. **Repair two stretch clips.** Thoracic rotation has an unnatural shoulder/arm pose resembling a dislocation; check joint anatomy carefully. Rear hand clasp hides the action behind the back; change the viewing angle so the hand position is visible.

### Execution status

- Recorded all six comments. Duplicate-progression prevention, voice speed/side cues and five-second hold-side setup are implemented; regression checks pass apart from the three expected missing-media asset checks.
- Structural progression changes were held for review, then authorized on 2026-10-16 after the owner accepted the review of `progression-comment.md`. See `30-progression-implementation.md` for the implementation and verification checkpoint.
- Rear hand clasp: corrected overhead arm and rear camera, reviewed four Blender frames across the loop, rendered, encoded and installed the replacement clip and thumbnail. Phone verification remains pending.
- Thoracic rotation: the first repair improves the arm position but does not show enough independent upper-back rotation. Candidate saved for further work; its replacement is not installed or accepted.
- No phone verification is claimed.

## Review lens clarified by the owner · 2026-10-15

Keep the exercise library comprehensive while keeping the app uncluttered and the progressions coherent. Library inclusion does not imply automatic prescription, a mandatory progression step, a new top-level screen, or a permanent visible choice. Prefer a small understandable main path, optional alternatives when relevant, and supporting work clearly separated from mastery requirements. Question or omit an addition that cannot justify its role; do not expand a ladder merely to accommodate every catalog entry.

## Integration hold · 2026-10-15

The current R8 catalog and app work remain a draft checkpoint, not approved final inclusion. Pause catalog expansion and app feature work while pose work is completed. Complete and review all 379 exercise pose sources for Blender before discussing which exercises to integrate. Preserve the existing work and behavior during this hold.


## Workout preview rest editing · 2026-10-16

Owner reports that rest durations cannot be changed in the workout preview. Make rest times editable there and carry the chosen durations into the started workout and preview total. This is explicitly requested app work alongside the remaining pose review; exercise-inclusion decisions remain held. Implemented: plain rest has a compact15-second stepper (15–300s), applied across sets and reflected in the preview total and started workout. Rest/stretch swapping and Reset remain available. Four focused preview checks pass after the final compact-row adjustment; the prior progression-name test failure also reproduces on unchanged HEAD. See `docs/evidence/r8-preview-rest-verify-2026-10-16.log`. No phone verification or APK installation is claimed.

Layout clarification: place the rest/stretch card and existing “+” button in one horizontal row. The card fills the available width after reserving the existing button size and gap.

## Progression implementation authorized · 2026-10-16

The owner accepted the review of Opus's `progression-comment.md` and asked to implement it. Use explicit main paths, route-specific alternatives and support, separate RDL and Nordic work, preserve app-achievement prerequisites and existing history, and keep specialist exercise pools off by default. Main-path mastery controls advancement; alternatives retain independent targets and rotate across completed workouts. Difficulty rank remains useful for display ordering and does not establish the next required exercise. This authorization supersedes the progression/app integration hold above; rendered demonstration acceptance and phone verification remain separate.

## Starting targets for alternatives · 2026-10-16

Owner: if feet-elevated push-ups are at12 reps, a first diamond push-up workout should start around8–10 instead of at the bottom. Implement a conservative product starting estimate for an untried, explicitly same-rung alternative:75% of the main exercise's current target, rounded down to an authored rep target and below the alternative's top tier. The example selects8 reps. Require the same family, progression group, movement pattern and side structure, with bodyweight reps only; do not convert holds, weighted work or unrelated core movements. Existing alternative history, explicit manual targets, rep-range rules and discomfort safeguards take precedence. The estimate establishes a starting level; completed tiers are not copied. Subsequent sessions use the alternative's own results. This percentage is a product heuristic, not a researched equivalence between exercises.
