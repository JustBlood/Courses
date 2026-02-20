# Активный контекст

## Текущая задача
- `TASK-014` завершена: FR-012 (прохождение THEORY-урока с фиксацией статуса и начислением баллов).

## Что сделано в текущей итерации
- Проведён анализ требований `FR-012 / AC-012` и текущей реализации `completeTheoryLesson`.
- Подтверждено, что базовая backend-логика уже соответствует FR-012:
  - доступ к теории только при enrollment;
  - создание `LessonSubmission` со статусом `COMPLETE`;
  - начисление `pointsAwarded = lesson.fullPoints`.
- Добавлен интеграционный тест
  `theory_lesson_completion_should_update_progress_and_stats`
  в `CourseLessonCrudIntegrationTest`, который проверяет:
  - отказ до enrollment;
  - успешное завершение THEORY-урока после enrollment;
  - обновление статистики студента (`/api/v1/student/my/stats`);
  - обновление статистики курса (`/api/v1/admin/progress/courses/{courseId}/stats`).

## Валидация
- Выполнен прогон:  
  `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test`
- Результат: `BUILD SUCCESS`, `Tests run: 6, Failures: 0, Errors: 0`.

## Что осталось
- Выбрать следующую ready-to-start non-UI задачу из `memory-bank/tasks.json` по приоритету и зависимостям (кандидат: `TASK-015`).
- При старте следующей задачи обновить этот файл под новый in-progress контекст.

## Ограничение текущей итерации
- Context Window Usage превысил порог 350k; продолжение реализации переносится в следующую итерацию (`/newtask`) по Token Budget Gate.
