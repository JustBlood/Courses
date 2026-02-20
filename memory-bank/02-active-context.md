# Активный контекст

## Текущая задача
- `TASK-007` завершена: FR-002 email-onboarding (генерация токена установки пароля, отправка ссылки, вход после set-password).

## Что проверено
- По коду контур уже реализован:
  - `UserService.createUser` вызывает `sendPasswordLink` при создании пользователя без пароля;
  - `UserService.sendPasswordLink` создаёт `PasswordSetupToken` и отправляет ссылку через `EmailService`;
  - `AuthController/AuthService` поддерживают `set-password` и `recover-password` flow.
- Интеграционный сценарий покрытия найден и содержит e2e-путь onboarding:
  - `UserAuthStudentFlowIntegrationTest` проверяет create user -> set-password by token -> login.
- Валидация выполнена:
  - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test` -> `BUILD SUCCESS`.

## Статус
- Контекст по `TASK-006` очищен из активного файла.
- `TASK-007` подтверждена как реализованная и провалидированная тестами.
- Следующая ready-to-start non-UI задача backlog: `TASK-008`.
