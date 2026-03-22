<#
ADHOC FULL BACKEND E2E — one-click chain
Date: 2026-03-18

Запуск (из корня репозитория):
  powershell -ExecutionPolicy Bypass -File .\memory-bank\task-artifacts\ADHOC-FULL-BACKEND-E2E-ONECLICK-CHAIN-2026-03-18.ps1

Опциональные env:
  E2E_BASE_URL, E2E_ADMIN_EMAIL, E2E_ADMIN_PASSWORD,
  E2E_PG_CONTAINER, E2E_PG_DB, E2E_PG_USER
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$BASE_URL = if ($env:E2E_BASE_URL) { $env:E2E_BASE_URL } else { "http://localhost:8099" }
$ADMIN_EMAIL = if ($env:E2E_ADMIN_EMAIL) { $env:E2E_ADMIN_EMAIL } else { "admin@local" }
$ADMIN_PASSWORD = if ($env:E2E_ADMIN_PASSWORD) { $env:E2E_ADMIN_PASSWORD } else { "123" }
$PG_CONTAINER = if ($env:E2E_PG_CONTAINER) { $env:E2E_PG_CONTAINER } else { "monolith-postgres" }
$PG_DB = if ($env:E2E_PG_DB) { $env:E2E_PG_DB } else { "courses" }
$PG_USER = if ($env:E2E_PG_USER) { $env:E2E_PG_USER } else { "postgres" }

$RUN = "run-$(Get-Date -Format 'yyyyMMdd-HHmm')"

function Step([string]$Text) {
    Write-Host "`n=== $Text ===" -ForegroundColor Cyan
}

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) {
        throw "ASSERT FAILED: $Message"
    }
}

function Convert-RawToText([object]$Raw) {
    if ($null -eq $Raw) { return "" }

    if ($Raw -is [string]) {
        return $Raw
    }

    if ($Raw -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Raw)
    }

    return [string]$Raw
}

function Parse-JsonOrNull([object]$Raw) {
    $text = Convert-RawToText $Raw
    if ([string]::IsNullOrWhiteSpace($text)) { return $null }
    try { return $text | ConvertFrom-Json } catch { return $null }
}

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [object]$Body = $null,
        [string]$Token = $null,
        [int[]]$ExpectedStatus = @(200)
    )

    $url = "$BASE_URL$Path"
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $status = $null
    $raw = ""

    try {
        if ($null -ne $Body) {
            $jsonBody = $Body | ConvertTo-Json -Depth 50 -Compress
            $resp = Invoke-WebRequest -Method $Method -Uri $url -Headers $headers -ContentType "application/json" -Body $jsonBody -UseBasicParsing
        } else {
            $resp = Invoke-WebRequest -Method $Method -Uri $url -Headers $headers -UseBasicParsing
        }
        $status = [int]$resp.StatusCode
        $raw = Convert-RawToText $resp.Content
    }
    catch {
        if ($null -eq $_.Exception.Response) {
            throw
        }

        $webResp = $_.Exception.Response
        $status = [int]$webResp.StatusCode
        $stream = $webResp.GetResponseStream()
        if ($null -ne $stream) {
            $reader = New-Object System.IO.StreamReader($stream)
            try {
                $raw = $reader.ReadToEnd()
            }
            finally {
                $reader.Close()
            }
        }
    }

    if ($ExpectedStatus -notcontains $status) {
        throw "Unexpected HTTP $status for [$Method $Path]. Expected: $($ExpectedStatus -join ', '). Body: $raw"
    }

    return [PSCustomObject]@{
        Status = $status
        Raw = $raw
        Json = Parse-JsonOrNull $raw
    }
}

function Invoke-FileUpload {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [string]$Token = $null,
        [int[]]$ExpectedStatus = @(200)
    )

    $tmpOut = [System.IO.Path]::GetTempFileName()
    try {
        $args = @("-sS", "-o", $tmpOut, "-w", "%{http_code}", "-X", "POST", "$BASE_URL/api/v1/files/upload")
        if ($Token) {
            $args += @("-H", "Authorization: Bearer $Token")
        }
        $args += @("-F", "file=@$FilePath")

        $statusText = (& curl.exe @args).Trim()
        $status = [int]$statusText
        $raw = Get-Content -Path $tmpOut -Raw

        if ($ExpectedStatus -notcontains $status) {
            throw "Unexpected HTTP $status for file upload. Expected: $($ExpectedStatus -join ', '). Body: $raw"
        }

        return [PSCustomObject]@{
            Status = $status
            Raw = $raw
            Json = Parse-JsonOrNull $raw
        }
    }
    finally {
        Remove-Item -Path $tmpOut -ErrorAction SilentlyContinue
    }
}

function Login {
    param(
        [Parameter(Mandatory = $true)][string]$Email,
        [Parameter(Mandatory = $true)][string]$Password,
        [int[]]$ExpectedStatus = @(200)
    )

    return Invoke-Api -Method "POST" -Path "/api/v1/auth/login" -Body @{
        email = $Email
        password = $Password
    } -ExpectedStatus $ExpectedStatus
}

function Get-SetupToken([string]$Email) {
    $safeEmail = $Email.Replace("'", "''")
    $sql = "select pst.token from password_setup_tokens pst join users u on u.id = pst.user_id where u.email = '$safeEmail' and pst.used_at is null order by pst.created_at desc limit 1;"
    $token = (& docker exec -i $PG_CONTAINER psql -U $PG_USER -d $PG_DB -t -A -c $sql).Trim()
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "Password setup token not found for email: $Email"
    }
    return $token
}

function InList-ContainsUserId($listDto, [long]$userId) {
    $ids = @($listDto.in | ForEach-Object { [long]$_.id })
    return ($ids -contains $userId)
}

Step "A. Health + security smoke"
$health = Invoke-Api -Method "GET" -Path "/actuator/health" -ExpectedStatus @(200)
Assert-True ($health.Json.status -eq "UP") "Health must be UP"

$adminLogin = Login -Email $ADMIN_EMAIL -Password $ADMIN_PASSWORD -ExpectedStatus @(200)
$ADMIN_TOKEN = [string]$adminLogin.Json.token
Assert-True (-not [string]::IsNullOrWhiteSpace($ADMIN_TOKEN)) "Admin token must be present"

Invoke-Api -Method "GET" -Path "/api/v1/admin/users" -ExpectedStatus @(401) | Out-Null

Step "B. Users: create/read/update/activation/recover/self-profile"
$ADMIN_REVIEWER_EMAIL = "adminReviewer+$RUN@example.local"
$STUDENT_A_EMAIL = "studentA+$RUN@example.local"
$STUDENT_B_EMAIL = "studentB+$RUN@example.local"
$STUDENT_C_EMAIL = "studentC+$RUN@example.local"

$ADMIN_REVIEWER_PASS = "AdminReviewer123!"
$STUDENT_A_PASS_1 = "StudentA123!"
$STUDENT_A_PASS_2 = "StudentAReset123!"
$STUDENT_B_PASS = "StudentB123!"
$STUDENT_C_PASS = "StudentC123!"

$adminReviewer = Invoke-Api -Method "POST" -Path "/api/v1/admin/users" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    fullName = "adminReviewer $RUN"
    email = $ADMIN_REVIEWER_EMAIL
    role = "ADMIN"
    password = $ADMIN_REVIEWER_PASS
    phone = "+70000000001"
    snils = "111-111-111 01"
    comment = "reviewer"
}
$ADMIN_REVIEWER_ID = [long]$adminReviewer.Json.id

$studentA = Invoke-Api -Method "POST" -Path "/api/v1/admin/users" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    fullName = "studentA $RUN"
    email = $STUDENT_A_EMAIL
    role = "STUDENT"
    password = $STUDENT_A_PASS_1
    phone = "+70000000002"
    snils = "111-111-111 02"
    comment = "studentA comment"
}
$STUDENT_A_ID = [long]$studentA.Json.id

$studentB = Invoke-Api -Method "POST" -Path "/api/v1/admin/users" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    fullName = "studentB $RUN"
    email = $STUDENT_B_EMAIL
    role = "STUDENT"
    password = $STUDENT_B_PASS
}
$STUDENT_B_ID = [long]$studentB.Json.id

$studentC = Invoke-Api -Method "POST" -Path "/api/v1/admin/users" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    fullName = "studentC $RUN"
    email = $STUDENT_C_EMAIL
    role = "STUDENT"
}
$STUDENT_C_ID = [long]$studentC.Json.id

$usersList = Invoke-Api -Method "GET" -Path "/api/v1/admin/users" -Token $ADMIN_TOKEN -ExpectedStatus @(200)
Assert-True (@($usersList.Json).Count -ge 4) "Users list must contain created users"

$studentAGet = Invoke-Api -Method "GET" -Path "/api/v1/admin/users/$STUDENT_A_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200)
Assert-True ($studentAGet.Json.email -eq $STUDENT_A_EMAIL) "studentA email mismatch"

Invoke-Api -Method "PUT" -Path "/api/v1/admin/users/$STUDENT_A_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    fullName = "studentA updated $RUN"
    phone = "+70000000999"
    snils = "111-111-111 99"
    comment = "studentA updated comment"
} | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/admin/users/activation" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    activate = $false
    userIds = @($STUDENT_B_ID)
} | Out-Null
Login -Email $STUDENT_B_EMAIL -Password $STUDENT_B_PASS -ExpectedStatus @(401) | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/users/activation" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    activate = $true
    userIds = @($STUDENT_B_ID)
} | Out-Null
Login -Email $STUDENT_B_EMAIL -Password $STUDENT_B_PASS -ExpectedStatus @(200) | Out-Null

$studentCInviteToken = Get-SetupToken -Email $STUDENT_C_EMAIL
Invoke-Api -Method "POST" -Path "/api/v1/auth/set-password?token=$studentCInviteToken" -ExpectedStatus @(200) -Body @{ password = $STUDENT_C_PASS } | Out-Null
Login -Email $STUDENT_C_EMAIL -Password $STUDENT_C_PASS -ExpectedStatus @(200) | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/auth/recover-password" -ExpectedStatus @(200) -Body @{ email = $STUDENT_A_EMAIL } | Out-Null
$studentAResetToken = Get-SetupToken -Email $STUDENT_A_EMAIL
Invoke-Api -Method "POST" -Path "/api/v1/auth/set-password?token=$studentAResetToken" -ExpectedStatus @(200) -Body @{ password = $STUDENT_A_PASS_2 } | Out-Null
Login -Email $STUDENT_A_EMAIL -Password $STUDENT_A_PASS_1 -ExpectedStatus @(401) | Out-Null
$studentALogin = Login -Email $STUDENT_A_EMAIL -Password $STUDENT_A_PASS_2 -ExpectedStatus @(200)
$STUDENT_A_TOKEN = [string]$studentALogin.Json.token

Invoke-Api -Method "GET" -Path "/api/v1/student/my/profile" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "PATCH" -Path "/api/v1/student/my/profile" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) -Body @{
    fullName = "studentA selfprofile $RUN"
    phone = "+70000000123"
} | Out-Null
Invoke-Api -Method "PATCH" -Path "/api/v1/student/my/profile" -Token $STUDENT_A_TOKEN -ExpectedStatus @(403) -Body @{
    role = "ADMIN"
} | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/student/my/last-visit" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null

Step "C. Files upload"
$uploadTmp = Join-Path $env:TEMP "e2e-$RUN.txt"
Set-Content -Path $uploadTmp -Value "E2E $RUN" -Encoding UTF8

$uploadResp = Invoke-FileUpload -FilePath $uploadTmp -Token $ADMIN_TOKEN -ExpectedStatus @(200)
$UPLOADED_LINK = [string]$uploadResp.Json.link
Assert-True ($UPLOADED_LINK.StartsWith("/files/")) "Upload link must start with /files/"

Invoke-FileUpload -FilePath $uploadTmp -ExpectedStatus @(401) | Out-Null

Invoke-Api -Method "PUT" -Path "/api/v1/admin/users/$STUDENT_A_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    avatarFilePath = $UPLOADED_LINK
} | Out-Null

Step "D. Groups"
function Create-Group([string]$title, [string]$type) {
    $resp = Invoke-Api -Method "POST" -Path "/api/v1/admin/groups" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
        title = $title
        type = $type
    }
    return [string]$resp.Json.id
}

$G_GENERAL_1 = Create-Group "gGeneral1-$RUN" "GENERAL"
$G_GENERAL_2 = Create-Group "gGeneral2-$RUN" "GENERAL"
$G_COMPANY_1 = Create-Group "gCompany1-$RUN" "COMPANY"
$G_COMPANY_2 = Create-Group "gCompany2-$RUN" "COMPANY"
$G_DEPARTMENT_1 = Create-Group "gDepartment1-$RUN" "DEPARTMENT"
$G_POSITION_1 = Create-Group "gPosition1-$RUN" "POSITION"
$G_POSITION_2 = Create-Group "gPosition2-$RUN" "POSITION"

Invoke-Api -Method "GET" -Path "/api/v1/admin/groups" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "GET" -Path "/api/v1/admin/groups/$G_GENERAL_1" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "GET" -Path "/api/v1/admin/groups/users/$STUDENT_A_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_GENERAL_1/members" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($STUDENT_A_ID, $STUDENT_B_ID) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_COMPANY_1/members" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($STUDENT_A_ID) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_POSITION_1/members" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($STUDENT_A_ID) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_DEPARTMENT_1/members" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($STUDENT_B_ID) } | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_COMPANY_2/members" -Token $ADMIN_TOKEN -ExpectedStatus @(400) -Body @{ ids = @($STUDENT_A_ID) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_POSITION_2/members" -Token $ADMIN_TOKEN -ExpectedStatus @(400) -Body @{ ids = @($STUDENT_A_ID) } | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_GENERAL_1/members" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($STUDENT_A_ID) } | Out-Null
Invoke-Api -Method "DELETE" -Path "/api/v1/admin/groups/$G_GENERAL_1/members" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($STUDENT_B_ID) } | Out-Null
Invoke-Api -Method "DELETE" -Path "/api/v1/admin/groups/$G_GENERAL_1/members" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($STUDENT_B_ID) } | Out-Null

Invoke-Api -Method "PUT" -Path "/api/v1/admin/groups/$G_GENERAL_2" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ title = "gGeneral2-renamed-$RUN"; type = "GENERAL" } | Out-Null

Step "E. Sections"
$section = Invoke-Api -Method "POST" -Path "/api/v1/admin/sections" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    title = "sCore-$RUN"
    description = "core section"
    priority = 10
}
$SECTION_ID = [long]$section.Json.id

Invoke-Api -Method "GET" -Path "/api/v1/admin/sections" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "GET" -Path "/api/v1/admin/sections/$SECTION_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "PUT" -Path "/api/v1/admin/sections/$SECTION_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    title = "sCore-updated-$RUN"
    description = "core section updated"
    priority = 20
} | Out-Null

Step "F. Courses + Lessons + Questions"
function Create-Course([string]$title) {
    $resp = Invoke-Api -Method "POST" -Path "/api/v1/admin/courses" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
        title = $title
        description = "$title description"
        authorFullName = "E2E Admin"
        coverFilePath = $UPLOADED_LINK
        deadlineDays = 30
        lessonsFreeOrder = $false
        sectionId = $SECTION_ID
    }
    return [long]$resp.Json.id
}

$COURSE_BASE = Create-Course "cJavaBase-$RUN"
$COURSE_PRACTICE = Create-Course "cJavaPractice-$RUN"
$COURSE_FINAL = Create-Course "cJavaFinal-$RUN"

Invoke-Api -Method "GET" -Path "/api/v1/admin/courses" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "GET" -Path "/api/v1/admin/courses/$COURSE_BASE" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null

$theoryText = Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/lessons/theory" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    title = "THEORY TEXT $RUN"
    description = "theory text"
    stopLesson = $false
    timeLimitMinutes = 30
    lessonType = "THEORY_TEXT"
    content = "Java text content"
    fullPoints = 1
}
$LESSON_THEORY_TEXT = [long]$theoryText.Json.id

$theoryVideo = Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/lessons/theory" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    title = "THEORY VIDEO $RUN"
    description = "theory video"
    stopLesson = $false
    timeLimitMinutes = 30
    lessonType = "THEORY_VIDEO"
    content = "https://youtu.be/dQw4w9WgXcQ"
    fullPoints = 1
}
$LESSON_THEORY_VIDEO = [long]$theoryVideo.Json.id

$theoryPdf = Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/lessons/theory" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    title = "THEORY PDF $RUN"
    description = "theory pdf"
    stopLesson = $false
    timeLimitMinutes = 30
    lessonType = "THEORY_PDF"
    content = $UPLOADED_LINK
    fullPoints = 1
}
$LESSON_THEORY_PDF = [long]$theoryPdf.Json.id

$practiceTest = Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/lessons/practice" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    title = "PRACTICE TEST $RUN"
    description = "practice test"
    stopLesson = $false
    attemptLimit = 3
    timeLimitMinutes = 25
    lessonType = "PRACTICE_TEST"
    passingThresholdPercent = 70
    shuffleOptions = $false
    showQuestionStatus = $true
    showCorrectAnswersAfterCompletion = $false
    questions = @(
        @{ position = 1; questionType = "SINGLE_CHOICE"; questionText = "2+2?"; options = @("3","4"); correctAnswers = @("4"); fullPoints = 2 },
        @{ position = 2; questionType = "MULTIPLE_CHOICE"; questionText = "choose vowels"; options = @("A","B","E"); correctAnswers = @("A","E"); fullPoints = 2; partialPoints = 1 },
        @{ position = 3; questionType = "ORDERING"; questionText = "order"; options = @("1","2","3"); correctAnswers = @("1","2","3"); fullPoints = 2; partialPoints = 1 }
    )
}
$LESSON_PRACTICE_TEST = [long]$practiceTest.Json.id
$Q_TEST_1 = [long]$practiceTest.Json.questions[0].id
$Q_TEST_2 = [long]$practiceTest.Json.questions[1].id
$Q_TEST_3 = [long]$practiceTest.Json.questions[2].id

$practiceOpen = Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/lessons/practice" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    title = "PRACTICE OPEN $RUN"
    description = "practice open"
    stopLesson = $false
    attemptLimit = 2
    timeLimitMinutes = 30
    lessonType = "PRACTICE_OPEN_ANSWER"
    passingThresholdPercent = 100
    shuffleOptions = $false
    showQuestionStatus = $true
    showCorrectAnswersAfterCompletion = $false
    questions = @(
        @{ position = 1; questionType = "OPEN_ANSWER"; questionText = "Explain JVM lifecycle"; trainerHint = "Mention classloading/execution"; fullPoints = 5; partialPoints = 2 }
    )
}
$LESSON_PRACTICE_OPEN = [long]$practiceOpen.Json.id
$Q_OPEN_1 = [long]$practiceOpen.Json.questions[0].id

Invoke-Api -Method "PUT" -Path "/api/v1/admin/courses/$COURSE_BASE/lessons/$LESSON_THEORY_VIDEO/theory" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    title = "THEORY VIDEO UPDATED $RUN"
    lessonType = "THEORY_VIDEO"
    content = "https://youtu.be/aqz-KE-bpKQ"
    timeLimitMinutes = 20
} | Out-Null

Invoke-Api -Method "PUT" -Path "/api/v1/admin/courses/$COURSE_BASE/lessons/$LESSON_PRACTICE_TEST/practice" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    title = "PRACTICE TEST UPDATED $RUN"
    attemptLimit = 3
    timeLimitMinutes = 25
    lessonType = "PRACTICE_TEST"
    passingThresholdPercent = 70
    shuffleOptions = $false
    showQuestionStatus = $true
    showCorrectAnswers = $true
    questions = @(
        @{ id = $Q_TEST_1; position = 1; questionType = "SINGLE_CHOICE"; questionText = "2+2?"; options = @("3","4"); correctAnswers = @("4"); fullPoints = 2 },
        @{ id = $Q_TEST_2; position = 2; questionType = "MULTIPLE_CHOICE"; questionText = "choose vowels"; options = @("A","B","E"); correctAnswers = @("A","E"); fullPoints = 2; partialPoints = 1 },
        @{ id = $Q_TEST_3; position = 3; questionType = "ORDERING"; questionText = "order"; options = @("1","2","3"); correctAnswers = @("1","2","3"); fullPoints = 2; partialPoints = 1 }
    )
} | Out-Null

$orderMap = [ordered]@{}
$orderMap["$LESSON_PRACTICE_TEST"] = 1
$orderMap["$LESSON_THEORY_TEXT"] = 2
$orderMap["$LESSON_THEORY_VIDEO"] = 3
$orderMap["$LESSON_THEORY_PDF"] = 4
$orderMap["$LESSON_PRACTICE_OPEN"] = 5

Invoke-Api -Method "PUT" -Path "/api/v1/admin/courses/$COURSE_BASE" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    title = "cJavaBase-updated-$RUN"
    description = "updated base"
    authorFullName = "E2E Admin"
    coverFilePath = $UPLOADED_LINK
    deadlineDays = 45
    lessonsFreeOrder = $true
    sectionId = $SECTION_ID
    lessonIdToPosition = $orderMap
} | Out-Null

$tmpLesson = Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/lessons/theory" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    title = "TEMP DELETE $RUN"
    lessonType = "THEORY_TEXT"
    content = "temp"
} 
$TMP_LESSON_ID = [long]$tmpLesson.Json.id
Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/$COURSE_BASE/lessons/$TMP_LESSON_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null

$baseAfterDelete = Invoke-Api -Method "GET" -Path "/api/v1/admin/courses/$COURSE_BASE" -Token $ADMIN_TOKEN -ExpectedStatus @(200)
$positions = @($baseAfterDelete.Json.lessons | ForEach-Object { [int]$_.position } | Sort-Object)
for ($i = 0; $i -lt $positions.Count; $i++) {
    Assert-True ($positions[$i] -eq ($i + 1)) "Lesson positions must be contiguous"
}

Step "G. Programs"
$programCreate = Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    title = "pJavaTrack-$RUN"
    description = "java track"
    accessCondition = "PREVIOUS_COURSES_COMPLETED"
    blockAfterDeadline = $false
}
$PROGRAM_ID = [long]$programCreate.Json.id

Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/courses/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    orderedCourseIds = @($COURSE_BASE, $COURSE_PRACTICE, $COURSE_FINAL)
} | Out-Null

Invoke-Api -Method "GET" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "GET" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/courses/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null

Invoke-Api -Method "PUT" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    title = "pJavaTrack-updated-$RUN"
    description = "updated"
    accessCondition = "PREVIOUS_COURSES_COMPLETED"
    blockAfterDeadline = $false
} | Out-Null

Step "H + I. Assignments matrix + cross effects"
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/enrollments" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @($STUDENT_A_ID, $STUDENT_B_ID); idsNotIn = @() } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/enrollments" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @(); idsNotIn = @($STUDENT_B_ID) } | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/reviewers" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @($ADMIN_REVIEWER_ID); idsNotIn = @() } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/reviewers" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @($ADMIN_REVIEWER_ID); idsNotIn = @() } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/reviewers" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @(); idsNotIn = @($ADMIN_REVIEWER_ID) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/reviewers" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @($ADMIN_REVIEWER_ID); idsNotIn = @() } | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_PRACTICE/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($G_GENERAL_1) } | Out-Null
Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/$COURSE_PRACTICE/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($G_GENERAL_1) } | Out-Null
Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/$COURSE_PRACTICE/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($G_GENERAL_1) } | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @($STUDENT_A_ID); idsNotIn = @() } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @($STUDENT_A_ID); idsNotIn = @() } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @(); idsNotIn = @($STUDENT_A_ID) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @($STUDENT_A_ID); idsNotIn = @() } | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @($G_GENERAL_1); idsNotIn = @() } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @(); idsNotIn = @($G_GENERAL_1) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @($G_GENERAL_1); idsNotIn = @() } | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_GENERAL_2/courses/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($COURSE_FINAL) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_GENERAL_2/programs/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($PROGRAM_ID) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_GENERAL_2/members" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($STUDENT_C_ID) } | Out-Null

$finalEnrollments = Invoke-Api -Method "GET" -Path "/api/v1/admin/courses/$COURSE_FINAL/enrollments" -Token $ADMIN_TOKEN -ExpectedStatus @(200)
Assert-True (InList-ContainsUserId -listDto $finalEnrollments.Json -userId $STUDENT_C_ID) "studentC must be auto-enrolled to assigned final course"

$programUsers = Invoke-Api -Method "GET" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200)
Assert-True (InList-ContainsUserId -listDto $programUsers.Json -userId $STUDENT_C_ID) "studentC must be auto-enrolled to assigned program"

Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_GENERAL_1/members" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($STUDENT_A_ID) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_PRACTICE/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($G_GENERAL_1) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @($G_GENERAL_1); idsNotIn = @() } | Out-Null

$practiceEnrollments = Invoke-Api -Method "GET" -Path "/api/v1/admin/courses/$COURSE_PRACTICE/enrollments" -Token $ADMIN_TOKEN -ExpectedStatus @(200)
Assert-True (InList-ContainsUserId -listDto $practiceEnrollments.Json -userId $STUDENT_A_ID) "studentA must be enrolled to practice course"

Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_GENERAL_2/members" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($STUDENT_A_ID) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_GENERAL_1/courses/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($COURSE_FINAL) } | Out-Null
Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/$COURSE_FINAL/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($G_GENERAL_1) } | Out-Null
$finalAfterFirstUnassign = Invoke-Api -Method "GET" -Path "/api/v1/admin/courses/$COURSE_FINAL/enrollments" -Token $ADMIN_TOKEN -ExpectedStatus @(200)
Assert-True (InList-ContainsUserId -listDto $finalAfterFirstUnassign.Json -userId $STUDENT_A_ID) "studentA should remain enrolled after removing one of two groups"

Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/$COURSE_FINAL/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($G_GENERAL_2) } | Out-Null

Step "J. Student learning flow + review + program conditions"
$reviewerLogin = Login -Email $ADMIN_REVIEWER_EMAIL -Password $ADMIN_REVIEWER_PASS -ExpectedStatus @(200)
$REVIEWER_TOKEN = [string]$reviewerLogin.Json.token

Invoke-Api -Method "GET" -Path "/api/v1/student/my/courses" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "GET" -Path "/api/v1/student/my/programs" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "GET" -Path "/api/v1/student/my/programs/$PROGRAM_ID" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "GET" -Path "/api/v1/student/courses/$COURSE_BASE" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "GET" -Path "/api/v1/student/courses/$COURSE_BASE/lessons/next" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_THEORY_TEXT/start" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_THEORY_TEXT/complete-theory" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_THEORY_VIDEO/start" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_THEORY_VIDEO/complete-theory" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_THEORY_PDF/start" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_THEORY_PDF/complete-theory" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_PRACTICE_TEST/start" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
$testWrong = Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_PRACTICE_TEST/submit-practice" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) -Body @{
    questionAnswers = @{
        "$Q_TEST_1" = @("3")
        "$Q_TEST_2" = @("A")
        "$Q_TEST_3" = @("2", "1", "3")
    }
    submittedAt = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
}
Assert-True ($testWrong.Json.status -eq "REWORKING") "First wrong test attempt should be REWORKING"

$testCorrect = Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_PRACTICE_TEST/submit-practice" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) -Body @{
    questionAnswers = @{
        "$Q_TEST_1" = @("4")
        "$Q_TEST_2" = @("A", "E")
        "$Q_TEST_3" = @("1", "2", "3")
    }
    submittedAt = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
}
Assert-True ($testCorrect.Json.status -eq "COMPLETED") "Second test attempt should be COMPLETED"

$limitLesson = Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_PRACTICE/lessons/practice" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    title = "LIMIT LESSON $RUN"
    lessonType = "PRACTICE_TEST"
    attemptLimit = 2
    timeLimitMinutes = 20
    passingThresholdPercent = 100
    showQuestionStatus = $true
    showCorrectAnswersAfterCompletion = $false
    questions = @(
        @{ position = 1; questionType = "SINGLE_CHOICE"; questionText = "1+1?"; options = @("2", "3"); correctAnswers = @("2"); fullPoints = 1 }
    )
}
$LESSON_LIMIT = [long]$limitLesson.Json.id
$Q_LIMIT = [long]$limitLesson.Json.questions[0].id

Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_PRACTICE/enrollments" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @($STUDENT_A_ID); idsNotIn = @() } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_LIMIT/start" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_LIMIT/submit-practice" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) -Body @{ questionAnswers = @{ "$Q_LIMIT" = @("3") }; submittedAt = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_LIMIT/submit-practice" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) -Body @{ questionAnswers = @{ "$Q_LIMIT" = @("3") }; submittedAt = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_LIMIT/submit-practice" -Token $STUDENT_A_TOKEN -ExpectedStatus @(400) -Body @{ questionAnswers = @{ "$Q_LIMIT" = @("3") }; submittedAt = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() } | Out-Null

# scheduler timeout validation: after 1 minute timeout, submission must be rejected
$timeoutLesson = Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/lessons/practice" -Token $ADMIN_TOKEN -ExpectedStatus @(201) -Body @{
    title = "TIMEOUT LESSON $RUN"
    description = "scheduler timeout check"
    stopLesson = $false
    attemptLimit = 1
    timeLimitMinutes = 1
    lessonType = "PRACTICE_TEST"
    passingThresholdPercent = 100
    shuffleOptions = $false
    showQuestionStatus = $true
    showCorrectAnswersAfterCompletion = $false
    questions = @(
        @{ position = 1; questionType = "SINGLE_CHOICE"; questionText = "timeout check"; options = @("A", "B"); correctAnswers = @("A"); fullPoints = 1 }
    )
}
$LESSON_TIMEOUT = [long]$timeoutLesson.Json.id
$Q_TIMEOUT = [long]$timeoutLesson.Json.questions[0].id

Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_TIMEOUT/start" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Start-Sleep -Seconds 130

$timeoutLessonState = Invoke-Api -Method "GET" -Path "/api/v1/student/lessons/$LESSON_TIMEOUT" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200)
Assert-True ($timeoutLessonState.Json.status -eq "INCOMPLETED") "Scheduler must set timeout lesson status to INCOMPLETED"
Assert-True ([int]$timeoutLessonState.Json.attempts -eq 1) "Scheduler must increment attempts for timeout lesson"

Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_TIMEOUT/submit-practice" -Token $STUDENT_A_TOKEN -ExpectedStatus @(400) -Body @{
    questionAnswers = @{ "$Q_TIMEOUT" = @("A") }
    submittedAt = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
} | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_PRACTICE_OPEN/start" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_PRACTICE_OPEN/submit-practice" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) -Body @{
    questionAnswers = @{ "$Q_OPEN_1" = @("open answer v1") }
    submittedAt = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
} | Out-Null

$pending1 = Invoke-Api -Method "GET" -Path "/api/v1/admin/progress/reviews/pending" -Token $REVIEWER_TOKEN -ExpectedStatus @(200)
$submission1 = @($pending1.Json | Where-Object { [long]$_.lessonId -eq $LESSON_PRACTICE_OPEN -and [long]$_.studentId -eq $STUDENT_A_ID } | Select-Object -First 1)
Assert-True ($submission1.Count -eq 1) "Open submission must be present in pending"
$SUBMISSION_ID_1 = [long]$submission1[0].submissionId

Invoke-Api -Method "GET" -Path "/api/v1/admin/progress/reviews/pending/$SUBMISSION_ID_1" -Token $REVIEWER_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/progress/reviews/$SUBMISSION_ID_1" -Token $REVIEWER_TOKEN -ExpectedStatus @(200) -Body @{
    questionReviews = @{ "$Q_OPEN_1" = @{ submissionStatus = "REWORK"; reviewComment = "Please improve" } }
} | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_PRACTICE_OPEN/start" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_PRACTICE_OPEN/submit-practice" -Token $STUDENT_A_TOKEN -ExpectedStatus @(200) -Body @{
    questionAnswers = @{ "$Q_OPEN_1" = @("open answer v2") }
    submittedAt = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
} | Out-Null

$pending2 = Invoke-Api -Method "GET" -Path "/api/v1/admin/progress/reviews/pending" -Token $REVIEWER_TOKEN -ExpectedStatus @(200)
$submission2 = @($pending2.Json | Where-Object { [long]$_.lessonId -eq $LESSON_PRACTICE_OPEN -and [long]$_.studentId -eq $STUDENT_A_ID } | Select-Object -First 1)
Assert-True ($submission2.Count -eq 1) "Resubmitted open submission must be pending"
$SUBMISSION_ID_2 = [long]$submission2[0].submissionId

Invoke-Api -Method "POST" -Path "/api/v1/admin/progress/reviews/$SUBMISSION_ID_2" -Token $REVIEWER_TOKEN -ExpectedStatus @(200) -Body @{
    questionReviews = @{ "$Q_OPEN_1" = @{ submissionStatus = "ACCEPTED"; pointsType = "FULL"; reviewComment = "Accepted" } }
} | Out-Null

$pendingAfterFinal = Invoke-Api -Method "GET" -Path "/api/v1/admin/progress/reviews/pending" -Token $REVIEWER_TOKEN -ExpectedStatus @(200)
$stillPendingIds = @($pendingAfterFinal.Json | ForEach-Object { [long]$_.submissionId })
Assert-True (-not ($stillPendingIds -contains $SUBMISSION_ID_2)) "Accepted submission must leave pending list"

$studentBLogin = Login -Email $STUDENT_B_EMAIL -Password $STUDENT_B_PASS -ExpectedStatus @(200)
$STUDENT_B_TOKEN = [string]$studentBLogin.Json.token

Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @($STUDENT_B_ID); idsNotIn = @() } | Out-Null

Invoke-Api -Method "PUT" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    title = "pJavaTrack-updated-$RUN"
    description = "condition check"
    accessCondition = "PREVIOUS_COURSES_COMPLETED"
    blockAfterDeadline = $false
} | Out-Null

$bProgramCompletedRule = Invoke-Api -Method "GET" -Path "/api/v1/student/my/programs/$PROGRAM_ID" -Token $STUDENT_B_TOKEN -ExpectedStatus @(200)
Assert-True (-not [bool]$bProgramCompletedRule.Json.courses[1].available) "Course #2 must be unavailable for PREVIOUS_COURSES_COMPLETED before completion"

Invoke-Api -Method "POST" -Path "/api/v1/student/lessons/$LESSON_THEORY_TEXT/start" -Token $STUDENT_B_TOKEN -ExpectedStatus @(200) | Out-Null

Invoke-Api -Method "PUT" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    title = "pJavaTrack-updated-$RUN"
    description = "condition check viewed"
    accessCondition = "PREVIOUS_COURSES_VIEWED_OR_PENDING"
    blockAfterDeadline = $false
} | Out-Null

$bProgramViewedRule = Invoke-Api -Method "GET" -Path "/api/v1/student/my/programs/$PROGRAM_ID" -Token $STUDENT_B_TOKEN -ExpectedStatus @(200)
Assert-True ([bool]$bProgramViewedRule.Json.courses[1].available) "Course #2 must be available after course #1 viewed"

Invoke-Api -Method "PUT" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    title = "pJavaTrack-updated-$RUN"
    description = "condition check all open"
    accessCondition = "ALL_OPEN"
    blockAfterDeadline = $false
} | Out-Null

$bProgramAllOpen = Invoke-Api -Method "GET" -Path "/api/v1/student/my/programs/$PROGRAM_ID" -Token $STUDENT_B_TOKEN -ExpectedStatus @(200)
$allAvailable = @($bProgramAllOpen.Json.courses | ForEach-Object { [bool]$_.available })
Assert-True ($allAvailable -notcontains $false) "All program courses must be available for ALL_OPEN"

$pastDeadline = [DateTimeOffset]::UtcNow.AddHours(-2).ToUnixTimeSeconds()
Invoke-Api -Method "PUT" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    title = "pJavaTrack-updated-$RUN"
    description = "deadline check"
    accessCondition = "ALL_OPEN"
    deadlineAt = $pastDeadline
    blockAfterDeadline = $true
} | Out-Null

$bProgramBlocked = Invoke-Api -Method "GET" -Path "/api/v1/student/my/programs/$PROGRAM_ID" -Token $STUDENT_B_TOKEN -ExpectedStatus @(200)
$anyBlocked = @($bProgramBlocked.Json.courses | ForEach-Object { [bool]$_.available }) -contains $false
Assert-True $anyBlocked "With past deadline + blockAfterDeadline=true at least one unfinished course should be unavailable"

Invoke-Api -Method "PUT" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{
    title = "pJavaTrack-updated-$RUN"
    description = "restored"
    accessCondition = "PREVIOUS_COURSES_COMPLETED"
    blockAfterDeadline = $false
    deadlineAt = $null
} | Out-Null

Step "K + L. Role model + negative contracts"
Invoke-Api -Method "POST" -Path "/api/v1/admin/groups" -Token $STUDENT_A_TOKEN -ExpectedStatus @(403) -Body @{ title = "forbidden-$RUN"; type = "GENERAL" } | Out-Null
Invoke-Api -Method "GET" -Path "/api/v1/student/my/courses" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "GET" -Path "/api/v1/student/my/courses" -ExpectedStatus @(401) | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_GENERAL_1/members" -Token $ADMIN_TOKEN -ExpectedStatus @(400) -Body @{ ids = @() } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(400) -Body @{ idsIn = @($STUDENT_A_ID); idsNotIn = @($STUDENT_A_ID) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/groups/$G_GENERAL_1/courses/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(400) -Body @{ idsIn = @($COURSE_BASE); idsNotIn = @() } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses" -Token $ADMIN_TOKEN -ExpectedStatus @(404) -Body @{ title = "bad-foreign-$RUN"; sectionId = 99999999; deadlineDays = 10 } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/lessons/practice" -Token $ADMIN_TOKEN -ExpectedStatus @(400) -Body @{ title = "bad-lesson-$RUN"; lessonType = "PRACTICE_TEST"; questions = @() } | Out-Null

Step "M. Cleanup"
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/$COURSE_BASE/reviewers" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @(); idsNotIn = @($ADMIN_REVIEWER_ID) } | Out-Null
Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/$COURSE_BASE/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($G_GENERAL_1, $G_GENERAL_2) } | Out-Null
Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/$COURSE_PRACTICE/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($G_GENERAL_1) } | Out-Null
Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/$COURSE_FINAL/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ ids = @($G_GENERAL_1, $G_GENERAL_2) } | Out-Null

Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/groups/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @(); idsNotIn = @($G_GENERAL_1, $G_GENERAL_2) } | Out-Null
Invoke-Api -Method "POST" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID/assign" -Token $ADMIN_TOKEN -ExpectedStatus @(200) -Body @{ idsIn = @(); idsNotIn = @($STUDENT_A_ID, $STUDENT_B_ID, $STUDENT_C_ID) } | Out-Null

function Remove-AllLessons([long]$courseId) {
    $details = Invoke-Api -Method "GET" -Path "/api/v1/admin/courses/$courseId" -Token $ADMIN_TOKEN -ExpectedStatus @(200)
    foreach ($lesson in @($details.Json.lessons)) {
        Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/$courseId/lessons/$($lesson.id)" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null
    }
}

Remove-AllLessons -courseId $COURSE_BASE
Remove-AllLessons -courseId $COURSE_PRACTICE
Remove-AllLessons -courseId $COURSE_FINAL

Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/$COURSE_BASE" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/$COURSE_PRACTICE" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null
Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/$COURSE_FINAL" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null

Invoke-Api -Method "DELETE" -Path "/api/v1/admin/courses/programs/$PROGRAM_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null

foreach ($gid in @($G_GENERAL_1, $G_GENERAL_2, $G_COMPANY_1, $G_COMPANY_2, $G_DEPARTMENT_1, $G_POSITION_1, $G_POSITION_2)) {
    Invoke-Api -Method "DELETE" -Path "/api/v1/admin/groups/$gid" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null
}

Invoke-Api -Method "DELETE" -Path "/api/v1/admin/sections/$SECTION_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null

foreach ($uid in @($ADMIN_REVIEWER_ID, $STUDENT_A_ID, $STUDENT_B_ID, $STUDENT_C_ID)) {
    Invoke-Api -Method "DELETE" -Path "/api/v1/admin/users/$uid" -Token $ADMIN_TOKEN -ExpectedStatus @(200) | Out-Null
}

Invoke-Api -Method "DELETE" -Path "/api/v1/admin/users/$STUDENT_C_ID" -Token $ADMIN_TOKEN -ExpectedStatus @(404) | Out-Null

Remove-Item -Path $uploadTmp -ErrorAction SilentlyContinue

Write-Host "`nFULL BACKEND E2E CHAIN PASSED ($RUN)" -ForegroundColor Green