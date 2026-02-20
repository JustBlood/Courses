# Прогресс разработки системы

## Назначение
Краткий накопительный журнал по системе в целом.
Обновляется только после полного завершения задач/итераций.

## Формат записи
### YYYY-MM-DD — Итерация / релиз
- Что сделано (кратко):
- Какие модули затронуты:
- Что отложено / следующий крупный этап:

## Текущее состояние
- Инициализировано.

### 2026-02-20 — PRD (главы 00–02) доработаны и согласованы
- Что сделано (кратко):
  - Обновлены главы `prd/00-overview-and-goals.md`, `prd/01-user-scenarios.md`, `prd/02-functional-requirements.md` по детальному feedback.
  - Формулировки синхронизированы с текущей backend-реализацией `monolith-mvp` (стек, auth flow, параметры Course/Lesson/PracticeLesson, отчётность).
  - Исправлен процессный момент: запись в этот файл ведётся после полного завершения задачи.
- Какие модули затронуты:
  - `memory-bank/prd/00-overview-and-goals.md`
  - `memory-bank/prd/01-user-scenarios.md`
  - `memory-bank/prd/02-functional-requirements.md`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
- Что отложено / следующий крупный этап:
  - Заполнение оставшихся глав PRD: `03-non-functional-requirements.md`, `04-constraints-and-assumptions.md`, `05-acceptance-criteria.md`.

### 2026-02-20 — Комплексный PRD 00–07 сформирован
- Что сделано (кратко):
  - Завершены главы `prd/03-non-functional-requirements.md`, `prd/04-constraints-and-assumptions.md`, `prd/05-technical-architecture.md`, `prd/06-acceptance-criteria.md`, `prd/07-development-and-risks-and-future.md`.
  - Актуализирован индекс `memory-bank/01-prd-index.md` с полной структурой PRD-00..PRD-07.
  - Зафиксирован гибридный подход качества: MVP — минимально реализуемый уровень, post-MVP — целевой уровень.
  - В критериях приемки добавлена полная трассировка AC к FR (MVP и post-MVP).
  - Обновлены `memory-bank/02-active-context.md` и `memory-bank/05-task-execution-progress.md`.
- Какие модули затронуты:
  - `memory-bank/01-prd-index.md`
  - `memory-bank/prd/03-non-functional-requirements.md`
  - `memory-bank/prd/04-constraints-and-assumptions.md`
  - `memory-bank/prd/05-technical-architecture.md`
  - `memory-bank/prd/06-acceptance-criteria.md`
  - `memory-bank/prd/07-development-and-risks-and-future.md`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
- Что отложено / следующий крупный этап:
  - Провести пользовательское ревью финальной версии PRD 00–07 и внести точечные правки (при необходимости).

### 2026-02-20 — Сформирован backlog задач из PRD для инкрементной разработки
- Что сделано (кратко):
  - На основе PRD-00..PRD-07 подготовлен `tasks.json` в строгом JSON-формате.
  - Добавлены `agent_instructions` для единых правил работы coding-агентов.
  - Сформирован backlog из 40 атомарных задач (`TASK-001..TASK-040`) с приоритетами, зависимостями, критериями приемки и test steps.
  - Покрыты обязательные категории задач: `infrastructure`, `functional`, `ui`, `integration`, `security`.
  - Обновлены `memory-bank/02-active-context.md` и `memory-bank/05-task-execution-progress.md`.
- Какие модули затронуты:
  - `tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Начать реализацию по одной задаче за сессию с верхушки critical-path (`TASK-001` и далее по зависимостям).

### 2026-02-20 — Добавлен pre-backlog gap-analysis и обновлён основной backlog по итогам ревью субагентов
- Что сделано (кратко):
  - Проведён ревью `tasks.json` с привлечением субагентов `system-architect` и `sprint-prioritizer`.
  - Скорректированы правила работы в `agent_instructions` и критичные зависимости (`TASK-028`, `TASK-019`, `TASK-020`).
  - Создан `pre-tasks.json` (20 задач `PRE-001..PRE-020`) для поэтапной сверки «код vs PRD».
  - Зафиксирован процесс: итоговая корректировка `tasks.json` выполняется только на шаге `PRE-020` после консолидированного gap-analysis.
  - Проверена валидность JSON для `tasks.json` и `pre-tasks.json`.
- Какие модули затронуты:
  - `tasks.json`
  - `pre-tasks.json`
  - `memory-bank/02-active-context.md`
  - `memory-bank/05-task-execution-progress.md`
  - `memory-bank/06-system-development-progress.md`
- Что отложено / следующий крупный этап:
  - Начать выполнение pre-phase с `PRE-001` и довести до `PRE-020`, затем синхронизировать основной backlog реализации.
