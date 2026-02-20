# Активный контекст

## Текущая задача
- `TASK-012` в финализации после реализации: FR-010 (двухсписочная модель enrollment `enrolled/notEnrolled`).

## Что реализовано
- Реализован API-контракт двух списков enrollment для курса:
  - добавлен DTO `CourseEnrollmentListsDto` с полями `enrolled` и `notEnrolled`;
  - добавлен endpoint `GET /api/v1/admin/courses/{courseId}/enrollments/lists` в `CoursesController`.
- Расширен сервисный слой `CourseService`:
  - `getEnrollmentLists(courseId)` — формирование двух списков на основе всех `STUDENT` и текущих `Enrollment`;
  - `enrollStudentsToCourse(courseId, ids)` и `unenrollStudentsFromCourse(courseId, ids)` — batch-операции для перемещения между списками.
- Усилена валидация входных идентификаторов:
  - проверка непустого payload;
  - проверка существования всех пользователей;
  - проверка роли `STUDENT` для enrollment-операций.
- Репозиторий пользователей расширен методом `findAllByRole(Role role)`.
- Интеграционное покрытие расширено:
  - добавлен тест `enrollment_two_lists_flow_should_work` в `CourseLessonCrudIntegrationTest`;
  - сценарий покрывает: начальное состояние списков, зачисление, проверку learner-доступа к курсу, отчисление и подтверждение потери доступа.

## Валидация
- `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS` (`Tests run: 5, Failures: 0, Errors: 0`).

## Статус
- `TASK-012` переведена в `done` в `memory-bank/tasks.json`.
- Из-за превышения лимита контекста (>370k) требуется завершение итерации и продолжение через `/newtask` для финального отчёта и синхронизации `06-system-development-progress.md`.
