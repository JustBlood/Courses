## TASK-043 (done) — API-модель двух списков enrolled/not-enrolled для курса (FR-010/AC-010)

### Цель
Подтвердить соответствие реализации требованиям FR-010/AC-010:
- наличие явного контракта двух списков `enrolled` / `notEnrolled`;
- корректное перемещение пользователей между списками;
- отсутствие дублей и корректность операций назначения/отчисления.

### Что подтверждено
- В коде уже реализованы:
  - DTO: `CourseEnrollmentListsDto`;
  - endpoint списка: `GET /api/v1/admin/courses/{courseId}/enrollments/lists`;
  - batch-операции: `POST/DELETE /api/v1/admin/courses/{courseId}/enrollments`.
- Функциональность соответствует FR-010/AC-010, дополнительных правок backend-кода не потребовалось.

### Валидация test_steps
- Запущен релевантный тест:
  - `mvn -f monolith-mvp/pom.xml -Dtest=CourseLessonCrudIntegrationTest#enrollment_two_lists_flow_should_work test`
- Результат: `BUILD SUCCESS`, `Tests run: 1, Failures: 0, Errors: 0`.

### Изменения в backlog/memory-bank
- `memory-bank/tasks.json`: статус `TASK-043` переведён в `done`.
- `memory-bank/02-active-context.md`: промежуточный и финальный контекст по задаче.

### Итог
Задача закрыта документально: реализация была уже в кодовой базе и подтверждена интеграционным тестом.