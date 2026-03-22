# ADHOC — Learner answers visibility (DTO + LearningService) — 2026-03-09

## 1) Исходная задача

Пользователь поставил задачу:

- проанализировать текущие изменения в DTO и `LearningService`;
- обеспечить отображение пользователю его отправленных ответов;
- по возможности добавить отображение корректности/статуса и баллов по каждому вопросу (как для тестов, так и для открытых вопросов);
- оценить, нужно ли что-то ещё добавить в DTO;
- тесты разрешено **не дорабатывать в этом шаге** (отдельным следующим шагом).

## 2) Что сделано в этом шаге

### 2.1 DTO для learner-ответа урока

Обновлён `LearnerPracticeQuestionDto`:

- добавлены поля:
  - `userAnswers`
  - `correctAnswers`
  - `status` (`OpenReviewStatus`)
  - `awardedPoints`

Файл:

- `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/lesson/LearnerPracticeQuestionDto.java`

### 2.2 Логика в LearningService

В `LearningService.getLessonForLearner(...)` реализовано:

- чтение текущего submission и `questionProgress`;
- возврат `userAnswers` по каждому вопросу;
- возврат `status/awardedPoints` при `showQuestionStatus=true`;
- возврат `correctAnswers` только при:
  - `showCorrectAnswersAfterCompletion=true`, и
  - `submission.completed=true`.

Также в DTO уровня урока прокинуты оба флага видимости из `PracticeLesson`.

Файл:

- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`

## 3) Анализ "нужно ли что-то ещё добавить в DTO"

Вывод по текущему шагу:

- для заявленной цели текущего набора полей достаточно;
- обязательных дополнительных полей в DTO сейчас не требуется.

## 4) Что НЕ делалось в этом шаге

- Тесты не обновлялись — по согласованию с пользователем (следующий шаг).

## 5) Что осталось сделать (следующий шаг)

1. Обновить/добавить интеграционные тесты для learner-представления урока:
   - проверка `userAnswers`;
   - проверка условного показа `correctAnswers`;
   - проверка условного показа `status/pointsType/awardedPoints`.
2. При необходимости — согласовать с UI, нужен ли дополнительный derived-флаг `isCorrect`.

## 6) Техническая проверка

Проверка компиляции выполнена:

- `mvn -f monolith-mvp/pom.xml -DskipTests compile`
- результат: `BUILD SUCCESS`.
