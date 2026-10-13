#!/usr/bin/env bash
# Before flow k7_chart: the debug seed with only the 7 newest sessions (R7.2-T08). Usage: bash k7_chart.pre.sh <variant>
bash "$(dirname "$0")/seed_n.sh" 7 "$1"
