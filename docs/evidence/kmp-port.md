# Multiplatform port evidence

## Starting point

The owner selected the latest APK posted on 2026-10-27: `Apps/builds/app-debug-2026-10-27-phone-feedback.apk`, application ID `app.calisthenics.personal`, version `0.11.5-r8` (19), SHA-256 `e1619feafcb12cafee85828e1196a2cf48f5666c3d885174d9ee9f855096853d`. Its signing certificate SHA-256 is `8638d15f7e5327d0a4f3197dad470e70b1425dfcb00a6bfc4bbcdc96a63bd940`.

The branch starts at `173ddcea`, whose tree is identical to `da410c39`; the APK was written immediately after that commit and all 1,070 packaged asset paths match this tree byte for byte. There is no explicit build manifest tying the APK to the commit, so this attribution is corroborated rather than proven. This owner-selected baseline supersedes the plan's older `main` base.

## Phase 0 — in progress

Toolchain selection: Kotlin 2.4.21, Compose Multiplatform 1.12.2, stable Material 3 1.9.0, KSP 2.3.12, AGP 9.1.1 and Gradle 9.3.1, Java 17. AGP 9.1 includes the R8 version required by Kotlin 2.4; the standalone R8 9.1.29 POM was unavailable in Google Maven. The Android Kotlin compiler is explicitly aligned with the Kotlin plugins. Compose requires app compileSdk 37; targetSdk 35 and minSdk 26 are unchanged. CI provisions platforms 35 and 37 and build-tools 36 while retaining its existing checks. Room 2.8.5 was brought forward from Phase 2 because Room 2.6.1 cannot process KSP2 Unit signatures; compiler-only upgrade failed due to a missing runtime type. The committed version-24 schema did not change. See [KSP issue 2177](https://github.com/google/ksp/issues/2177). Sources: [Kotlin compatibility](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html), [Compose compatibility](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html), [Android Kotlin support](https://developer.android.com/build/kotlin-support), [AGP 9.1](https://developer.android.com/build/releases/agp-9-1-0-release-notes), [AGP built-in Kotlin](https://developer.android.com/build/releases/agp-9-0-0-release-notes#runtime-dependency-on-kotlin-gradle-plugin), [KSP releases](https://github.com/google/ksp/releases).

Baseline verification attempted before changes: interrupted during dependency downloads; no pass is claimed. Incremental verification `:domain:test :data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` passed on the Mac with the shared debug keystore; clean verification and screenshot results pending.

A signed install-over check on the `persona` emulator (temporary `-read-only` overlay) installed the owner-selected APK, seeded eight sessions using the debug receiver, installed the new APK with `adb install -r`, and launched it. Both APK certificates have the digest above. All 16 database tables (row contents) and all six preference files matched before and after the install. Captures and comparison script are `/tmp/kmp-upgrade-before`, `/tmp/kmp-upgrade-after`, and `/tmp/kmp-compare-upgrade.py`; no personal data was used. A separate version-24 Room reopen instrumented test is pending.

