# TASK-031 (done) — FR-104: правила прохождения программы (deadline + accessCondition)

## Что сделано
- Доработана логика расчёта доступности курсов программы в `ProgramService` по требованиям `FR-104 / AC-104`.
- Итоговая формула доступности в `toProgramDto(...)`:
  - `availableByAccessCondition` (режим доступа: `ALL_OPEN` / `PREVIOUS_COURSES_COMPLETED` / `PREVIOUS_COURSES_VIEWED_OR_PENDING`)
  - `availableByDeadline` (правило дедлайна)
  - `available = availableByAccessCondition && availableByDeadline`

### Изменения в коде
- Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/service/ProgramService.java`
  - удалён старый закомментированный блок проверки дедлайна;
  - добавлен метод `isAvailableByDeadline(LearningProgram program, boolean completed)`:
    - если `blockAfterDeadline != true` → доступно;
    - если `deadlineAt == null` → доступно;
    - если курс уже завершён (`completed == true`) → доступно;
    - иначе после дедлайна доступ блокируется.

### Интеграционные тесты
- Файл: `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ProgramManagementIntegrationTest.java`
- Добавлен тест `should_apply_program_access_rules_and_deadline_blocking_for_student()`.
- Проверено:
  1. В режиме `PREVIOUS_COURSES_COMPLETED` второй курс недоступен до завершения первого.
  2. После завершения первого курса второй становится доступен.
  3. При прошедшем дедлайне и `blockAfterDeadline=true` незавершённый курс блокируется.
  4. Завершённый курс остаётся доступным после дедлайна.

## Проверки
- Команда:
  - `mvn -f monolith-mvp/pom.xml -Dtest=ProgramManagementIntegrationTest test -q`
- Результат:
  - `exit code 0` (успешно)

## Статус задачи
- `TASK-031` переведена в `done`.

## Копия активного контекста на момент завершения

```md
# Активный контекст

## 2026-02-24 — TASK-031 (FR-104: правила прохождения программы)

### Статус
- Текущая активная задача: `TASK-031`.
- Стадия: реализация и тестирование завершены, выполняется финальная артефактизация в memory-bank.

### Источники требований
- `memory-bank/tasks.json` → `TASK-031`.
- `memory-bank/prd/02-functional-requirements.md` → `FR-104`.
- `memory-bank/prd/06-acceptance-criteria.md` → `AC-104`.
- `memory-bank/prd/01-user-scenarios.md` → `US-07`.

### Реализовано
1) Обновлена логика доступности курса программы в `ProgramService`:
- Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/service/ProgramService.java`
- В `toProgramDto(...)` доступность вычисляется как:
  - `availableByAccessCondition` по `accessCondition`;
  - `availableByDeadline` по `deadlineAt + blockAfterDeadline`;
  - итог: `available = availableByAccessCondition && availableByDeadline`.
- Добавлен метод `isAvailableByDeadline(LearningProgram program, boolean completed)`:
  - если `blockAfterDeadline != true` → доступно;
  - если `deadlineAt == null` → доступно;
  - если курс уже завершён (`completed == true`) → доступно;
  - иначе после дедлайна доступ блокируется.

2) Добавлен интеграционный тест правил прохождения программы:
- Файл: `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ProgramManagementIntegrationTest.java`
- Тест: `should_apply_program_access_rules_and_deadline_blocking_for_student()`.
- Проверки:
  - в последовательном режиме `PREVIOUS_COURSES_COMPLETED` второй курс недоступен до завершения первого;
  - после завершения первого курса второй становится доступен;
  - при прошедшем дедлайне и `blockAfterDeadline=true` незавершённый курс блокируется;
  - завершённый курс остаётся доступным после дедлайна.

3) Повторно подтверждён прогон тестов:
- Команда: `mvn -f monolith-mvp/pom.xml -Dtest=ProgramManagementIntegrationTest test -q`
- Результат: `exit code 0`.

### Осталось закрыть в этой сессии
1. Создать `memory-bank/task-artifacts/TASK-031.md` с итогом и копией активного контекста.
2. Обновить индекс `memory-bank/05-task-execution-progress.md`.
3. Добавить запись в `memory-bank/06-system-development-progress.md`.
4. Перевести `TASK-031` в `done` через `memory-bank/change_task_status.py`.
5. Очистить `memory-bank/02-active-context.md` после завершения артефактизации.
```