# R7 owner report — 2026-10-13

Release: **0.10.1-r7 (R7.1)**, versionCode **11**; same R7 features as 0.10.0-r7 plus the Blender clips (the version bump refreshes the clip cache). Training content remains **DRAFT**. No phone test has been performed or approved.

| Work | What changed | Verified | NOT done / limits | Choices outside the plan |
|---|---|---|---|---|
| T01–T03 | Top hold targets; work-screen rep logger; DONE on breaks; clearer captions/headers. | Domain/UI tests and full offline verify passed. | Phone sound, haptics, voice and screen-off behavior remain owner checks. | Too hard / Pain remain on the end screen as decided. |
| T04–T07 | Above-top intake completes a ladder; Too easy can suggest a successor; corrected charts; version/size clip cache. | Unit tests and full verify passed. | Exactly-top intake keeps four stars; questionnaire above-top is not covered by Maestro. | Strictly above follows the explicit plank 60/50 s examples; counters cycle in pairs; chart as-planned values use range top. |
| T08–T09 | Exercises-per-round control; skill-specific plans and supporting chains. | Planning/UI tests and full verify passed. | Equipment-limited pools can stop before 30 minutes; hand-kept filler swaps are not persisted; Train settings themselves are not backed up. | Supporting skill chains are curated DRAFT lists; saved routines carry the new count. |
| T10 | Full timeline; category add sheet; chain stepper under range rules. | Preview tests passed. | Level/swap/removal still affect all sets for a slot; only number/stretch edits are per set (D4 limitation). | Existing slot-based planner editing retained for those three controls. |
| T11 | Manual completed levels; linked prerequisites/unlocks; detail stack; History links; weighted branches in movement tabs. | Tree/UI tests passed. | Back from an exercise opened in History returns to the Progress list. | A hand-set level counts itself completed, per the V-up level-3 / three-star example. |
| T12 | Deep stretches between sets; Jefferson curl / German hang with red cautions. | Content structure/rights fields and catalog/planning tests passed. | Source tags are generic and numbers/technique unreviewed. | R7.1: placeholder aliases removed; both have their own clips. |
| T13 | Stretching or timed cool-down, Preview edits and saved-routine fields. | Planning/UI tests passed. | Phone behavior remains unchecked; Train switches are not part of backups. | Timed relax is a block without a catalog item. |
| T14 | Version 0.10.0-r7 / code 10; APK; owner walkthrough and release evidence. | Fresh-clone and continuation full offline verify passed; GitHub verify passed at a372a85 and 89ade24. | Complete screenshot acceptance is recorded in ui/R7-review.md; owner phone gate remains open. | No version bump for the Blender preparation. |
| CI continuation | Portable tooling paths fix the preflight blocker; configurable key-file location. | Preflight, migration checks and GitHub full verify passed. | No AI API generation/spend performed. | Preserves existing workspace layout; allows OPENROUTER_KEY_FILE for other layouts. |
| Installer continuation | Catalog ID acceptance; exact fresh frames; staged capped replacement. | Seven focused tests independently passed in Hermes; first 100 reviewed clips passed dry install with zero problems. | Shipped assets were not installed. | Keep the existing 120 KB cap; reject over-cap files without replacing old media. |
| Blender continuation | Missing curl / supported German hang; wall/door cameras and wall HSPU body line; further stretch corrections. | Geometry and stick previews checked; 109 current renders accepted with recorded limits; first 100 encoded, max 27 KB, with seam outlier frame-sheet review. | Owner render remains active; changed/new poses require rerender and actual visual review. Remaining clips, install, alias removal and updated APK are pending. | Doorway becomes an open frame; static wall handstand preserves contact geometry. All technique remains DRAFT. |

## R7.1: clips and CI (2026-10-13 evening)

| Work | What changed | Verified | NOT done / limits |
|---|---|---|---|
| Clip review | 154 of 154 clips reviewed by eye (`docs/evidence/clips-bakeoff/blender-review.md`); one defect fixed (`stretch-lat-wall`: the wall hid the figure; camera moved, re-rendered). | Every frame set is newer than its `joints.json`. | By-eye review of sampled frames, not video playback; technique is not reviewed by an expert. Known limits are listed in the review. |
| Clip install | 154 clips and thumbnails installed; Jefferson curl and German hang have their own; `ClipAlias` and the placeholder `SHARED_POSES` entries removed. | Encode: max 27 KB (cap 120 KB), seams at most 1.09 of 255; `install_clips.py --dry` and the real install: 0 problems; `r71-verify-2026-10-13.log` exit 0; Python tests 27 OK. | None open. |
| CI | ui-screens split into one job per variant (120-minute timeout); flows `j_warmup` and `b_train_preview` fixed; ui-shots publishing no longer loses a variant. | ui-screens run [38442012588](https://github.com/Nilscost/Calisthenics-app/actions/runs/38442012588) on `fc183ad`: all 10 flows PASS in light, dark and font 1.3 (30 of 30), connected job passed; review in `docs/evidence/ui/R7-review.md`. | Phone behaviour unchecked. |

## Owner check

Use `owner-check-r7.md` for the one phone walkthrough; install `app-debug-R7.1.apk`. If a clip looks old, clear the app cache once (Settings > Apps > the app > Storage > Clear cache). APK: `/Users/nils/Documents/hermes/Apps/builds/app-debug-R7.1.apk` (`app-debug-R7.apk` is the earlier build without the new clips).

## Release gates

The build/test evidence does not approve the owner phone gate or training technique. All training content stays DRAFT.
