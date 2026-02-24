# TASK-022B (done) — Security reset-flow hardening (FR-019 / NFR-SEC)

## Что сделано
- Усилен reset-flow в `AuthService`:
  - добавлена проверка срока жизни reset-токена (TTL) в `setPassword(...)`;
  - при истёкшем токене возвращается `400 Bad Request` (`Invalid token`), пароль не обновляется;
  - `recoverPassword(...)` переведён в neutral-response режим: для существующего и несуществующего email API отвечает одинаково (`200 OK`), без раскрытия факта существования пользователя.
- Добавлена конфигурация TTL:
  - новый properties-record `PasswordResetProperties` (`app.security.password-reset.token-ttl`),
  - дефолт `PT24H` на уровне properties и в `application.yml`.
- Обновлены интеграционные проверки `UserAuthStudentFlowIntegrationTest`:
  - для несуществующего email в recover-password теперь ожидается `200`;
  - добавлен кейс истёкшего reset-токена (искусственно сдвинут `createdAt`) с ожиданием `400`;
  - добавлен повторный recover и успешный set-password по свежему токену.

## Изменённые файлы
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/AuthService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/config/properties/PasswordResetProperties.java`
- `monolith-mvp/src/main/resources/application.yml`
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`

## Проверки
- Выполнен релевантный тест из `test_steps` задачи:
  - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test`
- Результат:
  - `BUILD SUCCESS`
  - `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`

## Критерии приёмки TASK-022B
- ✅ Reset-токен ограничен по времени жизни (TTL).
- ✅ Recover endpoint не раскрывает существование email (neutral response).

## Копия активного контекста на момент завершения

```md
# Активный контекст

## Текущая задача
- **ID:** TASK-022B
- **Категория:** security
- **Описание:** усилить reset-flow (FR-019/NFR-SEC): TTL reset-токена и neutral responses.

## Почему выбрана именно эта задача
- В `memory-bank/tasks.json` активный контекст был пустой, поэтому выбрана новая задача по правилам backlog.
- Среди `pending` и `category != ui`, с учётом зависимостей и приоритета/порядка ID, стартуем с **TASK-022B**.

## Проверенные требования и ограничения
- Прочитаны обязательные документы: `.clinerules/*`, `memory-bank/00-project-brief.md`, `memory-bank/01-prd-index.md`, `memory-bank/02-active-context.md`, `memory-bank/tasks.json`, `memory-bank/05-task-execution-progress.md`.
- Релевантный PRD: `memory-bank/prd/03-non-functional-requirements.md` (NFR-SEC-03, NFR-SEC-05).
- По `agent_instructions`: работать только в рамках выбранной задачи, выполнить релевантные test_steps, отметить done только после успешных проверок.

## Текущее состояние реализации (до правок)
- `AuthService.recoverPassword(email)` бросает `NotFoundException`, если email не найден (не нейтральный ответ).
- `AuthService.setPassword(token, ...)` проверяет только существование/usedAt, TTL нет.
- Токен хранится в `PasswordSetupToken` (`createdAt`, `usedAt`), генерация токена — `UserService.sendPasswordLink(...)`.
- Интеграционный тест `UserAuthStudentFlowIntegrationTest` сейчас ожидает `404` для восстановления пароля на несуществующий email.

## План реализации
1. Добавить конфиг TTL reset-токена через `@ConfigurationProperties`.
2. Ввести проверку TTL в `AuthService.setPassword(...)`.
3. Сделать `AuthService.recoverPassword(...)` нейтральным (всегда 200, без раскрытия существования email).
4. Обновить интеграционный тест: non-existing email -> 200, добавить кейс протухшего токена.
5. Запустить релевантный интеграционный тест и зафиксировать результат.

## Риски
- TTL применяется к токенам установки/восстановления пароля в единой таблице — важно не сломать существующий onboarding flow.
- Нельзя менять публичный контракт больше, чем требуется задачей (без лишнего рефакторинга/массовых изменений).
```