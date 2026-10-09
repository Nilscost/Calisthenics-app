#!/usr/bin/env bash
# Before flow e_seeded: wipe the app and load the debug seed (V00b).
set -u
PKG=app.calisthenics.personal
adb shell pm clear $PKG
adb shell am broadcast -n $PKG/io.github.gonbei774.calisthenicsmemory.debug.SeedReceiver -a $PKG.debug.SEED -f 0x20 | tee /dev/stderr | grep -q "result=1"
