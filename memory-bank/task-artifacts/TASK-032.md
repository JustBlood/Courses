# TASK-032 (done) — FR-105: назначение программ пользователю и группе

## Что сделано
- Закрыт сценарий `FR-105 / AC-105`: назначение программ конкретным пользователям и группам с проверкой, что назначение отражается у всех целевых пользователей.
- Включены ранее закомментированные публичные API-методы для admin и student контуров.
- Добавлен сквозной интеграционный e2e-тест, подтверждающий назначение через user+group и видимость в кабинете студента.

### Изменения в коде
1. `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
   - включён endpoint:
     - `POST /api/v1/admin/courses/programs/{programId}/assign`

2. `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/StudentController.java`
   - включены endpoint'ы:
     - `GET /api/v1/student/my/programs`
     - `GET /api/v1/student/my/programs/{programId}`

3. `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ProgramManagementIntegrationTest.java`
   - добавлен e2e-тест:
     - `should_assign_program_to_user_and_group_and_expose_in_student_cabinet()`
   - тест покрывает:
     - назначение программы пользователю через admin API;
     - назначение программы группе через admin API;
     - появление `program_enrollments` у всех целевых пользователей;
     - автозачисление на курсы программы;
     - видимость программы в student API (`/my/programs*`).

## Проверки
- Выполнена команда:
  - `mvn -f monolith-mvp/pom.xml -Dtest=ProgramManagementIntegrationTest,GroupManagementIntegrationTest test`
- Результат:
  - `BUILD SUCCESS`
  - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`

## Статус задачи
- `TASK-032` переведена в `done`.

## Копия активного контекста на момент завершения

```md
# Активный контекст

## Текущая задача
- **ID:** TASK-032
- **Категория:** integration
- **Приоритет:** high
- **Статус:** in progress

## Основание выбора
- В `memory-bank/tasks.json` выбрана pending-задача с наивысшим приоритетом среди задач с закрытыми зависимостями и `category != ui`.
- Проверены зависимости TASK-032: `TASK-030`, `TASK-027` — обе в статусе `done`.

## Требования задачи
- Реализовать **FR-105**: назначение программ пользователю и группе.
- Покрыть **AC-105**: назначение отражается у всех целевых пользователей.

## Релевантные источники
- `memory-bank/tasks.json` (TASK-032)
- `memory-bank/prd/02-functional-requirements.md` (FR-105)
- `memory-bank/prd/06-acceptance-criteria.md` (AC-105)

## Что уже есть в коде
- В `ProgramService` есть:
  - `assignUsersToProgram(...)`
  - `assignGroupToProgram(...)`
  - автоназначение курсов программы пользователю в `assignUserToProgram(...)`
- В `GroupService` уже есть автоприменение program-назначений новым участникам группы.
- Есть endpoint назначения программы группе: `POST /api/v1/admin/courses/programs/{programId}/groups/assign`.

## Выявленные gap
- Закомментирован endpoint назначения программы пользователю в `CoursesController`.
- Закомментированы student-endpoint'ы просмотра программ (`/api/v1/student/my/programs*`) в `StudentController`, из-за чего нельзя полноценно проверить «появляется в кабинете» по API.
- В текущих интеграционных тестах нет полного e2e-покрытия сценария TASK-032 через публичные endpoint'ы.

## План реализации
1. Включить endpoint назначения программы пользователям в `CoursesController`.
2. Включить endpoint'ы студента для просмотра своих программ в `StudentController`.
3. Расширить интеграционные тесты:
   - проверить назначение программы конкретному пользователю через admin API;
   - проверить, что у пользователя программа видна в student API;
   - проверить назначение программы группе и доступность программы/курсов у всех целевых пользователей.
4. Прогнать релевантные тесты модуля.

## Риски и меры
- **Риск регрессии API-контрактов student/admin:** минимизируется точечным включением уже существующих методов без изменения бизнес-логики.
- **Риск дублирования назначений:** логика уже идемпотентна (`existsByUserIdAndProgramId`), дополнительно проверяется тестами.

## Token Budget Gate checkpoint (обязательная фиксация)
- На момент фиксации: **Context Window Usage = 362,515 / 400K (91%)**, что превышает лимит из `.clinerules/10-memory-bank-workflow.md` (**>350000**).
- Реализация приостановлена до запуска новой задачи (`/newtask`).

## Что уже выполнено в TASK-032
1. Обновлён активный контекст под TASK-032.
2. Включён admin endpoint назначения программы пользователям:
   - `POST /api/v1/admin/courses/programs/{programId}/assign`
   - файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
3. Включены student endpoint'ы кабинета программ:
   - `GET /api/v1/student/my/programs`
   - `GET /api/v1/student/my/programs/{programId}`
   - файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/StudentController.java`
4. Добавлен e2e-интеграционный тест FR-105/AC-105:
   - `should_assign_program_to_user_and_group_and_expose_in_student_cabinet()`
   - файл: `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ProgramManagementIntegrationTest.java`
5. Дополнительно в том же тестовом классе уже присутствует тест правил доступа/дедлайна программ:
   - `should_apply_program_access_rules_and_deadline_blocking_for_student()`
6. Для тестов в `ProgramManagementIntegrationTest` добавлены зависимости:
   - `ProgramService`
   - `EnrollmentRepository`
   - `ProgramEnrollmentRepository`
   - и helper `createUser(...)`.

## Подтверждение прогонов
- Выполнена команда:
  - `mvn -f monolith-mvp/pom.xml -Dtest=ProgramManagementIntegrationTest,GroupManagementIntegrationTest test`
- Итог в терминале: **BUILD SUCCESS**.
- Результаты: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`.

## Изменённые файлы (фактический diff)
- `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/StudentController.java`
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ProgramManagementIntegrationTest.java`
- `memory-bank/02-active-context.md`

## Что осталось сделать после /newtask
1. Финализировать memory-bank артефакты по правилам:
   - создать файл артефакта `memory-bank/task-artifacts/TASK-032.md` (скопировать текущий active-context + итоги);
   - добавить запись в `memory-bank/05-task-execution-progress.md` со ссылкой на артефакт;
   - при необходимости обновить `memory-bank/06-system-development-progress.md` (если сочтём изменение архитектурно значимым);
   - очистить `memory-bank/02-active-context.md` после полного завершения.
2. Обновить статус задачи:
   - `python memory-bank/change_task_status.py TASK-032 done`
3. Подготовить финальный отчёт пользователю по выполненному TASK-032.
```