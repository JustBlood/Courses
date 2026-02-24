#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

sh "${SCRIPT_DIR}/backup-monolith.sh"

# Optional strict retention verification (default: enabled)
if [ "${BACKUP_VERIFY_RETENTION:-true}" = "true" ]; then
  sh "${SCRIPT_DIR}/check-backup-retention.sh"
fi
