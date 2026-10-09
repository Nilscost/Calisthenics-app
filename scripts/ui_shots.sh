#!/usr/bin/env bash
# V00: run every Maestro flow in maestro/flows on the attached emulator, once per variant.
# Usage: bash scripts/ui_shots.sh <apk> <out-dir>
# Output: <out-dir>/<flow>/<step>-<variant>.png and <out-dir>/results.txt (one line per flow and variant).
# A failing flow is recorded and the run continues, so the screenshots taken before the failure are kept;
# the script exits non-zero at the end if any flow failed.
set -uo pipefail
APK="$1"; OUT="$2"; ROOT=$(pwd); PKG=app.calisthenics.personal
export MAESTRO_CLI_NO_ANALYTICS=1
mkdir -p "$OUT"; : > "$OUT/results.txt"
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done
adb shell settings put global hide_error_dialogs 1   # no "isn't responding" dialogs over the app
sleep 30                                             # let the launcher and system apps settle after boot
adb shell wm size 1080x2400
adb shell wm density 420          # Galaxy S21: 1080x2400, ~421 dpi
adb install -r "$APK"
fail=0
for variant in light dark font13; do
  case $variant in
    light)  night=no;  scale=1.0 ;;
    dark)   night=yes; scale=1.0 ;;
    font13) night=no;  scale=1.3 ;;
  esac
  adb shell cmd uimode night $night
  adb shell settings put system font_scale $scale
  # A uimode/font change restarts system UI on the emulator; give it time, then start from the home screen.
  adb shell am force-stop $PKG
  sleep 15
  adb shell input keyevent KEYCODE_HOME
  sleep 3
  # Flows run in file order; a_* starts from a fresh install (clearState), the others continue from it.
  for flow in maestro/flows/*.yaml; do
    name=$(basename "$flow" .yaml)
    mkdir -p "$OUT/$name"
    # Optional hook before a flow (maestro/hooks/<flow>.pre.sh), e.g. the debug seed for e_seeded.
    hook="maestro/hooks/$name.pre.sh"
    if [ -f "$hook" ]; then
      bash "$hook" > "$OUT/$name/hook-$variant.txt" 2>&1 || echo "HOOKFAIL $name $variant" >> "$OUT/results.txt"
    fi
    # Maestro only writes screenshots inside its own output folder: run it there with relative names, then collect them.
    # One retry: the emulator occasionally shows a blank or frozen screen after a configuration change.
    for try in 1 2; do
      tmp=$(mktemp -d)
      if (cd "$tmp" && maestro test --no-ansi --test-output-dir "$tmp" -e VARIANT=$variant "$ROOT/$flow") > "$OUT/$name/log-$variant.txt" 2>&1; then
        ok=1; else ok=0; fi
      [ $ok = 1 ] && break
      [ $try = 1 ] && { echo "RETRY $name $variant" >> "$OUT/results.txt"; adb shell am force-stop $PKG; adb shell input keyevent KEYCODE_HOME; sleep 15; rm -rf "$tmp"; }
    done
    find "$tmp" -name '*.png' -not -path '*/.maestro/*' -not -name 'screenshot-*' -exec cp {} "$OUT/$name/" \;
    rm -rf "$tmp"
    if [ $ok = 1 ]; then
      echo "PASS $name $variant" >> "$OUT/results.txt"
    else
      echo "FAIL $name $variant" >> "$OUT/results.txt"; fail=1
      adb exec-out screencap -p > "$OUT/$name/zz-failure-$variant.png" || true
    fi
  done
done
adb shell settings put system font_scale 1.0
adb shell cmd uimode night no
cat "$OUT/results.txt"
exit $fail
