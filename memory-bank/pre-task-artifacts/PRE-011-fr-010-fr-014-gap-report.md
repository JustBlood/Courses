# PRE-011 — Сверка реализации FR-010..FR-014 (enrollment/review/learning flow)

## 1) Контекст и границы проверки
- Источник задачи: `memory-bank/pre-tasks.json` → `PRE-011`.
- Цель: проверить соответствие реализации требованиям `FR-010..FR-014` и критериям `AC-010..AC-014`.
- Проверенные требования:
  - `memory-bank/prd/02-functional-requirements.md` (FR-010..FR-014)
  - `memory-bank/prd/06-acceptance-criteria.md` (AC-010..AC-014)
  - `memory-bank/prd/01-user-scenarios.md` (US-03, US-04)
- Проверенные ключевые файлы реализации:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/StudentController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/ProgressController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/{Enrollment,CourseReviewer,LessonSubmission,SubmissionStatus}.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/{EnrollmentRepository,CourseReviewerRepository,LessonSubmissionRepository}.java`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`

---

## 2) Матрица соответствия FR

| FR | Статус | Наблюдения |
|---|---|---|
| FR-010 (зачисление/отчисление, two-lists) | **partial** | Есть зачисление/отчисление по endpoint `POST/DELETE /api/v1/admin/courses/{courseId}/assign`, атомарность и защита от дублей обеспечены (`uk_enrollment_user_course`, `existsByUserIdAndCourseId`). Но нет API/DTO модели двух списков «зачисленные/не зачисленные» в настройках курса. |
| FR-011 (назначение reviewer и рабочая область проверки) | **partial** | Назначение reviewer реализовано (`/reviewers`), назначать можно только `ADMIN`, а review-действие ограничено проверкой `canReviewCourse(...)`. Есть очередь pending (`/api/v1/admin/progress/reviews/pending`). При этом отдельного представления «назначенные курсы reviewer» нет (только список pending submissions), что частично расходится с PRD-формулировкой. |
| FR-012 (прохождение theory + начисление баллов) | **implemented** | `POST /api/v1/student/lessons/{lessonId}/complete-theory` проверяет зачисление, тип урока THEORY, создаёт submission со статусом `COMPLETE`, начисляет `lesson.fullPoints`, обновляет started/completed у enrollment. |
| FR-013 (автопроверка тестовых заданий) | **partial** | Автопроверка есть в `submitPractice`, выставляются статус/баллы. Но runtime всё ещё проверяет фактически только первый вопрос практики и не покрывает полноценный multi-question workflow и часть логики по всем типам тестовых вопросов. |
| FR-014 (workflow open answer: pending → rework → accepted) | **partial** | Есть ручная проверка: после отправки open-answer создаётся `PENDING_REVIEW`; reviewer может повторно отправить в review (`toNextReview=true`) или завершить с passed/failed. Но нет явных статусов `REWORK` и `ACCEPTED` (используются `PENDING_REVIEW/COMPLETE/INCOMPLETE`), поэтому workflow реализован частично относительно PRD. |

---

## 3) Матрица соответствия AC

| AC | Статус | Доказательства / комментарий |
|---|---|---|
| AC-010 (перевод между «зачислены/не зачислены») | **partial** | Перевод доступов по enroll/unenroll реализован и проверен тестами; но «двухсписочная» модель на уровне API отсутствует. |
| AC-011 (назначенные reviewer получают workspace с ожидающими ответами) | **partial** | Pending-очередь и проверки reviewer реализованы; отдельного endpoint/экрана списка назначенных курсов в текущем API нет. |
| AC-012 (завершение theory фиксируется и начисляет баллы) | **implemented** | Поведение подтверждается `LearningService.completeTheoryLesson` и интеграционным тестом `CourseLessonCrudIntegrationTest`. |
| AC-013 (автопроверка тестов: результат+статус+баллы) | **partial** | Базовый автоскоринг и статусы работают, но расчёт/логика прохождения упрощены и не покрывают полноценно question pool урока. |
| AC-014 (open answer: reviewer проверяет, может вернуть на доработку, статусы/баллы обновляются) | **partial** | Pending review и повторная отправка на review есть, но без отдельного состояния rework/accepted и с упрощённой моделью статусов. |

---

## 4) Выявленные gaps для learning path (приоритизация)

1. **GAP-LP-011-01 (High): нет двухсписочной API-модели enrollment для FR-010**
   - Факт: только batch assign/unassign по `IdsRequest`.
   - Риск: UI/операционный сценарий «enrolled/not enrolled» реализуется неявно и сложнее трассируется к AC-010.

2. **GAP-LP-011-02 (High): workflow open-answer не имеет явных бизнес-статусов rework/accepted**
   - Факт: `SubmissionStatus` = `COMPLETE/INCOMPLETE/PENDING_REVIEW`.
   - Риск: расхождение с PRD-терминами FR-014/AC-014 и неоднозначность аналитики по этапам ручной проверки.

3. **GAP-LP-011-03 (High): неполная автопроверка practice-урока**
   - Факт: `submitPractice` использует только первый вопрос урока для оценки.
   - Риск: FR-013/AC-013 формально закрываются частично; итоговый результат урока может быть некорректным для multi-question сценариев.

4. **GAP-LP-011-04 (Medium): reviewer workspace ограничен очередью pending**
   - Факт: нет отдельного API «список назначенных курсов reviewer».
   - Риск: частичное соответствие формулировке FR-011/AC-011 про «видит назначенные курсы + pending ответы».

---

## 5) Результат по test_steps PRE-011

1. **Проверить code flow enrollment/reviewer/learning** — выполнено.
2. **Сопоставить с AC-010..AC-014** — выполнено.
3. **Зафиксировать gap и приоритеты** — выполнено (см. раздел 4).

---

## 6) Валидация тестами
- Выполнен релевантный интеграционный прогон:
  - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test`
  - Результат: **BUILD SUCCESS**, `Tests run: 3, Failures: 0, Errors: 0`.

## 7) Итог PRE-011
- FR-010..FR-014: **implemented = 1**, **partial = 4**, **missing = 0**.
- AC-010..AC-014: **implemented = 1**, **partial = 4**, **missing = 0**.
- Следующий ready-to-start шаг pre-phase по приоритету/зависимостям: **PRE-007**.