#!/usr/bin/env bash
# Verify a signed install-over upgrade with the debug seed on a disposable emulator.
# Usage: ANDROID_SERIAL=emulator-5554 KMP_TEST_EMULATOR_RESET=1 \
#   scripts/verify_upgrade.sh <old-debug.apk> <new-debug.apk> <oracle-debug.apk>
# The emulator's app data is cleared. Start persona with -read-only to preserve its disk.
set -euo pipefail
: "${ANDROID_SERIAL:?Set ANDROID_SERIAL to the disposable emulator}"
[[ "$ANDROID_SERIAL" == emulator-* && "${KMP_TEST_EMULATOR_RESET:-}" == 1 ]] || {
  echo 'Use a disposable emulator and set KMP_TEST_EMULATOR_RESET=1.' >&2; exit 2;
}
[[ $# == 3 ]] || { echo 'Usage: verify_upgrade.sh old-debug.apk new-debug.apk oracle-debug.apk' >&2; exit 2; }
old_apk=$1
new_apk=$2
oracle_apk=$3
pkg=app.calisthenics.personal
oracle=app.calisthenics.personal.oracleprobe/androidx.test.runner.AndroidJUnitRunner
work=$(mktemp -d)
cleanup() {
  if [[ -n "${KMP_UPGRADE_EVIDENCE_DIR:-}" ]]; then
    mkdir -p "$KMP_UPGRADE_EVIDENCE_DIR"
    cp -R "$work"/. "$KMP_UPGRADE_EVIDENCE_DIR"/
  fi
  rm -rf "$work"
}
trap cleanup EXIT
[[ $(shasum -a 256 "$old_apk" | cut -d ' ' -f 1) == e1619feafcb12cafee85828e1196a2cf48f5666c3d885174d9ee9f855096853d ]] || { echo 'Unexpected baseline APK' >&2; exit 1; }
for apk in "$old_apk" "$new_apk" "$oracle_apk"; do
  cert=$(apksigner verify --print-certs "$apk" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
  [[ "$cert" == 8638d15f7e5327d0a4f3197dad470e70b1425dfcb00a6bfc4bbcdc96a63bd940 ]] || { echo "Unexpected signing certificate: $apk" >&2; exit 1; }
done
oracle_attempt=0
run_oracle() {
  oracle_attempt=$((oracle_attempt + 1))
  local test=$1 output="$work/oracle-$oracle_attempt-$1.log"
  adb shell am instrument -w -e class "app.calisthenics.personal.oracleprobe.UpgradeOracleTest#$test" "$oracle" > "$output"
  cat "$output"
  grep -q 'OK (1 test)' "$output"
}
adb install -r "$old_apk"
adb install -r "$oracle_apk"
adb shell pm clear "$pkg"
adb shell am broadcast -n "$pkg/io.github.gonbei774.calisthenicsmemory.debug.SeedReceiver" -a "$pkg.debug.SEED" -f 0x20 | grep -q 'result=1'
adb shell monkey -p "$pkg" 1 > /dev/null 2>&1
sleep 5
# Record a real synthetic workout on the original app in addition to the eight seeded sessions.
maestro --device "$ANDROID_SERIAL" test --no-ansi --test-output-dir "$work/old-workout" -e VARIANT=light "$(dirname "$0")/../maestro/flows/c_session.yaml" > "$work/old-workout.log" 2>&1
run_oracle setup
run_oracle verify
adb shell am force-stop "$pkg"
mkdir "$work/before" "$work/after"
adb exec-out run-as "$pkg" tar -cf - databases shared_prefs > "$work/before.tar"
tar -xf "$work/before.tar" -C "$work/before"
adb install -r "$new_apk"
adb shell monkey -p "$pkg" 1 > /dev/null 2>&1
# Allow app startup before force-stopping, then capture the database and its WAL together.
sleep 5
run_oracle verify
adb shell am force-stop "$pkg"
adb exec-out run-as "$pkg" tar -cf - databases shared_prefs > "$work/after.tar"
tar -xf "$work/after.tar" -C "$work/after"
python3 "$(dirname "$0")/compare_upgrade.py" "$work/before" "$work/after"
run_oracle verify

# Checkpoint must be tested without launching either app after setup: launch may consume recovery.
adb shell pm clear "$pkg"
adb install -r "$old_apk"
adb shell am broadcast -n "$pkg/io.github.gonbei774.calisthenicsmemory.debug.SeedReceiver" -a "$pkg.debug.SEED" -f 0x20 | grep -q 'result=1'
run_oracle setupCheckpoint
adb shell am force-stop "$pkg"
adb install -r "$new_apk"
run_oracle verifyCheckpoint

# Established one-time defaults are verified first on the original, then after upgrade.
adb shell pm clear "$pkg"
adb install -r "$old_apk"
adb shell am broadcast -n "$pkg/io.github.gonbei774.calisthenicsmemory.debug.SeedReceiver" -a "$pkg.debug.SEED" -f 0x20 | grep -q 'result=1'
run_oracle setupLegacy
run_oracle verifyLegacy
adb shell am force-stop "$pkg"
adb install -r "$new_apk"
run_oracle verifyLegacy
