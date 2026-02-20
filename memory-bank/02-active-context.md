# Активный контекст

## Текущая задача
- `TASK-008` завершена: FR-003/FR-004 (редактирование пользователя и смена роли с немедленным применением прав).

## Что проверено
- Реализован self-profile update для пользователя (FR-003):
  - добавлен DTO `UpdateMyProfileRequest` с whitelist-полями (`fullName`, `phone`, `comment`);
  - добавлен endpoint `PATCH /api/v1/student/my/profile` в `StudentController`;
  - добавлен сервисный метод `UserService.updateMyProfile` с валидацией и запретом пустого обновления.
- Подтверждена корректная реакция на запрещённые поля в self-profile update:
  - неизвестные/запрещённые поля в payload приводят к `400 Bad Request`.
- Подтверждён FR-004 (немедленное применение роли):
  - после `PATCH /api/v1/admin/users/{id}/role` существующий JWT начинает работать с новыми правами без re-login.
- Интеграционные тесты обновлены и проходят:
  - `UserAuthStudentFlowIntegrationTest` расширен кейсами self-profile update и immediate role effect.
- Валидация выполнена:
  - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest,CourseLessonCrudIntegrationTest test` -> `BUILD SUCCESS`.

## Статус
- `TASK-008` подтверждена как реализованная и провалидированная тестами.
- Следующая ready-to-start non-UI задача backlog: `TASK-011`.
- Token-budget checkpoint: в текущей итерации после завершения реализации и обновления `tasks.json/02/05` достигнут лимит контекста >370k.
- Осталось закрыть в следующей итерации: добавить финальную запись по `TASK-008` в `memory-bank/06-system-development-progress.md` и отправить итоговый отчёт пользователю.
