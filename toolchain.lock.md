# Toolchain lock (T03 — docs/02-coding-specification.md §6)

Pinned toolchain for the Calisthenics fork. Bump deliberately; record the
reason in the PR/task notes.

| Component | Version | Pinned in |
|---|---|---|
| Gradle wrapper | 8.13 | `gradle/wrapper/gradle-wrapper.properties` |
| AGP (com.android.application / library) | 8.13.0 | `gradle/libs.versions.toml` (`agp`) |
| Kotlin (android / jvm / compose / serialization plugins) | 2.0.21 | `gradle/libs.versions.toml` (`kotlin`) |
| KSP | 2.0.21-1.0.28 | root `build.gradle.kts` |
| JVM toolchain / target | 17 | each `build.gradle.kts` |
| compileSdk | 35 | `app` + `data` `build.gradle.kts` |
| minSdk / targetSdk | 26 / 35 | `app` + `data` `build.gradle.kts` |
| Android SDK platform | android-35 | local SDK (CI: same) |
| Android SDK build-tools | 35.0.0 | local SDK |
| Platform tools (adb) | current as of build | local SDK |
| Python (content tooling) | 3.14.x (3.10+ required) | `tools/validate_content.py` |
| ffmpeg (sample media only, NOT app dependency) | local install | `tools/make_sample_media.sh` |

## Upstream base (ADR 0001)
- Repo: `https://github.com/gonbei774/CalisthenicsMemory`
- Commit: `045b8577` (pinned; rebase/re-fork only via a new ADR)
- License: GPL-3.0-only

## Notes
- No NDK. No native libraries in the **app** (Room uses the Android SQLite
  framework on-device).
- **Build-host-only** native compatibility for this ARM64 Linux sandbox
  (no app source or runtime impact; see `docs/evidence/t01-build.md` and
  `docs/evidence/t03-scaffold.md`):
  - AGP's bundled `aapt2` is x86-64 only. It runs under `qemu-user-static`
    once an x86-64 glibc is installed: `dpkg --add-architecture amd64 &&
    apt-get install -y libc6:amd64 libstdc++6:amd64 libgcc-s1:amd64`, then
    `ln -sf /lib/x86_64-linux-gnu/ld-linux-x86-64.so.2 /lib64/ld-linux-x86-64.so.2`.
    (If the egress proxy is down, run apt with the proxy vars unset — direct
    network works.)
  - Room's schema verifier (KSP: `kspDebugKotlin`) loads `sqlite-jdbc`
    natively; the default version ships no working `Linux/aarch64` native, so
    `:data` forces `org.xerial:sqlite-jdbc:3.45.3.0` (which does). This force
    is build-time only — it never enters the APK.
  - A JVM Robolectric Room smoke test for `:data` is NOT included:
    Robolectric's native SQLite runtime is not built for `Linux/aarch64`, so it
    cannot run in this sandbox (host limitation, not a code defect). Room
    correctness is covered on-device by the instrumented `MigrationTest` (G5)
    and by `:domain`'s pure-JVM tests.
- License note: see `THIRD_PARTY_NOTICES.md`.
