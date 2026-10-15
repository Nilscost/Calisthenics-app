# R8 progression review for discussion

Status: proposals, not an approved training prescription. All exercise content remains DRAFT. This review follows the owner's workout feedback of 2026-10-15.

## Main conclusion

The new library is useful, but a larger exercise library is not automatically a coherent progression. The importer currently preserves existing main paths and attaches new exercises as branches. Its fallback choice uses family, kind and difficulty rank; that can choose a different skill rather than the next useful step. We should review those choices before adopting new main paths.

The approved feet-elevated/diamond rotation is implemented. The additional intermediate-ladder rewiring explored during implementation has been removed from generated output and is held for discussion. Existing explicit app-only prerequisites and the approved six-exercise rep cap remain in scope.

## What should govern the engine

- A movement category such as Push can contain several progressions, such as push-ups and dips. Automatic workouts should choose one exercise from each progression, while alternatives rotate between workouts.
- Main-path mastery advances the main path. Alternatives retain independent targets and achievements and do not all have to be mastered.
- Comparable alternatives need an explicit group. Matching ranks alone does not establish equivalence.
- An equipment-specific step should not become mandatory for everyone. If a step needs a box, band or bar that is unavailable, the planner must preserve a usable route and explain its choice.
- Exercise prerequisites should describe achievements in the app. Their relationship to alternative routes must be explicit: all required, or any acceptable route.

## Findings and proposed debate

| Finding | Why it matters | Proposal to discuss |
|---|---|---|
| New intermediate branches can end without rejoining the original path. Examples include partial pull-up, negative/elevated pistol, half bridge and beginner Harop curl. | A manually chosen branch can reach its top and stop even though harder related exercises exist. Keeping the old main path also skips the new intermediate. | Review each continuation separately. Add a rejoin only where its prerequisites and technique make sense; do not automatically insert every lower-ranked exercise. |
| A pistol on an elevated step changes free-leg clearance and equipment requirements. | Rank alone does not make it a universal mandatory step between box and floor pistols. | Keep an optional route unless we agree it should be part of a specific goal path. |
| Partial pull-ups train a restricted range. Negative and full-range assisted pull-ups address different limitations. | A simple negative → partial → full ladder may imply everyone must follow the same route. | Offer partials as a supported alternative or optional intermediate; review how full-range strength is represented. |
| Tuck/straddle V-ups and L-sit compression work overlap but have different actions and target kinds. | Replacing an isometric L-sit path with a rep-based compression path can unexpectedly change the goal. | Keep strength goals intact and treat compression drills as supporting or alternative work unless explicitly agreed. |
| Bird-dog currently chooses prone swimmer as its default successor instead of quadraplex. | The starting position and movement change despite the shared family. | Prefer an explicit quadruped route; decide whether prone swimmer belongs in a separate progression. |
| Advanced-tuck front-lever row can choose ice-cream maker before straddle row. | Ice-cream maker adds a transition skill rather than merely extending lever-row difficulty. | Separate lever-row advancement from the transition branch. |
| Manual range-mode exercise stepping sorts a whole family by rank. | Its next item can be an optional specialization rather than the explicit main successor. This differs from engine advancement. | Decide whether the stepper means “next main step” or “browse this family”; label and implement that consistently. |
| Family IDs mostly represent progressions, but loaded variants can use a separate family. | A family-only duplicate safeguard may still allow bodyweight and weighted versions of the same movement together. | Start with the reported same-family duplicate bug; consider a separate explicit progression-group key for equivalent loaded variants. |
| Achievement requirements use different legacy thresholds and imported defaults. | A new tier-5 requirement alongside an older tier-3 or tier-4 route can be inconsistent or unnecessarily restrictive. | Audit thresholds and AND/OR grouping per branch rather than copying them mechanically. |
| Some power drills are isolated jumps; others are consecutive hops or conditioning. | A blanket repetition cap or one generic ladder would erase their differences. | Keep the approved cap limited to the six named explosive push-ups. Review other quality profiles individually. |

## Checks required before any approved rewiring lands

Every main path should be acyclic and reach its named goal. Each transition should pass with the actual app achievements accumulated on that route. Existing users who have moved past an inserted step should not lose access. Equipment changes must not trap a person at an unavailable successor. Rotation must never create two variants of the same progression in one generated workout. Tests should cover each of those behaviors with the actual catalog.

The current generator review report lists 211 parent rows and 47 branches needing review. That report is an audit aid, not acceptance of those branches.

## Suggested first discussion

First agree on the meaning of a progression versus an alternative and how the manual stepper should behave. Then review push-up/pull-up and pistol branches, followed by core/lever and power branches. For each branch, one review should propose a route and another should challenge its prerequisites, equipment dependence and missing skills before a final choice is implemented.

## Review lens clarified by the owner · 2026-10-15

Keep the exercise library comprehensive while keeping the app uncluttered and the progressions coherent. Library inclusion does not imply automatic prescription, a mandatory progression step, a new top-level screen, or a permanent visible choice. Prefer a small understandable main path, optional alternatives when relevant, and supporting work clearly separated from mastery requirements. Question or omit an addition that cannot justify its role; do not expand a ladder merely to accommodate every catalog entry.
