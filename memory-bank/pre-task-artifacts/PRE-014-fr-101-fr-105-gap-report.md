# PRE-014 — Сверка FR-101..FR-105 (sections/programs/program rules/assignments)

## 1) Контекст
- Задача: `PRE-014` из `memory-bank/pre-tasks.json`.
- Область проверки: разделы каталога, программы обучения, правила прохождения программ, назначения программ пользователям и группам.
- Источники требований:
  - `memory-bank/prd/02-functional-requirements.md` (`FR-101..FR-105`)
  - `memory-bank/prd/06-acceptance-criteria.md` (`AC-101..AC-105`)
  - `memory-bank/prd/01-user-scenarios.md` (`US-06`, `US-07`)

Проверенные файлы реализации:
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/ProgramService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/model/{LearningProgram,ProgramCourse,ProgramEnrollment,ProgramAccessCondition,Course}.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/program/{CreateLearningProgramRequest,LearningProgramDto,LearningProgramCourseDto,ProgramCourseSettingsRequest}.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/{LearningProgramRepository,ProgramCourseRepository,ProgramEnrollmentRepository,GroupMembershipRepository}.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/{CoursesController,StudentController}.java`
- `monolith-mvp/src/main/resources/db/migration/V1__init_schema.sql`

## 2) Матрица соответствия FR/AC

| ID | Статус | Наблюдение |
|---|---|---|
| FR-101 | missing | В кодовой базе отсутствуют model/service/controller/DTO для `section` (разделов каталога). |
| AC-101 | missing | Нет API/логики создания и редактирования разделов, нет участия разделов в навигации каталога. |
| FR-102 | missing | В `Course`/`CreateCourseRequest` отсутствует связь с разделом; создание курса “из контекста раздела” не реализовано. |
| AC-102 | missing | Автопривязка курса к разделу невозможна из-за отсутствия доменной модели и endpoint-контуров разделов. |
| FR-103 | partial | Программный домен и сервисная логика есть (`LearningProgram`, `ProgramCourse`, `ProgramService#createProgram`), порядок курсов поддержан через `orderIndex`; но публичные endpoint программы в `CoursesController` закомментированы. |
| AC-103 | partial | На уровне service сохраняется порядок курсов в программе, но критерий не выполняется end-to-end из-за отсутствия активного API-контракта. |
| FR-104 | partial | Поддержаны `accessCondition` (ALL_OPEN / PREVIOUS_COURSES_COMPLETED / PREVIOUS_COURSES_VIEWED_OR_PENDING), `deadlineAt`, `blockAfterDeadline`; фактическая дедлайн-блокировка в `ProgramService` закомментирована и не применяется. |
| AC-104 | partial | Логика доступа к следующему курсу по условию программы реализована частично; deadline-ограничение не действует в runtime. |
| FR-105 | partial | В сервисе реализованы назначения программ пользователю и группе (`assignUsersToProgram`, `assignGroupToProgram`) с автоматическим назначением курсов программы; но admin/student endpoint программы закомментированы, поэтому функционал недоступен через API. |
| AC-105 | partial | На уровне service есть распространение назначения на участников группы, но критерий не закрыт как пользовательский сценарий (нет активных endpoint). |

## 3) Ключевые GAP и риски

### High
1. **Полное отсутствие блока sections (`FR-101`, `FR-102`)**: нет сущности, миграций, API и связки `section -> courses`.
2. **Program API выключен**: endpoint-ы программ в `CoursesController` и `StudentController` закомментированы, из-за чего текущая логика `ProgramService` фактически неэкспонирована.

### Medium
1. **Дедлайн-правила программ не доведены до runtime enforcement**: участок проверки `blockAfterDeadline/deadlineAt` в `ProgramService#toProgramDto` закомментирован.
2. **Частичное рассогласование модели и схемы**: в `V1__init_schema.sql` таблица `program_courses` содержит `deadline_at` и `block_after_deadline`, но entity `ProgramCourse` этих полей не содержит (поля вынесены на уровень `LearningProgram`).

## 4) Итог по acceptance_criteria PRE-014

- ✅ Проверен блок программ и правил доступа (domain/service/migration/API-контуры).
- ✅ Проверены правила доступа к курсам программы (`ProgramAccessCondition`, доступность по цепочке курсов).
- ✅ Заполнена матрица соответствия `AC-101..AC-105` (`missing/partial`).

Сводка статусов PRE-014:
- `FR implemented=0, partial=3, missing=2`
- `AC implemented=0, partial=3, missing=2`

## 5) Проверка test_steps PRE-014

- Шаг 1: Проверить model/service/controller по programs/sections — ✅
- Шаг 2: Сопоставить с AC-101..AC-105 — ✅
- Шаг 3: Зафиксировать implemented/partial/missing — ✅
