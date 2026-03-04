# PRE-008 — Проверка покрытия интеграционными/сквозными тестами по AC

## 1) Контекст и цель
- Задача: `PRE-008` из `memory-bank/pre-tasks.json`.
- Цель: инвентаризировать текущие integration/e2e тесты и сопоставить их с `AC-001..AC-019` и `AC-101..AC-114` из `PRD-06`.
- Область анализа: `monolith-mvp/src/test/java`.

## 2) Инвентаризация текущих integration tests

Обнаружено 2 интеграционных тест-класса:

1. `ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`
   - Сквозной user/auth поток: создание пользователей, onboarding/set-password, login, change/recover password, avatar, RBAC-граничные проверки, update/role/activation, import/export users.

2. `ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
   - Сквозной курс/уроки: CRUD course/lesson, назначение на курс, reviewer-назначения, submit/review open-answer, часть validation/negative/corner cases.

## 3) Матрица покрытия AC

Статусы:
- `covered` — сценарий явно и полно проверяется интеграционными тестами;
- `partial` — есть частичное покрытие, но ключевые условия AC проверены не полностью;
- `missing` — релевантных тест-проверок не найдено.

### 3.1 MVP AC-001..AC-019

| AC | Статус | Покрытие тестами / комментарий |
|---|---|---|
| AC-001 | partial | Есть создание пользователя админом, но нет явной проверки первичных назначений групп/курсов при создании. |
| AC-002 | covered | Полный invite/set-password/login поток покрыт (в т.ч. reuse token negative). |
| AC-003 | partial | Есть admin update пользователя; self-profile edit в разрешённых пределах явно не проверяется. |
| AC-004 | covered | Есть смена роли и подтверждение нового role-claim после логина. |
| AC-005 | partial | Есть create/update/get course; явная проверка отображения в каталоге (list) не зафиксирована. |
| AC-006 | covered | Добавление THEORY/PRACTICE уроков и структура курса покрыты. |
| AC-007 | partial | Проверен только `HTML_TEXT`; `VIDEO_URL`/`PDF_FILE` не покрыты. |
| AC-008 | partial | Проверены не все типы вопросов (нет `MATCHING`, `ORDERING` и т.д.). |
| AC-009 | partial | Есть поля full/partial points, но нет явной верификации расчёта баллов по правилам AC. |
| AC-010 | partial | Есть назначение на курс и проверка доступа; нет двусписочной модели enrolled/not-enrolled и явного unassign-сценария. |
| AC-011 | partial | Есть назначение reviewer и review action; нет явной проверки reviewer workspace/очереди ожидающих ответов. |
| AC-012 | partial | Есть complete-theory вызов; нет явной проверки начисления баллов/статистики. |
| AC-013 | missing | Нет явного позитивного автопроверяемого submit PRACTICE_TEST с проверкой результата/статуса/баллов. |
| AC-014 | partial | Есть open-answer review и повторная проверка, но workflow `pending -> rework -> accepted` отражён не полностью/неявно. |
| AC-015 | missing | Нет проверок endpoint личной статистики и формул эффективности/% выполнения. |
| AC-016 | missing | Нет проверок course statistics для администратора. |
| AC-017 | missing | Нет проверок course/global reports и колонок отчётов. |
| AC-018 | partial | Есть change-password позитив/негатив, но не везде явно зафиксировано требование «старый пароль больше не работает». |
| AC-019 | covered | Recover-password поток (initiate + set-password по токену + negative reuse) покрыт. |

Итог по MVP (19 AC):
- `covered`: 4
- `partial`: 11
- `missing`: 4

### 3.2 post-MVP AC-101..AC-114

| AC | Статус | Комментарий |
|---|---|---|
| AC-101 | missing | Нет integration-проверок разделов каталога. |
| AC-102 | missing | Нет integration-проверок создания курса из контекста раздела. |
| AC-103 | missing | Нет integration-проверок программ обучения и порядка курсов. |
| AC-104 | missing | Нет integration-проверок правил прохождения программы (дедлайн/режим доступа). |
| AC-105 | missing | Нет integration-проверок назначений программ пользователю/группе. |
| AC-106 | missing | Нет integration-проверок CRUD групп по типам. |
| AC-107 | missing | Нет integration-проверок membership ограничений/редактирования членства. |
| AC-108 | missing | Нет integration-проверок массовых назначений через группы. |
| AC-109 | missing | Нет integration/e2e проверок поиска/фильтрации каталогов. |
| AC-110 | missing | Нет проверок threshold прохождения практики. |
| AC-111 | missing | Нет проверок лимита попыток. |
| AC-112 | missing | Нет проверок time limit/deadline поведения. |
| AC-113 | missing | Нет проверок random/shuffle/randomQuestionCount. |
| AC-114 | missing | Нет проверок stopLesson-блокировки. |

Итог по post-MVP (14 AC):
- `covered`: 0
- `partial`: 0
- `missing`: 14

## 4) Сводка покрытия

Всего AC: 33
- `covered`: 4 (12.1%)
- `partial`: 11 (33.3%)
- `missing`: 18 (54.5%)

Ключевой вывод: текущие integration tests дают базовое покрытие ядра auth + course/lesson CRUD, но критичные зоны отчётности/статистики и почти весь post-MVP контур остаются без автоматизированной интеграционной валидации.

## 5) Приоритетный тест-долг (top)

### P0 (критично для MVP AC-ready)
1. AC-013: отдельный integration-тест на `PRACTICE_TEST` submit с проверкой автоскоринга (result/status/points).
2. AC-015/AC-016: integration-тесты формул статистики (личная + по курсу) с ручной верификацией вычислений.
3. AC-017: integration-тесты course/global reports со строгой проверкой структуры и порядка колонок.
4. AC-010/AC-011/AC-012/AC-014: усиление end-to-end learning/review path (двусписочное зачисление, reviewer queue, theory points, явный rework->accepted).

### P1 (закрытие quality gaps MVP)
1. AC-001: первичные назначения групп/курсов при create user.
2. AC-003: self-profile update с ограничениями полей.
3. AC-007/AC-008/AC-009: расширение покрытия по типам контента/вопросов и правилам балльной модели.
4. AC-018: явная проверка невозможности login старым паролем после change-password.

### P2 (post-MVP readiness)
1. Полный integration-suite по AC-101..AC-109 (sections/programs/groups/search).
2. Полный integration-suite по AC-110..AC-114 (advanced practice constraints).

## 6) Проверка test_steps PRE-008

- Шаг 1 (инвентаризация integration tests): ✅ выполнен.
- Шаг 2 (сопоставление с AC-таблицей PRD-06): ✅ выполнен.
- Шаг 3 (фиксация coverage-gap отчёта): ✅ выполнен (данный артефакт).
