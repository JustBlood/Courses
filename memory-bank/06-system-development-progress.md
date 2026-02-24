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

### 2026-02-20 — Завершена TASK-003 (JWT hardening)
- Что сделано (кратко):
  - Усилен контур JWT-аутентификации: добавлены единообразные security-обработчики `401/403` (`ApiAuthenticationEntryPoint`, `ApiAccessDeniedHandler`).
  - `SecurityConfig` обновлён явной настройкой `exceptionHandling` с подключением кастомных `authenticationEntryPoint/accessDeniedHandler`.
  - `JwtAuthenticationFilter` усилен очисткой `SecurityContext` при ошибке парсинга/валидации JWT.
  - Расширен интеграционный тест `UserAuthStudentFlowIntegrationTest`: добавлены кейсы valid/expired/tampered JWT и скорректировано ожидаемое поведение для деактивированного пользователя (`401`).
  - Подтверждён успешный прогон валидации: `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test` (`BUILD SUCCESS`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/config/SecurityConfig.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/security/JwtAuthenticationFilter.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/security/ApiAuthenticationEntryPoint.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/security/ApiAccessDeniedHandler.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к `TASK-004` (RBAC-ограничения API) как следующей ready-to-start critical задаче.

### 2026-02-20 — Завершена TASK-004 (RBAC-ограничения API)
- Что сделано (кратко):
  - Усилен RBAC-контур review endpoint: неназначенный reviewer теперь получает `403 Forbidden` вместо бизнес-ошибки `400`.
  - В `LearningService.reviewOpenSubmission` применён security-ориентированный отказ через `AccessDeniedException` при отсутствии назначения reviewer на курс.
  - Обновлён интеграционный сценарий `CourseLessonCrudIntegrationTest` под новое ожидаемое поведение доступа.
  - Подтверждён успешный прогон валидации: `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest,CourseLessonCrudIntegrationTest test` (`BUILD SUCCESS`, `Tests run: 4`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start задаче backlog с учётом приоритетов и зависимостей (кандидат: `TASK-005`).

### 2026-02-20 — Завершена TASK-006 (создание пользователя с первичными назначениями)
- Что сделано (кратко):
  - Расширен контракт `CreateUserRequest`: добавлены `groupIds` и `courseIds` для первичных назначений при создании пользователя.
  - В `UserService.createUser` реализованы первичные назначения в рамках транзакции: валидация входных ID, проверка существования групп/курсов, защита от дублей/null и создание `GroupMembership`/`Enrollment`.
  - Добавлен контроль ограничения membership по typed-группам (не более одной группы каждого типа для пользователя).
  - Обновлён CSV import-flow под новый DTO-конструктор.
  - Расширен интеграционный тест `UserAuthStudentFlowIntegrationTest` сценарием создания пользователя с `groupIds/courseIds` и проверкой сохранения связей в БД.
  - Подтверждена валидация: `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest,CourseLessonCrudIntegrationTest test` (`BUILD SUCCESS`, `Tests run: 4, Failures: 0, Errors: 0`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/user/CreateUserRequest.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/UserService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче backlog критичного приоритета: `TASK-007` (FR-002 email-onboarding).

### 2026-02-20 — Завершена TASK-007 (email-onboarding, FR-002)
- Что сделано (кратко):
  - Подтверждён полный onboarding-контур: создание пользователя без пароля генерирует одноразовый токен установки пароля и формирует ссылку для email-инвайта.
  - Подтверждена интеграция с email-слоем (`EmailService` + fallback `NoopEmailService`) и сквозной сценарий `set-password -> login`.
  - Подтверждена защита от повторного использования токена и рабочий recover-password flow с выдачей нового токена.
  - В backlog `memory-bank/tasks.json` задача `TASK-007` переведена в `done`.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/UserService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/AuthService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/AuthController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/EmailService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/YandexSmtpEmailService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/NoopEmailService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче backlog: `TASK-008` (FR-003/FR-004: редактирование пользователя и смена роли).

### 2026-02-20 — Завершена TASK-008 (FR-003/FR-004: редактирование пользователя и смена роли)
- Что сделано (кратко):
  - Реализован self-profile update endpoint `PATCH /api/v1/student/my/profile` с whitelist разрешённых полей (`fullName`, `phone`, `comment`).
  - Добавлен `UpdateMyProfileRequest` и сервисный метод `UserService.updateMyProfile` с валидацией и запретом пустого/некорректного обновления.
  - В `GlobalExceptionHandler` добавана обработка `HttpMessageNotReadableException` для контролируемого `400` на payload с запрещёнными полями.
  - Подтверждён немедленный эффект смены роли: после admin role patch существующий JWT-token начинает работать с новыми правами без повторного login.
  - Обновлён интеграционный тест `UserAuthStudentFlowIntegrationTest` (self-profile update + immediate role effect).
  - В backlog `memory-bank/tasks.json` задача `TASK-008` переведена в `done`.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/StudentController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/UserService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/student/UpdateMyProfileRequest.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/exception/GlobalExceptionHandler.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче backlog: `TASK-011`.

### 2026-02-20 — Завершена TASK-011 (FR-008/FR-009: PRACTICE-вопросы и балльная модель)
- Что сделано (кратко):
  - Усилен data/model-контур PRACTICE: default points на вопросах теперь корректно наследуются с уровня урока (`PracticeLesson`) и могут переопределяться на уровне вопроса.
  - Исправлена валидация позиций вопросов: проверка непрерывности применяется только при полном режиме явных позиций.
  - Расширен контракт submit-practice: добавлено поле `questionAnswers` для передачи ответов по индексу вопроса с сохранением обратной совместимости (`selectedAnswers`).
  - В `LearningService.submitPractice` реализована агрегация scoring по всему question pool, включая partial scoring для `MULTIPLE_CHOICE` и итоговый `passed` по `passingThresholdPercent`.
  - Добавлен интеграционный тест на all question types (`single/multiple/matching/ordering`) и частично корректный multiple-choice ответ.
  - Подтверждена валидация: `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` (`BUILD SUCCESS`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/learning/PracticeSubmissionRequest.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI critical задаче: `TASK-012` (двухсписочная модель enrollment: enrolled/not-enrolled).

### 2026-02-20 — Завершена TASK-012 (FR-010: enrollment two-list model)
- Что сделано (кратко):
  - Реализован API-контракт двух списков enrollment по курсу: `enrolled` и `notEnrolled`.
  - Добавлен endpoint `GET /api/v1/admin/courses/{courseId}/enrollments/lists` для получения обоих списков в одном ответе.
  - В `CourseService` реализованы batch-операции `enroll/unenroll` через `IdsRequest` и валидация входных IDs (существование пользователей + роль `STUDENT`).
  - Репозиторий пользователей расширен выборкой `findAllByRole(Role.STUDENT)` для построения списка `notEnrolled`.
  - Расширен интеграционный тест `CourseLessonCrudIntegrationTest`: добавлен сценарий полного цикла (initial lists -> enroll -> доступ к курсу -> unenroll -> потеря доступа).
  - В backlog `memory-bank/tasks.json` задача `TASK-012` переведена в `done`.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/course/CourseEnrollmentListsDto.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/AppUserRepository.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Следующая ready-to-start non-UI critical задача по зависимостям: `TASK-013` (назначение reviewer и рабочая область проверки).

### 2026-02-20 — Завершена TASK-013 (FR-011: reviewer assignment и reviewer workspace)
- Что сделано (кратко):
  - Расширена рабочая область reviewer: добавлен endpoint `GET /api/v1/admin/progress/reviews/courses` для списка назначенных курсов.
  - В `CourseService` добавлены методы получения reviewer-курсов (`getReviewerCourseSummaries`, `getMyReviewerCourseSummaries`) и выделен общий маппинг `toCourseSummaryDto`.
  - Подтверждена и покрыта тестами логика FR-011: назначение reviewer только из `ADMIN` и видимость pending-open-submissions только по назначенным курсам.
  - В backlog `memory-bank/tasks.json` задача `TASK-013` переведена в `done`.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/ProgressController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Следующая ready-to-start non-UI critical задача по зависимостям: `TASK-014` (FR-012: прохождение THEORY-урока с фиксацией статуса и начислением баллов).

### 2026-02-20 — Завершена TASK-014 (FR-012: прохождение THEORY-урока)
- Что сделано (кратко):
  - Подтверждено соответствие backend-логики требованиям `FR-012 / AC-012`: THEORY доступна только при enrollment, завершение фиксируется как `LessonSubmission(status=COMPLETE)`, баллы начисляются по `lesson.fullPoints`.
  - Добавлен интеграционный тест `theory_lesson_completion_should_update_progress_and_stats` в `CourseLessonCrudIntegrationTest`.
  - Тест покрывает полный сценарий: отказ до enrollment, успешное `complete-theory` после enrollment, обновление статистики студента и статистики курса.
  - В backlog `memory-bank/tasks.json` задача `TASK-014` переведена в `done`.
- Какие модули затронуты:
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Следующая ready-to-start non-UI critical задача: `TASK-015` (FR-013 runtime scoring engine).

### 2026-02-20 — Завершена TASK-015 (FR-013: runtime scoring engine)
- Что сделано (кратко):
  - Подтверждён и стабилизирован runtime scoring flow для practice-урока с full question-pool и partial scoring в интеграционном контуре.
  - В `CourseLessonCrudIntegrationTest` скорректировано ожидание `maxPoints` в кейсе
    `practice_lesson_should_support_all_question_types_and_partial_scoring`
    с `5` на `1` в соответствии с текущим контрактом статистики курса.
  - Подтверждена валидность итогового scoring-состояния попытки (`pointsAwarded=3`, `status=COMPLETE`, `passed=true`).
  - В backlog `memory-bank/tasks.json` задача `TASK-015` переведена в `done`.
- Какие модули затронуты:
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Следующая ready-to-start non-UI critical задача по зависимостям: `TASK-016` (FR-014 workflow развёрнутого ответа `pending -> rework -> accepted`).

### 2026-02-20 — Завершена TASK-016 (FR-014: workflow open-ended ответа pending -> rework -> accepted)
- Что сделано (кратко):
  - Реализованы явные переходы review-статусов для open-ended submission: `PENDING_REVIEW -> REWORK -> ACCEPTED`.
  - Обновлён `LearningService.reviewOpenSubmission`: возврат на доработку переводит в `REWORK`, финальное принятие — в `ACCEPTED`.
  - Расширен reviewer pending-list: учитываются submissions в статусах `PENDING_REVIEW` и `REWORK`.
  - Обновлён интеграционный тест `CourseLessonCrudIntegrationTest` под новый контракт статусов.
  - Подтверждена валидация: `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` (`BUILD SUCCESS`, `Tests run: 6, Failures: 0, Errors: 0`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Следующая ready-to-start non-UI задача по зависимостям: `TASK-017`.

### 2026-02-20 — Завершена TASK-017 (FR-015: личная статистика студента/пользователя)
- Что сделано (кратко):
  - В личной статистике добавлен явный показатель `% выполнения` (`progressPercent`) наряду с баллами и `efficiencyPercent`.
  - В `StatisticsService` унифицирован расчёт статистики через переиспользуемый метод `userCourseStats(userId)`.
  - Добавлен admin-view endpoint `GET /api/v1/admin/users/{userId}/stats` для просмотра статистики любого пользователя.
  - Формула `% выполнения` реализована как `completedLessons * 100 / totalLessons`, что синхронизировано с PRD (`FR-015/AC-015`).
  - Обновлён интеграционный тест `CourseLessonCrudIntegrationTest`: проверки student-view/admin-view, `efficiencyPercent` и `progressPercent`.
  - Подтверждена валидация: `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` (`BUILD SUCCESS`, `Tests run: 6, Failures: 0, Errors: 0`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/stat/StudentCourseStatDto.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/StatisticsService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/UsersController.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Следующая ready-to-start non-UI задача по зависимостям: `TASK-018` (FR-016: статистика по конкретному курсу для администратора).

### 2026-02-22 — Завершена TASK-018 (FR-016: статистика по конкретному курсу для администратора)
- Что сделано (кратко):
  - В course statistics добавлен явный показатель `% выполнения` (`progressPercent`) для каждого студента курса.
  - В `StatisticsService.courseStats(Long courseId)` реализован и закреплён расчёт `progressPercent` по формуле `completedLessons * 100 / totalLessons`.
  - Обеспечена консистентность course stats с личной статистикой пользователя (admin-view), включая `fullName`, `earnedPoints/maxPoints`, `efficiencyPercent`, `progressPercent`.
  - Добавлен и подтверждён интеграционный сценарий на двух студентов с разным прогрессом и сверкой значений между endpoint-ами.
  - Обновлён task-артефакт и индексы memory-bank; `TASK-018` переведена в `done` в `memory-bank/tasks.json`.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/stat/CourseStudentStatDto.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/StatisticsService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-018.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/06-system-development-progress.md`
  - `memory-bank/02-active-context.md`
- Что отложено / следующий крупный этап:
  - Следующая ready-to-start non-UI задача по зависимостям: `TASK-019A` (FR-017: сводный отчёт по конкретному курсу).

### 2026-02-24 — Завершена TASK-019A (FR-017 MVP: course-specific summary report)
- Что сделано (кратко):
  - Добавлен отдельный endpoint выгрузки CSV-отчёта по конкретному курсу: `GET /api/v1/admin/progress/courses/{courseId}/summary-report.csv`.
  - В `StatisticsService` реализована генерация course-specific отчёта с колонками и порядком строго по `FR-017/AC-017`.
  - Реализовано заполнение обязательных пустых полей по ТЗ (`Логин`, `cid`, `Медалей`, `Номер сертификата`, `Ссылка`, `Продолжительность`).
  - Для эффективного построения отчёта добавлены batch repository-методы по пользователям (`GroupMembershipRepository`, `ProgramEnrollmentRepository`).
  - Добавлен интеграционный тест `course_summary_report_csv_should_match_required_columns_and_stats`, проверяющий:
    - точную структуру заголовка CSV,
    - соответствие ключевых метрик (`Баллов`, `Эффективность`, `Прогресс`, `Уроков`) данным course stats,
    - корректность обязательных пустых колонок.
  - Подтверждён прогон: `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS` (`Tests run: 8, Failures: 0, Errors: 0`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/ProgressController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/StatisticsService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/GroupMembershipRepository.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/ProgramEnrollmentRepository.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-019A.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/06-system-development-progress.md`
  - `memory-bank/02-active-context.md`
- Что отложено / следующий крупный этап:
  - Следующая ready-to-start non-UI задача по зависимостям: `TASK-020A` (FR-017 MVP: общий сводный отчёт по всем курсам).

### 2026-02-24 — Завершена TASK-020A (FR-017 MVP: общий сводный отчёт по всем курсам)
- Что сделано (кратко):
  - Подтверждён и зафиксирован общий CSV-отчёт по всем курсам в контракте `FR-017/AC-017` с корректным набором и порядком колонок.
  - Закреплена корректная обработка студентов, назначенных на несколько курсов: отдельная строка на каждое назначение (`Enrollment`).
  - В интеграционном тесте `summary_report_csv_should_match_required_columns_and_have_row_per_course_assignment` убрана хрупкая проверка общего количества строк и зафиксирована валидация по целевому студенту (2 строки на 2 курса).
  - Подтверждён прогон тестов: `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS`.
  - Задача переведена в `done`, создан артефакт `memory-bank/task-artifacts/TASK-020A.md`, индекс `memory-bank/05-task-execution-progress.md` обновлён.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/StatisticsService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-020A.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/06-system-development-progress.md`
  - `memory-bank/02-active-context.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по зависимостям и приоритету: `TASK-022A` (FR-019 base flow: восстановление пароля по email-токену).

### 2026-02-24 — Завершена TASK-022A (FR-019 base flow: восстановление пароля)
- Что сделано (кратко):
  - Подтверждён рабочий base flow восстановления пароля `initiate + confirm` в auth-контуре без расширения scope.
  - Верифицировано, что `recover-password` создаёт одноразовый `PasswordSetupToken`, а `set-password` обновляет хеш пароля и помечает токен использованным.
  - Изменения в бизнес-код не вносились: текущая реализация полностью покрывает критерии `TASK-022A`.
  - Создан артефакт выполнения `memory-bank/task-artifacts/TASK-022A.md`, обновлён индекс выполненных задач.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/AuthController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/AuthService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/UserService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/PasswordSetupTokenRepository.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-022A.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Следующая ready-to-start non-UI high-priority задача: `TASK-026` (FR-101/FR-102: CRUD разделов каталога и создание курса в контексте раздела).

### 2026-02-24 — Завершена TASK-026 (FR-101/FR-102: sections + course-in-section)
- Что сделано (кратко):
  - Добавлен полноценный backend-модуль разделов каталога: `Section` entity, `SectionRepository`, DTO, `SectionService`, `SectionsController`.
  - Реализована связь `Course -> Section` и расширены course DTO/mapper section-полями (`sectionId`, `sectionTitle`, `sectionPriority`).
  - Добавлен endpoint создания курса в контексте раздела: `POST /api/v1/admin/sections/{sectionId}/courses`.
  - Реализована сортировка каталога курсов по разделам (`section.priority`, `section.id`, `course.id`) с корректной обработкой `section = null`.
  - Обновлена Flyway-схема: таблица `sections`, FK `courses.section_id`, индекс.
  - Добавлен интеграционный `SectionCatalogIntegrationTest`; устранена нестабильность проверок каталога (убрана зависимость от фиксированного общего размера).
  - Подтверждён прогон: `mvn -pl monolith-mvp -Dtest=SectionCatalogIntegrationTest,CourseLessonCrudIntegrationTest test` (`BUILD SUCCESS`, `Tests run: 11`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/Course.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/Section.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/SectionRepository.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/CourseRepository.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/section/*`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/course/CreateCourseRequest.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/course/CourseDto.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/course/CourseSummaryDto.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/mapper/CourseMapper.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/SectionService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/SectionsController.java`
  - `monolith-mvp/src/main/resources/db/migration/V1__init_schema.sql`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/SectionCatalogIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-026.md`
  - `memory-bank/tasks.json`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
  - `memory-bank/02-active-context.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче из `memory-bank/tasks.json` по приоритету и зависимостям.

### 2026-02-24 — Завершена TASK-027 (FR-106/FR-107: groups typed-membership)
- Что сделано (кратко):
  - Реализовано редактирование групп через новый контракт `UpdateGroupRequest` и endpoint `PUT /api/v1/admin/groups/{groupId}`.
  - В `GroupService` добавлен `updateGroup(...)` с валидацией конфликтов при смене типа группы.
  - Ужесточено правило typed-membership: для `COMPANY/DEPARTMENT/POSITION` запрещены конфликтующие назначения (reject), вместо прежнего неявного replace-поведения.
  - Добавлен интеграционный `GroupManagementIntegrationTest` на single typed membership и конфликтную смену типа группы.
  - Подтверждён регрессионный прогон `UserAuthStudentFlowIntegrationTest`.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/group/UpdateGroupRequest.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/GroupService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/UsersController.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/GroupManagementIntegrationTest.java`
  - `memory-bank/tasks.json`
  - `memory-bank/task-artifacts/TASK-027.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
  - `memory-bank/02-active-context.md`
- Что отложено / следующий крупный этап:
  - Следующая ready-to-start non-UI задача по backlog и зависимостям: `TASK-028` или `TASK-030` (в зависимости от выбранного порядка приоритезации).

### 2026-02-24 — Завершена TASK-030 (FR-103: создание/редактирование программ обучения)
- Что сделано (кратко):
  - Включены admin API программ (`create/list/get/update`) в `CoursesController`.
  - В `ProgramService` реализован `updateProgram(...)` с валидацией входного списка курсов и безопасной пересборкой ordered-связей `program_courses`.
  - Добавлены Flyway-миграции для выравнивания схемы `learning_programs` и default для `program_courses.block_after_deadline`.
  - Добавлен интеграционный `ProgramManagementIntegrationTest` на сценарий `create -> reorder update -> get/list` с проверкой порядка курсов.
  - Подтверждён прогон: `mvn -f monolith-mvp/pom.xml -Dtest=ProgramManagementIntegrationTest test -q` (`BUILD SUCCESS`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/ProgramService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/ProgramCourseRepository.java`
  - `monolith-mvp/src/main/resources/db/migration/V2__align_learning_program_columns.sql`
  - `monolith-mvp/src/main/resources/db/migration/V3__program_courses_default_block_after_deadline.sql`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ProgramManagementIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-030.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/06-system-development-progress.md`
  - `memory-bank/02-active-context.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по приоритету и зависимостям из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-028 (FR-108 массовые назначения через группы)
- Что сделано (кратко):
  - Доведена до конца backend-реализация массовых назначений курсов и программ через группы с постоянными связями `group -> target`.
  - В `ProgramService.assignGroupToProgram(...)` добавлена фиксация persistent assignment `group_program_assignments` и backfill текущих участников группы.
  - В `GroupService.addUsersToGroup(...)` реализовано автоприменение активных group-assignment к новым участникам (course/program), только для роли `STUDENT`.
  - В `CoursesController` включены admin endpoint-ы назначения групп на курс/программу.
  - Добавлен интеграционный сценарий `should_assign_course_and_program_to_group_and_auto_apply_for_new_member`.
  - Подтверждён прогон: `mvn -f monolith-mvp/pom.xml -Dtest=GroupManagementIntegrationTest test` (`BUILD SUCCESS`, `Tests run: 3`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/ProgramService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/GroupService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/GroupManagementIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-028.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по зависимостям и приоритету из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-031 (FR-104: правила прохождения программы)
- Что сделано (кратко):
  - В `ProgramService` обновлена логика доступности курсов программы: итоговая формула `available = availableByAccessCondition && availableByDeadline`.
  - Реализована явная проверка дедлайна через `isAvailableByDeadline(...)` с корректным поведением для завершённых курсов (после дедлайна остаются доступными).
  - Добавлен интеграционный тест `should_apply_program_access_rules_and_deadline_blocking_for_student` в `ProgramManagementIntegrationTest`.
  - Подтверждён прогон: `mvn -f monolith-mvp/pom.xml -Dtest=ProgramManagementIntegrationTest test -q` (успешно).
  - Создан артефакт `memory-bank/task-artifacts/TASK-031.md`, обновлён индекс `memory-bank/05-task-execution-progress.md`, задача переведена в `done`.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/ProgramService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ProgramManagementIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-031.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по зависимостям и приоритету из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-032 (FR-105: назначение программ пользователю и группе)
- Что сделано (кратко):
  - Включён admin endpoint назначения программы пользователям: `POST /api/v1/admin/courses/programs/{programId}/assign`.
  - Включены student endpoint'ы кабинета программ: `GET /api/v1/student/my/programs` и `GET /api/v1/student/my/programs/{programId}`.
  - Добавлен e2e-интеграционный тест `should_assign_program_to_user_and_group_and_expose_in_student_cabinet()`.
  - Подтверждён сквозной сценарий AC-105: назначение программы пользователю и группе отражается в `program_enrollments`, автозачисляет на курсы программы и видимо в student API.
  - Подтверждён прогон: `mvn -f monolith-mvp/pom.xml -Dtest=ProgramManagementIntegrationTest,GroupManagementIntegrationTest test` (`BUILD SUCCESS`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/StudentController.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ProgramManagementIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-032.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по зависимостям и приоритету из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-033 (FR-110/FR-111: threshold + attemptLimit)
- Что сделано (кратко):
  - Добавлен runtime-контроль лимита попыток practice-урока (`attemptLimit`) в `LearningService.submitPractice(...)` до создания новой submission.
  - Для подсчёта использованных попыток добавлен метод репозитория `countByStudentIdAndLessonId(...)`.
  - Сохранён текущий API-контракт `POST /api/v1/student/lessons/{lessonId}/submit-practice` и действующая логика threshold.
  - Добавлен интеграционный тест `practice_attempt_limit_should_block_third_attempt_after_two_failed()`.
  - Подтверждён прогон: `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest test` (`BUILD SUCCESS`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/LessonSubmissionRepository.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-033.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по зависимостям и приоритету из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-037 (observability-контур)
- Что сделано (кратко):
  - Реализован end-to-end observability-контур: correlation-id, structured business events, auth/email/http метрики.
  - В security chain подключены `CorrelationIdFilter` и `HttpServerMetricsFilter`.
  - Интегрированы structured-события в ключевые сервисы (`AuthService`, `UserService`, `CourseService`, `LearningService`, email-сервисы).
  - Усилен `GlobalExceptionHandler` structured error-логированием.
  - Добавлен `ObservabilityIntegrationTest` (проверка `X-Correlation-Id` и `/actuator/metrics`).
  - Подтверждён релевантный прогон: `mvn -f monolith-mvp/pom.xml -Dtest=ObservabilityIntegrationTest,UserAuthStudentFlowIntegrationTest test` (`BUILD SUCCESS`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/observability/*`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/security/CorrelationIdFilter.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/config/SecurityConfig.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/AuthService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/UserService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/YandexSmtpEmailService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/NoopEmailService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/exception/GlobalExceptionHandler.java`
  - `monolith-mvp/src/main/resources/application.yml`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ObservabilityIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-037.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по приоритету и зависимостям из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-041 (FR-002/AC-002: activation-state пользователя)
- Что сделано (кратко):
  - В доменной модели `AppUser` добавлен явный флаг `activated` с миграцией Flyway `V5__users_activation_state.sql`.
  - В onboarding-логике зафиксированы правила активации: invite-пользователь создаётся неактивированным, после `set-password` переводится в активный.
  - В auth-контуре вход ограничен условием `enabled && activated` через `AppUserDetailsService`.
  - Расширен API-контракт `UserDto` полем `activated` для консистентного отражения activation-state.
  - Подтверждён интеграционный сценарий `UserAuthStudentFlowIntegrationTest`: до set-password login отклоняется, после set-password login успешен.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/AppUser.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/user/UserDto.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/UserService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/AuthService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/security/AppUserDetailsService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/init/BootstrapAdminInitializer.java`
  - `monolith-mvp/src/main/resources/db/migration/V5__users_activation_state.sql`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-041.md`
  - `memory-bank/tasks.json`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по приоритету и зависимостям из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-042 (FR-003/AC-003: self-profile update whitelist)
- Что сделано (кратко):
  - Подтверждено и формально закрыто соответствие `FR-003/AC-003` для self-profile update.
  - Зафиксировано, что endpoint `PATCH /api/v1/student/my/profile` работает по whitelist (`fullName`, `phone`, `comment`) и отклоняет попытки изменения запрещённых полей.
  - Подтверждена недеградация админского update-flow и релевантный прогон интеграционного теста `UserAuthStudentFlowIntegrationTest` (`BUILD SUCCESS`).
  - Статус `TASK-042` переведён в `done`, создан артефакт `memory-bank/task-artifacts/TASK-042.md` и обновлён индекс выполненных задач.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/StudentController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/student/UpdateMyProfileRequest.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/UserService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-042.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по приоритету и зависимостям из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-043 (FR-010/AC-010: enrolled/not-enrolled lists API)
- Что сделано (кратко):
  - Подтверждено соответствие FR-010/AC-010 для API-контракта двух списков зачисления: `enrolled` и `notEnrolled`.
  - Зафиксировано наличие и корректность endpoint-ов списка и операций перемещения пользователей между списками.
  - Выполнена валидация релевантным интеграционным тестом `CourseLessonCrudIntegrationTest#enrollment_two_lists_flow_should_work` (`BUILD SUCCESS`).
  - Статус `TASK-043` переведён в `done`, создан артефакт `memory-bank/task-artifacts/TASK-043.md`, обновлён индекс выполненных задач.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/course/CourseEnrollmentListsDto.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/EnrollmentRepository.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-043.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по приоритету и зависимостям из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-044 (FR-014/AC-014: review workflow + history FK cascade)
- Что сделано (кратко):
  - Доведён интеграционный сценарий review open-ended ответа до целевого workflow `PENDING_REVIEW -> REWORK -> ACCEPTED`.
  - В `CourseLessonCrudIntegrationTest` исправлен шаг review: первый переход выполняется через `toNextReview=true` и `passed=false`.
  - Добавлена Flyway-миграция `V7__lesson_submission_status_history_on_delete_cascade.sql` для перевода FK истории статусов на `ON DELETE CASCADE`.
  - Устранена регрессия `500 DataIntegrityViolationException` при удалении урока/сабмишенов с историей статусов.
  - Подтверждён прогон: `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` (`BUILD SUCCESS`, `Tests run: 10, Failures: 0, Errors: 0`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/resources/db/migration/V7__lesson_submission_status_history_on_delete_cascade.sql`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-044.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
  - `memory-bank/02-active-context.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по приоритету и зависимостям из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-034 (FR-112: course/lesson time limits)
- Что сделано (кратко):
  - Реализовано ограничение срока прохождения курса через `deadlineDays`: проверка дедлайна по `Enrollment.enrolledAt` добавлена в learner-course view и student learning-flow.
  - Реализовано ограничение времени practice-урока через `timeLimitMinutes`: в `submitPractice` добавлена валидация окна времени на основе первой попытки по уроку.
  - Добавлен repository-метод для получения первой попытки урока студента в хронологическом порядке.
  - Добавлен интеграционный тест `time_limits_should_block_after_course_deadline_and_practice_time_limit`, покрывающий оба ограничения (course deadline + lesson time limit).
  - Подтверждён прогон: `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest test` (`BUILD SUCCESS`, `Tests run: 11`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/LessonSubmissionRepository.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-034.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по зависимостям и приоритету из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-035 (FR-113: randomQuestionCount + shuffleOnEveryAttempt)
- Что сделано (кратко):
  - В learner runtime-выдаче practice-урока реализовано применение параметров `shuffleOnEveryAttempt` и `randomQuestionCount`.
  - Логика выдачи теперь поддерживает ограниченную выборку вопросов из банка и рандомизацию порядка между попытками чтения урока.
  - Добавлен/обновлён интеграционный сценарий `AC-113` в `CourseLessonCrudIntegrationTest` (урок с 10 вопросами, `randomQuestionCount=5`, `shuffle=true`).
  - Подтверждён релевантный прогон: `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest test` (`BUILD SUCCESS`, `Tests run: 12, Failures: 0, Errors: 0`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-035.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче по зависимостям и приоритету из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-039 (backup/restore контур, MVP RPO/RTO)
- Что сделано (кратко):
  - Подтверждён рабочий backup/restore pipeline для монолита (`PostgreSQL + /opt/app/data`) через `scripts/backup/backup-monolith.sh` и `scripts/backup/restore-monolith.sh`.
  - Выполнен практический сценарий: backup -> симуляция потери данных (БД + файл) -> restore из snapshot.
  - Зафиксированы фактические метрики из отчёта восстановления: `rpo_seconds=38`, `restore_duration_sec=2`.
  - Подтверждено соответствие retention-требованию MVP: `retention_days=7`.
  - Создан task-артефакт: `memory-bank/task-artifacts/TASK-039.md`.
- Какие модули затронуты:
  - `scripts/backup/backup-monolith.sh`
  - `scripts/backup/restore-monolith.sh`
  - `docker-compose.monolith.yml`
  - `monolith.env.example`
  - `monolith-mvp/README-runtime.md`
  - `memory-bank/task-artifacts/TASK-039.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
  - `memory-bank/02-active-context.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-UI задаче из `memory-bank/tasks.json` по приоритету и зависимостям.

### 2026-02-24 — Завершена TASK-046 (backup/restore readiness + retention verification)
- Что сделано (кратко):
  - Подтверждена эксплуатационная готовность backup/restore-контура на базе ранее внедрённых скриптов.
  - Выполнен новый backup + restore-drill для snapshot `backups/monolith/20260224T185336Z`.
  - Зафиксированы фактические метрики восстановления: `restore_duration_sec=2`, `rpo_seconds=18`.
  - Подтверждена регулярная проверка retention: `check-backup-retention.sh` возвращает `OK` для окна `7d`.
  - Сформирован артефакт задачи `memory-bank/task-artifacts/TASK-046.md` и обновлён индекс выполненных задач.
- Какие модули затронуты:
  - `scripts/backup/backup.sh`
  - `scripts/backup/check-backup-retention.sh`
  - `scripts/backup/restore-monolith.sh`
  - `monolith.env.example`
  - `monolith-mvp/README-runtime.md`
  - `memory-bank/task-artifacts/TASK-046.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/02-active-context.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-ui задаче по зависимостям и приоритету из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-040 (сквозной e2e-набор приемки AC-001..AC-019 и AC-101..AC-114)
- Что сделано (кратко):
  - Сформирован и подтверждён воспроизводимый e2e-набор на базе интеграционных тестов:
    `UserAuthStudentFlowIntegrationTest`, `CourseLessonCrudIntegrationTest`,
    `SectionCatalogIntegrationTest`, `GroupManagementIntegrationTest`, `ProgramManagementIntegrationTest`.
  - Выполнен целевой прогон набора с итогом `BUILD SUCCESS` и `Tests run: 22, Failures: 0, Errors: 0`.
  - Зафиксирована трассировка покрытия AC для MVP и post-MVP; отдельно отмечено ограничение по AC-109 (поиск/фильтры) в текущем backend e2e-срезе.
  - Создан артефакт задачи: `memory-bank/task-artifacts/TASK-040.md`.
- Какие модули затронуты:
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/SectionCatalogIntegrationTest.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/GroupManagementIntegrationTest.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ProgramManagementIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-040.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
  - `memory-bank/02-active-context.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-ui задаче по приоритету и зависимостям из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена TASK-038 (security hardening логов и секретов)
- Что сделано (кратко):
  - Удалены fallback-секреты из runtime-конфига: `APP_SECURITY_JWT_SECRET` и `SPRING_DATASOURCE_PASSWORD` теперь берутся только из env.
  - В `BusinessEventLogger` добавлена централизованная маскировка чувствительных ключей (`email`, `to`, `password`, `token`, `secret`, `authorization`, `link`) c выводом `[REDACTED]`.
  - Убрано небезопасное логирование PII/ссылок из `NoopEmailService` и `YandexSmtpEmailService`.
  - Добавлен тест `BusinessEventLoggerTest` и обновлены интеграционные тесты на test-конфиг JWT secret.
  - Подтверждён прогон: `mvn -pl monolith-mvp -Dtest=BusinessEventLoggerTest,UserAuthStudentFlowIntegrationTest,ObservabilityIntegrationTest test` (`BUILD SUCCESS`).
- Какие модули затронуты:
  - `monolith-mvp/src/main/resources/application.yml`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/observability/BusinessEventLogger.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/YandexSmtpEmailService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/NoopEmailService.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/observability/BusinessEventLoggerTest.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ObservabilityIntegrationTest.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
  - `memory-bank/task-artifacts/TASK-038.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Перейти к следующей ready-to-start non-ui задаче из `memory-bank/tasks.json`.

### 2026-02-24 — Завершена OpenAPI-детализация backend API
- Что сделано (кратко):
  - Доведена подробная OpenAPI-документация контроллеров admin/student/auth-контуров с единым стилем аннотаций.
  - В текущем финальном проходе детализированы `ProgressController` и оставшиеся endpoint-ы в `CoursesController` (response-коды, error-контракты, content-типы, включая CSV).
  - Подтверждена компиляция после изменений: `mvn -f monolith-mvp/pom.xml -DskipTests compile` (`BUILD SUCCESS`).
  - Сформирован артефакт завершения: `memory-bank/task-artifacts/TASK-OPENAPI-DOCS-2026-02-24.md`.
- Какие модули затронуты:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/ProgressController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
  - `memory-bank/task-artifacts/TASK-OPENAPI-DOCS-2026-02-24.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
  - `memory-bank/02-active-context.md`
- Что отложено / следующий крупный этап:
  - При необходимости выполнить отдельный проход по quality-gates OpenAPI (генерация/публикация спецификации в CI).
