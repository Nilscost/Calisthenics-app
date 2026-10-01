# Full-version roadmap (owner-approved scope: full version, 2026-10-01)

Replaces nothing in `03-workplan.md` (T00–T26 stay the detailed task contracts).
This file groups them into milestones you can see and test, adds the fixes found
in `docs/evidence/review-2026-10-01.md`, and adapts device testing to a phone
**without a working USB port** (wireless only).

Status today: T00–T03 done, G0 accepted. Everything below is PLANNED.

## Ground rules (unchanged)
- One task at a time, tests first, real command output recorded in `docs/evidence/`.
- A model never approves a gate (G1–G6). Only the owner does.
- Every milestone ends with something the owner can install and try.

## How the phone is reached without USB
Android 11+ has **Wireless debugging** (adb over Wi-Fi). The S21 supports it.
1. Phone and Mac on the same Wi-Fi.
2. Phone: Developer options → Wireless debugging → ON → "Pair device with pairing code".
3. Mac: `adb pair <ip>:<pair-port>` (enter the 6-digit code shown), then
   `adb connect <ip>:<port>` (port shown on the Wireless debugging screen), `adb devices`.
4. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Limitation: airplane mode turns Wi-Fi off, which drops the connection. Therefore every
on-device test must be **startable and exportable from inside the app** (buttons + Android
share sheet), never dependent on adb during the run. Fallback with no adb at all: send the
APK to the phone (Drive/email), allow "Install unknown apps" for that app, tap to install.

## Signing key (do early — protects your data)
Android only lets an update replace an installed app if both were signed with the **same
key**. The sandbox and the Mac each create their own throwaway debug key, so switching
build machines forces an uninstall = **all workout history lost**. Fix in M0: one project
debug keystore stored outside Git (`~/.android-keys/calisthenics-debug.jks`), referenced
from `~/.gradle/gradle.properties`. A separate release key comes in M9.

---

## M0 — Clean-up and phone-test harness without USB  (agent, ~1 day)
- Housekeeping: duplicate `config/config` removed (done 2026-10-01), versionName `0.1.0-t04`,
  stale `local.properties` removed (done), shared debug keystore (above).
- Unify G1 pass thresholds: **each cue ≤ 1 s late, total ≤ 2 s** (spec A02) in runbook,
  ADR 0003 and `SpikeRuntimeFeasibilityTest`.
- Spike fixes: wait for `SoundPool` load before t=0; make the service `exported=false`
  (no longer needed — started from the app).
- **New debug screen "G1 runtime test"** (debug builds only): buttons *Smoke test (15 s)*,
  *Full test (45 min 40 s)*, *Stop*, *Share results* (CSV + summary via share sheet), live
  display of last run's numbers, and a check that notifications/battery are set correctly.
- Add to the spike (owner approved 2026-10-01): **offline spoken English** cue via Android
  TextToSpeech with a preflight that reports if the offline English voice is missing, and
  **music ducking** (request transient audio focus "may duck" around each spoken cue, release
  after). Log per cue: spoken OK / failed, focus granted / denied.
- Evidence: unit tests + lint + APK build on the Mac (`bash scripts/verify.sh`).
- Owner check: install wirelessly, open the debug screen, run the smoke test.

## M1 — G1: Can the phone run a locked-screen workout?  (owner, ~1h15)
Airplane mode on, music playing, another app open, start *Full test*, lock the phone, receive a
test call at ~10 min. Afterwards: *Share results* → fill `docs/evidence/t04-s21-runtime.md`.
**Owner decides G1.** Fail → stop, revise ADR 0003, re-test. Pass → delete the spike, M2.
Record Android + One UI version.

## M2 — Foundations: data model, safe storage, content rules  (agent; T05, T07, T06)
- T05 domain model (exercises, equipment, plans, sessions; frozen plan snapshots).
- T07 database — **mandatory change**: `exportSchema = true`, commit schema files, real
  migration tests, remove `fallbackToDestructiveMigration()` (today an update can wipe history).
- T06 content validator + first real starter pack records.
- Owner check: none new on phone beyond "app still opens, nothing lost after update".

### Parallel content track (starts with M2, runs to M7)
The biggest non-code dependency. For each starter exercise: instructions, 5 tiers,
equipment tags, source, and a **short loop demo** (owner-approved style: light, generated,
muted, loopable). Agent drafts records and generates loops; **owner reviews each exercise**
in batches (~10 at a time) — a reviewed record is the only thing that ships as real content.

## M3 — The planner: "what's my workout today?"  (agent; T08, T09, T10, T11 → G2)
Equipment profiles (Home, Travel = chair only), familiar routine + focuses + optional skill
goal, the 10–90 min time-fitting planner, stretching that fills recovery time, left/right balance.
- Owner check / **G2**: a preview screen showing generated workouts for 45 min / 20 min /
  upper-body / Travel, stretching on/off, with time breakdown and explanations.

## M4 — Progression and feedback  (agent; T12, T13 → G3)
Optional feedback (below/met/above, discomfort), "assumed met" labelling, stars per variation,
automatic slow progression (3 sessions over ≥7 days), discomfort hold, 14-day re-entry.
- Owner check / **G3**: a simulated multi-week history replay showing each star/difficulty
  change with its reason.

## M5 — First real hands-free workout  (agent; T14, T15, T16, T17 → G4)
Session engine (pause/skip/easier/recover after crash), the production timer service from
ADR 0003 (spoken + beep cues, ducking, call pause), onboarding + Today/preview, active
workout screen, end-of-session feedback.
- Owner check / **G4**: do a full real workout with the phone locked, then try every control.
  **This is the first point the app replaces your current timer.**

## M6 — Everything around the workout  (agent; T18, T19, T20 → G5)
Skill tree + exercise library, history + weekly summary + editing old feedback, backup
export/import (to a file you choose, e.g. Drive).
- Owner check / **G5**: export, wipe the app, restore, compare.

## M7 — Downloadable demo packs  (agent + owner decision; T21)
Extra exercise packs downloaded before a workout, never during. **Needs a hosting decision**
(recommended: GitHub Releases of your own repo — free, versioned). Blocked until decided.

## M8 — Hardening and polish  (agent + owner on phone; T22, T23)
Interruptions (calls, Bluetooth disconnect, app killed, reboot), airplane mode from cold start,
large fonts / TalkBack / contrast, battery and heat over a full session.
- Owner check: interruption checklist on the S21.

## M9 — Pilot and release  (T24, T25 → G6, T26)
CI checks, licence review (app becomes GPL-3.0 because of the fork), clean-clone build,
**release signing key** (backed up by you outside Git), 2–3 weeks of normal personal use
(Home, short, Travel sessions), fix issues, **G6**, then installable APK + optional GitHub repo.

---

## Rough effort (honest guess, not a promise)
Agent work dominates M2–M6. Expect roughly **6–10 weeks** of calendar time at the past pace,
plus the pilot (2–3 weeks). Biggest schedule risks: G1 failing on Samsung battery management,
content review volume, and build-environment friction.

## Owner actions summary
| When | What |
|---|---|
| Now | Install Android Studio on the Mac; enable Developer options + Wireless debugging on the S21; pair once |
| M0 end | Install the test build wirelessly, run the smoke test |
| M1 | 1h15 locked-phone test, share results, decide G1 |
| M2→M7 | Review exercise batches (instructions + demo loop) |
| M3/M4/M5/M6 | Gates G2, G3, G4, G5 (each ~15–60 min) |
| M7 | Decide where demo packs are hosted |
| M9 | Keep the release key backup safe; 2–3 week pilot; G6; decide on public GitHub |

## Open decisions (ask when the milestone starts, not now)
- M7: pack hosting location.
- M9: public open-source repo or private; app name/icon.
- Build location for agent work: Mac (needs Android Studio) vs. Hermes sandbox
  (needs `hermes egress start` + Android SDK install inside the container).
