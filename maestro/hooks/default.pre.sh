#!/usr/bin/env bash
# Hook for flows without their own hook: only the theme. Usage: bash default.pre.sh <variant>
bash "$(dirname "$0")/theme.sh" "$1"
