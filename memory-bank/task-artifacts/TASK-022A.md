# TASK-022A (done) — FR-019 (base flow): восстановление пароля по email-токену

## Что сделано
- Проведён анализ реализации reset-flow в `AuthController`, `AuthService`, `UserService`, `PasswordSetupTokenRepository`.
- Подтверждено соответствие базовому сценарию `TASK-022A`:
  - `POST /api/v1/auth/recover-password` инициирует восстановление по email;
  - создаётся одноразовый `PasswordSetupToken` и формируется ссылка;
  - `POST /api/v1/auth/set-password?token=...` подтверждает reset, сохраняет новый пароль в хеше и помечает токен использованным.
- Дополнительные security-усиления (TTL, neutral responses) намеренно не выполнялись, т.к. это scope `TASK-022B`.

## Проверки
- Выполнен релевантный интеграционный тест:
  - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test`
- Результат:
  - `BUILD SUCCESS`
  - `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`

## Копия активного контекста на момент завершения

```md
# Активный контекст

## Текущая задача
- **ID:** `TASK-022A`
- **Категория:** `integration` (non-ui)
- **Приоритет:** `high`
- **Зависимости:** `TASK-007` и `TASK-003` (done)

## Цель
Подтвердить и закрыть `FR-019` (base flow): восстановление пароля по email-токену (`initiate + confirm`) с валидацией по test steps задачи.

## Источники требований
- `memory-bank/tasks.json` → `TASK-022A`
- `memory-bank/prd/02-functional-requirements.md` → `FR-019`
- `memory-bank/prd/06-acceptance-criteria.md` → `AC-019`

## Границы
### Входит
- Проверка текущей реализации `AuthController` / `AuthService` / `UserService` / `PasswordSetupTokenRepository` на соответствие `TASK-022A`.
- Запуск релевантного интеграционного теста для сценария recover-password.
- Обновление memory-bank и статуса задачи.

### Не входит
- Усиления безопасности reset-flow (`TTL`, neutral responses, anti-enumeration) — это отдельная `TASK-022B`.

## Анализ текущей реализации
- `POST /api/v1/auth/recover-password` реализован в `AuthController`, делегирует в `AuthService.recoverPassword`.
- `AuthService.recoverPassword` находит пользователя по email и вызывает `UserService.sendPasswordLink`.
- `UserService.sendPasswordLink` генерирует `PasswordSetupToken`, сохраняет его и отправляет ссылку через `EmailService`.
- `POST /api/v1/auth/set-password?token=...` реализован в `AuthController`, делегирует в `AuthService.setPassword`.
- `AuthService.setPassword` валидирует токен, проверяет одноразовость (`usedAt`), хеширует новый пароль и помечает токен использованным.
- По результатам анализа и проверки субагентом: обязательных кодовых изменений для закрытия именно `TASK-022A` не требуется.

## План выполнения
1. Прогнать релевантный интеграционный тест `UserAuthStudentFlowIntegrationTest`.
2. При успешном прогоне перевести `TASK-022A` в `done`.
3. Зафиксировать артефакт выполнения и обновить индексы progress.
4. Очистить `memory-bank/02-active-context.md` по правилам после полного завершения.

## Риски
- Риск ложного падения теста из-за накопленного состояния in-memory БД в тестовом классе.
- Риск затрагивания scope `TASK-022B`; требуется строгое ограничение только базовым flow.
```