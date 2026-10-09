#!/usr/bin/env bash
# Before flow a_onboarding: a fresh install (the flow no longer clears the state itself, that would reset the theme), then the theme of the variant.
set -u
PKG=app.calisthenics.personal
adb shell pm clear $PKG
bash "$(dirname "$0")/theme.sh" "$1"
