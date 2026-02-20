# PRE-013 — Сверка FR-018..FR-019 (change/reset password) и security/NFR

## 1) Контекст
- Задача: `PRE-013` из `memory-bank/pre-tasks.json`.
- Область проверки: password flows (`change/reset/set`), токены, обработка ошибок, security-требования.
- Источники требований:
  - `memory-bank/prd/02-functional-requirements.md` (`FR-018`, `FR-019`)
  - `memory-bank/prd/06-acceptance-criteria.md` (`AC-018`, `AC-019`)
  - `memory-bank/prd/03-non-functional-requirements.md` (`NFR-SEC-01..05`)

Проверенные файлы реализации:
- `controller/AuthController.java`
- `service/AuthService.java`
- `service/UserService.java`
- `model/PasswordSetupToken.java`
- `repository/PasswordSetupTokenRepository.java`
- `config/SecurityConfig.java`
- `security/JwtService.java`, `security/JwtAuthenticationFilter.java`, `security/AppUserDetailsService.java`
- `dto/auth/{ChangePasswordRequest,RecoverPasswordRequest,SetPasswordRequest}.java`
- `exception/GlobalExceptionHandler.java`
- `resources/application.yml`
- `resources/db/migration/V1__init_schema.sql`
- `test/controller/UserAuthStudentFlowIntegrationTest.java`

## 2) Матрица соответствия FR/AC

| ID | Статус | Наблюдение |
|---|---|---|
| FR-018 | implemented | Реализован `POST /api/v1/auth/change-password` (требует auth), есть проверка `currentPassword`, сохранение нового `passwordHash`, позитив/негатив покрыты интеграционным тестом. |
| AC-018 | implemented | Подтверждён сценарий: смена пароля успешна, старый пароль перестаёт работать (в тесте выполняется повторный login новым паролем; старый при смене проверяется как invalid). |
| FR-019 | partial | Реализованы `recover-password` + `set-password?token=...`; токен одноразовый (`usedAt`). Но отсутствует TTL/expiration токена, отсутствует anti-abuse/rate-limit, присутствует user enumeration по `recover-password`. |
| AC-019 | partial | Позитивный сценарий восстановления работает и покрыт тестом, но security-часть сценария неполная: токен не ограничен по времени жизни, нет защиты от массовых запросов/брутфорса reset-контура. |

## 3) Сверка по NFR-SEC

| NFR | Статус | Доказательство/комментарий |
|---|---|---|
| NFR-SEC-01 (JWT + expiry + hash) | partial | JWT подпись/expiry проверяются (`JwtService`, `JwtAuthenticationFilter`), пароли хранятся как hash (`BCryptPasswordEncoder`). Но reset-токен (`password_setup_tokens`) не имеет срока действия и проверяется только на `usedAt`. |
| NFR-SEC-02 (RBAC) | implemented | `change-password` защищён (`@PreAuthorize` + общая аутентификация), публичными оставлены только `login/set-password/recover-password`. |
| NFR-SEC-03 (минимизация утечек) | partial | Безопасные ответы есть для части auth-ошибок, но `handleOther()` возвращает `ex.getMessage()` в клиент, а `recover-password` на неизвестный email отвечает `404` — риск enumeration. |
| NFR-SEC-04 (rate-limit/anti-abuse) | missing | Для `login`/`recover-password` отсутствуют throttling, lockout, backoff. |
| NFR-SEC-05 (secret management) | missing | В `application.yml` захардкожены чувствительные значения (JWT secret, SMTP credentials, bootstrap admin password, pg password). |

## 4) Тестовое покрытие (по задаче)

Проверено:
- Интеграционный тест `UserAuthStudentFlowIntegrationTest` содержит сценарии:
  - `change-password` (успех + неверный `currentPassword`),
  - `recover-password` (успех + not-found email),
  - `set-password` (успех + повторное использование токена).
- Команда прогона: `mvn -pl monolith-mvp -Dtest=UserAuthStudentFlowIntegrationTest test`.
- Результат: `BUILD SUCCESS`.

Пробелы покрытия:
- Нет тестов на истечение reset-токена (поскольку TTL не реализован).
- Нет тестов на rate-limit/anti-bruteforce (механизм отсутствует).
- Нет тестов на безопасное поведение reset при неизвестном email без раскрытия факта существования пользователя.

## 5) Приоритизированные gap и рекомендации

### Critical
1. **Секреты в `application.yml`** (NFR-SEC-05): вынести JWT/SMTP/DB/bootstrap secrets в env/secret store.
2. **Нет rate-limit/anti-abuse** для `login` и `recover-password` (NFR-SEC-04).

### High
1. **TTL для reset/setup токенов**: добавить `expiresAt` + проверку в `AuthService.setPassword()`.
2. **User enumeration в recover-password**: возвращать одинаковый neutral-response для существующего/несуществующего email.

### Medium
1. **Утечка внутренних сообщений ошибок**: убрать `ex.getMessage()` из 500-ответа (`GlobalExceptionHandler.handleOther`).
2. **Политика сложности пароля**: добавить валидацию (min length/charset), т.к. сейчас только `@NotBlank`.

## 6) Проверка test_steps PRE-013

- Шаг 1: Проверить `AuthService/AuthController` по password flows — ✅
- Шаг 2: Сопоставить с `AC-018..AC-019` и `NFR-SEC` — ✅
- Шаг 3: Зафиксировать gap и приоритет исправлений — ✅
