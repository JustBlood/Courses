# TASK-01 — Доменная модель и контракты (open lesson rework)

## 1) Scope и обязательные ограничения

- Документ фиксирует целевую доменную модель и API-контракты **до** реализации кода.
- Обратная совместимость не требуется.
- Внешний контракт использует **только `questionIndex`**, не `questionId`.
- Модель прогресса урока: одна актуальная запись на `(student, lesson)`.

---

## 2) Карта статусов lesson submission

## 2.1 Lesson-level статусы

- `PENDING_REVIEW` — open-урок отправлен студентом и ждёт ревью.
- `REWORK` — после ревью есть минимум один вопрос со статусом `REWORK`; урок не финализирован.
- `COMPLETE` — урок финализирован, порог прохождения выполнен.
- `INCOMPLETE` — урок финализирован, порог прохождения не выполнен.

> `REJECTED` **не является lesson-level статусом**. Он существует только на уровне вопроса в open review.

## 2.2 Финализированность урока (`completed`)

- `completed=false`: урок можно изменять по правилам текущего статуса.
- `completed=true`: урок финализирован и заблокирован для любых дальнейших student/reviewer изменений.

## 2.3 Переходы для `PRACTICE_TEST`

1. Student submit (`questionAnswers` по всем вопросам урока).
2. Рассчитывается вопросный скоринг и итог по порогу.
3. Агрегация:
   - `COMPLETE`, если порог выполнен;
   - `INCOMPLETE`, если порог не выполнен, если на уроке исчерпано количество попыток прохождения (Lesson.attemptLimit); Информация о попытке хранится в сущности submission
   - `REWORK`, если порог прохождения не выполнен, но на уроке настроено количество попыток прохождения (Lesson.attemptLimit).
4. Если статус `COMPLETE`/`INCOMPLETE` - то поле `completed=true`, дальнейшее редактирование запрещено. Если статус - `REWORK` - урок заново отображается у пользователя для прохождения.

## 2.4 Переходы для `PRACTICE_OPEN_ANSWER`

1. КАЖДЫЙ student submit должен содержать ответы на **все** вопросы урока.
2. Статус становится `PENDING_REVIEW`, `completed=false`.
3. Admin review принимает решения по **всем вопросам урока** в одном запросе.
4. Агрегация:
   - если есть хотя бы один вопрос `REWORK` → lesson status `REWORK`, `completed=false`;
   - если `REWORK` нет → урок финализируется (`completed=true`) и получает:
     - `COMPLETE` при выполненном пороге,
     - `INCOMPLETE` при невыполненном пороге.

---

## 3) Карта статусов review по вопросам

`OPEN_ANSWER`:

- `PENDING_REVIEW` — ожидает решения ревьювера.
- `ACCEPTED` — принят, баллы в диапазоне `0..fullPoints`.
- `REWORK` — отправлен на доработку, баллы всегда `0`.
- `REJECTED` — отклонён, баллы всегда `0`.


`PRACTICE_TEST` - там нет отдельных статусов. Если урок - тестовый, то он может быть перепройден только полностью - все вопросы сразу, а не отдельно как в случае с открытым вопросом.


---

## 4) Правила partial scoring для `MULTIPLE_CHOICE`

Пусть:
- `correct` — множество правильных опций;
- `selected` — множество выбранных студентом опций;
- `wrongSelected = selected - correct`;
- `missedCorrect = correct - selected`.

Тогда:

- `FULL`:
  - `selected == correct`.
- `PARTIAL`:
  - `|wrongSelected| <= 1` **и** `|missedCorrect| <= 1`,
  - и не выполняется условие `FULL`.
- `ZERO`:
  - во всех остальных случаях.

Подтверждённый edge-case:
- если в вопросе 3 правильных ответа, а выбран только 1 правильный, то
  `|missedCorrect| = 2` → `ZERO`.

---

## 5) Правила финализации урока и запрета повторной правки

## 5.1 Финализация

- Урок считается финализированным только при `completed=true`.
- Для test-урока финализация происходит сразу после submit, если уже не осталось attemptLimit.
- Для open-урока финализация происходит только после review, когда в вопросах нет `REWORK`.

## 5.2 Блокировки после финализации

- Student не может повторно отправлять ответы финализированного урока.
- Reviewer не может менять question-level решения финализированного урока.
- Финализированный open-урок не возвращается в pending-review выборку.

---

## 6) Контракты payload (формализация для последующей реализации)

## 6.1 Student submit contract

`POST /api/v1/student/lessons/{lessonId}/submit-practice`

```json
{
  "questionAnswers": {
    "0": ["Свободный текст ответа 0"],
    "1": ["Свободный текст ответа 1"],
    "2": ["Свободный текст ответа 2"]
  }
}
```

`POST /api/v1/student/lessons/{lessonId}/submit-practice`

```json
{
  "questionAnswers": {
    "0": ["A"],
    "1": ["A", "B", "C"],
    "2": ["D"]
  }
}
```

Правила:
- ключи — `questionIndex`;
- значение — `answers[]`;
- для open - вопросов: обязателен полный набор всех индексов урока в любом случае, даже если некоторые из них не в статусе `REWORK` - на беке мы будем переводить только те уроки, которые в базе имеют статус `REWORK` - какую-то ошибку выдавать не надо, валидацию будет делать фронтенд;

## 6.2 Admin review contract

### 6.2.0 Admin get lessons reviews

Выдаются все submissions доступные для проверки по-урокам

`GET /api/v1/admin/progress/reviews/pending`

ANSWER:
```json
[
    {
        "submissionId": 1,
        "lessonId": 1,
        "lessonTitle": "Урок №1",
        "courseTitle": "Курс такой-то",
        "studentId": 1,
        "studentFullname": "Зубенко Михаил Петрович"
    }
]
```

### 6.2.1 Admin get lesson review

Выдаются все открытые вопросы в рамках урока, на который сделан submission. В вопросах уже есть информация о предыдущих проверках, это нормально

`GET /api/v1/admin/progress/reviews/pending/{submissionId}`

ANSWER:
```json
[
    {
        "questionIndex": 1,
        "submissionStatus": "PENDING_REVIEW",
        "questionText": "Текст вопроса",
        "trainerHint": "Подсказка для тренера",
        "awardedPoints": 0,
        "fullPoints": 20,
        "answer": "Ответ, данный учеником (доработанный)",
        "reviewComment": "Не до конца понял твою мысль, раскрой подробнее"
    },
    {
        "questionIndex": 2,
        "submissionStatus": "ACCEPTED",
        "questionText": "Текст вопроса",
        "trainerHint": "Подсказка для тренера",
        "awardedPoints": 15,
        "fullPoints": 20,
        "answer": "Ответ, данный учеником",
        "reviewComment": "Хороший ответ, засчитано!"
    },
    {
        "questionIndex": 3,
        "submissionStatus": "REJECTED",
        "questionText": "Текст вопроса",
        "trainerHint": "Подсказка для тренера",
        "awardedPoints": 0,
        "fullPoints": 20,
        "answer": "Ответ, данный учеником",
        "reviewComment": "Ответ совсем неверный, не по теме. Ответ не засчитан"
    }
]
```

### 6.2.2 Admin post review

`POST /api/v1/admin/progress/reviews/{submissionId}`

```json
{
  "questionReviews": {
    "0": { "submissionStatus": "ACCEPTED", "awardedPoints": 10.0, "reviewComment": "Пересмотрел твой ответ - мне кажется, я переоценил тебя, поставил только 10 баллов." },
    "1": { "submissionStatus": "ACCEPTED", "awardedPoints": 13.0, "reviewComment": "Хорошо, пойдет" },
    "2": { "submissionStatus": "REJECTED", "awardedPoints": 0.0, "reviewComment": "Ответ совсем неверный, не по теме. Ответ не засчитан" }
  }
}
```

(заметь - комментария по уроку - НЕТ)

Правила:
- решения передаются по **всем вопросам урока**;
- ключи — `questionIndex`;
- `REJECTED` и `REWORK` всегда с `awardedPoints=0`.

---

## 7) Устранение противоречий по `REWORK / REJECTED / COMPLETE`

- `REWORK`:
  - только lesson-level агрегированный промежуточный статус,
  - и question-level решение ревьювера.
- `REJECTED`:
  - только question-level статус,
  - никогда не lesson-level,
  - всегда с `0` баллов.
- `COMPLETE`:
  - lesson-level статус, отображающий, что урок финализирован, пройден, т.е. за него зачислены баллы (баллы за вопросы > проходного в уроке)
- `INCOMPLETE`:
  - lesson-level статус, отображающий, что урок финализирован, не пройден, т.е. за него НЕ ЗАЧИСЛЕНЫ БАЛЛЫ (баллы за вопросы < проходного в уроке)
- `ACCEPTED`:
  - убрать из lesson-level статуса

Это снимает неоднозначность и фиксирует единую трактовку для TASK-02+.