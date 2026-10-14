# Coding specification — reference implementation

## 0. Status, authority and non-goals
The product requirements in `01-product-specification.md` are approved. This document turns them into concrete engineering contracts. Values marked **G0 DEFAULT** are explicit proposed product/training policies; implement only after the owner approves gate G0. They are testable heuristics, not scientifically validated exercise prescriptions.

No codebase audit or Android feasibility test has run. T01 must verify the reuse decision before application implementation. No runtime AI service, login, analytics, cloud database, social features, subscription or iOS/web client is in scope.

## 1. Reference architecture and reuse gate
Reference design: native Kotlin Android, Jetpack Compose/Material 3, Room, DataStore, coroutines/Flow, kotlinx.serialization and AndroidX Media3 for local video/audio. Use constructor injection, not a DI framework initially. This is an engineering recommendation driven by the Android-only/offline/background requirements, not a verified comparison result.

T01 must inspect Ironvellum and Calisthenics Memory first, then Calistenia and Ballast if needed, recording exact revisions, build evidence and code/data/media licenses. Either adapt a compatible Android base with its license intact, or justify the native reference scaffold with selected licensed content reuse. If a different architecture is materially better, STOP and obtain an ADR approval; do not improvise a WebView app that cannot prove background audio.

Planned modules (create only after T01):
- `:domain` — Kotlin/JVM only: models, planner, progression, timeline reducer, ports. No Android imports, wall-clock globals, network calls or Room annotations.
- `:data` — Android library: Room entities/DAOs, migrations, repositories, backup, asset/download adapters.
- `:app` — Compose UI, viewmodels, foreground session service, audio/video adapters, notifications and composition root.
- `content/` — reviewed JSON catalogs, provenance/licenses; source media manifests, not private user data.
- `tools/` — Python content/handoff validation, shell verification entrypoints.

Use stable mutually compatible dependency versions discovered in T03. Record exact JDK, Gradle wrapper/checksum, AGP, Kotlin, compile/target/min SDK and AndroidX versions in `toolchain.lock.md` and `gradle/libs.versions.toml`. Never use `+`, snapshots or runtime auto-updates. Proposed minimum Android API is 26; target/compile SDK must be selected against actual stable tools and current Android requirements at T03. No fabricated version numbers.

Namespace/application ID proposal: `app.calisthenics.personal`; working display name: Calisthenics. Confirm no installed app collision before device installation. Package feature folders by `onboarding`, `plan`, `session`, `history`, `progress`, `equipment`, `settings` rather than one giant file.

## 2. Type and identity conventions
- Stable machine IDs are lowercase kebab-case; display names may change without changing IDs.
- Time: `Long` milliseconds for runtime, integer seconds for content targets. UTC epoch timestamps for events plus IANA time zone ID for weekly display. Monotonic time for in-process timing, never wall time subtraction.
- Mass: integer grams; Home kettlebell = 12000g, small weights = quantity 2 at 2500g each. Band strength is a named label unless the user supplies a calibrated resistance; do not invent kg equivalents.
- Immutable domain values. Inject `Clock`, `IdGenerator`, repositories and `ContentCatalog`; deterministic tests use fixed values.
- Separate variation, prescription tier, star achievement and session execution. Do not identify an exercise by its label or media URL.
- Version content, progression policy, plan revisions and backup schema separately.

## 3. Domain data contract
The following records are required. Kotlin data classes are the normative target; Room may normalize them differently without changing behavior.

### ExerciseVariation
`id`, `familyId`, `name`, `movementPatterns:Set<Pattern>`, `strengthAreas:Set<Area>`, `kind:REPS|HOLD|STRETCH|MOBILITY`, `equipmentAlternatives:List<EquipmentRequirementSet>`, `unilateral:Boolean`, `instructions:List<String>`, `formCues:List<String>`, `cautions:List<String>`, `difficultyRank:Int`, `mediaId`, `sourceIds`, `reviewState:DRAFT|REVIEWED`, `reviewer`, `reviewedAt`, `progressionPolicyId?`, `compatibleStretchIds`, `catalogVersion`.

Equipment alternatives are OR across sets and AND inside each set; requirements include quantity, mass range where relevant, and required suitability flags. Chair availability alone does not imply it is stable enough for every exercise. Bands do not imply a suitable anchor. Floor and wall space are user-confirmed environment capabilities, not purchased equipment.

### ProgressionPolicy
`id`, `variationId`, `version`, `tiers:List<Tier>` with exactly five ordered entries, `nextVariationIds`, `prerequisiteRule`, `evidenceSourceIds`, `policyKind:REVIEWED_GUIDANCE|PRODUCT_HEURISTIC`, `approved:Boolean`.

`Tier`: `index:1..5`, `target:Reps(Int)|HoldSeconds(Int)`, `workWindowSeconds`, `minRecoverySeconds`, `tempoCue?`, `minQualifyingBlocks`, `maxWorkSeconds`, `earlyCompletionStretchId?`.

Each target is authored per variation. No universal formula like "tier = 5 more reps"; weighted targets must fit available equipment and movement quality constraints. Not every stretch needs a strength progression policy. Stretch coverage is not strength mastery.

### SkillNode / SkillEdge
Node: `id`, `name`, `description`, `variationIds`, `equipmentSummary`, `sourceIds`, `reviewState`.
Edge: `id`, `fromId`, `toId`, `relation:PREREQUISITE|RECOMMENDED_PREPARATION`, `criterion`, `sourceIds`.
Prerequisites use explicit AND groups of OR alternatives, with typed predicates (`variationTierMet`, `equipmentAvailable`, `userSelfAssessment`). No free-text expression evaluation. Reject cycles and dangling nodes. Equipment gates filter eligibility but do not erase achievement.

### EquipmentProfile / Preferences
Profile: `id`, `name`, `items:[{equipmentId,quantity,massGrams?,strengthLabel?,suitabilityFlags}]`, `environmentCapabilities`, `updatedAt`.
Preferences: default duration 2700 seconds; stretch on; stretch areas FULL_BODY; warm-up off; cooldown off; English; optional technique cues; audio enabled; remembered selected equipment profile; excluded variation IDs; dismissed equipment suggestions.
Routine: `id`, `revision`, ordered `RoutineSlot`s, default strength focus and optional goal. A slot has stable ID, movement intent and preferred variation, not merely an exercise list index.
Session draft overlays the routine with duration/toggles/profile availability/swaps/focus/goal. Duration/toggles/stretch focus are remembered after starting; strength focus and exercise swaps are session-only unless explicitly saved. Temporary equipment availability never edits the profile without Save Profile. Goal changes use an explicit persistent action.

### WorkoutPlan and TimelineBlock
Plan is an immutable snapshot: `id`, `routineId`, `routineRevision`, `catalogVersion`, `policyVersion`, `createdAt`, `equipmentSnapshot`, `requestedDurationSeconds`, `plannedDurationSeconds`, `focus`, `goalId?`, `changesExplained`, `warnings`, ordered blocks.
Block: `id`, `roundIndex?`, `slotId?`, `type:WORK|STRETCH|PASSIVE_RECOVERY|WARMUP|COOLDOWN|TRANSITION`, `variationId?`, `side:NONE|LEFT|RIGHT|BOTH`, `durationSeconds`, `target?`, `prescriptionTier?`, `recoveryForBlockIds`, `mediaId?`, `cueSchedule`.
A TRANSITION is setup time, never recovery disguised to evade stretch mode. **G0 DEFAULT:** transitions are at most 5 seconds per actual equipment/position change; ordinary no-change transitions are zero. Longer setup pauses are user-initiated pauses.

### WorkoutSession / block outcome / feedback
Session: `id`, `planSnapshot`, `state`, `startedAt`, `finishedAt?`, `finishReason`, `activeElapsedMs`, `checkpoint`, `eventSequence`, `createdAt`.
Outcome: `blockId`, `execution:NOT_STARTED|RUNNING|COMPLETED|SKIPPED|PARTIAL`, `activeMs`, `rating:BELOW|MET|ABOVE|UNRATED`, `ratingOrigin:ASSUMED|USER_BLOCK|USER_EXERCISE`, `actualReps?`, `actualHoldSeconds?`, `discomfort:Boolean`, `feedbackRevision`.
Actual metrics are null unless entered by the user. `ASSUMED/MET` is a label on completed work, not fabricated actual reps. No assumptions for skipped/partial/not-started blocks.

Progress record: variation/tier achievement history, current tier, maximum earned tier, current target, automatic advancement timestamps, qualifying exposure IDs, discomfort hold state, pending easier adjustment, explicit override and provenance summary. Immutable prescription history must survive later content/feedback changes.

## 4. Planner contract
Signature:
```kotlin
fun generate(input: PlanInput): PlanResult
sealed interface PlanResult {
  data class Ready(val plan: WorkoutPlan) : PlanResult
  data class Infeasible(val reasons: List<ConstraintFailure>, val alternatives: List<PlanOption>) : PlanResult
}
```
Pure and deterministic: identical inputs/catalog/policy/time snapshot produce identical output and explanations. No hidden randomness.

Algorithm order:
1. Validate available content, approved policies, profile suitability, duration and focus. **G0 DEFAULT:** supported duration 10..90 minutes in one-minute steps; a valid duration can still be infeasible.
2. Normalize strength focus: FULL_BODY is mutually exclusive with specific focuses; selecting it clears the others. Specific focuses combine by union. Stretch focus is separate.
3. Preserve routine slot order and familiar eligible variations. Filter exclusions, discomfort hold, prerequisites and equipment first. Then rank candidates: existing eligible variation, approved progression successor, equivalent substitute ordered by difficulty distance, stable ID as tie-breaker.
4. Non-equivalent replacement produces a visible warning identifying the missing movement; it does not claim movement balance or progression equivalence. An explicit confirmation before starting acknowledges the changed workout. Repeated use may save this preference, but not silently.
5. Optional skill goal replaces at most one eligible strength slot per round in the initial policy, retaining the other selected-area movement intents. **G0 DEFAULT**; explain when goal/equipment/focus constraints prevent practice.
6. Use each variation's reviewed tier timing and stretch compatibility data. Assemble WORK blocks and required recovery, filling recovery with compatible STRETCH blocks when enabled. If no valid stretch pairing exists, return Infeasible with alternatives (change stretch focus, swap work exercise, disable stretch mode). Never silently insert passive recovery.
7. Allocate warm-up/cooldown only when enabled using reviewed templates. Interleave bilateral stretches as inseparable paired units unless content is explicitly bilateral. Per-side allocated time must match; a one-sided injury/exclusion requires explicit exception, not automatic imbalance.
8. Fit the selected time by removing optional extra stretches, then complete rounds, then optional slots/paired blocks while preserving the mandatory movement intent set of the selected routine. Do not shorten work/recovery below reviewed minima. If nothing feasible remains, return Infeasible.
9. **G0 DEFAULT:** never exceed requested time; a ready plan should be within 60 seconds below it. If shorter by more, ask the user to accept the actual duration or change constraints; never add passive padding or quietly claim 45 minutes for a shorter plan.
10. Validate invariants and freeze the snapshot. Output every automatic change and why.

No-passive invariant: with stretching enabled, no PASSIVE_RECOVERY blocks; no unlabeled recovery gaps; no "transition" longer than setup policy; every required recovery relation has validated stretch coverage. This does not prohibit voluntary human rest; the software cannot infer bodily activity from a running timer.

Early repetition completion: the hands-free schedule remains timed; no microphone/camera inference. Guidance explains controlled target reps within the work window. If a reviewed early-completion stretch exists, announce it as the optional remainder-of-window action at block start. An optional Done button can jump into that preplanned compatible stretch for the remaining window; never require extra reps. If no safe remainder action exists, content cannot promise a fully occupied no-passive interval: flag this at preflight/content review. Do not create a compulsory passive-rest block automatically. This design distinction must be shown at G0.

## 5. Progression contract — G0 DEFAULTS
These exact engineering defaults make tests deterministic but MUST be approved/reviewed; they are not published physiological thresholds.

- Initial tier from the reviewed self-assessment mapping, conservative if unknown. Unassessed is shown as unassessed, not five stars or failed tier zero.
- An exposure is one session with the variation's minimum qualifying number of completed WORK blocks at the current prescribed target, no skipped/partial work for that variation, no BELOW/discomfort ratings, and no easier target override. Multiple rounds count as one exposure; multiple sessions on the same local date count at most once per variation.
- Missing completed-block ratings resolve to ASSUMED/MET. An exercise-row rating applies only to completed blocks with no explicit per-block rating. Per-block user rating wins. Discomfort anywhere wins over all MET/ABOVE evidence. Explicit actual values below the target imply BELOW even if a conflicting rating says MET; UI asks for correction instead of silently hiding the conflict.
- Three qualifying exposures spanning at least seven days make the current tier achieved and advance the next prescription by ONE tier. First advancement also requires seven days since enrollment at that tier. At most one automatic advancement per variation in any rolling seven-day period. ABOVE does not bypass prerequisites, windows or caps.
- Stars count the highest consecutive tiers achieved for that variation; initial manual self-assessment is displayed as self-reported baseline separately from earned history. Proposed initial level-to-star presentation must be approved at G0.
- After earning tier five, move to tier one of a reviewed harder variation only when prerequisites/equipment are satisfied, the same pacing gate passes, and no discomfort hold exists. No extra routine confirmation, but explain the change and allow undo/easier override. Do not erase five-star history of the prior variation. If no successor is available, maintain the variation and optionally suggest useful equipment.
- One BELOW breaks the current qualifying streak and holds difficulty. Two consecutive BELOW exposures recommend a lower target; do not automatically diagnose fatigue or change unrelated exercises. An easier user override pauses automatic advancement for that variation until the next normal target is selected.
- Discomfort sets a persistent family-level hold to avoid escaping it through a near-identical variation. Offer a reviewed easier target/variation for approval. Until accepted, preflight blocks that affected slot and offers skip/change/finish, not a harder normal prescription. Explicit comfort clearance is required to resume automatic advancement; clearance does not advance immediately.
- Inactivity never earns progress or removes historical stars. **G0 DEFAULT:** after 14 days without training that variation, propose a lower re-entry target and require a choice before that slot returns; do not silently erase achievements.
- Generate changes only for the next plan, never mutate an active/frozen plan.
- Late feedback edits recompute derived evidence deterministically and idempotently. Preserve old session prescriptions and achievement audit trail; add a superseding record if needed. Never repeatedly award a tier from the same exposure IDs. Newly reported discomfort applies immediately to future plans even when the original session is old.

Expose explanations such as "Target increased after three completed sessions; two results were assumed." Never claim form was observed, reps measured or a skill clinically mastered.

## 6. Session state machine and timing
States: DRAFT -> READY -> RUNNING <-> PAUSED -> COMPLETED or PARTIAL_FINISHED. On process recovery of an unfinished RUNNING/PAUSED record, enter RECOVERY_REQUIRED, then user chooses PAUSED-resumable or PARTIAL_FINISHED. Terminal states are immutable except feedback edits.

Events include Start, Tick, Pause(reason), Resume, Skip, EarlyDone, EasierAlternative, FinishEarly, AudioInterrupted, PersistCheckpoint, ProcessRecovered. A pure reducer returns new state plus typed effects; only Android adapters perform audio/storage effects.

- Use elapsed-realtime/monotonic deadline arithmetic. Ticks repaint state; never subtract one second per callback.
- One foreground session service owns the timer, not a composable, Activity or viewmodel. UI observes state. Never run competing Activity and Service timers.
- Derive remaining time from deadline while process continuity is certain. Pause freezes remaining time; resume establishes a new deadline.
- Persist a checkpoint transaction on every transition, feedback change, pause, skip, finish and at most every second while running. Use `(sessionId,eventSequence)` uniqueness and compare-and-set updates to prevent duplicates.
- On process restart or boot, do not apply old monotonic timestamps or wall-clock gaps. Restore the last checkpoint paused, with at most one second of uncheckpointed work explicitly unknown. No auto-completion of intervening blocks.
- Skip marks that block SKIPPED, not MET. Skipping a strength block still preserves appropriate recovery before subsequent work when earlier load requires it. Skipping a stretch does not silently remove required recovery; offer another compatible stretch or pause to adjust the plan. User may stop at any time.
- Easier alternative pauses; choose compatible reviewed option, preserve original block as partial, create a replacement block in a recorded amended plan, then resume explicitly. History retains both actual work and original prescription; incomplete original work grants no success.
- Finishing all scheduled blocks is COMPLETED even if individual blocks were skipped; the summary explicitly states skipped blocks, and the progression rules exclude them. Finish early is PARTIAL_FINISHED.

## 7. Android runtime/audio contract
T04 must prove an appropriate foreground-service design against the actual target SDK's allowed service types/permissions, notifications and background restrictions. Do not declare a fake media stream, misuse a health service permission or hold silent audio merely to stay alive. Record the chosen supported approach and relevant Android documentation in the ADR. If no compliant design meets requirements, stop before full implementation.

- Persistent active-session notification with pause/resume and stop; restore UI from notification.
- Select wake/foreground strategy only from official Android documentation and test screen-off timing. Acquire any wake lock only during active operation and release on pause/stop/error. No blanket battery-exemption requirement without first testing normal settings and explaining need.
- Offline English cue assets or an installed offline voice verified in airplane mode. Resolve cue assets before start. Cloud TTS is not a fallback during workouts.
- Cues use stable IDs tied to session/block/event, with duplicate suppression. Start cues, round cues, next exercise shortly before transition, and countdown have defined priority; never overlap spoken utterances. **G0 DEFAULT:** preview at 5 seconds, countdown at 3/2/1; suppress overlapping preview for short blocks and validate this during content checks.
- Brief audio ducking while cues play; release focus afterward. Test actual other-app behavior on S21. If the OS cannot duck the chosen media app, document the observed behavior instead of claiming success.
- Audio focus interruption/call pauses the workout. Regaining focus does not auto-resume. Headphone disconnection pauses; user chooses output and resumes. No recording, microphone or camera permissions.
- Video is local looping, muted, lifecycle-aware and not required to remain playing with screen locked. Audio/timer are independent of video lifecycle.

## 8. Content, media and pack contract
No starter catalog is complete until named exercise records, demonstrations, audio, licenses and reviewed policies exist. T02 proposes broad coverage: push, pull, shoulder work, squat/lunge, hip hinge/posterior chain, core/holds, calf/ankle/hip/hamstring/back/chest/shoulder stretches, plus beginner goal paths. Number of production exercises is determined by rights/review capacity and explicit G0 approval, not an unsupported promise.

Candidate families may include incline/knee/standard push-ups, band-assisted pull-ups/rows with suitable anchors, squats/split squats, controlled kettlebell deadlift, bridge/superman variants, plank/side plank/dead bug and appropriate stretches. These are catalog leads, NOT approved prescriptions. Do not add ballistic kettlebell work or advanced inversions merely to inflate breadth.

Every media record: `id`, `relativePath`, `mimeType`, `byteSize`, `sha256`, `durationMs`, `width`, `height`, `creator`, `sourceUrl`, `licenseId`, `attributionText`, `redistributionAllowed`, `offlineAllowed`, `reviewState`. Store local MP4 loops without embedded trackers. A software license does not automatically cover third-party photos/video. Do not bundle unclear-rights assets.

Pack manifest: `packId`, `version`, `schemaVersion`, `minAppVersion`, `catalogVersion`, files with SHA-256 and expected length, license inventory, HTTPS origin. Download only from allowlisted HTTPS origins. Install into staging, enforce size limits, verify all files, then atomic rename/catalog transaction. Reject traversal paths, absolute paths, symlinks, duplicate IDs and decompression bombs. Keep previous pack until successful activation. Hash verification detects corruption; trusted manifest/origin is still required to prevent malicious replacement.

Starter offline media is bundled with APK. Additional pack base URL must be configured and documented before release; do not invent a working CDN. Until a real endpoint exists, optional packs remain an explicitly blocked release feature, not a pretend download button.

Preflight detects absent/incompatible media and offers download before training or a compatible installed substitute with explanation. No silent text-only fallback where a demo was promised. Do not delete a pack referenced by an active/resumable session.

## 9. Persistence and backup
Room owns routines, plan/session snapshots, block outcomes, feedback revisions, progression events, equipment and catalog indices. DataStore holds lightweight preferences only. One repository transaction crosses session outcome/progression changes where consistency is required; avoid dual sources of truth for history.

Export via Android Storage Access Framework to a versioned `.calibackup` ZIP containing `manifest.json`, logical JSON records, custom settings and optionally user-authored notes if ever introduced. Initial backup contains catalog/policy snapshots needed to render history and pack IDs/hashes, not distributable demo binaries. Explain that additional demos may need redownloading after restore. Starter demos remain in APK.

Manifest: formatVersion, appVersion, createdAt, file lengths/hashes, record counts, content policy versions. No credentials, APK signing keys, absolute host paths or device identifiers. Plaintext backup contains workout history; warn before export. Disable implicit OS/cloud app backup for v1 so 'local only' is true unless the user explicitly chooses a cloud file destination.

Import v1 is **replace**, not merge: show summary, require confirmation, create a recoverable local pre-import backup, validate archive + relationships + supported schema into staging, then commit atomically. Never alter live records on failed validation. Reject duplicate IDs, unknown future schema, oversized files, malformed JSON, traversal/symlinks and mismatched checksums. Import while a session is active is blocked. Idempotent reimport must not duplicate history.

Migrations are explicit and tested from each released schema. Never use destructive migration fallback. Keep historical content snapshots readable even if current packs change. Delete-all-data needs confirmation and must not delete arbitrary chosen filesystem paths.

## 10. UI and accessibility contract
Navigation: Today, Progress, History, Library; Equipment and Settings accessible from Today/settings. Avoid implementing a complex graph editor. Progress tree initially uses an accessible grouped list plus zoomable graph, both backed by the same node/edge model.

Today: selected routine/focus/goal/profile, duration/toggles, preview, explained changes and start. Preflight must resolve failures before start.
Session: prominent countdown/name/target/next/round, loop demo, pause/resume, skip, easier, optional feedback. Stop is reachable without scrolling. End summary has optional per-exercise feedback and Done without mandatory forms.
Progress: stars with text labels, next criteria, assumed-versus-confirmed provenance, goal and equipment information. History: partial/completed states with skipped/assumed details.

Use scalable text, accessible labels, at least 48dp control targets, sufficient contrast, no color-only status; support TalkBack and large font. Audio can be disabled explicitly; do not trap the user if permission/audio initialization fails. Portrait first, but rotations/resizing must not restart sessions or lose edits.

## 11. Security and publishing boundaries
No secrets in repository, no internet use during active session, no analytics or remote history. Restrict INTERNET usage to explicit content downloads. Include data/media license notices in app and repository. Never silently switch this project to a GPL-incompatible license. Personal installation starts with a debug APK; stable updates require a retained private signing key outside Git. Do not publish or create a remote until owner chooses account/name/visibility.
