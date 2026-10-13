#!/usr/bin/env bash
# Wipe the app, load the debug seed with only the N newest sessions, then set the theme of the variant. Usage: bash seed_n.sh <N> <variant>
set -u
PKG=app.calisthenics.personal
adb shell pm clear $PKG
adb shell am broadcast -n $PKG/io.github.gonbei774.calisthenicsmemory.debug.SeedReceiver -a $PKG.debug.SEED -f 0x20 --ei keep "$1" | tee /dev/stderr | grep -q "result=1" || exit 1
bash "$(dirname "$0")/theme.sh" "$2"
