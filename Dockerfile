# PHASE 1: Build Environment
FROM eclipse-temurin:11-jdk-focal AS build
WORKDIR /app

# Copy gradle wrapper and config
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (caching layer)
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY src src
RUN ./gradlew build -x test --no-daemon

# PHASE 2: Runtime Environment
FROM eclipse-temurin:11-jre-focal
WORKDIR /app

# Maxing out Efficiency: Copy only the finalized fat JAR
COPY --from=build /app/build/libs/stadiumflow-0.0.1-SNAPSHOT.jar app.jar

# Expose the standard Spring Boot port
EXPOSE 8080

# Run with optimized JVM settings
ENTRYPOINT ["java", "-jar", "app.jar"]
