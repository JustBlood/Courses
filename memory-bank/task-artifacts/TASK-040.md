## TASK-040 (done) — Сквозной e2e-набор приемки AC-001..AC-019 и AC-101..AC-114

### Цель
Собрать и подтвердить воспроизводимый e2e-набор приемки по PRD-06 (critical-path MVP + ключевые post-MVP расширения) на базе текущего интеграционного тестового контура.

### Что использовано как e2e-набор
- `UserAuthStudentFlowIntegrationTest`
- `CourseLessonCrudIntegrationTest`
- `SectionCatalogIntegrationTest`
- `GroupManagementIntegrationTest`
- `ProgramManagementIntegrationTest`

### Фактический прогон
Команда:
`mvn -f monolith-mvp/pom.xml -Dtest=UserAuthStudentFlowIntegrationTest,CourseLessonCrudIntegrationTest,SectionCatalogIntegrationTest,GroupManagementIntegrationTest,ProgramManagementIntegrationTest test`

Результат:
- `CourseLessonCrudIntegrationTest`: Tests run 13, Failures 0, Errors 0
- `GroupManagementIntegrationTest`: Tests run 3, Failures 0, Errors 0
- `ProgramManagementIntegrationTest`: Tests run 3, Failures 0, Errors 0
- `SectionCatalogIntegrationTest`: Tests run 2, Failures 0, Errors 0
- `UserAuthStudentFlowIntegrationTest`: Tests run 1, Failures 0, Errors 0
- Total: `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

Источник лога:
`C:\Users\User\AppData\Local\Temp\cline\background-1771961022557-8ey85ei.log`

### Трассировка AC -> покрытие тестами

#### MVP (AC-001..AC-019)
- Покрыто:
  - AC-001..AC-004: `UserAuthStudentFlowIntegrationTest#full_user_auth_student_business_flow_should_work`
  - AC-005..AC-017: `CourseLessonCrudIntegrationTest` (набор сценариев CRUD, enrollment/reviewer, learning flow, statistics, reports)
  - AC-018..AC-019: `UserAuthStudentFlowIntegrationTest#full_user_auth_student_business_flow_should_work`

#### Post-MVP (AC-101..AC-114)
- Покрыто:
  - AC-101..AC-102: `SectionCatalogIntegrationTest`
  - AC-103..AC-105: `ProgramManagementIntegrationTest`
  - AC-106..AC-108: `GroupManagementIntegrationTest`
  - AC-110..AC-114: `CourseLessonCrudIntegrationTest`
- Частично/ограничения:
  - AC-109 (поиск/фильтры) отсутствует как backend e2e-кейс в текущем интеграционном наборе (в рамках текущего backlog UI-задачи по поиску отмечены `obsolete`).

### Вывод по задаче
- Критический e2e-путь (админ + студент) подтверждён зелёным прогоном.
- Ключевые post-MVP расширения (sections/programs/groups/advanced-practice) покрыты и проходят.
- Результаты прогона достаточны для приемки текущего backend-среза по `TASK-040`.

### Полная копия активного контекста на момент завершения
# Активный контекст

## Текущая задача
- **ID:** TASK-040
- **Категория:** integration (non-ui)
- **Статус в backlog:** pending
- **Причина выбора:** среди pending-задач с `category != ui` и закрытыми зависимостями имеет наивысший приоритет (`medium`) относительно TASK-038 (`low`).

## Контекст и рамки
- Цель: собрать и подтвердить сквозной e2e-набор приемки для `AC-001..AC-019` и `AC-101..AC-114` с фокусом на critical-path.
- Зависимости TASK-040 (`TASK-020A`, `TASK-022B`, `TASK-028`, `TASK-036`) — в статусе `done`.
- Реализация делается в рамках существующего интеграционного тестового контура `monolith-mvp/src/test/java/ru/just/monolithmvp/controller`.

## Релевантные PRD-главы
- `PRD-06: acceptance criteria` — основной источник AC для трассировки.

## Обследованные файлы
- `memory-bank/tasks.json`
- `memory-bank/05-task-execution-progress.en.md`
- `memory-bank/prd/06-acceptance-criteria.md`
- `memory-bank/pre-task-artifacts/PRE-008-test-coverage-gap-report.md`
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/SectionCatalogIntegrationTest.java`
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/GroupManagementIntegrationTest.java`
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ProgramManagementIntegrationTest.java`

## План выполнения
1. Сопоставить AC-матрицу с текущими интеграционными тестами и зафиксировать целевой e2e-набор для MVP и post-MVP.
2. Прогнать релевантные test-сценарии (MVP и post-MVP наборы) и собрать фактический pass/fail результат.
3. Подготовить артефакт TASK-040 с трассировкой AC -> test method и итогами прогона.
4. Обновить memory-bank: `05-task-execution-progress.en.md`, `06-system-development-progress.md`, статус задачи в `tasks.json` через helper-скрипт, очистить `02-active-context.md` после полного завершения.

## Риски
- Возможен флейк при длительных интеграционных прогонах H2/Flyway.
- Возможны частичные AC, не покрытые явно отдельными методами, что потребует фиксации ограничений в отчете.

## Что уже выполнено
1. Обязательный контекст прочитан:
   - `.clinerules/*`
   - `memory-bank/00-project-brief.en.md`
   - `memory-bank/02-active-context.md`
   - `memory-bank/tasks.json`
   - `memory-bank/01-prd-index.en.md`
   - `memory-bank/05-task-execution-progress.en.md`
   - релевантная PRD-глава: `memory-bank/prd/06-acceptance-criteria.md`
2. Выбрана задача `TASK-040` (non-ui, pending, зависимости закрыты).
3. Зафиксирован план и рамки в текущем файле.
4. Прочитаны и проанализированы ключевые интеграционные тесты:
   - `UserAuthStudentFlowIntegrationTest`
   - `CourseLessonCrudIntegrationTest`
   - `SectionCatalogIntegrationTest`
   - `GroupManagementIntegrationTest`
   - `ProgramManagementIntegrationTest`
5. Выполнен целевой прогон тестового набора:
   - Команда:
     `mvn -f monolith-mvp/pom.xml -Dtest=UserAuthStudentFlowIntegrationTest,CourseLessonCrudIntegrationTest,SectionCatalogIntegrationTest,GroupManagementIntegrationTest,ProgramManagementIntegrationTest test`
   - Итог из лога (`C:\Users\User\AppData\Local\Temp\cline\background-1771961022557-8ey85ei.log`):
     - `CourseLessonCrudIntegrationTest`: Tests run 13, Failures 0, Errors 0
     - `GroupManagementIntegrationTest`: Tests run 3, Failures 0, Errors 0
     - `ProgramManagementIntegrationTest`: Tests run 3, Failures 0, Errors 0
     - `SectionCatalogIntegrationTest`: Tests run 2, Failures 0, Errors 0
     - `UserAuthStudentFlowIntegrationTest`: Tests run 1, Failures 0, Errors 0
     - Total: `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`
     - `BUILD SUCCESS`

## Важные ссылки/артефакты текущей итерации
- Основной лог тестового прогона: `C:\Users\User\AppData\Local\Temp\cline\background-1771961022557-8ey85ei.log`
- Обновленный active context: `memory-bank/02-active-context.md`
