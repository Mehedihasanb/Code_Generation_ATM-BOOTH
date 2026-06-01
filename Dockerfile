# US-29: Spring Boot backend for Render (build from repo root)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY backend/ .
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/backend-*.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
