# ADHOC — Phase B: Архитектурный анализ сервисов через субагентов (2026-03-10)

Источник: запуск 4 параллельных subagent-потоков `backend-architect` по B1/B2/B3/B4 runbook.

---

## B1. CourseService — целевые границы декомпозиции

## Текущее состояние (кластеры ответственности)

`monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java` сейчас совмещает:

1. Course catalog CRUD + metadata (`createCourse`, `updateCourse`, `deleteCourse`, summaries/details).
2. Lesson authoring lifecycle (create/update/delete theory/practice, reorder, question pool).
3. Learner read-model по курсу (`getCourseForLearner`, `findNextLessonIdForLearner`, block reasons).
4. Enrollment/reviewer/group assignment orchestration.
5. Access/deadline guards (`isUserEnrolled`, `assertCourseDeadlineNotExceededForStudent`).
6. DTO assembly и mapper-обходы (ручные обёртки поверх `CourseMapper`/`LessonMapper`).

## Целевые границы

Рекомендуемая прагматичная декомпозиция (без переатомизации):

1. `CourseCatalogService`
   - CRUD курса, section binding, cover lifecycle.
2. `CourseLessonAdminService`
   - create/update/delete/reorder lessons, practice questions.
3. `CourseLearnerReadService`
   - learner course projection, next-lesson resolution, block reasons.
4. `CourseAssignmentService`
   - enrollments, reviewers, group-course assignment.
5. `CourseAccessPolicy` (policy-компонент)
   - enrollment/deadline/sequence/stop checks как переиспользуемая политика.

Текущий `CourseService` оставить как orchestration/facade на период миграции контрактов.

## Что централизовать из дублей

- Lesson type checks (`isTheoryLesson`) в единый policy/helper (сейчас дубли в `CourseService` и `LearningService`).
- Position shifting/reorder logic (сейчас разбросан по `resolveCreateLessonPosition`, `applyLessonPositionPatch`, `setLessonsToNewPositionsIfNeeded`).
- Course learner projection builder (`loadSubmissionsByLessonId`, `resolvePassedLessonIds`, `buildLearnerLessonSummaries`) как отдельный read-model assembler.
- File path normalize/cleanup policy (course cover lifecycle) в отдельный компонент рядом с `FileStorageService`.

## Транзакции и влияние на БД

Сейчас основные риски:
- Многошаговые reorder-операции с несколькими flush/save циклам.
- Group assignment вызывает проверку `existsByUserIdAndCourseId` на каждого membership (roundtrip-рост).
- Learner summary тянет submission list + расчёты в сервисе.

Рекомендации:
- Сохранить coarse-grained транзакции на use-case уровне (один public use-case = одна транзакция), не дробить на слишком мелкие transactional-компоненты.
- Для group-course assignment перейти к батч-предзагрузке существующих enrollment (set-based), затем insert only missing.
- Для learner summary — добавить проекции/агрегирующие запросы по completed/points там, где это даст измеримый профит.

## Порядок безопасного извлечения

1. Вынести read-only learner projection (`CourseLearnerReadService`) — минимальный риск.
2. Вынести admin lesson lifecycle.
3. Вынести assignments.
4. Вынести catalog CRUD.
5. Оставить `CourseService` как compatibility-facade, затем постепенно сужать.

Rollback point после каждого шага: старый `CourseService` всё ещё публичная точка входа.

## Ключевые риски

- Регрессии API payload по admin course/lesson endpoints (`CoursesController`).
- Нарушение порядка уроков при reorder/delete.
- Изменение side effects (event logs, file deletion order).
- Скрытое ухудшение DB roundtrip после слишком «чистой» декомпозиции.

Приоритет тестов:
- `CourseLessonCrudIntegrationTest` + learner course summary scenarios.
- Проверки assignment/reviewer/group flows.

---

## B2. LearningService — разрез access/submit/review/scoring/lifecycle

Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`

## Текущий разрез ответственности

1. Access policy:
   - `validateStudentEnrolled`, `assertLessonAccessAllowed`, `assertStopLessonAccessAllowed`, deadline checks через `CourseService`.
2. Submit:
   - theory completion (`completeTheoryLesson*`),
   - practice submit (`submitPractice`) для test/open.
3. Review:
   - `getPendingReviews`, `getPendingReviewQuestions`, `reviewOpenSubmission`.
4. Scoring:
   - `evaluateCorrectness`, `scoreQuestion`, `resolveTestQuestionPointsType`, `resolveOpenQuestionPointsType`.
5. Enrollment lifecycle hooks:
   - `markEnrollmentStarted`, `markEnrollmentCompletedIfDone` + callback в `ProgramService`.

## Дубли/кандидаты на policy/utility

- Review precondition validation дублируется в `validateAndGetPracticeLesson` и `validateSubmissionForReviewAndGetPracticeLesson`.
- Theory/test/open lesson type checks дублируются между сервисами.
- Ответо-нормализация/валидация (map of question answers) может быть отделена в `PracticeSubmissionValidator`.
- Scoring отдельно как `PracticeScoringPolicy`.

## Риски race-condition/consistency

1. Submit race на одном `(student, lesson)`:
   - чтение+запись без pessimistic lock в основном path.
   - при параллельном submit возможны lost update/двойной increment `attemptCounter`.
2. Review race:
   - два reviewer запроса до финализации могут перезаписать `questionProgress`.
3. Enrollment completion race:
   - `markEnrollmentCompletedIfDone` считает passed lessons и затем пишет enrollment; при параллельных completions возможны лишние roundtrip/повторный update.

Митигации:
- Для write-critical flows использовать `findWithLockingByStudentIdAndLessonId` (или optimistic versioning) в submit/review.
- Для review — final-state recheck под lock перед сохранением.
- Для completion lifecycle — один atomic update path (или idempotent update with where completed_at is null).

## Целевая архитектура (без over-fragmentation)

1. `LearningOrchestratorService` (текущий вход).
2. `LessonAccessPolicy`.
3. `PracticeSubmissionService`.
4. `OpenReviewService`.
5. `PracticeScoringPolicy`.
6. `EnrollmentProgressService`.

Важно: сохранить use-case транзакции целостными; не превращать каждую helper-операцию в отдельную транзакцию.

## Пошаговый extraction roadmap

1. Extract scoring + answer validation (чистые функции).
2. Extract review service.
3. Extract submission service.
4. Extract access policy.
5. Extract enrollment lifecycle.

Тесты-фокус:
- `Task05SubmitFlowIntegrationTest`
- `Task06ReviewFlowIntegrationTest`
- `Task08LearnerAnswersVisibilityIntegrationTest`
- deadline/attemptLimit/timeLimit + stop-lesson блокировки.

---

## B3. StatisticsService + Repository layer

Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/service/StatisticsService.java`

## Hotspots N+1 / roundtrip

1. `userCourseStats` / `toStudentCourseStat`:
   - per enrollment вызываются `maxPoints`, `earnedPoints`, `countDistinctCompletedLessons`, `countByCourseId`.
2. `courseStats(courseId)`:
   - per student в course вызываются `earnedPoints` + `countDistinctCompletedLessons`.
3. `summaryReportCsv()` и `summaryReportCsv(courseId)`:
   - nested per-row вычисления с повторным чтением lessons/submissions + memberships.

## Недостающие агрегирующие запросы (кандидаты)

1. Batch points per user/course:
   - `select s.student.id, s.lesson.course.id, sum(coalesce(s.pointsAwarded,0)) ... group by ...`
2. Batch completed lessons per user/course:
   - `select s.student.id, s.lesson.course.id, count(distinct s.lesson.id) where s.completed=true group by ...`
3. Course max points in один запрос:
   - `select l.course.id, sum(coalesce(l.fullPoints,0)) from Lesson l where l.course.id in :courseIds group by l.course.id`.
4. Retakes batch:
   - `sum(greatest(coalesce(s.attemptCounter,0)-1,0)) group by student/course`.
5. Enrollment join-fetch projection:
   - чтобы убрать LAZY N+1 по `Enrollment.user` / `Enrollment.course`.

## Repository API redesign

Добавить:
- агрегатные projection query methods (stats read model repositories).
- join-fetch methods для enrollments.

Кандидаты на cleanup (после проверки usage):
- `LessonSubmissionRepository.findFirstByStudentIdAndLessonIdAndStatusOrderBySubmittedAtDesc`.
- `LessonSubmissionRepository.findWithLockingByStudentIdAndLessonId` (если окончательно не используем в refactor path — иначе наоборот сделать обязательным).

## Разделение StatisticsService

Предложение:
1. `StatisticsQueryService` — собирает числовые read-model метрики.
2. `StatisticsReportService` — формирует DTO для API.
3. `CsvReportRenderer` — только рендер CSV (escape/header/row formatting).

Ожидаемый эффект:
- существенно меньше roundtrip на статистику/CSV,
- изоляция форматирования от бизнес-расчётов,
- проще regression тестирование.

---

## B4. ProgramService — глубина включения в первую волну

Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/service/ProgramService.java`

## Текущее положение и связность

`ProgramService` объединяет:
- program CRUD/composition,
- user/group assignments,
- enrollment propagation в курсы,
- availability read model (`toProgramDto`),
- progress reset внутри program context.

Критические coupling points:
- `ensureProgramCourseEnrollmentsForUser` -> `CourseService.enrollStudentToCourse`.
- `LearningService.markEnrollmentStarted/CompletedIfDone` -> `ProgramService.onCourseProgressChanged`.

## Что включать в wave-1

Включить только то, что необходимо для безопасной декомпозиции Course/Learning:

1. Явный integration boundary для course enrollment propagation.
2. Стабилизацию callback-контракта `onCourseProgressChanged`.
3. Идемпотентность enrollment propagation (batch + dedup).

Отложить:
- глубокую декомпозицию program CRUD/read-model,
- оптимизацию program availability алгоритма,
- возможную event-driven переработку.

## Риски и митигации

1. Массовые roundtrip в `ensureProgramCourseEnrollmentsForUser` (по каждому курсу + enrollment check).
   - mitigation: batch preload enrollments per user for all program courses.
2. Циклическая связность Course/Learning/Program.
   - mitigation: отдельный `CourseEnrollmentPort` интерфейс + однонаправленный callback контракт.
3. Неконсистентный reset flow:
   - часть reset логики дублируется с CourseService reset.
   - mitigation: единый reset use-case компонент, вызываемый обоими сервисами.

## Тесты приоритета

- `ProgramManagementIntegrationTest` (assignments, availability, reset).
- regression на цепочку: submit/complete -> `onCourseProgressChanged` -> availability update.

---

## Выход фазы B

Фаза B runbook выполнена:
- B1 CourseService boundaries/transactions/db-impact,
- B2 LearningService decomposition + race-risk analysis,
- B3 Statistics/repository aggregation analysis,
- B4 ProgramService integration depth and safe boundaries.
