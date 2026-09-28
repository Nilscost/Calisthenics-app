# T01 — Reuse candidate audit (code, data and media)

Date: 2026-09-25 (UTC). Environment: Docker sandbox, ARM64, OpenJDK 21.0.12.1
(Debian), Python 3.11.15. Network state at audit time: sandbox DNS
(`192.168.65.7`) intermittently down; egress proxy (`host.docker.internal:9090`)
403s outbound hosts that are not allowlisted. See `docs/evidence/environment.md`.

All four checkouts live under `/workspace/app/research-checkouts/` and were
verified clean (`git status --short` empty) at inspection time.

## Candidates and identity

| Repo | Source | Branch | Exact HEAD | License | Platform |
|---|---|---|---|---|---|
| CalisthenicsMemory | https://codeberg.org/Gonbei774/CalisthenicsMemory.git | `master` | `045b8577d1a5c7fa75d40bf6c1df95244af616f3` | GPL-3.0 (full text in `LICENSE`) | Android app, single `:app` module |
| Ironvellum | https://github.com/AlexMollard/Ironvellum.git | `main` | `d8d51ab35113c071099e25de7842138ce9e9d82e` | GPL-3.0 (full text in `LICENSE`) | Android app, single `:app` module |
| Ballast | (see landscape-survey) | — | `a4b7d74983bd5800b95cd96a6f19a68191d68537` | see repo | **Web PWA** (Vite + React + Tailwind), not Android |
| Calistenia | (see landscape-survey) | — | **NOT INSPECTED** — checkout never materialized (clone failed before network outage; directory absent) | unknown | unknown (reported web-based in prior survey) |

Honest limits: Calistenia could not be inspected at all; Ballast is a web app,
so it is not a buildable Android foundation and is only relevant as a source of
data/ideas. Neither was treated as a viable base.

## Toolchains (from each repo's own `gradle/libs.versions.toml` + wrapper)

| | CalisthenicsMemory | Ironvellum |
|---|---|---|
| AGP | 8.13.0 | 9.4.0 |
| Kotlin | 2.0.21 | 2.4.20 |
| Gradle wrapper | 8.13 | 9.7.1 |
| Compose BOM | 2024.09.00 | 2026.09.00 |
| Room | present | 2.8.5 |
| compileSdk / target / minSdk | 35 / 35 / **26** | 37 / 36 / **29** |
| Cloud dependency | **none (fully local)** | **Supabase BOM + auth baked in** (`app/build.gradle.kts` L217-218; publishable key shipped in APK per repo comment L11) |

S21 (Android 12-13, API 31-33) satisfies both minSdks, so neither is excluded
on device grounds.

## CalisthenicsMemory — spec-relevant implementation evidence

- **Timer service (real)**: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt`
  (141 lines). `Service` with `ACTION_START/ACTION_STOP`, `startForeground()`
  notification, and a `PowerManager.PARTIAL_WAKE_LOCK`
  (`"CalisthenicsMemory::WorkoutTimerService"`). Manifest declares
  `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` and
  `foregroundServiceType="specialUse"` (AndroidManifest L6-8, L37-38).
  Note: the *timing logic* currently lives in `WorkoutScreen.kt`; the service is
  the keep-alive layer. The spec (RUN-01) wants single service-owned timing —
  this is the main structural refactor, not a missing capability.
- **Audio cues (real, offline)**: `app/src/main/java/.../util/SoundPlayer.kt`
  (43 lines) — `SoundPool` loading `R.raw.beep_short`, `R.raw.start_cue`,
  `R.raw.set_complete`. Satisfies AUD-01/AUD-02 in spirit; T04 must re-prove on
  S21 with screen locked and competing audio.
- **Interval/timed program model (real)**: Room entities
  `IntervalProgram.kt` (15 lines), `IntervalProgramExercise.kt` (35 lines),
  `Program.kt`, `ProgramExercise.kt`, `ProgramLoop.kt` — exercise + per-block
  timing structure matching "timed circuits with repetition targets inside
  timed blocks".
- **Progression**: exercises carry a `sortOrder` level 1..10
  (`CreateScreen.kt` L810 `selectedLevel ... coerceIn(1, 10)`), displayed via
  `R.string.level_format` in Workout/ToDo/IntervalEdit screens. This is a
  manual per-exercise level ladder — **not** the spec's automatic assumed-met
  progression, five-star display or family discomfort hold (PROG-01..06 are
  still to be implemented in T12/T13/T18).
- **No skill/prerequisite tree** (grep for prerequisite/skillTree: no hits).
- **Persistence**: Room, local only. No retrofit/okhttp/supabase anywhere in
  `app/build.gradle.kts`.

## Ironvellum — spec-relevant implementation evidence

- **"Service" is a notification mirror, not a timer**:
  `app/src/main/kotlin/com/ironvellum/app/WorkoutSessionService.kt` (152 lines)
  — ongoing notification that *observes* Room (`observeSessionSets`) and shows
  "Next: X · set N". No `WakeLock`, no countdown, no audio.
- **No timer engine anywhere**: grep for `CountDownTimer|tick(|remainingMs|
  startMs|SystemClock` across `app/src/main` finds only UI `repeat()` loops and
  idle-time accounting — no per-second session timer exists.
- **No audio at all**: zero hits for `TextToSpeech|AudioManager|MediaPlayer|
  SoundPool|Vibrator|requestAudioFocus` in `app/src/main`.
- **Set/rep tracker model**: sessions are lists of `SessionSet` with reps or
  hold-seconds and optional `weightKg` (weight-column present). This is a
  strength logging app, not a guided timed-circuit player.
- **Skill tree (real, large)**: `app/src/main/kotlin/com/ironvellum/app/
  domain/Skills.kt` (704 lines) + `ui/titles/SkillTree.kt` — 106-technique
  graph with prerequisites/titles. This is the strongest reusable *data model*
  in either repo, but the techniques are its own content (license/curation
  review required; no media).
- **Cloud**: Supabase auth/URL/key are compile-time baked (see toolchain table)
  — conflicts with PLAT-02 local-only/offline-first and must be stripped if
  reused.
- **Toolchain risk**: AGP 9.4 / Kotlin 2.4.20 / Gradle 9.7.1 / compileSdk 37 is
  far ahead of stable tooling I can currently provision in this sandbox.

## Build attempt (T01 step 3) — actual result

Preferred candidate CalisthenicsMemory, isolated copy at
`/workspace/app/research-checkouts/cm-build-attempt/` (HEAD
`045b8577d1a5c7fa75d44af616f3`, `local.properties` → `sdk.dir=/opt/android-sdk`,
JDK 21):

```
# 2026-09-25 (first attempt, network down):
$ ./gradlew assembleDebug --no-daemon --console=plain
Downloading https://services.gradle.org/distributions/gradle-8.13-bin.zip
Exception in thread "main" java.net.UnknownHostException: services.gradle.org
...
GRADLE_EXIT=1

# 2026-09-28 (network restored, same isolated copy, build script unmodified):
$ ./gradlew assembleDebug --no-daemon --console=plain
BUILD SUCCESSFUL in 2m 9s (42 actionable tasks)
GRADLE_EXIT=0  → app/build/outputs/apk/debug/app-debug.apk (14,673,181 bytes)

$ ./gradlew testDebugUnitTest --no-daemon --console=plain
BUILD SUCCESSFUL in 29s  → 13 test classes, 209 tests, 0 failures, 0 errors

$ ./gradlew lintDebug --no-daemon --console=plain
BUILD FAILED (749 pre-existing upstream style errors; not build-breaking)
```

Two environment-only fixes were required in this ARM64 Docker sandbox — no
candidate source was changed (details: `docs/evidence/t01-build.md`):
(1) AGP's x86-64 `aapt2` binary runs under `qemu-user-static` once an x86-64
glibc loader is installed (`dpkg --add-architecture amd64 && apt-get install
libc6:amd64`); (2) `sqlite-jdbc 3.41.2.2` (transitive via Room) bundles no
`Linux/aarch64` native, so Room's KSP `DatabaseVerifier` failed — the aarch64
`libsqlitejdbc.so` from `sqlite-jdbc 3.45.3.0` was placed in
`/usr/lib/aarch64-linux-gnu` (a path the JDBC loader searches).

## Recommendation

**Fork/adapt CalisthenicsMemory** (`045b8577…`) as the foundation, keeping
Ironvellum's `Skills.kt` skill-graph data model as a reference (not a code
import — both are GPL-3.0 so importing code carries GPL obligations; see ADR).

Why CalisthenicsMemory over Ironvellum:
1. It is the only candidate with a real timed-workout runtime: foreground
   service + WakeLock + SoundPool cues (spec core: timed circuits,
   locked-screen audio, offline).
2. Fully local data model — no cloud to strip.
3. Toolchain (AGP 8.13/Gradle 8.13/Kotlin 2.0.21/minSdk 26) is provisionable
   and stable; Ironvellum needs AGP 9.4/compileSdk 37.
4. Interval program entities already model timed blocks.

What must be built regardless of base (spec, not present in either repo):
automatic assumed-met progression (PROG-01..06), five-star display, discomfort
hold + explicit clearance, stretch allocator with no passive-rest relabeling
(STR-01..05), deterministic duration-budget planner (PLAN-01..06), session
reducer (RUN-01), portable backup (DATA-02), demo packs (T21).

## G0 decision request for the owner

1. Approve fork/adapt of **CalisthenicsMemory @ 045b8577** (GPL-3.0) as the
   foundation — accepting that the resulting app is GPL-3.0 (or a stricter
   compatible choice) and that upstream attribution stays in
   THIRD_PARTY_NOTICES.md.
2. Confirm Ironvellum's `Skills.kt` may be *referenced* (read) for the skill
   graph design; importing its code would add GPL file-level obligations —
   recommend designing our own graph from reviewed content instead (T06).
3. If the owner would rather have a permissive-licensed base, the fallback is
   **native Kotlin from scratch** (T03 scaffold) — slower, and the timer/audio
   spike (T04) still gates everything.

No code from either repo has been copied into the project yet.
