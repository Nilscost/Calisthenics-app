#!/usr/bin/env bash
# Sets the app theme for a screenshot variant (light, dark, font13 = light with a big font). Usage: bash theme.sh <variant>
PKG=app.calisthenics.personal
case "${1:-light}" in dark) T=DARK ;; *) T=LIGHT ;; esac
adb shell am broadcast -n $PKG/io.github.gonbei774.calisthenicsmemory.debug.ThemeReceiver -a $PKG.debug.THEME --es theme $T -f 0x20 | tee /dev/stderr | grep -q "result=1"
