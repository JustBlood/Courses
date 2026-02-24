#!/usr/bin/env sh
set -eu

BACKUP_ROOT="${BACKUP_ROOT:-backups/monolith}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"

case "${BACKUP_RETENTION_DAYS}" in
  ''|*[!0-9]*)
    echo "[retention-check] BACKUP_RETENTION_DAYS must be a non-negative integer"
    exit 1
    ;;
esac

if [ ! -d "${BACKUP_ROOT}" ]; then
  echo "[retention-check] backup root not found: ${BACKUP_ROOT}"
  exit 1
fi

max_allowed_epoch="$(( $(date +%s) - (BACKUP_RETENTION_DAYS * 86400) ))"

violations=""

for dir in "${BACKUP_ROOT}"/*; do
  [ -d "${dir}" ] || continue

  meta_file="${dir}/metadata.env"
  if [ ! -f "${meta_file}" ]; then
    violations="${violations}\n- ${dir} (metadata.env missing)"
    continue
  fi

  snapshot_epoch="$(sed -n 's/^snapshot_epoch=//p' "${meta_file}" | head -n 1)"
  if [ -z "${snapshot_epoch}" ]; then
    violations="${violations}\n- ${dir} (snapshot_epoch missing)"
    continue
  fi

  case "${snapshot_epoch}" in
    ''|*[!0-9]*)
      violations="${violations}\n- ${dir} (snapshot_epoch invalid: ${snapshot_epoch})"
      continue
      ;;
  esac

  if [ "${snapshot_epoch}" -lt "${max_allowed_epoch}" ]; then
    violations="${violations}\n- ${dir} (too old for retention=${BACKUP_RETENTION_DAYS}d)"
  fi
done

if [ -n "${violations}" ]; then
  echo "[retention-check] FAILED: retention policy violation(s) detected:${violations}"
  exit 1
fi

echo "[retention-check] OK: all snapshots satisfy retention=${BACKUP_RETENTION_DAYS}d"
