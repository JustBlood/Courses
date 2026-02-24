# Активный контекст

## Текущая задача
- **ID:** TASK-038
- **Категория:** security
- **Приоритет:** low
- **Описание:** обеспечить защиту чувствительных данных: маскирование секретов/PII в логах и внешняя конфигурация секретов.

## Почему выбрана именно эта задача
- В `memory-bank/tasks.json` это единственная задача со статусом `pending` и `category != ui`.
- Все зависимости `TASK-001`, `TASK-003`, `TASK-037` имеют статус `done`.

## Релевантные требования
- `PRD-03 / NFR-SEC-03`: маскирование/минимизация чувствительных полей в логах.
- `PRD-03 / NFR-SEC-05`: секреты (JWT/SMTP/DB) не должны храниться в коде, только env/secret store.
- `tasks.json / TASK-038.acceptance_criteria`:
  1) пароли/токены/секреты не логируются в открытом виде;
  2) JWT/SMTP/DB секреты берутся из env;
  3) проверка репозитория на новые hardcoded secrets.

## Анализ текущего состояния
- В `application.yml` есть default-значения для чувствительных параметров:
  - `app.security.jwt.secret` содержит fallback-строку;
  - `spring.datasource.password` для `pg` имеет fallback `postgres`.
- В бизнес-логировании есть PII в открытом виде:
  - `AuthService` логирует `email` в `businessEventLogger.log(...)`;
  - `YandexSmtpEmailService`/`NoopEmailService` логируют `toEmail`, а в noop ещё и полный `link`.
- `BusinessEventLogger` сейчас пишет key/value без редактирования чувствительных значений.

## План реализации
1. Убрать fallback-дефолты для чувствительных конфигов (`JWT secret`, `pg datasource password`) в `application.yml`.
2. Добавить централизованную санитизацию значений в `BusinessEventLogger` (маскирование для ключей типа `email/to/password/token/secret/authorization/link`).
3. Убрать прямое логирование секретных/PII-данных из email-сервисов (особенно `link`) и привести к безопасному формату.
4. Добавить интеграционный тест на санитизацию событий логгера.
5. Выполнить тесты из `test_steps` задачи и зафиксировать результаты в артефактах memory-bank.

## Риски
- Удаление fallback-значений может влиять на локальные окружения без env; компенсируется тем, что тестовые профили задают свои datasource параметры.
- Нельзя ломать текущие контракты API и метрики observability.

---

## CHECKPOINT (Token Budget Gate)

Контекстное окно превысило лимит 350000 токенов, реализация приостановлена для продолжения через `/newtask`.

### Что уже сделано
1. Прочитаны обязательные правила `.clinerules`, обязательные memory-bank документы, `memory-bank/tasks.json`, `PRD index`, релевантные PRD главы (`PRD-03`, `PRD-06`).
2. Определена целевая задача: **TASK-038** (единственная `pending` с `category != ui`, зависимости закрыты).
3. Выполнен анализ кода и рисков по TASK-038:
   - прочитаны `AuthService`, `BusinessEventLogger`, `YandexSmtpEmailService`, `NoopEmailService`, `JwtAuthenticationFilter`, `GlobalExceptionHandler`, `SecurityConfig`, `application.yml`, `monolith.env.example`, тесты.
   - выявлены проблемные места: логирование PII/email/link, fallback-секреты в `application.yml`.
4. Обновлён этот файл активного контекста с планом реализации.
5. В код внесено изменение:
   - **Файл:** `monolith-mvp/src/main/java/ru/just/monolithmvp/observability/BusinessEventLogger.java`
   - Добавлена централизованная санитизация key/value:
     - список чувствительных ключей: `email`, `to`, `password`, `token`, `secret`, `authorization`, `link`
     - значения по этим ключам логируются как `[REDACTED]`.

### Что осталось сделать
1. Довести TASK-038 до полного acceptance:
   - убрать fallback-секреты из `monolith-mvp/src/main/resources/application.yml`:
     - `app.security.jwt.secret` без дефолтного значения,
     - `spring.datasource.password` (pg профиль) без дефолтного значения;
   - при необходимости скорректировать `monolith.env.example`/документацию (без добавления реальных секретов).
2. Убрать/уменьшить прямое PII-логирование в email-сервисах:
   - `YandexSmtpEmailService` (`toEmail` в `log.info`),
   - `NoopEmailService` (не логировать `link` в открытом виде).
3. Добавить/обновить интеграционный тест(ы) для проверки маскировки в `BusinessEventLogger`.
4. Выполнить test steps TASK-038:
   - login/reset-password сценарии и проверка логов;
   - проверка конфигурации запуска с env-only секретами;
   - grep/скан на hardcoded secrets.
5. После успешных тестов:
   - сменить `TASK-038` в `memory-bank/tasks.json` на `done`;
   - создать артефакт `memory-bank/task-artifacts/TASK-038.md`;
   - обновить `memory-bank/05-task-execution-progress.md` и `memory-bank/06-system-development-progress.md`;
   - очистить `memory-bank/02-active-context.md` по правилам после полного завершения.

### Текущее состояние task_progress
- [x] Прочитать обязательные правила из .clinerules и базовый контекст memory-bank
- [x] Определить целевую задачу: продолжить активную или выбрать новую non-ui из tasks.json
- [x] Изучить требования выбранной задачи (agent_instructions + релевантные PRD главы)
- [x] Провести анализ/проектирование (при необходимости с субагентом) и зафиксировать план в memory-bank/02-active-context.md
- [ ] Реализовать изменения в коде и добавить/обновить интеграционные тесты
- [ ] Прогнать релевантные проверки (тесты/сборка)
- [ ] Обновить memory-bank артефакты по завершению (task-artifact, 05-task-execution-progress, при необходимости 06-system-development-progress, очистка 02-active-context)
- [ ] Предоставить итог пользователю

---

## CHECKPOINT-2 (Token Budget Gate, 2026-02-24)

Контекстное окно снова превысило лимит 350000 токенов. Работа принудительно приостановлена по правилам `.clinerules/10-memory-bank-workflow.md`.

### Что успели сделать после первого checkpoint
1. Доведены изменения по безопасности логирования:
   - `NoopEmailService`: убрано логирование `toEmail/fullName/link` в открытом виде, оставлен безопасный `warn` без PII/секретов.
   - `YandexSmtpEmailService`: лог приведён к безопасному формату (без PII/секретов; только провайдер/результат).
   - `BusinessEventLogger`: уже содержит централизованную редакцию чувствительных ключей (email/token/secret/password/link/authorization/to).
2. Добавлен новый тест:
   - `monolith-mvp/src/test/java/ru/just/monolithmvp/observability/BusinessEventLoggerTest.java`
   - проверяет, что sensitive значения логируются как `[REDACTED]` и исходные значения не попадают в лог.
3. Исправлены тестовые конфигурации после удаления fallback JWT secret:
   - `ObservabilityIntegrationTest` — добавлен тестовый `app.security.jwt.secret`.
   - `UserAuthStudentFlowIntegrationTest` — добавлен тестовый `app.security.jwt.secret`.
4. Прогон тестов:
   - выполнен `mvn -pl monolith-mvp -Dtest=BusinessEventLoggerTest,UserAuthStudentFlowIntegrationTest,ObservabilityIntegrationTest test`
   - статус: **BUILD SUCCESS**.
5. Выполнен скан на hardcoded-паттерны:
   - в `application.yml` нет fallback-вариантов `APP_SECURITY_JWT_SECRET:...` и `SPRING_DATASOURCE_PASSWORD:postgres`.
6. Статус задачи в backlog обновлён:
   - выполнена команда `python memory-bank/change_task_status.py TASK-038 done`
   - результат `SUCCESS` (обновлён `memory-bank/tasks.json`).

### Что осталось сделать для полного закрытия итерации
1. Оформить финальные memory-bank артефакты по правилам:
   - создать `memory-bank/task-artifacts/TASK-038.md` (перенести финальный active context);
   - добавить запись/ссылку в `memory-bank/05-task-execution-progress.md`;
   - обновить `memory-bank/06-system-development-progress.md` (как архитектурно-важное изменение по безопасности/конфигам);
   - очистить `memory-bank/02-active-context.md` после фиксации артефакта.
2. Отдать пользователю финальный отчёт по задаче.

### Текущее состояние task_progress (актуализировано)
- [x] Подтвердить контекст и выбрать активную non-ui задачу (TASK-038)
- [x] Завершить изменения кода по TASK-038 (маскирование + env-only секреты)
- [x] Добавить/обновить тесты на маскирование чувствительных данных
- [x] Выполнить test_steps TASK-038 (тесты/сканы/проверки)
- [ ] Обновить memory-bank (active-context, artifacts, progress, system-progress, tasks status)
- [ ] Предоставить итог пользователю