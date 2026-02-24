# TASK-027 (done) — FR-106/FR-107: группы пользователей и правила typed-membership

## Что сделано
- Добавлен DTO запроса обновления группы:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/group/UpdateGroupRequest.java`.
- Расширен `GroupService`:
  - добавлен метод `updateGroup(UUID groupId, UpdateGroupRequest request)`;
  - в `addUsersToGroup(...)` для typed-групп (`COMPANY`/`DEPARTMENT`/`POSITION`) изменено поведение: вместо auto-replace теперь reject-конфликт;
  - добавлены проверки конфликтов membership при смене типа группы.
- Расширен `UsersController`:
  - включены endpoint-ы модуля групп;
  - добавлен endpoint `PUT /api/v1/admin/groups/{groupId}` для редактирования группы.
- Добавлен интеграционный тест:
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/GroupManagementIntegrationTest.java`:
    - проверка single typed membership;
    - проверка reject при конфликтной смене типа группы.

## Проверки
- `mvn -pl monolith-mvp -Dtest=GroupManagementIntegrationTest test`
  - `BUILD SUCCESS`, `Tests run: 2, Failures: 0, Errors: 0`.
- `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test`
  - `BUILD SUCCESS`, `Tests run: 1, Failures: 0, Errors: 0`.

## Commit
- `f64ca97` — `TASK-027: enforce typed group membership constraints`

## Копия активного контекста на момент завершения

```md
# Активный контекст

## Текущая задача
- **ID:** TASK-027
- **Категория:** functional
- **Приоритет:** high
- **Статус:** in_progress (финализация)

## Требования задачи (FR-106/FR-107)
1. создание/редактирование групп типов `GENERAL`, `COMPANY`, `DEPARTMENT`, `POSITION`;
2. ограничение: у одного пользователя не более одной группы типов `COMPANY`/`DEPARTMENT`/`POSITION`;
3. управление членством из модуля групп.

## Что реализовано
1. Добавлен `UpdateGroupRequest`:
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/group/UpdateGroupRequest.java`.
2. Обновлён `GroupService`:
   - добавлен `updateGroup(UUID groupId, UpdateGroupRequest request)`;
   - в `addUsersToGroup(...)` для typed-групп поведение сменено на reject-конфликт (вместо replace);
   - добавлены проверки конфликтов membership при смене типа группы.
3. Обновлён `UsersController`:
   - включены endpoint-ы управления группами;
   - добавлен `PUT /api/v1/admin/groups/{groupId}`.
4. Добавлен интеграционный тест:
   - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/GroupManagementIntegrationTest.java`:
     - `enforce single typed membership`;
     - `reject conflict on group type update`.

## Выполненные проверки
1. `mvn -pl monolith-mvp -Dtest=GroupManagementIntegrationTest test`
   - **BUILD SUCCESS**, `Tests run: 2, Failures: 0, Errors: 0`.
2. `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test`
   - **BUILD SUCCESS**, `Tests run: 1, Failures: 0, Errors: 0`.

## Что осталось до полного закрытия
1. Сделать commit изменений TASK-027 (только целевые файлы задачи).
2. Обновить статус `TASK-027 -> done` в `memory-bank/tasks.json`.
3. Создать артефакт `memory-bank/task-artifacts/TASK-027.md` (копия активного контекста + итог).
4. Добавить ссылку на артефакт в `memory-bank/05-task-execution-progress.md`.
5. Добавить запись в `memory-bank/06-system-development-progress.md`.
6. Очистить `memory-bank/02-active-context.md` после полного завершения задачи.
```