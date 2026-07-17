# Cloud Data Base

Учебный backend многопользовательского файлового облака. Пользователи и метаданные
аутентификации хранятся в PostgreSQL, HTTP-сессии — в Redis, файлы — в MinIO.

## Возможности

- регистрация, вход, выход и получение текущего пользователя;
- сессионная аутентификация через Spring Security и Spring Session Redis;
- изолированное файловое пространство для каждого пользователя;
- создание и просмотр директорий;
- загрузка одного или нескольких файлов;
- получение информации о ресурсе;
- скачивание файла или директории в виде ZIP-архива;
- поиск, перемещение и удаление ресурсов;
- миграции схемы PostgreSQL через Flyway;
- единый JSON-формат ошибок;
- unit- и интеграционные тесты с PostgreSQL, Redis и MinIO в Testcontainers.

## Технологии

- Java 25
- Spring Boot 4.1.0
- Spring Web MVC, Security, Session, Data JPA, Validation, Actuator
- PostgreSQL 18 и Flyway
- Redis 8
- MinIO (S3-compatible object storage)
- JUnit 5 и Testcontainers
- Maven Wrapper

## Требования

- JDK 25
- Docker с поддержкой Docker Compose

Устанавливать Maven, PostgreSQL, Redis и MinIO отдельно не требуется.

## Локальный запуск

1. Запустите инфраструктуру:

   ```bash
   docker compose up -d
   ```

2. Запустите приложение:

   ```bash
   ./mvnw spring-boot:run
   ```

   В Windows PowerShell используйте `./mvnw.cmd spring-boot:run`.

3. Проверьте состояние приложения:

   ```bash
   curl http://localhost:8080/actuator/health
   ```

Локальные сервисы:

| Сервис | Адрес | Данные для входа |
|---|---|---|
| Приложение | `http://localhost:8080` | — |
| PostgreSQL | `localhost:5433/cloud_storage` | `cloud_storage` / `cloud_storage` |
| Redis | `localhost:6379` | без пароля |
| MinIO API | `http://localhost:9000` | `minioadmin` / `minioadmin` |
| MinIO Console | `http://localhost:9001` | `minioadmin` / `minioadmin` |

Остановить инфраструктуру можно командой `docker compose down`. Данные сохраняются
в Docker volumes. Для их удаления используйте `docker compose down -v`.

## Аутентификация

API использует cookie-based HTTP-сессии. Только регистрация и вход доступны без
аутентификации; остальные маршруты `/api/**` требуют session cookie.

Пример регистрации и сохранения cookie:

```bash
curl -i -c cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"username":"ratmir","password":"strong-password"}' \
  http://localhost:8080/api/auth/sign-up
```

Пример авторизованного запроса:

```bash
curl -b cookies.txt http://localhost:8080/api/user/me
```

Имя пользователя должно содержать от 3 до 64 символов: латинские буквы, цифры,
точку, дефис или подчёркивание. Длина пароля — от 8 до 128 символов.

## API

| Метод | Маршрут | Назначение |
|---|---|---|
| `POST` | `/api/auth/sign-up` | Регистрация и создание сессии |
| `POST` | `/api/auth/sign-in` | Вход и создание сессии |
| `POST` | `/api/auth/sign-out` | Выход и инвалидирование сессии |
| `GET` | `/api/user/me` | Текущий пользователь |
| `GET` | `/api/directory?path=docs/` | Содержимое директории |
| `POST` | `/api/directory?path=docs/` | Создание директории |
| `GET` | `/api/resource?path=docs/file.txt` | Информация о ресурсе |
| `POST` | `/api/resource?path=docs/` | Загрузка файлов, multipart-поле `files` |
| `GET` | `/api/resource/download?path=docs/file.txt` | Скачивание файла или директории |
| `GET` | `/api/resource/search?query=file` | Поиск по имени |
| `GET` | `/api/resource/move?from=old&to=new` | Перемещение или переименование |
| `DELETE` | `/api/resource?path=docs/file.txt` | Удаление ресурса |

Корневая директория задаётся пустым `path`. Пути директорий оканчиваются символом
`/`, например `documents/reports/`.

## Тестирование

```bash
./mvnw test
```

В Windows PowerShell используйте `./mvnw.cmd test`.

Для интеграционных тестов нужен запущенный Docker. Testcontainers автоматически
поднимает изолированные PostgreSQL, Redis и MinIO; локальный `compose.yaml` во время
тестов не используется. Сейчас набор содержит 15 тестов, включая:

- создание и инвалидирование Redis-backed сессии;
- запрет повторной регистрации с тем же username;
- полный жизненный цикл файла в MinIO;
- изоляцию файлов разных пользователей;
- unit-тесты сервисов аутентификации, путей и хранилища.

## Конфигурация

Настройки локального окружения находятся в
[`src/main/resources/application.yml`](src/main/resources/application.yml). Основные
параметры хранилища задаются через `app.storage`: endpoint, bucket, access key,
secret key и шаблон пользовательского префикса.

## Следующие этапы

1. Добавить OpenAPI/Swagger и коллекцию запросов для ручной проверки API.
2. Ввести пользовательские квоты и ограничения суммарного объёма хранилища.
3. Добавить пагинацию списков и результатов поиска.
4. Подготовить frontend для работы с облаком.
5. Добавить CI-пайплайн для сборки и тестов.
