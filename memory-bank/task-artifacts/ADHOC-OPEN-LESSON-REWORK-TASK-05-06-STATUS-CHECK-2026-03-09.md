# ADHOC OPEN LESSON REWORK — TASK-05/TASK-06 status check (2026-03-09)

## Source of truth used

- `memory-bank/task-artifacts/ADHOC-OPEN-LESSON-REWORK-IMPLEMENTATION-PLAN-2026-03-08.json`
- `memory-bank/task-artifacts/ADHOC-OPEN-LESSON-REWORK-TASK-01-DOMAIN-CONTRACTS-2026-03-08.md`
- Current implementation files:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/ProgressController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/LessonSubmissionRepository.java`

---

## TASK-05 (submit flow) — status

### Deliverables from plan

1. `submitPractice` with upsert single submission
2. Scoring `FULL/PARTIAL/ZERO` by agreed rules
3. Open submit: first submit all questions, re-submit only `REWORK`
4. Lesson points: either `lesson.fullPoints` or `0` by threshold

### Actual state

- ✅ Open submit flow writes question-level progress in one submission record and reuses existing `REWORK` submission.
- ✅ Open re-submit behavior is implemented as “update only `REWORK` (and pending/missing) question entries”, while accepted/rejected stay unchanged.
- ❌ Test submit is **not** true upsert flow yet:
  - code still creates a new `LessonSubmission` for test submit path;
  - with unique index `(lesson_id, student_id)`, repeat submit/update semantics are incomplete.
- ❌ Test `PARTIAL` formula is still not strict per TASK-01:
  - current logic allows only subset-without-wrong as partial;
  - required rule is `wrongSelected <= 1` and `missedCorrect <= 1`.
- ❌ Test lesson points are still not binary full/0 in submission:
  - current code stores sum of question points for test in `submission.pointsAwarded`;
  - required is lesson-level binary: `fullPoints` or `0`.
- ⚠️ Finalized lesson submit blocking is not fully explicit in service layer:
  - for some paths it can currently rely on persistence constraints/flow side effects instead of dedicated business guard.

### Verdict

`TASK-05` is **partially implemented**, but **not complete** by acceptance criteria.

---

## TASK-06 (review flow) — status

### Deliverables from plan

1. Pending reviews provide reviewable lessons/submissions with question statuses
2. `reviewOpenSubmission` accepts decisions for all questions
3. Aggregation: any `REWORK` => lesson `REWORK`; else final `COMPLETE/INCOMPLETE`
4. `REJECTED` always `0` points

### Actual state

- ✅ Review endpoint accepts per-question decisions for full lesson (`questionReviews` keyed by `questionIndex`).
- ✅ Aggregation to lesson status is implemented (`REWORK` vs final `COMPLETE/INCOMPLETE`).
- ✅ Finalized lesson guard exists for reviewer (`completed=true` blocks review).
- ✅ Reviewer-zone check exists (`canReviewCourse(...)`).
- ✅ `REJECTED/REWORK` are forced to `0` in review calculation.
- ✅ Pending review data is implemented via two endpoints:
  - list: `GET /api/v1/admin/progress/reviews/pending`
  - details: `GET /api/v1/admin/progress/reviews/pending/{submissionId}`

### Dependency caveat

- ⚠️ Since `TASK-06` depends on `TASK-05`, and `TASK-05` is still partial, `TASK-06` cannot be considered fully final/closed at plan level yet.

### Verdict

`TASK-06` is **implemented in large part**, but final closure should follow after finishing missing `TASK-05` items.

---

## Consolidated conclusion

- `TASK-05`: **NOT DONE** (partial)
- `TASK-06`: **MOSTLY DONE**, pending final closure after TASK-05 completion

## Minimal next actions to close TASK-05/06 cleanly

1. Convert test submit path to real single-record upsert behavior.
2. Implement strict `MULTIPLE_CHOICE` partial scoring formula (`<=1 wrong` and `<=1 missed`).
3. Switch test lesson-level points awarding to binary `fullPoints` or `0`.
4. Add explicit service-level finalized-submit block for all submit paths (not via side effects).
5. Re-run compile/tests and then re-check TASK-06 as fully closed.
