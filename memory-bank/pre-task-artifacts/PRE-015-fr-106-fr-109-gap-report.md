# PRE-015 — Сверка FR-106..FR-109 (groups/mass assignments/search/filter)

## 1) Контекст
- Задача: `PRE-015` из `memory-bank/pre-tasks.json`.
- Область проверки: группы пользователей, membership-ограничения, массовые назначения через группы, поиск/фильтрация.
- Источники требований:
  - `memory-bank/prd/02-functional-requirements.md` (`FR-106..FR-109`)
  - `memory-bank/prd/06-acceptance-criteria.md` (`AC-106..AC-109`)
  - `memory-bank/prd/01-user-scenarios.md` (`US-08`, `US-09`)

Проверенные файлы реализации:
- `monolith-mvp/src/main/java/ru/just/monolithmvp/model/{LearningGroup,GroupMembership,GroupType}.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/{LearningGroupRepository,GroupMembershipRepository}.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/{GroupService,CourseService,ProgramService,UserService}.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/{UsersController,CoursesController,StudentController}.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/group/{CreateGroupRequest,GroupUsersRequest}.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/user/{CreateUserRequest,UpdateUserRequest}.java`
- `monolith-mvp/src/main/resources/db/migration/V1__init_schema.sql`

## 2) Матрица соответствия FR/AC

| ID | Статус | Наблюдение |
|---|---|---|
| FR-106 | partial | Домен и сервис для групп реализованы частично: создание (`GroupService#createGroup`), чтение и удаление есть; **редактирование группы отсутствует**. Все admin endpoint-ы групп в `UsersController` закомментированы, поэтому API-контур недоступен. |
| AC-106 | partial | Типы групп (`GENERAL/COMPANY/DEPARTMENT/POSITION`) и создание на уровне service поддержаны; критерий «создаёт/редактирует и использует для назначений» не закрыт end-to-end из-за отсутствия активных endpoint и update-операции. |
| FR-107 | partial | Изменение membership реализовано в `GroupService#addUsersToGroup/removeUsersFromGroup` с ограничением «не более одной группы typed-категории» (замена membership для `COMPANY/DEPARTMENT/POSITION`). Но в `CreateUserRequest/UpdateUserRequest` нет полей групп, а endpoint-ы membership в controller закомментированы. |
| AC-107 | partial | Сохранение membership работает на уровне service/repository; отображение групп в профиле есть (`StudentProfileDto` + `groupService.getUserGroups`). Полноценный пользовательский сценарий управления из API не закрыт. |
| FR-108 | partial | Есть методы массовых назначений: `CourseService#assignGroupToCourse`, `ProgramService#assignGroupToProgram`. Однако endpoint-ы массовых назначений закомментированы в `CoursesController`; нет persisted-связи «group -> target», поэтому при изменении состава группы доступы **не перерассчитываются автоматически**. |
| AC-108 | partial | Распространение на текущих участников выполняется при явном вызове service-методов, но критерий «все участники получают доступ как продуктовый сценарий» не закрыт через API и не обеспечен на последующие изменения membership. |
| FR-109 | partial | Частичный search/filter реализован только в `GroupService` (по title для admin/student), но соответствующие endpoint-ы закомментированы. Для каталогов курсов и пользователей серверных фильтров/поиска нет (`getAllCourses`, `getUsers` возвращают полные списки). |
| AC-109 | partial | Базовые role-ограничения на controllers присутствуют (`@PreAuthorize`), но полноценный критерий по трём каталогам (курсы/пользователи/группы) с поиском/фильтрацией не выполняется. |

## 3) Ключевые GAP и риски

### High
1. **Неэкспонированный групповой функционал**: endpoint-ы создания/управления группами и membership в `UsersController` закомментированы.
2. **Массовые назначения через группы не завершены архитектурно**: нет таблиц/моделей для хранения назначения `group -> course/program`; отсутствие авто-применения доступов при последующих изменениях состава группы.

### Medium
1. **FR-106 неполный CRUD**: отсутствует update группы.
2. **FR-109 покрыт только частично и только на сервисном уровне для групп**: нет server-side поиска/фильтров по пользователям и курсам, нет активных endpoint-ов поиска групп.

## 4) Итог по acceptance_criteria PRE-015

- ✅ Проверены типы групп и ограничения membership (typed-группы ограничены одной на пользователя, general — без лимита).
- ✅ Проверена механика массовых назначений через группы (course/program) и выявлены продуктовые/архитектурные пробелы.
- ✅ Проверены поиск/фильтрация и role-ограничения на уровне controller/service; зафиксирована частичная реализация.

Сводка статусов PRE-015:
- `FR implemented=0, partial=4, missing=0`
- `AC implemented=0, partial=4, missing=0`

## 5) Проверка test_steps PRE-015

- Шаг 1: Проверить GroupService и связанные endpoint — ✅
- Шаг 2: Сопоставить с AC-106..AC-109 — ✅
- Шаг 3: Зафиксировать пробелы и их влияние на backlog — ✅