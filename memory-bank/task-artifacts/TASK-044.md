## TASK-044 (done) — Workflow open-ended review `PENDING_REVIEW -> REWORK -> ACCEPTED` + history/audit consistency

### Цель
Довести до полного соответствия `FR-014/AC-014` review-flow для open-ended submissions и устранить регрессию удаления урока после добавления истории статусов.

### Что сделано
1. Зафиксирован корректный сценарий review в интеграционном тесте:
   - первый review шаг: `passed=false`, `toNextReview=true` (`PENDING_REVIEW -> REWORK`);
   - второй review шаг: `passed=true` (`REWORK -> ACCEPTED`).
2. Добавлена миграция:
   - `V7__lesson_submission_status_history_on_delete_cascade.sql`;
   - FK `lesson_submission_status_history(submission_id) -> lesson_submissions(id)` переведён на `ON DELETE CASCADE`.
3. Устранена причина `500 DataIntegrityViolationException` при удалении урока/сабмишенов с историей статусов.

### Проверка
- Выполнен релевантный прогон:
  - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test`
- Результат:
  - `BUILD SUCCESS`
  - `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`.

### Изменённые файлы
- `monolith-mvp/src/main/resources/db/migration/V7__lesson_submission_status_history_on_delete_cascade.sql`
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`

### Итог
Задача закрыта: workflow review соответствует требованию `pending -> rework -> accepted`, история статусов сохраняется, удаление уроков/сабмишенов не ломается по FK.

### Полная копия активного контекста на момент завершения
# Активный контекст

## Token Budget Gate
- Контекст превышает порог Token Budget Gate (текущий usage > 350k), итерация должна быть остановлена.
- Требуется продолжение через `/newtask`.

## Текущая задача
- TASK-044 (functional, non-ui), статус in progress.
- Цель: зафиксировать workflow review open-answer как `PENDING_REVIEW -> REWORK -> ACCEPTED` и вести историю переходов.

## Что уже реализовано
1. Добавлена сущность истории:
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/LessonSubmissionStatusHistory.java`
2. Добавлен репозиторий:
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/LessonSubmissionStatusHistoryRepository.java`
3. Добавлена миграция:
   - `monolith-mvp/src/main/resources/db/migration/V6__lesson_submission_status_history.sql`
4. Обновлён сервис:
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
   - Логика:
     - `toNextReview=true` только из `PENDING_REVIEW`
     - `passed=true` только из `REWORK`
     - при submit open-answer пишется история `null -> PENDING_REVIEW`
     - при review пишется история `from -> to`
5. Частично обновлён тест:
   - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
   - В кейсе `enrollment_submission_and_review_corner_cases_should_work` добавлен новый сценарий и проверка истории `hasSize(3)`.

## Дополнительные изменения после предыдущей фиксации
1. Исправлен интеграционный тест `admin_and_student_course_lesson_crud_flow_should_work`:
   - в первом review-шаге changed payload: `passed=false`, `toNextReview=true` (вместо `passed=true`), чтобы соответствовать новому workflow.
2. Обнаружена новая проблема при повторном прогоне:
   - при удалении урока возник `DataIntegrityViolationException` из-за FK на `lesson_submission_status_history`.
   - root cause: FK `lesson_submission_status_history -> lesson_submissions` без `ON DELETE CASCADE`.
3. Добавлена миграция:
   - `monolith-mvp/src/main/resources/db/migration/V7__lesson_submission_status_history_on_delete_cascade.sql`
   - миграция дропает старый FK и создаёт FK с `on delete cascade`.

## Результаты прогонов теста на текущий момент
1) Прогон после первого изменения теста:
   - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test`
   - падение с `500` в `admin_and_student_course_lesson_crud_flow_should_work`
   - причина: FK violation при удалении `lesson_submissions`.

2) Повторный прогон после добавления V7:
   - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test`
   - `BUILD SUCCESS`
   - `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`
   - подтверждено применение `Flyway v7`.

## Что сделать первым шагом после /newtask
1. Закрыть документационную часть задачи:
   - создать/обновить `memory-bank/task-artifacts/TASK-044.md`;
   - добавить запись в `memory-bank/05-task-execution-progress.en.md`;
   - добавить запись в `memory-bank/06-system-development-progress.md` (архитектурно значимое изменение: audit history + FK cascade).
2. Обновить статус TASK-044:
   - `python memory-bank/change_task_status.py TASK-044 done`
3. После полного завершения — очистить `memory-bank/02-active-context.md` по правилам.
