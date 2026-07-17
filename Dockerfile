FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src src
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package \
    && cp target/cloud-data-base-*.jar /workspace/application.jar

FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S cloud-storage \
    && adduser -S -G cloud-storage cloud-storage

WORKDIR /app
COPY --from=build --chown=cloud-storage:cloud-storage /workspace/application.jar /app/application.jar

USER cloud-storage:cloud-storage
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/application.jar"]
