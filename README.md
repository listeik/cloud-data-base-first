# Cloud Data Base

[![CI](https://github.com/listeik/cloud-data-base-first/actions/workflows/ci.yml/badge.svg)](https://github.com/listeik/cloud-data-base-first/actions/workflows/ci.yml)

Многопользовательское файловое облако со встроенным web-интерфейсом. Пользователи
хранятся в PostgreSQL, HTTP-сессии — в Redis, файлы — в MinIO.

## Возможности

- регистрация, вход, выход и получение текущего пользователя;
- сессионная аутентификация через Spring Security и Spring Session Redis;
- изолированное файловое пространство для каждого пользователя;
- создание и просмотр директорий;
- загрузка одного или нескольких файлов;
- получение информации о ресурсе;
- скачивание файла или директории в виде ZIP-архива;
- поиск, перемещение и удаление ресурсов;
- постраничный просмотр директорий и результатов поиска;
- пользовательская квота и ограничение размера отдельного файла;
- адаптивный файловый frontend без отдельного сервера;
- OpenAPI-контракт, Swagger UI и HTTP-коллекция запросов;
- миграции схемы PostgreSQL через Flyway;
- единый JSON-формат ошибок;
- unit- и интеграционные тесты с PostgreSQL, Redis и MinIO в Testcontainers;
- CI-сборка и тестирование в GitHub Actions;
- production Docker image и публикация версий в GitHub Container Registry.

## Технологии

- Java 25
- Spring Boot 4.1.0
- Spring Web MVC, Security, Session, Data JPA, Validation, Actuator
- PostgreSQL 18 и Flyway
- Redis 8
- MinIO (S3-compatible object storage)
- springdoc-openapi 3 и Swagger UI
- HTML, CSS и JavaScript ES modules
- JUnit 5 и Testcontainers
- Maven Wrapper
- Docker Compose и GitHub Container Registry
- GitHub Actions

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
| Файловый интерфейс | `http://localhost:8080` | зарегистрированный пользователь |
| Swagger UI | `http://localhost:8080/swagger-ui.html` | session cookie |
| PostgreSQL | `localhost:5433/cloud_storage` | `cloud_storage` / `cloud_storage` |
| Redis | `localhost:6379` | без пароля |
| MinIO API | `http://localhost:9000` | `minioadmin` / `minioadmin` |
| MinIO Console | `http://localhost:9001` | `minioadmin` / `minioadmin` |

Остановить инфраструктуру можно командой `docker compose down`. Данные сохраняются
в Docker volumes. Для их удаления используйте `docker compose down -v`.

## Production-запуск

Production-контур запускает готовый image приложения вместе с PostgreSQL, Redis и
MinIO. Скопируйте пример переменных окружения и замените все значения `change-me`
на случайно сгенерированные секреты:

```bash
cp .env.example .env
```

Затем загрузите image и запустите стек:

```bash
docker compose --env-file .env -f compose.prod.yaml pull
docker compose --env-file .env -f compose.prod.yaml up -d
docker compose --env-file .env -f compose.prod.yaml ps
```

Приложение доступно только на `127.0.0.1:${APP_PORT}`. Перед ним должен находиться
HTTPS reverse proxy, например Caddy или Nginx. Production-профиль учитывает
forwarded headers, включает graceful shutdown и secure session cookie, а Swagger
и OpenAPI по умолчанию отключает. Для временного включения документации добавьте
в окружение приложения `OPENAPI_ENABLED=true`.

Проверка готовности приложения:

```bash
curl http://127.0.0.1:8080/actuator/health/readiness
```

Остановить production-контур без удаления данных:

```bash
docker compose --env-file .env -f compose.prod.yaml down
```

Workflow [`.github/workflows/publish-image.yml`](.github/workflows/publish-image.yml)
собирает образы для `linux/amd64` и `linux/arm64`, добавляет SBOM и provenance и
публикует их в `ghcr.io/listeik/cloud-data-base-first`. Выпуск версии запускается
Git-тегом:

```bash
git tag v0.1.0
git push origin v0.1.0
```

Для приватного GHCR package перед `docker compose pull` нужно выполнить
`docker login ghcr.io` с Personal Access Token, имеющим право `read:packages`.

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
| `GET` | `/api/directory/page?path=docs/&page=0&size=20` | Страница содержимого директории |
| `POST` | `/api/directory?path=docs/` | Создание директории |
| `GET` | `/api/resource?path=docs/file.txt` | Информация о ресурсе |
| `POST` | `/api/resource?path=docs/` | Загрузка файлов, multipart-поле `files` |
| `GET` | `/api/resource/download?path=docs/file.txt` | Скачивание файла или директории |
| `GET` | `/api/resource/search?query=file` | Поиск по имени |
| `GET` | `/api/resource/search/page?query=file&page=0&size=20` | Страница результатов поиска |
| `GET` | `/api/resource/move?from=old&to=new` | Перемещение или переименование |
| `DELETE` | `/api/resource?path=docs/file.txt` | Удаление ресурса |
| `GET` | `/api/storage/usage` | Занятое место, квота и лимит файла |

Корневая директория задаётся пустым `path`. Пути директорий оканчиваются символом
`/`, например `documents/reports/`.

Интерактивная документация доступна в Swagger UI. Машиночитаемый OpenAPI JSON
находится по адресу `/v3/api-docs`. Для ручной проверки из IDE можно использовать
[`requests/cloud-storage.http`](requests/cloud-storage.http).

## Тестирование

```bash
./mvnw test
```

В Windows PowerShell используйте `./mvnw.cmd test`.

Для интеграционных тестов нужен запущенный Docker. Testcontainers автоматически
поднимает изолированные PostgreSQL, Redis и MinIO; локальный `compose.yaml` во время
тестов не используется. Сейчас набор содержит 24 теста, включая:

- создание и инвалидирование Redis-backed сессии;
- запрет повторной регистрации с тем же username;
- полный жизненный цикл файла в MinIO;
- изоляцию файлов разных пользователей;
- контроль квоты и максимального размера файла;
- пагинацию каталогов и поиска;
- доступность OpenAPI и frontend-ресурсов;
- unit-тесты сервисов аутентификации, путей и хранилища.

Workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) запускает `mvn verify`
на JDK 25 для каждого push в `main` и для pull request. Docker на GitHub runner
используется интеграционными тестами через Testcontainers. Отдельный workflow
`publish-image.yml` публикует versioned production images в GHCR.

## Конфигурация

Настройки локального окружения находятся в
[`src/main/resources/application.yml`](src/main/resources/application.yml). Основные
параметры хранилища задаются через `app.storage`: endpoint, bucket, access key,
secret key, шаблон пользовательского префикса, `user-quota` и `max-file-size`.
Production-настройки находятся в
[`src/main/resources/application-prod.yml`](src/main/resources/application-prod.yml)
и получают адреса сервисов и секреты из переменных окружения.

## Дальнейшее развитие

1. Подключить целевой сервер и CD-деплой опубликованного image.
2. Добавить метрики, трассировку и централизованные логи.
3. Перейти к потоковой сборке ZIP для очень больших директорий.
4. Добавить роли администратора и управление индивидуальными квотами.
