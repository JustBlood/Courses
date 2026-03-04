# PRE-009 — Сверка реализации FR-001..FR-004 (users/profile/roles) с PRD и AC

## 1) Контекст и границы проверки
- Источник задачи: `memory-bank/pre-tasks.json` → `PRE-009`.
- Цель: проверить соответствие реализации требованиям `FR-001..FR-004` и критериям `AC-001..AC-004`.
- Проверенные требования:
  - `memory-bank/prd/02-functional-requirements.md` (FR-001..FR-004)
  - `memory-bank/prd/06-acceptance-criteria.md` (AC-001..AC-004)
- Проверенные ключевые файлы реализации:
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/UsersController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/service/UserService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/StudentController.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/config/SecurityConfig.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/security/JwtAuthenticationFilter.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/security/AppUserDetailsService.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/model/AppUser.java`
  - `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/user/*`
  - `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`

---

## 2) Матрица соответствия FR

| FR | Статус | Наблюдения |
|---|---|---|
| FR-001 (создание пользователя админом + опциональные первичные назначения) | **partial** | Создание пользователя админом реализовано (`POST /api/v1/admin/users`), базовые поля и аудит (`createdAt/createdBy`) поддержаны. Но в контракте создания нет `groupIds/courseIds`; первичные назначения в группы/курсы при создании не реализованы. `enabled` не задаётся из запроса (принудительно `true` в сервисе). |
| FR-002 (активация через email-ссылку) | **partial** | Invite-flow реализован: при создании без пароля генерируется `PasswordSetupToken`, отправляется ссылка, есть `POST /api/v1/auth/set-password`. Но отсутствует явная модель состояния «не активирован до установки пароля» (отдельный флаг/статус активации). |
| FR-003 (редактирование пользователей) | **partial** | Админ-редактирование реализовано (`PUT /api/v1/admin/users/{userId}`). Просмотр собственного профиля есть (`GET /api/v1/student/my/profile`), но редактирование своего профиля пользователем (в разрешённых полях) отсутствует как отдельный endpoint/сценарий (кроме загрузки аватара). |
| FR-004 (назначение/изменение ролей) | **implemented** | Изменение роли реализовано (`PATCH /api/v1/admin/users/{userId}/role`). Проверки доступа завязаны на роли и `@PreAuthorize`; права применяются на каждый запрос через загрузку актуального `UserDetails`, что обеспечивает мгновенное применение роли. |

---

## 3) Матрица соответствия AC

| AC | Статус | Доказательства / комментарий |
|---|---|---|
| AC-001 (создание + опциональные группы/курсы) | **partial** | Создание подтверждено кодом и интеграционным тестом. Часть про опциональные назначения групп/курсов при создании не покрыта: в `CreateUserRequest` нет соответствующих полей и в `createUser` отсутствует orchestration назначений. |
| AC-002 (invite/set-password/активация/вход) | **partial** | Invite и set-password подтверждены кодом и `UserAuthStudentFlowIntegrationTest` (успешная установка пароля и вход). Не подтверждена часть «аккаунт активируется только после установки пароля» на уровне отдельного доменного статуса. |
| AC-003 (админ редактирует любого, пользователь редактирует себя в разрешённых пределах) | **partial** | Админское редактирование есть и протестировано. Саморедактирование профиля пользователем в явном виде отсутствует. |
| AC-004 (смена роли без доп. активации) | **implemented** | Endpoint смены роли реализован; права проверяются по актуальной роли пользователя на каждом запросе. Интеграционный тест подтверждает сценарий повышения до ADMIN и доступ к admin-функциям. |

---

## 4) Результат по test_steps PRE-009

1. **Проверить код и endpoint по FR-001..004** — выполнено.
2. **Сопоставить поведение с AC-001..004** — выполнено.
3. **Записать результат в матрицу соответствия FR/AC** — выполнено (см. разделы 2 и 3).

---

## 5) Точечные gap-задачи для блока users/authz

1. **GAP-UA-001 (High)**: добавить первичные назначения при создании пользователя (groups/courses)
   - Расширить `CreateUserRequest` полями `groupIds` и `courseIds`.
   - Добавить orchestration в `UserService.createUser` с валидацией существования сущностей и атомарностью транзакции.

2. **GAP-UA-002 (High)**: ввести явное состояние активации аккаунта для invite-flow
   - Добавить отдельный признак (например, `passwordSetAt`/`activationStatus`) и правило допуска к логину до установки пароля.
   - Уточнить семантику `enabled` (active/inactive vs activation state).

3. **GAP-UA-003 (High)**: реализовать self-profile update для пользователя
   - Добавить endpoint уровня `student/my/profile` (PATCH/PUT) с белым списком разрешённых полей.
   - Добавить тесты на запрет изменения role/enabled/security-полей пользователем.

4. **GAP-UA-004 (Medium)**: усилить тест-покрытие FR-004
   - Добавить явный тест, что роль применяется «на лету» без перевыпуска токена (действующий JWT + изменение роли + повторный доступ).

---

## 6) Валидация тестами
- Выполнен релевантный интеграционный тест:
  - `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test`
  - Результат: **BUILD SUCCESS**, `Tests run: 1, Failures: 0, Errors: 0`.

## 7) Итог PRE-009
- FR-001..FR-004: **implemented = 1**, **partial = 3**, **missing = 0**.
- AC-001..AC-004: **implemented = 1**, **partial = 3**, **missing = 0**.
- Критичные пробелы сосредоточены в `FR-001..FR-003` (назначения при создании, явная активация аккаунта, self-edit профиля).