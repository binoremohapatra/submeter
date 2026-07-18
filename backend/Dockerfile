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

# Create non-root user
RUN addgroup -S submeter && adduser -S submeter -G submeter

# Create storage directory as root and give permissions
RUN mkdir -p /app/storage/invoices && \
    chown -R submeter:submeter /app

# Copy application
COPY --from=builder /build/target/submeter-api-*.jar app.jar

# Switch to non-root user
USER submeter

EXPOSE 8080

ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "/app/app.jar"]