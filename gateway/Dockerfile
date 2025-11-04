# Build stage
FROM gradle:9-jdk21-alpine AS builder

WORKDIR /build

COPY build.gradle settings.gradle ./

COPY src ./src

RUN gradle :spotlessApply

RUN gradle clean build -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="your-org"
LABEL description="Gateway - API Gateway Service"

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /build/build/libs/*.jar gateway.jar

# Expose Eureka default port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Set JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar gateway.jar"]
