# TASK-036 — FR-114 / AC-114: stop-lesson блокировка

## Контекст задачи
- Категория: `functional`
- Приоритет: `medium`
- Источник: `memory-bank/tasks.json` (`TASK-036`)
- Требования: `FR-114` (`memory-bank/prd/02-functional-requirements.md`), `AC-114` (`memory-bank/prd/06-acceptance-criteria.md`)

## Цель
Реализовать backend-блокировку перехода к следующему уроку, если есть непройденный предыдущий урок с `stopLesson=true`, включая случай `course.lessonsFreeOrder=true`, с anti-bypass защитой на чтение и действия прохождения.

## Реализация

### 1) Service-guard в LearningService
Файл:
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`

Сделано:
1. Подключена зависимость `LessonRepository`.
2. Добавлен приватный guard:
   - `assertStopLessonAccessAllowed(Long studentId, Lesson lesson)`
3. Guard встроен в три критические точки:
   - `getLessonForLearner(...)`
   - `completeTheoryLesson(...)`
   - `submitPractice(...)`
4. Если найден непройденный stop-урок до целевого, выбрасывается:
   - `BadRequestException("Previous stop lesson is not passed")`

### 2) Репозиторная проверка (использована)
Файл:
- `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/LessonRepository.java`

Использован JPQL-метод:
- `existsUnpassedStopLessonBeforePosition(Long courseId, Long studentId, Integer targetPosition)`

Логика запроса:
- ищет в рамках курса все уроки с `position < targetPosition` и `stopLesson=true`,
- проверяет отсутствие passed-submission студента по таким урокам,
- возвращает `true`, если хотя бы один блокирующий stop-урок найден.

## Интеграционное тестирование (AC-114)
Файл:
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`

Добавлен тест:
- `stop_lesson_should_block_next_lessons_even_when_lessons_free_order_enabled`

Покрытые сценарии:
1. Курс с `lessonsFreeOrder=true`.
2. Первый урок — THEORY с `stopLesson=true`.
3. До прохождения stop-урока блокируется:
   - чтение следующего урока (`GET /student/lessons/{id}`),
   - завершение следующего THEORY (`POST /complete-theory`),
   - отправка следующего PRACTICE (`POST /submit-practice`).
4. После прохождения stop-урока доступ к следующим урокам открывается.

## Проверка
Команда:
- `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest test`

Результат:
- `BUILD SUCCESS`
- `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`

## Изменённые файлы
1. `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
2. `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`

## Итог
Требования `FR-114` и `AC-114` реализованы полностью: stop-lesson действительно блокирует переход к следующим урокам до прохождения текущего stop-урока, включая режим свободного порядка уроков (`lessonsFreeOrder=true`), и это подтверждено интеграционным тестом.