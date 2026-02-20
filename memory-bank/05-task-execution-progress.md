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

## TASK-002 (done) — стабилизация Flyway migrations
- Что сделано:
  - Проверен контур миграций Flyway на чистой PostgreSQL в docker-compose окружении.
  - Подтверждено, что текущая миграция `V1__init_schema.sql` успешно применяется и фиксируется в `flyway_schema_history`.
  - Подтверждён идемпотентный повторный прогон Flyway после restart приложения (без изменений схемы и без падений).
- Финальная валидация test-steps:
  - `docker compose -f docker-compose.monolith.yml --env-file monolith.env.example up -d --build` — успешно (чистый контур БД).
  - `docker exec monolith-postgres psql -U postgres -d courses -c "select ... from flyway_schema_history ..."` — `V1__init_schema.sql`, `success=true`.
  - `docker compose ... restart monolith-mvp` + проверка логов — `Schema "public" is up to date. No migration necessary.`
  - Дополнительно: `curl -sS http://localhost:8099/actuator/health` → `{"status":"UP","groups":["liveness","readiness"]}`.
- Итоговый статус задачи:
  - `TASK-002` переведена в `done`.

## TASK-003 (done) — JWT hardening
- Что сделано:
  - Добавлены новые обработчики security-ошибок:
    - `monolith-mvp/src/main/java/ru/just/monolithmvp/security/ApiAuthenticationEntryPoint.java`
    - `monolith-mvp/src/main/java/ru/just/monolithmvp/security/ApiAccessDeniedHandler.java`
  - Обновлён `SecurityConfig`:
    - подключены `authenticationEntryPoint` и `accessDeniedHandler` для единообразного безопасного формата auth-ответов.
  - Обновлён `JwtAuthenticationFilter`:
    - на ошибке парсинга/валидации JWT выполняется `SecurityContextHolder.clearContext()`.
- Реализация (тесты):
  - Обновлён `UserAuthStudentFlowIntegrationTest`:
    - добавлены проверки valid/expired/tampered JWT;
    - добавлены helper-методы генерации expired токена и tampered токена;
    - скорректировано ожидание статуса в кейсе deactivated-user token (`401` вместо `403`) в новой security-модели.
- Финальная валидация test-steps:
  - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test` → `BUILD SUCCESS`.
  - По тест-сценарию подтверждено: валидный JWT даёт доступ, просроченный и подменённый JWT возвращают `401 Unauthorized`.
- Итоговый статус задачи:
  - `TASK-003` переведена в `done`.

## TASK-004 (done) — RBAC-ограничения API
- Что сделано:
  - Усилен RBAC для review-flow в `LearningService.reviewOpenSubmission`.
  - Для неназначенного reviewer отказ переведён на security-семантику `403 Forbidden` через `AccessDeniedException`.
  - Сохранён контроль назначения reviewer на уровне course-reviewer связей.
- Реализация (тесты):
  - Обновлён `CourseLessonCrudIntegrationTest`:
    - скорректировано ожидание статуса для запроса review от неназначенного reviewer (`403` вместо `400`).
  - Подтверждён сценарий: review endpoint доступен только назначенному reviewer.
- Финальная валидация test-steps:
  - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest,CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS`.
  - Результат: `Tests run: 4, Failures: 0, Errors: 0`.
- Итоговый статус задачи:
  - `TASK-004` переведена в `done`.

## Token-budget checkpoint (2026-02-20, post-TASK-003)
- Причина фиксации:
  - В ходе дополнительного запроса пользователя контекст превысил 350k токенов (сработал `Token Budget Gate` из `.clinerules/10-memory-bank-workflow.md`).
- Что успели сделать до остановки:
  - Прочитаны и повторно проанализированы все правила `.clinerules/*`.
  - Зафиксированы ключевые зоны пересечения/конфликтов:
    - конфликт по очистке `02-active-context.md` (`10-memory-bank-workflow` требует только по явному запросу; `20-task-flow-and-modes` формулирует как обязательное действие после полного завершения);
    - дубли требований по обновлению `02/05/06` и по лимиту контекста в двух файлах (`10` и `20`);
    - дубли pre-tool контроля между локальными правилами и системным требованием `task_progress`.
  - Выполнена очистка `memory-bank/02-active-context.md` по явному запросу пользователя.
- Что запланировано сделать в следующей итерации (/newtask):
  1. Выдать структурированный аудит правил: группировка, дубли, конфликты, точки рефакторинга.
  2. Предложить точечные правки в `.clinerules/10-memory-bank-workflow.md` и `.clinerules/20-task-flow-and-modes.md`, чтобы исключить повтор ошибки с очисткой `02-active-context.md`.
  3. При подтверждении пользователя — внести согласованные изменения в rule-файлы.

## Token-budget checkpoint (2026-02-20, TASK-004 in progress)
- Причина фиксации:
  - В ходе закрытия `TASK-004` контекст превысил лимит 350k токенов (сработал `Token Budget Gate`).
- Что уже выполнено:
  - По коду: в `LearningService.reviewOpenSubmission` отказ для неназначенного reviewer переведён в `AccessDeniedException` (RBAC semantics `403`).
  - По тестам: обновлён `CourseLessonCrudIntegrationTest` под ожидаемый `403` для неназначенного reviewer.
  - Валидация: `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest,CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS` (`Tests run: 4, Failures: 0, Errors: 0`).
  - Backlog: `memory-bank/tasks.json` обновлён (`TASK-004` переведена в `done`).
  - Контекст: `memory-bank/02-active-context.md` обновлён под текущее состояние `TASK-004`.
- Что осталось сделать после `/newtask`:
  1. Дописать финальную запись в `memory-bank/06-system-development-progress.md` по завершению `TASK-004`.
  2. Сформировать и отправить итоговый отчёт пользователю по задаче.

## Token-budget checkpoint (2026-02-20, TASK-006 in progress)
- Причина фиксации:
  - В ходе закрытия `TASK-006` контекст превысил лимит 350k токенов (сработал `Token Budget Gate`).
- Что уже выполнено:
  - В `CreateUserRequest` добавлены поля `groupIds` и `courseIds` для первичных назначений при создании пользователя.
  - В `UserService.createUser` реализованы первичные назначения в рамках транзакции:
    - валидация входных списков,
    - проверка существования групп/курсов,
    - защита от `null` и дублей,
    - создание `GroupMembership` и `Enrollment`,
    - ограничение на typed-группы (не более одной группы каждого типа).
  - Обновлён import-flow CSV под новый конструктор `CreateUserRequest`.
  - Обновлён интеграционный тест `UserAuthStudentFlowIntegrationTest`:
    - добавлен сценарий создания пользователя с `groupIds/courseIds`,
    - добавлены проверки сохранения membership/enrollment.
  - Валидация пройдена:
    - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest,CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS`.
  - Backlog: `memory-bank/tasks.json` обновлён, `TASK-006` переведена в `done`.
  - Контекст: `memory-bank/02-active-context.md` обновлён под `TASK-006`.
- Что осталось сделать после `/newtask`:
  1. Дописать финальную запись в `memory-bank/06-system-development-progress.md` по завершению `TASK-006`.
  2. Отправить пользователю финальный отчёт по выполненной задаче.

## TASK-006 (done) — создание пользователя с первичными назначениями
- Что сделано:
  - Расширен DTO-контракт `CreateUserRequest`: добавлены `groupIds` и `courseIds`.
  - В `UserService.createUser` реализованы первичные назначения при создании пользователя:
    - валидация входных списков и запрет `null`-идентификаторов,
    - проверка существования целевых групп/курсов,
    - защита от дублей,
    - создание `GroupMembership` и `Enrollment` в рамках одной транзакции,
    - enforcement ограничения по typed-группам (не более одной группы каждого типа на пользователя).
  - Обновлён import-flow CSV под новый конструктор `CreateUserRequest`.
  - Обновлён интеграционный тест `UserAuthStudentFlowIntegrationTest`:
    - добавлен сценарий создания пользователя с первичными `groupIds/courseIds`;
    - добавлены проверки сохранённых связей в БД (`GroupMembershipRepository`, `EnrollmentRepository`).
- Финальная валидация test-steps:
  - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest,CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS`.
  - Результат: `Tests run: 4, Failures: 0, Errors: 0`.
- Итоговый статус задачи:
  - `TASK-006` переведена в `done` в `memory-bank/tasks.json`.

## TASK-007 (done) — email-onboarding (FR-002)
- Что сделано:
  - Подтверждён рабочий onboarding-flow после создания пользователя без пароля:
    - генерируется одноразовый `PasswordSetupToken`;
    - формируется invite-ссылка `.../set-password?token=...`;
    - отправка выполняется через `EmailService` (`YandexSmtpEmailService`/`NoopEmailService` fallback).
  - Подтверждён set-password/login путь:
    - `AuthService.setPassword` принимает токен и устанавливает новый password hash;
    - повторное использование токена блокируется (`Token already used`);
    - после успешной установки пароля пользователь проходит `login`.
  - Подтверждён recover-password контур как часть FR-002 интеграции (генерация нового токена и установка нового пароля).
- Финальная валидация test-steps:
  - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test` → `BUILD SUCCESS`.
  - Результат: `Tests run: 1, Failures: 0, Errors: 0`.
- Итоговый статус задачи:
  - `TASK-007` переведена в `done` в `memory-bank/tasks.json`.

## TASK-008 (done) — редактирование пользователя и смена роли (FR-003/FR-004)
- Что сделано:
  - Реализован self-profile update endpoint `PATCH /api/v1/student/my/profile` с whitelist разрешённых полей (`fullName`, `phone`, `comment`).
  - Добавлен DTO `UpdateMyProfileRequest` и сервисный метод `UserService.updateMyProfile`:
    - запрет пустого обновления;
    - валидация `fullName` (не blank);
    - обновление только разрешённых полей профиля.
  - Подтверждён отказ на попытку изменения запрещённых полей в self-profile payload (контролируемый `400 Bad Request`).
  - Подтверждён немедленный эффект смены роли: после admin role patch существующий JWT получает новые права без повторного login.
  - Обновлён интеграционный тест `UserAuthStudentFlowIntegrationTest`:
    - добавлены кейсы self-profile update (разрешённые/запрещённые поля);
    - добавлена проверка immediate-role-effect на существующем токене.
- Финальная валидация test-steps:
  - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest,CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS`.
  - Результат: `Tests run: 4, Failures: 0, Errors: 0`.
- Итоговый статус задачи:
  - `TASK-008` переведена в `done` в `memory-bank/tasks.json`.

## Token-budget checkpoint (2026-02-20, TASK-008 post-implementation)
- Причина фиксации:
  - Контекст текущей сессии превысил 370k токенов (сработал `Token Budget Gate`).
- Что уже выполнено:
  - Реализация TASK-008 в коде завершена (`StudentController`, `UserService`, `UpdateMyProfileRequest`, `GlobalExceptionHandler`, `UserAuthStudentFlowIntegrationTest`).
  - Прогон тестов выполнен успешно:
    - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest,CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS`.
  - `memory-bank/tasks.json` обновлён: `TASK-008` переведена в `done`.
  - `memory-bank/02-active-context.md` и `memory-bank/05-task-execution-progress.md` обновлены по факту реализации.
- Что осталось сделать после `/newtask`:
  1. Дописать финальную запись в `memory-bank/06-system-development-progress.md` по завершению `TASK-008`.
  2. Отправить пользователю итоговый отчёт по задаче.

## TASK-011 (done) — FR-008/FR-009 practice-модель и scoring-контракт
- Что сделано:
  - В `CourseService.applyQuestionPool` изменена логика default points:
    - `question.fullPoints` по умолчанию наследуется от `practiceLesson.fullPoints`;
    - `question.partialPoints` по умолчанию наследуется от `practiceLesson.partialPoints`.
  - В `CourseService.validatePracticeRequest` исправлена валидация question positions:
    - проверка непрерывности позиций применяется только при режиме `allWithPosition`.
  - Расширен DTO-контракт `PracticeSubmissionRequest`:
    - добавлено поле `questionAnswers: Map<Integer, List<String>>`;
    - сохранён legacy fallback через `selectedAnswers` для обратной совместимости.
  - В `LearningService.submitPractice` реализован scoring по full question-pool:
    - агрегация баллов по всем тестовым вопросам урока;
    - partial scoring для `MULTIPLE_CHOICE` при выборе подмножества корректных ответов;
    - вычисление `passed` по `passingThresholdPercent` урока;
    - сериализация ответов в `answerRaw` в формате по индексам вопросов.
  - Добавлен интеграционный тест `practice_lesson_should_support_all_question_types_and_partial_scoring` в `CourseLessonCrudIntegrationTest`:
    - покрывает типы `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `MATCHING`, `ORDERING`;
    - проверяет кейс частично правильного multiple-choice и итоговые поля submission.
- Финальная валидация test-steps:
  - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS`.
  - Результат: `Tests run: 4, Failures: 0, Errors: 0`.
- Итоговый статус задачи:
  - `TASK-011` переведена в `done` в `memory-bank/tasks.json`.

## Token-budget checkpoint (2026-02-20, TASK-012 near-complete)
- Причина фиксации:
  - Контекст текущей сессии превысил лимит 370k токенов (сработал `Token Budget Gate`).
- Что уже выполнено по `TASK-012`:
  - Реализована модель двух списков enrollment (`enrolled` / `notEnrolled`) в API:
    - добавлен DTO `CourseEnrollmentListsDto`;
    - добавлен endpoint `GET /api/v1/admin/courses/{courseId}/enrollments/lists` в `CoursesController`;
    - в `CourseService` добавлены методы получения списков и batch-операций enrollment/unenrollment через `IdsRequest`.
  - Усилена валидация enrollment-операций:
    - проверка, что все ID существуют;
    - проверка, что все переданные пользователи имеют роль `STUDENT`;
    - защита от некорректных дублей ID на уровне валидации существования.
  - Репозиторный слой расширен выборкой студентов:
    - `AppUserRepository.findAllByRole(Role role)`.
  - Обновлён интеграционный тест `CourseLessonCrudIntegrationTest`:
    - добавлен тест `enrollment_two_lists_flow_should_work` (добавление в курс, доступ к курсу, отчисление, проверка списков до/после).
  - Валидация пройдена:
    - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS` (`Tests run: 5, Failures: 0, Errors: 0`).
  - Backlog обновлён:
    - `memory-bank/tasks.json`: `TASK-012` переведена в `done`.
- Что осталось сделать после `/newtask`:
  1. Обновить `memory-bank/02-active-context.md` финальным описанием `TASK-012`.
  2. Дописать запись в `memory-bank/06-system-development-progress.md` по завершению `TASK-012`.
  3. Отправить пользователю финальный отчёт по задаче.

## TASK-013 (done) — FR-011 назначение reviewer и рабочая область проверки
- Что сделано:
  - Расширен reviewer-workspace API:
    - добавлен endpoint `GET /api/v1/admin/progress/reviews/courses` в `ProgressController`;
    - endpoint возвращает только курсы, назначенные текущему reviewer.
  - Расширен сервисный слой `CourseService`:
    - добавлен метод `getReviewerCourseSummaries(adminId)`;
    - добавлен метод `getMyReviewerCourseSummaries()` для текущего пользователя;
    - выделен общий маппинг `toCourseSummaryDto(Course)`.
  - Подтверждён контур ограничений reviewer:
    - назначение reviewer только из пользователей с ролью `ADMIN`;
    - pending-open-submissions доступны reviewer только по назначенным курсам.
  - Обновлён интеграционный тест `CourseLessonCrudIntegrationTest`:
    - добавлены проверки `GET /api/v1/admin/progress/reviews/courses`;
    - добавлены проверки `GET /api/v1/admin/progress/reviews/pending`.
- Финальная валидация test-steps:
  - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS`.
  - Результат: `Tests run: 5, Failures: 0, Errors: 0`.
- Итоговый статус задачи:
  - `TASK-013` переведена в `done` в `memory-bank/tasks.json`.

## Token-budget checkpoint (2026-02-20, TASK-014 near-complete)
- Причина фиксации:
  - Контекст текущей сессии превысил лимит 370k токенов (сработал `Token Budget Gate` из `.clinerules/10-memory-bank-workflow.md`).
- Что уже выполнено по `TASK-014`:
  - Подтверждён выбор задачи `TASK-014` как следующей ready-to-start critical non-UI задачи.
  - Подтверждено соответствие текущей backend-логики `FR-012`:
    - прохождение THEORY только для зачисленного студента;
    - фиксация submission со статусом `COMPLETE`;
    - начисление баллов по `lesson.fullPoints`.
  - Добавлен интеграционный тест `theory_lesson_completion_should_update_progress_and_stats` в
    `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`.
  - Тест проверяет:
    - отказ доступа к уроку до enrollment;
    - успешное `complete-theory` после enrollment;
    - обновление `student/my/stats`;
    - обновление `admin/progress/courses/{courseId}/stats`.
  - Выполнена валидация:
    - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS` (`Tests run: 6, Failures: 0, Errors: 0`).
  - Обновлён `memory-bank/02-active-context.md` под состояние `TASK-014` in-progress.
- Что осталось сделать после `/newtask`:
  1. Обновить `memory-bank/tasks.json`: перевести `TASK-014` в `done`.
  2. Дописать финальный блок по `TASK-014` в `memory-bank/05-task-execution-progress.md`.
  3. Добавить запись о завершении `TASK-014` в `memory-bank/06-system-development-progress.md`.
  4. Отправить пользователю финальный отчёт по задаче.

## TASK-014 (done) — FR-012 прохождение THEORY-урока
- Что сделано:
  - Подтверждено соответствие backend-логики требованиям `FR-012 / AC-012`:
    - прохождение THEORY доступно только для зачисленного студента;
    - при завершении урока фиксируется submission со статусом `COMPLETE`;
    - баллы начисляются в размере `lesson.fullPoints`.
  - Добавлен интеграционный тест
    `theory_lesson_completion_should_update_progress_and_stats`
    в `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`.
  - Тест покрывает сценарий отказа до enrollment и успешного завершения после enrollment,
    а также проверяет обновление статистики:
    - `/api/v1/student/my/stats`;
    - `/api/v1/admin/progress/courses/{courseId}/stats`.
- Финальная валидация test-steps:
  - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS`.
  - Результат: `Tests run: 6, Failures: 0, Errors: 0`.
- Итоговый статус задачи:
  - `TASK-014` переведена в `done` в `memory-bank/tasks.json`.
