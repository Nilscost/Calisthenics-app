#!/usr/bin/env bash
# Before flow k1_chart: the debug seed with only the 1 newest sessions (R7.2-T08). Usage: bash k1_chart.pre.sh <variant>
bash "$(dirname "$0")/seed_n.sh" 1 "$1"
