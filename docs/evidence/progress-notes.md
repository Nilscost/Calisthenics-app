# Progress note — 2026-10-02 (stopped on owner request)

## Done and saved in the project (Mac folder, git commit `4569f3e`)
- Review of the local model's work: `docs/evidence/review-2026-10-01.md`
- Full-version roadmap M0–M9: `docs/09-full-version-roadmap.md`
- Folder consolidated to `~/Documents/hermes/Apps/calisthenics`. Deleted the old copies (`Calsthenics V1/`, `pkg/`, zip) with your approval. T04 APK kept in `Apps/builds/` (checksum OK).
- Removed the broken `local.properties` and the duplicate `config/config` folder.
- `STATUS.md` updated with your decisions: G1 also tests spoken cues and music lowering, the phone has no USB, build the full version.

## Build environment (sandbox)
- Java 21 installed in the Hermes sandbox. `./gradlew --offline :domain:test` → **BUILD SUCCESSFUL** (the existing 8 tests).
- Android SDK download **started, then stopped** on your request, before it finished, to save hotspot data. The full Android app cannot be built yet.

## Work in progress: NOT compiled, NOT tested, NOT in the Mac project
These are draft "phone-free" building blocks. They live only in the sandbox copy `/workspace/cal` (cloned from `4569f3e`):

| File | Roadmap task | State |
|---|---|---|
| `domain/build.gradle.kts` | adds JSON serialization + a `review` report task | written |
| `domain/.../model/Model.kt` | T05 data model (exercises, tiers, equipment, routine, plan blocks) | written |
| `domain/.../equipment/Equipment.kt` | T08 Home/Travel profiles, equipment matching, optional suggestions | written |
| `domain/.../content/CatalogValidator.kt` | T06 catalog/skill-graph checks (IDs, 5 tiers, cycles, dangling links) | written |
| `domain/.../routine/Routine.kt` | T09 focus rules, today-only vs saved changes, weekly summary | written |
| `domain/.../feedback/Feedback.kt` | T12 optional feedback, "assumed met" labelling, evidence per session | written |
| `domain/.../progression/ProgressionEngine.kt` | T13 stars, slow automatic progression, discomfort hold, 14-day re-entry | written, mid-edit |

Not started: the starter `catalog.json`, the planner (T10/T11), the session engine (T14), the review report, and all unit tests for the new code.

## To resume
1. Compile `/workspace/cal` with `./gradlew --offline :domain:test` and fix the errors.
2. Write tests per file. Then copy the code into the Mac project and commit only once the tests pass.
3. Continue with T10/T11 planner → T14 session engine. Then M0 phone-test fixes, which need the Android SDK (~3–5 GB, so wait for proper Wi-Fi).

## Update 2026-10-04
Compiled and tested the draft code in the sandbox. `./gradlew --offline :domain:test` → BUILD SUCCESSFUL, 55 tests, 0 failures (8 existing + 47 new). Log: `domain-tests-2026-10-04.log`.
Committed: T05 model, T06 catalog validator, T08 equipment, T09 routine/focus, T12 feedback, T13 progression engine (domain module only).
Not started: starter `catalog.json`, planner (T10/T11), session engine (T14), M0 phone-test fixes (need Android SDK).
Known gap: the engine's equipment prerequisites are left to the planner; the owner-review report task was removed until it exists.

## Update 2026-10-04 (2) — planner + session reducer
`./gradlew --offline :domain:test` → BUILD SUCCESSFUL, 110 tests, 0 failures. Log: `domain-tests-2026-10-04-planner-session.log`.
Added: T10/T11 planner (`planner/Planner.kt`, 30 tests), T14 session reducer (`session/SessionReducer.kt`, 25 tests).

### Choices I made that are NOT in the approved ADR (owner review at G2)
- MAX_ROUNDS = 6 (cap on circuit repetitions).
- Minimum stretch segment 10 s (constant defined, not yet enforced).
- Unilateral stretches split the recovery window in two equal halves (L/R); a 20 s window gives 10 s per side.
- Slots are dropped only if `optional`; optional slots are dropped from the END of the routine first.
- Skill-goal planning (ADR B4) is NOT implemented: the planner warns and ignores a selected goal.
- Equipment prerequisites for progression successors are not checked in the engine.

### Still open
Starter catalog (`content/starter/catalog.json`) in the new model; Android side (M0 fixes, Room, UI, service); G1 phone test.

## Update 2026-10-04 (11): backup screen (T20 slice 1)
- Export to a user-chosen file (SAF), checksummed. Import validates fully first; rejected files change nothing; confirm restores settings + usual plan ONLY. History-row restore, pre-import recovery copy and active-session guard are NOT built. Not device-tested.
