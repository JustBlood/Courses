# TASK-041 (done) — FR-002/AC-002: явное activation-state пользователя

## Что сделано
- Реализовано явное состояние активации пользователя до установки пароля и перевод в активное состояние после успешного `set-password`.
- Сохранён текущий onboarding-flow (invite -> set-password -> login) без деградации существующих сценариев.

### Изменения в коде
1. `monolith-mvp/src/main/java/ru/just/monolithmvp/model/AppUser.java`
   - добавлено поле `activated`:
     - `@Column(nullable = false)`
     - `private boolean activated = true;`

2. `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/user/UserDto.java`
   - добавлен флаг `boolean activated` в DTO record.

3. `monolith-mvp/src/main/java/ru/just/monolithmvp/service/UserService.java`
   - в `createUser(...)`:
     - `user.setActivated(!sendInvite);` (invite flow = неактивирован, ручной пароль = активирован)
   - в `setUserPassword(...)`:
     - `user.setActivated(true);`

4. `monolith-mvp/src/main/java/ru/just/monolithmvp/service/AuthService.java`
   - в `setPassword(...)`:
     - `setupToken.getUser().setActivated(true);`

5. `monolith-mvp/src/main/java/ru/just/monolithmvp/security/AppUserDetailsService.java`
   - логин разрешён только при:
     - `user.isEnabled() && user.isActivated()`

6. `monolith-mvp/src/main/java/ru/just/monolithmvp/init/BootstrapAdminInitializer.java`
   - bootstrap-admin создаётся с `admin.setActivated(true);`

7. `monolith-mvp/src/main/resources/db/migration/V5__users_activation_state.sql`
   - добавлена миграция:
     - `alter table users add column if not exists activated boolean not null default true;`
     - `update users set activated = true where activated is null;`

8. `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
   - добавлены проверки:
     - при создании invite-user `activated=false`;
     - до `set-password` login => `401 Unauthorized`;
     - после `set-password` через `/api/v1/admin/users/{id}` видно `activated=true`.

## Проверки
- Выполнена команда:
  - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test`
- Результат:
  - `BUILD SUCCESS`
  - `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`

## Статус задачи
- `TASK-041` переведена в `done` в `memory-bank/tasks.json`.

## Копия активного контекста на момент завершения

```md
# Активный контекст

## Текущая задача
- **ID:** TASK-041
- **Категория:** functional
- **Приоритет:** high
- **Источник требований:**
  - `memory-bank/tasks.json` (описание, acceptance criteria, test_steps)
  - PRD: `FR-002` из `memory-bank/prd/02-functional-requirements.md`
  - AC: `AC-002` из `memory-bank/prd/06-acceptance-criteria.md`

## Цель
Реализовать явное состояние активации пользователя до установки пароля и перевод в активное состояние после успешного `set-password`, сохранив текущий onboarding-flow.

## Рамки
### В scope
- Доменная модель пользователя (`AppUser`) и persistence-слой.
- Логика создания пользователя в onboarding-flow (`UserService.createUser`).
- Логика установки пароля (`AuthService.setPassword`).
- Проверка допуска к логину через `AppUserDetailsService`.
- DTO/контракт чтения пользователя (`UserDto`) для отражения activation-state.
- Интеграционный тест на сценарий до/после активации.

### Вне scope
- Изменение reset security (TTL/neutral responses) — это отдельная `TASK-022B`.
- UI/фронтовые изменения.

## План реализации
1. Добавить явный признак активации в модель пользователя и БД (миграция Flyway).
2. Обновить логику создания пользователя:
   - пользователь с инвайтом (без пароля) создаётся как неактивированный;
   - пользователь с заданным паролем создаётся активированным.
3. Обновить `setPassword`: после успешной установки пароля помечать пользователя как активированного.
4. Обновить security user loading: логин разрешён только если `enabled=true` и пользователь активирован.
5. Обновить `UserDto` для консистентного отражения activation-state в API.
6. Дополнить интеграционный тест:
   - до `set-password`: неактивирован, login отклоняется;
   - после `set-password`: активирован, login успешен.

## Критерии готовности
- Выполнены acceptance criteria TASK-041:
  1) явный неактивированный статус до `set-password`;
  2) перевод в активный после `set-password`;
  3) консистентность в домене/API без деградации onboarding-flow.
- Выполнены test_steps задачи из `tasks.json`.

## Риски
- Риск влияния на существующие аккаунты: закрывается миграцией с безопасным default для текущих записей.
- Риск регрессии auth-flow: закрывается интеграционным тестом end-to-end.

## Чекпоинт из-за Token Budget Gate

Context Window Usage превысил лимит 350000 токенов. Реализация приостановлена согласно `.clinerules/10-memory-bank-workflow.md`.

### Что уже сделано по TASK-041
1. **Выбрана задача:** `TASK-041` (pending, high, dependency `TASK-007` done).
2. **Обновлён активный контекст** с рамками, критериями и планом.
3. **Изменения в коде:**
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/AppUser.java`
     - добавлено поле `activated`:
       - `@Column(nullable = false)`
       - `private boolean activated = true;`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/user/UserDto.java`
     - добавлен флаг `boolean activated` в DTO record.
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/UserService.java`
     - в `createUser(...)`:
       - `user.setActivated(!sendInvite);` (invite flow = неактивирован, ручной пароль = активирован)
     - в `setUserPassword(...)`:
       - `user.setActivated(true);`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/AuthService.java`
     - в `setPassword(...)`:
       - `setupToken.getUser().setActivated(true);`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/security/AppUserDetailsService.java`
     - логин разрешён только при:
       - `user.isEnabled() && user.isActivated()`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/init/BootstrapAdminInitializer.java`
     - bootstrap-admin создаётся с `admin.setActivated(true);`
   - `monolith-mvp/src/main/resources/db/migration/V5__users_activation_state.sql`
     - добавлена миграция:
       - `alter table users add column if not exists activated boolean not null default true;`
       - `update users set activated = true where activated is null;`
   - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
     - добавлены проверки:
       - при создании invite-user `activated=false`
       - до `set-password` login => `401 Unauthorized`
       - после `set-password` через `/api/v1/admin/users/{id}` видно `activated=true`

### Что уже проверено
- Запущен тест:
  - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test`
- По логу видно:
  - компиляция прошла;
  - Flyway применил миграции до `V5__users_activation_state` успешно;
  - тестовый контекст стартовал.
- Полный финал прогона зафиксирован:
  - `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`

### Что осталось сделать после /newtask
1. Обновить `memory-bank/tasks.json`: `TASK-041` -> `done`.
2. Обновить `memory-bank/06-system-development-progress.md` summary.
3. Создать артефакт `memory-bank/task-artifacts/TASK-041.md` (перенести весь контекст задачи).
4. Добавить ссылку в `memory-bank/05-task-execution-progress.md`.
5. Очистить `memory-bank/02-active-context.md` (по правилам после полного завершения задачи).

### Технические заметки для продолжения
- Изменение `UserDto` добавляет поле `activated` в JSON-контракты admin endpoints пользователей.
- MapStruct-маппер `UserMapper` должен автоматически маппить одноимённый флаг из `AppUser`.
- Логика блокировки логина для неактивированных пользователей реализована через `isEnabled()` в `UserDetails`.
```