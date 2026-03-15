Принял `POINTS_REWORK.md` как источник требований и подготовил детальный change-plan **без кода**. Ниже — целевая модель и пошаговый план изменений по слоям (migration/model/service/DTO/tests), с учетом всех зафиксированных решений.

## 1) Целевая модель прохождения (Target State)

### 1.1. Уровень вопроса
- `OPEN_ANSWER`:
    - ревьювер может выставить `awardedPoints` в диапазоне `0..fullPoints` при `ACCEPTED`;
    - `ACCEPTED + 0` — валидно;
    - `REJECTED` — финальный провал без пересдачи;
    - `REWORK` — промежуточный статус с пересдачей (если не исчерпан лимит попыток).
- `MULTIPLE_CHOICE`:
    - `FULL`, если выбраны все правильные и нет лишних;
    - `PARTIAL`, если `missedCorrect <= 1` и `wrongSelected <= 1`, **и** количество опций `!= 3`;
    - иначе `ZERO`.
- Остальные тестовые (`SINGLE_CHOICE`, `ORDERING`): только `FULL/ ZERO`.
- `MATCHING` полностью выводится из системы (тип больше не поддерживается).

### 1.2. Уровень урока
- `completed` = урок имеет **финальный** исход (успешный или неуспешный).
- Успешность урока определяется только `status`:
    - `COMPLETE` = успешно пройден;
    - `INCOMPLETE` = финально не пройден (0 очков в зачет курса);
    - `REWORK`/`PENDING_REVIEW` = не финализирован.
- Для `PRACTICE_OPEN_ANSWER` статус `INCOMPLETE` остается с `completed=true` (как вы указали).
- Лимит попыток влияет на финализацию:
    - если попытки исчерпаны и урок не сдан — урок переводится в финальный `INCOMPLETE` (`completed=true`).

### 1.3. Уровень курса
Курс завершается только при **AND**:
1) все уроки курса финализированы (`submission.completed=true` по каждому уроку);
2) достигнут порог по баллам курса:
- `earnedCoursePoints * 100 >= maxCoursePoints * coursePassingThresholdPercent`.

Где:
- `maxCoursePoints` = сумма максимальных баллов всех уроков;
- для практики `lesson.fullPoints` синхронизируется с суммой `question.fullPoints` (авторитетная модель);
- для теории учитываются `lesson.fullPoints` как fixed points;
- неуспешно финализированный урок (`INCOMPLETE`) дает `0` в earned, но участвует в max.

### 1.4. Статусы course_progress
- `NEW`, `IN_PROGRESS`, `COMPLETED`, + новый `INCOMPLETE` (курс финализирован по урокам, но порог курса не достигнут).
- `courseCompleted` в learner API = `course_progress.status == COMPLETED`.

---

## 2) Пошаговый change-plan по коду

## Фаза A — схема БД и backfill (Flyway)

### A1. Новая миграция `V5__course_progress_threshold_and_points_alignment.sql`
Изменения:
1. `course_progress`:
    - добавить агрегатные поля:
        - `awarded_points` (int, not null default 0)
    - статусный набор в Java расширяется `INCOMPLETE` (в БД строка, check не требуется).

2. Синхронизация `lessons.full_points` для практических уроков:
    - проставить `lessons.full_points = sum(practice_questions.full_points)` для practice-уроков, на уровне java-приложения, не БД;
    - для уроков без вопросов — `0`.

3. Выпиливание `MATCHING` из данных:
    - миграционный data-fix для существующих `practice_questions.question_type='MATCHING'` НЕ ТРЕБУЕТСЯ.

4. Backfill агрегатов `course_progress`:
    - посчитать для каждой пары user-course:
        - earned points

---

## Фаза B — domain/model/repository

### B1. `CourseProgressStatus`
Файл: `model/CourseProgressStatus.java`
- добавить `INCOMPLETE`.

### B2. `CourseProgress`
Файл: `model/CourseProgress.java`
- добавить поля агрегатов из миграции:
    - `earnedPoints`.
- без audit-полей и без completion reason (по вашему решению).

### B3. `QuestionType`
Файл: `model/QuestionType.java`
- удалить `MATCHING`;

### B4. Репозитории
Файлы:
- `LessonSubmissionRepository`
- `LessonRepository`
- `CourseProgressRepository`

Добавить/уточнить методы для быстрого пересчета агрегатов:
- earned points per user-course
  (часть уже есть — использовать и дорасширить минимально).

---

## Фаза C — scoring/submission/review

### C1. `PracticeScoringPolicy`
Файл: `service/PracticeScoringPolicy.java`
- удалить ветки/поддержку `MATCHING`;
- оставить `ORDERING` с учетом порядка.

### C2. `PracticeSubmissionService`
Файл: `service/PracticeSubmissionService.java`

Изменения:
1. Лимит попыток:
- заменить «hard fail exception при превышении» на финализацию урока в `INCOMPLETE` (когда достигнут/исчерпан лимит и урок не сдан).
- `completed=true`, `pointsAwarded=0`, блок дальнейших сабмитов стандартной проверкой finalized.

2. Для тестовых уроков:
- текущий pass/fail по порогу урока оставить;
- при fail и исчерпании попыток переводить в финальный `INCOMPLETE` (`completed=true`).

3. После каждого изменения lesson submission вызывать единый пересчет course_progress (см. Фаза D).

### C3. `OpenReviewService`
Файл: `service/OpenReviewService.java`

Изменения:
1. Сохранить правило: `INCOMPLETE` для open — финальный с `completed=true`.
2. Поддержать `REJECTED` как финальный провал без rework.
3. При `REWORK`, если попыток больше нельзя — принудительно финализировать как `INCOMPLETE`.
4. После review всегда запускать пересчет course_progress.

---

## Фаза D — единый пересчет прогресса курса

### D1. Новый сервис пересчета
Новый файл (рекомендуемо): `service/CourseProgressRecalculationService.java`

Задачи сервиса:
- пересчет агрегатов и статуса курса для `userId+courseId`;
- пересчет для всех записанных на курс (`courseId`) при структурных изменениях;
- правило «пересчитываем только НЕ завершенные курсы» при изменении структуры/порогов:
    - `COMPLETED` не трогаем,
    - `NEW/IN_PROGRESS/INCOMPLETE` пересчитываем.

### D2. Логика вычисления статуса
- `totalLessons` = все уроки курса;
- `completedLessons` = уроки с финальным `submission.completed=true`;
- `maxPoints` = сумма `lesson.fullPoints`;
- `earnedPoints` = сумма `submission.pointsAwarded`;
- `progressPercent` = completed/total;
- `scorePercent` = earned/max;
- статус:
    - если нет старта/активности -> `NEW`;
    - если `completedLessons < totalLessons` -> `IN_PROGRESS`;
    - если `completedLessons == totalLessons` и threshold met -> `COMPLETED`;
    - если `completedLessons == totalLessons` и threshold not met -> `INCOMPLETE`.

### D3. Встраивание пересчета
Обновить вызовы в:
- `PracticeSubmissionService` (test/open submit, theory complete),
- `OpenReviewService` (review decisions),
- `CourseLessonAdminService` (create/update/delete lesson/questions),
- `CourseService` (изменение course threshold и др. влияющих полей),
- reset/unassign flows (`CourseEnrollmentLifecycleService`) — аккуратно, с учетом сохранения completed при unassign.

---

## Фаза E — уроки/курсы admin editing и синхронизация баллов

### E1. `CourseLessonAdminService`
Файл: `service/CourseLessonAdminService.java`

Изменения:
1. При create/update practice lesson:
- `lesson.fullPoints` рассчитывать из `sum(question.fullPoints)` и сохранять как authoritative;
- входной `fullPoints` для practice игнорировать/депрекейтить (без ломки контракта).

2. После любого изменения вопросов/порогов урока:
- пересчет `lesson.fullPoints`;
- триггер пересчета прогресса по курсу для незавершенных записей.

3. Удалить поддержку `MATCHING` в валидациях request.

### E2. `CourseService`
Файл: `service/CourseService.java`
- унифицировать дефолт `course.passingThresholdPercent` (сейчас 100 в сервисе vs 70 в entity): выбрать один (рекомендация: 70, чтобы совпадало с entity и DTO-ожиданиями).
- при изменении course-level threshold запускать массовый пересчет незавершенных прогрессов.

---

## Фаза F — learner/read/report DTO и маппинг

### F1. `LearnerLessonSummaryDto`
Файл: `dto/lesson/LearnerLessonSummaryDto.java`

Изменения:
- `passed` трактовать как `submission.status == COMPLETE` (а не `completed==true`);
- добавить поле `submissionStatus` (чтобы UI отличал `INCOMPLETE` от незавершенных статусов);
- при необходимости добавить `completed` как отдельный терминальный флаг (опционально).

### F2. `CourseLearnerDto`
Файл: `dto/course/CourseLearnerDto.java`

Изменения:
- `courseCompleted` брать из `course_progress.status == COMPLETED`;
- добавить `courseStatus`, `earnedPoints`, `maxPoints`, `scorePercent`, `progressPercent` из агрегатов course_progress.

### F3. `CourseLearnerReadService`
Файл: `service/CourseLearnerReadService.java`
- перестроить формирование learner summary:
    - успех урока по `status==COMPLETE`;
    - завершенность курса по `course_progress`;
    - completion/progress/points брать из агрегатов, а не из локального «count passed lessons».

### F4. Отчеты/статистика
Файл: `service/StatisticsReportService.java`
- учесть `CourseProgressStatus.INCOMPLETE` (новый текст статуса в отчетах);
- приоритетно использовать агрегаты из `course_progress`, чтобы убрать расхождения realtime.

---

## Фаза G — программы и доступ

### G1. `ProgramService`
Файл: `service/ProgramService.java`
- `completed` курса для program-flow остается строго `status==COMPLETED`;
- `INCOMPLETE` считать `viewed=true`, `completed=false`.

---

## 3) Полный набор интеграционных тестов (на фиксацию логики)

Ниже рекомендуемый пакет (обновление существующих + новые):

## A. Обновить существующие

1. `Task05SubmitFlowIntegrationTest`
- сохранить проверку upsert single submission;
- добавить сценарий: исчерпан `attemptLimit` -> финальный `INCOMPLETE` (`completed=true`, дальнейшие submit запрещены);
- сохранить бизнес-правило partial при 3 опциях.

2. `Task06ReviewFlowIntegrationTest`
- зафиксировать `REJECTED` как финальный провал;
- зафиксировать `INCOMPLETE` open с `completed=true`;
- обновить проверку `pointsAwarded` в финализации (после синхронизации lesson.fullPoints с вопросами — убрать ожидание cap=lesson.fullPoints).

3. `Task07StatisticsAndLearnerSummaryIntegrationTest`
- `passed` в learner summary проверять по `status==COMPLETE`;
- добавить проверку `courseStatus` и новых агрегатов из `course_progress`;
- проверить отображение `INCOMPLETE` в статистике.

## B. Новые интеграционные тесты (предлагаемые файлы)

4. `Task09CourseCompletionThresholdAndLogicIntegrationTest`
- кейс AND-логики курса:
    - все уроки завершены + threshold выполнен -> `COMPLETED`;
    - все уроки завершены + threshold НЕ выполнен -> `INCOMPLETE`;
    - threshold выполнен, но не все уроки завершены -> `IN_PROGRESS`.

5. `Task10CourseProgressRecalculationOnCourseStructureChangesIntegrationTest`
- добавление/удаление урока,
- изменение вопроса (fullPoints),
- изменение threshold курса;
- проверка пересчета **только незавершенных** course_progress.

6. `Task11LessonFullPointsSynchronizationIntegrationTest`
- create/update practice lesson -> `lesson.fullPoints == sum(question.fullPoints)` всегда;
- проверка миграционного/runtime-синка.

7. `Task12OpenReviewRejectedAndReworkAttemptLimitIntegrationTest`
- `REJECTED` финален;
- `REWORK` при исчерпанных попытках -> автопереход в `INCOMPLETE`.

8. `Task13MatchingQuestionTypeRemovalIntegrationTest`
- создание/обновление вопроса с `MATCHING` отклоняется;
- старые данные после миграции читаются без падения enum mapping.

9. `Task14LearnerCourseDtoStatusSourceIntegrationTest`
- `courseCompleted` и `courseStatus` берутся из `course_progress`, а не из простого счетчика уроков.

10. `Task15UnassignCompletedCourseDataRetentionIntegrationTest`
- unassign с завершенного курса сохраняет submissions и course_progress.

---

## 4) Порядок внедрения (безопасный rollout)

1. Миграция V5 + backfill (без изменения API поведения).
2. Domain/status + репозитории + новый recalculation service.
3. Изменения submit/review/attempt-limit.
4. Изменения admin editing + синхронизация lesson.fullPoints.
5. Learner DTO/read/report updates.
6. Program status alignment.
7. Интеграционные тесты и стабилизация.
8. Финальный прогон тестов модуля.

---

Если хотите, следующим шагом сделаю **implementation-ready execution checklist** (по PR-коммитам: PR1 schema+model, PR2 scoring+submit, PR3 progress recalculation, PR4 DTO/report, PR5 tests) с точным списком файлов по каждому PR и критериями приемки.
