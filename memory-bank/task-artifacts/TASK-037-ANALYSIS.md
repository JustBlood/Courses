# TASK-037-ANALYSIS — оценка observability-реализации (standard tracing vs over-engineering)

## Контекст
- Основание: запрос из `memory-bank/02-active-context.md` — оценить выполненную `TASK-037` по двум вопросам:
  1) стоит ли переходить на стандартные инструменты трассировки,
  2) есть ли переусложнение решения.

## Проанализированные источники
- Артефакт: `memory-bank/task-artifacts/TASK-037.md`
- Код:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/security/CorrelationIdFilter.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/observability/HttpServerMetricsFilter.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/observability/ObservabilityMetricsService.java`
  - `monolith-mvp/src/main/resources/application.yml`
- PRD/NFR:
  - `memory-bank/prd/03-non-functional-requirements.md`
  - `memory-bank/prd/05-technical-architecture.md`

## Вывод по текущему состоянию
Текущая реализация **в целом адекватна для MVP** и покрывает ключевые требования по наблюдаемости:
- correlation-id добавляется в MDC и возвращается в `X-Correlation-Id`;
- есть доменные business events;
- есть метрики latency/error/auth/email;
- метрики экспонируются через actuator.

## Где есть частичное переусложнение
1. Кастомный `HttpServerMetricsFilter` + `ObservabilityMetricsService.recordHttp(...)` дублируют часть того, что уже даёт стандартный стек Actuator/Micrometer (`http.server.requests`).
2. При fallback на `request.getRequestURI()` есть риск роста кардинальности метрик по `uri` (операционный риск для Prometheus/TSDB).
3. Поддержка самописных HTTP-метрик увеличивает стоимость сопровождения без существенной выгоды для MVP.

## Ответ на вопрос про «стандартные инструменты трассировки»
Да, **эволюционно стоит перейти** на стандартный стек:
- Micrometer Observation/Tracing;
- OpenTelemetry bridge/exporter (на следующем этапе).

Это уменьшит кастомный код и упростит дальнейший переход к distributed tracing.

## Рекомендуемая траектория
### Этап 1 (минимальный, без риска)
- Сохранить `BusinessEventLogger` и доменные счётчики (`auth/email/business`).
- Для HTTP-латентности/ошибок постепенно опереться на стандартные метрики Actuator/Micrometer.

### Этап 2 (post-MVP)
- Подключить Micrometer Tracing + OTel exporter.
- Унифицировать propagation trace/span-контекста между сервисами.

## Итог
- Срочной переделки для закрытия `TASK-037` не требуется.
- Рекомендован постепенный переход на стандартный tracing/metrics-контур для снижения технического долга и операционных рисков.