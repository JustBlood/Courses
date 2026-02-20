# PRE-019 — Change-set план для актуализации `tasks.json`

## 1) Контекст
- Задача: `PRE-019` из `memory-bank/pre-tasks.json`.
- Основание: сводная матрица и gap-реестр из `PRE-018` + детальные отчёты `PRE-008..PRE-017`.
- Цель: подготовить **пакет изменений backlog** (done/obsolete/split/merge/add) перед фактическим редактированием `tasks.json` в `PRE-020`.

Источники:
- `memory-bank/pre-task-artifacts/PRE-018-fr-ac-nfr-consolidated-matrix.md`
- `memory-bank/pre-task-artifacts/PRE-008-test-coverage-gap-report.md`
- `memory-bank/pre-task-artifacts/PRE-009-fr-001-fr-004-gap-report.md`
- `memory-bank/pre-task-artifacts/PRE-010-fr-005-fr-009-gap-report.md`
- `memory-bank/pre-task-artifacts/PRE-011-fr-010-fr-014-gap-report.md`
- `memory-bank/pre-task-artifacts/PRE-012-fr-015-fr-017-gap-report.md`
- `memory-bank/pre-task-artifacts/PRE-013-fr-018-fr-019-security-gap-report.md`
- `memory-bank/pre-task-artifacts/PRE-014-fr-101-fr-105-gap-report.md`
- `memory-bank/pre-task-artifacts/PRE-015-fr-106-fr-109-gap-report.md`
- `memory-bank/pre-task-artifacts/PRE-016-fr-110-fr-114-gap-report.md`
- `memory-bank/pre-task-artifacts/PRE-017-nfr-gap-report.md`

---

## 2) Change-set по текущим задачам `tasks.json`

Ниже — предлагаемые операции для применения в `PRE-020`.

## 2.1 Статус `done` (фактически реализовано по итогам pre-phase)

1. **TASK-009** (`FR-005`, `AC-005`) → `done`
   - Обоснование: `PRE-010` фиксирует `FR-005 = implemented`, `AC-005 = implemented`.
2. **TASK-010** (`FR-006/FR-007`, `AC-006/AC-007`) → `done`
   - Обоснование: `PRE-010` фиксирует `FR-006/007 = implemented`, `AC-006/007 = implemented`.
3. **TASK-021** (`FR-018`, `AC-018`) → `done`
   - Обоснование: `PRE-013` фиксирует `FR-018 = implemented`, `AC-018 = implemented`, подтверждён интеграционным тестом.

## 2.2 Статус `obsolete` (вне текущего PRD-скоупа)

1. **TASK-025** (CSV import/export users+progress) → `obsolete`
   - Обоснование: отсутствует в `PRD-02` (`FR-001..FR-019`, `FR-101..FR-114`) и в `PRD-06`.

## 2.3 `split` (декомпозиция крупных/смешанных задач)

1. **TASK-019** split на:
   - `TASK-019A` (MVP): course report по `AC-017` с обязательными колонками и фиксированными пустыми полями.
   - `TASK-019B` (post-MVP enrichment): заполнение колонок, зависящих от групп/программ, после `TASK-027/030/032`.
   - Обоснование: `PRE-012`, `PRE-018` — `AC-017` критичный MVP-блокер, не должен полностью зависеть от post-MVP.

2. **TASK-020** split на:
   - `TASK-020A` (MVP): global report по `AC-017` (минимально полный контракт PRD).
   - `TASK-020B` (post-MVP enrichment): расширение по программам/группам и форматам после post-MVP блока.
   - Обоснование: `PRE-012`, `PRE-018`.

3. **TASK-022** split на:
   - `TASK-022A` (базовый reset-flow: initiate/confirm) — почти реализовано.
   - `TASK-022B` (security hardening reset-flow): TTL reset-токена, neutral response, anti-enumeration.
   - Обоснование: `PRE-013` (`FR-019/AC-019 = partial`, критичные SEC gaps).

4. **TASK-011 + TASK-015** (логически разделить responsibilities):
   - `TASK-011` оставить на data/model + типы вопросов.
   - `TASK-015` перефокусировать как runtime scoring engine (полный question-pool, корректная агрегация баллов, partial scoring rules).
   - Обоснование: `PRE-010`, `PRE-011`, `PRE-016` (критичный gap practice runtime/scoring).

## 2.4 `merge` (объединение пересекающихся security задач)

1. **TASK-005 + TASK-022B** (или сильная связка через dependency)
   - Новый объединённый фокус: anti-abuse на auth/reset (`rate-limit`, backoff/lockout, reset abuse controls).
   - Обоснование: `PRE-013` (`NFR-SEC-04 missing`, reset-flow security gaps).

> Примечание: если merge нежелателен организационно, в `PRE-020` оставить отдельными задачами, но сделать взаимные зависимости и единый acceptance-блок.

## 2.5 `add` (новые задачи, которых не хватает для закрытия gap-реестра)

1. **NEW-TASK-ACTIVATION-STATE**
   - Явное состояние активации пользователя до установки пароля (`FR-002`, `AC-002`).
   - Источник: `PRE-009`, `GAP-018-02`.

2. **NEW-TASK-SELF-PROFILE-UPDATE**
   - Endpoint self-profile update с white-list разрешённых полей (`FR-003`, `AC-003`).
   - Источник: `PRE-009`, `GAP-018-03`.

3. **NEW-TASK-ENROLLMENT-TWO-LISTS-API**
   - API-модель «enrolled/not enrolled» для курса (`FR-010`, `AC-010`).
   - Источник: `PRE-011`, `GAP-018-06`.

4. **NEW-TASK-REVIEW-WORKFLOW-STATUSES**
   - Явные статусы workflow `pending -> rework -> accepted` (`FR-014`, `AC-014`).
   - Источник: `PRE-011`, `GAP-018-07`.

5. **NEW-TASK-AUDIT-TRAIL**
   - Централизованный audit-event слой для ключевых действий (`NFR-AUD-01`).
   - Источник: `PRE-017`, `GAP-018-18`.

6. **NEW-TASK-BACKUP-RESTORE-READINESS** (если `TASK-039` не расширять)
   - Проверяемый регламент backup/restore + фиксация фактического RPO/RTO (`NFR-BCK-01..03`).
   - Источник: `PRE-017`, `GAP-018-19`.

---

## 3) Dependency-план после правок (предложение)

Чтобы устранить конфликт MVP vs post-MVP и сохранить корректный critical path:

1. `TASK-019A` зависит от: `TASK-018` (+ при необходимости `TASK-027` только для группы-колонки, если не допускается пустое значение).
2. `TASK-020A` зависит от: `TASK-019A`.
3. `TASK-019B` зависит от: `TASK-027`, `TASK-030`, `TASK-032`, `TASK-019A`.
4. `TASK-020B` зависит от: `TASK-019B`, `TASK-020A`.
5. `TASK-022B` зависит от: `TASK-022A`, `TASK-005`, `TASK-003`.
6. `TASK-015` (runtime scoring) зависит от: `TASK-011`, `TASK-012`.
7. `TASK-033..036` завязать на обновлённый `TASK-015`, чтобы advanced-practice строился на корректном базовом движке.

---

## 4) Проверка непротиворечивости (test_step PRE-019 #2)

Проверено на уровне diff-плана:
- нет циклических зависимостей в предложенном фрагменте;
- MVP-блокер `AC-017` выводится на отдельный MVP-контур (`TASK-019A/020A`) без жёсткой блокировки всей отчётности post-MVP задачами;
- security hardening reset-flow выводится в явный dependency-chain (`TASK-022A -> TASK-022B`, с anti-abuse через `TASK-005`);
- расширенные practice-требования `FR-110..FR-114` остаются после стабилизации базового runtime scoring.

---

## 5) Финальный diff-план для `PRE-020` (кратко)

1. Обновить статусы:
   - `TASK-009`, `TASK-010`, `TASK-021` → `done`
   - `TASK-025` → `obsolete`
2. Выполнить split:
   - `TASK-019 -> TASK-019A/TASK-019B`
   - `TASK-020 -> TASK-020A/TASK-020B`
   - `TASK-022 -> TASK-022A/TASK-022B`
3. Добавить новые задачи:
   - activation-state, self-profile-update, enrollment-two-lists-api, review-workflow-statuses, audit-trail, backup-restore-readiness (или расширение `TASK-039`).
4. Пересобрать зависимости согласно разделу 3.
5. Проверить JSON-валидность и readiness-цепочки после правок.

---

## 6) Результат по test_steps PRE-019

1. **Шаг 1: На базе PRE-018 собрать предложения по корректировкам** — ✅ выполнено (разделы 2–3).
2. **Шаг 2: Проверить непротиворечивость зависимостей после правок** — ✅ выполнено (раздел 4).
3. **Шаг 3: Подготовить финальный diff-план для tasks.json** — ✅ выполнено (раздел 5).
