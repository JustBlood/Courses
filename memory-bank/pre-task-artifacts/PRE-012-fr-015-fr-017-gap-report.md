# PRE-012 — Сверка FR-015..FR-017 (statistics/reporting)

## 1) Контекст
- Задача: `PRE-012` из `memory-bank/pre-tasks.json`.
- Область: реализация статистики и отчётов в `monolith-mvp`.
- Релевантные требования: `FR-015..FR-017`, `AC-015..AC-017`.

Ключевые проверенные файлы:
- `controller/StudentController.java`
- `controller/ProgressController.java`
- `service/StatisticsService.java`
- `dto/stat/StudentCourseStatDto.java`
- `dto/stat/CourseStudentStatDto.java`
- `dto/stat/ReportRowDto.java`
- `repository/EnrollmentRepository.java`
- `repository/LessonSubmissionRepository.java`
- `model/Enrollment.java`, `model/AppUser.java`

## 2) Матрица соответствия FR/AC

| ID | Статус | Наблюдение |
|---|---|---|
| FR-015 | partial | Есть личная статистика студента (`GET /api/v1/student/my/stats`) и расчёт `efficiency = earned*100/max`. Для админа статистика конкретного пользователя как отдельный endpoint отсутствует. `% выполнения` не отдается отдельным полем (только `completedLessons/totalLessons`). |
| AC-015 | partial | Часть условий выполнена (курс/баллы/эффективность, базовые данные для прогресса), но нет явного admin-view «по выбранному пользователю» и нет явного поля `% выполнения` в контракте ответа. |
| FR-016 | partial | Есть статистика по курсу для админа (`GET /api/v1/admin/progress/courses/{courseId}/stats`) с ФИО/баллами/эффективностью и данными завершения уроков. `% выполнения` не представлен отдельным вычисленным полем. |
| AC-016 | partial | Список студентов и ключевые метрики есть, но формат не полностью соответствует требованию по явному `% выполнения` как метрике отчёта. |
| FR-017 | partial | Есть только один CSV endpoint (`GET /api/v1/admin/progress/reports/summary.csv`). Отдельного отчёта по конкретному курсу нет, набор и порядок колонок существенно отличаются от PRD. |
| AC-017 | missing | Критерий структуры колонок PRD (для course-report и global-report, включая обязательные пустые поля) не выполняется в текущем контракте CSV. |

## 3) Детализация по формулам и колонкам

### 3.1 Формулы (FR-015/016)
- Реализовано в `StatisticsService`:
  - `efficiency = earnedPoints * 100 / maxPoints` — соответствует PRD.
  - Основа для `% выполнения` есть: `completedLessons`, `totalLessons`.
- Gap:
  - `% выполнения = completedLessons / totalLessons * 100` не возвращается как явное значение в DTO.

### 3.2 Колонки отчётов (FR-017)
- Текущий CSV-заголовок:
  - `fullName,email,login,lang,course,earnedPoints,maxPoints,efficiency,enrolledAt,startedAt,completedAt`
- Основные расхождения с PRD:
  1. Нет отдельного отчёта по конкретному курсу.
  2. Нет требуемого полного набора колонок и порядка для course/global отчётов.
  3. Поля, которые по PRD должны быть стабильно пустыми (`login/cid/...`), не соблюдены (например, `login` заполняется из email).

## 4) Покрытие тестами

По текущему integration-набору (`src/test/java`) проверки `FR-015..FR-017` не обнаружены:
- нет тестов на формулы статистики,
- нет тестов на структуру/колонки CSV-отчётов,
- нет тестов на admin-view личной статистики пользователя.

## 5) Приоритетные gaps

### High
1. `AC-017`: реализовать два отчётных контракта (по курсу + общий) со строгим соответствием колонок PRD.
2. `FR-015`: добавить admin endpoint личной статистики выбранного пользователя.

### Medium
1. Добавить в DTO явный `completionPercent` для `FR-015/FR-016` (или формально зафиксировать вычисление на клиенте и синхронизировать PRD/контракт).
2. Добавить integration-тесты на `AC-015..AC-017`.

## 6) Проверка test_steps PRE-012

- Шаг 1: Проверить `StatisticsService` и DTO отчётов — ✅
- Шаг 2: Сопоставить формулы/колонки с `PRD-02` и `PRD-06` — ✅
- Шаг 3: Зафиксировать расхождения в отчёте — ✅
