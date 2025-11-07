# Build stage
FROM gradle:9-jdk21-alpine AS builder

WORKDIR /build

COPY build.gradle settings.gradle ./

COPY src ./src

RUN gradle :spotlessApply

RUN gradle clean build -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="chatwolf"
LABEL description="Auth Service - JWT Authentication Service"

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /build/build/libs/*.jar auth.jar

# Expose Eureka default port
EXPOSE 7100

# Set JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar auth.jar"]
