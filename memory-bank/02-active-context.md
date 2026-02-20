# Активный контекст

## Текущая задача
- `TASK-013` завершена: FR-011 (назначение reviewer и рабочая область проверки).

## Что реализовано
- Расширен reviewer-workspace API:
  - добавлен endpoint `GET /api/v1/admin/progress/reviews/courses` в `ProgressController`;
  - endpoint возвращает только курсы, назначенные текущему reviewer.
- Расширен сервисный слой `CourseService`:
  - добавлен метод `getReviewerCourseSummaries(adminId)`;
  - добавлен convenience-метод `getMyReviewerCourseSummaries()` для текущего пользователя;
  - выделен переиспользуемый маппинг `toCourseSummaryDto(Course)`.
- Существующий reviewer-flow подтверждён как соответствующий FR-011:
  - назначение reviewer доступно только для `ADMIN` (`assignReviewerToCourse` + валидация роли);
  - pending-open-submissions доступны только по назначенным reviewer курсам (`getPendingReviews`).
- Интеграционное покрытие расширено в `CourseLessonCrudIntegrationTest`:
  - проверка `GET /api/v1/admin/progress/reviews/courses` (видны только назначенные reviewer курсы);
  - проверка `GET /api/v1/admin/progress/reviews/pending` (видны pending-ответы по назначенным курсам).

## Валидация
- `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS` (`Tests run: 5, Failures: 0, Errors: 0`).

## Статус
- `TASK-013` переведена в `done` в `memory-bank/tasks.json`.
- Следующая ready-to-start critical non-UI задача по зависимостям: `TASK-014`.
