# PRE-002 — Инвентаризация API-контуров (controller / endpoint / DTO / OpenAPI)

## 1) Контекст и рамки
- Задача: `PRE-002` из `memory-bank/pre-tasks.json`.
- Цель: собрать карту `endpoint -> controller -> DTO`, покрыть домены `auth / user / course / learning / reporting`, зафиксировать расхождения с PRD.
- Область анализа:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/*`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/config/OpenAPIConfig.java`
  - `monolith-mvp/src/main/resources/application.yml`

## 2) Инвентаризация controller-контуров

Обнаруженные REST-контроллеры:
- `AuthController` (`/api/v1/auth`)
- `UsersController` (`/api/v1/admin`)
- `CoursesController` (`/api/v1/admin/courses`)
- `StudentController` (`/api/v1/student`)
- `ProgressController` (`/api/v1/admin/progress`)

## 3) Таблица endpoint -> controller -> DTO

| Endpoint | Controller | Request DTO / вход | Response DTO / выход | Домен |
|---|---|---|---|---|
| `POST /api/v1/auth/login` | `AuthController` | `LoginRequest` | `LoginResponse` | auth |
| `POST /api/v1/auth/recover-password` | `AuthController` | `RecoverPasswordRequest` | `ApiResponse` | auth |
| `POST /api/v1/auth/set-password?token=` | `AuthController` | `SetPasswordRequest` + query `token` | `ApiResponse` | auth |
| `POST /api/v1/auth/change-password` | `AuthController` | `ChangePasswordRequest` | `ApiResponse` | auth |
| `POST /api/v1/admin/users` | `UsersController` | `CreateUserRequest` | `UserDto` | user |
| `GET /api/v1/admin/users` | `UsersController` | - | `List<UserDto>` | user |
| `GET /api/v1/admin/users/{userId}` | `UsersController` | path `userId` | `UserDto` | user |
| `PUT /api/v1/admin/users/{userId}` | `UsersController` | `UpdateUserRequest` | `UserDto` | user |
| `POST /api/v1/admin/users/{userId}/password` | `UsersController` | `SetUserPasswordRequest` | `ApiResponse` | user/auth |
| `POST /api/v1/admin/users/activation` | `UsersController` | `ActivationRequest` | `ApiResponse` | user |
| `PATCH /api/v1/admin/users/{userId}/role` | `UsersController` | `UpdateUserRoleRequest` | `UserDto` | user/roles |
| `DELETE /api/v1/admin/users/{userId}` | `UsersController` | path `userId` | `ApiResponse` | user |
| `DELETE /api/v1/admin/users` | `UsersController` | `IdsRequest` | `ApiResponse` | user |
| `POST /api/v1/admin/users/import` (multipart) | `UsersController` | file part `file` | `ApiResponse` | user |
| `GET /api/v1/admin/users/export` | `UsersController` | - | `String` (CSV) | reporting |
| `POST /api/v1/admin/courses` | `CoursesController` | `CreateCourseRequest` | `CourseDto` | course |
| `GET /api/v1/admin/courses` | `CoursesController` | - | `List<CourseSummaryDto>` | course |
| `GET /api/v1/admin/courses/{courseId}` | `CoursesController` | path `courseId` | `CourseAdminDetailsDto` | course |
| `PUT /api/v1/admin/courses/{courseId}` | `CoursesController` | `CreateCourseRequest` | `CourseDto` | course |
| `DELETE /api/v1/admin/courses/{courseId}` | `CoursesController` | path `courseId` | `ApiResponse` | course |
| `POST /api/v1/admin/courses/{courseId}/lessons/theory` | `CoursesController` | `CreateTheoryLessonRequest` | `LessonDto` | course/learning |
| `POST /api/v1/admin/courses/{courseId}/lessons/practice` | `CoursesController` | `CreatePracticeLessonRequest` | `LessonDto` | course/learning |
| `GET /api/v1/admin/courses/{courseId}/lessons/{lessonId}` | `CoursesController` | path params | `LessonDto` | course/learning |
| `PUT /api/v1/admin/courses/{courseId}/lessons/{lessonId}/theory` | `CoursesController` | `CreateTheoryLessonRequest` | `LessonDto` | course/learning |
| `PUT /api/v1/admin/courses/{courseId}/lessons/{lessonId}/practice` | `CoursesController` | `CreatePracticeLessonRequest` | `LessonDto` | course/learning |
| `DELETE /api/v1/admin/courses/{courseId}/lessons/{lessonId}` | `CoursesController` | path params | `ApiResponse` | course/learning |
| `POST /api/v1/admin/courses/{courseId}/assign` | `CoursesController` | `IdsRequest` | `ApiResponse` | learning/enrollment |
| `DELETE /api/v1/admin/courses/{courseId}/assign` | `CoursesController` | `IdsRequest` | `ApiResponse` | learning/enrollment |
| `POST /api/v1/admin/courses/{courseId}/reviewers` | `CoursesController` | `IdsRequest` | `ApiResponse` | learning/review |
| `DELETE /api/v1/admin/courses/{courseId}/reviewers` | `CoursesController` | `IdsRequest` | `ApiResponse` | learning/review |
| `GET /api/v1/student/my/courses` | `StudentController` | - | `List<CourseDto>` | learning |
| `GET /api/v1/student/my/profile` | `StudentController` | - | `StudentProfileDto` | user |
| `POST /api/v1/student/my/avatar` (multipart) | `StudentController` | file part `file` | `UserDto` | user |
| `GET /api/v1/student/courses/{courseId}` | `StudentController` | path `courseId` | `CourseLearnerDto` | learning |
| `GET /api/v1/student/lessons/{lessonId}` | `StudentController` | path `lessonId` | `LearnerLessonDto` | learning |
| `POST /api/v1/student/lessons/{lessonId}/complete-theory` | `StudentController` | path `lessonId` | `SubmissionResultDto` | learning |
| `POST /api/v1/student/lessons/{lessonId}/submit-practice` | `StudentController` | `PracticeSubmissionRequest` | `SubmissionResultDto` | learning |
| `GET /api/v1/student/my/stats` | `StudentController` | - | `List<StudentCourseStatDto>` | reporting |
| `GET /api/v1/admin/progress/reviews/pending` | `ProgressController` | - | `List<PendingSubmissionDto>` | learning/review |
| `POST /api/v1/admin/progress/reviews/{submissionId}` | `ProgressController` | `ReviewOpenSubmissionRequest` | `SubmissionResultDto` | learning/review |
| `GET /api/v1/admin/progress/courses/{courseId}/stats` | `ProgressController` | path `courseId` | `List<CourseStudentStatDto>` | reporting |
| `GET /api/v1/admin/progress/reports/summary.csv` | `ProgressController` | - | `String` (CSV) | reporting |

## 4) OpenAPI и контрактная прозрачность

Что есть:
- Включён `springdoc` (`application.yml`, `springdoc.api-docs.enabled=true`).
- Есть `OpenAPIConfig` с `@OpenAPIDefinition`, basic `Info`, JWT security scheme `bearerAuth`.
- В security-разрешениях присутствуют `"/swagger-ui/**"`, `"/v3/api-docs/**"`.

Что отсутствует/ограничено:
- Нет аннотаций `@Operation`, `@Tag`, `@Schema` на контроллерах/методах/DTO.
- Из-за этого OpenAPI строится в основном рефлексией Spring без доменной группировки и явных описаний business-контрактов.

## 5) Сопоставление с PRD (auth/user/course/learning/reporting)

### Покрытые домены
- **auth**: login, recover/set/change password — endpoints присутствуют.
- **user/profile/roles**: CRUD users, role change, profile/avatar — endpoints присутствуют.
- **course/lesson**: CRUD курса, CRUD theory/practice уроков, enrollment/reviewer assignment — endpoints присутствуют.
- **learning flow**: learner lesson access, theory completion, practice submission, review flow — endpoints присутствуют.
- **reporting/stat**: student stats, course stats, summary CSV — endpoints присутствуют.

### Явные расхождения (API-контракты vs PRD)
1. **FR-017 (отчётность):**
   - PRD требует два формата: отчёт по конкретному курсу и общий отчёт по всем курсам с фиксированными колонками.
   - В API обнаружен только `GET /api/v1/admin/progress/reports/summary.csv` (общий summary), отдельного endpoint для course-specific CSV не найдено.

2. **FR-101..FR-109 (sections/programs/groups/search/filter):**
   - В контроллерах отсутствуют активные endpoints по разделам, программам, группам и каталогным фильтрам.
   - Есть закомментированные заготовки (groups/program assignments), но они не участвуют в runtime API.

3. **Reviewer workspace по PRD FR-011:**
   - Есть endpoint очереди review (`/admin/progress/reviews/pending`), но отсутствует отдельный API контур «список назначенных reviewer-курсов» как отдельный контракт.

4. **OpenAPI-документирование:**
   - Технически OpenAPI включён, но контрактная детализация слабая (нет описательных аннотаций), что затрудняет формальную трассировку AC через API-спеку.

## 6) Проверка полноты PRE-002 test_steps

- Шаг 1 (скан controller-классов и маршрутов): **выполнен**.
- Шаг 2 (сопоставление маршрутов с OpenAPI и DTO): **выполнен**.
- Шаг 3 (проверка полноты по ключевым модулям): **выполнен**.

Итог: артефакт PRE-002 сформирован.
