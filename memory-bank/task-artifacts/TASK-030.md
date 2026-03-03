# TASK-030 (done) — FR-103: создание/редактирование программ с упорядоченным списком курсов

## Что сделано
- Включены admin endpoint-ы программ в `CoursesController`:
  - `POST /api/v1/admin/courses/programs`
  - `GET /api/v1/admin/courses/programs`
  - `GET /api/v1/admin/courses/programs/{programId}`
  - `PUT /api/v1/admin/courses/programs/{programId}`
- Расширен `ProgramService`:
  - добавлен `updateProgram(Long programId, CreateLearningProgramRequest request)`;
  - добавлена валидация входного списка курсов (дубликаты/существование);
  - обновление состава программы реализовано через безопасную пересборку `program_courses` с сохранением порядка.
- Расширен `ProgramCourseRepository` методом `deleteByProgramId(Long programId)`.
- Добавлены Flyway-миграции:
  - `V2__align_learning_program_columns.sql` — выравнивание колонок `learning_programs` (`deadline_at`, `block_after_deadline`);
  - `V3__program_courses_default_block_after_deadline.sql` — default `false` для `program_courses.block_after_deadline`.
- Добавлен интеграционный тест:
  - `ProgramManagementIntegrationTest` с полным lifecycle: `create -> update(reorder) -> get/list` и проверкой порядка курсов.

## Проверки
- `mvn -f monolith-mvp/pom.xml -Dtest=ProgramManagementIntegrationTest test -q`
  - `BUILD SUCCESS` (exit code 0)
  - Flyway применил `V1`, `V2`, `V3`; тестовый сценарий выполнен успешно.

## Статус задачи
- `TASK-030` переведена в `done` через скрипт:
  - `python memory-bank/change_task_status.py TASK-030 done`

## Копия активного контекста на момент завершения

```md
# Активный контекст

## Текущая задача
- **ID:** TASK-030
- **Категория:** functional (не UI)
- **Приоритет:** high
- **Статус:** in progress

## Источники требований
- `memory-bank/tasks.json` → TASK-030
- `memory-bank/prd/02-functional-requirements.md` → FR-103
- `memory-bank/prd/06-acceptance-criteria.md` → AC-103
- Правила выполнения: `.clinerules/*`

## Цель задачи
Реализовать backend-поддержку создания и редактирования программ обучения с упорядоченным списком курсов так, чтобы:
1. порядок курсов явно задавался и сохранялся;
2. порядок корректно возвращался через API;
3. редактирование состава/порядка не ломало существующие назначения программ.

## Границы задачи
### Входит
- Admin API для программ: create/list/get/update.
- Логика `ProgramService` для обновления программы и порядка курсов.
- Интеграционный тест на сценарий TASK-030 (3+ курсов, смена порядка, проверка порядка в API).
- Необходимая коррекция схемы БД через Flyway для согласования с `LearningProgram`.

### Не входит
- TASK-031 (правила прохождения программы).
- TASK-032 (назначения программ пользователям/группам).
- UI-часть и student endpoints.

## Технические наблюдения
- В `ProgramService` уже есть `createProgram/getPrograms/getProgram`, но admin endpoints в `CoursesController` закомментированы.
- В `LearningProgram` есть поля `deadlineAt` и `blockAfterDeadline`, но в `V1__init_schema.sql` эти поля есть у `program_courses`, а не у `learning_programs`.
- Без миграции с добавлением колонок в `learning_programs` использование `LearningProgram` в runtime рискованно.

## Риски и зависимости
- Риск SQL-ошибок на чтении/записи `learning_programs` без выравнивания схемы.
- Риск нарушения уникальности `uk_program_order` при обновлении порядка (нужна безопасная пересборка списка `ProgramCourse`).
- Риск побочного влияния на назначения (`program_enrollments`) при изменении состава программы — минимизировать через обновление только `program_courses`.

## План реализации
1. Добавить Flyway-миграцию для `learning_programs.deadline_at` и `learning_programs.block_after_deadline`.
2. Реализовать `ProgramService.updateProgram(...)` с валидацией дубликатов/существования курсов и пересборкой ordered-списка курсов.
3. Раскомментировать/добавить admin endpoints программ в `CoursesController` (create/list/get/update).
4. Добавить интеграционный тест на lifecycle программы: create → update(order) → get/list.
5. Прогнать релевантный тестовый класс Maven.
6. Обновить memory-bank артефакты и статус задачи.

## Checkpoint (Token Budget Gate)
- **Дата/время:** 2026-02-24 13:25 (MSK)
- **Причина checkpoint:** Context Window Usage > 350000 (фактически ~363902)
- **Статус:** реализация приостановлена согласно `.clinerules/10-memory-bank-workflow.md`

### Что уже сделано в коде
1. `ProgramCourseRepository`
   - добавлен метод `deleteByProgramId(Long programId)`.

2. `ProgramService`
   - добавлена зависимость `ProgramCourseRepository`;
   - нормализована установка `blockAfterDeadline` через `Boolean.TRUE.equals(...)`;
   - вынесена общая валидация курсов в `validateAndLoadCourses(...)`;
   - добавлен `updateProgram(Long programId, CreateLearningProgramRequest request)`:
     - загрузка и валидация курсов,
     - обновление полей программы,
     - удаление старых `program_courses` через `deleteByProgramId`,
     - `flush()`, очистка коллекции, пересоздание ordered-связей,
     - сохранение программы.

3. `CoursesController`
   - подключены импорты DTO программ;
   - включены admin endpoints:
     - `POST /api/v1/admin/courses/programs`
     - `GET /api/v1/admin/courses/programs`
     - `GET /api/v1/admin/courses/programs/{programId}`
     - `PUT /api/v1/admin/courses/programs/{programId}`.

4. Flyway-миграции
   - `V2__align_learning_program_columns.sql`
     - добавляет `learning_programs.deadline_at`;
     - добавляет `learning_programs.block_after_deadline` (not null default false).
   - `V3__program_courses_default_block_after_deadline.sql`
     - выставляет default false для `program_courses.block_after_deadline`.

5. Интеграционный тест
   - создан `ProgramManagementIntegrationTest`:
     - логин admin,
     - создание 3 курсов,
     - создание программы с порядком A,B,C,
     - update с новым порядком C,A,B,
     - проверки порядка в get/list API.

### Результат последнего прогона теста
- Команда: `mvn -f monolith-mvp/pom.xml -Dtest=ProgramManagementIntegrationTest test`
- Статус: **FAIL**
- Ошибка: `DataIntegrityViolationException` при insert в `program_courses`:
  - `NULL not allowed for column "block_after_deadline"`.
- Контекст:
  - в `V1__init_schema.sql` у `program_courses.block_after_deadline` нет default;
  - JPA entity `ProgramCourse` не маппит это поле;
  - при вставке Hibernate не передает колонку, БД требует not null.

### Что нужно сделать следующим шагом (после /newtask)
1. Проверить, что `V3__program_courses_default_block_after_deadline.sql` реально применяется в чистой БД теста.
2. Перезапустить тест `ProgramManagementIntegrationTest`.
3. Если ошибка останется:
   - добавить маппинг поля в `ProgramCourse` (например `Boolean blockAfterDeadline` с default false) **или**
   - скорректировать DDL/миграции так, чтобы insert без колонки стабильно проходил.
4. Довести тест до зеленого.
5. Выполнить пост-задачные шаги memory-bank:
   - создать task artifact `memory-bank/task-artifacts/TASK-030.md`;
   - добавить запись в `memory-bank/05-task-execution-progress.en.md`;
   - при необходимости обновить `memory-bank/06-system-development-progress.md`;
   - обновить статус TASK-030 в `memory-bank/tasks.json` (через скрипт);
   - очистить `memory-bank/02-active-context.md` после полного завершения.
```
