# R8 release review · 2026-10-16

## Scope and result

This review covers the R8 progression catalog, planner and feedback behavior, and the rendered-media release evidence present in the checkout. It is a source and evidence review; it does not claim a real-device walk-through or owner acceptance.

The runtime catalog contains 533 exercise variations, 53 explicit progression routes, and the eight fixed programs. All 533 exercise IDs have explicit route membership. The route data contains 588 memberships because an exercise may be a main step in one route and serve as support or an alternative in another. Membership roles are explicit: 218 MAIN, 66 ALTERNATE, 91 SUPPORT, 34 PROGRAM, and 179 LIBRARY. The default workout pool is restricted to 12 named routes; power, conditioning, preparation, and mobility routes remain deliberate extras. Floor and hanging leg raises share duplicate-prevention semantics.

The data preserves existing exercise IDs and physical family IDs, so logical route splits do not create new history identities. The route model separates progression group, main path, anchor-specific alternatives and support, goal mapping, focus pool, and teaching hold. Eight fixed programs retain their authored item sequences.

## Progression and target safeguards

Progression advances through the authored main path only. Exercise rank and catalog order do not establish successors. Alternatives keep their own targets and achievement history; their results do not advance a host main path. Rotation is limited to alternatives explicitly attached to the current route and anchors at or before its current main step. Support and library entries are not treated as main achievements.

Prerequisite data retains compound AND requirements and explicit OR entry paths. Route-specific readiness gates were authored instead of applying a blanket tier threshold to every new step. Shared exercises can belong to more than one route without forcing one global successor. RDL hinge work remains distinct from Nordic knee-flexion work, and the hip-thrust variation does not become a mandatory successor to a floor bridge.

For an untried, explicitly reviewed same-rung alternative, the planner may suggest a conservative starting level at 75% of the associated main target, rounded down to an authored tier. It requires matching progression group, movement pattern, side structure, unloaded kind, and target unit. Existing alternative history and manual targets take precedence. Loaded work, unrelated movements, cross-unit conversion, and completed achievement tiers are excluded. This is a product estimate, not a claim that two exercises are physiologically equivalent.

The planner prevents two exercises from the same progression appearing in one generated workout, including loaded counterparts. The path view emphasizes the main route and keeps alternatives and support distinct. Programs show compact summaries with authored details available on request. Demonstration holds suppress automatic selection and video playback while leaving written instructions available.

## Feedback carried into the release

The workout feedback items implemented in this source include slower spoken cues (speech rate 0.75), left/right announcements, and a five-second change-side interval for two-sided holds without reducing either hold duration. Preview rest is editable in 15-second steps from 15 to 300 seconds; the edited duration updates the preview total and carries into the started workout. Rest/stretch swapping and Reset remain available. Duplicate-progression prevention applies to generated workouts.

The owner-approved progression review is recorded in `docs/progression-comment.md` and `docs/30-progression-implementation.md`. That review controls the route design; it supersedes the earlier temporary hold on progression changes while keeping media acceptance and phone verification separate.

## Media coverage and remaining limits

The media evidence records 379 frozen render inputs, 379 encoded review MP4s, and six evenly spaced decoded samples for each ID. The full contact-sheet coverage is documented in `docs/evidence/clips-r8/r8-media-release-review-2026-10-16.json`. That report identifies 38 unresolved teaching or rig limitations, three items needing explicit visual disposition, and several candidate holds. Its findings are a triage record, not blanket semantic approval.

Root recorded six camera-only repairs in `docs/evidence/clips-r8/camera-repair-2026-10-16.json`; the repair record says pose data and frozen hashes were unchanged. Final media disposition should use the corrected outputs. For the install candidate, the final metadata removes the `flag-support-press` hold, leaving 25 held demonstrations, including `short-foot`, `rower-situp`, and `turn-and-lunge`. The short-foot clip does not show intrinsic arch shortening; rower sit-up loses visible floor support at peak extension; turn-and-lunge needs the authored low reach/transition explained clearly. Held clips show written instructions and do not autoplay. Other unresolved rig limits include selected neck and scapular motions, wrist/finger detail, rope visibility, and the schematic Pilates/spinal-rotation motions documented in the media review.

The final catalog has 25 demonstration holds. Root reviewed all six corrected full-render sequences and installed their encoded outputs. Of the three baseline visual-review questions, `handstand-press-wall-negative` is repaired, `stretch-shoulder-er` remains usable with a partly occluded static contact, and `turn-and-lunge` is withheld. The media JSON retains its baseline triage and adds the final root disposition.

## Verification and release gates

The recorded verification includes 13 catalog-authoring checks, 428 domain tests for progression and alternative target behavior, 29 preview checks, and 186 app tests at the progression checkpoint. The recorded logs are `docs/evidence/r8-progression-verify-2026-10-16.log`, `docs/evidence/r8-progression-ui-final-2026-10-16.log`, `docs/evidence/r8-alternative-target-domain-final-2026-10-16.log`, and `docs/evidence/r8-preview-rest-verify-2026-10-16.log`. The earlier full app run had three expected media-asset failures before clip installation. This document ran no builds or tests.

Still open after source review: inspect the corrected videos on a phone, check the installation/update preserves the owner's existing history and settings, verify speech and haptics during an actual session (including screen-off timing), and complete the owner walk-through in `docs/evidence/owner-check-r8.md`. Automated tests, emulator views, and contact sheets do not substitute for those checks.
