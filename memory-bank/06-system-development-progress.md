# System development progress

## 2026-03-18 — ADHOC full backend logical E2E scenario (without stats/tests)

- Prepared a full cross-module **logical** backend validation scenario artifact:
  - `memory-bank/task-artifacts/ADHOC-FULL-BACKEND-LOGICAL-E2E-SCENARIO-2026-03-18.md`.
- Scope decisions fixed in the scenario:
  - includes CRUD for users/groups/sections/courses/lessons/programs,
  - includes assignments/enrollments matrix (`user/group/course/program`),
  - includes role model checks (`401/403/200`),
  - includes learner flow (theory/practice/open-review/program progression),
  - includes idempotency and cross-module side-effect validation,
  - **explicitly excludes** statistics and test-suite execution for this run.

- Scenario structure was aligned to current API contracts (not legacy payloads), including:
  - group-centric operations via `ids` payloads,
  - program two-lists operations via `idsIn/idsNotIn`.

## 2026-03-18 — User SNILS field propagation across domain and DTO contracts

- Added `snils` to user domain and API contracts:
  - `AppUser.snils` in persistence model;
  - `UserDto.snils` in read model;
  - `CreateUserRequest.snils` and `UpdateUserRequest.snils` in write contracts.
- Wired SNILS handling in `UserService` create/update/profile-update flows and preserved the field in student-visible profile shaping.
- Added Flyway migration:
  - `V5__add_snils_to_users.sql` (`users.snils varchar(255)`).
- Added integration test coverage for SNILS persistence in users validation suite:
  - `UsersControllerValidationIntegrationTest.createUser_shouldPersistSnils`.
- Verification status:
  - module compilation succeeds (`mvn -f monolith-mvp/pom.xml -DskipTests compile`);
  - targeted test run is currently blocked by pre-existing unrelated application context startup error due duplicate `DELETE /api/v1/admin/groups/{groupId}/courses/assign` mapping in `UsersController`.

## 2026-03-05 — Files storage URL strategy alignment

- Unified files strategy for monolith backend:
  - `POST /api/v1/files/upload` returns **relative path** with `/files/` prefix.
  - Relative paths are normalized and persisted for user avatars/course covers.
  - API read models return **relative path** with `/files/` prefix (frontend composes full host URL).
- Implemented replacement lifecycle for avatars/covers:
  - old file is deleted on update when path changes;
  - file is deleted on entity removal (user/course deletion).
- Enabled direct static file serving from backend under `/files/**`:
  - added `StorageWebConfig` resource handler bound to `APP_STORAGE_ROOT_DIR`;
  - allowed unauthenticated read access to `/files/**` in security config.
- Runtime/env/docker alignment:
  - aligned to single storage config key `APP_STORAGE_ROOT_DIR` in compose/env examples.

## 2026-03-05 — Storage config simplification (single logic)

- Removed obsolete split storage settings:
  - dropped `app.storage.user-avatar-dir` and `app.storage.public-base-url` from `application.yml`;
  - dropped `APP_STORAGE_USER_AVATAR_DIR` and `APP_STORAGE_PUBLIC_BASE_URL` from env/compose examples.
- Unified avatar upload path strategy with generic file upload strategy:
  - `UserService.updateUserAvatar(...)` now stores files via `fileStorageService.store(file, "uploads")`.
- Kept lifecycle behavior unchanged:
  - old avatar/cover files are deleted on replacement;
  - avatar/cover files are deleted on entity removal.

## 2026-03-07 — Lessons DTO alignment and explicit theory completion

- Unified practical question DTO naming to `position` for create/update/get and learner payloads.
- Added lesson `position` support in create/update lesson DTOs and wired position insert/reorder behavior in lesson service.
- Removed implicit theory completion on lesson read; introduced explicit endpoint `POST /api/v1/student/lessons/{lessonId}/complete-theory`.
- Added configurable files framing policy for split frontend/backend deployment:
  - `app.storage.csp-frame-ancestors` / `APP_STORAGE_CSP_FRAME_ANCESTORS`;
  - applied `Content-Security-Policy: frame-ancestors ...` for `/files/**` responses via MVC interceptor;
  - supports multiple allowed embedding origins via comma-separated env/property value.

## 2026-03-07 — Clickjacking hardening for mixed framing requirements

- Reduced clickjacking risk after introducing `/files/**` embedding support:
  - enabled `X-Frame-Options: SAMEORIGIN` for all endpoints by default;
  - excluded only `/files/**` from `X-Frame-Options` to avoid conflict with cross-origin embedding use case;
  - kept `Content-Security-Policy: frame-ancestors ...` on `/files/**` as the explicit allow-list mechanism.

## 2026-03-07 — Learner course flow progress DTO and next-lesson API

- Implemented learner-facing progress enrichment without changing `CourseDto`:
  - extended `CourseLearnerDto` with `completionPercent`, `completedLessons`, `remainingLessons`, `totalLessons`, `courseCompleted`, `nextLessonId`;
  - extended `LearnerLessonSummaryDto` with `passed`, `pointsAwarded`, `blocked`, `blockReason`.
- Added progress/blocking calculation in `CourseService.getCourseForLearner(...)`:
  - lesson passed rule: at least one submission with `passed=true`;
  - `pointsAwarded` is max points among learner submissions for lesson;
  - `completionPercent` uses floor division, and for zero-lesson course returns `100`;
  - `nextLessonId` is first lesson that is not passed and not blocked.
- Added block reason mapping for learner lesson summary:
  - `PREVIOUS_LESSON_NOT_PASSED` when strict order (`lessonsFreeOrder=false`) is violated;
  - `STOP_LESSON_BLOCK` when an earlier stop-lesson is not passed.
- Added learner endpoint:
  - `GET /api/v1/student/courses/{courseId}/lessons/next` returning `LearnerLessonDto`;
  - implemented via `LearningService.getNextLessonForLearner(...)`, reusing existing access checks through `getLessonForLearner(...)`.
- Added integration coverage in `CourseLessonCrudIntegrationTest`:
  - course progress fields and per-lesson blocked/passed/points;
  - next-lesson endpoint behavior before progress, after partial progress, and after full completion.

## 2026-03-07 — Learner flow simplification: single source for next lesson

- Optimized learner flow by removing duplicated `nextLessonId` calculation from course details payload:
  - removed `nextLessonId` from `CourseLearnerDto`;
  - updated integration test expectations accordingly.
- Refactored `CourseService.getCourseForLearner(...)` into smaller focused methods:
  - `loadSubmissionsByLessonId(...)`,
  - `resolvePassedLessonIds(...)`,
  - `buildLearnerLessonSummaries(...)`,
  - `toCourseLearnerDto(...)`.
- Added dedicated `CourseService.findNextLessonIdForLearner(...)` and switched `LearningService.getNextLessonForLearner(...)` to use it.
- Resource-impact improvement:
  - `GET /api/v1/student/courses/{courseId}` no longer computes next lesson id;
  - next lesson is computed only on `GET /api/v1/student/courses/{courseId}/lessons/next`, reducing unnecessary work for course details requests.

## 2026-03-08 — Learner lesson view policy for already passed lessons

- Updated learner lesson read behavior (`GET /api/v1/student/lessons/{lessonId}`):
  - if lesson is already passed by student, access is allowed in read mode even when regular progression constraints would block new attempts;
  - this bypass applies to strict order, stop-lesson, and course deadline checks only for already passed lessons.
- Progression constraints remain unchanged for not-passed lessons and for completion/submission actions:
  - still enforced for `complete-theory`, `submit-practice`, and next lesson flow.
- Added integration test coverage:
  - `learner_should_be_able_to_view_already_passed_lesson_after_deadline` verifies:
    - passed lesson remains viewable after deadline;
    - not-passed lesson is still blocked after deadline.

## 2026-03-08 — Practice submission contract hardening and multi-open review flow

- Unified student practice submission contract to a single payload field:
  - `PracticeSubmissionRequest` now contains only `questionAnswers` (`questionIndex -> List<String>`).
  - Removed legacy `openAnswer` and `selectedAnswers` usage from backend flow.
- Hardened submission validation in `LearningService.submitPractice(...)`:
  - `questionAnswers` is mandatory;
  - payload must contain answers for all lesson questions and only for them;
  - each question must have non-empty answer list;
  - `SINGLE_CHOICE` must contain exactly one selected answer;
  - `OPEN_ANSWER` must contain exactly one textual answer.
- Enforced strict lesson/question type consistency:
  - `PRACTICE_OPEN_ANSWER` accepts only `OPEN_ANSWER` questions;
  - `PRACTICE_TEST` accepts only test question types;
  - mixed test/open question pools in one practice lesson are rejected at lesson create/update validation.
- Extended open-practice workflow to support multiple open questions in one lesson:
  - student submits all open-question answers via full `questionAnswers` map;
  - answers are stored as serialized per-question map in single submission;
  - admin review remains submission-level (review of whole lesson attempt).
- Updated integration tests accordingly:
  - replaced old `openAnswer`/`selectedAnswers` payloads with `questionAnswers`;
  - added dedicated test `open_practice_with_multiple_questions_should_require_full_question_answers_and_be_reviewed_as_single_submission`.
- Verification:
  - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test -DskipITs`
  - result: **BUILD SUCCESS**, tests run: 17, failures: 0, errors: 0.

## 2026-03-08 — ADHOC open-lesson rework: TASK-01 domain model/contracts freeze

- Created formal TASK-01 artifact:
  - `memory-bank/task-artifacts/ADHOC-OPEN-LESSON-REWORK-TASK-01-DOMAIN-CONTRACTS-2026-03-08.md`.
- Fixed unambiguous target model before code implementation:
  - lesson-level statuses: `PENDING_REVIEW`, `REWORK`, `COMPLETE`, `INCOMPLETE`;
  - question-level open review statuses: `PENDING_REVIEW`, `ACCEPTED`, `REWORK`, `REJECTED`;
  - explicit rule that `REJECTED` is question-level only (never lesson-level).
- Formalized `MULTIPLE_CHOICE` partial scoring rule:
  - `PARTIAL` only when `wrongSelected <= 1` and `missedCorrect <= 1`,
  - otherwise `ZERO`, including confirmed edge case with too many missed correct options.
- Formalized finalization/lock rules:
  - lesson is locked by `completed=true`;
  - finalized lesson cannot be edited by student or reviewer;
  - finalized open submissions are excluded from pending-review list.
- Fixed payload contract direction for next tasks:
  - both student submit and admin review are keyed by `questionIndex` (not `questionId`);
  - open review request contains decisions for all lesson questions in a single request.

## 2026-03-08 — ADHOC open-lesson rework: TASK-02 lesson progress schema migration

- Added Flyway migration:
  - `monolith-mvp/src/main/resources/db/migration/V2__open_lesson_rework_single_submission_schema.sql`.
- `lesson_submissions` migrated towards single-record progress model by adding:
  - `completed boolean not null default false`;
  - `question_progress_json json` for question-level progress payload;
  - `attempt_counter integer not null default 0`.
- Backfill for existing rows:
  - `completed=true` for statuses `COMPLETE` / `INCOMPLETE`, otherwise `false`.
- Added unique index for single-submission flow:
  - `uk_lesson_submissions_lesson_student` on `(lesson_id, student_id)`.
- Removed obsolete old-flow tables at schema level:
  - `submission_question_reviews` (drop-if-exists);
  - `lesson_submission_status_history` (drop-if-exists).
- Migration policy decision (explicit):
  - no automatic deduplication is performed before unique index creation;
  - migration is allowed to fail on duplicate historical submissions, as agreed.

## 2026-03-08 — ADHOC open-lesson rework: TASK-03 domain/entity/repository alignment

- Updated lesson submission domain model for single-record flow:
  - `LessonSubmission` now contains lifecycle field `completed`;
  - added `questionProgress` mapped to `question_progress_json` via JPA converter;
  - added `attemptCounter` field for attempt tracking in same record;
  - kept `passed`/`pointsAwarded` for compatibility with current dependent services until next tasks.
- Added new question-level domain types:
  - `QuestionPointsType` (`FULL`, `PARTIAL`, `ZERO`),
  - `OpenReviewStatus` (`PENDING_REVIEW`, `ACCEPTED`, `REWORK`, `REJECTED`),
  - `QuestionProgress` (answer/points/review snapshot per question),
  - `QuestionProgressJsonConverter` for JSON serialization/deserialization.
- Removed obsolete status history layer:
  - deleted `LessonSubmissionStatusHistory` model,
  - deleted `LessonSubmissionStatusHistoryRepository`,
  - removed `saveSubmissionStatusHistory(...)` usage from `LearningService`.
- Updated enums/repository contracts for new flow direction:
  - removed obsolete lesson-level `SubmissionStatus.ACCEPTED`;
  - review transition now uses `COMPLETE` instead of `ACCEPTED`;
  - `LessonSubmissionRepository` enriched with single-record methods:
    - `findByStudentIdAndLessonId(...)`,
    - `findWithLockingByStudentIdAndLessonId(...)`.
- Verification:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` → `BUILD SUCCESS`.

## 2026-03-08 — ADHOC open-lesson rework: TASK-04 DTO/API contracts for learning/review

- Updated learning DTO contracts to match TASK-01/TASK-04 target API shape:
  - `PracticeSubmissionRequest` kept as `questionAnswers` (`questionIndex -> answers[]`) with aligned Swagger description.
  - `ReviewOpenSubmissionRequest` replaced old booleans (`passed/partialPoints/toNextReview/comment`) with map contract:
    - `questionReviews: Map<Integer, ReviewQuestionDecisionDto>`.
  - Added `ReviewQuestionDecisionDto` for per-question review decision:
    - `submissionStatus` (`OpenReviewStatus`), `awardedPoints`, `reviewComment`.
  - Expanded `PendingSubmissionDto` for admin pending list contract:
    - `submissionId`, `lessonId`, `lessonTitle`, `courseId`, `courseTitle`, `studentId`, `studentFullname`, `submittedAt`, `attempt`.
  - Added `PendingSubmissionQuestionDto` for detailed question-level pending review payload.

- Updated admin API endpoints in `ProgressController`:
  - kept `GET /api/v1/admin/progress/reviews/pending` as lesson-level list endpoint;
  - added `GET /api/v1/admin/progress/reviews/pending/{submissionId}` for full question-level review details;
  - updated Swagger summaries/descriptions to match review-by-all-questions flow.

- Updated `LearningService` contract handling for new DTO/API behavior:
  - `getPendingReviews()` now returns expanded pending list payload (including course and attempt fields).
  - Added `getPendingReviewQuestions(submissionId)`:
    - loads open lesson questions,
    - merges question text/hints/fullPoints with current answer/progress/review state,
    - blocks finalized submissions and non-open lessons.
  - Reworked `reviewOpenSubmission(...)` to consume per-question decisions for all questions:
    - validates full question coverage by `questionIndex`,
    - validates status/awardedPoints rules (`REWORK`/`REJECTED` => `0`, `ACCEPTED` in `0..fullPoints`),
    - stores question-level review state into `questionProgress`,
    - aggregates lesson status (`REWORK` vs final `COMPLETE/INCOMPLETE`) and `completed`.

- Aligned dependent compilation break from prior `passed -> completed` rename:
  - updated remaining usages to repository methods `...Completed...` and `countDistinctCompletedLessons(...)` in `LearningService` and `StatisticsService`.

- Verification:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` → `BUILD SUCCESS`.

## 2026-03-08 — ADHOC open-lesson rework: answerRaw removal and questionProgress-only flow

- Confirmed migration strategy with product owner: **destructive**, no backfill from `answer_raw`.
- Switched lesson submission model to questionProgress-only storage:
  - removed `answerRaw` field from `LessonSubmission` entity;
  - updated Flyway V2 migration to drop legacy column:
    - `alter table lesson_submissions drop column if exists answer_raw;`.

- Refactored `LearningService` to stop using any raw-answer serialization format:
  - removed `serializeAnswersByQuestion(...)` and `deserializeAnswersByQuestion(...)` helpers;
  - submit flow now writes student answers directly into `questionProgress`:
    - open practice submit writes/updates per-question entries with `answers`, `reviewStatus=PENDING_REVIEW` for new/REWORK questions;
    - test practice submit writes per-question `answers`, `pointsType`, `awardedPoints`.
  - pending review details are now built only from `questionProgress`.
  - review flow updates existing question progress entries directly, without fallback to `answer_raw`.

- Additional alignment done during refactor:
  - test scoring helper now reuses `QuestionPointsType` resolution for consistent per-question state.

- Verification:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` → `BUILD SUCCESS`.

- Noted behavioral caveat for current implementation:
  - in open rework resubmit, only questions with previous `REWORK` (or missing/pending status) are reset to `PENDING_REVIEW` and have answers replaced;
  - previously accepted/rejected questions keep prior reviewer decision and answer snapshot in questionProgress.

## 2026-03-09 — ADHOC open-lesson rework: TASK-05 submit flow closure

- Closed remaining TASK-05 gaps in `LearningService.submitPractice(...)`:
  - test practice path switched to true single-record upsert via `findWithLockingByStudentIdAndLessonId(...)`;
  - explicit finalized submission guard added (`completed=true` blocks any new submit);
  - test lesson awarded points switched to binary model (`lesson.fullPoints` or `0`), while per-question points remain in `questionProgress` only;
  - attempt counting for both open and test paths now increments in the same submission record.

- Implemented strict MULTIPLE_CHOICE partial rule per TASK-01 contract:
  - `PARTIAL` only when `wrongSelected <= 1` and `missedCorrect <= 1`;
  - otherwise `ZERO`.

- Updated attempt/time-limit checks to align with single-submission model:
  - attempt limit now uses `attemptCounter` from the single submission record;
  - practice time-limit baseline now reads from the same record (`submittedAt`) with null-safe handling.

- Added focused integration coverage for TASK-05:
  - new `Task05SubmitFlowIntegrationTest` verifies:
    - test submit upsert semantics and finalized-submit rejection,
    - strict MULTIPLE_CHOICE partial vs zero behavior through API outcomes.

- Target-flow converter policy:
  - removed temporary legacy parsing fallback from `QuestionProgressJsonConverter`;
  - converter now deserializes only the target payload shape (JSON array of `QuestionProgress`).

- Test alignment for target converter flow:
  - `Task05SubmitFlowIntegrationTest` now aligns H2 column type for `question_progress_json` before each test (`varchar`) to keep the test runtime consistent with converter target contract and avoid H2 JSON-wrapper behavior.

- Build/test verification:
  - `mvn -f monolith-mvp/pom.xml -Dtest=Task05SubmitFlowIntegrationTest test` → **BUILD SUCCESS**.

## 2026-03-09 — ADHOC open-lesson rework: TASK-06 review flow closure

- Closed TASK-06 review-flow gaps in `LearningService`:
  - `getPendingReviews()` now returns both `PENDING_REVIEW` and `REWORK` submissions for reviewer scope;
  - added explicit empty-scope fast return when reviewer has no assigned courses;
  - added explicit reviewable-status guard for pending-details and review actions (`PENDING_REVIEW`/`REWORK` only);
  - kept finalized submission lock (`completed=true` blocks pending-details and re-review).

- Fixed open-lesson type validation robustness for JPA proxies:
  - switched review-path lesson type checks to `Hibernate.unproxy(...)` before `PracticeLesson`/open-type checks;
  - added open-lesson predicate that accepts canonical `PRACTICE_OPEN_ANSWER` and all-open-question practice lessons.

- Repository update for new pending query semantics:
  - added `findAllByStatusInAndLessonCourseIdIn(...)` in `LessonSubmissionRepository`.

- Updated integration coverage for TASK-06:
  - adjusted `Task06ReviewFlowIntegrationTest` final review payload/assertions to match lesson-level binary awarding on finalized open review (`fullPoints` or `0`),
  - verified finalized open lesson is excluded from pending and cannot be reviewed again.

- Build/test verification:
  - `mvn -f monolith-mvp/pom.xml -Dtest=Task06ReviewFlowIntegrationTest test -q` → **BUILD SUCCESS**;
  - `mvn -f monolith-mvp/pom.xml -Dtest=Task05SubmitFlowIntegrationTest,Task06ReviewFlowIntegrationTest test -q` → **BUILD SUCCESS**.

## 2026-03-09 — ADHOC open-lesson rework: TASK-06 follow-up adjustments

- Removed Hibernate-specific unproxy usage from review flow (`LearningService`):
  - replaced `Hibernate.unproxy(...)` in review validation with explicit lesson loading by id.

- Refined explicit loading approach to preserve open-practice type safety:
  - added `LessonRepository.findPracticeLessonById(...)` (typed query to `PracticeLesson`);
  - review validators now load `PracticeLesson` explicitly and then apply `isOpenPracticeLesson(...)` check.

- Brought pending-list semantics back to agreed contract:
  - pending list remains only `PENDING_REVIEW` in `getPendingReviews()`;
  - removed no-longer-needed repository method `findAllByStatusInAndLessonCourseIdIn(...)`.

- Updated TASK-06 integration expectation accordingly:
  - `Task06ReviewFlowIntegrationTest` now asserts empty `/reviews/pending` after submission becomes `REWORK`.

- Verification:
  - `mvn -f monolith-mvp/pom.xml -Dtest=Task06ReviewFlowIntegrationTest test -q` → success (exit code 0);
  - `mvn -f monolith-mvp/pom.xml -Dtest=Task05SubmitFlowIntegrationTest test -q` → success (exit code 0).

## 2026-03-09 — ADHOC open-lesson rework: TASK-07 statistics/learner-summary alignment

- Aligned learner summary logic in `CourseService` to single-submission model:
  - replaced lesson submissions aggregation `Map<Long, List<LessonSubmission>>` with `Map<Long, LessonSubmission>`;
  - removed best-of-many semantics (`max(points)` and `anyMatch(completed)`);
  - `passed` and `pointsAwarded` are now derived directly from the single submission record per lesson.

- Aligned `StatisticsService` calculations to single-submission model:
  - `earnedPoints(...)` no longer computes per-lesson max across attempts; now sums current `pointsAwarded` values from single lesson submissions;
  - `retakes(...)` no longer uses `count(submissions)-1`; now uses `attemptCounter` (`sum(max(0, attemptCounter - 1))`).

- Added integration coverage for TASK-07:
  - new `Task07StatisticsAndLearnerSummaryIntegrationTest` verifies:
    - learner course summary (`passed`, `pointsAwarded`) after resubmit in one submission record,
    - student/admin stats `earnedPoints` consistency,
    - course CSV `Пересдач` sourced from `attemptCounter` and `Баллов` from current submission points.

- Verification:
  - `mvn -f monolith-mvp/pom.xml -Dtest=Task07StatisticsAndLearnerSummaryIntegrationTest test` → **BUILD SUCCESS**.

## 2026-03-09 — Learner practice answers visibility and scoring exposure by lesson flags

- Extended learner lesson DTO contract for practice visibility settings:
  - `LearnerLessonDto` now includes:
    - `showQuestionStatus`,
    - `showCorrectAnswersAfterCompletion`.

- Extended learner practice-question DTO contract:
  - `LearnerPracticeQuestionDto` now includes:
    - `correctAnswers`,
    - `pointsType`,
    - `awardedPoints`.

- Updated `LearningService.getLessonForLearner(...)` to use existing `PracticeLesson` flags as source of truth:
  - always returns learner submitted answers (`userAnswers`) from `questionProgress`;
  - question status/points (`status`, `pointsType`, `awardedPoints`) are returned only when `showQuestionStatus=true`;
  - correct answers are returned only when:
    - `showCorrectAnswersAfterCompletion=true`, and
    - learner submission exists and is finalized (`completed=true`).

- DTO design decision for this step:
  - no additional feature flags or schema changes were introduced;
  - visibility behavior is fully driven by already existing lesson model flags.

- Build verification:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` → **BUILD SUCCESS**.

## 2026-03-09 — Learner DTO exposure refinement (current step)

- Finalized learner-facing DTO payload for practice questions to explicitly expose learner result details:
  - `LearnerPracticeQuestionDto` now carries `userAnswers`, `correctAnswers`, `status`, `pointsType`, `awardedPoints`.
- Added lesson-level visibility flags to learner lesson response:
  - `LearnerLessonDto.showQuestionStatus`,
  - `LearnerLessonDto.showCorrectAnswersAfterCompletion`.
- `LearningService.getLessonForLearner(...)` now maps `QuestionProgress.pointsType` into learner DTO and keeps visibility policy based on existing lesson flags.
- Tests were intentionally not updated in this step (deferred by product-owner request); module compilation was validated.

## 2026-03-09 — Admin progress reset endpoint test coverage

- Added integration coverage for admin endpoint `POST /api/v1/admin/progress/users/reset` in:
  - `CourseLessonCrudIntegrationTest.admin_progress_reset_endpoint_should_clear_student_course_progress`.
- Test verifies full reset behavior for a student/course pair:
  - existing lesson submissions are deleted;
  - enrollment progress markers are cleared (`startedAt`, `completedAt` become `null`);
  - API response message equals `Student progress has been cleared`.

- Verification:
  - `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest#admin_progress_reset_endpoint_should_clear_student_course_progress test` → **BUILD SUCCESS**.

## 2026-03-10 — ADHOC analysis for course/learning refactor scope

- Performed targeted architecture/code analysis for the most complex domain area: course management + learning flow.
- Fixed analysis and refactor plan in separate artifact:
  - `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-ANALYSIS-PLAN-2026-03-10.md`.
- Key decisions prepared for upcoming implementation phase:
  - refactor should start from domain-flag/DTO contract normalization,
  - then split oversized `CourseService` and `LearningService` into focused use-case services,
  - in parallel reduce DB roundtrips in statistics/reporting and clean repository/API technical debt.
- Captured open requirement questions (course flags behavior, DTO compatibility strategy, ProgramService scope, implementation priority, JSON mapping strategy) to resolve before coding.

## 2026-03-10 — ADHOC requirements fixation for next refactor-analysis wave

- Updated analysis artifact according to explicit product-owner decisions:
  - `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-ANALYSIS-PLAN-2026-03-10.md`.
- Fixed mandatory scope decisions before implementation:
  - remove course fields `deadlineAt`, `allowContinueAfterFail`, `keepAccessAfterDeadline`;
  - keep `blockAfterDeadline`, `includeInOverallStats`, `passingThresholdPercent` as configurable but still non-functional for now;
  - make `lessonType` the single source of truth, remove `TheoryLesson.contentType`, remove `TheoryLesson.syncLessonType()`, and separately validate necessity of `PracticeLesson.syncLessonType()`;
  - make `PracticeQuestion.questionType` non-null;
  - allow optional simplification `null -> empty list` for `options/correctAnswers` only if it clearly reduces complexity;
  - keep `reviewedByAdminId/reviewedAt`; evaluate `SubmissionStatus` terminal-flag approach pragmatically;
  - allow removing `QuestionProgressJsonConverter` if confirmed redundant for current mapping.
- Fixed process constraints for next phase:
  - no coding changes until this fixation is approved;
  - next step is mandatory subagent-driven architecture analysis for `CourseService`, `LearningService`, `StatisticsService`, repository aggregation layer, and `ProgramService` integration boundaries;
  - priority is service decomposition with performance-aware boundaries (avoid over-fragmentation that increases DB roundtrips).

## 2026-03-10 — ADHOC course/learning refactor analysis execution (runbook phase A/B/C)

- Executed analysis runbook from:
  - `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-ANALYSIS-PLAN-2026-03-10.md`.

- Produced Phase A artifact (domain/contracts + migration decisions):
  - `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-PHASE-A-DOMAIN-CONTRACT-DECISION-MATRIX-2026-03-10.md`.
  - fixed decisions:
    - remove `Course.deadlineAt`, `allowContinueAfterFail`, `keepAccessAfterDeadline`;
    - keep configurable `blockAfterDeadline`, `includeInOverallStats`, `passingThresholdPercent`;
    - use `lessonType` as single source of truth (remove `TheoryLesson.contentType` and `TheoryLesson.syncLessonType()` in implementation wave);
    - make `PracticeQuestion.questionType` non-null;
    - keep `LessonSubmission` model as `status + completed` for first wave;
    - mark `QuestionProgressJsonConverter` as removable dead code (not used by current mapping).

- Executed mandatory Phase B via parallel `backend-architect` subagents and consolidated outputs into:
  - `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-PHASE-B-SUBAGENT-ARCH-ANALYSIS-2026-03-10.md`.
  - captured:
    - target decomposition boundaries for `CourseService` and `LearningService`;
    - policy/utility centralization candidates;
    - race-condition and transactional risk points;
    - missing aggregation queries and repository-layer optimization points for statistics/reporting;
    - bounded wave-1 integration scope for `ProgramService`.

- Completed Phase C final consolidation into:
  - `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-PHASE-C-CONSOLIDATED-PLAN-2026-03-10.md`.
  - includes:
    - unified target architecture map;
    - final ordered backlog with dependencies/risks (`REF-CM-01..07`);
    - readiness criteria for transition from analysis to implementation;
    - regression test gate list for implementation waves.

- Current analysis package status:
  - **READY FOR IMPLEMENTATION PLANNING / EXECUTION**.

## 2026-03-10 — ADHOC course/learning refactor: Wave 0 (REF-CM-01) execution

- Executed Wave 0 from consolidated plan (`REF-CM-01` domain/contract alignment).

- Domain and DTO contract changes implemented:
  - removed from `Course` model and admin course payloads:
    - `deadlineAt`,
    - `allowContinueAfterFail`,
    - `keepAccessAfterDeadline`;
  - kept configurable fields unchanged:
    - `blockAfterDeadline`,
    - `includeInOverallStats`,
    - `passingThresholdPercent`.

- Lesson contract alignment implemented:
  - removed `TheoryLesson.contentType` + `TheoryLesson.syncLessonType()`;
  - removed `PracticeLesson.syncLessonType()`;
  - switched theory create/update flow to `lessonType` as the only authoritative type field;
  - removed theory content-type exposure from lesson DTO/mapper flow.

- Practice question alignment implemented:
  - enforced `PracticeQuestion.questionType` as non-null in model and DB migration.

- DB migration added:
  - `V4__course_learning_wave0_domain_contract_alignment.sql`:
    - drops `courses.deadline_at`, `courses.allow_continue_after_fail`, `courses.keep_access_after_deadline`,
    - drops `lessons.theory_content_type`,
    - performs null precheck/fix for `practice_questions.question_type` and sets `NOT NULL`.

- Dead code cleanup completed:
  - removed `QuestionProgressJsonConverter` (unused by current entity mapping),
  - removed obsolete `TheoryContentType` enum.

- Regression verification:
  - wave gate integration tests passed:
    - `CourseLessonCrudIntegrationTest`,
    - `Task05SubmitFlowIntegrationTest`,
    - `Task06ReviewFlowIntegrationTest`,
    - `Task07StatisticsAndLearnerSummaryIntegrationTest`,
    - `Task08LearnerAnswersVisibilityIntegrationTest`,
    - `ProgramManagementIntegrationTest`;
  - result: **BUILD SUCCESS**, 33 tests, 0 failures/errors;
  - post-cleanup compile check also passed (`mvn -f monolith-mvp/pom.xml -DskipTests compile`).

## 2026-03-11 — ADHOC course/learning refactor: Wave 1 (REF-CM-02/REF-CM-03) baseline closure

- Completed Wave 1 baseline from consolidated Phase C plan:
  - finalized `LearningService` as compatibility facade with delegation to:
    - `LessonAccessPolicy`,
    - `PracticeSubmissionService`,
    - `OpenReviewService`;
  - preserved learner lesson read-model assembly inside `LearningService`.

- Aligned Program/Course integration boundary to port-based contract:
  - `ProgramService` switched from direct `CourseService` enrollment calls to `CourseEnrollmentPort`;
  - `GroupService` switched from direct `CourseService` enrollment calls to `CourseEnrollmentPort`.

- Verification:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` -> **BUILD SUCCESS**;
  - regression gate executed:
    - `CourseLessonCrudIntegrationTest`,
    - `Task05SubmitFlowIntegrationTest`,
    - `Task06ReviewFlowIntegrationTest`,
    - `Task07StatisticsAndLearnerSummaryIntegrationTest`,
    - `Task08LearnerAnswersVisibilityIntegrationTest`;
  - result: **BUILD SUCCESS**, tests run: 25, failures: 0, errors: 0.

- Wave status: **WAVE 1 DONE (baseline), ready for Wave 2 (`REF-CM-04`/`REF-CM-07`)**.

## 2026-03-11 — Wave 1 follow-up: single source of truth for theory lesson type checks

- Addressed duplicated helper logic identified during Wave 1 service review:
  - duplicate `isTheoryLesson(...)` checks existed in `CourseService` and `PracticeSubmissionService`.

- Introduced a single source of truth at domain enum level:
  - `LessonType.isTheory()`;
  - `LessonType.isPractice()` (added alongside for symmetric domain API).

- Refactored services to consume unified domain predicate:
  - `CourseService` theory-count calculation now uses `lesson.getLessonType().isTheory()`;
  - `PracticeSubmissionService.completeTheoryLesson(...)` now uses `lesson.getLessonType().isTheory()`;
  - removed duplicated private helper methods.

- Verification:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` → **BUILD SUCCESS**;
  - `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest,Task05SubmitFlowIntegrationTest test` → **BUILD SUCCESS** (20 tests, 0 failures/errors).

## 2026-03-11 — Wave 1 follow-up: facade delegation cleanup after boundary migration

- Reduced residual compatibility-facade coupling after Wave 1 decomposition by switching remaining internal consumers from `CourseService` pass-through methods to focused services:
  - `EnrollmentProgressService`: `CourseService` -> `CourseLessonAdminService` for course lessons read;
  - `OpenReviewService`: `CourseService` -> `CourseAssignmentService` for reviewer-scope and review-permission checks.

- Finalized `CourseService` scope as thin public application facade for externally used course endpoints only:
  - removed obsolete delegation methods that mirrored extracted services (`CourseLessonAdminService`, `CourseLearnerReadService`, `CourseAssignmentService`) and no longer had call sites;
  - removed now-unused injected dependencies from `CourseService` (`CourseAccessPolicy`, `CourseLessonAdminService`) and cleaned unused imports;
  - kept local private `getCourseEntity(...)` helper backed by `CourseLearnerReadService` for internal CRUD methods in `CourseService`.

- Minor follow-up cleanup in learner read service facade:
  - removed unused imports in `LearningService` after previous decomposition.

- Verification:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` → **BUILD SUCCESS**;
  - `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest,Task05SubmitFlowIntegrationTest,Task06ReviewFlowIntegrationTest,Task07StatisticsAndLearnerSummaryIntegrationTest,Task08LearnerAnswersVisibilityIntegrationTest test` → **BUILD SUCCESS**, `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`.

## 2026-03-11 - Course/learning controllers: redundant controller logic cleanup

- Performed controller-layer validation for course/learning endpoints and moved non-transport logic from controllers into services.

- `CoursesController` cleanup:
  - removed overlap-validation checks and per-id iteration loops from controller methods;
  - switched to service-level orchestration methods:
    - `courseAssignmentService.updateCourseEnrollments(...)`,
    - `courseAssignmentService.updateCourseReviewers(...)`,
    - `courseAssignmentService.assignGroupsToCourse(...)`,
    - `courseAssignmentService.unassignGroupsFromCourse(...)`,
    - `programService.updateProgramUsers(...)`,
    - `programService.updateProgramGroups(...)`.

- `StudentController` cleanup:
  - removed profile composition and role-specific comment masking from controller;
  - switched to application-service methods:
    - `userService.getStudentProfile(...)`,
    - `userService.updateStudentProfile(...)`.

- Service additions introduced to host extracted logic:
  - `CourseAssignmentService`: overlap validation and list-orchestration wrappers for enrollments/reviewers/groups;
  - `ProgramService`: overlap validation wrappers for program users/groups updates;
  - `UserService`: student profile assembly and student comment masking for profile endpoints.

- Verification:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` -> **BUILD SUCCESS**;
  - `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest,Task05SubmitFlowIntegrationTest,Task06ReviewFlowIntegrationTest,Task07StatisticsAndLearnerSummaryIntegrationTest,Task08LearnerAnswersVisibilityIntegrationTest test` -> **BUILD SUCCESS**, `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`.

## 2026-03-11 — ADHOC course/learning refactor: Wave 2 (REF-CM-04/REF-CM-07) completion

- Completed Wave 2 performance/data-access scope from consolidated plan:
  - `StatisticsService` preserved as compatibility facade and switched to delegation through:
    - `StatisticsReportService`,
    - `StatisticsQueryService`,
    - `CsvReportRenderer`.

- Implemented statistics query-layer aggregation to reduce per-row roundtrips:
  - `LessonSubmissionRepository`:
    - batch points sum by `(userId, courseId)`,
    - batch completed lessons count by `(userId, courseId)`,
    - batch retakes sum by `(userId, courseId)`;
  - `LessonRepository`:
    - batch max-points sum by `courseId`,
    - batch lessons count by `courseId`;
  - `EnrollmentRepository`:
    - join-fetch read models (`findByUserIdWithUserAndCourse`, `findByCourseIdWithUserAndCourse`, `findAllWithUserAndCourse`);
  - `GroupMembershipRepository`:
    - join-fetch memberships by user set (`findByUserIdInWithGroup`).

- Repository cleanup after usage scan (`REF-CM-07`):
  - removed unused methods from `LessonSubmissionRepository`:
    - `findFirstByStudentIdAndLessonIdAndStatusOrderBySubmittedAtDesc(...)`,
    - `findFirstByStudentIdAndLessonIdOrderBySubmittedAtAsc(...)`;
  - kept `findWithLockingByStudentIdAndLessonId(...)` as actively used by submit/review critical flows.

- Added/supporting artifacts for Wave 2 context continuity:
  - `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-PHASE-C-CONSOLIDATED-PLAN-2026-03-10.md` updated with Wave 2 execution status section;
  - `memory-bank/task-artifacts/ADHOC-COURSE-LEARNING-REFACTOR-PHASE-B-SUBAGENT-ARCH-ANALYSIS-2026-03-10.md` updated with implementation feedback against B3 recommendations.

- Verification:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` -> **BUILD SUCCESS**;
  - regression gate:
    - `CourseLessonCrudIntegrationTest`,
    - `Task05SubmitFlowIntegrationTest`,
    - `Task06ReviewFlowIntegrationTest`,
    - `Task07StatisticsAndLearnerSummaryIntegrationTest`,
    - `Task08LearnerAnswersVisibilityIntegrationTest`,
    - `ProgramManagementIntegrationTest`;
  - result: **BUILD SUCCESS**, `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`.

## 2026-03-12 — ADHOC course/learning refactor: Wave 3 (REF-CM-05) completion

- Completed Wave 3 from consolidated Phase C plan (`Program integration alignment`).

- Program integration and idempotent propagation updates:
  - `ProgramService` keeps enrollment propagation via `CourseEnrollmentPort` and now uses a shared state resolver for both DTO read-model and propagation path:
    - `resolveProgramCourseStatesForUser(...)`;
  - replaced per-course enrollment lookups with batch preload:
    - `EnrollmentRepository.findByUserIdAndCourseIdIn(...)`.

- Assignment pattern parity (program two-lists contract):
  - implemented missing method for programs assignment lists:
    - `ProgramService.getProgramEnrollmentLists(programId)` returning `UserInNotInListsDto`;
  - added admin endpoint:
    - `GET /api/v1/admin/courses/programs/{programId}/assign`.

- Program enrollment consistency hardening:
  - direct unassign no longer removes `ProgramEnrollment` when user is still assigned through any program group;
  - group unassign/group-membership removal now also preserves enrollment when user remains assigned through another group;
  - save operations for program enrollments are now conditional (persist only newly created enrollments).

- Repository read-path alignment:
  - `ProgramEnrollmentRepository.findByUserId(...)` and `findByProgramId(...)` switched to join-fetch user/program read-model queries.

- Integration coverage updates:
  - `ProgramManagementIntegrationTest` expanded with wave-3 scenarios:
    - two-lists program assignment API behavior,
    - preserving enrollment on direct unassign while group assignment exists,
    - removing enrollment after group unassignment.

- Verification:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` -> **BUILD SUCCESS**;
  - `mvn -f monolith-mvp/pom.xml -Dtest=ProgramManagementIntegrationTest test` -> **BUILD SUCCESS**, `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`.

## 2026-03-13 — ADHOC validation: groups in monolith vs Unicraft

- Completed targeted validation of current groups implementation in `monolith-mvp` against reconstructed Unicraft group model:
  - source artifact: `memory-bank/task-artifacts/ADHOC-UNICRAFT-GROUPS-MONOLITH-VALIDATION-2026-03-13.md`.

- Fixed current-state architecture conclusions:
  - platform currently uses 2 roles (`ADMIN`, `STUDENT`) and already supports "admin works as learner" via learner endpoints;
  - groups currently implement: membership + group->course assignment + group->program assignment + auto-apply for newly added members;
  - typed-group invariant (single `COMPANY`/`DEPARTMENT`/`POSITION` per user) is enforced in service logic.

- Main gap cluster documented for future refactor planning:
  - no manager/trainer/self-registration/lifecycle/audit capabilities in groups;
  - reviewer scope is course-based (`course_reviewers`) and not group-scoped;
  - critical consistency gap: no assignment-source model (direct/group/program), causing unstable behavior on unassign/remove/delete flows.

- Architectural simplification decisions (for Unicraft-clone target) documented:
  - keep simplified role model (admin + user/student), no manager/trainer roles for now;
  - focus group subsystem on bulk assignment to courses/programs and progress tracking;
  - prioritize P0 refactor scope around assignment source-of-truth and consistent enrollment recalculation.

## 2026-03-13 — ADHOC: technical specification for source-aware group/course/program assignment refactor

- Prepared detailed implementation-ready technical specification for backend refactor:
  - `memory-bank/task-artifacts/ADHOC-GROUP-ASSIGNMENT-SOURCE-REFORM-TZ-2026-03-13.md`.

- Specification fixes target domain contract for assignment consistency:
  - source-aware model (`DIRECT` / `GROUP` / `PROGRAM`) as single source of truth for access;
  - deterministic unassign/reconcile rules for group/course/program operations;
  - explicit progress fate policy for full unassign (including Unicraft-aligned program unassign rule).

- Specification includes implementation boundaries and rollout details:
  - DB changes (new source tables, constraints, indexes, migration/backfill strategy),
  - service-layer orchestration (`EnrollmentSourceService`) and transactional guarantees,
  - non-breaking API strategy,
  - state-machine and mandatory decision-matrix edge cases,
  - integration/regression test requirements and acceptance criteria.

## 2026-03-13 - ADHOC correction: source model canceled, separate course progress model

- Replaced previous source-aware direction with source-agnostic contract.
- Added updated technical specification:
  - memory-bank/task-artifacts/ADHOC-GROUP-ASSIGNMENT-SOURCE-REFORM-TZ-2026-03-13.md
- Fixed mandatory rules in spec:
  - unassign from course always removes Enrollment;
  - incomplete progress is deleted;
  - completed progress is retained permanently;
  - unassign from program removes ProgramEnrollment and cascades unassign to all program courses.
- Fixed architectural decision:
  - split assignment and progress by introducing dedicated course_progress entity;
  - move started completed status from Enrollment to course_progress in target design.

## 2026-03-13 - ADHOC implementation: group/program/course unassign refactor without assignment source

- Implemented target persistence split `Enrollment` vs `CourseProgress`:
  - added Flyway migration `V5__course_progress_split_from_enrollment.sql`;
  - created `course_progress` table with unique `(user_id, course_id)` and index;
  - performed backfill from `enrollments.started_at/completed_at` with status derivation (`NEW`, `IN_PROGRESS`, `COMPLETED`);
  - removed `started_at/completed_at` from `enrollments`.

- Added domain/repository layer for course progress:
  - `CourseProgress`, `CourseProgressStatus`, `CourseProgressRepository`.

- Introduced unified orchestration service for enrollment lifecycle:
  - `CourseEnrollmentLifecycleService` (assign, unassign, reset progress, program cascade);
  - `CourseAssignmentService`, `ProgramService`, `GroupService` switched to source-agnostic lifecycle behavior.

- Implemented required unassign semantics in services:
  - course unassign always removes `Enrollment`;
  - incomplete progress -> delete `CourseProgress` + `LessonSubmission` for course;
  - completed progress -> retain `CourseProgress` and `LessonSubmission`;
  - program unassign -> remove `ProgramEnrollment` + cascade through unified course unassign;
  - repeated unassign is idempotent.

- Updated read/statistics path to new progress source:
  - learner/statistics/reporting now read started/completed/status from `course_progress`.

- Added/updated integration coverage for mandatory matrix and regressions:
  - `CourseLessonCrudIntegrationTest`,
  - `GroupManagementIntegrationTest`,
  - `ProgramManagementIntegrationTest`,
  - `Task07StatisticsAndLearnerSummaryIntegrationTest`.

- Verification:
  - `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest,GroupManagementIntegrationTest,ProgramManagementIntegrationTest,Task07StatisticsAndLearnerSummaryIntegrationTest test -DfailIfNoTests=false`;
  - all targeted suites passed (0 failures / 0 errors).

## 2026-03-13 - Program access rule fix: PREVIOUS_COURSES_COMPLETED requires full prefix completion

- Fixed `ProgramService.resolveProgramCourseStatesForUser(...)`:
  - for `PREVIOUS_COURSES_COMPLETED`, course availability now depends on completion of **all previous courses** in program order, not only the immediate previous course.
  - implemented via cumulative prefix flag `allPreviousCompleted`.

- Added integration regression test:
  - `ProgramManagementIntegrationTest.previous_courses_completed_should_require_all_previous_courses_completed`;
  - verifies 3-course chain behavior: C opens only after A and B are completed.

- Verification:
  - `mvn -f monolith-mvp/pom.xml clean -Dtest=ProgramManagementIntegrationTest#previous_courses_completed_should_require_all_previous_courses_completed test`;
  - result: **BUILD SUCCESS**, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.

## 2026-03-17 — Swagger/OpenAPI contract validation across all controllers

- Performed full swagger-annotation validation for all REST controllers in `monolith-mvp`:
  - `AuthController`,
  - `CoursesController`,
  - `FilesController`,
  - `ProgramsController`,
  - `ProgressController`,
  - `SectionsController`,
  - `StudentController`,
  - `UsersController`.

- Contract alignment decisions and changes:
  - standardized operation descriptions for every endpoint (`@Operation(summary, description)`),
  - aligned response DTO schemas to actual method return types,
  - fixed list endpoint schemas to use array contracts (`@ArraySchema`) instead of single-object schemas,
  - clarified business-level `400/404` responses where service behavior indicates validation/domain-not-found scenarios,
  - refined CSV export response content declarations to explicit text payload schemas.

- OpenAPI global configuration update:
  - removed global OpenAPI security requirement from `OpenAPIConfig` (`addSecurityItem(...)`),
  - kept only `bearerAuth` scheme registration in components,
  - security is now declared explicitly at controller level through `@SecurityRequirement`, preventing accidental auth requirement exposure on public endpoints (e.g., `/api/v1/auth/**`).

- Verification:
  - module compilation passed: `mvn -q -f monolith-mvp/pom.xml -DskipTests compile`;
  - full selected integration test run was attempted, but blocked by pre-existing unrelated test compilation issue in `CourseLessonCrudIntegrationTest` (`LessonSubmission#setFirstSubmittedAt(...)` missing), not by swagger changes.

## 2026-03-22 — Course points aggregation aligned with question-level scoring model

- Reworked statistics/reporting earned-points calculation to support current lesson scoring model (question-level `pointsType` in `questionProgress`) instead of legacy direct DB sum of `pointsAwarded`.
- `StatisticsReportService` now computes earned points by:
  - loading submissions in batch for `(userIds, courseIds)`;
  - loading practice questions in batch (`PracticeQuestionRepository.findByLessonIdIn(...)`);
  - recalculating submission points via `PracticeScoringPolicy.scoreQuestion(...)` for each question progress entry;
  - summing per `(userId, courseId)` in-memory aggregation.
- Repository support updates:
  - added `LessonSubmissionRepository.findByStudentIdInAndLessonCourseIdIn(...)`;
  - added `PracticeQuestionRepository.findByLessonIdIn(...)`;
  - restored query-backed implementations for:
    - `countCompletedLessonsByUserIdsAndCourseIds(...)`,
    - `sumRetakesByUserIdsAndCourseIds(...)`.
- Integration test alignment:
  - updated `Task07StatisticsAndLearnerSummaryIntegrationTest` to current learner flow contract:
    - explicit lesson start before each submit (`POST /student/lessons/{id}/start`),
    - assertions via `lessonProgress.status/pointsAwarded` payload shape.
- Verification:
  - `mvn -f monolith-mvp/pom.xml -DskipTests compile` → **BUILD SUCCESS**;
  - `mvn -f monolith-mvp/pom.xml -Dtest=Task07StatisticsAndLearnerSummaryIntegrationTest test` → **BUILD SUCCESS**.

## 2026-03-22 - Efficiency formula aligned to completed-lessons-only rule

- Business rule alignment implemented for statistics and CSV reports:
  - efficiency now equals: `earned points on completed lessons / max points of completed lessons * 100`;
  - progress remains: `completed lessons / total lessons * 100`.
- Introduced a single in-service source of truth for course/user point aggregation in `StatisticsReportService`:
  - `aggregatePointsByUserAndCourse(...)` now returns one aggregation object with:
    - total earned points,
    - earned points on completed lessons,
    - max points on completed lessons;
  - `calculateEfficiencyByUserAndCourse(...)` computes efficiency map once and is reused by:
    - `userCourseStats(...)`,
    - `courseStats(...)`,
    - `summaryReportCsv()` and `summaryReportCsv(courseId)`.
- Integration test update:
  - `Task07StatisticsAndLearnerSummaryIntegrationTest` extended with a second (not completed) lesson to validate:
    - efficiency = `100` when all completed lessons are solved at max,
    - progress = `50` when completed 1 of 2 lessons,
    - CSV values: efficiency `100.00`, progress `50.00%`.

- Verification:
  - `mvn -pl monolith-mvp -DskipTests compile -q` -> **BUILD SUCCESS**;
  - `mvn -pl monolith-mvp -Dtest=Task07StatisticsAndLearnerSummaryIntegrationTest test -q` -> **BUILD SUCCESS**;
  - focused `CourseLessonCrudIntegrationTest` methods still fail on pre-existing flow constraints (`Theory lesson not started`), not introduced by this change-set.

## 2026-03-22 — Fix: `LessonRepository.sumFullPointsByCourseIds` query binding/type alignment

- Fixed JPQL aggregation method in `LessonRepository`:
  - added explicit parameter binding `@Param("courseIds")` for `sumFullPointsByCourseIds(...)`;
  - aligned `coalesce` fallback literal to long type (`0L`) in projection query:
    - `coalesce(sum(l.fullPoints), 0L) as value`.
- Context:
  - the method returns `CourseMetricProjection.getValue(): Long`, so explicit long fallback avoids numeric type ambiguity in JPQL/Hibernate parsing and projection mapping.

- Verification:
  - `mvn -pl monolith-mvp -DskipTests compile -q` → **BUILD SUCCESS**.

## 2026-03-22 — Practice lesson points consistency: DB recalculation method

- Added SQL-based repository method to normalize persisted `lessons.full_points` for existing practice lessons according to sum of question points:
  - `LessonRepository.recalculatePracticeLessonsFullPoints()`.
- Implementation details:
  - native SQL update over `lessons` with correlated aggregate from `practice_questions`;
  - applies only to `lesson_kind = 'PRACTICE'`;
  - writes `coalesce(sum(pq.full_points), 0)` to `lessons.full_points`.

- Verification:
  - `mvn -pl monolith-mvp -DskipTests compile -q` → **BUILD SUCCESS**.

## 2026-03-22 — Practice lesson `full_points` as source of truth (dynamic getter rollback)

- Finalized architecture decision for practice lessons:
  - `lessons.full_points` is the authoritative value;
  - dynamic override `PracticeLesson#getFullPoints()` was removed to avoid lazy/N+1 side effects and dual-source inconsistency.

- Ensured deterministic recalculation on question mutations in `CourseLessonAdminService`:
  - added `recalculatePracticeLessonFullPoints(PracticeLesson lesson)`;
  - called after create question-pool apply (`createPracticeLesson(...)`);
  - called after update question-pool apply (`updatePracticeLesson(...)` when `questions` payload is present).

- Behavior validation notes:
  - practice question mutations in application flow are centralized in `CourseLessonAdminService.applyQuestionPool(...)`;
  - with transactional save, `lesson.questions` and `lesson.fullPoints` are persisted atomically within the same write flow.

- Verification:
  - `mvn -pl monolith-mvp -DskipTests compile -q` → **BUILD SUCCESS**;
  - `mvn -pl monolith-mvp -Dtest=Task07StatisticsAndLearnerSummaryIntegrationTest test -q` → **BUILD SUCCESS**.

## 2026-03-22 — CSV report streaming in HTTP response body (flush each 50 rows)

- Refactored CSV export delivery path to stream directly to HTTP response writer instead of building one large in-memory response string.
- Scope intentionally limited to transport/output layer; statistics aggregation logic was not changed.

- Controller layer:
  - `ProgressController` CSV endpoints switched from `ResponseEntity<String>` to direct writing via `HttpServletResponse` writer:
    - `GET /api/v1/admin/progress/reports/summary.csv`
    - `GET /api/v1/admin/progress/courses/{courseId}/summary-report.csv`
  - response `Content-Type`/encoding are set explicitly (`text/plain`, UTF-8).

- Service/renderer layer:
  - `CsvReportRenderer` extended with writer-based methods:
    - `writeHeader(Writer, List<String>)`
    - `writeRow(Writer, List<String>)`
  - `StatisticsReportService` added writer-based streaming methods:
    - `writeSummaryReportCsv(Writer)`
    - `writeSummaryReportCsv(Long, Writer)`
  - Implemented periodic flush policy in both CSV flows:
    - `writer.flush()` every 50 data rows,
    - final flush at method end.
  - Existing string-returning methods were preserved as compatibility wrappers via `StringWriter`.

- Facade alignment:
  - `StatisticsService` now exposes writer-based delegating methods for both summary CSV variants.

- Verification:
  - `mvn -pl monolith-mvp -DskipTests compile -q` → **BUILD SUCCESS**.
  - Focused CSV-related integration test run was prepared; broader `Task07StatisticsAndLearnerSummaryIntegrationTest` currently fails in this branch due a pre-existing unrelated schema mismatch (`learning_programs.deadline_days` in H2 test context), not introduced by CSV streaming changes.

## 2026-03-22 - Statistics/report refactor: unified report context + typed CSV rows + formatter extraction

- Refactored report/statistics internals without changing controller contracts or endpoint behavior.
- Scope implemented according to requested items: **1, 2, 3, 5**.

- `StatisticsReportService`:
  - introduced unified internal `ReportContext` that centralizes loading of:
    - progress by user/course,
    - course max points and lessons count,
    - earned points and efficiency,
    - optional completed lessons / retakes / memberships.
  - replaced duplicated per-method data-loading blocks in:
    - `userCourseStats(...)`,
    - `courseStats(...)`,
    - `writeSummaryReportCsv(...)`,
    - `writeSummaryReportCsv(courseId, ...)`.
  - extracted shared CSV writing pipeline (`writeCsvRows(...)`) to standardize row streaming + periodic flush behavior.

- Typed row models for CSV mapping:
  - `ReportRowDto` converted into shared typed base row contract used by CSV row mappers.
  - introduced internal typed CSV row carriers (`SummaryCsvRow`, `CourseSummaryCsvRow`) and explicit column mappers instead of ad-hoc inline `List<String>` construction.
  - row mapping now follows a stable, explicit transformation path:
    domain/enrollment -> typed row dto -> CSV columns.

- Formatting standardization:
  - added `StatisticsReportFormatter` component with centralized formatting rules for:
    - date/time parts,
    - duration/spent time,
    - login extraction,
    - enrollment status text,
    - numeric formatting (efficiency/progress),
    - null-safe textual rendering.
  - removed duplicated formatter logic from `StatisticsReportService` and delegated to formatter component.

- Null-safety/stability improvements (internal):
  - `courseStats(...)` now safely handles missing `CourseProgress` by returning `null` status instead of risking NPE.
  - practice submission scoring uses null-safe access for `questionProgress` collection.

- Verification:
  - `mvn -pl monolith-mvp -DskipTests compile -q` -> **BUILD SUCCESS**.
