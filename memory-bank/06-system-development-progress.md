# System development progress

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