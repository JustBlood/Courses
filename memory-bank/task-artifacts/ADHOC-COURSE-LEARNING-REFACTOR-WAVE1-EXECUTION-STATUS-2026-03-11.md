# ADHOC — Course/Learning Refactor Wave 1: Execution Status Snapshot (2026-03-11)

## 1) Контекст

Документ создан как "handoff" для следующего запуска агента по задаче:
- продолжение рефакторинга по Phase C;
- текущая целевая волна: **Wave 1 — Service decomposition baseline** (`REF-CM-02`, `REF-CM-03`).

Базовые артефакты (обязательный контекст):
- `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-PHASE-A-DOMAIN-CONTRACT-DECISION-MATRIX-2026-03-10.md`
- `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-PHASE-B-SUBAGENT-ARCH-ANALYSIS-2026-03-10.md`
- `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-PHASE-C-CONSOLIDATED-PLAN-2026-03-10.md`

---

## 2) Почему выполнение остановилось

Остановка произошла не по логике реализации, а из-за прерывания сессии (восстановление через `[TASK RESUMPTION]` спустя ~16 часов).

Дополнительно в момент перед прерыванием были tool-вызовы `apply_patch`, по которым не вернулся результат (`result missing`), из-за чего часть запланированных правок (в `LearningService`/`ProgramService`/`GroupService`) не была подтверждена применённой.

---

## 3) Что уже сделано (фактическое состояние кода)

### 3.1 Wave 1 — `REF-CM-02` (CourseService decomposition)

Реализованы и присутствуют в проекте новые сервисы:
- `CourseAccessPolicy`
- `CourseLearnerReadService`
- `CourseLessonAdminService`
- `CourseAssignmentService`
- `CourseEnrollmentPort`

`CourseService` уже переведён в режим compatibility facade с делегированием в extracted-сервисы по крупным зонам ответственности.

### 3.2 Wave 1 — задел под `REF-CM-03` (Learning decomposition)

Добавлены новые компоненты:
- `PracticeScoringPolicy`
- `LessonAccessPolicy`
- `EnrollmentProgressService`
- `OpenReviewService`
- `PracticeSubmissionService`

Важно: на момент этого snapshot **`LearningService` ещё не переведён на делегирование** в эти компоненты (в текущем файле остаётся старая монолитная реализация).

### 3.3 Компиляция

Проверка компиляции выполнена:
- `mvn -f monolith-mvp/pom.xml -DskipTests compile`
- результат: **BUILD SUCCESS** (2026-03-11 08:44 +03)

---

## 4) Git-состояние (на момент snapshot)

### 4.1 Stage/index (добавлено)

В staged состоянии находятся:
- `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-ANALYSIS-PLAN-2026-03-10.md`
- `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-PHASE-B-SUBAGENT-ARCH-ANALYSIS-2026-03-10.md`
- `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-PHASE-C-CONSOLIDATED-PLAN-2026-03-10.md`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseAccessPolicy.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseAssignmentService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseEnrollmentPort.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseLearnerReadService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseLessonAdminService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/PracticeScoringPolicy.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LessonAccessPolicy.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/EnrollmentProgressService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/OpenReviewService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/PracticeSubmissionService.java`

### 4.2 Unstaged

- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java` (существенный diff фасадизации)
- `docker-compose.monolith.yml` (вне текущего scope задачи)

### 4.3 Untracked

- `memory-bank/task-artifacts/ADHOC-LEARNER-ANSWERS-VISIBILITY-SUMMARY-2026-03-09.md` (вне текущего scope волны)

---

## 5) Что НЕ завершено (remaining work)

### 5.1 `REF-CM-03` не доведён до рабочего baseline

Не сделано/не подтверждено:
- перевод `LearningService` на delegation в:
  - `LessonAccessPolicy`
  - `PracticeSubmissionService`
  - `OpenReviewService`
  - `PracticeScoringPolicy`
  - `EnrollmentProgressService`
- удаление/сужение дублирующей логики из `LearningService` после делегирования.

### 5.2 Program integration boundary (wave-1 bounded)

Не применены правки перевода зависимостей на порт:
- `ProgramService`: `CourseService` -> `CourseEnrollmentPort`
- `GroupService`: `CourseService` -> `CourseEnrollmentPort`

Сейчас оба сервиса всё ещё используют `CourseService.enrollStudentToCourse(...)` напрямую.

### 5.3 Regression gate Wave 1

Не запущен обязательный набор интеграционных тестов после текущего рефакторинга:
- `CourseLessonCrudIntegrationTest`
- `Task05SubmitFlowIntegrationTest`
- `Task06ReviewFlowIntegrationTest`
- `Task07StatisticsAndLearnerSummaryIntegrationTest`
- `Task08LearnerAnswersVisibilityIntegrationTest`
- `ProgramManagementIntegrationTest`

---

## 6) Риски текущего промежуточного состояния

1. **Дублирование логики** между `LearningService` и новыми сервисами Learning-области.
2. **Несогласованная интеграционная граница** Program/Group с Course (порт добавлен, но не внедрён в потребителях).
3. Возможный drift поведения review/submit, пока orchestration в `LearningService` не завершена.

---

## 7) Рекомендуемый порядок продолжения (next run)

1. Завершить `REF-CM-03`:
   - перевести публичные методы `LearningService` на делегирование;
   - удалить дублирующие private helper-блоки из `LearningService`.
2. Довести Program integration boundary:
   - переподключить `ProgramService` и `GroupService` на `CourseEnrollmentPort`.
3. Запустить regression gate список интеграционных тестов.
4. По результатам тестов:
   - исправить регрессии,
   - обновить phase artifacts A/B/C статусом выполнения Wave 1,
   - обновить `memory-bank/06-system-development-progress.md`.

---

## 8) Краткий итог

На текущий момент Wave 1 выполнена частично:
- `REF-CM-02` в основном реализован (Course area decomposition + facade behavior).
- `REF-CM-03` подготовлен архитектурно (созданы сервисы), но не завершён интеграционно (LearningService и Program boundary ещё не переключены полностью).

---

## 9) Execution update (same day) — Wave 1 closure status

После данного snapshot работы по незакрытым пунктам были продолжены и доведены до рабочего состояния.

### 9.1 Что доведено в коде

1. `LearningService` восстановлен и переключён на делегирование:
   - learner lesson read path оставлен в `LearningService` как orchestration/read assembly;
   - submit/review orchestration переведена на:
     - `PracticeSubmissionService`,
     - `OpenReviewService`;
   - policy checks перенесены на `LessonAccessPolicy`.

2. Program/Group integration boundary переключена на порт:
   - `ProgramService`: `CourseService` -> `CourseEnrollmentPort`;
   - `GroupService`: `CourseService` -> `CourseEnrollmentPort`.

### 9.2 Верификация

Компиляция:
- `mvn -f monolith-mvp/pom.xml -DskipTests compile` -> **BUILD SUCCESS**.

Wave 1 regression gate:
- `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest,Task05SubmitFlowIntegrationTest,Task06ReviewFlowIntegrationTest,Task07StatisticsAndLearnerSummaryIntegrationTest,Task08LearnerAnswersVisibilityIntegrationTest test`
- результат: **BUILD SUCCESS**, `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`.

Program integration gate:
- `mvn -f monolith-mvp/pom.xml -Dtest=ProgramManagementIntegrationTest test`
- результат: **BUILD SUCCESS**, `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`.

### 9.3 Обновлённый краткий статус

- `REF-CM-02`: выполнен.
- `REF-CM-03`: выполнен для текущей Wave 1 цели (delegation baseline + integration boundary alignment).
- Wave 1 regression gate: пройден (включая `ProgramManagementIntegrationTest`).
