## TASK-039 (done) — Backup/Restore контур (БД + файлы) и фиксация MVP RPO/RTO

### Цель
Реализовать и practically верифицировать контур резервного копирования/восстановления для монолита (`PostgreSQL + /opt/app/data`) с фиксацией фактических метрик RPO/RTO и соответствием минимальным MVP-требованиям хранения backup.

### Что выполнено
1. Подтверждён рабочий runtime-контур:
   - `monolith-postgres` в статусе healthy;
   - `monolith-mvp` в статусе running.
2. Выполнен подготовительный шаг test-scenario:
   - в БД создана/обновлена probe-таблица `task039_probe` с контрольной записью `before-backup`;
   - в файловом хранилище приложения создан probe-файл `/opt/app/data/task039/probe.txt` со значением `before-backup`.
3. Выполнен backup через `scripts/backup/backup-monolith.sh`:
   - сформирован snapshot `backups/monolith/20260224T184003Z`;
   - в snapshot есть SQL dump БД (`db/courses.sql`) и tar-архив файлов (`files/app-data.tar`);
   - `files_status=archived`, `duration_sec=1`.
4. Смоделирована потеря данных после backup:
   - БД: значение probe-записи изменено на `after-loss`;
   - файл: `probe.txt` удалён.
5. Выполнен restore из snapshot через `scripts/backup/restore-monolith.sh backups/monolith/20260224T184003Z`:
   - `restore_duration_sec=2`;
   - `rpo_seconds=38`;
   - `files_restore_status=restored`.
6. Проверена целостность после восстановления:
   - БД вернулась к состоянию `before-backup`;
   - файл `probe.txt` восстановлен и содержит `before-backup`.

### Фактические метрики
- Источник: `backups/monolith/20260224T184003Z/metadata.env`
  - `files_status=archived`
  - `retention_days=7`
  - `duration_sec=1`
- Источник: `backups/monolith/20260224T184003Z/restore-report.env`
  - `restore_duration_sec=2`
  - `rpo_seconds=38`
  - `files_restore_status=restored`

### Проверка acceptance criteria
1. **Есть регламент и автоматизируемый скрипт ежедневного backup БД и файлов** — выполнено (`scripts/backup/backup-monolith.sh`, snapshot с БД и файлами сформирован).
2. **Выполнен тест восстановления с фиксацией фактических RPO/RTO** — выполнено (`restore-report.env`, `rpo_seconds=38`, `restore_duration_sec=2`).
3. **Хранение backup соответствует минимуму для MVP (не менее 7 дней)** — выполнено (`BACKUP_RETENTION_DAYS=7`, в metadata `retention_days=7`).

### Изменённые/задействованные файлы
- `scripts/backup/backup-monolith.sh`
- `scripts/backup/restore-monolith.sh`
- `monolith.env.example`
- `docker-compose.monolith.yml`
- `monolith-mvp/README-runtime.md`
- `memory-bank/02-active-context.md`
- `memory-bank/task-artifacts/TASK-039.md`

### Полная копия активного контекста на момент завершения
# Активный контекст

## Выбранная задача
- **ID:** `TASK-039`
- **Категория:** infrastructure (non-ui)
- **Описание:** реализовать контур backup/restore (БД + файловые материалы) и подтвердить MVP RPO/RTO.

## Состояние окружения
1. Проверен compose-контур:
   - `docker compose -f docker-compose.monolith.yml --env-file monolith.env.example ps`
   - `monolith-postgres` — healthy, `monolith-mvp` — running.
2. Подтверждён статус задачи:
   - `python -X utf8 memory-bank/find_task.py TASK-039`
   - статус: `pending`, acceptance criteria и test_steps подтверждены.

## Что выполнено по test_steps
1. Подготовлены тест-данные перед backup:
   - БД (`monolith-postgres`, user `postgres`):
     - создана/обновлена таблица `task039_probe`
     - записано значение `id=1, note='before-backup'`.
   - Файлы (`monolith-mvp`):
     - создан `/opt/app/data/task039/probe.txt`
     - содержимое: `before-backup`.
2. Выполнен backup (через Git Bash + PowerShell):
   - команда: `./scripts/backup/backup-monolith.sh`
   - snapshot: `backups/monolith/20260224T184003Z`
   - `files_status=archived`
   - `duration_sec=1`
   - в snapshot присутствует `db/courses.sql` и `files/app-data.tar`.
3. Смоделирована потеря данных:
   - в БД значение изменено на `after-loss`;
   - файл `/opt/app/data/task039/probe.txt` удалён.
4. Выполнен restore из snapshot:
   - команда: `./scripts/backup/restore-monolith.sh backups/monolith/20260224T184003Z`
   - результат: `files_status=restored`, `duration_sec=2`, `rpo_seconds=38`.
5. Проверена целостность после restore:
   - БД: `task039_probe.note = before-backup`;
   - файл: `/opt/app/data/task039/probe.txt` содержит `before-backup`.

## Фактические метрики (из snapshot-отчётов)
- `backups/monolith/20260224T184003Z/metadata.env`:
  - `files_status=archived`
  - `retention_days=7`
  - `duration_sec=1`
- `backups/monolith/20260224T184003Z/restore-report.env`:
  - `restore_duration_sec=2`
  - `rpo_seconds=38`
  - `files_restore_status=restored`

## Вывод по acceptance criteria TASK-039
1. **Автоматизируемый backup БД + файлов**: выполнен (`scripts/backup/backup-monolith.sh`).
2. **Тест restore с фиксацией RPO/RTO**: выполнен (`restore-report.env`, фактические метрики зафиксированы).
3. **Retention минимум 7 дней**: выполнен (`BACKUP_RETENTION_DAYS=7`, `retention_days=7` в metadata).

## Что осталось для полного закрытия задачи
1. Сформировать артефакт `memory-bank/task-artifacts/TASK-039.md` с итогами и полной копией этого активного контекста.
2. Добавить ссылку на артефакт в `memory-bank/05-task-execution-progress.md`.
3. Добавить запись в `memory-bank/06-system-development-progress.md`.
4. Перевести задачу в done: `python memory-bank/change_task_status.py TASK-039 done`.
5. Очистить `memory-bank/02-active-context.md` после полного завершения.