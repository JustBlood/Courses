# PRE-004 — Сверка схемы БД и миграций Flyway с entity и PRD-полями

## 1) Контекст задачи
- Задача: `PRE-004` из `memory-bank/pre-tasks.json`.
- Цель: проверить цепочку миграций Flyway, сопоставить таблицы/колонки со слоем `model` и обязательными полями PRD, зафиксировать gap-таблицу.
- Область анализа:
  - `monolith-mvp/src/main/resources/db/migration/V1__init_schema.sql`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/*`
  - `memory-bank/prd/02-functional-requirements.md`
  - `memory-bank/prd/06-acceptance-criteria.md`

## 2) Проверка цепочки миграций Flyway

### 2.1 Наличие и последовательность миграций
- В каталоге миграций обнаружен один скрипт: `V1__init_schema.sql`.
- Разрывов в нумерации между версиями нет (цепочка состоит из одного baseline-скрипта).

### 2.2 Конфигурация применения миграций
- В `application.yml`:
  - `spring.flyway.enabled=true`
  - `spring.flyway.locations=classpath:db/migration`
  - `spring.flyway.baseline-on-migrate=true`

### 2.3 Целостность `flyway_schema_history`
- Прямой runtime-снимок таблицы `flyway_schema_history` в рамках PRE-004 не снимался (анализ выполнен статически по миграциям/конфигу).
- По структуре проекта ожидается регистрация единственной версии `V1` при первом запуске Flyway.
- Явных признаков конфликтной цепочки (дубли версий, out-of-order, повторяющиеся номера) в репозитории не обнаружено.

## 3) Сопоставление схемы с domain-моделями

Статусы в таблице:
- `OK` — схема и entity согласованы по назначению полей.
- `PARTIAL` — есть отличия, но базовая работоспособность контура вероятна.
- `GAP` — существенное расхождение (недостающие/лишние колонки, риск runtime ошибок).

| Таблица | Entity | Статус | Наблюдение |
|---|---|---|---|
| `users` | `AppUser` | PARTIAL | Основные поля FR-001 покрыты. В БД есть доп. колонка `lang`, отсутствующая в entity (избыточная относительно текущей модели). |
| `courses` | `Course` | OK | Поля курса из FR-005 присутствуют (`title`, `description`, `author_full_name`, `cover_file_path`, `passing_threshold_percent`, `deadline_days`, `lessons_free_order`, `allow_continue_after_fail`, `created_by_admin_id`) + расширения (`deadline_at`, `block_after_deadline`, `keep_access_after_deadline`, `include_in_overall_stats`). |
| `enrollments` | `Enrollment` | OK | Поля и уникальность `(user_id, course_id)` соответствуют модели FR-010. |
| `lessons` | `Lesson` + `TheoryLesson` + `PracticeLesson` | PARTIAL | Базовые поля и inheritance (`lesson_kind`) согласованы. Для theory-полей в БД допускается `NULL`, при этом в `TheoryLesson` стоит `nullable=false` (строже на уровне JPA). |
| `practice_questions` | `PracticeQuestion` | PARTIAL | Состав колонок покрывает модель FR-008/FR-009, но `question_type` в БД nullable, в модели семантически ожидается обязательность типа. |
| `lesson_submissions` | `LessonSubmission` | OK | Поля сабмишнов и ревью покрыты, соответствуют текущему workflow реализации. |
| `password_setup_tokens` | `PasswordSetupToken` | OK | Токенная таблица соответствует модели и FR-002/FR-019 контурам. |
| `learning_groups` | `LearningGroup` | OK | UUID PK, `title`, `type` соответствуют модели FR-106; доп. уникальный индекс `title+type` валиден. |
| `group_memberships` | `GroupMembership` | OK | Связка user-group и уникальность пары присутствуют, соответствует FR-107. |
| `course_reviewers` | `CourseReviewer` | OK | Связка course-reviewer и уникальность пары соответствуют FR-011. |
| `learning_programs` | `LearningProgram` | GAP | В entity есть `deadlineAt` и `blockAfterDeadline`, но в таблице `learning_programs` этих колонок нет. Высокий риск runtime-падений при работе с программами. |
| `program_courses` | `ProgramCourse` | GAP | В таблице есть `deadline_at` и `block_after_deadline`, но в entity `ProgramCourse` этих полей нет. Также `block_after_deadline` объявлен `not null` без default, что создаёт риск INSERT-ошибок. |
| `program_enrollments` | `ProgramEnrollment` | OK | Поля и уникальность соответствуют модели FR-105. |

## 4) Сверка обязательных PRD-полей (фокус PRE-004)

### 4.1 FR-001 (пользователь)
- Обязательные профильные поля из PRD присутствуют в `users`.
- Дополнительное поле `lang` в PRD не требуется (избыточность схемы).

### 4.2 FR-005 (курс)
- Обязательные поля курса присутствуют в `courses`.
- Имеются расширения post-MVP/операционного характера, не противоречащие PRD.

### 4.3 FR-104/FR-105 (программы)
- Зафиксировано ключевое расхождение размещения дедлайн-параметров:
  - модель `LearningProgram` хранит дедлайн и блокировку на уровне программы;
  - схема содержит дедлайн/блокировку на уровне `program_courses`.
- Требуется нормализация схемы и entity-контракта в одном направлении.

## 5) Gap-таблица по миграциям (требуемый итог PRE-004)

| Gap ID | Тип | Severity | Описание | Рекомендуемое действие |
|---|---|---|---|---|
| DB-GAP-004-01 | Missing migration / schema-entity mismatch | High | В `learning_programs` отсутствуют колонки `deadline_at`, `block_after_deadline`, которые есть в `LearningProgram`. | Добавить миграцию `V2` с `ALTER TABLE learning_programs ADD ...` (или синхронно изменить модель, если выбран иной источник истины). |
| DB-GAP-004-02 | Redundant/misaligned columns | High | В `program_courses` есть `deadline_at`, `block_after_deadline`, отсутствующие в `ProgramCourse`; `block_after_deadline` not null без default. | Либо добавить поля в entity и сервисную логику, либо вынести/удалить колонки отдельной миграцией по целевой модели. |
| DB-GAP-004-03 | Redundant column | Low | Колонка `users.lang` отсутствует в `AppUser` и не фигурирует в PRD-02. | Решить судьбу поля: либо добавить в модель/DTO, либо задеприкейтить и удалить миграцией после проверки использования. |
| DB-GAP-004-04 | Constraint alignment | Medium | Для `lessons.theory_content_type/theory_content` в БД nullable, в `TheoryLesson` ожидается обязательность. | Уточнить инвариант и при необходимости ужесточить schema-constraint миграцией. |
| DB-GAP-004-05 | Constraint alignment | Low | `practice_questions.question_type` в БД nullable, для аналитики/валидации лучше обязательное значение. | При подтверждении бизнес-правила — добавить `NOT NULL` миграцией и очистить legacy-данные. |

## 6) Проверка test_steps PRE-004

- Шаг 1: Проанализировать migration scripts и итоговую схему — **выполнен**.
- Шаг 2: Сопоставить схему с полями domain-моделей — **выполнен**.
- Шаг 3: Зафиксировать gap-таблицу по миграциям — **выполнен**.

Итог: артефакт PRE-004 сформирован.