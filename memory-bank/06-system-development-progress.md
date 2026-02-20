# Прогресс разработки системы

## Назначение
Краткий накопительный журнал по системе в целом.
Обновляется только после полного завершения задач/итераций.

## Формат записи
### YYYY-MM-DD — Итерация / релиз
- Что сделано (кратко):
- Какие модули затронуты:
- Что отложено / следующий крупный этап:

## Текущее состояние
- Инициализировано.

### 2026-02-20 — PRD (главы 00–02) доработаны и согласованы
- Что сделано (кратко):
  - Обновлены главы `prd/00-overview-and-goals.md`, `prd/01-user-scenarios.md`, `prd/02-functional-requirements.md` по детальному feedback.
  - Формулировки синхронизированы с текущей backend-реализацией `monolith-mvp` (стек, auth flow, параметры Course/Lesson/PracticeLesson, отчётность).
  - Исправлен процессный момент: запись в этот файл ведётся после полного завершения задачи.
- Какие модули затронуты:
  - `memory-bank/prd/00-overview-and-goals.md`
  - `memory-bank/prd/01-user-scenarios.md`
  - `memory-bank/prd/02-functional-requirements.md`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
- Что отложено / следующий крупный этап:
  - Заполнение оставшихся глав PRD: `03-non-functional-requirements.md`, `04-constraints-and-assumptions.md`, `05-acceptance-criteria.md`.

### 2026-02-20 — Комплексный PRD 00–07 сформирован
- Что сделано (кратко):
  - Завершены главы `prd/03-non-functional-requirements.md`, `prd/04-constraints-and-assumptions.md`, `prd/05-technical-architecture.md`, `prd/06-acceptance-criteria.md`, `prd/07-development-and-risks-and-future.md`.
  - Актуализирован индекс `memory-bank/01-prd-index.md` с полной структурой PRD-00..PRD-07.
  - Зафиксирован гибридный подход качества: MVP — минимально реализуемый уровень, post-MVP — целевой уровень.
  - В критериях приемки добавлена полная трассировка AC к FR (MVP и post-MVP).
  - Обновлены `memory-bank/02-active-context.md` и `memory-bank/05-task-execution-progress.md`.
- Какие модули затронуты:
  - `memory-bank/01-prd-index.md`
  - `memory-bank/prd/03-non-functional-requirements.md`
  - `memory-bank/prd/04-constraints-and-assumptions.md`
  - `memory-bank/prd/05-technical-architecture.md`
  - `memory-bank/prd/06-acceptance-criteria.md`
  - `memory-bank/prd/07-development-and-risks-and-future.md`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
- Что отложено / следующий крупный этап:
  - Провести пользовательское ревью финальной версии PRD 00–07 и внести точечные правки (при необходимости).

### 2026-02-20 — Сформирован backlog задач из PRD для инкрементной разработки
- Что сделано (кратко):
  - На основе PRD-00..PRD-07 подготовлен `tasks.json` в строгом JSON-формате.
  - Добавлены `agent_instructions` для единых правил работы coding-агентов.
  - Сформирован backlog из 40 атомарных задач (`TASK-001..TASK-040`) с приоритетами, зависимостями, критериями приемки и test steps.
  - Покрыты обязательные категории задач: `infrastructure`, `functional`, `ui`, `integration`, `security`.
  - Обновлены `memory-bank/02-active-context.md` и `memory-bank/05-task-execution-progress.md`.
- Какие модули затронуты:
  - `tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Начать реализацию по одной задаче за сессию с верхушки critical-path (`TASK-001` и далее по зависимостям).

### 2026-02-20 — Добавлен pre-backlog gap-analysis и обновлён основной backlog по итогам ревью субагентов
- Что сделано (кратко):
  - Проведён ревью `tasks.json` с привлечением субагентов `system-architect` и `sprint-prioritizer`.
  - Скорректированы правила работы в `agent_instructions` и критичные зависимости (`TASK-028`, `TASK-019`, `TASK-020`).
  - Создан `pre-tasks.json` (20 задач `PRE-001..PRE-020`) для поэтапной сверки «код vs PRD».
  - Зафиксирован процесс: итоговая корректировка `tasks.json` выполняется только на шаге `PRE-020` после консолидированного gap-analysis.
  - Проверена валидность JSON для `tasks.json` и `pre-tasks.json`.
- Какие модули затронуты:
  - `tasks.json`
  - `pre-tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Начать выполнение pre-phase с `PRE-001` и довести до `PRE-020`, затем синхронизировать основной backlog реализации.

### 2026-02-20 — Закрыта pre-задача PRE-006 (слойность и архитектурные отклонения)
- Что сделано (кратко):
  - Выполнена проверка слойности `controller -> service -> repository` для контуров `auth/user/course/learning/report`.
  - Подтверждено, что активные REST-контракты не отдают entity напрямую и используют DTO/response-модели.
  - Зафиксированы архитектурные отклонения: зависимость `LessonMapper -> CourseService`, batch-orchestration в `CoursesController`, неиспользуемые service-инъекции в ряде controller.
  - Сформирован отдельный артефакт анализа: `memory-bank/pre-task-artifacts/PRE-006-layering-and-architecture-gap-report.md`.
  - По правилу компрессии обновлён `memory-bank/05-task-execution-progress.md` до индексного формата ссылок на pre-артефакты.
- Какие модули затронуты:
  - `memory-bank/pre-task-artifacts/PRE-006-layering-and-architecture-gap-report.md`
  - `memory-bank/pre-tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к `PRE-009` как следующей ready-to-start задаче критичного приоритета (сверка FR-001..FR-004 с AC).

### 2026-02-20 — Закрыта pre-задача PRE-010 (FR-005..FR-009, courses/lessons/questions/scoring)
- Что сделано (кратко):
  - Проведена сверка реализации `FR-005..FR-009` и `AC-005..AC-009` по слоям `model/controller/service/dto/test`.
  - Сформирована матрица соответствия: `FR implemented=3, partial=2, missing=0`; `AC implemented=3, partial=2, missing=0`.
  - Зафиксированы ключевые gaps: упрощённый runtime проверки practice (фактически первый вопрос), неполная агрегация scoring по question-level баллам, частичные DTO/model расхождения.
  - Выполнена валидация релевантным интеграционным тестом `CourseLessonCrudIntegrationTest` (`BUILD SUCCESS`).
  - Обновлены контексты memory-bank и индекс артефактов pre-phase.
- Какие модули затронуты:
  - `memory-bank/pre-task-artifacts/PRE-010-fr-005-fr-009-gap-report.md`
  - `memory-bank/pre-tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к `PRE-011` (сверка `FR-010..FR-014` с `AC-010..AC-014`) как следующей critical ready-to-start задаче.

### 2026-02-20 — Закрыта pre-задача PRE-007 (интеграции SMTP/YouTube/файловое хранение)
- Что сделано (кратко):
  - Выполнена сверка интеграций SMTP/YouTube/файловых путей и деградационных сценариев относительно `FR-002/007/019`, `AC-002/007/019`, `NFR-REL-04`, `NFR-SCL-03`, `NFR-SEC-05`.
  - Подтверждён fallback-контур `NoopEmailService` при отсутствии `JavaMailSender`, зафиксированы риски синхронной email-отправки (rollback user-flow при SMTP-сбое) и отсутствие retry/outbox.
  - Зафиксированы gaps по отсутствию backend-валидации внешних YouTube/PDF ссылок и контролируемой обработки недоступного контента.
  - Сформирован отдельный артефакт анализа: `memory-bank/pre-task-artifacts/PRE-007-integrations-gap-report.md`.
- Какие модули затронуты:
  - `memory-bank/pre-task-artifacts/PRE-007-integrations-gap-report.md`
  - `memory-bank/pre-tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к `PRE-008` (покрытие интеграционных/сквозных тестов по AC) как следующей ready-to-start задаче.

### 2026-02-20 — Закрыта pre-задача PRE-012 (FR-015..FR-017, statistics/reporting)
- Что сделано (кратко):
  - Проведена сверка реализации статистики/отчётности по `FR-015..FR-017` и `AC-015..AC-017`.
  - Зафиксированы статусы соответствия: `FR-015=partial`, `FR-016=partial`, `FR-017=partial`, `AC-017=missing`.
  - Подтверждена формула эффективности (`earned/max*100`), выявлены gap по явному полю `% выполнения` и по формату/составу колонок course/global отчётов.
  - Сформирован отдельный артефакт анализа: `memory-bank/pre-task-artifacts/PRE-012-fr-015-fr-017-gap-report.md`.
- Какие модули затронуты:
  - `memory-bank/pre-task-artifacts/PRE-012-fr-015-fr-017-gap-report.md`
  - `memory-bank/pre-tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к `PRE-013` как следующей ready-to-start pre-задаче.

### 2026-02-20 — Закрыта pre-задача PRE-013 (FR-018..FR-019, password/security)
- Что сделано (кратко):
  - Проведена сверка password-контуров `change-password` / `recover-password` / `set-password` относительно `FR-018..FR-019` и `AC-018..AC-019`.
  - Зафиксированы статусы соответствия: `FR-018=implemented`, `AC-018=implemented`, `FR-019=partial`, `AC-019=partial`.
  - Выполнена трассировка `NFR-SEC-01..05`; выявлены ключевые gaps: отсутствие TTL reset-токенов, отсутствие rate-limit/anti-abuse, user-enumeration в recover-flow, утечка internal error message в `500`, секреты в `application.yml`.
  - Подтверждён прогон релевантного интеграционного теста `UserAuthStudentFlowIntegrationTest` (`BUILD SUCCESS`).
  - Сформирован отдельный артефакт анализа: `memory-bank/pre-task-artifacts/PRE-013-fr-018-fr-019-security-gap-report.md`.
- Какие модули затронуты:
  - `memory-bank/pre-task-artifacts/PRE-013-fr-018-fr-019-security-gap-report.md`
  - `memory-bank/pre-tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к `PRE-014` как следующей ready-to-start pre-задаче; параллельно доступны `PRE-016` и `PRE-017`.

### 2026-02-20 — Закрыта pre-задача PRE-014 (FR-101..FR-105, sections/programs)
- Что сделано (кратко):
  - Выполнена сверка реализации `FR-101..FR-105` и `AC-101..AC-105` по слоям `model/service/controller/repository/dto/migration`.
  - Сформирована матрица соответствия: `FR implemented=0, partial=3, missing=2`; `AC implemented=0, partial=3, missing=2`.
  - Зафиксированы ключевые gaps: отсутствие блока sections (`FR-101/102`), закомментированные program endpoint-ы, частичная реализация deadline-ограничений программ.
  - Подготовлен отдельный артефакт анализа: `memory-bank/pre-task-artifacts/PRE-014-fr-101-fr-105-gap-report.md`.
- Какие модули затронуты:
  - `memory-bank/pre-task-artifacts/PRE-014-fr-101-fr-105-gap-report.md`
  - `memory-bank/pre-tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к `PRE-015` как следующей ready-to-start pre-задаче; параллельно доступны `PRE-016` и `PRE-017`.

### 2026-02-20 — Закрыта pre-задача PRE-015 (FR-106..FR-109, groups/mass assignments/search/filter)
- Что сделано (кратко):
  - Выполнена сверка реализации `FR-106..FR-109` и `AC-106..AC-109` по слоям `model/repository/service/controller/dto/migration`.
  - Сформирована матрица соответствия: `FR implemented=0, partial=4, missing=0`; `AC implemented=0, partial=4, missing=0`.
  - Зафиксированы ключевые gaps: закомментированные endpoint-ы групп и массовых назначений, отсутствие persisted-связи `group -> target` и авто-применения доступов при изменении состава группы, отсутствие server-side search/filter для каталогов пользователей и курсов.
  - Подготовлен отдельный артефакт анализа: `memory-bank/pre-task-artifacts/PRE-015-fr-106-fr-109-gap-report.md`.
- Какие модули затронуты:
  - `memory-bank/pre-task-artifacts/PRE-015-fr-106-fr-109-gap-report.md`
  - `memory-bank/pre-tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к `PRE-016` как следующей ready-to-start pre-задаче; параллельно доступна `PRE-017`.

### 2026-02-20 — Закрыта pre-задача PRE-016 (FR-110..FR-114, advanced practice constraints)
- Что сделано (кратко):
  - Выполнена сверка реализации `FR-110..FR-114` и `AC-110..AC-114` по слоям `model/dto/mapper/service/controller/test`.
  - Сформирована матрица соответствия: `FR implemented=0, partial=4, missing=1`; `AC implemented=0, partial=4, missing=1`.
  - Зафиксированы ключевые gaps: отсутствие runtime-enforcement для `passingThresholdPercent`, `attemptLimit`, `timeLimitMinutes/deadlineDays`; отсутствие применения `randomQuestionCount/shuffleOnEveryAttempt` в learner-submit flow; отсутствие server-side gate-логики `stopLesson` с приоритетом над `lessonsFreeOrder`.
  - Подтверждён релевантный интеграционный прогон `CourseLessonCrudIntegrationTest` (`BUILD SUCCESS`).
  - Подготовлен отдельный артефакт анализа: `memory-bank/pre-task-artifacts/PRE-016-fr-110-fr-114-gap-report.md`.
- Какие модули затронуты:
  - `memory-bank/pre-task-artifacts/PRE-016-fr-110-fr-114-gap-report.md`
  - `memory-bank/pre-tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к `PRE-017` как следующей ready-to-start pre-задаче; после её завершения перейти к консолидации `PRE-018`.

### 2026-02-20 — Закрыта pre-задача PRE-020 и зафиксирована новая baseline-версия backlog
- Что сделано (кратко):
  - Выполнена финальная синхронизация `memory-bank/tasks.json` по согласованному change-set из `PRE-019`.
  - Обновлены статусы задач по фактической реализации (`TASK-009`, `TASK-010`, `TASK-021` → `done`; `TASK-025` → `obsolete`).
  - Выполнен split задач: `TASK-019 -> TASK-019A/TASK-019B`, `TASK-020 -> TASK-020A/TASK-020B`, `TASK-022 -> TASK-022A/TASK-022B`.
  - Добавлены gap-closure задачи `TASK-041..TASK-046` (activation state, self-profile update, enrollment lists API, review statuses, audit-trail, backup/restore readiness).
  - Проведена валидация обновлённого backlog: JSON корректен, `tasks_count=49`, битых зависимостей нет (`dependency_errors=0`).
  - Обновлены `memory-bank/pre-tasks.json` (`PRE-020=done`), `02-active-context.md`, `05-task-execution-progress.md`; создан итоговый артефакт `PRE-020-backlog-baseline-update-report.md`.
- Какие модули затронуты:
  - `memory-bank/tasks.json`
  - `memory-bank/pre-tasks.json`
  - `memory-bank/pre-task-artifacts/PRE-020-backlog-baseline-update-report.md`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Pre-phase завершён (`PRE-001..PRE-020`); перейти к реализации основной очереди из `tasks.json` с первой ready-to-start critical-задачи `TASK-001`.

### 2026-02-20 — Завершена TASK-001 (runtime-конфигурации MVP)
- Что сделано (кратко):
  - Подготовлен runtime-контур `dev/stage/prod` с profile-groups и выносом чувствительных параметров в env.
  - Добавлены Docker/runtime-артефакты (`monolith-mvp/Dockerfile`, `docker-compose.monolith.yml`, `monolith.env.example`, `monolith-mvp/README-runtime.md`) и health-check через actuator.
  - Обновлён дефолт в `monolith.env.example` на `pg,mail-noop` для локального smoke-check без внешнего SMTP.
  - Пройдено тестирование задачи: docker compose запуск, проверка логов подключения к БД, `/actuator/health = UP`.
- Какие модули затронуты:
  - `monolith-mvp/src/main/resources/application.yml`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/config/SecurityConfig.java`
  - `monolith-mvp/pom.xml`
  - `monolith-mvp/Dockerfile`
  - `monolith-mvp/.dockerignore`
  - `docker-compose.monolith.yml`
  - `monolith.env.example`
  - `monolith-mvp/README-runtime.md`
  - `.gitignore`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к `TASK-002` (Flyway migration stabilization) как следующей ready-to-start critical задаче.

### 2026-02-20 — Завершена TASK-002 (Flyway migration stabilization)
- Что сделано (кратко):
  - Проведена проверка стабильности Flyway-миграций на чистой PostgreSQL в docker-compose контуре.
  - Подтверждено успешное первичное применение миграции `V1__init_schema.sql` и корректное состояние `flyway_schema_history`.
  - Подтверждён повторный идемпотентный прогон миграций после restart приложения (`Schema "public" is up to date. No migration necessary.`).
  - Дополнительно проверен runtime health-check: `/actuator/health` возвращает `UP`.
- Какие модули затронуты:
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к `TASK-003` (усиление JWT-аутентификации) как следующей ready-to-start critical задаче.
