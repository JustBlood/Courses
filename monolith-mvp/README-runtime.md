# Runtime конфигурация monolith-mvp (TASK-001)

## 1) Профили окружений

- `dev` = `h2 + mail-noop`
- `stage` = `pg + mail-smtp`
- `prod` = `pg + mail-smtp`

Профиль выбирается переменной:

```bash
SPRING_PROFILES_ACTIVE=dev|stage|prod
```

## 2) Быстрый запуск через Docker Compose + .env

1. Скопировать пример окружения:

```bash
copy monolith.env.example monolith.env
```

2. Заполнить минимум:
- `APP_SECURITY_JWT_SECRET`
- `POSTGRES_*`
- `SPRING_MAIL_*` (если нужен SMTP; для локального smoke-check можно оставить `mail-noop`)

3. Поднять окружение:

```bash
docker compose -f docker-compose.monolith.yml --env-file monolith.env up -d --build
```

4. Проверить health-check:

```bash
curl http://localhost:8099/actuator/health
```

Ожидаемый ответ: `{"status":"UP"}`

> В `monolith.env.example` профиль по умолчанию для compose: `pg,mail-noop`.
> Это позволяет поднять PostgreSQL-контур и пройти health-check без внешнего SMTP.

## 3) Переключение H2/PostgreSQL без Docker

### H2 (локальный dev)

```bash
mvn -pl monolith-mvp spring-boot:run -Dspring-boot.run.profiles=dev
```

### PostgreSQL (локальный stage-like)

```bash
mvn -pl monolith-mvp spring-boot:run -Dspring-boot.run.profiles=stage
```

При необходимости переопредели datasource через env:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
