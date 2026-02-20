# Прогресс выполнения задачи

## Назначение
Оперативный индекс выполнения pre-phase и ссылок на артефакты gap-analysis.

## Индекс артефактов (compressed)

### PRE-phase: завершённые задачи с артефактами
- `PRE-001` — Baseline анализа
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-001-baseline-report.md`
- `PRE-002` — Инвентаризация API-контуров
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-002-api-inventory-report.md`
- `PRE-003` — Инвентаризация доменных моделей
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-003-domain-model-inventory-report.md`
- `PRE-004` — Сверка Flyway/схемы БД
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-004-flyway-schema-gap-report.md`
- `PRE-005` — Сверка auth/security-контура
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-005-security-gap-report.md`
- `PRE-006` — Проверка слойности и сервисного слоя
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-006-layering-and-architecture-gap-report.md`
- `PRE-007` — Интеграции SMTP/YouTube/файлового хранения и деградационные сценарии
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-007-integrations-gap-report.md`
- `PRE-008` — Покрытие интеграционных/сквозных тестов по AC
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-008-test-coverage-gap-report.md`
- `PRE-009` — Сверка FR-001..FR-004 (users/profile/roles) с AC-001..AC-004
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-009-fr-001-fr-004-gap-report.md`
- `PRE-010` — Сверка FR-005..FR-009 (courses/lessons/questions/scoring) с AC-005..AC-009
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-010-fr-005-fr-009-gap-report.md`
- `PRE-011` — Сверка FR-010..FR-014 (enrollment/review/learning flow) с AC-010..AC-014
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-011-fr-010-fr-014-gap-report.md`
- `PRE-012` — Сверка FR-015..FR-017 (statistics/reporting) с AC-015..AC-017
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-012-fr-015-fr-017-gap-report.md`
- `PRE-013` — Сверка FR-018..FR-019 (change/reset password) и security/NFR
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-013-fr-018-fr-019-security-gap-report.md`
- `PRE-014` — Сверка FR-101..FR-105 (sections/programs/program rules/assignments)
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-014-fr-101-fr-105-gap-report.md`
- `PRE-015` — Сверка FR-106..FR-109 (groups/mass assignments/search/filter)
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-015-fr-106-fr-109-gap-report.md`
- `PRE-016` — Сверка FR-110..FR-114 (advanced practice: threshold/attempts/time/random/stopLesson)
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-016-fr-110-fr-114-gap-report.md`
- `PRE-017` — Сверка NFR-контура (observability/audit/backup-restore/reliability)
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-017-nfr-gap-report.md`
- `PRE-018` — Сводная матрица соответствия FR/AC/NFR с risk/effort/phase
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-018-fr-ac-nfr-consolidated-matrix.md`
- `PRE-019` — Change-set план для финальной синхронизации backlog (`tasks.json`)
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-019-backlog-change-set-plan.md`
- `PRE-020` — Финальная синхронизация backlog и фиксация новой baseline-версии `tasks.json`
  - Артефакт: `memory-bank/pre-task-artifacts/PRE-020-backlog-baseline-update-report.md`

## Текущий статус ветки
- Последняя завершённая pre-задача: `PRE-020` (`status=done` в `memory-bank/pre-tasks.json`).
- Pre-phase полностью завершён (`PRE-001..PRE-020`).
- Актуальный рабочий backlog: `memory-bank/tasks.json` (baseline после PRE-020).

## Примечание
- Файл сжат до индексного формата согласно правилу `.clinerules/10-memory-bank-workflow.md` для атомарных задач с отдельными артефактами.

## TASK-001 (done) — runtime-конфигурации MVP
- Что сделано:
  - В `monolith-mvp` реализованы profile groups `dev/stage/prod` и вынос секретов в env.
  - Добавлены actuator health endpoint и допуск к нему в security.
  - Добавлены контейнерные артефакты: `monolith-mvp/Dockerfile`, `monolith-mvp/.dockerignore`, `docker-compose.monolith.yml`.
  - Добавлен пример env: `monolith.env.example`.
  - Добавлена документация запуска/переключения профилей: `monolith-mvp/README-runtime.md`.
  - Проверена сборка: `mvn -pl monolith-mvp -DskipTests package` (`BUILD SUCCESS`).
- Финальная валидация test-steps:
  - `docker compose -f docker-compose.monolith.yml --env-file monolith.env.example up -d --build` — успешно.
  - Логи monolith подтверждают запуск приложения и подключение к PostgreSQL (`pg,mail-noop`, Flyway/JPA/Tomcat старт).
  - `curl http://localhost:8099/actuator/health` → `{"status":"UP","groups":["liveness","readiness"]}`.
- Итоговый статус задачи:
  - `TASK-001` переведена в `done`.
