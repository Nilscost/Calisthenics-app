#!/usr/bin/env bash
# Reproducible local verification contract from docs/archive/04-verification.md.
# Run with: bash scripts/verify.sh
#
# Fail immediately on a missing tool or failing check. Device tests are never
# launched/installed automatically; see the explicit commands at the end.
set -Eeuo pipefail

cd "$(dirname "$0")/.."

step() { printf '\n==> %s\n' "$1"; }

step "Gradle wrapper and pinned runtime"
./gradlew --version --console=plain

step "Domain JVM and WebAssembly, data and app unit tests"
./gradlew :domain:jvmTest :domain:wasmJsNodeTest :data:testDebugUnitTest :data:wasmJsNodeTest :app:testDebugUnitTest --console=plain
python3 tools/check_kmp_test_parity.py
python3 tools/check_app_test_preservation.py

step "App lint and debug APK build"
./gradlew :app:lintDebug :app:assembleDebug --console=plain

step "Starter content structure and rights validation"
python3 tools/validate_content.py content/starter

printf '\nAll host-runnable verification checks passed.\n'
printf 'Device checks (only after explicit device authorization):\n'
printf '  adb devices -l\n'
printf '  ./gradlew :data:connectedDebugAndroidTest :app:connectedDebugAndroidTest\n'
