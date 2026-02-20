# PRE-018 — Сводная матрица соответствия FR/AC/NFR с risk/effort/phase

## 1) Контекст
- Задача: `PRE-018` из `memory-bank/pre-tasks.json`.
- Цель: консолидировать результаты `PRE-009..PRE-017` в единый слой принятия решений перед `PRE-019`.
- Источники:
  - артефакты `PRE-009..PRE-017` в `memory-bank/pre-task-artifacts/*`;
  - PRD: `memory-bank/prd/02-functional-requirements.md`, `memory-bank/prd/03-non-functional-requirements.md`, `memory-bank/prd/06-acceptance-criteria.md`.

## 2) Сводка покрытия требований

### 2.1 FR (FR-001..FR-019, FR-101..FR-114)
- Всего FR: **33**
- `implemented`: **6**
- `partial`: **24**
- `missing`: **3**

### 2.2 AC (AC-001..AC-019, AC-101..AC-114)
- Всего AC: **33**
- `implemented`: **6**
- `partial`: **23**
- `missing`: **4**

### 2.3 NFR (консолидированный проверенный периметр)
- `implemented`: **1** (`NFR-SEC-02`)
- `partial`: **4** (`NFR-SEC-01`, `NFR-SEC-03`, `NFR-OBS-01`, `NFR-AUD-01`)
- `missing`: **5** (`NFR-SEC-04`, `NFR-SEC-05`, `NFR-BCK-01`, `NFR-BCK-02`, `NFR-BCK-03`)

---

## 3) Единая матрица FR/AC/NFR по блокам

| Блок | Источник | FR статус | AC статус | Ключевой вывод |
|---|---|---:|---:|---|
| Users/Authz (`FR-001..004`) | PRE-009 | 1 impl / 3 partial / 0 missing | 1 / 3 / 0 | База есть, но не хватает первичных назначений, явной активации invite-flow и self-profile update. |
| Courses/Lessons/Scoring (`FR-005..009`) | PRE-010 | 3 / 2 / 0 | 3 / 2 / 0 | CRUD/типы контента готовы, но scoring и submit-flow практики упрощены. |
| Enrollment/Review/Learning (`FR-010..014`) | PRE-011 | 1 / 4 / 0 | 1 / 4 / 0 | Есть рабочий контур, но нет full workflow review и полного runtime по practice. |
| Statistics/Reporting (`FR-015..017`) | PRE-012 | 0 / 3 / 0 | 0 / 2 / 1 | Формулы частично есть, но AC-017 (структуры отчётов) не выполнен. |
| Password/Security (`FR-018..019`) | PRE-013 | 1 / 1 / 0 | 1 / 1 / 0 | Change-password готов; reset-flow частичный из-за отсутствия TTL/anti-abuse/neutral response. |
| Sections/Programs (`FR-101..105`) | PRE-014 | 0 / 3 / 2 | 0 / 3 / 2 | Sections отсутствуют, program API неэкспонирован полностью. |
| Groups/Assignments/Search (`FR-106..109`) | PRE-015 | 0 / 4 / 0 | 0 / 4 / 0 | Сервисный слой частично есть, но endpoint-ы и auto-propagation назначений не доведены. |
| Advanced Practice (`FR-110..114`) | PRE-016 | 0 / 4 / 1 | 0 / 4 / 1 | Параметры сохраняются, но почти не применяются в runtime (threshold/attempts/time/random/stop). |
| NFR Observability/Audit/Backup + SEC | PRE-013, PRE-017 | см. раздел 2.3 | n/a | Наблюдаемость/резервирование и часть security-механик — ключевой инфраструктурный долг. |

---

## 4) Gap-реестр с severity / effort / phase

Шкалы:
- `severity`: Critical / High / Medium / Low
- `effort`: S (1-2 дня), M (3-5 дней), L (6-10 дней), XL (10+ дней)
- `phase`: MVP / post-MVP

| Gap ID | Описание | Связанные FR/AC/NFR | severity | effort | phase | Источник |
|---|---|---|---|---|---|---|
| GAP-018-01 | Нет первичных назначений групп/курсов при создании пользователя | FR-001, AC-001 | High | M | MVP | PRE-009 |
| GAP-018-02 | Нет явного activation-state до установки пароля | FR-002, AC-002 | High | M | MVP | PRE-009 |
| GAP-018-03 | Нет endpoint self-profile update (разрешённые поля) | FR-003, AC-003 | High | M | MVP | PRE-009 |
| GAP-018-04 | Practice submit-flow проверяет не весь question-pool | FR-008, FR-013, AC-008, AC-013 | Critical | L | MVP | PRE-010, PRE-011 |
| GAP-018-05 | Неполный scoring engine (question-level/threshold aggregation) | FR-009, AC-009, FR-110, AC-110 | Critical | L | MVP | PRE-010, PRE-016 |
| GAP-018-06 | Нет API-модели two-lists enrolled/not-enrolled | FR-010, AC-010 | High | M | MVP | PRE-011 |
| GAP-018-07 | Workflow review без явных статусов rework/accepted | FR-014, AC-014 | High | M | MVP | PRE-011 |
| GAP-018-08 | Нет полного reviewer workspace по назначенным курсам | FR-011, AC-011 | Medium | M | MVP | PRE-011 |
| GAP-018-09 | Нет admin-view личной статистики выбранного пользователя | FR-015, AC-015 | High | M | MVP | PRE-012 |
| GAP-018-10 | AC-017 не выполнен: нет course/global report контрактов по PRD | FR-017, AC-017 | Critical | L | MVP | PRE-012 |
| GAP-018-11 | Reset-token без TTL + без anti-abuse + user enumeration | FR-019, AC-019, NFR-SEC-01/04 | Critical | M | MVP | PRE-013 |
| GAP-018-12 | Секреты в конфиге (JWT/SMTP/DB/bootstrap) | NFR-SEC-05 | Critical | S | MVP | PRE-013 |
| GAP-018-13 | Sections-домен отсутствует полностью | FR-101, FR-102, AC-101, AC-102 | High | XL | post-MVP | PRE-014 |
| GAP-018-14 | Program endpoint-ы закомментированы, partial runtime-rules | FR-103..105, AC-103..105 | High | L | post-MVP | PRE-014 |
| GAP-018-15 | Group API частично выключен + нет persisted group→target assign model | FR-106..108, AC-106..108 | High | L | post-MVP | PRE-015 |
| GAP-018-16 | Нет server-side search/filter по users/courses/groups | FR-109, AC-109 | Medium | M | post-MVP | PRE-015 |
| GAP-018-17 | Нет enforcement attemptLimit/timeLimit/deadline/random/stopLesson | FR-111..114, AC-111..114 | High | XL | post-MVP | PRE-016 |
| GAP-018-18 | Нет correlation-id/metrics/audit-event слоя | NFR-OBS-01, NFR-AUD-01 | High | L | MVP | PRE-017 |
| GAP-018-19 | Нет backup/restore регламента и подтверждённых RPO/RTO | NFR-BCK-01..03 | Critical | L | MVP | PRE-017 |

---

## 5) MVP critical-path блокеры (приоритет для PRE-019)

1. **GAP-018-04 + GAP-018-05** — core learning/scoring runtime для practice не соответствует FR-008/009/013/110.
2. **GAP-018-10** — `AC-017` (отчётность) не выполнен, PRD-контракты отчётов не соблюдены.
3. **GAP-018-11 + GAP-018-12** — критические security-риски reset-flow и secrets management.
4. **GAP-018-19** — отсутствует обязательный backup/restore-контур (операционный блокер эксплуатации).
5. **GAP-018-02** — недоформализован lifecycle активации пользователя (invite/onboarding).

---

## 6) Результат по test_steps PRE-018

1. **Консолидировать результаты PRE-009..PRE-017** — ✅ выполнено (разделы 2–3).
2. **Проставить severity/effort и phase (MVP/post-MVP)** — ✅ выполнено (раздел 4).
3. **Проверить полноту матрицы по всем FR/AC/NFR** — ✅ выполнено (разделы 2–3; выделены пробелы и блокеры в разделе 5).

## 7) Выход в следующий этап
- Следующая ready-to-start pre-задача: **PRE-019**.
- Назначение PRE-019: сформировать change-set для `tasks.json` на основе gap-реестра PRE-018 с привязкой каждой правки к источнику.
