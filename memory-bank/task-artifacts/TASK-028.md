# TASK-028 (done) — FR-108: массовые назначения через группы

## Что сделано
- Доведена до конца backend-реализация массовых назначений course/program через группы с автоприменением для новых участников.

### 1) Persistent assignment-связи (group→target)
- Используются таблицы из миграции `V4__group_target_assignments.sql`:
  - `group_course_assignments`
  - `group_program_assignments`
- Сущности/репозитории:
  - `GroupCourseAssignment`, `GroupProgramAssignment`
  - `GroupCourseAssignmentRepository`, `GroupProgramAssignmentRepository`

### 2) ProgramService: group→program + backfill
- Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/service/ProgramService.java`
- В `assignGroupToProgram(...)` реализовано:
  - загрузка и валидация группы;
  - сохранение persistent связи `group_program_assignments` (идемпотентно);
  - backfill текущих участников группы в program enrollments.

### 3) GroupService: авто-применение assignment новым участникам
- Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/service/GroupService.java`
- После добавления новых memberships вызывается автоприменение assignment только для новых участников:
  - group→course через `courseService.assignStudentToCourse(...)`;
  - group→program через `programService.assignUsersToProgram(...)`.
- Применение ограничено ролью `STUDENT`.

### 4) CoursesController: включены admin endpoints
- Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
- Включены endpoint-ы:
  - `POST /api/v1/admin/courses/{courseId}/groups/assign`
  - `DELETE /api/v1/admin/courses/{courseId}/groups/assign`
  - `POST /api/v1/admin/courses/groups/{groupId}/assign`
  - `POST /api/v1/admin/courses/programs/{programId}/groups/assign`

### 5) Интеграционный тест TASK-028
- Файл: `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/GroupManagementIntegrationTest.java`
- Добавлен сценарий:
  - `should_assign_course_and_program_to_group_and_auto_apply_for_new_member()`
  - проверяет:
    1. назначение курса/программы группе с текущими участниками,
    2. наличие enrollments/programEnrollments у текущих участников,
    3. авто-применение после добавления нового участника в группу.

## Проверки
- Команда:
  - `mvn -f monolith-mvp/pom.xml -Dtest=GroupManagementIntegrationTest test`
- Результат:
  - `BUILD SUCCESS`
  - `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`

## Статус задачи
- TASK-028 переведена в `done`.

## Копия активного контекста на момент завершения

```md
# Активный контекст

## Текущая задача
- **ID:** TASK-028
- **Категория:** integration
- **Приоритет:** high
- **Статус:** in progress (почти завершена)

## Причина фиксации
- Сработал **Token Budget Gate**: Context Window Usage > 350000.
- Текущее значение на момент остановки: **~356711 / 400000**.
- По правилам `.clinerules/10-memory-bank-workflow.md` реализацию нужно немедленно приостановить и зафиксировать детальный контекст.

---

## Источники требований
- `memory-bank/tasks.json` → TASK-028
- `memory-bank/prd/02-functional-requirements.md` → FR-108
- `memory-bank/prd/06-acceptance-criteria.md` → AC-108
- `.clinerules/*`

## Цель TASK-028
Реализовать массовые назначения курсов и программ через группы:
1. Назначение группе применяется к текущим участникам.
2. Назначение автоматически применяется к новым участникам группы.
3. Операции остаются идемпотентными и без дублей.

---

## Что уже было сделано до этой итерации
1. Добавлены сущности и репозитории:
   - `GroupCourseAssignment`, `GroupProgramAssignment`
   - `GroupCourseAssignmentRepository`, `GroupProgramAssignmentRepository`
2. Добавлена миграция:
   - `V4__group_target_assignments.sql`
3. `CourseService.assignGroupToCourse(...)` уже сохранял persistent group→course assignment + backfill участников.

---

## Что реализовано в текущей итерации (после продолжения TASK-028)

### 1) ProgramService доработан
Файл:
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/ProgramService.java`

Изменения:
- Добавлены зависимости:
  - `GroupProgramAssignmentRepository`
  - `LearningGroupRepository`
- В `assignGroupToProgram(Long programId, UUID groupId)`:
  - добавлена загрузка/валидация группы (`NotFoundException` если группы нет),
  - добавлено сохранение persistent связи `group_program_assignments` (если ещё нет),
  - сохранён backfill участников группы через существующую логику,
  - backfill выполняется только для `STUDENT` (согласовано с правилами `assignUserToProgram`).

### 2) GroupService доработан
Файл:
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/GroupService.java`

Изменения:
- Добавлены зависимости:
  - `GroupCourseAssignmentRepository`
  - `GroupProgramAssignmentRepository`
  - `CourseService`
  - `ProgramService`
  - импорт `Role`
- В `addUsersToGroup(...)`:
  - после `membershipRepository.saveAll(membershipsToCreate)` вызывается автоприменение assignment только для **новых** memberships.
- Добавлен приватный метод:
  - `applyAssignmentsToNewMembers(UUID groupId, List<GroupMembership> newMemberships)`
  - выбирает только новых пользователей с ролью `STUDENT`,
  - применяет все group→course assignments (через `courseService.assignStudentToCourse`),
  - применяет все group→program assignments (через `programService.assignUsersToProgram(..., List.of(studentId))`).

### 3) CoursesController — включены admin endpoints
Файл:
- `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`

Изменения:
- Добавлены импорты:
  - `UuidIdsRequest`
  - `GroupAssignmentRequest`
  - `ProgramTargetType`
  - `UUID`
- Раскомментированы/включены endpoints:
  - `POST /api/v1/admin/courses/{courseId}/groups/assign`
  - `DELETE /api/v1/admin/courses/{courseId}/groups/assign`
  - `POST /api/v1/admin/courses/groups/{groupId}/assign` (универсальный target endpoint)
  - `POST /api/v1/admin/courses/programs/{programId}/groups/assign`

### 4) Интеграционный тест TASK-028
Файл:
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/GroupManagementIntegrationTest.java`

Изменения:
- Добавлены `EnrollmentRepository` и `ProgramEnrollmentRepository` для проверок состояния БД.
- Добавлен тест:
  - `should_assign_course_and_program_to_group_and_auto_apply_for_new_member()`
  - сценарий:
    1. Создаёт группу, 3 студентов, курс, программу.
    2. Добавляет 2 студентов в группу.
    3. Назначает группе курс и программу через admin API.
    4. Проверяет наличие `Enrollment` и `ProgramEnrollment` у первых двух участников.
    5. Добавляет нового участника в группу.
    6. Проверяет, что у нового участника назначения появились автоматически.
- Добавлены helper-методы `createCourse(...)` и `createProgram(...)` для теста.

---

## Проверка тестами
Команда:
- `mvn -f monolith-mvp/pom.xml -Dtest=GroupManagementIntegrationTest test`

Результат:
- **BUILD SUCCESS**
- `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`
```