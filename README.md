# Cloud Data Base

Учебный backend для многопользовательского файлового облака.

## Стек

- Java 25 LTS
- Spring Boot 4.1
- Spring Security, Spring Session Redis
- Spring Web MVC
- Spring Data JPA, PostgreSQL 18
- Flyway
- MinIO S3-compatible storage
- Redis 8
- Testcontainers
- Maven Wrapper

## Локальная инфраструктура

```bash
docker compose up -d
```

MinIO console: http://localhost:9001

Default credentials:

- PostgreSQL: `localhost:5433`, database/user/password: `cloud_storage`
- MinIO: `minioadmin` / `minioadmin`

## Что уже есть

- Регистрация, вход, выход и текущий пользователь через Spring Security session auth.
- Пользователи в PostgreSQL, миграции Flyway.
- Redis-backed HTTP sessions.
- MinIO bucket и пользовательский root-prefix при регистрации.
- API ресурсов: загрузка, скачивание файла, скачивание директории ZIP-архивом, список директории, создание директории, поиск, перемещение, удаление.
- Базовая обработка ошибок и unit-тесты для auth/path/service-слоя.

## Следующие этапы

1. Добавить интеграционные тесты на Testcontainers для PostgreSQL, Redis и MinIO.
2. Добавить OpenAPI/Swagger, когда совместимая версия будет доступна для Spring Boot 4.1.
3. Ввести квоты пользователя, лимиты размера файлов и пагинацию списков.
4. Подготовить frontend или Postman/HTTP collection для ручного тестирования API.
