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

- PostgreSQL: `cloud_storage` / `cloud_storage`
- MinIO: `minioadmin` / `minioadmin`

## Следующие этапы

1. Реализовать регистрацию и вход через Spring Security.
2. Создавать пользовательский root-prefix в MinIO после регистрации.
3. Реализовать операции `upload`, `list`, `download`, `move`, `delete`.
4. Добавить интеграционные тесты для пользователей и MinIO.
5. Подключить OpenAPI/Swagger, когда совместимая версия будет доступна для Spring Boot 4.1.
