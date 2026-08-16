FROM maven:3.9.11-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml -B dependency:go-offline
COPY backend backend
RUN mvn -f backend/pom.xml -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 appuser \
    && useradd --system --uid 10001 --gid 10001 --no-create-home appuser \
    && mkdir -p /app/data/uploads /app/logs \
    && chown -R appuser:appuser /app
COPY --from=builder --chown=appuser:appuser /workspace/backend/target/*.jar app.jar
USER appuser
EXPOSE 8080
STOPSIGNAL SIGTERM
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
