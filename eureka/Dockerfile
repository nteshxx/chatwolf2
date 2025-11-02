# Build stage
FROM gradle:9-jdk21-alpine AS builder

WORKDIR /build

COPY build.gradle settings.gradle gradle.properties* ./
COPY gradle ./gradle
RUN gradle build --no-daemon 2>/dev/null || true

COPY src ./src
RUN gradle :spotlessApply
RUN gradle clean build -x test --no-daemon -q

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="your-org"
LABEL description="Eureka Server - Service Discovery"

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1000 eureka && adduser -D -u 1000 -G eureka eureka

# Copy built JAR from builder stage
COPY --from=builder /build/build/libs/*.jar eureka.jar

# Expose Eureka default port
EXPOSE 8761

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

# Switch to non-root user
USER eureka

# Set JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar eureka.jar"]
