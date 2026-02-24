# Активный контекст

## Текущая аналитическая задача
- Запрос пользователя: проанализировать выполненную `TASK-037` и оценить:
  1) стоит ли применить стандартные инструменты трассировки,
  2) не переусложнено ли решение.
- Дополнительное требование: использовать `backend-architect` и `system-architect` subagent.

## Что проанализировано
- Артефакт: `memory-bank/task-artifacts/TASK-037.md`
- Код observability-контура:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/security/CorrelationIdFilter.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/observability/HttpServerMetricsFilter.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/observability/ObservabilityMetricsService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/observability/BusinessEventLogger.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/config/SecurityConfig.java`
  - `monolith-mvp/src/main/resources/application.yml`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/ObservabilityIntegrationTest.java`
- Проверено использование бизнес-логгера/метрик в сервисах (`AuthService`, `UserService`, `CourseService`, `LearningService`, `YandexSmtpEmailService`, `NoopEmailService`).
- Использованы 2 субагента: `backend-architect`, `system-architect`.

## Синтез выводов (по коду + субагентам)
1. Текущая реализация **адекватна для MVP** и закрывает требования TASK-037/NFR-OBS:
   - correlation-id в MDC + ответном заголовке;
   - отдельные бизнес-события;
   - метрики latency/error/auth/email.

2. Есть признаки **частичного усложнения**:
   - `HttpServerMetricsFilter` + `ObservabilityMetricsService.recordHttp(...)` вручную дублируют часть стандартного actuator/micrometer контура для HTTP-метрик.
   - Вручную поддерживаются дополнительные счетчики там, где часть уже предоставляет стандартный `http.server.requests`.

3. По «стандартным инструментам трассировки»:
   - Для **полноценного distributed tracing** действительно лучше эволюционно перейти на стандартный стек Micrometer Observation/Tracing (+ OTel bridge/exporter).
   - Срочной переделки для TASK-037 не требуется: текущий scope задачи выполнен.

4. Риски текущего варианта:
   - возможный рост кардинальности метрик по URI (если использовать raw path);
   - операционная стоимость поддержки кастомных HTTP-метрик выше, чем у стандартного контура.

5. Практичная траектория:
   - Этап 1 (минимум): оставить `BusinessEventLogger` и доменные счетчики auth/email/business, но постепенно опереться на стандартные HTTP-метрики Actuator/Micrometer.
   - Этап 2 (post-MVP): подключить стандартный tracing (Micrometer Tracing + OpenTelemetry exporter), унифицировать propagation и trace/span-контекст.