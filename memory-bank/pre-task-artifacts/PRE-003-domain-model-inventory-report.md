# PRE-003 — Инвентаризация доменных моделей (entity/enum/repository/mapper)

## 1) Контекст задачи
- Задача: `PRE-003` из `memory-bank/pre-tasks.json`.
- Цель: зафиксировать карту доменных сущностей и связей, сопоставить с repository/mapper слоями и отметить соответствие PRD.
- Область анализа:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/*`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/*`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/mapper/*`

## 2) Инвентаризация model-пакета

### 2.1 Entity-классы
- `AppUser`
- `Course`
- `Lesson` (base, single-table inheritance)
- `TheoryLesson` (наследник `Lesson`)
- `PracticeLesson` (наследник `Lesson`)
- `PracticeQuestion`
- `LessonSubmission`
- `Enrollment`
- `CourseReviewer`
- `LearningGroup`
- `GroupMembership`
- `LearningProgram`
- `ProgramCourse`
- `ProgramEnrollment`
- `PasswordSetupToken`

### 2.2 Enum-классы
- `Role`
- `LessonType` (+ nested `LessonSubType`)
- `TheoryContentType`
- `QuestionType`
- `SubmissionStatus`
- `GroupType`
- `ProgramAccessCondition`

## 3) Ключевые связи между сущностями

- `Course (1) -> (N) Lesson`
- `Lesson (N) -> (1) Course`
- `PracticeLesson (1) -> (N) PracticeQuestion`
- `PracticeQuestion (N) -> (1) PracticeLesson`
- `LessonSubmission (N) -> (1) Lesson`
- `LessonSubmission (N) -> (1) AppUser(student)`
- `Enrollment (N) -> (1) AppUser` и `Enrollment (N) -> (1) Course`
- `CourseReviewer (N) -> (1) Course` и `CourseReviewer (N) -> (1) AppUser(reviewer)`
- `GroupMembership (N) -> (1) LearningGroup` и `GroupMembership (N) -> (1) AppUser`
- `ProgramCourse (N) -> (1) LearningProgram` и `ProgramCourse (N) -> (1) Course`
- `ProgramEnrollment (N) -> (1) LearningProgram` и `ProgramEnrollment (N) -> (1) AppUser`
- `PasswordSetupToken (N) -> (1) AppUser`

Примечания по ограничениям:
- Есть уникальные ограничения на ключевых join-таблицах (`enrollments`, `course_reviewers`, `group_memberships`, `program_courses`, `program_enrollments`).
- `Lesson` реализован через single-table inheritance с discriminator `lesson_kind`.

## 4) Сопоставление с repository-слоем

### 4.1 Сущности с явными репозиториями
- `AppUser` -> `AppUserRepository`
- `Course` -> `CourseRepository`
- `Lesson` -> `LessonRepository`
- `PracticeQuestion` -> `PracticeQuestionRepository`
- `LessonSubmission` -> `LessonSubmissionRepository`
- `Enrollment` -> `EnrollmentRepository`
- `CourseReviewer` -> `CourseReviewerRepository`
- `LearningGroup` -> `LearningGroupRepository`
- `GroupMembership` -> `GroupMembershipRepository`
- `LearningProgram` -> `LearningProgramRepository`
- `ProgramCourse` -> `ProgramCourseRepository`
- `ProgramEnrollment` -> `ProgramEnrollmentRepository`
- `PasswordSetupToken` -> `PasswordSetupTokenRepository`

### 4.2 Сущности без отдельного репозитория (обслуживаются через базовые)
- `TheoryLesson` -> через `LessonRepository`
- `PracticeLesson` -> через `LessonRepository`

## 5) Сопоставление с mapper-слоем

- `AppUser` -> `UserMapper` -> `UserDto`
- `Course` -> `CourseMapper` -> `CourseDto`
- `Lesson`/`TheoryLesson`/`PracticeLesson`/`PracticeQuestion` -> `LessonMapper` -> `LessonDto`/`PracticeQuestionDto`

Частичное покрытие mapper-слоя:
- Для `Enrollment`, `CourseReviewer`, `LearningGroup`, `GroupMembership`, `LearningProgram`, `ProgramCourse`, `ProgramEnrollment`, `PasswordSetupToken`, `LessonSubmission` отдельных mapstruct-mapper интерфейсов не обнаружено (вероятно, ручной маппинг в service-слое).

## 6) Матрица соответствия модели PRD (PRE-003 итог)

| Область | Статус | Наблюдение |
|---|---|---|
| Users (`AppUser`) | соответствует | Поля FR-001 в целом покрыты (`fullName`, `email`, `role`, `enabled`, `phone`, `comment`, `createdAt`, `createdBy`, `lastVisit`, `deactivatedAt`, `deactivatedBy`, `avatarFilePath`). |
| Courses (`Course`) | частично | Ядро FR-005 покрыто; есть расхождение в нейминге (`passingThresholdPercent` vs порог прохождения), плюс дополнительные поля вне PRD (`keepAccessAfterDeadline`, `includeInOverallStats`, `deadlineAt`, `blockAfterDeadline`). |
| Lessons (`Lesson` + heirs) | соответствует | Поддержаны THEORY/PRACTICE через иерархию и типы контента theory. |
| Practice questions (`PracticeQuestion`, `QuestionType`) | соответствует | Поддерживаются типы SINGLE/MULTIPLE/MATCHING/ORDERING/OPEN_ANSWER, есть поля баллов full/partial. |
| Learning flow (`Enrollment`, `CourseReviewer`, `LessonSubmission`) | частично | Базовые связи покрыты, но `SubmissionStatus` не отражает явно workflow PRD `pending -> rework -> accepted`. |
| Groups (`LearningGroup`, `GroupMembership`, `GroupType`) | соответствует | Модель групп и членства реализована; типы покрывают FR-106/107. |
| Programs (`LearningProgram`, `ProgramCourse`, `ProgramEnrollment`) | частично | Модель программ и назначений есть, но `ProgramAccessCondition` содержит 3 режима при ожидаемых в PRD двух базовых режимах. |
| Password flows (`PasswordSetupToken`) | соответствует | Токенная сущность есть, привязана к пользователю, есть `usedAt`. |

## 7) Выявленные потенциальные несоответствия названий/типов (для последующих PRE)

1. `SubmissionStatus`:
   - сейчас: `COMPLETE`, `INCOMPLETE`, `PENDING_REVIEW`;
   - PRD (FR-014) описывает workflow с состояниями типа `pending/rework/accepted`.

2. `ProgramAccessCondition`:
   - сейчас: `PREVIOUS_COURSES_COMPLETED`, `PREVIOUS_COURSES_VIEWED_OR_PENDING`, `ALL_OPEN`;
   - PRD (FR-104) фиксирует 2 базовых режима (свободный/последовательный).

3. `Course` поля:
   - часть полей названа иначе относительно PRD терминов (`passingThresholdPercent`),
   - присутствуют дополнительные post-MVP/операционные поля, не перечисленные прямо в FR-005.

4. Mapper-покрытие неравномерно:
   - часть сущностей не имеет явных mapper-интерфейсов (риск рассинхронизации DTO-контрактов в дальнейшем).

## 8) Проверка test_steps PRE-003

- Шаг 1: список сущностей и enum из model-пакета — **выполнен**.
- Шаг 2: сопоставление сущностей с repository и mapper — **выполнен**.
- Шаг 3: матрица «соответствует/частично/не соответствует» — **выполнен**.

Итог: артефакт PRE-003 сформирован.
