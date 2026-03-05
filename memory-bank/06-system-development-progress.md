# System development progress

## 2026-03-05 — Files storage URL strategy alignment

- Unified files strategy for monolith backend:
  - `POST /api/v1/files/upload` returns **relative path** with `/files/` prefix.
  - Relative paths are normalized and persisted for user avatars/course covers.
  - API read models return **relative path** with `/files/` prefix (frontend composes full host URL).
- Implemented replacement lifecycle for avatars/covers:
  - old file is deleted on update when path changes;
  - file is deleted on entity removal (user/course deletion).
- Enabled direct static file serving from backend under `/files/**`:
  - added `StorageWebConfig` resource handler bound to `APP_STORAGE_ROOT_DIR`;
  - allowed unauthenticated read access to `/files/**` in security config.
- Runtime/env/docker alignment:
  - added/updated `APP_STORAGE_ROOT_DIR` and `APP_STORAGE_PUBLIC_BASE_URL` in compose/env examples.