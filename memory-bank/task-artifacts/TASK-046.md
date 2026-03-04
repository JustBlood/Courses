## TASK-046 (done) — Backup/Restore readiness, измеримые RPO/RTO и retention-контроль

### Цель
Подтвердить эксплуатационную готовность backup/restore-контура (БД + файлы) с проверяемыми метриками RPO/RTO и явной проверкой соблюдения retention.

### Что сделано
1. Завершена автоматизация контроля retention:
   - добавлен `scripts/backup/check-backup-retention.sh`;
   - обновлён `scripts/backup/backup.sh` (wrapper + optional retention-check);
   - обновлены runtime-настройки/документация (`monolith.env.example`, `monolith-mvp/README-runtime.md`).
2. Выполнен backup по регламенту:
   - snapshot: `backups/monolith/20260224T185336Z`;
   - результат: `files_status=archived`, retention-check `OK`.
3. Выполнен restore-drill из того же snapshot:
   - `restore_duration_sec=2`;
   - `rpo_seconds=18`;
   - `files_restore_status=restored`.

### Источники фактических метрик
- `backups/monolith/20260224T185336Z/metadata.env`
- `backups/monolith/20260224T185336Z/restore-report.env`

### Проверка acceptance criteria
1. Документирован и автоматизирован регламент backup/restore — **выполнено**.
2. Проведён проверяемый restore-drill с фиксацией фактических RPO/RTO — **выполнено**.
3. Определён retention и проверка его соблюдения — **выполнено**.

### Изменённые/затронутые файлы
- `scripts/backup/backup.sh`
- `scripts/backup/check-backup-retention.sh` (new)
- `monolith.env.example`
- `monolith-mvp/README-runtime.md`
- `memory-bank/02-active-context.md`
- `memory-bank/task-artifacts/TASK-046.md`

### Полная копия активного контекста на момент завершения
# Активный контекст

## Выбранная задача
- **ID:** `TASK-046`
- **Категория:** `infrastructure` (non-ui)
- **Приоритет:** `medium`
- **Зависимости:** `TASK-039` (done)

## Основание выбора
- В `memory-bank/tasks.json` pending-задачи: `TASK-022B`, `TASK-038`, `TASK-040`, `TASK-046`.
- `TASK-046` — единственная ready-to-start задача с закрытой зависимостью (`TASK-039=done`).

## Релевантные требования
- PRD-03 / NFR-BCK-01..03:
  - ежедневный backup БД;
  - backup файлов не реже 1 раза в сутки;
  - MVP-цели восстановления: RPO ≤ 24ч, RTO ≤ 8ч.
- PRD-04: эксплуатационный контур Linux/VPS и локальное файловое хранение.

## Цель
Подтвердить backup/restore readiness в эксплуатационно-пригодном виде:
1) регламент + автоматизируемые команды,
2) проверяемый restore-drill с фиксацией фактических RPO/RTO,
3) минимальный retention и регулярную проверку его соблюдения.

## Фактически выполнено
1. Доведена автоматизация retention-проверки:
   - создан `scripts/backup/check-backup-retention.sh`;
   - обновлён wrapper `scripts/backup/backup.sh` (вызов `backup-monolith.sh` + опциональный retention-check);
   - обновлены `monolith.env.example` и `monolith-mvp/README-runtime.md`.
2. Выполнен backup по регламенту:
   - команда: `C:\Progra~1\Git\bin\bash.exe -lc "cd /c/Users/User/AllMine/prog/prog_java/mts/Courses && ./scripts/backup/backup.sh"`
   - результат: snapshot `backups/monolith/20260224T185336Z`, `files_status=archived`, retention-check OK.
3. Выполнен restore-drill для того же snapshot:
   - команда: `C:\Progra~1\Git\bin\bash.exe -lc "cd /c/Users/User/AllMine/prog/prog_java/mts/Courses && ./scripts/backup/restore-monolith.sh backups/monolith/20260224T185336Z"`
   - результат: `restore_duration_sec=2`, `rpo_seconds=18`, `files_status=restored`.

## Артефакты измерений
- `backups/monolith/20260224T185336Z/metadata.env`
- `backups/monolith/20260224T185336Z/restore-report.env`

Ключевые значения из `restore-report.env`:
- `restore_duration_sec=2`
- `rpo_seconds=18`
- `files_restore_status=restored`

## Статус acceptance criteria TASK-046
1. Документирован и автоматизирован регламент backup/restore — **выполнено**.
2. Проведён проверяемый restore-drill с фиксацией фактических RPO/RTO — **выполнено**.
3. Определён retention и проверка его соблюдения — **выполнено**.

## Изменённые файлы в рамках TASK-046
- `scripts/backup/backup.sh`
- `scripts/backup/check-backup-retention.sh` (new)
- `monolith.env.example`
- `monolith-mvp/README-runtime.md`
- `memory-bank/02-active-context.md`