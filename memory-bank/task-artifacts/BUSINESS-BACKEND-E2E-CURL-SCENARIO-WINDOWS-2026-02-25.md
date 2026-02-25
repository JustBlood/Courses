# BUSINESS BACKEND E2E SCENARIO (Windows / PowerShell + curl.exe) — 2026-02-25

Это Windows-версия того же кросс-функционального backend-сценария.
Формат: **PowerShell 5+/7 + curl.exe**.

> Важно: сценарий опирается на PRD + task-artifacts. Точные payload-поля сверяйте в Swagger: `http://localhost:8099/swagger-ui/index.html`.

---

## 0) Подготовка переменных и хелперов

```powershell
$BASE_URL = "http://localhost:8099"
$ADMIN_EMAIL = "admin@local"
$ADMIN_PASSWORD = "admin123"

# Тестовые пользователи
$U1_EMAIL = "student.invite@example.com"     # invite-flow
$U2_EMAIL = "student.direct@example.com"     # direct password
$U3_EMAIL = "reviewer.admin@example.com"     # reviewer (ADMIN)

$U1_PASS = "Start123!"
$U1_PASS_NEW = "Changed123!"
$U1_PASS_RESET = "Reset123!"
$U2_PASS = "Direct123!"

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $ADMIN_TOKEN
    )

    if ($null -ne $Body) {
        $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 20 -Compress }
        $tmpBodyPath = Join-Path $env:TEMP ("api-body-" + [guid]::NewGuid().ToString() + ".json")
        [System.IO.File]::WriteAllText($tmpBodyPath, $json, (New-Object System.Text.UTF8Encoding($false)))
        try {
            curl.exe -sS -X $Method "$BASE_URL$Path" `
              -H "Authorization: Bearer $Token" `
              -H "Content-Type: application/json" `
              --data-binary "@$tmpBodyPath"
        } finally {
            Remove-Item $tmpBodyPath -ErrorAction SilentlyContinue
        }
    } else {
        curl.exe -sS -X $Method "$BASE_URL$Path" `
          -H "Authorization: Bearer $Token"
    }
}

function Invoke-ApiNoAuth {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null
    )

    if ($null -ne $Body) {
        $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 20 -Compress }
        $tmpBodyPath = Join-Path $env:TEMP ("api-noauth-body-" + [guid]::NewGuid().ToString() + ".json")
        [System.IO.File]::WriteAllText($tmpBodyPath, $json, (New-Object System.Text.UTF8Encoding($false)))
        try {
            curl.exe -sS -X $Method "$BASE_URL$Path" `
              -H "Content-Type: application/json" `
              --data-binary "@$tmpBodyPath"
        } finally {
            Remove-Item $tmpBodyPath -ErrorAction SilentlyContinue
        }
    } else {
        curl.exe -sS -X $Method "$BASE_URL$Path"
    }
}
```

---

## 1) Поднять сервис в prod-like режиме (mail provider отключен)

```powershell
docker compose -f docker-compose.monolith.yml --env-file monolith.env up -d --build
curl.exe -sS "$BASE_URL/actuator/health"
```

Ожидание: `status=UP`.

---

## 2) Проверка базовой доступности API + логин админа

```powershell
$ADMIN_LOGIN = Invoke-ApiNoAuth -Method POST -Path "/api/v1/auth/login" -Body @{
  email    = $ADMIN_EMAIL
  password = $ADMIN_PASSWORD
}

$ADMIN_TOKEN = ($ADMIN_LOGIN | ConvertFrom-Json).token

# Важно: запускать именно в PowerShell. В cmd.exe кавычки/экранирование для JSON отличаются.
```

---

## 3) Блок USERS (полный жизненный цикл)

### 3.1 Создать пользователя без пароля (invite-flow)

```powershell
$u1Create = Invoke-Api -Method POST -Path "/api/v1/admin/users" -Body @{
  fullName   = "Student Invite"
  email      = $U1_EMAIL
  role       = "STUDENT"
  enabled    = $true
  sendInvite = $true
}
$U1_ID = ($u1Create | ConvertFrom-Json).id
```

Проверка: `activated=false` (по TASK-041).

### 3.2 Создать пользователя с установленным паролем

```powershell
$u2Create = Invoke-Api -Method POST -Path "/api/v1/admin/users" -Body @{
  fullName = "Student Direct"
  email    = $U2_EMAIL
  role     = "STUDENT"
  enabled  = $true
  password = $U2_PASS
}
$U2_ID = ($u2Create | ConvertFrom-Json).id
```

### 3.3 Создать reviewer-админа

```powershell
$u3Create = Invoke-Api -Method POST -Path "/api/v1/admin/users" -Body @{
  fullName = "Reviewer Admin"
  email    = $U3_EMAIL
  role     = "ADMIN"
  enabled  = $true
  password = "Admin123!"
}
$U3_ID = ($u3Create | ConvertFrom-Json).id
```

### 3.4 Получить пользователей, профиль и self-profile update

```powershell
Invoke-Api -Method GET -Path "/api/v1/admin/users"
Invoke-Api -Method GET -Path "/api/v1/admin/users/$U1_ID"

# После получения U1-токена:
# PATCH /api/v1/student/my/profile
```

### 3.5 Установка пароля пользователем по токену (invite)

При `mail-noop` токен можно взять из БД (тестовый контур):

```powershell
$U1_INVITE_TOKEN = docker exec -i monolith-postgres psql -U postgres -d courses -t -c "select pst.token from password_setup_tokens pst join users u on u.id=pst.user_id where u.email='$U1_EMAIL' and pst.used_at is null order by pst.created_at desc limit 1;"
$U1_INVITE_TOKEN = $U1_INVITE_TOKEN.Trim()

Invoke-ApiNoAuth -Method POST -Path "/api/v1/auth/set-password?token=$U1_INVITE_TOKEN" -Body @{ password = $U1_PASS }
```

### 3.6 Логин U1 и самостоятельная смена пароля

```powershell
$U1_LOGIN = Invoke-ApiNoAuth -Method POST -Path "/api/v1/auth/login" -Body @{ email = $U1_EMAIL; password = $U1_PASS }
$U1_TOKEN = ($U1_LOGIN | ConvertFrom-Json).token

Invoke-Api -Method POST -Path "/api/v1/auth/change-password" -Token $U1_TOKEN -Body @{ currentPassword = $U1_PASS; newPassword = $U1_PASS_NEW }
```

### 3.7 Восстановление пароля

```powershell
Invoke-ApiNoAuth -Method POST -Path "/api/v1/auth/recover-password" -Body @{ email = $U1_EMAIL }

# reset token забрать аналогично invite token
$U1_RESET_TOKEN = "<FROM_DB_OR_MAIL_ADAPTER>"
Invoke-ApiNoAuth -Method POST -Path "/api/v1/auth/set-password?token=$U1_RESET_TOKEN" -Body @{ password = $U1_PASS_RESET }
```

### 3.8 Обновление пользователя админом

```powershell
Invoke-Api -Method PUT -Path "/api/v1/admin/users/$U1_ID" -Body @{
  fullName = "Student Invite Updated"
  phone    = "+79990000000"
  comment  = "updated by admin"
  enabled  = $true
  role     = "ADMIN"
}
```

### 3.9 Активация/деактивация и удаление

```powershell
Invoke-Api -Method POST -Path "/api/v1/admin/users/activation" -Body @{ userIds = @($U1_ID); activate = $false }
Invoke-Api -Method POST -Path "/api/v1/admin/users/activation" -Body @{ userIds = @($U1_ID); activate = $true }

# удаление в конце:
# Invoke-Api -Method DELETE -Path "/api/v1/admin/users/$U2_ID"
```

---

## 4) Блок GROUPS

### 4.1 Создать группы разных типов

```powershell
$gCompany = Invoke-Api -Method POST -Path "/api/v1/admin/groups" -Body @{ title = "Company A"; type = "COMPANY" }
$gDept    = Invoke-Api -Method POST -Path "/api/v1/admin/groups" -Body @{ title = "Dept A"; type = "DEPARTMENT" }
$gPos1     = Invoke-Api -Method POST -Path "/api/v1/admin/groups" -Body @{ title = "Position A"; type = "POSITION" }
$gPos2     = Invoke-Api -Method POST -Path "/api/v1/admin/groups" -Body @{ title = "Position A2"; type = "POSITION" }
$gGeneral = Invoke-Api -Method POST -Path "/api/v1/admin/groups" -Body @{ title = "General A"; type = "GENERAL" }

$GENERAL_GROUP_ID = ($gGeneral | ConvertFrom-Json).id
$POSITION_GROUP_ID = ($gPos1 | ConvertFrom-Json).id
$POSITION2_GROUP_ID = ($gPos2 | ConvertFrom-Json).id
```

### 4.2 Добавить/удалить пользователей из групп

```powershell
Invoke-Api -Method POST -Path "/api/v1/admin/groups/$GENERAL_GROUP_ID/members" -Body @{ userIds = @($U1_ID, $U2_ID) }
Invoke-Api -Method DELETE -Path "/api/v1/admin/groups/$GENERAL_GROUP_ID/members" -Body @{ userIds = @($U2_ID) }
```

### 4.3 Негатив: запрет второй уникальной typed-group

```powershell
Invoke-Api -Method POST -Path "/api/v1/admin/groups/$POSITION_GROUP_ID/members" -Body @{ userIds = @($U1_ID) }
# Должен вернуть 4xx
Invoke-Api -Method POST -Path "/api/v1/admin/groups/$POSITION2_GROUP_ID/members" -Body @{ userIds = @($U1_ID) }
```

---

## 5) Блок COURSES / SECTIONS / PROGRAMS / ASSIGNMENTS

### 5.1 Создать раздел и курс внутри раздела

```powershell
$section = Invoke-Api -Method POST -Path "/api/v1/admin/sections" -Body @{ title = "Section E2E"; description = "main section"; priority = 10 }
$SECTION_ID = ($section | ConvertFrom-Json).id

$course = Invoke-Api -Method POST -Path "/api/v1/admin/sections/$SECTION_ID/courses" -Body @{
  title                  = "Course E2E"
  description            = "course for full flow"
  passingThresholdPercent= 60
  deadlineDays           = 30
  lessonsFreeOrder       = $false
  allowContinueAfterFail = $false
}
$COURSE_ID = ($course | ConvertFrom-Json).id
```

### 5.2 Добавить уроки (теория + практика)

```powershell
$theory1 = Invoke-Api -Method POST -Path "/api/v1/admin/courses/$COURSE_ID/lessons/theory" -Body @{
  title       = "Theory Text"
  contentType = "HTML_TEXT"
  content     = "<h1>Текстовая теория</h1><p>Контент урока</p>"
}
$THEORY_TEXT_ID = ($theory1 | ConvertFrom-Json).id

$theory2 = Invoke-Api -Method POST -Path "/api/v1/admin/courses/$COURSE_ID/lessons/theory" -Body @{
  title       = "Theory YouTube"
  contentType = "VIDEO_URL"
  content     = "https://youtu.be/dQw4w9WgXcQ"
}
$THEORY_VIDEO_ID = ($theory2 | ConvertFrom-Json).id

# THEORY PDF: сначала upload файла, затем создание урока со ссылкой (path) из upload response
$LOCAL_PDF_PATH = "C:\Users\User\Downloads\Ticket (2).pdf"   # подготовьте файл заранее
$pdfUploadRaw = curl.exe -sS -X POST "$BASE_URL/api/v1/files/upload" `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -F "file=@$LOCAL_PDF_PATH;type=application/pdf"
$pdfUpload = $pdfUploadRaw | ConvertFrom-Json
$PDF_CONTENT_PATH = $pdfUpload.path

$theoryPdf = Invoke-Api -Method POST -Path "/api/v1/admin/courses/$COURSE_ID/lessons/theory" -Body @{
  title       = "Theory PDF"
  contentType = "PDF_FILE"
  content     = $PDF_CONTENT_PATH
}
$THEORY_PDF_ID = ($theoryPdf | ConvertFrom-Json).id

# PRACTICE_TEST: все тестовые типы вопросов
$practiceTest = Invoke-Api -Method POST -Path "/api/v1/admin/courses/$COURSE_ID/lessons/practice" -Body @{
  title                   = "Practice Test All Types"
  lessonType              = "PRACTICE_TEST"
  passingThresholdPercent = 70
  attemptLimit            = 2
  timeLimitMinutes        = 20
  randomQuestionCount     = 4
  shuffleOptions          = $true
  showQuestionStatus      = $true
  showCorrectAnswers      = $false
  fullPoints              = 1
  partialPoints           = 0
  questions               = @(
    @{
      position       = 1
      questionType   = "SINGLE_CHOICE"
      questionText   = "2 + 2 = ?"
      options        = @("3", "4")
      correctAnswers = @("4")
      fullPoints     = 1
    },
    @{
      position       = 2
      questionType   = "MULTIPLE_CHOICE"
      questionText   = "Выберите простые числа"
      options        = @("2", "3", "4")
      correctAnswers = @("2", "3")
      fullPoints     = 2
      partialPoints  = 1
    },
    @{
      position       = 3
      questionType   = "MATCHING"
      questionText   = "Сопоставьте термины"
      options        = @("JVM=runtime", "JDK=tools")
      correctAnswers = @("JVM=runtime", "JDK=tools")
      fullPoints     = 2
      partialPoints  = 1
    },
    @{
      position       = 4
      questionType   = "ORDERING"
      questionText   = "Расставьте шаги по порядку"
      options        = @("1", "2", "3")
      correctAnswers = @("1", "2", "3")
      fullPoints     = 2
      partialPoints  = 1
    }
  )
}
$PRACTICE_TEST_ID = ($practiceTest | ConvertFrom-Json).id

# PRACTICE_OPEN_ANSWER: тип практики с ручной проверкой
$practiceOpen = Invoke-Api -Method POST -Path "/api/v1/admin/courses/$COURSE_ID/lessons/practice" -Body @{
  title                   = "Practice Open Answer"
  lessonType              = "PRACTICE_OPEN_ANSWER"
  passingThresholdPercent = 100
  attemptLimit            = 2
  timeLimitMinutes        = 30
  fullPoints              = 5
  partialPoints           = 2
  questions               = @(
    @{
      position      = 1
      questionType  = "OPEN_ANSWER"
      questionText  = "Опишите жизненный цикл JVM"
      trainerHint   = "Укажите этапы и назначение"
      fullPoints    = 5
      partialPoints = 2
    }
  )
}
$PRACTICE_OPEN_ID = ($practiceOpen | ConvertFrom-Json).id
```

### 5.3 Изменить параметры сущностей, порядок уроков и назначения

```powershell
# (опционально) получить текущие admin-детали курса, чтобы проверить порядок до изменений
Invoke-Api -Method GET -Path "/api/v1/admin/courses/$COURSE_ID"
```
# 5.3.1 Обновить курс + поменять порядок уроков через lessonIdToPosition
# ВАЖНО: lessonIdToPosition должен содержать ВСЕ уроки курса и позиции 1..N без пропусков.
```powershell
Invoke-Api -Method PUT -Path "/api/v1/admin/courses/$COURSE_ID" -Body @{
  title                   = "Course E2E Updated"
  description             = "course for full flow (updated)"
  authorFullName          = "E2E Admin"
  coverFilePath           = $PDF_CONTENT_PATH
  passingThresholdPercent = 75
  deadlineDays            = 45
  lessonsFreeOrder        = $true
  allowContinueAfterFail  = $true
  blockAfterDeadline      = $true
  keepAccessAfterDeadline = $false
  includeInOverallStats   = $true
  sectionId               = $SECTION_ID
  lessonIdToPosition      = @{
    "$PRACTICE_TEST_ID" = 1
    "$THEORY_TEXT_ID"   = 2
    "$THEORY_VIDEO_ID"  = 3
    "$THEORY_PDF_ID"    = 4
    "$PRACTICE_OPEN_ID" = 5
  }
}
```
# 5.3.2 Обновить теоретический урок (пример: VIDEO_URL)
```powershell
Invoke-Api -Method PUT -Path "/api/v1/admin/courses/$COURSE_ID/lessons/$THEORY_VIDEO_ID/theory" -Body @{
  title                     = "Theory YouTube Updated"
  description               = "video lesson updated"
  coverFilePath             = $PDF_CONTENT_PATH
  requiresPreviousCompleted = $true
  openForAccess             = $true
  stopLesson                = $false
  blockedDuringAttempt      = $true
  attemptLimit              = 1
  timeLimitMinutes          = 20
  contentType               = "VIDEO_URL"
  content                   = "https://youtu.be/aqz-KE-bpKQ"
  fullPoints                = 2
}
```

# 5.3.3 Обновить практический урок + изменить состав/порядок вопросов
```powershell
Invoke-Api -Method PUT -Path "/api/v1/admin/courses/$COURSE_ID/lessons/$PRACTICE_TEST_ID/practice" -Body @{
  title                   = "Practice Test Updated"
  description             = "test lesson updated"
  coverFilePath           = $PDF_CONTENT_PATH
  requiresPreviousCompleted = $true
  openForAccess           = $true
  stopLesson              = $false
  blockedDuringAttempt    = $true
  attemptLimit            = 3
  timeLimitMinutes        = 25
  lessonType              = "PRACTICE_TEST"
  fullPoints              = 2
  partialPoints           = 1
  passingThresholdPercent = 80
  evaluateByCorrectCount  = $false
  randomQuestionCount     = 3
  shuffleOptions          = $true
  showQuestionStatus      = $true
  showCorrectAnswers      = $true
  questions               = @(
    @{
      position       = 1
      questionType   = "MULTIPLE_CHOICE"
      questionText   = "Выберите JVM-языки"
      options        = @("Java", "Kotlin", "Python")
      correctAnswers = @("Java", "Kotlin")
      fullPoints     = 2
      partialPoints  = 1
    },
    @{
      position       = 2
      questionType   = "SINGLE_CHOICE"
      questionText   = "Главный артефакт JDK?"
      options        = @("компилятор", "Браузер")
      correctAnswers = @("компилятор")
      fullPoints     = 1
    },
    @{
      position       = 3
      questionType   = "ORDERING"
      questionText   = "Порядок 1-2-3"
      options        = @("1", "2", "3")
      correctAnswers = @("1", "2", "3")
      fullPoints     = 1
      partialPoints  = 1
    }
  )
}
```

# 5.3.4 Обновить раздел
```powershell
Invoke-Api -Method PUT -Path "/api/v1/admin/sections/$SECTION_ID" -Body @{
  title       = "Section E2E Updated"
  description = "section updated"
  priority    = 20
}
```

# 5.3.5 Сценарии изменения назначений (assign/unassign)
# reviewers
```powershell
Invoke-Api -Method POST -Path "/api/v1/admin/courses/$COURSE_ID/reviewers" -Body @{ ids = @($U3_ID) }
Invoke-Api -Method DELETE -Path "/api/v1/admin/courses/$COURSE_ID/reviewers" -Body @{ ids = @($U3_ID) }
Invoke-Api -Method POST -Path "/api/v1/admin/courses/$COURSE_ID/reviewers" -Body @{ ids = @($U3_ID) }

# enrollments
Invoke-Api -Method POST -Path "/api/v1/admin/courses/$COURSE_ID/enrollments" -Body @{ ids = @($U1_ID, $U2_ID) }
Invoke-Api -Method DELETE -Path "/api/v1/admin/courses/$COURSE_ID/enrollments" -Body @{ ids = @($U2_ID) }

# groups to course
Invoke-Api -Method POST -Path "/api/v1/admin/courses/$COURSE_ID/groups/assign" -Body @{ ids = @($GENERAL_GROUP_ID) }
Invoke-Api -Method DELETE -Path "/api/v1/admin/courses/$COURSE_ID/groups/assign" -Body @{ ids = @($GENERAL_GROUP_ID) }
Invoke-Api -Method POST -Path "/api/v1/admin/courses/$COURSE_ID/groups/assign" -Body @{ ids = @($GENERAL_GROUP_ID) }
```

### 5.5 Зачисление студентов и групп

```powershell
Invoke-Api -Method GET -Path "/api/v1/admin/courses/$COURSE_ID/enrollments/lists"
Invoke-Api -Method POST -Path "/api/v1/admin/courses/$COURSE_ID/enrollments" -Body @{ ids = @($U1_ID) }
```

### 5.6 Программы (post-MVP)

```powershell
$program = Invoke-Api -Method POST -Path "/api/v1/admin/courses/programs" -Body @{
  title           = "Program E2E"
  description     = "program for full flow"
  coverFilePath   = $PDF_CONTENT_PATH
  accessCondition = "PREVIOUS_COURSES_COMPLETED"
  blockAfterDeadline = $true
  courses         = @(
    @{ courseId = $COURSE_ID }
  )
}
$PROGRAM_ID = ($program | ConvertFrom-Json).id

Invoke-Api -Method PUT -Path "/api/v1/admin/courses/programs/$PROGRAM_ID" -Body @{
  title           = "Program E2E Updated"
  description     = "program updated"
  coverFilePath   = $PDF_CONTENT_PATH
  accessCondition = "ALL_OPEN"
  blockAfterDeadline = $false
  courses         = @(
    @{ courseId = $COURSE_ID }
  )
}

Invoke-Api -Method POST -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/assign" -Body @{ ids = @($U1_ID) }
Invoke-Api -Method POST -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/groups/assign" -Body @{ ids = @($GENERAL_GROUP_ID) }
```

---

## 6) Блок LEARNING FLOW (student + reviewer)

### 6.1 Студент открывает курс и уроки

```powershell
Invoke-Api -Method GET -Path "/api/v1/student/my/courses" -Token $U1_TOKEN
Invoke-Api -Method GET -Path "/api/v1/student/courses/$COURSE_ID" -Token $U1_TOKEN
Invoke-Api -Method GET -Path "/api/v1/student/lessons/$THEORY_TEXT_ID" -Token $U1_TOKEN
Invoke-Api -Method GET -Path "/api/v1/student/lessons/$THEORY_VIDEO_ID" -Token $U1_TOKEN
Invoke-Api -Method GET -Path "/api/v1/student/lessons/$THEORY_PDF_ID" -Token $U1_TOKEN
Invoke-Api -Method GET -Path "/api/v1/student/lessons/$PRACTICE_TEST_ID" -Token $U1_TOKEN
Invoke-Api -Method GET -Path "/api/v1/student/lessons/$PRACTICE_OPEN_ID" -Token $U1_TOKEN
```

### 6.3 Практика: успешный/неуспешный/fail-by-attempt-limit

```powershell
Invoke-Api -Method POST -Path "/api/v1/student/lessons/$PRACTICE_TEST_ID/submit-practice" -Token $U1_TOKEN -Body @{
  questionAnswers = @{
    "1" = @("3")
    "2" = @("2")
    "3" = @("JVM=runtime")
    "4" = @("2", "1", "3")
  }
}

Invoke-Api -Method POST -Path "/api/v1/student/lessons/$PRACTICE_TEST_ID/submit-practice" -Token $U1_TOKEN -Body @{
  questionAnswers = @{
    "1" = @("Java", "Kotlin")
    "2" = @("компилятор")
    "3" = @("1", "2", "3")
  }
}
```

Проверить отдельный lesson с `attemptLimit=2`: 3-я попытка -> 4xx.

### 6.4 Open-ended + review workflow

```powershell
# Логин reviewer (ADMIN) и получение токена
$U3_LOGIN = Invoke-ApiNoAuth -Method POST -Path "/api/v1/auth/login" -Body @{ email = $U3_EMAIL; password = "Admin123!" }
$U3_TOKEN = ($U3_LOGIN | ConvertFrom-Json).token

# Студент отправляет open-ended ответ (DTO: PracticeSubmissionRequest.openAnswer)
Invoke-Api -Method POST -Path "/api/v1/student/lessons/$PRACTICE_OPEN_ID/submit-practice" -Token $U1_TOKEN -Body @{ openAnswer = "my open answer" }

# Reviewer смотрит назначенные курсы и pending-ответы
Invoke-Api -Method GET -Path "/api/v1/admin/progress/reviews/courses" -Token $U3_TOKEN
$pendingRaw = Invoke-Api -Method GET -Path "/api/v1/admin/progress/reviews/pending" -Token $U3_TOKEN
$pending = @($pendingRaw | ConvertFrom-Json)
$pendingMatch = $pending |
  Where-Object { [long]$_.lessonId -eq [long]$PRACTICE_OPEN_ID -and [long]$_.studentId -eq [long]$U1_ID } |
  Sort-Object { [datetime]$_.submittedAt } -Descending |
  Select-Object -First 1
if (-not $pendingMatch) { throw "Pending submission for student $U1_ID and lesson $PRACTICE_OPEN_ID not found" }
$SUBMISSION_ID = [long]$pendingMatch.submissionId

# Reviewer -> REWORK (DTO: ReviewOpenSubmissionRequest)
Invoke-Api -Method POST -Path "/api/v1/admin/progress/reviews/$SUBMISSION_ID" -Token $U3_TOKEN -Body @{ passed = $false; partialPoints = $false; toNextReview = $true; comment = "need fix" }

# Студент дорабатывает и отправляет повторно
Invoke-Api -Method POST -Path "/api/v1/student/lessons/$PRACTICE_OPEN_ID/submit-practice" -Token $U1_TOKEN -Body @{ openAnswer = "my open answer (reworked)" }

$pending2Raw = Invoke-Api -Method GET -Path "/api/v1/admin/progress/reviews/pending" -Token $U3_TOKEN
$pending2 = @($pending2Raw | ConvertFrom-Json)
$pending2Match = $pending2 |
  Where-Object { [long]$_.lessonId -eq [long]$PRACTICE_OPEN_ID -and [long]$_.studentId -eq [long]$U1_ID } |
  Sort-Object { [datetime]$_.submittedAt } -Descending |
  Select-Object -First 1
if (-not $pending2Match) { throw "Second pending submission for student $U1_ID and lesson $PRACTICE_OPEN_ID not found" }
$SUBMISSION_ID_2 = [long]$pending2Match.submissionId

# Reviewer -> ACCEPTED
Invoke-Api -Method POST -Path "/api/v1/admin/progress/reviews/$SUBMISSION_ID_2" -Token $U3_TOKEN -Body @{ passed = $true; partialPoints = $false; toNextReview = $false; comment = "ok" }
```

---

## 7) Блок STATS / REPORTING

```powershell
curl.exe -sS -H "Authorization: Bearer $U1_TOKEN" "$BASE_URL/api/v1/student/my/stats"

Invoke-Api -Method GET -Path "/api/v1/admin/users/$U1_ID/stats"
Invoke-Api -Method GET -Path "/api/v1/admin/progress/courses/$COURSE_ID/stats"

curl.exe -sS -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE_URL/api/v1/admin/progress/courses/$COURSE_ID/summary-report.csv"
curl.exe -sS -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE_URL/api/v1/admin/progress/reports/summary.csv"
```

Проверить колонки по AC-017 (включая поля, которые по ТЗ всегда пустые).

---

## 8) Блок финальной очистки данных

```powershell
Invoke-Api -Method DELETE -Path "/api/v1/admin/courses/$COURSE_ID/groups/assign" -Body @{ ids = @($GENERAL_GROUP_ID) }
Invoke-Api -Method DELETE -Path "/api/v1/admin/courses/$COURSE_ID/enrollments" -Body @{ ids = @($U1_ID, $U2_ID) }

Invoke-Api -Method DELETE -Path "/api/v1/admin/users/$U1_ID"
Invoke-Api -Method DELETE -Path "/api/v1/admin/users/$U2_ID"
Invoke-Api -Method DELETE -Path "/api/v1/admin/users/$U3_ID"
```

---

## 9) Список архитектурных/контрактных вопросов и уточнений

1. Нужен ли служебный test-endpoint для invite/reset token при `mail-noop` (чтобы не читать БД)?
2. Фиксируем canonical enrollment API (`/enrollments`) и deprecation policy для legacy-ручек?
3. Review API: заменить связку `passed + toNextReview` на enum-переход статусов?
4. Утвердить единую матрицу `enabled / activated / deactivatedAt` для UI-статусов.
5. Что считать «стартом курса» для отчетов (первый open lesson / first submit / enrollment)?
6. Поля `login/cid/сертификат` в AC-017 всегда пустые — это временный или постоянный контракт?
7. Какой точный error-payload для UI при нарушении typed-group ограничений?
8. Основной файловый endpoint для UI: `/api/v1/files/upload` или `/api/v1/student/my/avatar`?
9. Обязателен ли student-кабинет программ как release-criteria в текущем релизе?
10. Batch-формат `/api/v1/admin/users/activation` утверждаем как основной контракт массового управления?
