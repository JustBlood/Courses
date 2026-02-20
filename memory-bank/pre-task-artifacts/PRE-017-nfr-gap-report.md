# PRE-017 — Сверка NFR-контура (observability / audit / backup-restore / reliability)

## 1) Контекст задачи
- **ID:** `PRE-017`
- **Область:** нефункциональные требования по наблюдаемости, аудиту, backup/restore и эксплуатационной надежности.
- **Цель:** сверить фактическую реализацию `monolith-mvp` с NFR из PRD:
  - `NFR-OBS-01`, `NFR-AUD-01`
  - `NFR-BCK-01..03`
  - эксплуатационные ожидания из `PRD-05` и `PRD-07`.

## 2) Проверенные источники

### 2.1 PRD / memory-bank
- `memory-bank/prd/03-non-functional-requirements.md`
- `memory-bank/prd/04-constraints-and-assumptions.md`
- `memory-bank/prd/05-technical-architecture.md`
- `memory-bank/prd/06-acceptance-criteria.md` (сквозные критерии качества)
- `memory-bank/prd/07-development-and-risks-and-future.md`

### 2.2 Конфиги и runtime-контур
- `monolith-mvp/src/main/resources/application.yml`
- `monolith-mvp/pom.xml`
- `docker-compose.yml`
- `docker-compose-dev.yml`
- `docker-compose1.yml`

### 2.3 Код наблюдаемости / аудита
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/UserService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/{YandexSmtpEmailService,NoopEmailService}.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/exception/GlobalExceptionHandler.java`

## 3) Матрица соответствия NFR

| NFR-ID | Статус | Наблюдение |
|---|---|---|
| NFR-OBS-01 (логи + correlation-id) | **partial** | Логирование есть (`Slf4j`, `Logger`) и фиксируются отдельные события SMTP/ошибок, но **корреляционный ID отсутствует** (нет MDC/filter/interceptor, нет шаблонов логов под correlation). |
| NFR-AUD-01 (аудит ключевых действий) | **partial** | Есть частичные служебные поля (`createdBy`, `createdByAdminId`, `deactivatedBy`) и их заполнение в `UserService`/`CourseService`, но нет единого audit-trace контура (кто/когда/что изменил по ключевым операциям) и нет централизованного аудита reviewer-операций. |
| NFR-BCK-01 (backup БД) | **missing** | В репозитории и compose-контурах отсутствуют backup-скрипты/джобы/reglament (`pg_dump`, cron, ротация хранения). |
| NFR-BCK-02 (backup файлов) | **missing** | Нет автоматизированного ежедневного резервирования директорий файловых материалов/аватаров. |
| NFR-BCK-03 (RPO/RTO readiness) | **missing** | Не найдена реализованная/задокументированная restore-процедура и фактическая фиксация RPO/RTO. |

Итог по NFR-блоку PRE-017:
- **implemented:** 0
- **partial:** 2
- **missing:** 3

## 4) Детализация gap'ов и критичность

### GAP-1: Отсутствует correlation-id (High)
- Симптомы:
  - нет `MDC`/`traceId`/`X-Correlation-ID` фильтра;
  - нет конфигурации лог-паттернов для correlation;
  - расследование инцидентов по цепочке запроса затруднено.
- Риск:
  - деградация наблюдаемости при росте нагрузки и количестве интеграционных ошибок.

### GAP-2: Частичный и несистемный аудит (Medium)
- Симптомы:
  - аудит сведен к отдельным полям в сущностях;
  - нет централизованной аудиторной модели/журнала ключевых операций;
  - нет единых правил по фиксации actor/action/target/result.
- Риск:
  - слабая трассируемость действий администраторов при разборе инцидентов и спорных действий.

### GAP-3: Отсутствует контур backup/restore (Critical)
- Симптомы:
  - отсутствуют backup/restore сценарии в кодовой базе и инфраструктурных файлах;
  - нет регламента хранения/ротации;
  - нет подтвержденных тестов восстановления.
- Риск:
  - невыполнение `NFR-BCK-01..03`, высокий риск потери данных и недостижения целевых RPO/RTO.

### GAP-4: Эксплуатационные метрики и alerting не внедрены (High)
- Симптомы:
  - в `pom.xml` нет actuator/micrometer/prometheus зависимостей;
  - в `application.yml` нет `management.*` конфигурации;
  - отсутствуют экспонируемые метрики latency/error/auth/email.
- Риск:
  - отсутствие раннего детекта деградации и контроля SLO/SLA.

## 5) Сопоставление с критериями PRE-017

### Test Step 1: Проанализировать runtime-конфиги и наблюдаемость — ✅
- Проверены `application.yml`, `pom.xml`, logging usage в сервисах/handler.
- Выявлено: базовое логирование присутствует, но нет correlation-id и метрик/actuator.

### Test Step 2: Проверить наличие аудита и backup-механик — ✅
- Проверены сервисы/модели на audit-поля и точки заполнения.
- Проверены compose/sh-артефакты на backup/restore — автоматизация не найдена.

### Test Step 3: Зафиксировать NFR-gap-report — ✅
- Текущий артефакт фиксирует матрицу соответствия, критичность и рекомендации.

## 6) Рекомендации для следующего backlog-этапа
1. Добавить базовый observability-контур:
   - request filter/interceptor c `X-Correlation-ID` + MDC;
   - `spring-boot-starter-actuator` + micrometer/prometheus;
   - минимальный набор метрик (`latency`, `error-rate`, `auth`, `email`).
2. Выделить централизованный audit-event слой для ключевых бизнес-операций (user create/update, course assignments, review actions).
3. Ввести инфраструктурный backup/restore контур:
   - ежедневный backup БД + файлов;
   - политика хранения (не менее 7 дней для MVP);
   - регулярный тест restore с фиксацией фактических RPO/RTO.
