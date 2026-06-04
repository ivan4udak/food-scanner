# Runtime-образ backend. JAR собирается в GitHub Actions (mvn package) — на сервере
# и внутри этого образа Maven НЕ запускается (требование: сборка только в CI).
# Шаг пайплайна "Docker Build" лишь упаковывает готовый артефакт target/*.jar.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Непривилегированный пользователь.
RUN addgroup -S app && adduser -S app -G app

# Готовый артефакт (создан шагом Maven Build).
COPY target/*.jar /app/app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8080

# Контейнерный health-check (busybox wget есть в alpine). Используется Docker/Compose
# и blue-green деплоем для проверки готовности новой версии.
HEALTHCHECK --interval=15s --timeout=4s --start-period=45s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/api/v1/ping >/dev/null 2>&1 || exit 1

# JAVA_OPTS позволяет тюнить память/GC из env без пересборки.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
