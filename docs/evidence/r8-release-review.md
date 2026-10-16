# R8 final release review · 2026-10-17

Status: final code-14 release (0.11.0-r8, versionCode 14), source commit `95fa4fc`. All 25 repaired demonstrations are installed and there are 0 demonstration holds. The earlier code-13 APK and its 25 withheld demonstrations are superseded. This review does not claim a phone walk-through or owner acceptance. Training content remains DRAFT.

## App behavior

The library contains 533 variations, 53 explicit progression routes and eight fixed programs. The default workout pool stays limited to 12 reviewed routes. Fixed programs live under Routine alongside saved routines. Extras and its four specialist-pool switches are removed; old hidden pool selections are cleared. Specialist exercises remain available through equipment-filtered Preview additions and fixed routines. New specialist flexibility and mobility entries stay out of automatic recovery/cool-down matching. Flow remains available as explicitly accepted by the owner.

Main-path mastery advances the progression. Reviewed alternatives rotate across completed workouts and retain their own results. Rank and catalog order do not establish successors. Existing IDs and history remain intact; RDL hip-hinge work and Nordic knee-flexion work have separate paths. Prerequisites use achievements from exercises in the app, including explicit AND/OR paths and route-specific readiness gates. Only the six reviewed explosive push-up variations inherit the three-rep cap.

Untried comparable alternatives receive a conservative 75% starting estimate, rounded down to an authored tier. Matching progression group, movement, side structure, unloaded kind and target unit are required. Existing alternative results and manual targets take precedence. Rep targets never convert to seconds; loaded exercises are excluded. The estimate completes no achievement tier and cannot advance the main path. Feet-elevated push-ups at 12 reps suggesting 8 diamond push-ups is one instance of this general rule, not a special case.

Generated workouts avoid two exercises from the same progression. Speech rate is 0.75, side-specific cues name left/right, and adjacent sides of a hold have five seconds of setup without shortening either hold. This includes stretch-only sessions and unilateral cool-down holds. Preview plain rests change in 15-second steps from 15 to 300 seconds and carry into the displayed total and started workout. The rest/stretch block fills the row beside the existing +. Draft edits survive the asynchronous history refresh. Large system text gives long exercise names the card width.

## Decisions resolved with the owner

Keep SSD at 10 reps per raise; retain Russian/L-sit dips, shoulder rolls and cautioned kipping pull-ups. Use proper power/conditioning and neck labels, rename Shoulders to Handstand and German hang to Back lever, retain step and box as separate equipment, and include only the eight contracted programs. Keep the Pilates mappings, including Hundred as hollow hold with arm pumps, and Army 4C non-flow. The approximate Pilates duration difference was accepted. Equipment comes from the selected profile; no ownership list is required outside the app.

The CC2 copy lacks photos and the owner authorized internet references. The flag evidence records the available book text and instructor references, plus the limits of exact named-stage photo reconstruction. See `clips-r8/flag-reference-review.md` and the subsequent media dispositions.

## Media revision

The original 379 new clip/thumbnail pairs and six camera corrections were installed at the earlier checkpoint. The owner then requested replacement videos for all 25 unclear demonstrations. The repair track extends the mannequin's independent neck/thoracic controls, finger lifts and foot arch, corrects contact and choreography, and uses closer or opposite-side cameras where needed. The other 508 builder payloads remain byte-equivalent as parsed JSON to their canonical joint caches; the audit is `clips-r8/non-repair-payload-audit-2026-10-16.json`.

The 25 repaired videos were installed in R8-T14 after the coordinator decoded and reviewed every encoded MP4 (`clips-r8/repair-final-acceptance-2026-10-17.md`; installed hashes equal the reviewed encodes, `clips-r8/repair-install-2026-10-16.json`). `DEMONSTRATION_HOLD` is now empty and the runtime and content catalogs were regenerated; the synthetic hold-guard tests remain. Corrected twists cross the leg, brace the opposite elbow and show the rear bind; Saw reaches the opposite little-toe edge while the other arm reaches behind. The rig remains a schematic mannequin with two trunk segments and stylized hands and feet. Accepted limits: the three scapular push-up clips and chin-nod-supine show a real but subtle action, short-foot's arch change is subtle, neck-head-raise-manual shows hand contact rather than visible resistance.

## Verification

- Docker full verify on the final tree: 439 domain tests, 191 app tests, lint and assembleDebug pass; preflight, migration SQL and content structure/rights validation pass (`r8-final-verify-2026-10-17.log`). Python pose/catalog/installer checks: 41 tests pass.
- GitHub `verify` run 38973320301 on `95fa4fc`: success. GitHub `ui-screens` run 38973320244: all 51 flow results PASS (17 flows in light, dark and font 1.3; one light retry on `b_train_preview` passed) and the connected job passed 9 tests with 0 failures.
- Screenshot reviews of all three themes: `ui/R8-light-review.md`, `ui/R8-dark-review.md`, `ui/R8-font13-review.md`.
- Fresh clone of `95fa4fc` built in Docker with no host Java: all checks and assembleDebug exit 0 (`r8-fresh-clone-verify-2026-10-17.log`).
- APK `Apps/builds/app-debug-R8.apk`: package `app.calisthenics.personal`, versionCode 14, versionName 0.11.0-r8, SHA-256 `cfa9f42c5305e6d3f195614f0d13001a80b52804d56714bc1aa32c1f153982ea`, signing certificate SHA-256 `8638d15f7e5327d0a4f3197dad470e70b1425dfcb00a6bfc4bbcdc96a63bd940` (matches R7.2). Record: `r8-build-2026-10-17.json`.

Phone installation, voice/haptics, screen-off timing and workout comfort remain owner checks in `owner-check-r8.md`. Nothing was tested on a phone.
