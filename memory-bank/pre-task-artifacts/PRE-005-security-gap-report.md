# PRE-005 — Сверка auth/security-контура (JWT, RBAC, password flows, exception handling)

## 1) Контекст задачи
- Задача: `PRE-005` из `memory-bank/pre-tasks.json`.
- Цель: проверить контур аутентификации/авторизации и password flows, сопоставить с PRD/NFR, зафиксировать security-gap с оценкой риска.
- Область анализа:
  - `config/SecurityConfig`
  - `security/JwtAuthenticationFilter`, `security/JwtService`, `security/AppUserDetailsService`
  - `controller/AuthController`, `UsersController`, `StudentController`, `CoursesController`, `ProgressController`
  - `service/AuthService`, `service/CourseService`, `service/LearningService`, `service/UserService`
  - `exception/GlobalExceptionHandler`
  - `application.yml`, `prd/02-functional-requirements.md`, `prd/06-acceptance-criteria.md`, `prd/03-non-functional-requirements.md`

## 2) Проверка auth/password flow

### 2.1 Login/JWT
- Login: `POST /api/v1/auth/login` (public).
- `AuthService.login` аутентифицирует через `AuthenticationManager`, затем выпускает JWT (`uid`, `role`, `subject=email`, `exp`).
- `JwtService` валидирует подпись и срок жизни (`exp`).
- `JwtAuthenticationFilter` добавляет аутентификацию в SecurityContext только для валидного токена и `enabled` пользователя.

Статус: **частично соответствует** (ядро JWT-контра проверено).

### 2.2 Set password / recover / change
- `set-password`: токен ищется в `PasswordSetupTokenRepository`, проверяется только `usedAt == null`; по успешной установке пароль хешируется и токен помечается использованным.
- `recover-password`: инициирует генерацию нового токена и отправку ссылки.
- `change-password`: доступен авторизованному пользователю (`STUDENT|ADMIN`) с проверкой текущего пароля.

Статус: **частично соответствует** (базовый flow есть, но отсутствует TTL токена и anti-abuse).

## 3) RBAC и policy endpoint access

## 3.1 Глобальный security chain
- Разрешены без auth только: `/api/v1/auth/login`, `/api/v1/auth/set-password`, `/api/v1/auth/recover-password`, `/h2-console/**`, `/swagger-ui/**`, `/v3/api-docs/**`.
- Все остальные endpoint требуют аутентификацию.

### 3.2 Role-level доступ
- `UsersController` (`/api/v1/admin/**`) — `@PreAuthorize("hasRole('ADMIN')")`.
- `CoursesController` (`/api/v1/admin/courses/**`) — `@PreAuthorize("hasRole('ADMIN')")`.
- `ProgressController` (`/api/v1/admin/progress/**`) — `@PreAuthorize("hasRole('ADMIN')")`.
- `StudentController` (`/api/v1/student/**`) — `@PreAuthorize("hasAnyRole('STUDENT','ADMIN')")` (соответствует правилу PRD, что ADMIN имеет функциональность STUDENT).
- `AuthController.change-password` — `@PreAuthorize("hasAnyRole('STUDENT','ADMIN')")`.

### 3.3 Reviewer-политика
- Endpoint review ограничен ролью ADMIN.
- В `LearningService.reviewOpenSubmission` есть бизнес-проверка назначения reviewer на курс через `courseService.canReviewCourse(...)`.
- `getPendingReviews()` возвращает только сабмишены по курсам, назначенным текущему админу.

Статус RBAC: **в целом соответствует FR-011 / NFR-SEC-02**.

## 4) Exception handling и безопасность ответов
- Централизованный `GlobalExceptionHandler` есть.
- `BadCredentialsException`/`DisabledException` возвращают контролируемые 401-ответы.
- `AccessDeniedException` возвращает 403 с безопасным текстом.
- Для `Exception` возвращается `500` с текстом `Internal server error: <ex.getMessage()>`.

Статус: **частично соответствует** (есть утечка внутренних деталей через `ex.getMessage()` для 500).

## 5) Сопоставление с PRD/AC/NFR

| Требование | Оценка | Комментарий |
|---|---|---|
| FR-018 / AC-018 (смена пароля) | implemented | Проверка текущего пароля + хеширование нового реализованы. |
| FR-019 / AC-019 (восстановление пароля) | partial | Flow есть, но нет срока жизни recovery/setup токена. |
| FR-004 / AC-004 (применение роли) | implemented | Роль в authorities (`ROLE_*`), ограничения применяются немедленно. |
| FR-011 / AC-011 (reviewer access policy) | implemented | Роль+назначение проверяются на service-уровне. |
| NFR-SEC-01 (JWT + expiration + hash) | implemented | Подпись/exp проверяются, пароль только hash. |
| NFR-SEC-04 (rate-limit/anti-abuse) | missing | Не найдено rate-limit/lockout для login/recover-password. |
| NFR-SEC-05 (secrets management) | missing | В `application.yml` присутствуют секреты/пароли в открытом виде. |
| NFR-SEC-03 (минимизация утечек) | partial | 500-ответ включает внутреннее сообщение исключения; в Noop mail-log пишется invite-link с токеном. |

## 6) Security-gap матрица (итог PRE-005)

| Gap ID | Severity | Область | Описание | Рекомендация |
|---|---|---|---|---|
| SEC-GAP-005-01 | High | Password token policy | `PasswordSetupToken` не имеет TTL/expiration-check, токен действует бессрочно до использования. | Добавить `expiresAt` + проверку срока в `AuthService.setPassword`; установить разумный TTL (например 15–60 мин для recovery). |
| SEC-GAP-005-02 | High | Anti-abuse | Отсутствуют rate-limit/замедление/блокировка для `/auth/login` и `/auth/recover-password`. | Внедрить rate limiter (bucket/sliding window), lockout/backoff после N неуспешных попыток, аудит событий. |
| SEC-GAP-005-03 | Critical | Secret management | В `application.yml` захардкожены JWT secret, bootstrap admin password, SMTP credentials, DB credentials. | Вынести секреты в env/secret-store; удалить секреты из репозитория; провести ротацию скомпрометированных значений. |
| SEC-GAP-005-04 | Medium | Error disclosure | `GlobalExceptionHandler.handleOther` возвращает клиенту `ex.getMessage()`. | Возвращать фиксированное безопасное сообщение без внутренних деталей; детальные данные — только в server logs. |
| SEC-GAP-005-05 | Medium | Sensitive logging | `NoopEmailService` логирует полный invite-link (с токеном). | Маскировать/не логировать токены в явном виде; оставлять только безопасный trace-id/часть идентификатора. |
| SEC-GAP-005-06 | Low/Medium | Public surface | `/h2-console/**` и swagger открыты по `permitAll` без профильного ограничения. | Ограничить доступ профилями/окружением (dev-only) и/или IP allowlist/basic auth. |
| SEC-GAP-005-07 | Medium | User enumeration | `/auth/recover-password` возвращает `404` для несуществующего email, что раскрывает факт наличия учётки. | Возвращать единый ответ вида «если email существует, письмо отправлено». |

## 7) Проверка test_steps PRE-005

- Шаг 1: Проанализировать SecurityConfig, фильтры, AuthService — **выполнен**.
- Шаг 2: Проверить RBAC-правила на уровне endpoint/service — **выполнен**.
- Шаг 3: Зафиксировать отклонения и уровень риска — **выполнен**.

Итог: артефакт PRE-005 сформирован.