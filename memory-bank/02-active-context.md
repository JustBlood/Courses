## Active Context — 2026-03-03 — FR-103/104/105 Programs (TOKEN GATE SNAPSHOT #2)

### 0) Почему остановка
- Сработало правило Token Budget Gate из `.clinerules/10-memory-bank-workflow.en.md`.
- Текущее использование контекста: **~358k/400k (>350k)**.
- По правилам реализацию нужно немедленно остановить и продолжить через `/newtask`.

---

### 1) Цель итерации
Продолжить стабилизацию learning-program функционала (FR-103/104/105):
- добить интеграцию прогресса курса в `ProgramService`;
- закрыть reset-progress MVP для программ;
- стабилизировать контракт assign endpoint’ов;
- прогнать целевые интеграционные тесты и устранить регрессии.

---

### 2) Что уже было в рабочем дереве до этой итерации
- `CoursesController`: добавлены admin endpoints `/api/v1/admin/courses/programs...`.
- `StudentController`: добавлены `/api/v1/student/my/programs` и `/api/v1/student/my/programs/{programId}`.
- `GroupService`: hooks на `programService.handleGroupMembershipAdded/Removed`.
- `ProgramService`: фикс `groupAssigned` через `isUserAssignedThroughAnyGroup(...)`.

---

### 3) Что сделано в ЭТОЙ итерации

#### 3.1 LearningService -> ProgramService интеграция
Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`

Сделано:
1. Добавлена зависимость `ProgramService`.
2. В `markEnrollmentStarted(...)` после первого выставления `startedAt` вызывается:
   - `programService.onCourseProgressChanged(userId, courseId)`.
3. В `markEnrollmentCompletedIfDone(...)` после первого выставления `completedAt` вызывается:
   - `programService.onCourseProgressChanged(userId, courseId)`.

Смысл: синхронизация progressive unlock/auto-enroll в программах при фактическом старте/завершении курса.

#### 3.2 Фикс updateProgram по unique-констрейнту program_courses
Проблема подтверждена тестами: `PUT /api/v1/admin/courses/programs/{programId}` падал с 500 (`uk_program_course`) на вставке новых `program_courses`.

Файлы:
- `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/ProgramCourseRepository.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/ProgramService.java`

Сделано:
1. В `ProgramCourseRepository` добавлен метод:
   - `void deleteByProgramId(Long programId);`
2. В `ProgramService.replaceProgramCourses(...)`:
   - перед пересборкой списка выполняется `programCourseRepository.deleteByProgramId(program.getId())` + `flush()`;
   - после формирования списка используется `learningProgramRepository.saveAndFlush(program)`.

Смысл: гарантировать удаление старых строк до вставки новых и избежать 500 при reorder/update программы.

#### 3.3 Черновая реализация reset-progress в ProgramService
Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/service/ProgramService.java`

Добавлен метод:
- `resetProgramCourseProgress(Long programId, Long userId, Long courseId)`

Логика метода:
1. Проверяет существование программы.
2. Проверяет, что пользователь назначен в программу.
3. Проверяет, что курс входит в программу.
4. Проверяет наличие `Enrollment` пользователя в курсе.
5. Запрещает reset для completed курса (`completedAt != null`).
6. Удаляет submissions пользователя по курсу: `lessonSubmissionRepository.deleteByStudentIdAndLesson_Course_Id(...)`.
7. Чистит `startedAt/completedAt` в enrollment, сохраняет и триггерит `onCourseProgressChanged(...)`.

Важно: endpoint в контроллере для этого метода **ещё не добавлен**.

---

### 4) Что проверено тестами
Запускалось:
- `mvn -pl monolith-mvp -Dtest=ProgramManagementIntegrationTest,GroupManagementIntegrationTest test -q`

Результат:
- `GroupManagementIntegrationTest` проходил.
- `ProgramManagementIntegrationTest` падал (2 fail) на update program со статусом 500,
  из-за `DataIntegrityViolationException` (`uk_program_course`), что и привело к фиксу п.3.2.

После фикса п.3.2 полный повторный прогон **не успел выполниться** в этой итерации из-за Token Gate.

---

### 5) Текущее состояние незавершенных задач
1. **Допрогон тестов после последних правок**:
   - минимум `ProgramManagementIntegrationTest` и `GroupManagementIntegrationTest`.
2. **Добавить ADMIN endpoint reset-progress** в `CoursesController`:
   - например `POST /api/v1/admin/courses/programs/{programId}/users/{userId}/courses/{courseId}/reset-progress`.
3. **Добавить/обновить интеграционные тесты reset-progress**:
   - reset для not-completed курса (200);
   - reset completed курса (400);
   - reset курса вне программы (400);
   - reset при отсутствии enrollment (404).
4. Проверить, что контракты assign (`idsIn/idsNotIn` + backward-compat `ids`) корректно покрыты тестами и не регресснули.

---

### 6) Список ключевых измененных файлов на момент стопа
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/ProgramCourseRepository.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/ProgramService.java`
- `memory-bank/02-active-context.md`

Плюс ранее уже измененные (до этой итерации):
- `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/StudentController.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/GroupService.java`

---

### 7) Риски и примечания
- Возможны компиляционные/контекстные ошибки из-за новой зависимости `LearningService -> ProgramService` и расширения `ProgramService`.
- Reset-метод реализован в сервисе, но без endpoint и без тестового покрытия пока не считается завершенным FR/AC.
- Есть несвязанные изменения в рабочем дереве (`docker-compose.monolith.yml`, `scripts/test.py`, `backups/`) — не трогались в рамках задачи.

---

### 8) Первый шаг в новом `/newtask`
1. Повторно запустить:
   - `mvn -pl monolith-mvp -Dtest=ProgramManagementIntegrationTest,GroupManagementIntegrationTest test`
2. Если update program ещё падает — дофиксить точечно.
3. Добавить controller endpoint reset-progress + интеграционные тесты.
4. Снова прогнать целевые тесты.
