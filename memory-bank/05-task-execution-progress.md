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

### COMPLETED TASKS INDEX: завершенные основные задачи с артефактами
- `TASK-001` — runtime-конфигурации MVP
  - Артефакт: `memory-bank/task-artifacts/TASK-001.md`
- `TASK-002` — стабилизация Flyway migrations
  - Артефакт: `memory-bank/task-artifacts/TASK-002.md`
- `TASK-003` — JWT hardening
  - Артефакт: `memory-bank/task-artifacts/TASK-003.md`
- `TASK-004` — RBAC-ограничения API
  - Артефакт: `memory-bank/task-artifacts/TASK-004.md`
- `TASK-006` — создание пользователя с первичными назначениями
  - Артефакт: `memory-bank/task-artifacts/TASK-006.md`
- `TASK-007` — email-onboarding (FR-002)
  - Артефакт: `memory-bank/task-artifacts/TASK-007.md`
- `TASK-008` — редактирование пользователя и смена роли (FR-003/FR-004)
  - Артефакт: `memory-bank/task-artifacts/TASK-008.md`
- `TASK-011` — FR-008/FR-009 practice-модель и scoring-контракт
  - Артефакт: `memory-bank/task-artifacts/TASK-011.md`
- `TASK-013` — FR-011 назначение reviewer и рабочая область проверки
  - Артефакт: `memory-bank/task-artifacts/TASK-013.md`
- `TASK-014` — FR-012 прохождение THEORY-урока
  - Артефакт: `memory-bank/task-artifacts/TASK-014.md`
- `TASK-015` — FR-013 runtime scoring engine
  - Артефакт: `memory-bank/task-artifacts/TASK-015.md`
- `TASK-016` — FR-014 workflow развёрнутого ответа
  - Артефакт: `memory-bank/task-artifacts/TASK-016.md`
- `TASK-017` — FR-015 личная статистика студента/пользователя
  - Артефакт: `memory-bank/task-artifacts/TASK-017.md`
- `TASK-018` — FR-016 статистика по конкретному курсу для администратора
  - Артефакт: `memory-bank/task-artifacts/TASK-018.md`
- `TASK-019A` — FR-017 (MVP) сводный отчёт по конкретному курсу
  - Артефакт: `memory-bank/task-artifacts/TASK-019A.md`
- `TASK-020A` — FR-017 (MVP) общий сводный отчёт по всем курсам
  - Артефакт: `memory-bank/task-artifacts/TASK-020A.md`
- `TASK-022A` — FR-019 (base flow) восстановление пароля по email-токену
  - Артефакт: `memory-bank/task-artifacts/TASK-022A.md`
- `TASK-026` — FR-101/FR-102 CRUD разделов каталога и создание курса в контексте раздела
  - Артефакт: `memory-bank/task-artifacts/TASK-026.md`
- `TASK-027` — FR-106/FR-107 группы пользователей и правила typed-membership
  - Артефакт: `memory-bank/task-artifacts/TASK-027.md`
- `TASK-030` — FR-103 создание/редактирование программ с упорядоченным списком курсов
  - Артефакт: `memory-bank/task-artifacts/TASK-030.md`
- `TASK-028` — FR-108 массовые назначения курсов/программ через группы
  - Артефакт: `memory-bank/task-artifacts/TASK-028.md`
- `TASK-031` — FR-104 правила прохождения программы (deadline + accessCondition)
  - Артефакт: `memory-bank/task-artifacts/TASK-031.md`
- `TASK-032` — FR-105 назначение программ пользователю и группе
  - Артефакт: `memory-bank/task-artifacts/TASK-032.md`
- `TASK-033` — FR-110/FR-111 порог прохождения практики и лимит попыток
  - Артефакт: `memory-bank/task-artifacts/TASK-033.md`












