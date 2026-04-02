# ─── Build Stage ──────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B -Denforcer.skip=true

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -Denforcer.skip=true

# ─── Runtime Stage ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security (OWASP A01)
RUN addgroup -S monitor && adduser -S monitor -G monitor
USER monitor

COPY --from=build /app/target/*.jar app.jar

# Spring Boot default port
EXPOSE 8080

# Profile prod by default when running in container
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
