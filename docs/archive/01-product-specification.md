# Calisthenics app — consolidated feature specification

Status: consolidated product scope approved by the user. Engineering/training defaults introduced in the coding specification remain subject to G0 review. This is NOT the implementation plan, a completed source audit, or a claim that software has been built/tested.

## 1. Purpose and precedence
Build a personal Android workout app that retains a familiar, hands-free circuit experience while progressively adapting difficulty, available equipment and optional skill goals. Reuse an appropriate open-source foundation where practical. The personal release should allow eventual open-source publication.

This document reconciles the chronological notes in the feature-definition conversation. Later user decisions supersede earlier proposals. In particular:
- Normal progression is automatic, not approval-gated.
- Post-session feedback is optional; missing feedback on completed work assumes targets were met.
- Stretching mode has no programmed passive-rest-only blocks.
- Warm-up and cooldown are optional and initially off.

The earlier `research/landscape-survey.md` is a discovery input, not verified evidence of training correctness, repository quality or reuse rights.

## 2. Confirmed requirements

### Platform and onboarding
**PLAT-01** Android first, verified on the user's Samsung Galaxy S21. Record Android/One UI versions during device setup. English interface and spoken guidance.

**PLAT-02** Core workout preparation from installed content, execution, guidance, feedback and local history must work without internet. Starter demonstrations are bundled; additional content packs can be downloaded beforehand. No account required.

**ONB-01** Short self-assessment, conservative starting workout and subsequent adaptation. No mandatory maximum-effort test. Allow manual selection of familiar exercise variations.

### Planning and customization
**PLAN-01** Propose a familiar, reusable workout routine. Preserve repeated circuit rounds by default. Evolve it gradually rather than randomizing every session; explain exceptions such as separate skill-practice blocks.

**PLAN-02** Initial session duration is 45 minutes. Let the user adjust duration before each session. Include programmed strength, recovery/stretching, enabled warm-up/cooldown and transitions in the duration budget. Adapt round/set/block counts rather than speeding up movement or removing necessary recovery. Exact budget tolerance and supported duration range remain open.

**PLAN-03** Choose exercise-specific work and recovery/stretch timings automatically. Use timed holds/stretches and repetition targets within timed blocks as appropriate. Timings are editable before starting and can be saved.

**PLAN-04** Preview the proposed workout and swap exercises. Offer Full body, Upper body, Lower body and Core strength focuses, allowing combinations of specific focuses. Strength focus and stretch focus are independent.

**PLAN-05** Exercise swaps and strength-focus changes apply to today's session by default. Offer an explicit save-to-usual-plan action and an avoid-this-exercise-in-future preference. Preserve familiar routines across temporary changes.

**PLAN-06** Recommend a weekly training frequency while allowing sessions to start on demand. The user currently trains three times per week; this is a baseline, not a universal prescription. Do not penalize missed days.

### Equipment and travel
**EQ-01** Saved, editable equipment profiles. Initial Home profile: pull-up bar, one 12 kg kettlebell, two 2.5 kg weights, mat and resistance bands of different strengths. Initial Travel profile: chair only. Everyday objects are supported equipment categories, with suitability requirements for their use.

**EQ-02** Switching profiles or temporarily changing equipment adapts today's workout without resetting progression or permanently rewriting the usual plan.

**EQ-03** Prefer genuine movement-appropriate substitutions. If none exists, explain the limitation and offer a useful alternative without falsely crediting it toward the unavailable movement/skill. Do not treat chair dips as equivalent to pull-ups.

**EQ-04** Suggest equipment only when it offers a meaningful progression benefit. Explain the benefit, offer equipment-free alternatives where possible, and allow dismissal/suppression. Suggestions appear outside active training; purchases are not mandatory.

### Stretching and optional preparation
**STR-01** Between-exercise stretching is initially on, with full-body selection. Permit multiple chosen stretch areas independently of strength focus. Remember this preference.

**STR-02** Stretching on means no programmed passive-rest-only blocks. Stretching off means ordinary recovery intervals remain, not that recovery disappears. User pause/stop is always available. Appropriate exercise/stretch pairings are required; this feature is not a claim that all stretching equals passive recovery.

**STR-03** Balance left/right stretching, support replacing/excluding uncomfortable stretches, and report limited coverage honestly when the session cannot cover the whole body.

**STR-04** When shortening workouts, trim additional stretching before strength work where possible. Removing stretching from an unchanged recovery window saves no time; reduce round/set/block counts when necessary. Do not silently violate STR-02.

**STR-05** Warm-up and cooldown are independently optional, initially off, included in the duration budget when enabled. Remember preferences.

### Progression, feedback and skill map
**PROG-01** General progressive training works without choosing a skill goal. Optionally select a goal; adapt part of the routine toward it while retaining general strength work. Changing/removing goals preserves history.

**PROG-02** Browsable progression map: current variations, easier/harder variations, prerequisites, exercise-to-skill relationships, equipment requirements and advancement criteria.

**PROG-03** Five-star levels apply within each exercise variation, not across an entire exercise family or simply to attendance. Show current target, next milestone and explicit exercise-specific criteria. Retain previous variation achievements when advancing. Stars are app estimates, not externally certified mastery; their evidence can include assumed target attainment under PROG-05.

**PROG-04** Automatically increase difficulty gradually over weeks of completed training when targets are met or assumed met. Increase appropriate targets within a variation, then transition to a harder variation when its defined conditions are satisfied. Do not require routine approval prompts. Explain changes before training and allow easier/harder overrides. Do not progress from calendar passage alone or make ever-longer sessions the default progression mechanism. Exact thresholds and caps require research and specification.

**PROG-05** Feedback is optional during and after training. Post-session default: one row per exercise, not per round, with Below target / Met target / Above target, optional actual reps/hold duration and a separate discomfort flag. Completed work without feedback defaults to target met and can contribute to stars/progression. Distinguish assumed results from explicitly confirmed results; never present them as measured repetitions. Skipped or unfinished blocks do not count as completed targets.

**PROG-06** After discomfort, offer an easier variation or lower target for approval and retain that adjustment. Block automatic advancement for that exercise until the user explicitly indicates comfort with progressing again; silence must not clear this restriction. Do not diagnose or prescribe rehabilitation. Preserve pause/stop controls and do not portray an easier level as guaranteeing safe continuation.

### Demonstrations and active workout
**UI-01** A reusable library of short looping exercise demonstrations plus concise guidance. New exercises can be reviewed before starting. Starter demos are bundled; additional packs are downloadable. Check required media availability before starting; no unexpected mid-session downloads.

**UI-02** Active screen: current exercise/demo, prominent countdown, applicable repetition target, current round, overall session progress and next exercise. Optional controls: pause/resume, skip, easier alternative and performance feedback. Normal execution requires no taps.

**AUD-01** Announce the upcoming exercise shortly before transition, provide start cue and ending countdown, announce rounds, and offer optional brief technique reminders. Offline English speech/cues must be available; verify actual speech availability offline rather than assuming installed TTS works.

**AUD-02** Timing/audio continue while the screen is locked or another app is foregrounded. Briefly lower music/podcast volume for spoken cues, then restore it, subject to verified Android behavior.

**RUN-01** Pause preserves position and remaining time. An audio-interrupting phone call pauses training; user resumes explicitly afterward. After crash/restart, recover saved session state and offer resume or finish early. Do not silently convert unobserved elapsed time into completed training.

### History and data
**DATA-01** History shows completed/partial sessions, duration, exercises, feedback, achieved/assumed targets, per-exercise star development and weekly sessions versus suggested frequency. Preserve completed work from interrupted sessions.

**DATA-02** Local on-device storage without accounts. Export/import backup for recovery and phone migration. Backup scope, conflict handling, schema migration and media treatment require explicit implementation contracts and tests.

**SCOPE-01** Personal-first release, designed for later open sourcing. Broad but reasonable initial exercise library, not just the user's current routine. Each included exercise needs sufficient instructions, equipment tags, progression context where applicable, and usable licensed demonstrations. Exact catalog membership is a research deliverable, not an arbitrary promised count.

**SCOPE-02** Initial release does not require cloud sync/backup, accounts, social features, body-weight tracking, progress photos, public-store launch or iOS/web support. These exclusions are scope boundaries, not claims that later expansion is impossible.

## 3. Research and design decisions still open
These are not permission for an implementing model to invent behavior silently. Resolve them with evidence/recommendations before finalizing the implementation plan; bring consequential user-facing tradeoffs back to the user.

1. Foundation: inspect candidate repositories from the survey; verify maintenance, buildability, architecture fit and code licenses. Treat native Android/offline/background audio as a major suitability gate. No framework selected yet.
2. Content provenance: source exercise families, prerequisite relationships, training guidance and stretch pairings. Distinguish app conventions from supported exercise guidance. Record evidence and uncertainty. Inspect code, data and media reuse rights separately, including attribution/share-alike obligations.
3. Library scope: name the initial exercises, skill goals and stretch areas; separate fully usable starter content from optional packs. Decide how unavailable or unlicensed demonstrations will be produced/replaced and reviewed. Do not represent generated media as validated technique.
4. Progression: exact five-star thresholds; qualifying history/window; automatic cadence/caps; below-target response; plateaus; inactivity; moving between equipment variants; initial self-assessment mapping; and regression/override precedence.
5. Feedback: how one exercise-level rating applies across several rounds, how optional mid-session feedback overrides defaults, and how later edits recompute stars without rewriting historical prescriptions.
6. Timing: duration bounds and tolerance, transition/setup time, bilateral scheduling, how changing timings affects the plan, and behavior when a repetition target is finished before its timed block ends. Early completion must not introduce a hidden passive-rest block contrary to stretching mode or force extra repetitions.
7. Infeasibility: what the app does when selected focus, exclusions, equipment, duration and no-passive-rest stretching cannot form a suitable plan. Present alternatives before starting; never silently break user constraints.
8. Controls/runtime: skip/easier-alternative behavior inside a block, seek/restart policy on recovery, audio-focus/Bluetooth/headphone interruptions, media-pack failures, installed TTS requirements and Android battery restrictions. Confirm supported behavior on Galaxy S21.
9. Persistence: precisely separate remembered session settings from session-only exercise/focus edits; define Full body versus combined subfocus semantics; define usual plan versus routine variants.
10. Data/distribution: backup versioning and restore conflicts, corruption protection, deletion/privacy behavior, pack version compatibility, minimum Android version and personal APK installation/update path.
11. Presentation: navigation and accessible screen layout still need a reviewable prototype. App name/branding are not architecture blockers.

## 4. Verification checkpoints for the eventual implementation plan
All checks below are PLANNED, NOT EXECUTED. The final plan must map requirements to automated tests, real-device checks, required evidence and user sign-off. Numeric tolerances must be specified where applicable.

| Gate | What we inspect together | Required evidence |
|---|---|---|
| V-01 Source/foundation | Why reuse this base; why the exercise rules/content are credible and reusable | Repository/build audit, license inventory, source-to-rule matrix, explicit gaps |
| V-02 Android feasibility | Offline timer/audio on Galaxy S21, screen locked, app backgrounded, other media playing | Runnable device build, observed timing/audio results, documented permissions and Android version |
| V-03 Session preview | Default session, shorter session, upper-body focus, temporary swap and saved-plan edit | Duration breakdown and persisted-versus-temporary behavior; no silent focus/equipment violations |
| V-04 Stretch modes | Full-body/targeted stretches, bilateral balance, warm-up/cooldown toggles | Generated timelines: no passive-rest-only blocks in stretch mode; preserved recovery in non-stretch mode |
| V-05 Travel | Switch Home to chair-only Travel and back | Compatible selections, explained non-equivalent substitutions and unchanged underlying progression |
| V-06 Progression | Replay completed sessions with no feedback, below-target ratings, skipped work and discomfort | Deterministic star/difficulty changes, provenance labels, bounded increases, discomfort lock until explicit clearance |
| V-07 Workout controls | Hands-free circuit plus pause, skip, feedback and easier alternative | Device walkthrough; correct countdown/state transitions and history |
| V-08 Recovery | Phone call, interruption, app termination and device restart | Saved state restored without fabricated completed work or duplicate history |
| V-09 Offline library | Starter content and downloaded pack in airplane mode; missing/damaged content | Playable local demos/cues; clear preflight failure/recovery instead of mid-session network dependence |
| V-10 Backup | Export, restore into clean app storage, compare; reject corrupt/incompatible backup safely | Verified settings/plans/history/progression round trip; no destructive partial import |
| V-11 Personal pilot | Real complete sessions on the user's S21, including ordinary and travel configurations | User feedback, resolved blockers and acceptance record; installation/update instructions |

## 5. Handoff boundary
After consolidated feature approval and the targeted source/technical audit, produce a separate executable implementation plan. It must include the selected stack/base and pinned revisions, data/state contracts, content manifest, task dependencies, tests-first acceptance criteria, risk spikes, migration/backup strategy, failure handling, stop-and-ask conditions, verification commands and human checkpoints.

Do not claim any verification gate is passed from a design document or a successful build alone. An unavailable device, unusable license, missing demonstration source or unvalidated training rule is a reported blocker, not a reason to invent successful output.
