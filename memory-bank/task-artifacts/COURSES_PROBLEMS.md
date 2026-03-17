## Субагент 1

Как работает сейчас

- `PracticeScoringPolicy` считает баллы по `QuestionPointsType` (`FULL/PARTIAL/ZERO`), для test-вопросов `pointsType` выводится из ответов (`resolveTestQuestionPointsType`).
- В `PracticeSubmissionService.submitPractice` проверяются enrollment/access/deadline, затем upsert одной записи `LessonSubmission` на пару student+lesson.
- Для `PRACTICE_TEST`: на сабмите сразу считается итог, `status=COMPLETE|INCOMPLETE`, `completed = passedByPoints || isLastAttempt`, `questionProgress` заполняется с `pointsType`.
- Для `PRACTICE_OPEN_ANSWER`: на сабмите `status=PENDING_REVIEW`, `completed=false`, `pointsAwarded=0`, `questionProgress.reviewStatus=PENDING_REVIEW`, `pointsType=null`; повторный сабмит разрешён только из `REWORK`.
- В `OpenReviewService.reviewOpenSubmission` решение принимается по `questionIndex` (не по `questionId`), проверяется полное покрытие всех вопросов урока.
- В open-review итоговый статус: `REWORK` если есть хотя бы один REWORK; иначе `COMPLETE` при прохождении порога, иначе `INCOMPLETE`; при `REWORK` баллы урока сбрасываются в 0.
- `getPendingReviews/getPendingReviewQuestions` работают только для `submission.status == PENDING_REVIEW` и `completed=false`.
- В learner-read (`LearningService`) видимость correct answers привязана к `showCorrectAnswersAfterCompletion && submission.completed`; per-question awarded points берутся через `pointsType`.
- В `CourseLearnerReadService` для practice-уроков баллы пересчитываются «на чтении» из `questionProgress.pointsType` + текущих `PracticeQuestion`.

Критичные баги

1. Приложение не поднимается из-за невалидного JPQL в `LessonSubmissionRepository`.

- Доказательство: `LessonSubmissionRepository.deleteAllByLessonIdIfCourseNotCompleted` содержит `cp.status == 'COMPLETED'` (JPQL/HQL ждёт `=`).
- Подтверждено запуском тестов: `mvn -pl monolith-mvp -Dtest=Task05SubmitFlowIntegrationTest,Task06ReviewFlowIntegrationTest,Task08LearnerAnswersVisibilityIntegrationTest test` падает на `QueryCreationException`/`SyntaxException` по этому запросу.

2. NPE в course-read для practice-уроков (ломает `/api/v1/student/courses/{courseId}` в реальных сценариях).

- Доказательство A: `CourseLearnerReadService.buildLearnerLessonSummaries` вызывает `submission.getQuestionProgress().stream()` без проверки `submission != null`.
- Доказательство B: для open-submit `PracticeSubmissionService.buildOpenQuestionProgressForSubmit` явно ставит `pointsType=null`, а затем `CourseLearnerReadService` вызывает `practiceScoringPolicy.scoreQuestion(progress.getPointsType(), ...)`; в `PracticeScoringPolicy.scoreQuestion` `switch(pointsType)` -> NPE при null.

3. Контракт review API и тесты/клиенты рассинхронизированы по модели начисления баллов.

- Доказательство: `ReviewQuestionDecisionDto` содержит `submissionStatus + pointsType + reviewComment` (поля `awardedPoints` нет).
- В `OpenReviewService.reviewOpenSubmission` баллы считаются только из `decision.pointsType()`.
- Но `Task06ReviewFlowIntegrationTest` и `Task08LearnerAnswersVisibilityIntegrationTest` отправляют `awardedPoints` и ожидают произвольные значения (7/8/9), что текущей моделью не поддерживается.

Высокие/средние риски

- `OpenReviewService.validateReviewQuestionDecision` не требует `pointsType` для `ACCEPTED`: можно принять вопрос без баллов, получить 0 и финализировать `INCOMPLETE`.
- В `ProgressController.reviewOpenAnswer` нет `@Valid` на `ReviewOpenSubmissionRequest`; null-решения/null-status в map могут приводить к неконтролируемым runtime-ошибкам вместо 400.
- `PracticeSubmissionService.validateAndNormalizeAnswersByQuestion`: для `OPEN_ANSWER/SINGLE_CHOICE` используется `answers.getFirst()` без проверки пустого списка -> `NoSuchElementException` (500) при пустом/blank input.
- Семантика REWORK изменилась относительно тестов: `OpenReviewService.getPendingReviewQuestions` допускает только `PENDING_REVIEW`, тогда как `Task06` ожидает чтение деталей и в `REWORK`.
- Тесты submit/review жёстко используют ключи `"1"`, `"2"` как questionId (`Task05/06/08`) вместо реальных id из API — хрупкая связка с генерацией идентификаторов.

Рекомендации исправления (без патчей)

1. Исправить `LessonSubmissionRepository.deleteAllByLessonIdIfCourseNotCompleted`: валидный JPQL (`=`), использовать параметр `lessonId` в фильтре, и добавить отдельный репозиторный/контекстный тест на создание query.
2. В `CourseLearnerReadService` сделать null-safe расчёт practice-прогресса: обрабатывать `submission == null`, `questionProgress == null`, `pointsType == null`; определить единый source of truth для lesson points (пересчёт vs `submission.pointsAwarded`).
3. Зафиксировать единый контракт review: либо окончательно перейти на `pointsType` (и обновить тесты/клиенты/Swagger), либо вернуть numeric `awardedPoints`; в любом случае валидировать `ACCEPTED => pointsType обязателен`.
4. Усилить валидацию payload: `@Valid` на review request, null-check map values/status, явные 400-ошибки вместо runtime исключений.
5. В submit-валидации явно проверять обязательный непустой ответ для `OPEN_ANSWER/SINGLE_CHOICE` до `getFirst()`, чтобы исключить 500 на некорректном вводе.


## Субагент 2

Ниже — архитектурная валидация текущего флоу прогресса и «реакций на обновления» после рефакторинга.

## 1) Фактическая архитектура реакций (цепочки вызовов)

### A. Обновление урока

__Theory lesson__

- `CoursesController.updateTheoryLesson`\
  → `CourseLessonAdminService.updateTheoryLesson`\
  → `CourseLessonAdminService.applyLessonPositionPatch` (если меняется позиция)\
  → `CourseLessonAdminService.applyTheoryLessonFields`\
  → `lessonRepository.save(...)`

__Practice lesson__

- `CoursesController.updatePracticeLesson`\
  → `CourseLessonAdminService.updatePracticeLesson`\
  → `applyLessonPositionPatch`\
  → `applyPracticeLessonFields`\
  → (если `questions != null && !empty`) `applyQuestionPool`\
  → (только при удалении вопросов) `onQuestionsChanged`\
  → `lessonRepository.save(...)`

__Факт реакции:__ обновляется только lesson/question данные (+ частично JSON прогресса по удалённым вопросам). Пересчёт `CourseProgress`, пересчёт `LessonSubmission.status/completed/pointsAwarded`, синхронизация с программами — отсутствуют.

---

### B. Обновление вопросов урока

- Внутри `CourseLessonAdminService.applyQuestionPool`:

  - новые вопросы добавляются,
  - существующие обновляются,
  - удалённые удаляются из lesson.

- При удалении вопросов: `CourseLessonAdminService.onQuestionsChanged`:

  - `submissionRepository.findAllByLessonId(...)`,
  - из `questionProgress_json` удаляются записи по удалённым questionId,
  - `submissionRepository.saveAllAndFlush(...)`.

__Факт реакции:__ реакция есть только на удаление question progress из JSON. На изменение корректных ответов/баллов/порогов/типа урока реакции по перерасчёту фактического прохождения нет.

---

### C. Обновление курса

- `CoursesController.updateCourse`\
  → `CourseService.updateCourse`\
  → `setLessonsToNewPositionsIfNeeded` (массовый reorder)\
  → `applyCourseFields` (title/desc/deadlineDays/freeOrder/section/cover)\
  → `courseRepository.save(...)`.

__Факт реакции:__ только запись course/lesson positions. Прогрессы не пересчитываются.\
Реакции «на чтении» работают динамически через:

- `CourseAccessPolicy.assertCourseDeadlineNotExceededForStudent` (deadlineDays),
- `LessonAccessPolicy.assertLessonAccessAllowed` / `assertStopLessonAccessAllowed`.

---

### D. Reset’ы прогресса

1. __Reset course progress (по пользователю):__

- `ProgressController.resetStudentProgressByCourse`\
  → `CourseService.resetStudentCourseProgress`\
  → `CourseEnrollmentLifecycleService.resetCourseProgress`\
  → `course_progress.startedAt/completedAt/status=NEW` + `lessonSubmissionRepository.deleteByStudentIdAndLesson_Course_Id`.

2. __Reset lesson progress (по пользователю):__

- `ProgressController.resetStudentProgressByLesson`\
  → `CourseService.resetStudentLessonProgress`\
  → `CourseEnrollmentLifecycleService.resetLessonProgress`\
  → проверка «course already passed»\
  → `deleteByStudentIdAndLessonId`.

3. __Reset lesson progress (для всех):__

- `ProgressController.resetLessonProgressForAllUsers`\
  → `CourseService.resetLessonProgress`\
  → `CourseEnrollmentLifecycleService.resetLessonProgressForAllUncompleted`\
  → `LessonSubmissionRepository.deleteAllByLessonIdIfCourseNotCompleted`.

__Факт реакции:__ reset-операции не вызывают `ProgramService.onCourseProgressChanged`, не инициируют ревокацию/переназначение program-derived enrollment.

---

### E. Связи с program enrollment

__Позитивная реакция (unlock/enroll):__

- `PracticeSubmissionService` / `OpenReviewService`\
  → `EnrollmentProgressService.markEnrollmentStarted` / `markEnrollmentCompletedIfDone`\
  → `ProgramService.onCourseProgressChanged`\
  → `ProgramService.ensureProgramCourseEnrollmentsForUser`\
  → `CourseEnrollmentPort.enrollStudentToCourse` (impl: `CourseAssignmentService.enrollStudentToCourse`)\
  → `CourseEnrollmentLifecycleService.assignToCourse`.

__Негативная реакция (lock/revoke) отсутствует:__

- `ensureProgramCourseEnrollmentsForUser` делает только __enroll__, но не unassign.
- reset’ы прогресса не триггерят program resync.

## 2) Где реакции отсутствуют / не подключены (critical/high)

### CRITICAL-1: «Reset lesson for all» реализован некорректно и опасно

- `LessonSubmissionRepository.deleteAllByLessonIdIfCourseNotCompleted`
- `CourseEnrollmentLifecycleService.resetLessonProgressForAllUncompleted`

Проблемы:

- параметр `lessonId` __не используется__ в JPQL;
- удаление не коррелировано по пользователю/уроку (логика по course-id в целом);
- используется `cp.status == 'COMPLETED'` (нестандартный оператор для JPQL, риск runtime/portable failure).

---

### CRITICAL-2: Реакция на изменение курса/уроков в прогрессе не подключена

- Метод есть: `EnrollmentProgressService.markEnrollmentCompletedOnCourseChanged`.
- Вызовов нет (dead code).
- `CourseLessonAdminService` содержит `EnrollmentProgressService`, но не использует его в `create*/update*/deleteLesson`.
- `CourseService.updateCourse` не инициирует progress-resync.

---

### HIGH-1: Нет обратной синхронизации program enrollment (только «доназначение»)

- `ProgramService.ensureProgramCourseEnrollmentsForUser` → только `enrollStudentToCourse(...)`.
- `CourseEnrollmentLifecycleService.resetCourseProgress` / `resetLessonProgress` не дергают `ProgramService.onCourseProgressChanged`.

Итог: после reset/деградации прогресса уже назначенные downstream-курсы остаются доступными.

---

### HIGH-2: На update questions нет пересчёта результатов прохождения

- `CourseLessonAdminService.applyQuestionPool` + `onQuestionsChanged`.

Реакция ограничена удалением questionProgress по удалённым вопросам. Нет пересчёта:

- `LessonSubmission.pointsAwarded`,
- `LessonSubmission.status/completed`,
- `CourseProgress.status/completedAt`,
- program unlock state.

---

### HIGH-3: Последовательный доступ к урокам считается с кросс-курсовым шумом

- `LessonAccessPolicy.assertLessonAccessAllowed`
- `LessonSubmissionRepository.findAllCompletedByStudentIdAndLessonPositionLessThan`

В запросе нет фильтра по `courseId`, что может и блокировать, и ошибочно открывать доступ к урокам.

## 3) Бизнес-эффект каждого дефекта

- __CRITICAL-1 (reset all lesson):__ риск массовой потери прогресса не по целевому уроку/курсу; админская операция становится небезопасной.
- __CRITICAL-2 (нет course-changed resync):__ сертификаты/статусы прохождения становятся недостоверными после изменения контента (добавили/удалили урок, изменили структуру).
- __HIGH-1 (program only-enroll):__ обход правил программы (последовательность/блокировки) после reset; пользователи сохраняют доступ туда, где по бизнес-правилам должны быть снова заблокированы.
- __HIGH-2 (нет перерасчёта после update questions):__ неверные pass/fail, неверная статистика/баллы, жалобы на «необновившиеся» результаты после изменения теста.
- __HIGH-3 (кросс-курсовый prerequisite):__ непредсказуемый learner experience (уроки «вдруг закрыты/открыты» из-за прогресса в других курсах), рост обращений в поддержку.

## 4) Узкие места и деградации

- `CourseLessonAdminService.onQuestionsChanged`: full-scan всех submissions урока + массовый `saveAllAndFlush` (тяжело на больших потоках).

- `LessonAccessPolicy.assertLessonAccessAllowed`: тянет список submissions и сравнивает `size`, вместо cheap `count` + корректного `courseId` фильтра.

- `EnrollmentProgressService.markEnrollmentCompletedOnCourseChanged`: потенциально N+1-цикл по пользователям (count + lessons fetch на каждого), при подключении без батчинга может стать дорогим.

- `CourseLearnerReadService.buildLearnerLessonSummaries`: риск NPE для практик без submission (`submission.getQuestionProgress()` без null-check) и некорректная логика `allPreviousLessonsPassed` (блокировка вычисляется с учётом текущего урока).

- Контракт schema↔model после миграций имеет риски расхождения:

  - `V4` удаляет `courses.passing_threshold_percent`, но поле есть в `Course`;
  - `V4` добавляет `lesson_submissions.firstSubmittedAt` (camelCase), модель — `firstSubmittedAt` без явного `@Column(name=...)` (при стандартном snake_case naming это потенциальный runtime mismatch).


## Субагент 3

Ниже независимый аудит покрытия и рисков по course learning (фокус на 5 указанных интеграционных тестах + текущие controller/dto/service-контракты).

__Реально покрытые бизнес-сценарии__

- `Task05SubmitFlowIntegrationTest.practice_test_submit_should_upsert_single_submission_and_block_after_finalization` → `POST /api/v1/student/lessons/{lessonId}/submit-practice`: один submission на урок (upsert), завершение по порогу, блок повторной отправки после финализации.
- `Task05SubmitFlowIntegrationTest.practice_test_multiple_choice_should_apply_strict_partial_formula` → `submit-practice`: частичная/нулевая оценка для `MULTIPLE_CHOICE` по текущей формуле `PracticeScoringPolicy`.
- `Task06ReviewFlowIntegrationTest.open_review_flow_should_keep_rework_in_pending_and_finalize_with_complete` → `GET /api/v1/admin/progress/reviews/pending`, `GET /pending/{submissionId}`, `POST /reviews/{submissionId}`: ACL reviewer-а, цикл `PENDING_REVIEW -> REWORK -> PENDING_REVIEW -> COMPLETE`, запрет late review.
- `Task08LearnerAnswersVisibilityIntegrationTest.practice_test_should_show_user_answers_and_correct_answers_only_after_completion` → `GET /api/v1/student/lessons/{lessonId}`: видимость `userAnswers`, отложенная видимость `correctAnswers` после completion.
- `Task08LearnerAnswersVisibilityIntegrationTest.options_and_correct_answers_should_be_preserved_with_special_characters` + `GET /api/v1/admin/courses/{courseId}/lessons/{lessonId}`: сохранность special chars в `options/correctAnswers` (JSON converter).
- `CourseLessonCrudIntegrationTest.stop_lesson_should_block_next_lessons_even_when_lessons_free_order_enabled` → блокировка по `stopLesson` даже при `lessonsFreeOrder=true`.
- `CourseLessonCrudIntegrationTest.practice_attempt_limit_should_block_third_attempt_after_two_failed` и `...time_limits_should_block_after_course_deadline_and_practice_time_limit` → лимиты попыток/времени и дедлайн курса.
- `CourseLessonCrudIntegrationTest.enrollment_two_lists_flow_should_work` → `POST/GET /api/v1/admin/courses/{courseId}/enrollments`: two-list enroll/unenroll.
- `CourseLessonCrudIntegrationTest` (stats/csv методы) → `GET /student/my/stats`, `GET /admin/progress/courses/{id}/stats`, `GET .../summary-report.csv`, `GET /admin/progress/reports/summary.csv`.

__Критичные сценарии, которые НЕ покрыты (или покрыты невалидно относительно текущего контракта)__

- Open-review по новому контракту `pointsType` (`FULL/PARTIAL/ZERO`) и статусу `REJECTED` не покрыт корректно (см. рассинхрон ниже).
- `GET /api/v1/student/courses/{courseId}` для курса с __неотправленным practice__ и для open-submission с `pointsType=null` (pending/rework) — нет валидного теста на null-safe поведение.
- Межкурсовая изоляция prerequisite-логики (`LessonAccessPolicy`): нет теста, что completion уроков в курсе A не открывает уроки в курсе B.
- Актуальные reset-контракты: `POST /api/v1/admin/progress/courses/user/reset`, `POST /api/v1/admin/progress/lessons/user/reset`, `POST /api/v1/admin/progress/lessons/{lessonId}/reset` (последний вообще без покрытия).

__Где тесты устарели / рассинхронизированы с текущими DTO/endpoint/service__

1. __Review DTO drift (критично)__

- Файлы/методы:

  - `Task06ReviewFlowIntegrationTest.open_review_flow_should_keep_rework_in_pending_and_finalize_with_complete`
  - `Task08LearnerAnswersVisibilityIntegrationTest.open_lesson_should_show_or_hide_status_and_awarded_points_by_flag`
  - `CourseLessonCrudIntegrationTest.enrollment_submission_and_review_corner_cases_should_work`
  - `CourseLessonCrudIntegrationTest.open_practice_with_multiple_questions_should_require_full_question_answers_and_be_reviewed_as_single_submission`

- Endpoint: `POST /api/v1/admin/progress/reviews/{submissionId}`

- Тесты шлют `awardedPoints`, а текущий контракт `ReviewQuestionDecisionDto` принимает `pointsType`. Это разные API-семантики.

2. __Reset endpoint drift__

- `CourseLessonCrudIntegrationTest.admin_progress_reset_endpoint_should_clear_student_course_progress`
- Тест вызывает `POST /api/v1/admin/progress/users/reset`, а controller объявляет `POST /api/v1/admin/progress/courses/user/reset`.

3. __Learner DTO drift (структура ответа изменилась)__

- `Task07StatisticsAndLearnerSummaryIntegrationTest.stats_and_learner_summary_should_use_single_submission_and_attempt_counter` ожидает в `lessons[]` плоские `completed/pointsAwarded`.
- `CourseLessonCrudIntegrationTest.learner_course_progress_and_next_lesson_endpoint_should_return_expected_data` ожидает плоские `completionPercent/completedLessons/.../courseCompleted` и `lessons[].completed/pointsAwarded`.
- Текущий контракт: `CourseLearnerDto.progress` + `LearnerLessonSummaryDto.lessonProgress`.

4. __Block reason enum/string drift__

- `CourseLessonCrudIntegrationTest.learner_course_progress_and_next_lesson_endpoint_should_return_expected_data` проверяет `PREVIOUS_LESSON_NOT_PASSED`, а сервис возвращает `PREVIOUS_LESSONS_NOT_PASSED` / `STOP_LESSON_BLOCK`.

5. __Practice submission payload в тестах хрупкий__

- Во многих тестах `questionAnswers` ключи жестко как `"1"`, `"2"`; текущий контракт — `Map<Long,List<String>>` по __questionId__, не по позиции.
- Это невалидный/хрупкий паттерн для реального поведения.

---

### Приоритезированный список проблем (P0/P1/P2)

__P0__

1. __Рассинхрон review-контракта (`awardedPoints` vs `pointsType`)__

  - Где: Task06/Task08/CourseLessonCrud методы review, endpoint `POST /admin/progress/reviews/{submissionId}`.
  - Риск: ручная проверка open-ответов может фактически не начислять баллы по ожидаемой логике; блокируется завершение уроков/курсов и выдача результатов.

2. __Не покрыт (и по коду выглядит рискованно) learner summary для practice/open состояний__

  - Где: `CourseLearnerReadService.buildLearnerLessonSummaries`, endpoint `GET /student/courses/{courseId}`.
  - Риск: 500 на ключевом learner-экране (новый студент с непройденной практикой, open pending/rework).

3. __Не покрыта межкурсовая изоляция prerequisite-проверки__

  - Где: `LessonAccessPolicy.assertLessonAccessAllowed` + `LessonSubmissionRepository.findAllCompletedByStudentIdAndLessonPositionLessThan`.
  - Риск: студент может получать доступ к урокам курса B за счет прогресса в курсе A (нарушение бизнес-правил обучения).

__P1__

1. __Сломанное/устаревшее покрытие reset API__

  - Где: `CourseLessonCrudIntegrationTest.admin_progress_reset_endpoint_should_clear_student_course_progress`, `ProgressController`.
  - Риск: регресс в support-операциях сброса прогресса не ловится тестами.

2. __Сломанное покрытие learner DTO-контракта__

  - Где: Task07 + CourseLessonCrud (`learner_course_progress...`).
  - Риск: ложное чувство покрытия при изменениях API для learner-dashboard.

3. __Хрупкие payload-ключи questionAnswers (позиции вместо questionId)__

  - Где: Task05/07/08/CourseLessonCrud submit-practice вызовы.
  - Риск: флак и пропуск реальных контрактных ошибок клиента.

__P2__

1. __Бриттл-ассерты по строковым константам blockReason__ (`PREVIOUS_LESSON_NOT_PASSED` vs текущее значение).
2. __CSV-тесты сильно индексные; слабая защита от эволюции контракта (семантика полей проверяется частично).__


## Субагент 4
