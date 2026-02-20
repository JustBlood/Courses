# PRE-016 — Gap-analysis по FR-110..FR-114 / AC-110..AC-114

## 1) Контекст задачи
- **ID:** PRE-016
- **Область:** advanced practice (`threshold / attemptLimit / time / random / stopLesson`)
- **Цель:** сверить фактическую реализацию в `monolith-mvp` с требованиями:
  - `FR-110..FR-114` из `memory-bank/prd/02-functional-requirements.md`
  - `AC-110..AC-114` из `memory-bank/prd/06-acceptance-criteria.md`

## 2) Проверенные источники
- Модели:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/Lesson.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/PracticeLesson.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/Course.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/LessonSubmission.java`
- DTO/маппинг:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/lesson/CreatePracticeLessonRequest.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/lesson/LessonDto.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/mapper/LessonMapper.java`
- Бизнес-логика:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/StudentController.java`
- Репозитории:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/LessonRepository.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/LessonSubmissionRepository.java`
- Тесты:
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`

## 3) Матрица соответствия FR/AC

| FR | AC | Статус | Вывод |
|---|---|---|---|
| FR-110 (порог прохождения practice) | AC-110 | **partial** | Параметр `passingThresholdPercent` сохраняется в `PracticeLesson`, но при проверке `submitPractice` не используется для определения `passed`/`status`. Логика фактически бинарная по корректности ответа (и в основном по первому вопросу). |
| FR-111 (лимит попыток) | AC-111 | **partial** | Поле `attemptLimit` хранится в `Lesson` и проходит через DTO/service, но в `LearningService.submitPractice` нет проверки исчерпания попыток и запрета следующей попытки. |
| FR-112 (ограничение времени курс/урок) | AC-112 | **partial** | Поля `deadlineDays` (курс) и `timeLimitMinutes` (урок) в модели/DTO есть, но runtime-ограничения при прохождении уроков/попыток не применяются (нет проверки истечения времени и соответствующего статуса). |
| FR-113 (рандомизация/выборка вопросов) | AC-113 | **partial** | Параметры `randomQuestionCount` и `shuffleOnEveryAttempt` сохраняются, но выдача задания и автопроверка не используют их: студенту возвращается полный список вопросов, проверка в submit-flow идёт по первому вопросу без random/shuffle-логики. |
| FR-114 (`stopLesson` + приоритет над `lessonsFreeOrder`) | AC-114 | **missing** | `stopLesson` и `lessonsFreeOrder` сохраняются в сущностях, но нет серверной логики блокировки перехода к следующему уроку до прохождения stop-урока, включая приоритет над свободным порядком. |

Итог по блоку `FR-110..FR-114`:
- **implemented:** 0
- **partial:** 4
- **missing:** 1

Итог по блоку `AC-110..AC-114`:
- **implemented:** 0
- **partial:** 4
- **missing:** 1

## 4) Детализация ключевых gap

### GAP-1: Threshold не участвует в принятии решения о прохождении (FR-110)
- `PracticeLesson.passingThresholdPercent` хранится и маппится.
- В `LearningService.submitPractice` отсутствует расчёт процента/баллов относительно порога урока.
- Текущее поведение: `passed=true` только при `correct=true` (для одного проверяемого вопроса), иначе `INCOMPLETE`.

### GAP-2: Attempt limit не enforced (FR-111)
- `Lesson.attemptLimit` сохраняется (`CourseService.applyCommonLessonFields`).
- Нет запроса/проверки количества попыток пользователя по уроку перед созданием новой `LessonSubmission`.
- Ограничение «следующая попытка недоступна» не реализовано.

### GAP-3: Time limits не применяются в runtime (FR-112)
- Данные о времени есть (`Course.deadlineDays`, `Lesson.timeLimitMinutes`).
- Нет механизма старта/истечения таймера попытки и реакции на таймаут.
- Нет блокировки/изменения статуса по дедлайну курса при прохождении.

### GAP-4: Random/shuffle не влияют на выдачу и проверку (FR-113)
- `PracticeLesson.randomQuestionCount` / `shuffleOnEveryAttempt` сохраняются.
- `getLessonForLearner` возвращает вопросы в фиксированном порядке из БД.
- `submitPractice` проверяет только минимальный `questionIndex`, без индивидуального набора вопросов попытки.

### GAP-5: Stop-lesson правило отсутствует (FR-114)
- Есть поле `Lesson.stopLesson` и `Course.lessonsFreeOrder`.
- Нет проверки «можно ли открыть следующий урок» на основе статуса текущего stop-урока.
- В `StudentController/LearningService` отсутствует gate-логика перед выдачей урока.

## 5) Проверка test_steps PRE-016

1. **Шаг 1: Проверить practice-модели и LearningService-логику** — выполнено.
2. **Шаг 2: Сопоставить поведение с AC-110..AC-114** — выполнено.
3. **Шаг 3: Зафиксировать partial/missing по каждому параметру** — выполнено (см. матрицу выше).

Дополнительно выполнен релевантный интеграционный прогон:
- `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` → **BUILD SUCCESS**.

## 6) Рекомендации для последующих backlog-задач
- Приоритетно закрыть runtime enforcement для `FR-110/111` (блокирующие параметры practice-прохождения).
- Реализовать attempt-session/attempt-state для `FR-112/113` (время + набор вопросов конкретной попытки).
- Добавить server-side gate проверки переходов между уроками для `FR-114` (включая приоритет над `lessonsFreeOrder`).
- Добавить интеграционные тесты на AC-110..AC-114 (сценарии лимита попыток, таймаута, randomQuestionCount, stopLesson).
