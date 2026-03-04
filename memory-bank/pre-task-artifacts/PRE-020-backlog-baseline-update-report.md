# PRE-020 — Финальная синхронизация `tasks.json` по результатам gap-analysis

## Контекст
- Основание: `memory-bank/pre-task-artifacts/PRE-019-backlog-change-set-plan.md`.
- Цель: применить согласованный change-set к `memory-bank/tasks.json`, проверить валидность JSON/зависимостей и зафиксировать новую baseline-версию backlog.

## Внесённые изменения в backlog

### 1) Обновление статусов существующих задач
- `TASK-009` → `done`
- `TASK-010` → `done`
- `TASK-021` → `done`
- `TASK-025` → `obsolete`

### 2) Split задач
- `TASK-019` заменена на:
  - `TASK-019A` (MVP course report по `AC-017`)
  - `TASK-019B` (post-MVP enrichment course report)
- `TASK-020` заменена на:
  - `TASK-020A` (MVP global report)
  - `TASK-020B` (post-MVP enrichment global report)
- `TASK-022` заменена на:
  - `TASK-022A` (base reset-flow)
  - `TASK-022B` (security hardening reset-flow)

### 3) Добавленные задачи (gap closure)
- `TASK-041` — activation-state до установки пароля (`FR-002/AC-002`)
- `TASK-042` — self-profile update с whitelist (`FR-003/AC-003`)
- `TASK-043` — API двух списков enrolled/not-enrolled (`FR-010/AC-010`)
- `TASK-044` — явные статусы `pending -> rework -> accepted` (`FR-014/AC-014`)
- `TASK-045` — централизованный audit-trail (`NFR-AUD-01`)
- `TASK-046` — backup/restore readiness с измеримыми RPO/RTO (`NFR-BCK-01..03`)

### 4) Уточнение зависимостей
- `TASK-040.dependencies` обновлены на:
  - `TASK-020A`, `TASK-022B`, `TASK-028`, `TASK-036`
- Dependency chain для split-веток задан согласно PRE-019:
  - `TASK-019A -> TASK-020A`
  - `TASK-019A -> TASK-019B -> TASK-020B`
  - `TASK-022A -> TASK-022B` (связка с `TASK-005`, `TASK-003`)

## Валидация (test_steps PRE-020)

1. **Шаг 1 (применить правки к `tasks.json`)** — ✅ выполнено.
2. **Шаг 2 (проверить JSON и зависимости)** — ✅ выполнено:
   - итоговый размер backlog: `49` задач;
   - отсутствуют битые зависимости (`dependency_errors=0`);
   - legacy-id после split отсутствуют: `TASK-019`, `TASK-020`, `TASK-022`.
3. **Шаг 3 (зафиксировать summary и baseline)** — ✅ выполнено:
   - обновлены memory-bank контексты и прогресс;
   - новая baseline backlog зафиксирована в `memory-bank/tasks.json`.

## Итог
- `PRE-020` завершена: backlog синхронизирован с результатами pre-phase gap-analysis.
- Pre-phase (`PRE-001..PRE-020`) полностью закрыт.
- Следующий этап: старт выполнения основного backlog из `tasks.json` (первая ready-to-start critical-задача — `TASK-001`).
