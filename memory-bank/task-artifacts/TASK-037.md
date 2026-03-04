# TASK-037 (done) — Observability-контур (correlation-id, structured events, metrics)

## Что сделано
- Реализован infrastructure-контур observability в `monolith-mvp` без изменения бизнес-контрактов API.
- Добавлен `CorrelationIdFilter` с пробросом `X-Correlation-Id` в HTTP-ответы и MDC.
- Добавлен `HttpServerMetricsFilter` для метрик HTTP latency/error.
- Добавлены сервисы:
  - `ObservabilityMetricsService` (auth/email counters и HTTP-метрики);
  - `BusinessEventLogger` (структурные бизнес-события).
- Встроены structured events и метрики в ключевые сервисы:
  - `AuthService` (`auth.login`, `auth.set_password`, `auth.change_password`, `auth.recover_password`);
  - `UserService` (`user.create`, `user.update`);
  - `CourseService` (`course.enrollment.*`, `course.reviewer.*`);
  - `LearningService` (`learning.open_submission.review`);
  - `YandexSmtpEmailService` и `NoopEmailService` (`email.password_link.send` + email metrics).
- Добавлено structured error-logging в `GlobalExceptionHandler` для целевых exception-сценариев.
- В `application.yml` подтверждён observability runtime:
  - `management.endpoints.web.exposure.include=health,info,metrics`;
  - log pattern с `%X{correlationId:-na}`.

## Тестирование
- Добавлен интеграционный тест `ObservabilityIntegrationTest`:
  - проверка наличия `X-Correlation-Id` в API-ответе;
  - проверка доступности `/actuator/metrics` и наличия `courses_http_requests_total`.
- Прогон:
  - `mvn -f monolith-mvp/pom.xml -Dtest=ObservabilityIntegrationTest,UserAuthStudentFlowIntegrationTest test`
  - Результат: `BUILD SUCCESS` (`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`).

## Изменённые файлы (ключевые)
- `monolith-mvp/src/main/java/ru/just/monolithmvp/observability/ObservabilityMetricsService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/observability/BusinessEventLogger.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/security/CorrelationIdFilter.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/observability/HttpServerMetricsFilter.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/config/SecurityConfig.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/AuthService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/UserService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/LearningService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/YandexSmtpEmailService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/NoopEmailService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/exception/GlobalExceptionHandler.java`
- `monolith-mvp/src/main/resources/application.yml`
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ObservabilityIntegrationTest.java`

## Копия активного контекста на момент завершения

```md
# Активный контекст

## Текущая задача
- **ID:** TASK-037
- **Категория:** infrastructure
- **Приоритет:** high
- **Статус:** in_progress (checkpoint по Token Budget Gate)

## Срабатывание Token Budget Gate
- На текущем шаге: **Context Window Usage = 357,871 / 400K (> 350000)**.
- По правилу `.clinerules/10-memory-bank-workflow.md` выполнение должно быть приостановлено до запуска новой итерации через `/newtask`.

## Цель задачи
Реализовать observability-контур в `monolith-mvp`:
1. correlation-id для каждого HTTP-запроса;
2. структурные логи ключевых бизнес-событий;
3. метрики latency/error/auth/email.

## Что уже реализовано в коде

### Базовый observability-контур (было сделано ранее и сохранено)
- Добавлены:
  - `observability/ObservabilityMetricsService.java`
  - `observability/BusinessEventLogger.java`
  - `security/CorrelationIdFilter.java`
  - `observability/HttpServerMetricsFilter.java`
- Подключено в `SecurityConfig`:
  - `CorrelationIdFilter` перед `UsernamePasswordAuthenticationFilter`
  - `HttpServerMetricsFilter` после `JwtAuthenticationFilter`
- В `application.yml`:
  - exposure `health,info,metrics`
  - log pattern с MDC `correlationId`

### Доработки в текущей итерации
- `AuthService`:
  - добавлены auth-метрики с outcome success/failure для `login`, `set_password`, `change_password`, `recover_password`;
  - добавлены structured business events (`auth.*`) через `BusinessEventLogger`.
- `UserService`:
  - добавлены structured events `user.create` и `user.update` (success/failure, actor/reason).
- `CourseService`:
  - добавлены structured events:
    - `course.enrollment.assign/unassign`
    - `course.reviewer.assign/unassign`
    - с outcome `success/noop` и ключевыми идентификаторами.
- `LearningService`:
  - добавлен structured event `learning.open_submission.review` с переходом статуса `fromStatus -> toStatus`, reviewer/student/course/lesson/points.
- Email-сервисы:
  - `YandexSmtpEmailService`: метрики email success/failure + structured event `email.password_link.send`.
  - `NoopEmailService`: метрика failure + structured event `email.password_link.send` с причиной `mail_sender_not_configured`.
- `GlobalExceptionHandler`:
  - добавлено structured logging для `NotFound/BadRequest/Validation/NotReadable/AccessDenied/BadCredentials/Disabled`;
  - добавлен безопасный `safeCurrentUser()` для исключения падений при отсутствии аутентификации.
- Интеграционный тест:
  - добавлен `ObservabilityIntegrationTest`:
    - проверка, что в ответе есть `X-Correlation-Id`;
    - проверка доступности `/actuator/metrics` и наличия `courses_http_requests_total`.

## Прогон тестов
- Выполнен запуск:
  - `mvn -f monolith-mvp/pom.xml -Dtest=ObservabilityIntegrationTest,UserAuthStudentFlowIntegrationTest test`
- Результат: **BUILD SUCCESS**
  - `ObservabilityIntegrationTest`: passed
  - `UserAuthStudentFlowIntegrationTest`: passed
  - `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
```
