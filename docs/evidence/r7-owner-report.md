# R7 owner report — 2026-10-13

Release: **0.10.0-r7**, versionCode **10**. Training content remains **DRAFT**. No phone test has been performed or approved.

| Work | What changed | Evidence / remaining limits |
|---|---|---|
| T01–T03: workout and logging | Hold targets use the top of the range; rep logging is on the work screen; DONE skips breaks; stretch captions and warm-up/cool-down headers are clearer. | Domain/UI unit tests and offline verify passed. Haptics, sound, voice cues and screen-off behaviour need the owner phone check. |
| T04–T07: progression and media cache | Above-top questionnaire answers complete a ladder; two Too easy reports can suggest the next exercise; charts apply logged/corrected values; clip cache refreshes by version and asset size. | Unit tests passed. Exactly the top questionnaire answer still starts the top level with four stars, per the explicit 50 s example. |
| T08–T09: session planning | Exercises-per-round control and skill-specific exercise selection; default focus sessions aim for 30 minutes. | Planning/UI tests passed. Equipment-limited pools can stop short of 30 minutes; hand-kept filler swaps are not persisted. Supporting skill chains remain curated DRAFT content. |
| T10: Preview | Full timeline replaces Detailed edit; add sheet filters by category; range-rule steppers move along the exercise chain. | Preview tests passed. Number/stretch edits are per set; level/swap/removal still apply across all sets for that slot. This is a remaining D4 limitation. |
| T11: Progress and History | Manual completion levels, linked prerequisites/unlocks, detail back stack, exercise links from History, weighted branches inside movement tabs. | Tree/UI tests passed. Back after opening an exercise from History returns to the Progress list. |
| T12: content | Deep stretches between sets; Jefferson curl and German hang; light-red cautions. | Structural content validation and tests passed. Clips still use RDL/dead-hang placeholders; Blender replacements are prepared but not installed. Exercise numbers and technique are unreviewed; these two source tags remain generic. |
| T13: cool-down | Stretching or timed relax mode, editable in Preview and saved routines. | Planning/UI tests passed. Train switches are not included in backups; saved routines carry the new fields. Timed relax is a block without a catalog item. |
| T14: release | Version, APK, owner walkthrough and release evidence. | Fresh-clone verify already passed in `r7-t14-verify-2026-10-13.log`; this continuation's full offline verify also passed, logged in `r7-resume-verify-2026-10-13.log`. UI and CI status are recorded in `ui/R7-review.md`. |
| Continuation: CI hygiene | Removed machine-specific paths from clip tooling; key-file lookup preserves the existing workspace layout and accepts `OPENROUTER_KEY_FILE`. | Preflight and migration checks pass locally. No API generation or spend occurred. |
| Continuation: installer | Accepts catalog IDs for new clips; requires exact, fresh frame sequences; staged replacement preserves shipped files on encode failure or over-cap output. | Seven focused tests passed in Hermes and were independently rerun by the coordinator. Dry mode does not change shipped files. |
| Continuation: Blender | Prepared Jefferson curl / supported German hang; corrected six wall/door camera occlusions discovered in the first visual review, plus the folded body line in the wall handstand push-ups. | Pose geometry checks and stick previews reviewed. Owner's render remains in progress; corrected/new IDs require rendering and rendered visual review before encoding/installing. See `clips-bakeoff/blender-review.md`. |

## Owner check

Use `owner-check-r7.md` for the one phone walkthrough, including cache clearing if old clips remain visible. The existing APK is `/Users/nils/Documents/hermes/Apps/builds/app-debug-R7.apk`; no app code or shipped assets changed in this continuation, so the prior APK remains current. A comparison against release commit `71b053e` found no changes in `app`, `domain` or `data`.

## Release gates

The build/test evidence does not approve the owner phone gate or training technique. Blender encoding, seam/size checks, asset installation and placeholder removal remain pending until corrected renders have passed visual review. No version bump was made for the pose work.
