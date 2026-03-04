# PRE-006 — Проверка слойности controller → service → repository и DTO/mapper discipline

## 1) Контекст задачи
- Источник: `memory-bank/pre-tasks.json` → `PRE-006`
- Цель: проверить ключевые цепочки `auth/user/course/learning/report` на:
  1. соблюдение слойности,
  2. отсутствие утечки entity наружу,
  3. наличие бизнес-логики в controller,
  4. архитектурные отклонения для последующей нормализации.

Проверенные файлы:
- Controllers: `AuthController`, `UsersController`, `CoursesController`, `StudentController`, `ProgressController`
- Services: `AuthService`, `UserService`, `CourseService`, `LearningService`, `StatisticsService`
- Mappers: `UserMapper`, `CourseMapper`, `LessonMapper`

---

## 2) Результат по acceptance criteria PRE-006

### AC-1: Для ключевых модулей зафиксирован проход по слоям без утечек entity наружу
**Статус: PARTIAL (в целом соблюдается, есть точечные архитектурные отклонения)**

- Во всех проверенных controller нет прямой работы с repository.
- Внешние API-контракты во всех проверенных controller отдают DTO (`UserDto`, `CourseDto`, `LessonDto`, `SubmissionResultDto`, и т.д.) либо `ApiResponse`/`String`.
- Прямой утечки JPA-entity через REST-контракты в проверенных модулях не обнаружено.

### AC-2: Найдены места нарушения слойности/бизнес-логики в controller
**Статус: MET**

Найдены контроллерные участки с orchestration/business-поведением, которое лучше держать в service/facade:
- `CoursesController`: batch-операции `assign/unassign` и `assign/unassign reviewers` выполняются через `forEach` в controller (поэлементный orchestration).
- `StudentController.myProfile`: композиция профиля из нескольких сервисов формируется в controller.

### AC-3: Составлен список задач на архитектурную нормализацию
**Статус: MET**

См. раздел 5 (приоритизированный backlog архитектурной нормализации).

---

## 3) Матрица слойности по ключевым модулям

| Модуль | Цепочка controller→service→repository | DTO/mapper discipline | Статус |
|---|---|---|---|
| Auth | Соблюдена (`AuthController -> AuthService -> repositories`) | DTO соблюдены (`LoginResponse`, `ApiResponse`) | OK |
| User | Соблюдена (`UsersController -> UserService -> repositories`) | DTO + `UserMapper` | OK |
| Course | Базово соблюдена, но есть batch-orchestration в controller | DTO + `CourseMapper`/`LessonMapper` | PARTIAL |
| Learning | Соблюдена (`Student/ProgressController -> LearningService`) | DTO вручную + через сервисы | OK |
| Report/Stats | Соблюдена (`Progress/StudentController -> StatisticsService`) | DTO/CSV contract | OK |

---

## 4) Выявленные архитектурные отклонения

### 4.1 High
1. **Mapper зависит от Service (циклическая связка через `@Lazy`)**
   - Файл: `mapper/LessonMapper.java`
   - Факт: `LessonMapper` использует `CourseService.splitRaw(...)`.
   - Риск: размывание ответственности mapper-слоя, усиление связности, хрупкость DI-графа.

### 4.2 Medium
2. **Controller-уровень содержит batch-orchestration**
   - Файл: `controller/CoursesController.java`
   - Факт: `request.ids().forEach(...)` для assign/unassign-операций.
   - Риск: дублирование orchestration, сложнее централизовать транзакционность/валидацию/аудит.

3. **Неиспользуемые сервисные зависимости в controller**
   - `UsersController`: инжектированы `courseService`, `learningService`, `groupService`, `statisticsService`, `programService`, но фактически не используются.
   - `StudentController`: инжектирован `programService`, но сейчас не используется.
   - `ProgressController`: инжектирован `courseService`, но не используется.
   - Риск: техдолг, шум в API-слое, ложное ощущение ответственности контроллера.

### 4.3 Low (наблюдения в рамках PRE-006)
4. **Сигнатура `applyCommonLessonFields(...)` шире фактического применения полей**
   - Файл: `service/CourseService.java`
   - Факт: часть аргументов не используется внутри метода.
   - Риск: неоднозначность доменной логики и ложные ожидания по поведению.

---

## 5) Рекомендуемые задачи архитектурной нормализации (приоритет)

1. **ARCH-NORM-001 (High)**
   - Вынести `splitRaw` из `CourseService` в утилиту/компонент mapper-support,
   - разорвать зависимость `LessonMapper -> CourseService`.

2. **ARCH-NORM-002 (Medium)**
   - Перенести batch-orchestration assign/unassign из `CoursesController` в `CourseService` как атомарные batch-методы.

3. **ARCH-NORM-003 (Medium)**
   - Удалить неиспользуемые сервисные зависимости из `UsersController`, `StudentController`, `ProgressController`.

4. **ARCH-NORM-004 (Low)**
   - Вычистить неиспользуемые параметры/методы в `CourseService` и синхронизировать сигнатуры с реальным поведением.

---

## 6) Итог PRE-006

Слойность в ключевых контурах **в основном соблюдается**: контроллеры работают через сервисы, репозитории в API-слой не протекают, entity наружу не отдаются. При этом зафиксированы архитектурные отклонения (сильнейшее — зависимость mapper от service), которые не блокируют текущую работу, но формируют техдолг и должны быть учтены в backlog нормализации.
