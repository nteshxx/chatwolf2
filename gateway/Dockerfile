# Build stage
FROM gradle:9-jdk21-alpine AS builder

WORKDIR /home/gradle

COPY build.gradle settings.gradle ./

COPY gradle ./gradle

COPY src ./src

RUN gradle :spotlessApply

RUN gradle clean build -x test --no-daemon --parallel --build-cache

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="chatwolf"
LABEL description="API Gateway - Gateway Service for Microservices"

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /home/gradle/build/libs/*.jar gateway.jar

EXPOSE 7000

# Set JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar gateway.jar"]
