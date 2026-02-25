# ADHOC — стабилизация CourseLessonCrudIntegrationTest (2026-02-25)

## Контекст
- В ходе выполнения активной задачи требовалась стабилизация интеграционного теста:
  `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`.
- Цель: устранить нестабильные/некорректные ожидания в сценариях lesson/progress/review без изменения бизнес-контрактов вне тестового контура.

## Что было изменено
1. Для тестового класса включён профиль `mail-noop` вместе с `h2`:
   - `spring.profiles.active=h2,mail-noop`.
2. Уточнён сценарий с THEORY:
   - добавлены проверки существования авто-сабмишена `COMPLETE`;
   - подтверждена идемпотентность `complete-theory` (не создаёт дубль).
3. В сценарии `admin_and_student_course_lesson_crud_flow_should_work` добавлены обязательные prereq-шаги перед open-ended submit:
   - завершение theory;
   - успешный submit по тестовой практике.
4. В corner-case сценарии прохождения/review добавлены и уточнены шаги:
   - предварительный успешный submit по choice-вопросу;
   - корректный workflow `REWORK -> resubmit -> ACCEPTED`;
   - отдельная проверка нового open-ended submission;
   - проверки истории статусов по обоим сабмишенам.

## Статус валидации
- Пользователь подтвердил, что тесты после правок проходят.
- По прямому указанию пользователя повторный запуск тестов не выполнялся.

## Затронутые файлы
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/CourseLessonCrudIntegrationTest.java`
- `memory-bank/02-active-context.md`
