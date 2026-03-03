## TASK-018 (done) — FR-016 статистика по конкретному курсу для администратора
- Что сделано:
  - В `CourseStudentStatDto` добавлено поле `progressPercent` для явного `% выполнения` на уровне статистики курса.
  - В `StatisticsService.courseStats(Long courseId)` добавлен расчёт прогресса:
    - `progressPercent = completedLessons * 100 / totalLessons` (с защитой от деления на ноль).
  - Обновлён интеграционный тест `CourseLessonCrudIntegrationTest`:
    - в существующем сценарии `theory_lesson_completion_should_update_progress_and_stats` добавлены проверки `fullName`, `efficiencyPercent`, `progressPercent`;
    - добавлен отдельный сценарий `course_stats_should_return_progress_percent_and_match_user_stats` с двумя студентами и сверкой консистентности course-stats vs admin user-stats.
- Финальная валидация test-steps:
  - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test` → `BUILD SUCCESS`.
  - Результат: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`.
- Итоговый статус задачи:
  - `TASK-018` переведена в `done` в `memory-bank/tasks.json`.

### Копия активного контекста на момент завершения

```md
# Активный контекст

## Token Budget Gate
- Текущее значение Context Window Usage превысило порог: `364,417 / 400,000` (>350000).
- Реализация приостановлена в соответствии с `.clinerules/10-memory-bank-workflow.md`.
- Ниже зафиксирован полный контекст для продолжения в новой итерации (`/newtask`).

## Текущая задача
- ID: `TASK-018`
- Категория: `functional` (`category != ui`)
- Приоритет: `high`
- Цель: закрыть `FR-016 / AC-016` (статистика по конкретному курсу для администратора) и синхронизировать метрики с `FR-015`.

## Что уже выполнено по TASK-018

### 1) Изменения в коде
1. `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/stat/CourseStudentStatDto.java`
   - Добавлено поле:
     - `Double progressPercent`

2. `monolith-mvp/src/main/java/ru/just/monolithmvp/service/StatisticsService.java`
   - В `courseStats(Long courseId)` добавлен расчёт:
     - `progress = totalLessons == 0 ? 0D : ((double) completed * 100D) / totalLessons`
   - `progress` прокинут в конструктор `CourseStudentStatDto`.

3. `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
   - В существующий тест `theory_lesson_completion_should_update_progress_and_stats` добавлены проверки:
     - `fullName`
     - `efficiencyPercent`
     - `progressPercent`
   - Добавлен новый интеграционный тест:
     - `course_stats_should_return_progress_percent_and_match_user_stats`
     - Сценарий: 2 студента на одном курсе (один прошёл theory lesson, второй нет).
     - Проверяется `course stats` для обоих студентов:
       - `earnedPoints`, `maxPoints`, `efficiencyPercent`, `progressPercent`, `completedLessons`, `totalLessons`, `fullName`
     - Проверяется консистентность с admin user stats endpoint:
       - `GET /api/v1/admin/users/{userId}/stats`

### 2) Прогон тестов
- Выполнена команда:
  - `mvn -pl monolith-mvp -Dtest=CourseLessonCrudIntegrationTest test`
- Результат:
  - `BUILD SUCCESS`
  - `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`

## Что осталось сделать (не выполнено из-за Token Budget Gate)

1. Обновить memory-bank артефакты по завершению TASK-018:
   - `memory-bank/task-artifacts/TASK-018.md` (создать и зафиксировать выполненные изменения + результаты валидации)
   - `memory-bank/05-task-execution-progress.en.md` (добавить ссылку на артефакт TASK-018, не затирая историю)
   - `memory-bank/tasks.json` (обновить статус `TASK-018` на `done`, если ещё не обновлён)

2. Обновить `memory-bank/06-system-development-progress.md`:
   - добавить запись о завершении TASK-018 (архитектурно значимое изменение DTO+service контрактов статистики).

3. После полного завершения задачи:
   - очистить `memory-bank/02-active-context.md` согласно правилам workflow.

## Важные заметки для следующего агента
- Кодовые изменения и тесты уже готовы и зелёные; основная незавершённая часть — только memory-bank bookkeeping.
- Перед правками желательно проверить актуальный статус `TASK-018` в `memory-bank/tasks.json` (в текущей сессии поиск по regex не нашёл `status: pending`, возможно статус уже изменён ранее/вручную).
- Не делать дополнительных функциональных изменений в коде для TASK-018: задача по сути закрыта, требуется только корректно завершить документальный workflow.
```
