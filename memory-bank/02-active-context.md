# Активный контекст

## Текущая цель
Подготовить двухконтурный backlog для разработки с учётом уже существующего кода:
1) `pre-tasks.json` — задачи gap-analysis (сверка кодовой базы с PRD),
2) `tasks.json` — основной backlog реализации/доработок, который будет актуализирован по итогам pre-phase.

## Что сделано в текущей итерации
- Проведён ревью `tasks.json` через субагентов `system-architect` и `sprint-prioritizer` согласно `.clinerules/50-subagents-guidelines.md`.
- В `tasks.json` внесены правки структуры и зависимостей:
  - улучшены `agent_instructions.before_start` (выбор только ready-to-start задач, tie-break по минимальному id, фиксация блокеров);
  - убрана конфликтная формулировка про запрет редактирования backlog и очищен шумный текст в инструкции `before_finish`;
  - исправлены зависимости `TASK-028` (связка с программами вместо параметров практики);
  - усилены зависимости отчётов: `TASK-019`/`TASK-020` привязаны к группам/программам.
- Создан новый файл `pre-tasks.json` с 20 атомарными pre-задачами (`PRE-001..PRE-020`) для полного gap-analysis:
  - инвентаризация API/моделей/миграций/сервисов/security;
  - трассировка FR/AC/NFR;
  - подготовка change-set и финальная корректировка `tasks.json` в `PRE-020`.
- Проверена JSON-валидность обоих файлов (`tasks.json`, `pre-tasks.json`).
- Выполнена pre-задача `PRE-001` (baseline gap-analysis):
  - зафиксирован актуальный commit: `0ee4fbf02b639c21bff77482632cae3376a884ae`;
  - зафиксированы профили запуска monolith (`h2`, `pg`, `yandex-mail`) и активный профиль по умолчанию (`h2`);
  - зафиксированы доступные контуры окружений из compose-конфигов (`docker-compose-dev.yml`, `docker-compose.yml`, `docker-compose1.yml`);
  - зафиксированы источники требований PRD-00..07;
  - зафиксированы границы анализа: `monolith-mvp` + связанные конфиги, с исключениями по `.clineignore`.
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-001-baseline-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-001` переведена в статус `done`.
- Выполнена pre-задача `PRE-002` (инвентаризация API-контуров):
  - просканированы все активные REST-контроллеры (`AuthController`, `UsersController`, `CoursesController`, `StudentController`, `ProgressController`);
  - собрана endpoint-карта `endpoint -> controller -> request/response DTO` по доменам `auth/user/course/learning/reporting`;
  - сопоставлены контракты с OpenAPI-конфигурацией (`springdoc`, `OpenAPIConfig`) и PRD-02/PRD-06;
  - зафиксированы gap: отсутствие отдельного course-report endpoint по FR-017, отсутствие активных endpoint по FR-101..FR-109 (есть только закомментированные заготовки), ограниченная детализация OpenAPI-аннотаций.
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-002-api-inventory-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-002` переведена в статус `done`.
- Выполнена pre-задача `PRE-003` (инвентаризация доменных моделей):
  - зафиксирован полный состав model-слоя (`entity` + `enum`) в `monolith-mvp`;
  - собрана карта ключевых связей между сущностями (users/courses/lessons/enrollments/review/groups/programs/tokens);
  - выполнено сопоставление сущностей с `repository` и `mapper` слоями;
  - зафиксированы потенциальные расхождения с PRD по статусам submission, режимам доступа программ, части нейминга/состава полей курса и неравномерному mapper-покрытию.
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-003-domain-model-inventory-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-003` переведена в статус `done`.
- Выполнена pre-задача `PRE-004` (сверка миграций Flyway и схемы БД):
  - проанализирована цепочка миграций (`V1__init_schema.sql`) и Flyway-конфигурация (`enabled`, `locations`, `baseline-on-migrate`);
  - выполнено сопоставление таблиц/колонок с актуальными entity (`AppUser`, `Course`, `Lesson`, `PracticeQuestion`, `LearningProgram`, `ProgramCourse` и др.);
  - сформирована gap-таблица по миграциям и схеме (включая критичные расхождения по `learning_programs` и `program_courses`);
  - зафиксированы точки риска по constraint alignment и избыточным колонкам (`users.lang`, nullable-поля theory/question type).
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-004-flyway-schema-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-004` переведена в статус `done`.
- Выполнена pre-задача `PRE-005` (сверка auth/security-контура):
  - проанализированы `SecurityConfig`, `JwtAuthenticationFilter`, `JwtService`, `AuthService/AuthController`, `GlobalExceptionHandler`;
  - проверены RBAC-ограничения на уровне controller/service для контуров `ADMIN/STUDENT/reviewer`;
  - сопоставлены текущие механики с PRD/AC/NFR (включая FR-018/019, NFR-SEC-01..05);
  - зафиксирована security-gap матрица с критичностью и рекомендациями.
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-005-security-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-005` переведена в статус `done`.
- Выполнена pre-задача `PRE-006` (проверка слойности и сервисного слоя):
  - проверены цепочки `controller -> service -> repository` для модулей `auth/user/course/learning/report`;
  - подтверждено отсутствие прямой отдачи entity наружу через активные REST-контракты;
  - выявлены архитектурные отклонения: mapper-зависимость от service (`LessonMapper -> CourseService`), batch-orchestration в `CoursesController`, неиспользуемые service-инъекции в ряде controller;
  - подготовлен приоритизированный список задач архитектурной нормализации (High/Medium/Low).
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-006-layering-and-architecture-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-006` переведена в статус `done`.
- Выполнена pre-задача `PRE-009` (сверка FR-001..FR-004 с AC-001..AC-004):
  - проведена трассировка `FR-001..FR-004` и `AC-001..AC-004` по фактическим controller/service/security/test слоям (`UsersController`, `UserService`, `StudentController`, `SecurityConfig`, `UserAuthStudentFlowIntegrationTest`);
  - сформирована матрица соответствия со статусами реализации:
    - FR: `implemented=1`, `partial=3`, `missing=0`;
    - AC: `implemented=1`, `partial=3`, `missing=0`;
  - подтверждён интеграционный прогон релевантного теста: `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test` (`BUILD SUCCESS`);
  - зафиксированы ключевые gaps блока users/authz: отсутствие первичных назначений групп/курсов при создании пользователя, отсутствие явного activation-state до установки пароля, отсутствие self-profile update endpoint.
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-009-fr-001-fr-004-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-009` переведена в статус `done`.
- Выполнена pre-задача `PRE-010` (сверка FR-005..FR-009 с AC-005..AC-009):
  - проведена трассировка `FR-005..FR-009` и `AC-005..AC-009` по слоям `model/controller/service/dto/test` (`Course`, `Lesson*`, `PracticeQuestion`, `CoursesController`, `CourseService`, `LearningService`, `CourseLessonCrudIntegrationTest`);
  - сформирована матрица соответствия со статусами реализации:
    - FR: `implemented=3`, `partial=2`, `missing=0`;
    - AC: `implemented=3`, `partial=2`, `missing=0`;
  - подтверждён интеграционный прогон релевантного теста: `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` (`BUILD SUCCESS`);
  - зафиксированы ключевые gaps блока courses/lessons/scoring: упрощённая модель `submitPractice` (проверка фактически только первого вопроса), неполная агрегация баллов по question-level настройкам, частичные расхождения DTO-контрактов с фактически применяемой моделью.
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-010-fr-005-fr-009-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-010` переведена в статус `done`.
- Выполнена pre-задача `PRE-011` (сверка FR-010..FR-014 с AC-010..AC-014):
  - проведена трассировка `FR-010..FR-014` и `AC-010..AC-014` по слоям `controller/service/model/repository/test` (`CoursesController`, `StudentController`, `ProgressController`, `CourseService`, `LearningService`, `CourseLessonCrudIntegrationTest`);
  - сформирована матрица соответствия со статусами реализации:
    - FR: `implemented=1`, `partial=4`, `missing=0`;
    - AC: `implemented=1`, `partial=4`, `missing=0`;
  - подтверждён интеграционный прогон релевантного теста: `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` (`BUILD SUCCESS`);
  - зафиксированы ключевые gaps learning-path: отсутствие API-модели двух списков enrollment (FR-010), отсутствие явных статусов `rework/accepted` в workflow review (FR-014), упрощённая автопроверка practice (FR-013), отсутствие отдельного представления назначенных курсов reviewer (FR-011).
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-011-fr-010-fr-014-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-011` переведена в статус `done`.
- Выполнена pre-задача `PRE-007` (интеграции SMTP/YouTube/файлового хранения и деградации):
  - проанализирован email-контур (`EmailService`, `YandexSmtpEmailService`, `NoopEmailService`, `UserService/AuthService`) и `mail-properties`;
  - проверен контур ссылочного контента theory (`HTML_TEXT/VIDEO_URL/PDF_FILE`), поведение `CourseService/LearningService` и отсутствие backend-валидации внешних ссылок/файловых путей;
  - зафиксированы деградационные сценарии: fallback при отсутствии `JavaMailSender`, риски rollback при SMTP-сбоях, отсутствие retry/outbox и контролируемого повтора;
  - собрана матрица соответствия `FR-002/007/019`, `AC-002/007/019`, `NFR-REL-04/NFR-SCL-03/NFR-SEC-05` со статусами и приоритетными gap-рекомендациями.
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-007-integrations-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-007` переведена в статус `done`.
- Выполнена pre-задача `PRE-008`; контекст по ней сжат до ссылочного уровня (без детальной технической конкретики).
- Выполнена pre-задача `PRE-012` (сверка `FR-015..FR-017` / `AC-015..AC-017` по statistics/reporting).
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-012-fr-015-fr-017-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-012` переведена в статус `done`.
- Выполнена pre-задача `PRE-013` (сверка `FR-018..FR-019` / `AC-018..AC-019` / `NFR-SEC` по password/security flows):
  - проверены контуры `change-password`, `recover-password`, `set-password` на уровне `AuthController/AuthService/UserService` и security-цепочки (`SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `GlobalExceptionHandler`);
  - зафиксированы статусы соответствия: `FR-018=implemented`, `AC-018=implemented`, `FR-019=partial`, `AC-019=partial`;
  - проведена сверка `NFR-SEC-01..05`: выявлены gaps по TTL reset-токенов, anti-abuse/rate-limit, user-enumeration в recover-flow, утечке internal error message в 500, и секретам в `application.yml`;
  - подтверждён интеграционный прогон релевантного теста: `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test` (`BUILD SUCCESS`).
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-013-fr-018-fr-019-security-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-013` переведена в статус `done`.
- Выполнена pre-задача `PRE-014` (сверка `FR-101..FR-105` / `AC-101..AC-105` по sections/programs/program rules/assignments):
  - проверены `model/service/controller/repository/dto` контуры программ и разделов (`ProgramService`, `LearningProgram*`, `CoursesController`, `StudentController`, `V1__init_schema.sql`);
  - сформирована матрица соответствия со статусами реализации:
    - FR: `implemented=0`, `partial=3`, `missing=2`;
    - AC: `implemented=0`, `partial=3`, `missing=2`;
  - подтверждены ключевые gaps: отсутствие блока sections (`FR-101/102`), закомментированные program endpoint-ы, частичная реализация deadline-ограничений программы.
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-014-fr-101-fr-105-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-014` переведена в статус `done`.
- Выполнена pre-задача `PRE-015` (сверка `FR-106..FR-109` / `AC-106..AC-109` по groups/mass assignments/search/filter):
  - проверены `model/repository/service/controller/dto` контуры групп и membership (`LearningGroup`, `GroupMembership`, `GroupService`, `UsersController`, `CoursesController`, `StudentController`);
  - сформирована матрица соответствия со статусами реализации:
    - FR: `implemented=0`, `partial=4`, `missing=0`;
    - AC: `implemented=0`, `partial=4`, `missing=0`;
  - подтверждены ключевые gaps: закомментированные endpoint-ы групп и массовых назначений, отсутствие persisted-связи `group -> target` и авто-применения доступов при изменении состава группы, отсутствие server-side search/filter для каталогов пользователей и курсов.
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-015-fr-106-fr-109-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-015` переведена в статус `done`.
- Выполнена pre-задача `PRE-016` (сверка `FR-110..FR-114` / `AC-110..AC-114` по advanced practice constraints):
  - проведена трассировка параметров `passingThresholdPercent`, `attemptLimit`, `deadlineDays/timeLimitMinutes`, `randomQuestionCount/shuffleOnEveryAttempt`, `stopLesson + lessonsFreeOrder` по слоям `model/dto/mapper/service/controller/test`;
  - сформирована матрица соответствия со статусами реализации:
    - FR: `implemented=0`, `partial=4`, `missing=1`;
    - AC: `implemented=0`, `partial=4`, `missing=1`;
  - подтверждены ключевые gaps: отсутствие runtime-enforcement для threshold/attempt-limit/time-limit, отсутствие применения random/shuffle в learner-submit flow, отсутствие server-side gate-логики stop-lesson.
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-016-fr-110-fr-114-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-016` переведена в статус `done`.
- Выполнена pre-задача `PRE-017` (сверка NFR-контура observability/audit/backup-restore/reliability):
  - проанализированы runtime-конфиги и контур наблюдаемости (`application.yml`, `pom.xml`, logging-pattern по сервисам/exception-handler);
  - проведена проверка наличия correlation-id, метрик/actuator, audit-trace и backup/restore-процедур;
  - сформирована NFR-матрица соответствия: `implemented=0`, `partial=2`, `missing=3`;
  - зафиксированы ключевые gaps: отсутствие correlation-id, отсутствие централизованного audit-event слоя, отсутствие backup/restore регламента и подтвержденного RPO/RTO.
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-017-nfr-gap-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-017` переведена в статус `done`.
- Выполнена pre-задача `PRE-018` (консолидированная матрица FR/AC/NFR + risk/effort/phase):
  - консолидированы результаты `PRE-009..PRE-017` в единую матрицу соответствия по блокам `users/courses/learning/reporting/security/programs/groups/advanced-practice/NFR`;
  - зафиксирована агрегированная сводка покрытия требований:
    - FR: `implemented=6`, `partial=24`, `missing=3`;
    - AC: `implemented=6`, `partial=23`, `missing=4`;
    - NFR (проверенный периметр): `implemented=1`, `partial=4`, `missing=5`;
  - сформирован gap-реестр `GAP-018-01..19` с параметрами `severity/effort/phase (MVP/post-MVP)`;
  - выделены MVP critical-path блокеры: practice runtime/scoring, AC-017 reporting contracts, reset-flow security + secrets management, backup/restore readiness, lifecycle активации пользователя.
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-018-fr-ac-nfr-consolidated-matrix.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-018` переведена в статус `done`.
- Выполнена pre-задача `PRE-019` (подготовка change-set для синхронизации `tasks.json`):
  - на основании артефактов `PRE-008..PRE-018` сформирован пакет изменений backlog с типами правок `done/obsolete/split/merge/add`;
  - подготовлен dependency-план для устранения конфликтов MVP/post-MVP (в частности для блоков `AC-017`, reset-flow security hardening и advanced practice);
  - подготовлен финальный diff-план для применения в `PRE-020` (без правок `tasks.json` на шаге `PRE-019`, согласно правилам pre-phase).
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-019-backlog-change-set-plan.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-019` переведена в статус `done`.
- Выполнена pre-задача `PRE-020` (финальная синхронизация backlog):
  - применён change-set из `PRE-019` к `memory-bank/tasks.json`;
  - обновлены статусы задач (`done/obsolete`), выполнены split-операции (`TASK-019/020/022`) и добавлены новые gap-closure задачи (`TASK-041..TASK-046`);
  - обновлены зависимости (включая critical-path для `TASK-040`) и подтверждена валидность backlog (`tasks_count=49`, `dependency_errors=0`);
  - pre-phase полностью закрыт (`PRE-001..PRE-020 = done`).
- Создан артефакт: `memory-bank/pre-task-artifacts/PRE-020-backlog-baseline-update-report.md`.
- В `memory-bank/pre-tasks.json` задача `PRE-020` переведена в статус `done`.

## Источники требований
- `memory-bank/prd/00-overview-and-goals.md`
- `memory-bank/prd/01-user-scenarios.md`
- `memory-bank/prd/02-functional-requirements.md`
- `memory-bank/prd/03-non-functional-requirements.md`
- `memory-bank/prd/04-constraints-and-assumptions.md`
- `memory-bank/prd/05-technical-architecture.md`
- `memory-bank/prd/06-acceptance-criteria.md`
- `memory-bank/prd/07-development-and-risks-and-future.md`
- `.clinerules/50-subagents-guidelines.md`

## Что дальше
1. Pre-phase завершён; в качестве рабочего backlog используется обновлённый `memory-bank/tasks.json` (baseline после `PRE-020`).
2. Переход к реализации основной очереди задач по `agent_instructions.before_start` из `tasks.json`:
   - брать только `pending` + ready-to-start задачи;
   - при равном приоритете выбирать минимальный id.
3. Следующая ready-to-start critical-задача: `TASK-001`.

## Обновление по TASK-001 (runtime-конфигурации MVP)
- `TASK-001` завершена полностью (`status=done` в `memory-bank/tasks.json`).
- Финально подтверждено выполнение `test_steps`:
  1. Поднят docker-контур через `docker compose -f docker-compose.monolith.yml --env-file monolith.env.example up -d --build`.
  2. Проверен запуск приложения и подключение к PostgreSQL в логах (`pg,mail-noop`, Tomcat 8099, Hikari/Flyway OK).
  3. Проверен health-check: `curl http://localhost:8099/actuator/health` → `{"status":"UP"...}`.
- Внесено уточнение runtime-профиля для smoke-check:
  - `monolith.env.example`: дефолт `SPRING_PROFILES_ACTIVE=pg,mail-noop` (без зависимости от внешнего SMTP при локальном запуске).
  - `monolith-mvp/README-runtime.md`: обновлены инструкции по запуску и пояснение по `mail-noop`.
- Следующая ready-to-start critical-задача по backlog: `TASK-002`.
