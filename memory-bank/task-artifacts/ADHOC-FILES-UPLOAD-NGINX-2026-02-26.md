# ADHOC — FilesController.upload: публичный URL + раздача через Nginx (2026-02-26)

## Что требовалось
- Провалидировать `ru.just.monolithmvp.controller.FilesController.upload`.
- Возвращать не путь в ФС, а полноценную публичную ссылку на файл.
- Спроектировать схему, где запросы идут через Nginx, а сам файл отдаёт файловая система (не Java-приложение).

## Что сделано

### 1) Backend: upload возвращает URL
- `FilesController.upload(...)` теперь:
  1. сохраняет файл,
  2. строит публичный URL,
  3. возвращает его в `FileUploadResponse.path`.

Файл:
- `monolith-mvp/src/main/java/ru/just/monolithmvp/controller/FilesController.java`

### 2) Backend: усилена файловая безопасность и URL-резолвинг
- В `FileStorageService` добавлено:
  - `app.storage.public-base-url`;
  - метод `toPublicUrl(storedPath)`;
  - проверка, что целевая директория/файл не выходят за `root-dir`;
  - защита `deleteIfExists` от попыток удалить URL.

Файл:
- `monolith-mvp/src/main/java/ru/just/monolithmvp/service/FileStorageService.java`

### 3) Конфигурация приложения
- Добавлены свойства:
  - `app.storage.root-dir`
  - `app.storage.public-base-url`

Файлы:
- `monolith-mvp/src/main/resources/application.yml`
- `monolith.env.example`

### 4) Документация runtime/Nginx
- В `README-runtime.md` добавлен раздел:
  - обязательные env для файлов,
  - пример Nginx-конфига (`/api` -> backend, `/files` -> `alias` на ФС),
  - пояснение, что контент отдаёт Nginx с диска.

Файл:
- `monolith-mvp/README-runtime.md`

### 5) Тесты
- Обновлён интеграционный тест: после `/api/v1/files/upload` ожидается URL с префиксом `http://localhost:8099/files/`.

Файл:
- `monolith-mvp/src/test/java/ru/just/monolithmvp/controller/UserAuthStudentFlowIntegrationTest.java`

Запуск:
- `mvn -f monolith-mvp/pom.xml -Dtest=UserAuthStudentFlowIntegrationTest test`
- Результат: `BUILD SUCCESS`.

## Итого
- Endpoint upload теперь возвращает публичную ссылку.
- Архитектура раздачи соответствует требованию: приложение сохраняет и возвращает URL, Nginx отдаёт файл напрямую из файловой системы.