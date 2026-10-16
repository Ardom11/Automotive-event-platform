FROM eclipse-temurin:21-jdk-alpine AS dependencies
RUN apk add --no-cache maven
WORKDIR /build
COPY pom.xml .
COPY notification-service/pom.xml notification-service/pom.xml
WORKDIR /build/notification-service
RUN mvn dependency:go-offline

FROM dependencies AS builder
COPY notification-service/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
EXPOSE 8081
COPY --from=builder /build/notification-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]