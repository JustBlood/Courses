# BUSINESS BACKEND E2E SCENARIO (curl) — 2026-02-25

Ниже единый кросс-функциональный backend-сценарий для ручной/полуавтоматической приемки.
Он разбит на смысловые блоки и покрывает: runtime, auth, users, groups, courses/lessons, learning flow, review, statistics/reporting.

> Важно: сценарий опирается на PRD + task-artifacts. Для 100% запуска сверяйте точные payload-поля в Swagger (`/swagger-ui/index.html`) перед прогоном.

---

## 0) Подготовка переменных

```bash
export BASE_URL="http://localhost:8099"
export ADMIN_EMAIL="admin@local"
export ADMIN_PASSWORD="admin123"

# Тестовые пользователи
export U1_EMAIL="student.invite@example.com"      # без пароля (invite)
export U2_EMAIL="student.direct@example.com"      # с паролем при создании
export U3_EMAIL="reviewer.admin@example.com"      # reviewer (ADMIN)

export U1_PASS="Start123!"
export U1_PASS_NEW="Changed123!"
export U1_PASS_RESET="Reset123!"
export U2_PASS="Direct123!"

export AUTH_HEADER=""
```

Хелпер для запросов:

```bash
req() {
  method="$1"; path="$2"; body="$3"
  if [ -n "$body" ]; then
    curl -sS -X "$method" "$BASE_URL$path" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "$body"
  else
    curl -sS -X "$method" "$BASE_URL$path" \
      -H "Authorization: Bearer $ADMIN_TOKEN"
  fi
}
```

---

## 1) Поднять сервис в prod-like режиме (mail provider отключен)

```bash
docker compose -f docker-compose.monolith.yml --env-file monolith.env up -d --build
curl -sS "$BASE_URL/actuator/health"
```

Ожидание: `status=UP`.

---

## 2) Проверка базовой доступности API + логин админа

```bash
curl -sS -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}"
```

Сохранить `ADMIN_TOKEN` из ответа.

---

## 3) Блок USERS (полный жизненный цикл)

### 3.1 Создать пользователя без пароля (invite-flow)

```bash
U1_CREATE=$(req POST "/api/v1/admin/users" \
  "{\"fullName\":\"Student Invite\",\"email\":\"$U1_EMAIL\",\"role\":\"STUDENT\",\"enabled\":true,\"sendInvite\":true}")
```

Проверить: пользователь создан, `activated=false` (по TASK-041).

### 3.2 Создать пользователя с установленным паролем

```bash
U2_CREATE=$(req POST "/api/v1/admin/users" \
  "{\"fullName\":\"Student Direct\",\"email\":\"$U2_EMAIL\",\"role\":\"STUDENT\",\"enabled\":true,\"password\":\"$U2_PASS\"}")
```

### 3.3 Создать reviewer-админа

```bash
U3_CREATE=$(req POST "/api/v1/admin/users" \
  "{\"fullName\":\"Reviewer Admin\",\"email\":\"$U3_EMAIL\",\"role\":\"ADMIN\",\"enabled\":true,\"password\":\"Admin123!\"}")
```

### 3.4 Получить пользователей, их профиль и обновить профиль пользователя (self)

```bash
req GET "/api/v1/admin/users" ""
req GET "/api/v1/admin/users/{U1_ID}" ""

# Под U1 (после установки пароля) — PATCH /student/my/profile
```

### 3.5 Установка пароля пользователем по токену (invite)

Когда mail-noop включен, токен можно взять из DB (только для test-контура):

```bash
# пример (адаптируйте контейнер/SQL под ваш контур)
docker exec -i monolith-postgres psql -U postgres -d courses -t -c \
"select pst.token from password_setup_tokens pst
 join users u on u.id=pst.user_id
 where u.email='${U1_EMAIL}' and pst.used_at is null
 order by pst.created_at desc limit 1;"
```

Далее:

```bash
curl -sS -X POST "$BASE_URL/api/v1/auth/set-password?token=$U1_INVITE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"password\":\"$U1_PASS\"}"
```

### 3.6 Логин U1 и смена пароля самостоятельно

```bash
curl -sS -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$U1_EMAIL\",\"password\":\"$U1_PASS\"}"

curl -sS -X POST "$BASE_URL/api/v1/auth/change-password" \
  -H "Authorization: Bearer $U1_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"currentPassword\":\"$U1_PASS\",\"newPassword\":\"$U1_PASS_NEW\"}"
```

### 3.7 Восстановление пароля

```bash
curl -sS -X POST "$BASE_URL/api/v1/auth/recover-password" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$U1_EMAIL\"}"

# взять reset-токен аналогично invite-токену из БД/почтового адаптера
curl -sS -X POST "$BASE_URL/api/v1/auth/set-password?token=$U1_RESET_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"password\":\"$U1_PASS_RESET\"}"
```

### 3.8 Обновление пользователя админом (включая role/enabled/прочие поля)

```bash
req PUT "/api/v1/admin/users/{U1_ID}" \
"{\"fullName\":\"Student Invite Updated\",\"phone\":\"+79990000000\",\"comment\":\"updated by admin\",\"enabled\":true}"

req PATCH "/api/v1/admin/users/{U1_ID}/role" "{\"role\":\"STUDENT\"}"
```

### 3.9 Активация/деактивация и удаление

```bash
req POST "/api/v1/admin/users/activation" "{\"ids\":[\"{U1_ID}\"],\"enabled\":false}"
req POST "/api/v1/admin/users/activation" "{\"ids\":[\"{U1_ID}\"],\"enabled\":true}"

# удаление тестового пользователя (делать в конце сценария)
# req DELETE "/api/v1/admin/users/{U2_ID}" ""
```

---

## 4) Блок GROUPS

### 4.1 Создать группы разных типов

```bash
req POST "/api/v1/admin/groups" "{\"name\":\"Company A\",\"type\":\"COMPANY\"}"
req POST "/api/v1/admin/groups" "{\"name\":\"Dept A\",\"type\":\"DEPARTMENT\"}"
req POST "/api/v1/admin/groups" "{\"name\":\"Position A\",\"type\":\"POSITION\"}"
req POST "/api/v1/admin/groups" "{\"name\":\"General A\",\"type\":\"GENERAL\"}"
```

### 4.2 Добавить/удалить пользователей из групп

```bash
req POST "/api/v1/admin/groups/{GROUP_ID}/users" "{\"userIds\":[\"{U1_ID}\",\"{U2_ID}\"]}"
req DELETE "/api/v1/admin/groups/{GROUP_ID}/users" "{\"userIds\":[\"{U2_ID}\"]}"
```

### 4.3 Негатив: запрет второй уникальной typed-group

```bash
# попытка добавить U1 во вторую COMPANY-группу
req POST "/api/v1/admin/groups/{SECOND_COMPANY_GROUP_ID}/users" "{\"userIds\":[\"{U1_ID}\"]}"
```

Ожидание: 4xx (валидационная ошибка).

---

## 5) Блок COURSES / SECTIONS / PROGRAMS / ASSIGNMENTS

### 5.1 Создать раздел и курс внутри раздела

```bash
req POST "/api/v1/admin/sections" "{\"name\":\"Section E2E\",\"description\":\"main section\",\"priority\":10}"
req POST "/api/v1/admin/sections/{SECTION_ID}/courses" \
"{\"title\":\"Course E2E\",\"description\":\"course for full flow\",\"threshold\":60,\"deadlineDays\":30,\"lessonsFreeOrder\":false,\"allowContinueAfterFail\":false}"
```

### 5.2 Добавить уроки (теория + практика)

```bash
req POST "/api/v1/admin/courses/{COURSE_ID}/lessons/theory" \
"{\"title\":\"Theory Text\",\"contentType\":\"TEXT\",\"content\":\"Hello\"}"

req POST "/api/v1/admin/courses/{COURSE_ID}/lessons/theory" \
"{\"title\":\"Theory YouTube\",\"contentType\":\"YOUTUBE\",\"content\":\"https://youtu.be/dQw4w9WgXcQ\"}"

req POST "/api/v1/admin/courses/{COURSE_ID}/lessons/practice" \
"{\"title\":\"Practice Mixed\",\"passingThresholdPercent\":70,\"attemptLimit\":2,\"timeLimitMinutes\":20,\"shuffleOnEveryAttempt\":true,\"randomQuestionCount\":3,\"questions\":[...]}"
```

### 5.3 Изменить параметры сущностей

```bash
req PUT "/api/v1/admin/courses/{COURSE_ID}" "{...updated course fields...}"
req PUT "/api/v1/admin/courses/{COURSE_ID}/lessons/{THEORY_ID}/theory" "{...}"
req PUT "/api/v1/admin/courses/{COURSE_ID}/lessons/{PRACTICE_ID}/practice" "{...}"
req PUT "/api/v1/admin/sections/{SECTION_ID}" "{\"name\":\"Section E2E Updated\",\"priority\":20}"
```

### 5.4 Назначить/убрать reviewer

```bash
req POST "/api/v1/admin/courses/{COURSE_ID}/reviewers" "{\"ids\":[\"{U3_ID}\"]}"
req DELETE "/api/v1/admin/courses/{COURSE_ID}/reviewers" "{\"ids\":[\"{U3_ID}\"]}"
req POST "/api/v1/admin/courses/{COURSE_ID}/reviewers" "{\"ids\":[\"{U3_ID}\"]}"
```

### 5.5 Зачисление студентов и групп

```bash
req GET "/api/v1/admin/courses/{COURSE_ID}/enrollments/lists" ""
req POST "/api/v1/admin/courses/{COURSE_ID}/enrollments" "{\"ids\":[\"{U1_ID}\"]}"
req POST "/api/v1/admin/courses/{COURSE_ID}/groups/assign" "{\"groupIds\":[\"{GENERAL_GROUP_ID}\"]}"
req DELETE "/api/v1/admin/courses/{COURSE_ID}/enrollments" "{\"ids\":[\"{U2_ID}\"]}"
```

### 5.6 Программы (post-MVP)

```bash
req POST "/api/v1/admin/courses/programs" "{\"title\":\"Program E2E\",\"courseIds\":[{COURSE_ID}],\"accessCondition\":\"SEQUENTIAL\"}"
req PUT "/api/v1/admin/courses/programs/{PROGRAM_ID}" "{...reordered/updated...}"
req POST "/api/v1/admin/courses/programs/{PROGRAM_ID}/groups/assign" "{\"groupIds\":[\"{GENERAL_GROUP_ID}\"]}"
```

---

## 6) Блок LEARNING FLOW (student + reviewer)

### 6.1 Студент открывает курс и уроки

```bash
curl -sS -H "Authorization: Bearer $U1_TOKEN" "$BASE_URL/api/v1/student/my/courses"
curl -sS -H "Authorization: Bearer $U1_TOKEN" "$BASE_URL/api/v1/student/courses/{COURSE_ID}"
curl -sS -H "Authorization: Bearer $U1_TOKEN" "$BASE_URL/api/v1/student/lessons/{THEORY_ID}"
```

### 6.2 Прохождение теории

```bash
curl -sS -X POST -H "Authorization: Bearer $U1_TOKEN" \
  "$BASE_URL/api/v1/student/lessons/{THEORY_ID}/complete-theory"
```

### 6.3 Практика: успешный/неуспешный/fail-by-attempt-limit

```bash
# неуспешная попытка
curl -sS -X POST "$BASE_URL/api/v1/student/lessons/{PRACTICE_ID}/submit-practice" \
  -H "Authorization: Bearer $U1_TOKEN" -H "Content-Type: application/json" \
  -d "{\"questionAnswers\":{...mostly wrong...}}"

# успешная попытка
curl -sS -X POST "$BASE_URL/api/v1/student/lessons/{PRACTICE_ID}/submit-practice" \
  -H "Authorization: Bearer $U1_TOKEN" -H "Content-Type: application/json" \
  -d "{\"questionAnswers\":{...enough correct...}}"
```

Отдельно проверить lesson с `attemptLimit=2`: третья попытка должна дать 4xx.

### 6.4 Open-ended + review workflow

```bash
# студент отправляет open-ended
curl -sS -X POST "$BASE_URL/api/v1/student/lessons/{OPEN_LESSON_ID}/submit-practice" \
  -H "Authorization: Bearer $U1_TOKEN" -H "Content-Type: application/json" \
  -d "{\"questionAnswers\":{\"{OPEN_QUESTION_ID}\":\"my open answer\"}}"

# reviewer смотрит свои курсы и pending
curl -sS -H "Authorization: Bearer $U3_TOKEN" "$BASE_URL/api/v1/admin/progress/reviews/courses"
curl -sS -H "Authorization: Bearer $U3_TOKEN" "$BASE_URL/api/v1/admin/progress/reviews/pending"

# reviewer -> REWORK
curl -sS -X POST "$BASE_URL/api/v1/admin/progress/reviews/{SUBMISSION_ID}" \
  -H "Authorization: Bearer $U3_TOKEN" -H "Content-Type: application/json" \
  -d "{\"passed\":false,\"toNextReview\":true,\"reviewComment\":\"need fix\"}"

# студент дорабатывает и отправляет повторно
# reviewer -> ACCEPTED
curl -sS -X POST "$BASE_URL/api/v1/admin/progress/reviews/{SUBMISSION_ID_2}" \
  -H "Authorization: Bearer $U3_TOKEN" -H "Content-Type: application/json" \
  -d "{\"passed\":true,\"toNextReview\":false,\"reviewComment\":\"ok\"}"
```

---

## 7) Блок STATS / REPORTING

```bash
# личная статистика студента
curl -sS -H "Authorization: Bearer $U1_TOKEN" "$BASE_URL/api/v1/student/my/stats"

# статистика пользователя для админа
req GET "/api/v1/admin/users/{U1_ID}/stats" ""

# статистика по курсу
req GET "/api/v1/admin/progress/courses/{COURSE_ID}/stats" ""

# course-specific summary report
curl -sS -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE_URL/api/v1/admin/progress/courses/{COURSE_ID}/summary-report.csv"

# global summary report
curl -sS -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE_URL/api/v1/admin/progress/reports/summary.csv"
```

Проверить колонки по AC-017, включая поля, которые по ТЗ всегда пустые.

---

## 8) Блок финальной очистки данных

```bash
# отписать группу/студента от курса
req DELETE "/api/v1/admin/courses/{COURSE_ID}/groups/assign" "{\"groupIds\":[\"{GENERAL_GROUP_ID}\"]}"
req DELETE "/api/v1/admin/courses/{COURSE_ID}/enrollments" "{\"ids\":[\"{U1_ID}\",\"{U2_ID}\"]}"

# удалить пользователей (кроме bootstrap-admin)
req DELETE "/api/v1/admin/users/{U1_ID}" ""
req DELETE "/api/v1/admin/users/{U2_ID}" ""
req DELETE "/api/v1/admin/users/{U3_ID}" ""
```

---

## 9) Список архитектурных/контрактных вопросов и уточнений

1. **Invite/reset flow при `mail-noop`:** нужен ли официальный test-endpoint для выдачи последнего токена (вместо чтения из БД)?
2. **Дубли enrollment-контрактов:** исторически встречались `/assign` и `/enrollments`. Фиксируем единый canonical endpoint и deprecation policy?
3. **Review API:** комбинация `passed` + `toNextReview` двусмысленна. Нужен ли enum-переход (`REWORK`, `ACCEPTED`, `REJECTED`) вместо двух boolean?
4. **User state model:** как трактовать `enabled` vs `activated` vs `deactivatedAt` для UI (единая матрица статусов/бейджей)?
5. **Course start semantics:** нет явного `start-course` endpoint. Что считается стартом курса для отчета (первое открытие урока / первая отправка / зачисление)?
6. **Reports AC-017:** поля `login`, `cid`, часть сертификатных полей всегда пустые. Это временно (MVP) или долгосрочный контракт?
7. **Group typed-constraint:** какой точный код и error-payload должен ожидать UI при нарушении правила «не более одной COMPANY/DEPARTMENT/POSITION»?
8. **Files API:** основной путь для UI — `/api/v1/files/upload` или `/api/v1/student/my/avatar` (или оба с разным назначением)?
9. **Program assignment visibility:** есть ли обязательный endpoint для student-кабинета программ как release-criteria в текущем релизе?
10. **Activation endpoint contract:** подтверждаем batch-формат для toggle activation как основной для UI-операций массового управления?
