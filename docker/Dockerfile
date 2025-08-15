# Multi-stage build für optimale Image-Größe
FROM gradle:8.8-jdk21 AS builder

# Copy source code
WORKDIR /app
COPY . .

# Build the application
RUN gradle clean build -x test

# Runtime stage - Eclipse Temurin ist die empfohlene Alternative
FROM eclipse-temurin:21-jre

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app user
RUN groupadd -r app && useradd -r -g app app

# Set working directory
WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs && chown -R app:app /app/logs

# Copy JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership
RUN chown -R app:app /app

# Switch to app user
USER app

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/test/health || exit 1

# Environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=docker
ENV CONTAINER_NAME=unknown-container
ENV CONTAINER_ID=0

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]