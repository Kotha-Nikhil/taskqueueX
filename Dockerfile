# Multi-stage build for both API and Worker modules
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent POM and all module POMs
COPY pom.xml .
COPY taskqueuex-common/pom.xml ./taskqueuex-common/
COPY taskqueuex-migrations/pom.xml ./taskqueuex-migrations/
COPY taskqueuex-api/pom.xml ./taskqueuex-api/
COPY taskqueuex-worker/pom.xml ./taskqueuex-worker/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY taskqueuex-common/src ./taskqueuex-common/src
COPY taskqueuex-migrations/src ./taskqueuex-migrations/src
COPY taskqueuex-api/src ./taskqueuex-api/src
COPY taskqueuex-worker/src ./taskqueuex-worker/src

# Build argument to specify which module to build
ARG MODULE_NAME=taskqueuex-api

# Build the specified module
RUN mvn clean package -DskipTests -pl ${MODULE_NAME} -am

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR
ARG MODULE_NAME=taskqueuex-api
COPY --from=build /app/${MODULE_NAME}/target/*.jar app.jar

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
