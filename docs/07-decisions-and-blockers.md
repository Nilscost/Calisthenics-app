# Decisions and blockers

## Approved product scope
The owner approved the consolidated feature specification and final defaults. Earlier approval-before-progression proposals are superseded: routine progression is automatic, and completed work without feedback is assumed met. No passive-rest-only blocks in enabled stretching mode. Warm-up/cooldown optional/off. Personal Android first, Galaxy S21, local data, eventual open source.

## Engineering defaults awaiting G0
These are now specified precisely in `02-coding-specification.md`, but are NOT retrospectively attributed to the user:
- Native Kotlin reference architecture if no compatible audited foundation is better.
- Proposed API 26 minimum; actual toolchain/target versions selected and pinned in T03.
- 10..90-minute session range, no overrun, 60-second underfill tolerance.
- At most five-second real setup transitions; no disguised passive recovery.
- One skill-goal slot per circuit in the initial policy.
- Three qualifying exposures spanning seven days, at most one tier advancement per seven days, with no same-day acceleration.
- Earned stars versus self-reported starting level distinguished; exact presentation reviewed at G0.
- Family-level discomfort hold; explicit approval of an easier adjustment and explicit clearance before progression resumes.
- Re-entry choice after 14 days without that variation; no loss of historical achievement.
- Replace-only transactional backup import; binary demo packs excluded from backup with explicit explanation.
- Early repetition completion can use a reviewed remainder stretch; actual voluntary rest is not observable. No scheduled passive rest is introduced, and no extra reps are forced.

Numeric defaults are product heuristics until reviewed. The content tiers and recovery/stretch pairings require sourced, qualified review; absence of review blocks real workout release. An app convention is not physiological validation.

## Current blockers / unverified items
| ID | Item | Consequence | Resolution owner/task |
|---|---|---|---|
| B01 | File tools access a Docker sandbox, not the Mac Apps folder | Project registration alone does not prove target files exist | Local installer or restored host access; verify hashes in target |
| B02 | GitHub CLI/authentication on the Mac not accessible from this sandbox | No claim of authenticated create/push capability | Owner runs installer auth check or grants host tooling; T00/T26 |
| B03 | Browser Use CLI unavailable; external source extraction backend unavailable in this session | Prior survey not upgraded to a code/content audit | T01/T02 with working network/source tools |
| B04 | Host-runnable build now done (2026-09-29); **device** validation still not performed | `:domain`/`:data`/`:app` unit tests, lint and `assembleDebug` pass in the sandbox (T03 verified), but on-device Room (MigrationTest) and launch (LaunchSmokeTest) + G1 offline/background/audio/timer remain unverified | T04 (S21 runtime spike + owner observation) |
| B09 | Sandbox cannot build **androidTest** APKs: Room's KSP schema verifier (`DatabaseVerifier.<clinit>`) loads sqlite-jdbc natively, and this aarch64 Linux sandbox cannot mmap any native lib (`failed to map segment` — also blocks Robolectric/jansi). Main `assembleDebug` is unaffected (KSP main tasks cached/UP-TO-DATE; the spike service is in the app's main source set). **On the Mac (Darwin natives) the test APKs build normally.** | Test APKs (`app-debug-androidTest.apk`, data test APK) must be built/run on the Mac, not in the sandbox; the S21 session uses the **exported** spike service via `adb am`, so it does not need the test APK at all | Owner: build `:app:assembleDebugAndroidTest` on Mac only if running instrumented tests; S21 runbook is `spikes/android-session-runtime/README.md` |
| B05 | Model name/local runner availability unverified | No Qwen delegation has occurred | T00: inventory exact installed model, no silent substitution |
| B06 | No approved production media catalog or reusable media hosting endpoint | Broad library/optional packs not yet deliverable | T02/T06/T21 |
| B07 | Protected AGENTS.md write lacked approval | No auto-loaded agent file exists; use explicit startup prompt | Owner may authorize a separate future write; do not retry/bypass denial |
| B08 | No software license chosen pending reuse audit | Ready for private source storage, not public open-source release yet | T01/T24 and owner |

## What can proceed now
The handoff files, task/dependency checks, requirement coverage checks, local Git preparation and package integrity verification do not require pretending the blocked Android/research steps passed. A local model can start T00 in the actual folder after installation.

## Decision record template
ID / proposed by / affected requirements / alternatives / evidence URLs and inspected revisions / selected behavior / tradeoffs / approving owner response / tests to change. G0..G6 approvals must cite an actual owner response. A model cannot approve its own human gate.
