# T04 S21 runtime feasibility — G1 evidence

Status: **PENDING S21 SESSION** (owner runs `spikes/android-session-runtime/README.md`).
Device: Samsung Galaxy S21 — Android version: `______`, One UI: `______` (fill at session).
Build tested: `app-debug.apk` versionName 0.1.0-t03, SHA-256: `______`.
Date: `______`.

## Settings recorded (A02 requires normal battery settings, recorded)

- Airplane mode: ON for whole run (yes/no)
- Battery setting for app: `Unrestricted | Adaptive | ____` (screenshot: `______`)
- POST_NOTIFICATIONS: granted (yes/no) (screenshot: `______`)
- Other app foregrounded before lock: `______` (which app)
- Music app + track: `______`, volume: `______`

## Run 1 — A02 (45-min synthetic, locked, other app FG, airplane)

Params: **110 blocks × 15 s work + 10 s rest** → 110×15 + 109×10 = 2740 s
= **45 min 40 s**. Planned boundaries: **219** (110 work-starts + 109 rest-starts).

```bash
adb shell am start-foreground-service \
  -n app.calisthenics.personal/io.github.gonbei774.calisthenicsmemory.spike.SpikeSessionService \
  --es spike.tag a02-45min --ei spike.blocks 110 --el spike.workMs 15000 --el spike.restMs 10000
```

| Metric | Value | Pass? |
|---|---|---|
| Planned boundaries | 219 (110 work + 109 rest) | — |
| Boundaries observed in CSV | `______` | 219 = pass |
| Missing/duplicated cues | `______` | 0 = pass |
| max abs delta (ms) | `______` | ≤ 1000 = pass (A02: start cue within 1 s) |
| p95 abs delta (ms) | `______` | — |
| mean delta (ms) | `______` | — |
| TOTAL row observed (ms) | `______` | ≈ 2740000 |
| TOTAL row delta (ms) | `______` | ≤ 2000 = pass (A02: final within 2 s) |
| Service alive at 10-min check (notif shade) | yes/no | yes = pass |
| Service alive at 30-min check (notif shade) | yes/no | yes = pass |
| Airplane mode maintained | yes/no | yes = pass |

CSV file: `t04-s21-pull/spike_______a02-45min.csv`
`spike_done.txt`:
```
(paste)
```

## Run 2 — A03 (other media + call), observed

Music playing during the A02 run (same run). At ~10 min: test call to S21.

| Check | Observed | Note |
|---|---|---|
| Cue audible while music playing (A03) | yes/no | which music app |
| Call interruption: service killed? | yes/no | if yes, logcat excerpt below |
| Cues resumed after call? | yes/no | probe has no pause/resume — full A03 semantics are a T15 check |
| Headphone disconnect (if tested) | n/a or result | |

Logcat excerpt (if service died):
```
(paste `adb logcat -d | grep -iE "spike|SpikeSession|Foreground"` around the kill)
```

## Screenshots

- [ ] Airplane mode ON
- [ ] Battery settings page (app = Unrestricted)
- [ ] Notification permission granted
- [ ] Notification shade during run (spike notification visible)
- [ ] Music app "now playing" during run

## Verdict

- [ ] **G1 PASS** — all A02 checks pass; A03 observed and recorded. Proceed to T05.
- [ ] **G1 FAIL** — record exact failure(s) above. ADR 0003 is revised per its
      fallback table before T05 starts. STOP until resolved or owner approves a
      changed requirement (per A02).

## Follow-ups

- [ ] Delete spike (`spikes/android-session-runtime/`, `spike/` source + test dirs,
      manifest entries) after G1 is recorded — T04 step 4.
- [ ] Update `docs/08-traceability.md` T04 row with the evidence path.
