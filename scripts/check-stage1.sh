#!/usr/bin/env bash
set -euo pipefail
# The original source-comparison check is saved in the Stage 1 commit.
# Run the current regression suite for the developed agent.
exec bash "$(dirname "$0")/check.sh" "$@"
