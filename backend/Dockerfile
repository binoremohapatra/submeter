# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Fetch dependencies first so this layer is cached even when source changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn -q -B package -DskipTests

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S submeter && adduser -S submeter -G submeter
USER submeter

# PDF and invoice file storage (mounted as a volume in docker-compose)
RUN mkdir -p /app/storage/invoices

COPY --from=builder /build/target/submeter-api-*.jar app.jar

EXPOSE 8080

# JVM flags tuned for container memory limits:
# -XX:+UseContainerSupport  — respects cgroup memory limits (not host RAM)
# -XX:MaxRAMPercentage=75   — use up to 75% of container RAM for heap
# -Dspring.profiles.active  — set via environment; defaults to prod
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "/app/app.jar"]
