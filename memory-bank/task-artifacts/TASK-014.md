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
