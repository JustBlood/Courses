# TASK-026 (done) — FR-101/FR-102: CRUD разделов каталога и создание курса в контексте раздела

## Что сделано
- Реализован backend-модуль разделов каталога:
  - `Section` entity;
  - `SectionRepository`;
  - DTO: `CreateSectionRequest`, `UpdateSectionRequest`, `SectionDto`;
  - `SectionService` с безопасной отвязкой курсов при удалении раздела;
  - `SectionsController` (`/api/v1/admin/sections`) с CRUD.
- Реализована связь `Course -> Section`:
  - `Course` расширен `@ManyToOne Section section`;
  - `CreateCourseRequest` поддерживает `sectionId`;
  - `CourseDto` и `CourseSummaryDto` расширены section-полями;
  - `CourseMapper` дополнен mapping-ами section-полей.
- Реализовано создание курса в контексте раздела:
  - `POST /api/v1/admin/sections/{sectionId}/courses`.
- Реализована сортировка каталога курсов с учетом разделов:
  - по `section.priority`, затем `section.id`, затем `course.id`;
  - курсы без раздела сортируются после курсов с разделом.
- Обновлена схема БД (Flyway):
  - добавлена таблица `sections`;
  - добавлено поле `courses.section_id`, внешний ключ и индекс.
- Добавлен интеграционный тест `SectionCatalogIntegrationTest`.
- Исправлена нестабильность теста: убрана хрупкая проверка фиксированного размера каталога, проверки переведены на целевые курсы и относительный порядок.

## Проверки
- Выполнен прогон:
  - `mvn -pl monolith-mvp -Dtest=SectionCatalogIntegrationTest,CourseLessonCrudIntegrationTest test`
- Результат:
  - `BUILD SUCCESS`
  - `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`

## Копия активного контекста на момент завершения

```md
# Активный контекст

## Текущая задача
- **ID:** `TASK-026`
- **Категория:** `functional` (non-ui)
- **Приоритет:** `high`
- **Зависимости:** `TASK-009` (done)
- **Требования:** `FR-101`, `FR-102`, `AC-101`, `AC-102`

## Что сделано
1. Реализован backend-модуль разделов каталога:
   - `Section` entity;
   - `SectionRepository`;
   - DTO: `CreateSectionRequest`, `UpdateSectionRequest`, `SectionDto`;
   - `SectionService` с безопасной отвязкой курсов при удалении раздела;
   - `SectionsController` (`/api/v1/admin/sections`) с CRUD.
2. Реализована связь курса с разделом:
   - `Course` расширен `@ManyToOne Section section`;
   - `CreateCourseRequest` поддерживает `sectionId`;
   - `CourseDto` и `CourseSummaryDto` расширены section-полями;
   - `CourseMapper` дополнен mapstruct-маппингом section-полей.
3. Реализовано создание курса в контексте раздела:
   - `POST /api/v1/admin/sections/{sectionId}/courses`.
4. Реализована сортировка каталога курсов с учетом разделов:
   - сортировка по `section.priority`, затем `section.id`, затем `course.id`;
   - курсы без раздела идут после курсов с разделами.
5. Обновлена схема БД (Flyway):
   - таблица `sections`;
   - поле `courses.section_id`, FK и индекс.
6. Добавлен интеграционный тест:
   - `SectionCatalogIntegrationTest` (CRUD разделов, создание курса в разделе, отвязка после удаления, сортировка каталога и section-поля).
7. Исправлена нестабильность теста:
   - убрана хрупкая проверка фиксированного размера списка;
   - проверка выполняется по целевым курсам и их относительному порядку.

## Проверки
- Выполнен прогон:
  - `mvn -pl monolith-mvp -Dtest=SectionCatalogIntegrationTest,CourseLessonCrudIntegrationTest test`
- Результат:
  - `BUILD SUCCESS`
  - `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`

## Изменённые файлы
- `monolith-mvp/src/main/java/ru/just/monolithmvp/model/Course.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/model/Section.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/course/CreateCourseRequest.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/SectionRepository.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/repository/CourseRepository.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/section/CreateSectionRequest.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/section/UpdateSectionRequest.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/section/SectionDto.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/SectionService.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/SectionsController.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/course/CourseDto.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/dto/course/CourseSummaryDto.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/mapper/CourseMapper.java`
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/CourseService.java`
- `monolith-mvp/src/main/resources/db/migration/V1__init_schema.sql`
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/SectionCatalogIntegrationTest.java`

## Статус
- Реализация завершена, тесты зелёные.
- Следующие шаги workflow: зафиксировать артефакт, обновить индексы, отметить задачу `done`, затем очистить `02-active-context.md`.
```