# TASK-033 (done) — FR-110/FR-111: threshold + attemptLimit

## Что сделано
- Закрыт сценарий `FR-110/FR-111` для практического урока в runtime: учтён порог прохождения (`passingThresholdPercent`) и добавлено ограничение количества попыток (`attemptLimit`).
- Сохранён текущий API-контракт `POST /api/v1/student/lessons/{lessonId}/submit-practice` без изменения endpoint/DTO.
- Добавлена server-side блокировка новой попытки после достижения лимита попыток.
- Добавлен интеграционный тест на сценарий из задачи: две неуспешные попытки + блокировка третьей.

### Изменения в коде
1. `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/LessonSubmissionRepository.java`
   - добавлен метод:
     - `long countByStudentIdAndLessonId(Long studentId, Long lessonId)`

2. `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
   - в `submitPractice(...)` добавлена проверка лимита попыток **до** создания новой submission;
   - добавлен helper `validatePracticeAttemptLimit(...)`;
   - при исчерпании лимита выбрасывается:
     - `BadRequestException("Attempt limit exceeded for this lesson")`.

3. `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
   - добавлен интеграционный тест:
     - `practice_attempt_limit_should_block_third_attempt_after_two_failed()`
   - покрытие теста:
     - урок с `attemptLimit=2` и `passingThresholdPercent=100`;
     - 2 неуспешные попытки (`INCOMPLETE`);
     - 3-я попытка отклоняется `400 Bad Request`.

## Проверки
- Выполнена команда:
  - `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest test`
- Результат:
  - `BUILD SUCCESS`
  - `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`

## Статус задачи
- `TASK-033` переведена в `done`.

## Копия активного контекста на момент завершения

```md
# Активный контекст

## 2026-02-24 — TASK-033 (FR-110/FR-111: threshold + attemptLimit)

### Статус
- Текущая активная задача: `TASK-033`.
- Стадия: анализ завершён, начинается реализация.

### Источники требований
- `memory-bank/tasks.json` → `TASK-033`.
- `memory-bank/prd/02-functional-requirements.md` → `FR-110`, `FR-111`.
- `memory-bank/prd/06-acceptance-criteria.md` → `AC-110`, `AC-111`.

### Подтверждённый scope
1. Сохранить текущий API `POST /api/v1/student/lessons/{lessonId}/submit-practice`.
2. Оставить действующую логику threshold (`passingThresholdPercent`) и убедиться тестом, что она применяется.
3. Добавить runtime-ограничение количества попыток (`attemptLimit`) для practice-уроков:
   - если лимит не задан (`null`) — без ограничений;
   - если лимит задан — после достижения лимита новая попытка блокируется.
4. Добавить/обновить интеграционный тест под сценарий задачи (2 неуспешные попытки + блокировка третьей).

### План реализации
1. Доработать `LessonSubmissionRepository` методом подсчёта попыток по `(studentId, lessonId)`.
2. В `LearningService.submitPractice(...)` добавить проверку `attemptLimit` до создания новой submission.
3. Добавить интеграционный тест в `CourseLessonCrudIntegrationTest`.
4. Прогнать релевантный тестовый набор Maven.
5. После успешных проверок выполнить артефактизацию memory-bank и закрыть задачу.

### Риски и контроль
- Риск регрессии в workflow open-answer/review: минимизируется тем, что лимит считается по всем существующим submission одной пары (student, lesson), не меняя существующие статусы.
- Риск изменения текущей семантики `markEnrollmentStarted`: проверка лимита будет выполняться до сохранения blocked-попытки.

## Token Budget Gate checkpoint (обязательная фиксация)
- На текущем шаге: **Context Window Usage = 356,965 / 400K (> 350000)**.
- По правилу `.clinerules/10-memory-bank-workflow.md` реализация приостановлена до запуска новой задачи (`/newtask`).

## Что уже выполнено по TASK-033
1. Выбрана задача `TASK-033` (pending, non-ui, high, зависимости `TASK-011` и `TASK-015` закрыты).
2. Прочитаны релевантные документы:
   - `memory-bank/00-project-brief.en.md`
   - `memory-bank/02-active-context.md`
   - `memory-bank/tasks.json`
   - `memory-bank/01-prd-index.en.md`
   - `memory-bank/prd/02-functional-requirements.md` (FR-110/FR-111)
   - `memory-bank/prd/06-acceptance-criteria.md` (AC-110/AC-111)
   - `memory-bank/05-task-execution-progress.en.md`
3. Выполнен анализ кода и затронутых файлов:
   - `LearningService`, `CourseService`, `Lesson`, `PracticeLesson`, `LessonSubmissionRepository`, `CourseLessonCrudIntegrationTest`, `StudentController`, `LessonMapper`, `V1__init_schema.sql`.
4. Проведён дополнительный ресёрч через субагента (`backend-architect`) с рекомендацией минимального дизайна: считать попытки по `(studentId, lessonId)` и блокировать новые при `attempts >= attemptLimit`.
5. Внесены изменения в код:
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/LessonSubmissionRepository.java`
     - добавлен метод `countByStudentIdAndLessonId(Long studentId, Long lessonId)`.
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
     - добавлена проверка лимита попыток в `submitPractice(...)` до создания новой submission;
     - добавлен helper `validatePracticeAttemptLimit(...)`;
     - при исчерпании лимита выбрасывается `BadRequestException("Attempt limit exceeded for this lesson")`.
   - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
     - добавлен интеграционный тест `practice_attempt_limit_should_block_third_attempt_after_two_failed()`:
       - practice-урок с `attemptLimit=2` и `passingThresholdPercent=100`;
       - две неуспешные попытки дают `INCOMPLETE`;
       - третья попытка возвращает `400 Bad Request`.
6. Выполнен прогон тестов:
   - `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest test`
   - Результат: `BUILD SUCCESS`, `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`.

## Что осталось сделать после /newtask
1. Финализировать memory-bank артефакты по задаче:
   - создать `memory-bank/task-artifacts/TASK-033.md` (с итогами и копией активного контекста);
   - добавить запись о `TASK-033` в `memory-bank/05-task-execution-progress.en.md`.
2. Обновить `memory-bank/06-system-development-progress.md` краткой записью по TASK-033.
3. Перевести задачу в done:
   - `python memory-bank/change_task_status.py TASK-033 done`.
4. После полной артефактизации очистить `memory-bank/02-active-context.md`.
```
