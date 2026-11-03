#!/usr/bin/env bash
set -euo pipefail
# The Stage 1 provenance check belongs to the archived baseline commit.
# Current source is verified by the general regression runner.
exec bash "$(dirname "$0")/check.sh" "$@"
