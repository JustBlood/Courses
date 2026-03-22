# ADHOC — Phase A: Domain/Contract Decision Matrix (Course/Learning Refactor)

Date: 2026-03-10  
Scope: runbook phase A from `ADHOC-COURSE-LEARNING-REFACTOR-ANALYSIS-PLAN-2026-03-10.md`.

## 1) Decision matrix (models/DTO/contracts)

## 1.1 `Course` fields

| Area | Current | Decision | Contract impact | Migration impact |
|---|---|---|---|---|
| `Course.deadlineAt` | present in model/DB | **Remove** | No direct DTO field now, internal logic simplification | drop `courses.deadline_at` |
| `Course.allowContinueAfterFail` | present in model + DTO (`CreateCourseRequest`, `CourseDto`) | **Remove** | Admin course payload changes (field removed) | drop `courses.allow_continue_after_fail` |
| `Course.keepAccessAfterDeadline` | present in model + DTO | **Remove** | Admin course payload changes (field removed) | drop `courses.keep_access_after_deadline` |
| `Course.blockAfterDeadline` | present | **Keep as configurable (no new logic)** | keep DTO fields | keep column |
| `Course.includeInOverallStats` | present | **Keep as configurable (no new logic)** | keep DTO fields | keep column |
| `Course.passingThresholdPercent` | present | **Keep (course-level setting)** | keep DTO fields | keep column |

Notes:
- Files touched later in implementation: `model/Course.java`, `dto/course/CreateCourseRequest.java`, `dto/course/CourseDto.java`, `service/CourseService.java`, `mapper/CourseMapper.java`, integration tests with course payload assertions.

## 1.2 Lesson hierarchy (`lessonType` as single source of truth)

| Area | Current | Decision | Contract impact | Migration impact |
|---|---|---|---|---|
| `TheoryLesson.contentType` | present (`theory_content_type`) | **Remove** | theory lesson DTO contract must stop exposing/accepting `contentType` | drop `lessons.theory_content_type` |
| `TheoryLesson.syncLessonType()` | present | **Remove** | none external; internal behavior becomes explicit in service/validation | none |
| `PracticeLesson.syncLessonType()` | present | **Remove after service-level guard is final** | none external | none |
| `LessonType` ownership | split with sync methods | **Only `lessonType` is authoritative** | theory create/update should derive behavior from `lessonType` only | none |

Rationale:
- Current sync methods hide invariants and can silently rewrite values; with explicit service validation (`CourseService`) this is unnecessary and complicates reasoning.

## 1.3 `PracticeQuestion`

| Area | Current | Decision | Contract impact | Migration impact |
|---|---|---|---|---|
| `questionType` | nullable in model/DB | **Make not-null** | no change in request payload semantics (already required by validation) | set `practice_questions.question_type` NOT NULL (+ data cleanup precheck) |
| `options` / `correctAnswers` | nullable | **Keep nullable in DB; normalize to empty-list in service/mapper boundaries when read** | stable external API; fewer null checks in code | optional none |

## 1.4 `LessonSubmission` status model and completion

## Evaluated options

1. `status + completed` (current)  
2. `status(terminal flag) + derived completed`

## Criteria and result

| Criterion | Current (`status+completed`) | Derived completion | Result |
|---|---|---|---|
| Query simplicity (`countDistinctCompletedLessons`, stop-lesson checks) | strong | requires status lists/joins | current wins |
| Backward compatibility in service/tests | strong | medium/low | current wins |
| Risk of inconsistent writes | medium | lower | derived wins |
| Net refactor cost now | low | high | current wins |

Decision: **keep current `status + completed` model for first refactor wave**.

Additional fixed constraints:
- `reviewedByAdminId` / `reviewedAt` stay unchanged.

## 1.5 `QuestionProgressJsonConverter`

Observation:
- `QuestionProgressJsonConverter` exists but is not referenced by entity mapping; `LessonSubmission.questionProgress` is mapped via `@JdbcTypeCode(SqlTypes.JSON)` directly.

Decision:
- **Mark as removable dead code** (delete in implementation phase after quick compile check).

---

## 2) Migration consequences (Phase A output)

## Required DB migration set (planned)

1. `courses`:
   - drop columns: `deadline_at`, `allow_continue_after_fail`, `keep_access_after_deadline`.
2. `lessons`:
   - drop column: `theory_content_type`.
3. `practice_questions`:
   - data precheck/fix for null `question_type`;
   - set `question_type` to `NOT NULL`.
4. No DB change for `options_raw`/`correct_answers_raw` required (nullable allowed).

## Contract migration implications

- Admin course DTOs/requests remove:
  - `allowContinueAfterFail`,
  - `keepAccessAfterDeadline`.
- Theory lesson DTOs/requests remove:
  - `contentType`/`theoryContentType` field flow.
- Keep key learner APIs stable (`StudentController` course/lesson/submit/review/stats endpoints).

---

## 3) Phase A exit status

Phase A runbook items are completed:
- model/DTO decision matrix fixed,
- migration implications fixed,
- status vs completed criteria/decision fixed,
- converter keep/remove decision fixed.

---

## 4) Implementation status update — Wave 0 execution (2026-03-10)

Status: **EXECUTED (REF-CM-01 baseline completed)**.

Implemented in codebase:

1. `Course` domain/DTO alignment
   - Removed from domain model: `Course.deadlineAt`, `Course.allowContinueAfterFail`, `Course.keepAccessAfterDeadline`.
   - Removed from admin DTO contracts:
     - `CreateCourseRequest.allowContinueAfterFail`,
     - `CreateCourseRequest.keepAccessAfterDeadline`,
     - `CourseDto.allowContinueAfterFail`,
     - `CourseDto.keepAccessAfterDeadline`.
   - `CourseService` updated to stop reading/writing removed fields while keeping:
     - `blockAfterDeadline`,
     - `includeInOverallStats`,
     - `passingThresholdPercent`.

2. Lesson hierarchy alignment (`lessonType` single source of truth)
   - Removed `TheoryLesson.contentType` field.
   - Removed `TheoryLesson.syncLessonType()` lifecycle method.
   - Removed `PracticeLesson.syncLessonType()` lifecycle method.
   - Theory lesson DTO flow switched from content-type-driven contract to `lessonType`:
     - `CreateTheoryLessonRequest`: `contentType` -> `lessonType`,
     - `UpdateTheoryLessonRequest`: `contentType` -> `lessonType`,
     - `LessonDto`: removed `theoryContentType`.
   - `CourseService` updated:
     - explicit validation that theory create/update uses theory `lessonType`,
     - explicit `lessonType` assignment in theory patch flow,
     - preserved PDF file existence + replacement lifecycle checks under new `lessonType` flow.
   - `LessonMapper` updated to stop exposing removed theory content-type field.

3. `PracticeQuestion.questionType` non-null enforcement
   - Domain model updated to `@Column(nullable = false)` for `questionType`.

4. Dead code removal
   - Removed unused `QuestionProgressJsonConverter` class.
   - Removed obsolete `TheoryContentType` enum.

5. DB migration for Wave 0
   - Added `V4__course_learning_wave0_domain_contract_alignment.sql` with:
     - `courses`: drop columns `deadline_at`, `allow_continue_after_fail`, `keep_access_after_deadline`,
     - `lessons`: drop column `theory_content_type`,
     - `practice_questions`: null precheck/fix + `question_type` set `NOT NULL`.

6. Verification
   - Regression gate test suite executed successfully:
     - `CourseLessonCrudIntegrationTest`,
     - `Task05SubmitFlowIntegrationTest`,
     - `Task06ReviewFlowIntegrationTest`,
     - `Task07StatisticsAndLearnerSummaryIntegrationTest`,
     - `Task08LearnerAnswersVisibilityIntegrationTest`,
     - `ProgramManagementIntegrationTest`.
   - Result: **BUILD SUCCESS**, tests run: 33, failures: 0, errors: 0.
   - Additional compile check after dead-code deletion:
     - `mvn -f monolith-mvp/pom.xml -DskipTests compile` -> **BUILD SUCCESS**.

---

## 5) Implementation status update — Wave 3 execution (2026-03-12)

Status: **EXECUTED (REF-CM-05 baseline completed)**.

Implemented in codebase:

1. Program integration contract alignment
   - `ProgramService` keeps Program -> Course enrollment propagation via `CourseEnrollmentPort` (stable integration boundary).
   - Program availability and propagation logic unified through shared resolver flow:
     - added `resolveProgramCourseStatesForUser(...)`,
     - reused by both `toProgramDto(...)` and `ensureProgramCourseEnrollmentsForUser(...)`.

2. Program two-lists enrollment contract (parity with course assignments)
   - Added service read-model method:
     - `ProgramService.getProgramEnrollmentLists(programId)` -> `UserInNotInListsDto`.
   - Added admin endpoint:
     - `GET /api/v1/admin/courses/programs/{programId}/assign`.
   - Contract now mirrors existing course two-lists assignment pattern (`in` / `notIn`).

3. Idempotent propagation and assignment consistency hardening
   - Added batch enrollment preload for program-course states:
     - `EnrollmentRepository.findByUserIdAndCourseIdIn(...)`.
   - Stabilized direct/group assignment interplay:
     - direct unassign keeps `ProgramEnrollment` when user is still assigned through any program group;
     - group unassign and membership-removal flows now also preserve enrollment when other assigned groups still exist;
     - assign flows save `ProgramEnrollment` only when it is newly created.

4. Repository read-model support for wave flows
   - `ProgramEnrollmentRepository.findByUserId(...)` and `findByProgramId(...)` moved to `join fetch` user/program reads to avoid lazy/N+1 drift in wave path.

5. Verification
   - Compile gate:
     - `mvn -f monolith-mvp/pom.xml -DskipTests compile` -> **BUILD SUCCESS**.
   - Program integration regression gate:
     - `mvn -f monolith-mvp/pom.xml -Dtest=ProgramManagementIntegrationTest test` -> **BUILD SUCCESS**.
   - `ProgramManagementIntegrationTest` expanded and passed with wave-3 scenarios:
     - two-lists program enrollment API behavior,
     - preserving enrollment on direct unassign while group assignment remains,
     - cleanup after group assignment removal.
