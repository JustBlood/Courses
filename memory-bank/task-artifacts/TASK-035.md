# Активный контекст

## 2026-02-24 — TASK-035 (FR-113: randomQuestionCount + shuffleOnEveryAttempt)

### Статус
- Текущая активная задача: `TASK-035`.
- Основание выбора: среди `pending` задач с максимальным доступным приоритетом (`medium`) и закрытыми зависимостями выбрана задача с минимальным id (`TASK-035`), согласно `agent_instructions` в `memory-bank/tasks.json`.

### Источники требований
- `memory-bank/tasks.json` → `TASK-035`.
- `memory-bank/prd/02-functional-requirements.md` → `FR-113`.
- `memory-bank/prd/06-acceptance-criteria.md` → `AC-113`.

### Наблюдения по текущей реализации
- Поля `randomQuestionCount` и `shuffleOnEveryAttempt` уже есть в модели `PracticeLesson` и заполняются в `CourseService`.
- В runtime-выдаче урока (`LearningService.getLessonForLearner`) вопросы всегда возвращаются как полный отсортированный список, без применения `randomQuestionCount`/`shuffleOnEveryAttempt`.
- В `submitPractice` нет явной логики, завязанной на выдачу случайной выборки.

### Границы задачи
- Входит:
  - runtime-выдача вопросов для студента по правилам FR-113;
  - интеграционный тест на выдачу ограниченной выборки и перемешивание порядка.
- Не входит:
  - архитектурная переработка хранения attempt-снимков;
  - изменения API-контрактов вне нужного минимума.

### План реализации
1. Добавить в `LearningService` логику применения `randomQuestionCount` и `shuffleOnEveryAttempt` при выдаче practice-вопросов студенту.
2. Добавить интеграционный тест в `CourseLessonCrudIntegrationTest`:
   - урок с 10 вопросами;
   - `randomQuestionCount=5`, `shuffle=true`;
   - проверка, что на выдаче ровно 5 вопросов и набор/порядок меняется между попытками чтения.
3. Прогнать релевантный тестовый класс Maven.
4. После успеха — обновить task-артефакт и индексы memory-bank, перевести задачу в `done`, очистить `02-active-context.md`.

### Риски
- Вероятностная природа shuffle может дать одинаковый порядок на двух чтениях; тест нужно сделать устойчивым (несколько выборок).
- Нельзя ломать существующие тесты и текущие контракты lesson DTO.

### Что уже сделано в этой итерации
1. Полностью прочитаны обязательные правила `.clinerules/*` и стартовый контекст memory-bank.
2. Проверен `git log --oneline -20`.
3. Из `memory-bank/tasks.json` выбрана задача `TASK-035` по правилам `agent_instructions`:
   - статус `pending`;
   - категория `functional` (не `ui`);
   - приоритет `medium` (наивысший из готовых, кроме уже закрытых high/critical);
   - зависимости `TASK-011`, `TASK-033` имеют статус `done`.
4. Прочитаны релевантные PRD-главы:
   - `memory-bank/prd/02-functional-requirements.md` (FR-113),
   - `memory-bank/prd/06-acceptance-criteria.md` (AC-113).
5. Прочитаны релевантные файлы кода для анализа:
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/StudentController.java`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/lesson/CreatePracticeLessonRequest.java`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/learning/PracticeSubmissionRequest.java`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/PracticeLesson.java`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/PracticeQuestion.java`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/LessonSubmission.java`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/LessonSubmissionRepository.java`
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/lesson/LearnerPracticeQuestionDto.java`
   - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
6. Обновлён текущий файл `memory-bank/02-active-context.md` с планом реализации.

### Детальный технический вывод перед реализацией
- Настройки FR-113 уже сохраняются:
  - `PracticeLesson.randomQuestionCount`
  - `PracticeLesson.shuffleOnEveryAttempt`
  - маппинг при create/update идёт через `CourseService` (`request.randomQuestionCount()`, `request.shuffleOptions()`).
- Пробел реализации: в `LearningService.getLessonForLearner(...)` при практике возвращается полный список вопросов урока (`courseService.getPracticeQuestionForLesson(lessonId)`), без применения:
  - ограничения по `randomQuestionCount`;
  - перемешивания по `shuffleOnEveryAttempt`.
- API для студента уже подходит для runtime-выдачи (`GET /api/v1/student/lessons/{lessonId}`) и менять контракт не требуется.
- `submitPractice` принимает карту `questionAnswers`, поэтому может обрабатывать подмножество вопросов без изменения DTO.

### Запланированные кодовые изменения (следующий шаг)
1. `LearningService.getLessonForLearner(...)`:
   - добавить private-helper для применения правил выдачи practice-вопросов;
   - логика:
     - стартуем с полного списка вопросов урока;
     - если `shuffleOnEveryAttempt == true` — перемешиваем порядок;
     - если `randomQuestionCount != null && randomQuestionCount > 0 && randomQuestionCount < size` — берём первые `randomQuestionCount` после (возможного) shuffle;
     - если `randomQuestionCount >= size` — возвращаем весь список;
   - маппинг в `LearnerPracticeQuestionDto` оставить без изменений.
2. `CourseLessonCrudIntegrationTest`:
   - добавить тест сценария FR-113:
     - создать урок с 10 вопросами, `randomQuestionCount=5`, `shuffleOptions=true`;
     - несколько раз вызвать `GET /api/v1/student/lessons/{lessonId}`;
     - проверить, что каждый ответ содержит ровно 5 вопросов;
     - убедиться, что вопросные id/индексы принадлежат банку урока;
     - подтвердить факт рандомизации (устойчиво, через несколько выборок/сравнений).
3. Прогон тестов:
   - `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest test`

### Что останется после реализации
- Обновить memory-bank артефакты по задаче:
  - создать `memory-bank/task-artifacts/TASK-035.md` (с копией активного контекста);
  - добавить запись в `memory-bank/05-task-execution-progress.md`;
  - обновить `memory-bank/06-system-development-progress.md` summary;
  - перевести задачу в done через `memory-bank/change_task_status.py`;
  - очистить `memory-bank/02-active-context.md`.

### Token Budget Gate checkpoint
- Текущее значение (актуальный checkpoint): **Context Window Usage = 362,767 / 400K (> 350000)**.
- Согласно `.clinerules/10-memory-bank-workflow.md` реализация должна быть немедленно остановлена после фиксации полного контекста.

---

## Промежуточная фиксация перед обязательной остановкой (Token Budget Gate)

### Что успели сделать фактически
1. По коду выполнены изменения для `TASK-035` (FR-113):
   - runtime-выдача practice-вопросов в learner-flow теперь учитывает:
     - `shuffleOnEveryAttempt` (перемешивание порядка);
     - `randomQuestionCount` (ограничение размера выборки из банка вопросов).
2. В интеграционные тесты добавлен/обновлён сценарий `AC-113` в `CourseLessonCrudIntegrationTest`:
   - урок с 10 вопросами;
   - `randomQuestionCount=5`, `shuffle=true`;
   - проверка, что выдача содержит 5 вопросов и рандомизация действительно применяется.
3. Выполнен прогон релевантного тестового класса:
   - Команда: `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest test`
   - Результат: `BUILD SUCCESS`
   - Итог: `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`.

### Что ещё НЕ завершено (по process/memory-bank)
1. Не создан финальный task-артефакт `memory-bank/task-artifacts/TASK-035.md`.
2. Не добавлена запись о `TASK-035` в `memory-bank/05-task-execution-progress.md`.
3. Не внесена запись о завершении `TASK-035` в `memory-bank/06-system-development-progress.md`.
4. Статус задачи в `memory-bank/tasks.json` ещё `pending` (нужно перевести в `done`).
5. Не выполнена очистка `memory-bank/02-active-context.md` после полного завершения задачи (пока нельзя, задача процессно не закрыта).

### Последовательность продолжения в новой сессии (/newtask)
1. Проверить и при необходимости перечитать изменённые файлы:
   - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
   - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
2. Повторно (коротко) прогнать подтверждающий тест:
   - `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest test`
3. Создать артефакт `memory-bank/task-artifacts/TASK-035.md` на основе этого active-context.
4. Обновить индекс `memory-bank/05-task-execution-progress.md` (добавить `TASK-035`).
5. Обновить `memory-bank/06-system-development-progress.md` записью по завершённой задаче.
6. Перевести статус задачи:
   - `python memory-bank/change_task_status.py TASK-035 done`
7. После полной фиксации артефактов очистить `memory-bank/02-active-context.md` согласно правилам.

### Важные заметки для следующего агента
- Не брать новую задачу: нужно завершить текущую активную `TASK-035`.
- Не делать лишний рефакторинг: только закрытие process-части и финальная проверка.
- Ориентир по требованиям: `FR-113` / `AC-113`.