# PRE-010 — Сверка реализации FR-005..FR-009 (courses/lessons/questions/scoring)

## 1) Контекст и границы проверки
- Источник задачи: `memory-bank/pre-tasks.json` → `PRE-010`.
- Цель: проверить соответствие реализации требованиям `FR-005..FR-009` и критериям `AC-005..AC-009`.
- Проверенные требования:
  - `memory-bank/prd/02-functional-requirements.md` (FR-005..FR-009)
  - `memory-bank/prd/06-acceptance-criteria.md` (AC-005..AC-009)
- Проверенные ключевые файлы реализации:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/CoursesController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/StudentController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/{Course,Lesson,TheoryLesson,PracticeLesson,PracticeQuestion,LessonType,TheoryContentType,QuestionType}.java`
  - `monolith-mvp/src/main/resources/db/migration/V1__init_schema.sql`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`

---

## 2) Матрица соответствия FR

| FR | Статус | Наблюдения |
|---|---|---|
| FR-005 (создание/редактирование курса) | **implemented** | Есть полноценный CRUD курса (`POST/PUT/DELETE/GET /api/v1/admin/courses`). Поля из PRD поддержаны: `title`, `description`, `coverFilePath`, `passingThresholdPercent`, `deadlineDays`, `authorFullName`, `lessonsFreeOrder`, `allowContinueAfterFail`, `createdByAdminId` (проставляется из текущего admin в сервисе). Курс отображается в каталоге (`GET /api/v1/admin/courses`). |
| FR-006 (структура уроков THEORY/PRACTICE) | **implemented** | Реализованы отдельные контуры создания `THEORY` и `PRACTICE` уроков. На уровне модели/enum поддерживается разделение на подтипы теории и практики, валидация не допускает non-practice `lessonType` в practice endpoint. |
| FR-007 (форматы THEORY: text/YouTube/PDF) | **implemented** | `TheoryContentType` поддерживает `HTML_TEXT`, `VIDEO_URL`, `PDF_FILE`; для студента формат и контент возвращаются через `GET /api/v1/student/lessons/{lessonId}`. |
| FR-008 (типы вопросов практики) | **partial** | Модель и валидация поддерживают типы `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `MATCHING`, `ORDERING`, `OPEN_ANSWER`. Но runtime-прохождение реализовано упрощённо: `submitPractice` фактически проверяет только первый вопрос урока и использует payload без структуры ответов по всем вопросам. Для open-answer логика завязана на `lessonType == PRACTICE_OPEN_ANSWER`, а не на mixed-набор вопросов. |
| FR-009 (балльная модель: default + lesson/question + partial scoring) | **partial** | Дефолты и переопределения на уровне lesson/question есть в модели/DTO. Частичная оценка для `MULTIPLE_CHOICE` реализована. Ключевой gap: при проверке используются в основном `lesson.fullPoints/partialPoints`, а не агрегирование по всем вопросам с учётом per-question баллов; нет полного расчёта результата по question pool и порогу урока. |

---

## 3) Матрица соответствия AC

| AC | Статус | Доказательства / комментарий |
|---|---|---|
| AC-005 (создание/редактирование курса и доступность в каталоге) | **implemented** | Подтверждается `CoursesController` + `CourseService` и интеграционными сценариями `CourseLessonCrudIntegrationTest` (создание/обновление/чтение курса). |
| AC-006 (добавление урока с допустимыми типами THEORY/PRACTICE) | **implemented** | Есть отдельные endpoint под theory/practice, корректное сохранение структуры уроков в курсе, включая reorder и удаление. |
| AC-007 (отображение THEORY в форматах text/YouTube/PDF) | **implemented** | Форматы поддерживаются на уровне `TheoryContentType`, lesson DTO и learner endpoint. |
| AC-008 (все типы вопросов сохраняются и доступны в режиме прохождения) | **partial** | Типы сохраняются, но прохождение покрывает только ограниченный сценарий проверки (по сути single question pipeline), что не закрывает полноценно режим прохождения для много-вопросных и mixed-type уроков. |
| AC-009 (применение default/individual scoring и partial scoring) | **partial** | Частичная оценка присутствует, но итоговый scoring engine не учитывает полноценно question-level настройки и порог прохождения по всему уроку. |

---

## 4) Выявленные gaps и потенциальные точки миграций

1. **GAP-CL-001 (High): неполная модель прохождения practice-урока**
   - `PracticeSubmissionRequest` не позволяет передавать ответы по набору вопросов (структурно только `selectedAnswers`/`openAnswer`).
   - `LearningService.submitPractice` проверяет только первый вопрос урока.

2. **GAP-CL-002 (High): scoring engine частично использует lesson-level баллы**
   - Per-question `fullPoints/partialPoints` сохраняются, но не участвуют полноценно в итоговом подсчёте.
   - Нет полного агрегирования результата по всем вопросам урока.

3. **GAP-CL-003 (Medium): threshold-параметры не доведены до полного бизнес-применения в FR-005..009 контуре**
   - `Course.passingThresholdPercent` и `PracticeLesson.passingThresholdPercent` сохраняются, но в текущем проверочном потоке отсутствует целостный расчёт «урок/курс пройден по порогу» для multi-question сценариев.

4. **GAP-CL-004 (Medium): контрактные/DTO-несоответствия**
   - В запросах уроков есть поля `coverFilePath`, `requiresPreviousCompleted`, `openForAccess`, но они не отражены в model и фактически не применяются.
   - Именование `shuffleOptions` в request маппится на `shuffleOnEveryAttempt` в модели (функционально близко, но терминологически расходится с PRD).

5. **Потенциальные точки миграций/эволюции схемы**
   - При переходе к полноценному multi-question scoring может потребоваться нормализация хранения ответов (декомпозиция `answer_raw`) и/или детализация attempt-level сущностей.
   - Возможна миграция для удаления/добавления колонок под реально используемые lesson-параметры (сейчас часть контрактных полей не имеет отражения в entity).

---

## 5) Результат по test_steps PRE-010

1. **Проверить модели/сервисы/endpoint блока courses/lessons** — выполнено.
2. **Сопоставить с AC-005..AC-009** — выполнено.
3. **Зафиксировать результаты и пробелы** — выполнено (см. матрицы и gaps выше).

---

## 6) Валидация тестами
- Выполнен релевантный интеграционный прогон:
  - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test`
  - Результат: **BUILD SUCCESS**, `Tests run: 3, Failures: 0, Errors: 0`.

## 7) Итог PRE-010
- FR-005..FR-009: **implemented = 3**, **partial = 2**, **missing = 0**.
- AC-005..AC-009: **implemented = 3**, **partial = 2**, **missing = 0**.
- Ключевой фокус следующих итераций: довести practice runtime и scoring до полного соответствия FR-008/FR-009.