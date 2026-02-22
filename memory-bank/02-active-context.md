# Активный контекст

## Текущая задача
- `TASK-017` (FR-015: личная статистика студента/пользователя с едиными формулами эффективности и % выполнения).

## Что сделано в текущей итерации
- Выбран и подтверждён следующий ready-to-start non-UI task: `TASK-017`.
- Прочитаны релевантные PRD-главы: `PRD-01`, `PRD-02 (FR-015)`, `PRD-06 (AC-015)`.
- Реализованы изменения backend:
  - в `StudentCourseStatDto` добавлено поле `progressPercent`;
  - в `StatisticsService`:
    - `myCourseStats()` переведён на переиспользуемый метод `userCourseStats(userId)`;
    - добавлен метод `userCourseStats(Long userId)` для admin-view статистики конкретного пользователя;
    - добавлен расчёт `progressPercent = completedLessons * 100 / totalLessons`;
  - в `UsersController` добавлен endpoint `GET /api/v1/admin/users/{userId}/stats`.
- Обновлены интеграционные тесты `CourseLessonCrudIntegrationTest`:
  - добавлены проверки `efficiencyPercent` и `progressPercent`;
  - добавлен сценарий получения статистики того же пользователя через admin endpoint `/api/v1/admin/users/{userId}/stats`.

## Валидация
- Выполнен прогон:
  `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test`
- Результат: `BUILD SUCCESS`, `Tests run: 6, Failures: 0, Errors: 0`.

## Что осталось
- `TASK-017` завершена: backlog обновлён (`status=done`), валидация пройдена.
- Следующий кандидат из ready-to-start non-UI задач по зависимостям: `TASK-018`.
