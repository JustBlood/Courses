# TASK-034 — FR-112: ограничения времени на курс и урок

## Контекст и цель
- Задача: реализовать `FR-112 / AC-112`.
- Требования:
  - ограничение времени на курс через `deadlineDays`;
  - ограничение времени на practice-урок через `timeLimitMinutes`.

## Реализованные изменения

### 1) Course deadline (`deadlineDays`)
- Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
- Добавлен метод:
  - `assertCourseDeadlineNotExceededForStudent(Long userId, Long courseId)`
- Логика:
  - ищет enrollment пользователя по курсу;
  - если `deadlineDays == null || deadlineDays <= 0` — ограничение отключено;
  - иначе вычисляет дедлайн относительно `enrolledAt`;
  - при превышении возвращает `BadRequestException("Course deadline exceeded")`.
- Проверка применена в:
  - `getCourseForLearner(...)`;
  - `LearningService.completeTheoryLesson(...)`;
  - `LearningService.submitPractice(...)`.

### 2) Lesson time limit (`timeLimitMinutes`)
- Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/LessonSubmissionRepository.java`
- Добавлен метод:
  - `findFirstByStudentIdAndLessonIdOrderBySubmittedAtAsc(...)`

- Файл: `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
- Добавлен метод `validatePracticeTimeLimit(...)`:
  - при `timeLimitMinutes == null || <= 0` ограничений нет;
  - точка отсчёта — время первой попытки по уроку;
  - если текущее время вышло за окно `firstAttempt.submittedAt + timeLimitMinutes`, возвращается
    `BadRequestException("Time limit exceeded for this lesson")`.
- Вызов добавлен в `submitPractice(...)` перед сохранением новой попытки.

## Тестирование
- Прогнан релевантный набор:
  - `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest test`
- Результат:
  - `BUILD SUCCESS`
  - `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`.

## Интеграционный тест под AC-112
- Файл: `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
- Добавлен тест:
  - `time_limits_should_block_after_course_deadline_and_practice_time_limit()`
- Покрыто:
  1. Курс с `deadlineDays=1`, искусственное старение `enrollment.enrolledAt` => блокировка:
     - `GET /api/v1/student/courses/{id}` -> `400`;
     - `POST /api/v1/student/lessons/{id}/complete-theory` -> `400`.
  2. Practice-урок с `timeLimitMinutes=1`, после истечения окна второй submit блокируется:
     - `POST /api/v1/student/lessons/{id}/submit-practice` -> `400`.

## Итог
- `FR-112 / AC-112` реализованы в текущем backend-контуре.
- Изменения ограничены целевыми файлами без лишнего рефакторинга.