#!/usr/bin/env sh
set -eu

BACKUP_ROOT="${BACKUP_ROOT:-backups/monolith}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-monolith-postgres}"
POSTGRES_DB="${POSTGRES_DB:-courses}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_VOLUME_SOURCE="${POSTGRES_VOLUME_SOURCE:-/var/lib/docker/volumes/backend_monolith_pg_data/_data}"

APP_CONTAINER="${APP_CONTAINER:-monolith-mvp}"
BACKUP_FILES_SOURCE="${BACKUP_FILES_SOURCE:-/opt/app/data}"

case "${BACKUP_ROOT}" in
  ""|"/"|"." )
    echo "[backup] unsafe BACKUP_ROOT='${BACKUP_ROOT}'"
    exit 1
    ;;
esac

case "${BACKUP_RETENTION_DAYS}" in
  ''|*[!0-9]*)
    echo "[backup] BACKUP_RETENTION_DAYS must be a non-negative integer"
    exit 1
    ;;
esac

SNAPSHOT_TS="$(date -u +%Y%m%dT%H%M%SZ)"
SNAPSHOT_EPOCH="$(date +%s)"
SNAPSHOT_DIR="${BACKUP_ROOT}/${SNAPSHOT_TS}"

mkdir -p "${SNAPSHOT_DIR}/db" "${SNAPSHOT_DIR}/files"

echo "[backup] snapshot=${SNAPSHOT_DIR}"

DB_DUMP_FILE="${SNAPSHOT_DIR}/db/${POSTGRES_DB}.sql"
docker exec -i "${POSTGRES_CONTAINER}" sh -lc \
  "pg_dump -U '${POSTGRES_USER}' --no-owner --no-privileges '${POSTGRES_DB}'" > "${DB_DUMP_FILE}"

FILES_ARCHIVE="${SNAPSHOT_DIR}/files/app-data.tar"
if docker exec -i "${APP_CONTAINER}" sh -lc "test -d '${BACKUP_FILES_SOURCE}'"; then
  docker exec -i "${APP_CONTAINER}" sh -lc "tar -C / -cf - '${BACKUP_FILES_SOURCE#/}'" > "${FILES_ARCHIVE}"
  FILES_STATUS="archived"
else
  FILES_STATUS="source-missing"
  : > "${SNAPSHOT_DIR}/files/SKIPPED.txt"
  printf '%s\n' "'${BACKUP_FILES_SOURCE}' does not exist inside ${APP_CONTAINER}." > "${SNAPSHOT_DIR}/files/SKIPPED.txt"
fi

FINISHED_EPOCH="$(date +%s)"
DURATION_SEC="$((FINISHED_EPOCH - SNAPSHOT_EPOCH))"

cat > "${SNAPSHOT_DIR}/metadata.env" <<META
snapshot_id=${SNAPSHOT_TS}
snapshot_epoch=${SNAPSHOT_EPOCH}
backup_root=${BACKUP_ROOT}
postgres_container=${POSTGRES_CONTAINER}
postgres_db=${POSTGRES_DB}
postgres_user=${POSTGRES_USER}
postgres_volume_source=${POSTGRES_VOLUME_SOURCE}
db_dump_file=${DB_DUMP_FILE}
app_container=${APP_CONTAINER}
backup_files_source=${BACKUP_FILES_SOURCE}
files_archive=${FILES_ARCHIVE}
files_status=${FILES_STATUS}
retention_days=${BACKUP_RETENTION_DAYS}
duration_sec=${DURATION_SEC}
META

find "${BACKUP_ROOT}" -mindepth 1 -maxdepth 1 -type d -mtime +"${BACKUP_RETENTION_DAYS}" -exec rm -rf {} +

echo "[backup] done snapshot=${SNAPSHOT_DIR} duration_sec=${DURATION_SEC} files_status=${FILES_STATUS}"
