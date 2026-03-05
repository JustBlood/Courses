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

## 4) Backup / Restore (TASK-039)

Скрипты:
- `scripts/backup/backup-monolith.sh` — backup PostgreSQL + файлового хранилища.
- `scripts/backup/restore-monolith.sh` — restore из конкретного snapshot.

Ключевые env-переменные (см. `monolith.env.example`):
- `BACKUP_ROOT` (по умолчанию `backups/monolith`)
- `BACKUP_RETENTION_DAYS` (по умолчанию `7`)
- `POSTGRES_CONTAINER` (по умолчанию `monolith-postgres`)
- `APP_CONTAINER` (по умолчанию `monolith-mvp`)
- `BACKUP_FILES_SOURCE` (по умолчанию `/opt/app/data`)
- `RESTORE_RECREATE_SCHEMA` (по умолчанию `true`)
- `BACKUP_VERIFY_RETENTION` (по умолчанию `true`, проверяет соблюдение retention после backup)

Запуск backup:

```bash
sh scripts/backup/backup-monolith.sh
```

Запуск backup c проверкой retention (рекомендуется для эксплуатации):

```bash
sh scripts/backup/backup.sh
```

Результат: каталог snapshot вида `backups/monolith/<UTC timestamp>`, внутри:
- `db/<db>.sql`
- `files/app-data.tar`
- `metadata.env` (включая `snapshot_epoch` для расчёта фактического RPO)

Запуск restore:

```bash
sh scripts/backup/restore-monolith.sh backups/monolith/<UTC timestamp>
```

Результат: рядом с snapshot создаётся `restore-report.env` c:
- `restore_duration_sec` (фактический RTO для теста)
- `rpo_seconds` (фактический RPO по `snapshot_epoch`)

Проверка retention отдельно:

```bash
sh scripts/backup/check-backup-retention.sh
```

### Пример cron (ежедневный backup)

```bash
0 2 * * * cd /opt/courses && /bin/sh scripts/backup/backup-monolith.sh >> /var/log/courses-backup.log 2>&1
```

## 5) Раздача загруженных файлов через Nginx (без отдачи из приложения)

`POST /api/v1/files/upload` сохраняет файл в файловую систему и возвращает **относительный путь** (например `/files/uploads/xxx.png`).

Обязательные env-параметры:
- `APP_STORAGE_ROOT_DIR` — корень файлового хранилища приложения (в контейнере обычно `/opt/app/data`)

Поведение API:
- upload endpoint возвращает относительный путь c префиксом `/files/`;
- в сущностях/БД хранится относительный путь внутри storage root;
- при чтении (профиль пользователя, курсы и т.д.) backend возвращает относительный путь c префиксом `/files/`.

Пример Nginx-конфигурации (балансировщик + статика из ФС):

```nginx
upstream monolith_backend {
    server 127.0.0.1:8099;
}

server {
    listen 80;
    server_name courses.example.com;

    # API -> Java backend
    location /api/ {
        proxy_pass http://monolith_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Статические файлы -> файловая система
    location /files/ {
        alias /opt/courses/data/monolith-mvp/files/;
        autoindex off;
        add_header Cache-Control "public, max-age=31536000";
        try_files $uri =404;
    }
}
```

Важно:
- Каталог в `alias` должен указывать на тот же volume/директорию, куда приложение пишет файлы (`APP_STORAGE_ROOT_DIR`).
- Для docker-compose из этого репозитория файловый volume: `./data/monolith-mvp/files:/opt/app/data`.
- В таком варианте приложение хранит и возвращает ссылку, а фактическую отдачу контента делает Nginx с диска.
