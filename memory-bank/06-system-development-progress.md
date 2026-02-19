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
