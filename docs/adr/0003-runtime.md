# ADR 0003 — Session runtime: foreground-service timer ownership for the Galaxy S21

Status: **PROPOSED — pending G1 (S21 owner session, T04).** No device evidence yet.
Date: 2026-09-30 (proposed).
Affected requirements: RUN-01, PLAT-02, AUD-01, AUD-02 (docs/01 § runtime; docs/03 T04/T15).
Evidence to attach at G1: `docs/evidence/t04-s21-runtime.md` (timing CSV, logcat, screenshots).

## Context

The spec requires a 10–90 minute timed workout that keeps running with the
screen locked, in airplane mode, with another app foregrounded, and keeps
delivering audible cues (A02, A03). The fork's starting point
(CalisthenicsMemory @ `045b8577`) has two runtime-relevant facts that shape
this decision:

1. **Timer ownership is in the UI.** The countdown and cue scheduling live in
   the Compose `*ExecutionScreen`; `WorkoutTimerService` only acquires a
   `PARTIAL_WAKE_LOCK` and shows a foreground notification. Whether a
   UI-owned timer survives "locked + another app foregrounded" is exactly the
   question A02 must answer empirically — it cannot be assumed either way.
2. **The device target is a Galaxy S21** (Android 13/14, One UI) while the
   build targets `targetSdk 35` (Android 15). The relevant platform rules:

   - **Android 14 (targetSdk 34+)**: a foreground service started while the
     app is in the background must declare a `foregroundServiceType` and
     `Service.startForeground(id, notification, type)` must be called with
     that type, or the process throws
     `MissingForegroundServiceTypeException` and the timer dies on start.
     (Verified: developer.android.com foreground-service-type docs.)
   - **Android 15 (targetSdk 35)**: `specialUse` is the only FGS type that
     fits a workout timer; it requires the
     `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` manifest property (a Play/owner
     justification string) and, on the device, the
     `POST_NOTIFICATIONS` runtime permission must be granted or the
     foreground notification is suppressed — which on Samsung One UI makes
     the battery manager far more likely to kill the service.
   - **Audio**: cues must play offline (bundled `res/raw` sounds — no
     TTS/network). A `SoundPool` with `USAGE_MEDIA` plays through the
     notification/media path and coexists with the user's music; it does not
     require audio focus (cues are short and non-preemptive by design — A03
     checks they are audible *with* music playing).

## Decision (proposed)

1. **Single service-owned timer (RUN-01).** Move countdown and cue
   scheduling into the foreground service (a dedicated timer loop on a
   background thread), keeping `WorkoutTimerService` as the one runtime
   owner. The UI renders state but does not own time. This is the
   architecture A02 most wants to survive, because the service — not the
   composition — keeps running when locked.
2. **Monotonic time base.** All timing derives from
   `SystemClock.elapsedRealtime()` (monotonic, immune to wall-clock/NTP
   changes), not `System.currentTimeMillis()`. The T04 spike logs
   expected-vs-observed per cue so drift is measured, not guessed.
3. **Foreground service, `specialUse` type.** The service is started with
   `startForeground(id, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)`,
   manifest declares `FOREGROUND_SERVICE_SPECIAL_USE` + the subtype property
   + `stopWithTask="false"`, and the app requests `POST_NOTIFICATIONS` on
   first run so the visible persistent notification is shown.
4. **Wake lock + no silent-media hack.** A `PARTIAL_WAKE_LOCK` (bounded to
   the session length) keeps the CPU awake for the timer thread. We rely
   only on documented mechanisms; no doze-exemption tricks, no silent-media
   abuse. If the service is killed in the tested state, that is the
   evidence and the ADR is revised (STOP rule for G1).
5. **Offline cues via bundled raw sounds.** `SoundPool` loads from
   `res/raw` (start cue, beep, set-complete). No network, no TTS. Cues are
   short sonifications that do not take audio focus, so they remain audible
   over the user's music (verified on-device in A03).

## T04 spike (this ADR's evidence generator)

`spike/SpikeSessionService` (in `:app`) is a **disposable** foreground
service that:

- runs a fully service-owned schedule of synthetic work/rest blocks
  (service defaults: 3 blocks × 3 s work / 2 s rest — short for a smoke run;
  the G1 run on the S21 uses 110 blocks × 15 s work / 10 s rest = 45 min 40 s,
  matching A02's synthetic 45-minute schedule; overridable via intent extras);
- logs every boundary as
  `run_tag,block_index,phase,expected_elapsed_ms,observed_elapsed_ms,delta_ms`
  to `getExternalFilesDir/spike/spike_<t0>_<tag>.csv` using
  `elapsedRealtime()`;
- plays an audible cue per boundary via `SoundPool`;
- holds a `PARTIAL_WAKE_LOCK`;
- shows a foreground notification (channel `spike_session_channel`) with a
  **Stop** action and a completion marker file (`spike_done.txt`).

`SpikeRuntimeFeasibilityTest` (androidTest) starts it, waits for the marker
(30 s budget), and asserts every planned cue fired with max |drift| < 4 s.
**A timeout is itself valid G1 evidence** (service not kept alive in that
state). See `spikes/android-session-runtime/README.md` for the full runbook.

## What G1 must establish (STOP rules)

| Check | Pass criterion | If it fails |
|---|---|---|
| A02 liveness (locked, other app FG, airplane) | Every cue fires; no missed boundary; total within schedule ± 60 s underfill tolerance | Runtime strategy does not hold → revise ADR (service type, wakelock, or move to a WorkManager/notification-alarm fallback) and re-spike |
| Cue timing accuracy | Max |delta| per boundary < 5 s (the spec's transition budget) for the full synthetic run | Drift > 5 s → monotonic base is wrong or the timer thread is being throttled; inspect |
| A03 audio with music | Cue audible while music plays, no crash on focus loss/gain | Adjust `AudioAttributes` usage (USAGE_MEDIA) or cue scheduling |
| Notification visibility | Foreground notification shown (POST_NOTIFICATIONS granted) and survives lock | Grant flow is wrong; Samsung may then kill the service — retest after grant |
| No network used | Airplane mode throughout; nothing touches the network | N/A (spike is offline by construction) |

## Consequences

- T15 (production runtime) implements exactly this ADR if G1 passes; the
  spike is then discarded (per T04 step 4) but its findings + tests are
  retained.
- `WorkoutTimerService` gains the 3-arg `startForeground(..., type)` call
  and the manifest gains `POST_NOTIFICATIONS` — already applied in this
  change so the app does not crash on start on the S21 (targetSdk 34+ rule).
- Owner action required at G1: grant POST_NOTIFICATIONS on first launch,
  disable "Sleeping apps" / set the app to "Unrestricted" battery use on the
  S21 (One UI), and run the full-length synthetic session with the screen
  locked. See `docs/evidence/t04-s21-runtime.md` for the exact steps.

## Rejected alternatives

- **Keep the UI-owned timer**: rejected — cannot be proven to survive the
  locked + background state; that is precisely what A02 must rule out/in.
  If the service-owned timer fails G1, *this* becomes the fallback to test.
- **WorkManager periodic notifications**: rejected as the primary mechanism
  — 15-minute granularity cannot drive per-second cues; viable only as a
  liveness backstop.
- **AlarmsManager `setExactAndAllowWhileIdle` per boundary**: rejected —
  alarm batching under Doze makes per-second cue timing unreliable; the FGS
  + wakelock path is the documented mechanism for a continuously-running
  session.
