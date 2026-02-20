# Активный контекст

## Текущая цель
Подготовить двухконтурный backlog для разработки с учётом уже существующего кода:
1) `pre-tasks.json` — задачи gap-analysis (сверка кодовой базы с PRD),
2) `tasks.json` — основной backlog реализации/доработок, который будет актуализирован по итогам pre-phase.

## Что сделано в текущей итерации
- Проведён ревью `tasks.json` через субагентов `system-architect` и `sprint-prioritizer` согласно `.clinerules/50-subagents-guidelines.md`.
- В `tasks.json` внесены правки структуры и зависимостей:
  - улучшены `agent_instructions.before_start` (выбор только ready-to-start задач, tie-break по минимальному id, фиксация блокеров);
  - убрана конфликтная формулировка про запрет редактирования backlog и очищен шумный текст в инструкции `before_finish`;
  - исправлены зависимости `TASK-028` (связка с программами вместо параметров практики);
  - усилены зависимости отчётов: `TASK-019`/`TASK-020` привязаны к группам/программам.
- Создан новый файл `pre-tasks.json` с 20 атомарными pre-задачами (`PRE-001..PRE-020`) для полного gap-analysis:
  - инвентаризация API/моделей/миграций/сервисов/security;
  - трассировка FR/AC/NFR;
  - подготовка change-set и финальная корректировка `tasks.json` в `PRE-020`.
- Проверена JSON-валидность обоих файлов (`tasks.json`, `pre-tasks.json`).

## Источники требований
- `memory-bank/prd/00-overview-and-goals.md`
- `memory-bank/prd/01-user-scenarios.md`
- `memory-bank/prd/02-functional-requirements.md`
- `memory-bank/prd/03-non-functional-requirements.md`
- `memory-bank/prd/04-constraints-and-assumptions.md`
- `memory-bank/prd/05-technical-architecture.md`
- `memory-bank/prd/06-acceptance-criteria.md`
- `memory-bank/prd/07-development-and-risks-and-future.md`
- `.clinerules/50-subagents-guidelines.md`

## Что дальше
1. Выполнять `pre-tasks.json` по порядку ready-to-start (начиная с `PRE-001`).
2. По итогам `PRE-018/019` сформировать согласованный пакет изменений backlog.
3. В `PRE-020` синхронизировать `tasks.json` с фактическим состоянием проекта (done/partial/pending/new/obsolete в рамках принятой схемы).
