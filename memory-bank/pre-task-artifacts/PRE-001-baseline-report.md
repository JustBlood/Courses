# PRE-001 — Baseline отчёт (gap-analysis)

## 1) Метаданные baseline
- **Дата/время фиксации:** 2026-02-20 (Europe/Moscow)
- **Git commit (HEAD):** `0ee4fbf02b639c21bff77482632cae3376a884ae`
- **Git log (последние 20):** зафиксирован в ходе PRE-001 (`git log --oneline -20`)

## 2) Профили запуска и окружения (Spring/runtime)

Источник: `monolith-mvp/src/main/resources/application.yml`

- **Активный профиль по умолчанию:** `h2`
- **Доступные профили в конфигурации:**
  - `h2` — локальная H2 БД (`jdbc:h2:file:./data/monolith-mvp-db;AUTO_SERVER=TRUE`)
  - `pg` — PostgreSQL (`jdbc:postgresql://localhost:5432/courses`)
  - `yandex-mail` — SMTP-профиль для отправки почты

Дополнительно:
- Flyway включён (`spring.flyway.enabled=true`)
- OpenAPI включён (`springdoc.api-docs.enabled=true`)

## 3) Доступные runtime-контуры/окружения в репозитории

1. **Монолитный контур (целевая область анализа):**
   - модуль `monolith-mvp` (Java 21, Spring Boot, Flyway, JWT, Mail)
   - локальный запуск через профили `h2`/`pg`

2. **Compose-контуры в корне репозитория:**
   - `docker-compose-dev.yml` — dev-контур Kafka/Zookeeper/Kafka UI
   - `docker-compose.yml` / `docker-compose1.yml` — legacy/microservices-контур (gateway, сервисы, БД, Redis, Kafka, MinIO)

## 4) Baseline по переменным окружения/конфига

### 4.1. monolith-mvp (application.yml)
- DB (pg): `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
- JWT: `app.security.jwt.secret`, `app.security.jwt.expiration-ms`
- SMTP: `spring.mail.host`, `spring.mail.port`, `spring.mail.username`, `spring.mail.password`
- Bootstrapping: `app.bootstrap-admin.*`

### 4.2. docker-compose (корневые)
- В конфигурациях присутствуют переменные/поля для:
  - PostgreSQL (`POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`)
  - Redis (`REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`)
  - Kafka (`KAFKA_BOOTSTRAP_SERVERS`, listener/zookeeper vars)
  - Mongo (`MONGO_URI`, `MONGO_INITDB_*`)
  - MinIO (`MINIO_*`)

Примечание: в baseline зафиксирован факт наличия захардкоженных секретов в конфигурационных файлах как текущего состояния (без изменений кода в рамках PRE-001).

## 5) Источники требований (PRD)

Зафиксирован полный набор входных документов:
- `memory-bank/prd/00-overview-and-goals.md`
- `memory-bank/prd/01-user-scenarios.md`
- `memory-bank/prd/02-functional-requirements.md`
- `memory-bank/prd/03-non-functional-requirements.md`
- `memory-bank/prd/04-constraints-and-assumptions.md`
- `memory-bank/prd/05-technical-architecture.md`
- `memory-bank/prd/06-acceptance-criteria.md`
- `memory-bank/prd/07-development-and-risks-and-future.md`

## 6) Границы анализа PRE-phase

### Входит в анализ
- `monolith-mvp` (код, DTO/контроллеры/сервисы/репозитории/модели)
- `monolith-mvp/src/main/resources/db/migration/*`
- релевантные root-конфиги запуска (`docker-compose*.yml`) как контекст окружений
- `memory-bank/*` и PRD-документы

### Исключено из анализа по умолчанию
- `media-service/**` (по `.clineignore`)
- бинарные/медиа/данные в `data/**`
- нерелевантные артефакты сборки/логов

## 7) Результат PRE-001

Критерии PRE-001 выполнены:
1. ✅ Собран baseline (commit/hash, профили, env/runtime-контуры)
2. ✅ Зафиксирован список источников требований PRD-00..07
3. ✅ Определены границы анализа (`monolith-mvp + связанные конфиги`)

Следующий ready-to-start шаг: **PRE-002** (инвентаризация API-контуров).
