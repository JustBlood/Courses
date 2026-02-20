import json
from pathlib import Path


path = Path("memory-bank/tasks.json")
data = json.loads(path.read_text(encoding="utf-8"))


def get_task(tasks, tid):
    for t in tasks:
        if t["id"] == tid:
            return t
    return None


remove_ids = {"TASK-019", "TASK-020", "TASK-022"}
tasks = [t for t in data["tasks"] if t["id"] not in remove_ids]

for tid, status in [
    ("TASK-009", "done"),
    ("TASK-010", "done"),
    ("TASK-021", "done"),
    ("TASK-025", "obsolete"),
]:
    task = get_task(tasks, tid)
    if task is None:
        raise RuntimeError(f"Missing task: {tid}")
    task["status"] = status

t11 = get_task(tasks, "TASK-011")
t11["description"] = (
    "Реализовать FR-008/FR-009 (data/model): PRACTICE-уроки, все типы вопросов "
    "и конфигурируемую балльную модель (без полного runtime scoring-движка)."
)
t11["acceptance_criteria"] = [
    "Поддерживаются типы вопросов: single, multiple, matching, ordering, open-ended.",
    "Есть дефолтные баллы и переопределение на уровне урока/вопроса в модели и API-контрактах.",
    "Конфигурация урока/вопросов корректно сохраняется и возвращается через API для последующей runtime-проверки.",
]

t15 = get_task(tasks, "TASK-015")
t15["description"] = (
    "Реализовать FR-013 (runtime scoring engine): автопроверка тестовых заданий "
    "с полным question-pool, корректной агрегацией баллов и partial scoring rules."
)
t15["acceptance_criteria"] = [
    "После отправки тестовой попытки система автоматически рассчитывает результат по всем вопросам попытки, а не по упрощённой модели.",
    "Статус попытки и урока обновляется без ручной проверки на основе порога прохождения и фактического scoring.",
    "Баллы рассчитываются с учётом дефолтных/индивидуальных настроек и частичной оценки multiple-choice по правилам PRD.",
]

new_tasks = [
    {
        "id": "TASK-019A",
        "category": "integration",
        "priority": "high",
        "description": "Реализовать FR-017 (MVP): сводный отчёт по конкретному курсу по AC-017 со строгим набором колонок и фиксированными пустыми полями.",
        "acceptance_criteria": [
            "Отчёт по курсу формируется отдельным действием из каталога/карточки курса.",
            "Список колонок и порядок соответствуют PRD/AC-017, включая поля, которые по ТЗ должны оставаться пустыми.",
            "Данные в отчёте соответствуют фактическому прогрессу студентов и согласованы с course statistics.",
        ],
        "test_steps": [
            "Шаг 1: Сформировать отчёт по одному курсу с несколькими студентами.",
            "Шаг 2: Проверить структуру и порядок колонок относительно PRD-02/PRD-06 (AC-017).",
            "Шаг 3: Сверить ключевые поля (баллы, эффективность, прогресс) с course statistics.",
        ],
        "dependencies": ["TASK-018"],
        "status": "pending",
    },
    {
        "id": "TASK-019B",
        "category": "integration",
        "priority": "medium",
        "description": "Реализовать FR-017 (post-MVP enrichment): расширение course report полями, зависящими от групп/программ и их назначений.",
        "acceptance_criteria": [
            "Course report расширен данными групп/программ без нарушения контракта TASK-019A.",
            "Поля, зависящие от групп/программ, заполняются консистентно после реализации соответствующих модулей.",
            "Расширенный отчёт сохраняет обратную совместимость по структуре и формату MVP-версии.",
        ],
        "test_steps": [
            "Шаг 1: Подготовить данные с группами и программами, назначенными на пользователей курса.",
            "Шаг 2: Сформировать отчёт и проверить заполнение enriched-полей.",
            "Шаг 3: Сверить, что базовые колонки/формат из TASK-019A не деградировали.",
        ],
        "dependencies": ["TASK-027", "TASK-030", "TASK-032", "TASK-019A"],
        "status": "pending",
    },
    {
        "id": "TASK-020A",
        "category": "integration",
        "priority": "high",
        "description": "Реализовать FR-017 (MVP): общий сводный отчёт по всем курсам с минимально полным контрактом AC-017.",
        "acceptance_criteria": [
            "Общий отчёт формируется с требуемыми колонками и форматами дат/времени.",
            "Поля, отмеченные как пустые по ТЗ, заполняются пустыми значениями стабильно.",
            "Отчёт корректно обрабатывает пользователей, назначенных на несколько курсов.",
        ],
        "test_steps": [
            "Шаг 1: Сформировать общий отчёт в системе с минимум двумя курсами.",
            "Шаг 2: Проверить колонки и порядок в сравнении с PRD/AC-017.",
            "Шаг 3: Убедиться, что у одного студента есть отдельная строка для каждого курса с корректными значениями.",
        ],
        "dependencies": ["TASK-019A"],
        "status": "pending",
    },
    {
        "id": "TASK-020B",
        "category": "integration",
        "priority": "medium",
        "description": "Реализовать FR-017 (post-MVP enrichment): расширение общего отчёта полями программ/групп и дополнительными форматами.",
        "acceptance_criteria": [
            "Общий отчёт расширен данными программ/групп после реализации post-MVP модулей.",
            "Расширение не нарушает MVP-контракт и формат TASK-020A.",
            "Данные корректны для пользователей, участвующих в нескольких группах/программах.",
        ],
        "test_steps": [
            "Шаг 1: Подготовить набор пользователей с несколькими группами/программами и курсами.",
            "Шаг 2: Сформировать расширенный общий отчёт и проверить enriched-поля.",
            "Шаг 3: Проверить совместимость с MVP-форматом общего отчёта.",
        ],
        "dependencies": ["TASK-019B", "TASK-020A"],
        "status": "pending",
    },
    {
        "id": "TASK-022A",
        "category": "integration",
        "priority": "high",
        "description": "Реализовать FR-019 (base flow): восстановление пароля по email-токену (initiate + confirm).",
        "acceptance_criteria": [
            "Пользователь может инициировать reset по email.",
            "Система отправляет одноразовую ссылку/токен для установки нового пароля.",
            "После подтверждения новым паролем доступ успешно восстанавливается.",
        ],
        "test_steps": [
            "Шаг 1: Инициировать восстановление пароля для существующего пользователя.",
            "Шаг 2: Использовать полученный токен для установки нового пароля.",
            "Шаг 3: Войти с новым паролем и убедиться в успешной аутентификации.",
        ],
        "dependencies": ["TASK-007", "TASK-003"],
        "status": "pending",
    },
    {
        "id": "TASK-022B",
        "category": "security",
        "priority": "high",
        "description": "Усилить security reset-flow (FR-019/NFR-SEC): TTL токена, anti-enumeration, neutral responses, anti-abuse controls.",
        "acceptance_criteria": [
            "Reset-токен имеет контролируемый срок жизни и отклоняется после истечения TTL.",
            "Recover/reset endpoint не раскрывает существование пользователя (neutral response, anti-enumeration).",
            "Механики anti-abuse/rate-limit для reset-flow согласованы с TASK-005 и проверены тестами.",
        ],
        "test_steps": [
            "Шаг 1: Проверить, что просроченный reset-токен отклоняется и не меняет пароль.",
            "Шаг 2: Проверить нейтральные ответы для существующего и несуществующего email.",
            "Шаг 3: Выполнить серию reset-запросов и убедиться в применении anti-abuse ограничений.",
        ],
        "dependencies": ["TASK-022A", "TASK-005", "TASK-003"],
        "status": "pending",
    },
    {
        "id": "TASK-041",
        "category": "functional",
        "priority": "high",
        "description": "Реализовать явное activation-state пользователя до установки пароля (FR-002/AC-002).",
        "acceptance_criteria": [
            "До установки пароля пользователь имеет явный неактивированный статус.",
            "После успешной установки пароля статус переводится в активный.",
            "Статус активации отражается консистентно в API/доменной модели и не ломает текущий onboarding-flow.",
        ],
        "test_steps": [
            "Шаг 1: Создать пользователя и проверить, что до set-password он в неактивированном состоянии.",
            "Шаг 2: Выполнить set-password по валидному токену.",
            "Шаг 3: Проверить, что статус активации изменился на активный и пользователь может пройти login.",
        ],
        "dependencies": ["TASK-007"],
        "status": "pending",
    },
    {
        "id": "TASK-042",
        "category": "functional",
        "priority": "high",
        "description": "Реализовать self-profile update endpoint с whitelist разрешённых полей (FR-003/AC-003).",
        "acceptance_criteria": [
            "Пользователь может обновлять только разрешённые поля собственного профиля.",
            "Попытка изменить запрещённые поля отклоняется контролируемой ошибкой.",
            "Администраторские сценарии редактирования не деградируют.",
        ],
        "test_steps": [
            "Шаг 1: Под пользователем вызвать self-profile update с валидными разрешёнными полями.",
            "Шаг 2: Попробовать изменить запрещённые поля и проверить отказ.",
            "Шаг 3: Проверить, что администраторский update-flow продолжает работать корректно.",
        ],
        "dependencies": ["TASK-008"],
        "status": "pending",
    },
    {
        "id": "TASK-043",
        "category": "functional",
        "priority": "high",
        "description": "Реализовать API-модель двух списков enrolled/not-enrolled для курса (FR-010/AC-010).",
        "acceptance_criteria": [
            "Для курса доступен endpoint/контракт с двумя явными списками: enrolled и notEnrolled.",
            "Перемещение пользователей между списками отражается в API без рассинхронизации.",
            "Операции назначения/отчисления сохраняют атомарность и отсутствие дублей связей.",
        ],
        "test_steps": [
            "Шаг 1: Запросить списки enrolled/not-enrolled по курсу.",
            "Шаг 2: Переместить пользователя между списками.",
            "Шаг 3: Повторно запросить списки и проверить корректное состояние.",
        ],
        "dependencies": ["TASK-012"],
        "status": "pending",
    },
    {
        "id": "TASK-044",
        "category": "functional",
        "priority": "high",
        "description": "Реализовать явные статусы workflow pending -> rework -> accepted для проверки open-ended ответов (FR-014/AC-014).",
        "acceptance_criteria": [
            "Open-ended submission проходит через явные статусы pending, rework и accepted.",
            "Переходы статусов валидируются и не допускают некорректные переходы.",
            "При accepted корректно пересчитываются урок/баллы и фиксируется история статусов.",
        ],
        "test_steps": [
            "Шаг 1: Студент отправляет open-ended ответ и получает статус pending.",
            "Шаг 2: Reviewer переводит submission в rework, затем в accepted.",
            "Шаг 3: Проверить историю переходов и итоговый результат в статистике.",
        ],
        "dependencies": ["TASK-016"],
        "status": "pending",
    },
    {
        "id": "TASK-045",
        "category": "infrastructure",
        "priority": "high",
        "description": "Реализовать централизованный audit-trail ключевых бизнес-действий (NFR-AUD-01).",
        "acceptance_criteria": [
            "Фиксируются audit-события для критичных операций (создание пользователя, назначения, проверки ответов, password flows).",
            "Audit-события имеют консистентный формат и корреляцию с запросом/пользователем.",
            "Audit-логирование не раскрывает чувствительные данные и не ломает SLA основных операций.",
        ],
        "test_steps": [
            "Шаг 1: Выполнить набор ключевых операций (create user/enroll/review/password).",
            "Шаг 2: Проверить наличие и формат audit-событий.",
            "Шаг 3: Убедиться, что чувствительные поля замаскированы, а производительность не деградирует критично.",
        ],
        "dependencies": ["TASK-037"],
        "status": "pending",
    },
    {
        "id": "TASK-046",
        "category": "infrastructure",
        "priority": "medium",
        "description": "Подтвердить backup/restore readiness с измеримыми RPO/RTO и эксплуатационным регламентом (NFR-BCK-01..03).",
        "acceptance_criteria": [
            "Документирован и автоматизирован регламент backup/restore для БД и файловых материалов.",
            "Проведён проверяемый restore-drill с фиксацией фактических RPO/RTO.",
            "Определён минимальный retention и проверка его соблюдения в эксплуатационном контуре.",
        ],
        "test_steps": [
            "Шаг 1: Выполнить backup согласно регламенту.",
            "Шаг 2: Выполнить restore-drill на тестовом контуре.",
            "Шаг 3: Зафиксировать фактические RPO/RTO и сверить с целевыми ограничениями.",
        ],
        "dependencies": ["TASK-039"],
        "status": "pending",
    },
]


def sort_key(task):
    tid = task["id"]
    prefix, num = tid.split("-")
    digits = "".join(ch for ch in num if ch.isdigit())
    suffix = num[len(digits):]
    return prefix, int(digits), suffix


all_tasks = tasks + new_tasks
all_tasks.sort(key=sort_key)
data["tasks"] = all_tasks

t40 = get_task(data["tasks"], "TASK-040")
t40["dependencies"] = ["TASK-020A", "TASK-022B", "TASK-028", "TASK-036"]

ids = {t["id"] for t in data["tasks"]}
for t in data["tasks"]:
    for dep in t.get("dependencies", []):
        if dep not in ids:
            raise RuntimeError(f"Dependency {dep} not found for {t['id']}")

path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

print(f"Updated tasks count: {len(data['tasks'])}")