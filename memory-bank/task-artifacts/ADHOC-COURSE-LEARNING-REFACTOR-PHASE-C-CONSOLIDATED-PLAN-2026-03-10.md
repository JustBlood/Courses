# ADHOC — Phase C: Consolidated Architecture Plan / Backlog / Readiness (2026-03-10)

Основано на:
- `ADHOC-COURSE-LEARNING-REFACTOR-PHASE-A-DOMAIN-CONTRACT-DECISION-MATRIX-2026-03-10.md`
- `ADHOC-COURSE-LEARNING-REFACTOR-PHASE-B-SUBAGENT-ARCH-ANALYSIS-2026-03-10.md`

---

## 1) Единый архитектурный план (Phase C consolidation)

## 1.1 Guiding principles

1. Декомпозиция сервисов — приоритет №1, но без роста roundtrip к БД.
2. Текущие API точки (`CoursesController`, `StudentController`, `ProgressController`) сохраняются через compatibility-facade слой на первых итерациях.
3. Выносить сначала read-only/low-risk области, затем write/transaction-heavy.
4. Любая декомпозиция должна сопровождаться query-агрегацией в statistics и enrollment propagation, иначе выигрыша по сопровождению не будет.

## 1.2 Target component map

## Course area
- `CourseService` (временный facade/orchestrator)
- `CourseCatalogService`
- `CourseLessonAdminService`
- `CourseLearnerReadService`
- `CourseAssignmentService`
- `CourseAccessPolicy`

## Learning area
- `LearningService` (временный facade/orchestrator)
- `LessonAccessPolicy`
- `PracticeSubmissionService`
- `OpenReviewService`
- `PracticeScoringPolicy`
- `EnrollmentProgressService`

## Statistics area
- `StatisticsQueryService` (агрегирующие read-model запросы)
- `StatisticsReportService` (API DTO composition)
- `CsvReportRenderer` (format/escaping only)

## Program integration area (wave-1 bounded)
- `CourseEnrollmentPort` (контрактная граница интеграции Program -> Course)
- стабилизированный callback-контракт `onCourseProgressChanged` (Learning -> Program)

---

## 2) Final refactor backlog (order + dependencies + risks)

## Wave 0 — Domain/contract alignment

`REF-CM-01` Domain/DTO alignment
- Включает:
  - remove `Course.deadlineAt`, `allowContinueAfterFail`, `keepAccessAfterDeadline`;
  - keep configurable `blockAfterDeadline`, `includeInOverallStats`, `passingThresholdPercent`;
  - remove `TheoryLesson.contentType` + `TheoryLesson.syncLessonType()`;
  - finalize `lessonType` as single source of truth;
  - make `PracticeQuestion.questionType` non-null;
  - keep `LessonSubmission.status + completed`.
- Dependencies: none.
- Main risk: admin DTO payload compatibility for non-key fields.

## Wave 1 — Service decomposition baseline

`REF-CM-02` CourseService decomposition
- Dependency: `REF-CM-01`.
- Extract learner read-model first, then lesson admin, then assignments.
- Risk: reorder/position regression, side-effects order (file deletion, events).

`REF-CM-03` LearningService decomposition
- Dependency: `REF-CM-02` (for stable access policy boundary reuse).
- Extract scoring+validation first, then review, then submit, then enrollment lifecycle.
- Risk: race-condition behavior drift in submit/review.

## Wave 2 — Performance and data access

`REF-CM-04` Statistics + repository aggregation optimization
- Dependency: `REF-CM-03` (shared status/completion semantics already stabilized).
- Introduce batch aggregate queries + service split (`QueryService`/`ReportService`/`CsvRenderer`).
- Risk: stat numeric regressions and CSV column value drift.

`REF-CM-07` Repository cleanup (unused methods) + regression safety
- Dependency: `REF-CM-04` (after new query layer added).
- Remove dead repo methods only after usage scan + tests pass.
- Risk: hidden reflective or future-call usage (low but possible).

## Wave 3 — Program integration alignment

`REF-CM-05` ProgramService integration alignment
- Dependency: `REF-CM-02` + `REF-CM-03`.
- Scope limited to integration boundaries and idempotent propagation; full Program decomposition deferred.
- Risk: program availability/enrollment propagation regressions.

## Cross-wave

`REF-CM-06` Mapper-centric DTO transformations
- Dependency: starts after `REF-CM-02`, completes after `REF-CM-05`.
- Move manual DTO wrapping from services to mapper layer where safe.
- Risk: accidental API shape changes in key contracts.

---

## 3) Readiness criteria to move from analysis -> implementation

Переход в реализацию считается готовым, если:

1. Зафиксирован и принят domain/contract matrix (Phase A artifact).
2. Зафиксированы service boundaries и wave-sequencing (этот документ).
3. Для Wave 0 подтверждена миграционная стратегия:
   - drop columns list,
   - null-data precheck для `practice_questions.question_type`.
4. Утверждён compatibility policy:
   - ключевые API контракты не ломаем,
   - вторичные/вспомогательные DTO можно менять.
5. Утверждён performance guardrail:
   - запрет на декомпозицию, увеличивающую DB roundtrip без компенсации агрегирующими запросами.
6. Определён regression test gate (минимум):
   - `CourseLessonCrudIntegrationTest`,
   - `Task05SubmitFlowIntegrationTest`,
   - `Task06ReviewFlowIntegrationTest`,
   - `Task07StatisticsAndLearnerSummaryIntegrationTest`,
   - `Task08LearnerAnswersVisibilityIntegrationTest`,
   - `ProgramManagementIntegrationTest`.

---

## 4) Итог статуса фазы анализа

Runbook phase A/B/C закрыт как analysis package:
- Domain/contract decisions fixed,
- Subagent architecture analysis fixed,
- Consolidated implementation backlog and readiness criteria fixed.

Статус: **READY FOR IMPLEMENTATION PLANNING / EXECUTION**.

---

## 5) Execution status update — Wave 0 (REF-CM-01) completed on 2026-03-10

Wave 0 (`REF-CM-01`) implementation has been executed in the codebase.

Completed scope:

1. Domain/DB contract alignment
   - Removed `Course.deadlineAt`, `allowContinueAfterFail`, `keepAccessAfterDeadline` from model/DTO/service flow.
   - Removed `TheoryLesson.contentType` and sync methods in `TheoryLesson`/`PracticeLesson`.
   - Finalized `lessonType` as authoritative field for theory flows.
   - Enforced `PracticeQuestion.questionType` non-null at model + DB level.

2. Migration alignment
   - Added migration `V4__course_learning_wave0_domain_contract_alignment.sql`:
     - drop columns:
       - `courses.deadline_at`,
       - `courses.allow_continue_after_fail`,
       - `courses.keep_access_after_deadline`,
       - `lessons.theory_content_type`;
     - precheck/fix for `practice_questions.question_type` null values;
     - apply `NOT NULL` constraint on `practice_questions.question_type`.

3. Dead code cleanup
   - Removed unused `QuestionProgressJsonConverter`.
   - Removed obsolete `TheoryContentType` enum.

4. Compatibility and regression gate verification
   - Key learner APIs remained stable (`StudentController` / `ProgressController` behavior validated by integration suite).
   - Wave-0 regression gate executed successfully:
     - `CourseLessonCrudIntegrationTest`
     - `Task05SubmitFlowIntegrationTest`
     - `Task06ReviewFlowIntegrationTest`
     - `Task07StatisticsAndLearnerSummaryIntegrationTest`
     - `Task08LearnerAnswersVisibilityIntegrationTest`
     - `ProgramManagementIntegrationTest`
   - Result: **BUILD SUCCESS**, 33 tests run, 0 failures/errors.

Wave status after execution: **WAVE 0 DONE, READY FOR WAVE 1 (REF-CM-02)**.

## 6) Execution status update — Wave 1 (REF-CM-02/REF-CM-03) completed on 2026-03-11

Wave 1 baseline implementation has been completed in the current working tree.

Completed scope:

1. `REF-CM-02` (Course decomposition baseline)
   - `CourseService` continues as compatibility facade.
   - Extracted services are active in integration path:
     - `CourseAccessPolicy`,
     - `CourseLearnerReadService`,
     - `CourseLessonAdminService`,
     - `CourseAssignmentService`.

2. `REF-CM-03` (Learning decomposition baseline)
   - `LearningService` restored and switched to delegation for submit/review orchestration:
     - `PracticeSubmissionService`,
     - `OpenReviewService`,
     - policy checks through `LessonAccessPolicy`.
   - learner lesson read flow intentionally remains assembled in `LearningService` as facade/read-model orchestration.

3. Program integration boundary alignment (Wave 1 bounded scope)
   - `ProgramService` switched from `CourseService` direct enrollment calls to `CourseEnrollmentPort`.
   - `GroupService` switched from `CourseService` direct enrollment calls to `CourseEnrollmentPort`.

Verification:

- Compile gate:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` -> **BUILD SUCCESS**.
- Wave 1 regression gate:
  - `CourseLessonCrudIntegrationTest`
  - `Task05SubmitFlowIntegrationTest`
  - `Task06ReviewFlowIntegrationTest`
  - `Task07StatisticsAndLearnerSummaryIntegrationTest`
  - `Task08LearnerAnswersVisibilityIntegrationTest`
  - Result: **BUILD SUCCESS**, tests run: 25, failures: 0, errors: 0.

Wave status after execution: **WAVE 1 DONE (baseline), READY FOR WAVE 2 (REF-CM-04/REF-CM-07)**.

## 7) Execution status update — Wave 2 (REF-CM-04/REF-CM-07) completed on 2026-03-11

Wave 2 implementation has been completed in the current working tree.

Completed scope:

1. `REF-CM-04` (Statistics + repository aggregation optimization)
   - `StatisticsService` preserved as compatibility facade and delegated to:
     - `StatisticsReportService` (DTO/report composition),
     - `StatisticsQueryService` (aggregated read-model queries),
     - `CsvReportRenderer` (CSV formatting/escaping only).
   - Added batch aggregation queries and projections to reduce roundtrips:
     - `LessonSubmissionRepository`:
       - points sum by `(userId, courseId)`,
       - completed lessons count by `(userId, courseId)`,
       - retakes sum by `(userId, courseId)`;
     - `LessonRepository`:
       - max points by `courseId` batch,
       - lessons count by `courseId` batch;
     - `EnrollmentRepository`:
       - join-fetch read models for user/course/all enrollments;
     - `GroupMembershipRepository`:
       - join-fetch memberships for user set.

2. `REF-CM-07` (repository cleanup + regression safety)
   - Removed unused repository methods after usage scan and verification:
     - `LessonSubmissionRepository.findFirstByStudentIdAndLessonIdAndStatusOrderBySubmittedAtDesc(...)`,
     - `LessonSubmissionRepository.findFirstByStudentIdAndLessonIdOrderBySubmittedAtAsc(...)`.
   - Existing locking method `findWithLockingByStudentIdAndLessonId(...)` was kept (actively used in submit/review critical paths).

Verification:

- Wave 2 compile gate:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` -> **BUILD SUCCESS**.
- Wave regression gate:
  - `CourseLessonCrudIntegrationTest`
  - `Task05SubmitFlowIntegrationTest`
  - `Task06ReviewFlowIntegrationTest`
  - `Task07StatisticsAndLearnerSummaryIntegrationTest`
  - `Task08LearnerAnswersVisibilityIntegrationTest`
  - `ProgramManagementIntegrationTest`
  - Result: **BUILD SUCCESS**, tests run: 33, failures: 0, errors: 0.

Wave status after execution: **WAVE 2 DONE, READY FOR WAVE 3 (REF-CM-05) and cross-wave REF-CM-06 continuation**.
