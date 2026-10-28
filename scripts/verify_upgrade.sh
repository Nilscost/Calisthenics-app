#!/usr/bin/env bash
# Verify a signed install-over upgrade with the debug seed on a disposable emulator.
# Usage: ANDROID_SERIAL=emulator-5554 KMP_TEST_EMULATOR_RESET=1 \
#   scripts/verify_upgrade.sh <old-debug.apk> <new-debug.apk>
# The emulator's app data is cleared. Start persona with -read-only to preserve its disk.
set -euo pipefail
: "${ANDROID_SERIAL:?Set ANDROID_SERIAL to the disposable emulator}"
[[ "$ANDROID_SERIAL" == emulator-* && "${KMP_TEST_EMULATOR_RESET:-}" == 1 ]] || {
  echo 'Use a disposable emulator and set KMP_TEST_EMULATOR_RESET=1.' >&2; exit 2;
}
[[ $# == 2 ]] || { echo 'Usage: verify_upgrade.sh old-debug.apk new-debug.apk' >&2; exit 2; }
old_apk=$1
new_apk=$2
pkg=app.calisthenics.personal
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
adb install -r "$old_apk"
adb shell pm clear "$pkg"
adb shell am broadcast -n "$pkg/io.github.gonbei774.calisthenicsmemory.debug.SeedReceiver" -a "$pkg.debug.SEED" -f 0x20 | grep -q 'result=1'
adb shell am force-stop "$pkg"
mkdir "$work/before" "$work/after"
adb exec-out run-as "$pkg" tar -cf - databases shared_prefs > "$work/before.tar"
tar -xf "$work/before.tar" -C "$work/before"
adb install -r "$new_apk"
adb shell monkey -p "$pkg" 1 > /dev/null 2>&1
# Allow app startup before force-stopping, then capture the database and its WAL together.
sleep 5
adb shell am force-stop "$pkg"
adb exec-out run-as "$pkg" tar -cf - databases shared_prefs > "$work/after.tar"
tar -xf "$work/after.tar" -C "$work/after"
python3 "$(dirname "$0")/compare_upgrade.py" "$work/before" "$work/after"
