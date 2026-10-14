# T01 build evidence — CalisthenicsMemory @ 045b8577 (2026-09-28)

Isolated copy: `/workspace/app/research-checkouts/cm-build-attempt/`
- HEAD: `045b8577d1a5c7fa75d40bf6c1df95244af616f3` (`master`, codeberg)
- `app/build.gradle.kts` **unmodified from upstream** (verified by diff against the
  clean checkout; an earlier experiment with a `resolutionStrategy.force` was
  reverted before the successful run).
- `local.properties` → `sdk.dir=/opt/android-sdk` (untracked, local only).

## Environment (Docker sandbox, ARM64)

| Item | Value |
|---|---|
| JDK | OpenJDK 21.0.12.1 (`/usr/lib/jvm/java-21-openjdk-arm64`) |
| Gradle | 8.13 (wrapper, downloaded from services.gradle.org) |
| Android SDK | platform-tools, platforms;android-35, build-tools;35.0.0 (`/opt/android-sdk`) |

Two **environment-only** fixes were needed (no candidate source changed):

1. **aapt2 architecture mismatch.** AGP 8.13 ships `aapt2-8.13.0-13719691-linux`
   as an x86-64 ELF. On this ARM64 sandbox it runs via qemu-user-static
   (`apt-get install qemu-user-static`) but needed an x86-64 glibc loader:
   `dpkg --add-architecture amd64 && apt-get install libc6:amd64` provides
   `/lib64/ld-linux-x86-64.so.2`. Verified: direct exec of the aapt2 binary
   prints `Android Asset Packaging Tool (aapt) 2.20-13719691`.
2. **Room DatabaseVerifier native.** `kspDebugKotlin` loads
   `org.xerial:sqlite-jdbc:3.41.2.2` (pulled in transitively by Room), whose jar
   bundles no `org/sqlite/native/Linux/aarch64/libsqlitejdbc.so` (it bundles
   Mac/aarch64 — the author builds on macOS). The loader searches
   `/usr/lib/aarch64-linux-gnu` as a system path; the aarch64 native from
   `sqlite-jdbc 3.45.3.0` was extracted there
   (`/usr/lib/aarch64-linux-gnu/libsqlitejdbc.so`). Environment-only: upstream
   builds on macOS where the bundled Mac/aarch64 native loads fine.

## Commands and actual results

```
$ ./gradlew assembleDebug --no-daemon --console=plain
BUILD SUCCESSFUL in 2m 9s (42 actionable tasks: 23 executed, 19 up-to-date)
GRADLE_EXIT=0
→ app/build/outputs/apk/debug/app-debug.apk  (14,673,181 bytes)

$ ./gradlew testDebugUnitTest --no-daemon --console=plain
BUILD SUCCESSFUL in 29s
TEST_EXIT=0
→ JUnit XML totals: 13 test classes, 209 tests, 0 failures, 0 errors, 0 skipped

$ ./gradlew lintDebug --no-daemon --console=plain
BUILD FAILED in 1m 55s
LINT_EXIT=1
→ "Lint found 749 errors, 364 warnings and 7 hints" — all pre-existing upstream
  style issues (e.g. SuspiciousIndentation in ProgramExecutionUtils.kt:138).
  Not build-breaking for assemble/test; to be baselined at T24.
```

Raw logs retained: `/workspace/cm-build-attempt.log`, `/workspace/cm-test.log`,
`/workspace/cm-lint.log`.

## Earlier attempts (2026-09-25, network down) — for the record

- `./gradlew assembleDebug` → `java.net.UnknownHostException: services.gradle.org`,
  exit 1 (Gradle distribution unreachable). Log: `/workspace/cm-build-attempt.log`
  (overwritten by the successful run; error preserved in
  `docs/research/reuse-audit.md`).

## Verdict

The preferred candidate **builds and passes its own test suite** in this
environment. No code-level defects surfaced by the build. Remaining T01
verification is the G0 owner approval of the reuse strategy (ADR 0001).
