# 41 · Multiplatform port plan (Android + web now, iPhone later)

Status: Phase 0 approved; Phase 1 verification in progress · Written 2026-10-28 · Executor: Codex · Owner phone test only at the end.

## Goal

Friends with an iPhone can use the app without anyone paying Apple $99/year, and the
project stays **one codebase**. Android keeps working at every step.

Route: convert the project to Kotlin Multiplatform (KMP) with Compose Multiplatform (CMP).
The same Kotlin code builds:

| Target | Who it is for | When | Cost |
|---|---|---|---|
| Android APK | Owner + Android friends | Already exists, stays green throughout | Free |
| Web app (Kotlin/Wasm), installable to the iPhone home screen | iPhone friends | Phase 5 | Free (GitHub Pages) |
| Native iOS app | Owner / friends via TestFlight | Phase 6, only if the app sticks | Needs Xcode; $99/yr only for TestFlight |

Rejected: a separate web app or a SwiftUI app (two codebases); paying Apple now.

## Starting point (surveyed 2026-10-28)

- `:domain` — 13.6k lines of pure Kotlin/JVM, 68 test files (594 tests). JVM-only APIs:
  `java.time` in 3 files, `java.security.MessageDigest` in `backup/Backup.kt`. Uses
  kotlinx-serialization already.
- `:data` — Room 2.6.1, ~1k lines.
- `:app` — Jetpack Compose + Material 3, 536 strings in `res/values/strings.xml`, fonts in
  `res/font`, 533 MP4 demo clips + thumbs + `catalog.json` in `assets/` (14 MB).
- Android-only APIs in `:app` (29 files), grouped by what replaces them:
  - **Key-value stores** — `*Store.kt` (Profile, Routine, SavedRoutine, Mode, Onboarding,
    Suggestion, Prefs, Goal, Level, PersonalTarget), `ThemePreferences`: `SharedPreferences` via `Context`.
  - **Clip playback** — `DemoClips.kt` (`VideoView`, `MediaController`), `ExerciseThumb.kt` (`BitmapFactory`).
  - **Session cues** — `CuePlayback.kt` (`SoundPool`, `AudioManager`, `TextToSpeech`).
  - **Session keep-alive** — `WorkoutSessionService.kt` (foreground service, wake lock,
    notifications), `SessionWindowPolicy.kt` (keep screen on), `Checkpoint.kt`, `SystemClock`.
  - **Shell** — `MainActivity`, `CalisthenicsMemoryApplication`, `Theme.kt`, `BackupApply.kt`.
- Toolchain: Kotlin 2.0.21, AGP 8.13, Compose BOM 2024.10. Gradle builds locally on the Mac
  (see the local emulator memory/`toolchain.lock.md`). **Xcode is not installed.**

## Tooling facts that shape the plan (checked 2026-10-28)

- Compose Multiplatform: Android and iOS stable; **web (Wasm) is beta** since 1.9.
  Acceptable for a friends-only build; re-check status before Phase 5.
- Room: KMP for Android/iOS/JVM is stable since 2.7. **Web support only arrives in Room 3.0,
  which was in alpha** (April 2026), with gaps such as room3-paging lacking wasmJs.
  So the plan does not make web depend on Room — see Phase 2.
- iPhone Safari runs WasmGC from iOS 18.2. Older iPhones get an "update iOS" message.

## Target module layout

```
:domain   KMP library  — commonMain (all logic), jvm/android + wasmJs (+ iOS in Phase 6)
:data     KMP library  — repository interfaces in commonMain; Room on android/ios; web store on wasmJs
:shared   KMP + CMP    — all screens, theme, navigation, view models, strings, fonts, catalog
                         loading; expect/actual platform adapters (see Phase 4)
:app      Android app  — MainActivity, WorkoutSessionService, Android adapters only
:web      wasmJs app   — index.html, manifest, service worker, entry point
iosApp/   Xcode shell  — Phase 6 only
```

The rule: **only the adapters are written per platform.** Each adapter is an interface in
`commonMain` and is kept small. Everything else lives once in commonMain.

## Phases

**Owner decision (2026-10-28): run Phases 0–5 back to back without stopping for phone
tests. The owner tests on real phones only once, at the end (see "Final owner phone test").**
Nothing in Phases 0–5 waits on the owner.

Each phase still ends with automated gates before the next phase starts: Android build, all
existing domain + app tests passing, the Maestro `ui-screens` regression on the emulator, and
(from Phase 3 on) the browser smoke test below. A failing gate blocks the next phase; fix it
rather than deferring it to the phone test. One commit series per phase, tagged
`kmp-phase-N` so a phase can be reverted on its own.

### Phase 0 — Toolchain upgrade (Android only, no behaviour change)
- Kotlin 2.0.21 → current 2.x, matching Compose Multiplatform plugin; AGP as required.
- Swap androidx Compose BOM for the JetBrains `org.jetbrains.compose` plugin (Android
  output is still Jetpack Compose underneath).
- Done when: APK identical in behaviour, tests green.

### Phase 1 — `:domain` to KMP
- Convert to `kotlin("multiplatform")`; move `src/main` → `src/commonMain`, tests → `commonTest`.
- `java.time` → `kotlinx-datetime` (3 files). `MessageDigest` → `expect fun sha256(bytes)`
  (or a KMP crypto lib, e.g. `org.kotlincrypto.hash`) — **backup hashes must stay byte-identical**,
  add a test pinning an existing backup's hash.
- Add targets jvm and wasmJs. iOS targets need Xcode to build, so they are enabled in
  Phase 6; until then keep commonMain free of JVM/browser-only APIs so iOS compiles later.
- Done when: 594 domain tests pass on JVM **and** wasmJs (`wasmJsNodeTest` / browser test).

### Phase 2 — Storage behind interfaces
- Key-value stores: replace `SharedPreferences` with `com.russhwolf:multiplatform-settings`
  (Android: SharedPreferences, same file names so **existing users keep their data**; web:
  localStorage; iOS: NSUserDefaults). One `Settings` instance passed in, not `Context`.
- Room data: define repository interfaces in `:data` commonMain. Android: Room upgraded to
  current 2.x KMP, same schema + migrations (**no data loss** — add a migration test from the
  2.6.1 schema). Web: a small store saving the same `@Serializable` records as JSON in
  IndexedDB/localStorage, reusing the existing backup format. Revisit Room 3 for web once stable
  and drop the web store if it fits.
- Backup export/import (`BackupApply.kt`) moves to common code — it is also how a friend moves
  data between browsers/phones.
- Done when: Android upgrade-in-place from the current release keeps all history. This is an
  emulator test, not a phone test: install the current APK, log a workout, install the new APK
  over it, check history and settings. Script it (adb + Maestro) and keep it for later phases.

### Phase 3 — UI to `:shared`
- Move screens, theme, components, view models to `:shared` commonMain.
- `R.string` / `stringResource` → Compose Multiplatform resources (`Res.string.*`, generated);
  a script converts `strings.xml` once. Fonts (Barlow Condensed, IBM Plex Sans) and drawables
  → `composeResources`. Catalog JSON, thumbs and clips → `composeResources/files`.
- `androidx.lifecycle` ViewModel → JetBrains KMP lifecycle-viewmodel-compose.
- Material icons → `compose.materialIconsExtended` (or copy the ~30 used icons as vectors to
  keep the web bundle small).
- Test tags stay identical so Maestro flows keep working.
- `aboutlibraries` licences screen: keep Android-only behind an adapter or use its KMP artefact.
- Add a throwaway `wasmJs` entry point now (adapters stubbed) so the shared UI runs in a browser
  from this phase on.
- **Browser smoke test** (new, CI): Playwright with **WebKit** (closest engine to iPhone Safari)
  and Chromium at iPhone viewport 390×844 opens the web build, walks onboarding → Train →
  start a workout → log a set → Done, takes screenshots, and fails on console errors. Runs on
  every later phase. It stands in for the per-phase iPhone checks the owner is not doing.
- Done when: Android app is `MainActivity` + adapters only, looks/behaves the same in
  `ui-screens` screenshots, and the browser smoke test passes in WebKit.

### Phase 4 — Platform adapters (interfaces in commonMain)

| Adapter | Android (existing code moves here) | Web | iOS (Phase 6) |
|---|---|---|---|
| `ClipPlayer` composable | VideoView | HTML `<video>` via CMP HTML interop, muted + `playsinline` + loop | AVPlayerLayer via UIKitView |
| `CuePlayer` (beeps, voice) | SoundPool + TextToSpeech | Web Audio + `speechSynthesis` (unlocked on first tap — iOS rule) | AVAudioPlayer + AVSpeechSynthesizer |
| `SessionKeepAlive` | foreground service + wake lock + notification | Screen Wake Lock API | idle timer off + background audio |
| `Clock` | SystemClock.elapsedRealtime | `performance.now()` | `CACurrentMediaTime` |
| `Haptics` | Compose haptics | no-op / `navigator.vibrate` | UIImpactFeedbackGenerator |
| `Files` (backup share/open) | Intents / SAF | download + `<input type=file>` | share sheet |

Rest timer must stay correct when the phone sleeps: store the target end time, recompute on
resume (the checkpoint logic already does this on Android — reuse it in common code).

### Phase 5 — Web release for friends
- `:web` module: `index.html`, web app manifest (name, ultramarine theme colour, icons),
  service worker caching the app + clips so it works offline at the gym.
- Request `navigator.storage.persist()`; tell friends to **Add to Home Screen** (installed web
  apps on iOS are exempt from Safari's 7-day storage eviction).
- GitHub Actions job builds `wasmJsBrowserDistribution` and deploys to GitHub Pages
  (`nilscost.github.io/Calisthenics-app`). Existing Android CI unchanged.
- Extend the browser smoke test: offline reload (service worker), data still there after reload,
  backup export/import round trip, rest timer correct after the tab is hidden and shown.
- Known web limits to tell friends: cues may not sound while the screen is locked; clips load
  on first use, then come from cache.
- Deploy to Pages, but do not announce the link — the owner shares it after the phone test.
- Done when: Pages URL serves the app, all automated gates pass, and the "Final owner phone
  test" checklist below is filled in with the exact APK build and web URL to test.

### Final owner phone test (the only owner step, after Phase 5)

Prepared by the executor: a release APK in `Apps/builds/` and the Pages URL, both recorded in
`STATUS.md`. The owner then checks:

**Android (Galaxy S21), installed over the current app — not a fresh install:**
1. All existing history, routines, levels and settings are still there.
2. A full workout: clips play, rest timer + beeps + voice, screen off and on mid-rest, Done.
3. End screen, Progress tree, exercise detail, backup export.

**iPhone (Safari, iOS 18.2+), ideally on a friend's phone:**
1. Open the URL, Add to Home Screen, open from the icon.
2. Onboarding, then a full workout as above, including locking the phone during rest.
3. Close the app fully, reopen: history kept. Airplane mode: app and seen clips still work.
4. Backup export, then import it on another browser/phone.

Findings go to `docs/user-feedback.md`; fixes are a follow-up batch, not part of this plan.
After a pass, the owner shares the link with friends.

### Phase 6 — Native iPhone app (later, optional — not part of the Phase 0–5 run)
- Prerequisite: Xcode installed and selected (`xcode-select -s`).
- Enable iOS targets, add `iosApp/` SwiftUI shell hosting `ComposeUIViewController`, iOS adapters.
- Run in Simulator; owner can install on own phone with a free Apple ID (re-sign every 7 days).
- **Owner:** pay $99/yr for TestFlight only if friends use it regularly.

## Risks

- **Compose web is beta:** rendering or text-input bugs on Safari. Mitigation: a web build
  exists from Phase 3 and the WebKit smoke test runs on every phase after it. WebKit in
  Playwright is close to, but not the same as, iPhone Safari — residual issues surface in the
  final phone test.
- **Bugs found late:** with one phone test at the end, a problem may trace back several phases.
  Mitigation: per-phase tags, automated gates, and no feature work mixed into the port.
- **Data migration on Android:** biggest real risk to the owner. Mitigated by Phase 2 upgrade test.
- **Web bundle size:** Wasm + Skia ≈ several MB plus 14 MB clips; clips lazy-load and cache.
- **Behaviour drift during the move:** mitigated by unchanged test tags + Maestro screenshots
  each phase, and no feature work in the same PRs.

## Out of scope

New features, design changes, Play Store / App Store publishing, accounts or cloud sync.

## Executor notes (Codex)

Execution is delegated to Codex; this doc is the contract.

- Work on one branch `codex/kmp-port` from `main`, phases in order, tag `kmp-phase-N` at the
  end of each. Open one PR at the end of Phase 5; do not merge it — the owner merges after the
  phone test.
- Build locally on the Mac: `JAVA_HOME=$(brew --prefix openjdk@17)`, Android SDK at
  `/opt/homebrew/share/android-commandlinetools` (already in `local.properties`). Emulator AVD
  `persona` for the upgrade and Maestro tests. New dependencies may be downloaded (online Gradle).
- Commit and push as in doc 14 §2 (commit identity, token push, never print the token).
- Never claim a phone test, never fill in owner results, keep the Android app ship-ready at
  every tag.
- Do not change behaviour, design, catalog or clips. If something can't be ported 1:1, keep the
  Android behaviour, note the web difference in `docs/evidence/kmp-port.md`, and carry on.
- Stop and ask the owner only if: existing user data would be lost or changed, a gate cannot
  be made to pass, or a dependency needs a paid account.
- Record per phase in `docs/evidence/kmp-port.md`: commands run, test counts, screenshot paths.
