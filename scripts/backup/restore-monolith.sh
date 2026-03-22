#!/usr/bin/env sh
set -eu

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <snapshot-dir>"
  echo "Example: $0 backups/monolith/20260224T210000Z"
  exit 1
fi

SNAPSHOT_DIR="$1"
BACKUP_ROOT="${BACKUP_ROOT:-backups/monolith}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-monolith-postgres}"
POSTGRES_DB="${POSTGRES_DB:-courses}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_VOLUME_SOURCE="${POSTGRES_VOLUME_SOURCE:-/var/lib/docker/volumes/backend_monolith_pg_data/_data}"
APP_CONTAINER="${APP_CONTAINER:-monolith-mvp}"
RESTORE_RECREATE_SCHEMA="${RESTORE_RECREATE_SCHEMA:-true}"

DB_DUMP="${SNAPSHOT_DIR}/db/${POSTGRES_DB}.sql"
FILES_ARCHIVE="${SNAPSHOT_DIR}/files/app-data.tar"
META_FILE="${SNAPSHOT_DIR}/metadata.env"

if [ ! -d "${SNAPSHOT_DIR}" ]; then
  echo "[restore] snapshot directory not found: ${SNAPSHOT_DIR}"
  exit 1
fi

case "${SNAPSHOT_DIR}" in
  "${BACKUP_ROOT}"/*) ;;
  *)
    echo "[restore] snapshot must be inside BACKUP_ROOT='${BACKUP_ROOT}'"
    exit 1
    ;;
esac

if [ ! -f "${DB_DUMP}" ]; then
  echo "[restore] DB dump not found: ${DB_DUMP}"
  exit 1
fi

echo "[restore] snapshot=${SNAPSHOT_DIR}"
RESTORE_STARTED_EPOCH="$(date +%s)"

if [ "${RESTORE_RECREATE_SCHEMA}" = "true" ]; then
  docker exec -i "${POSTGRES_CONTAINER}" sh -lc \
    "psql -v ON_ERROR_STOP=1 -U '${POSTGRES_USER}' -d '${POSTGRES_DB}' -c \"DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;\""
fi

docker exec -i "${POSTGRES_CONTAINER}" sh -lc \
  "psql -v ON_ERROR_STOP=1 -U '${POSTGRES_USER}' -d '${POSTGRES_DB}'" < "${DB_DUMP}"

if [ -f "${FILES_ARCHIVE}" ]; then
  docker exec -i "${APP_CONTAINER}" sh -lc "tar -C / -xf -" < "${FILES_ARCHIVE}"
  FILES_RESTORE_STATUS="restored"
else
  FILES_RESTORE_STATUS="skipped"
fi

RESTORE_FINISHED_EPOCH="$(date +%s)"
RESTORE_DURATION_SEC="$((RESTORE_FINISHED_EPOCH - RESTORE_STARTED_EPOCH))"

BACKUP_EPOCH=""
if [ -f "${META_FILE}" ]; then
  BACKUP_EPOCH="$(sed -n 's/^snapshot_epoch=//p' "${META_FILE}" | head -n 1)"
fi

if [ -n "${BACKUP_EPOCH}" ]; then
  RPO_SECONDS="$((RESTORE_STARTED_EPOCH - BACKUP_EPOCH))"
else
  RPO_SECONDS="unknown"
fi

cat > "${SNAPSHOT_DIR}/restore-report.env" <<META
snapshot_dir=${SNAPSHOT_DIR}
restore_started_epoch=${RESTORE_STARTED_EPOCH}
restore_finished_epoch=${RESTORE_FINISHED_EPOCH}
restore_duration_sec=${RESTORE_DURATION_SEC}
rpo_seconds=${RPO_SECONDS}
files_restore_status=${FILES_RESTORE_STATUS}
postgres_container=${POSTGRES_CONTAINER}
postgres_db=${POSTGRES_DB}
postgres_volume_source=${POSTGRES_VOLUME_SOURCE}
app_container=${APP_CONTAINER}
META

echo "[restore] done duration_sec=${RESTORE_DURATION_SEC} rpo_seconds=${RPO_SECONDS} files_status=${FILES_RESTORE_STATUS}"
