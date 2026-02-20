# Активный контекст

## Текущая задача
- `TASK-016` (FR-014 workflow развёрнутого ответа `pending -> rework -> accepted`) — реализация и валидация завершены.

## Что сделано в текущей итерации
- В `LearningService.reviewOpenSubmission` реализованы целевые переходы статусов:
  - `toNextReview=true` -> `SubmissionStatus.REWORK`;
  - финальное принятие (`passed=true`, `toNextReview=false`) -> `SubmissionStatus.ACCEPTED`.
- Разрешён повторный review для submission в статусе `REWORK` (финализированные статусы повторно не ревьюятся).
- Обновлён reviewer-pending список (`getPendingReviews`) — теперь включает `PENDING_REVIEW` и `REWORK`.
- Обновлён интеграционный тест `CourseLessonCrudIntegrationTest`:
  - промежуточный статус после first review изменён на `REWORK`.

## Валидация
- Выполнен прогон:
  `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test`
- Результат: `BUILD SUCCESS`, `Tests run: 6, Failures: 0, Errors: 0`.

## Что осталось
- Задача `TASK-016` закрыта, следующий шаг — переход к следующей ready-to-start задаче backlog.
