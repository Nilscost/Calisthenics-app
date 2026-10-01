# T04 spike — Android session runtime feasibility (Galaxy S21)

Disposable probe for **G1**: does a **foreground-service-owned timer** survive
`locked screen + another app foreground + airplane mode` on the S21, with
audible cues on time? The service is in `:app`'s **main** source set (already
inside `app-debug.apk`) and is `exported="true"` so it can be started via `adb`
without the androidTest APK. **Delete the spike (and its manifest entry) before
T15** — nothing here is production content.

- Service: `app/src/main/java/.../spike/SpikeSessionService.kt`
- Instrumented driver (runs on Mac, not needed for the S21 session):
  `app/src/androidTest/java/.../spike/SpikeRuntimeFeasibilityTest.kt`
- Decision: `docs/adr/0003-runtime.md` (PROPOSED until this session's evidence)
- Evidence template: `docs/evidence/t04-s21-runtime.md`

## What the spike does

On `ACTION_RUN` it:

1. calls `startForeground(id, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)`;
2. acquires a bounded `PARTIAL_WAKE_LOCK`;
3. schedules N synthetic blocks (work/rest) on a single background thread using
   `SystemClock.elapsedRealtime()` (monotonic);
4. plays an offline cue (`res/raw/start_cue.ogg` via `SoundPool`, `USAGE_MEDIA`)
   at every boundary;
5. appends one CSV row per boundary:
   `run_tag,block_index,phase,expected_elapsed_ms,observed_elapsed_ms,delta_ms`
   to `Android/data/app.calisthenics.personal/files/spike/spike_<t0>_<tag>.csv`;
6. writes `spike_done.txt` (marker) and stops the foreground notification when
   the schedule ends, or immediately on `ACTION_STOP` (also available as the
   "Stop spike" action in the notification).

## S21 session — step by step

### 0. One-time prep (≈10 min)

```bash
# from the Mac project folder, with the S21 connected and USB debugging on
./gradlew :app:assembleDebug          # produces app/build/outputs/apk/debug/app-debug.apk
adb devices                           # expect: <serial> device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

1. Open the app once (so it registers) and **grant the "Post notifications"
   permission** when asked (Settings → Apps → Calisthenics → Notifications →
   allow). On Android 13+ a foreground service whose notification is suppressed
   is a much easier kill target on Samsung.
2. Battery: Settings → Battery → Calisthenics → **Unrestricted** (or at minimum
   disable "Put unused apps to sleep" for it). Record which battery setting you
   used — A02 says "run once on normal battery settings and record them".
3. **Airplane mode ON.**
4. Optional but recommended: start a music/podcast app you'll leave running
   (for A03) — Spotify/YouTube Music/any local player.

### 1. Short smoke run first (35 s, screen ON — sanity check only)

```bash
adb shell am start-foreground-service \
  -n app.calisthenics.personal/io.github.gonbei774.calisthenicsmemory.spike.SpikeSessionService \
  --es spike.tag smoke --ei spike.blocks 3 --el spike.workMs 3000 --el spike.restMs 2000
```

You should hear **5 short cues** (3 work-starts + 2 rests; cues at t=0/3/5/8/10 s,
run ends at t≈13 s), see the "T04 spike
running (smoke)" notification. Stop it:

```bash
adb shell am start-foreground-service \
  -n app.calisthenics.personal/io.github.gonbei774.calisthenicsmemory.spike.SpikeSessionService \
  --es spike.tag smoke-stop \
  -a io.github.gonbei774.calisthenicsmemory.spike.STOP
```

If the notification never appears or no cues play, stop here and capture
`adb logcat -d | grep -iE "spike|SpikeSession|SecurityException|Foreground"`
into the evidence file.

### 2. THE A02 run — 45-min synthetic schedule, locked, other app foreground

This is the G1 measurement. Params: 110 blocks × 15 s work + 10 s rest =
110×15 + 109×10 = 1650 + 1090 = **2740 s = 45 min 40 s** — the synthetic
45-minute schedule A02 asks for.

1. Open a **second app** (e.g. a browser or notes) and leave it foregrounded.
2. Start the music app playing (for A03 overlap; volume ~half).
3. Start the spike:

   ```bash
   adb shell am start-foreground-service \
     -n app.calisthenics.personal/io.github.gonbei774.calisthenicsmemory.spike.SpikeSessionService \
     --es spike.tag a02-45min --ei spike.blocks 110 --el spike.workMs 15000 --el spike.restMs 10000
   ```
4. **Lock the screen** (`adb shell input keyevent KEYEVENT_POWER`) and leave it
   locked the whole ~46 minutes. Keep the S21 on charger (battery setting:
   Unrestricted, recorded in step 0).
5. During the run, at a couple of random minutes, note from the notification
   shade (or the music app's "now playing") that the service is alive and the
   music is still playing. **Do not unlock.**
6. At ~10 min in, for the **A03 sub-check**, place a test call to the S21, let
   it ring out (answer briefly or not — record which), then let it return to
   locked+music. The spike service has no pause/resume API (it's a probe) —
   what you're observing is whether the *OS kills it* under the call
   interruption. Note whether the service survived and whether cues kept
   playing (you'll hear the next boundary cue when you check the log).

   If you want the full A03 pause/resume semantics, that is a T15
   production-runtime check, not part of this spike — record it as such.
7. After ~46 min the service self-stops and writes `spike_done.txt`.

### 3. Collect evidence

```bash
adb pull /sdcard/Android/data/app.calisthenics.personal/files/spike ./t04-s21-pull/
ls t04-s21-pull/
cat t04-s21-pull/spike_done.txt
# timing analysis
python3 - <<'EOF'
import csv, glob, math
f = sorted(glob.glob('t04-s21-pull/spike_*.csv'), key=lambda p: __import__('os').path.getmtime(p))[-1]
rows = list(csv.reader(open(f)))[1:]
b = [r for r in rows if r and not r[0].startswith('TOTAL')]
deltas = [int(r[4]) for r in b]
absd = [abs(x) for x in deltas]
print(f"file={f}")
print(f"boundaries={len(deltas)}  maxAbs={max(absd)}ms  mean={sum(deltas)/len(deltas):.0f}ms  p95={sorted(absd)[int(0.95*len(absd))]}ms")
bad = [x for x in deltas if abs(x) > 1000]
print(f"outside ±1s: {len(bad)} -> {bad[:10]}")
EOF
```

### 4. Record it

Copy the numbers into `docs/evidence/t04-s21-runtime.md` and attach the CSV
path + `spike_done.txt` contents + the exact battery/notification/music settings
used. Screenshots: notification shade with the spike notification, battery
settings page, and the airplane-mode toggle.

## Pass criteria (G1) — from A02/A03

| Check | Pass | Evidence |
|---|---|---|
| Every boundary fired | 219 work/rest cues (110 work + 109 rest) + TOTAL row in CSV, none missing/duplicated | CSV row count |
| Cue timing | Each start cue within **1 s** of its boundary (`abs(delta_ms) ≤ 1000`); final within **2 s** of planned | python analysis above |
| Offline | Airplane mode whole run; no network calls (spike is offline by construction) | airplane toggle screenshot |
| Notification visible | Foreground notification shown throughout (POST_NOTIFICATIONS granted) | shade screenshot |
| A03 (observed) | Cues audible with music playing; note whether the call killed the service | notes + logcat if killed |

**If any check fails: G1 fails.** Record the exact failure (e.g. "service died
at t=12:40, `delta` shows 3 missing cues") and update `docs/adr/0003-runtime.md`
with the revised strategy (or the fallback from the ADR: UI-owned timer test /
WorkManager backstop). Do **not** paper over a miss with "it felt fine".

## Notes / known limitations

- The spike has **no pause/resume** — A03's "call pauses workout, focus
  restoration does not resume, owner resumes with remaining time preserved"
  is a T15 production check. Here we only record whether the OS keeps the
  service alive across a call.
- `start-foreground-service` from `adb` is exempt from the Android 14
  background-FGS start restriction; the *app itself* starting the production
  service from the background will need the documented exemptions — that is
  exactly the risk the ADR flags for T15.
- The instrumented driver (`SpikeRuntimeFeasibilityTest`) exists as the
  automated short probe (3×1.5 s) and to keep the test APK exercising the
  spike; it is **not** required for the S21 session.
- Deleting the spike: remove `app/src/main/java/.../spike/`,
  `app/src/androidTest/java/.../spike/`, the two `spike` manifest lines, and
  this folder.
