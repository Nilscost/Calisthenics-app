# Progression implementation · 2026-10-16

The owner accepted the direction in `progression-comment.md` and the coordinator's corrections, then asked to implement it. This supersedes the earlier hold on progression implementation. Movie review and exercise demonstration acceptance remain separate; all content remains DRAFT.

## Accepted interpretation

- Roles belong to a route. One exercise may be a main step in one route and support in another.
- Main paths, optional alternatives, supporting drills, fixed-program membership and library visibility are explicit data. Difficulty rank is a browsing aid, not a successor rule.
- Rotation uses explicitly reviewed groups at a named main step. Each variant retains its own targets and achievements; main mastery advances the route. Do not infer equivalence from rank or broad core pattern.
- Preserve compound app-achievement gates and OR entry routes. Do not replace them with a blanket previous-step tier or infer readiness from equipment ownership.
- Hip-hinge/RDL and Nordic knee-flexion work have separate routes. Bilateral bench hip thrust is not automatically a compulsory successor to single-leg floor bridge.
- Keep existing exercise IDs, physical family IDs and historical evidence. Logical route splits must not reset or demote a person's existing progress.
- Default workouts use the small agreed pool. Goals and deliberately selected extras add specialist work. Conditioning is a timed finisher, not an ordinary strength rung.
- Demonstration hold is separate from an exercise's role. A held clip must not play merely because an exercise is searchable or listed by a fixed program.
- Trees show a main spine, with alternatives/support disclosed deliberately. Programs show compact summaries and exact authored details on request.

## Data and verification

`tools/progression_routes.py` is the reviewed authoring map; `docs/29-progression-routes.json` records the generated exact assignments and branch audit. Runtime catalogs carry the same route metadata. Every catalog ID must be accounted for; remaining review edges must be listed explicitly rather than claimed resolved by a rough count.

Required checks include valid route references/anchors, cycles and goal reachability, actual achievement prerequisites along each path, equipment alternatives without route disappearance, no duplicate progression including loaded variants, route-specific replay and stepping, independent alternate targets, existing-progress preservation, and deliberate default/extra pool selection.

The Blender task is independent: all379 accepted draft render inputs are frozen under `docs/evidence/clips-r8/render-input-freeze-2026-10-16.json`. Do not regenerate their caches or change accepted pose sources during this implementation.

## Implemented checkpoint

The runtime catalogs contain 533 exercises, 53 explicit routes and the eight contracted programs. The default pool is limited to push-up, dip, pull-up, row, squat, RDL, calf, anti-extension, floor/hanging leg raise, side plank and extension routes. Floor and hanging leg raises share a duplicate-prevention group. Power, conditioning, preparation and mobility remain deliberately selected extras; conditioning uses three timed intervals without strength achievement targets.

Planner slots, timeline blocks and derived evidence preserve the trained route. Completed workouts alternate main work and approved alternatives, including explicitly reviewed core alternatives from another physical family. Each exercise keeps its own targets; alternate mastery cannot promote the host route. Loaded variants share the relevant progression's duplicate-prevention group. Manual stepping follows the route, and missing equipment does not silently skip a main rung or select an unrelated equivalent. Existing exercise IDs and evidence remain intact; the debug seed fixture was regenerated for the appended route fields.

Progress shows the main spine, collapsed alternatives/support, deliberate access to all routes and searchable library entries. Fixed programs have compact equipment/duration summaries and expandable details. Demonstration holds suppress automatic selection and video playback in Progress/program previews; written program content remains available.

Verification on 2026-10-16: 13 catalog-authoring checks and all 423 domain tests pass. The full app suite ran 186 tests: 182 passed, one scrolling-list test needed a correction, and three existing media asset checks fail because the new clips/thumbnails are not installed. The corrected route UI test then passed in a focused rerun. Android lint and debug assembly pass. Logs: `docs/evidence/r8-progression-verify-2026-10-16.log` and `docs/evidence/r8-progression-ui-final-2026-10-16.log`. All 379 frozen render-input payloads and timestamps remain unchanged.

A short default exercise pool can produce a session shorter than the 30-minute target instead of adding specialist exercises or repeating a progression. This is reflected in the displayed duration. Rendered demonstration review, media installation, phone visual/interaction checks and owner release gates remain open. No phone, rendered-movie, or release gate is approved by this document.
