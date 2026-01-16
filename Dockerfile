FROM gradle:8.10-jdk22 AS build
WORKDIR /app
COPY . .
RUN ./gradlew :modules:bootstrap:api-payment-gateway:bootJar --no-daemon

FROM eclipse-temurin:22-jre
WORKDIR /app
COPY --from=build /app/modules/bootstrap/api-payment-gateway/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
