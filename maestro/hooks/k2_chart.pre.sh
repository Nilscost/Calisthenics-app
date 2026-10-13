#!/usr/bin/env bash
# Before flow k2_chart: the debug seed with only the 2 newest sessions (R7.2-T08). Usage: bash k2_chart.pre.sh <variant>
bash "$(dirname "$0")/seed_n.sh" 2 "$1"
