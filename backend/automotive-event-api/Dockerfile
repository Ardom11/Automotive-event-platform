FROM eclipse-temurin:21-jdk-alpine AS dependencies
RUN apk add --no-cache maven
WORKDIR /build
COPY pom.xml .
COPY automotive-event-api/pom.xml automotive-event-api/pom.xml
WORKDIR /build/automotive-event-api
RUN mvn dependency:go-offline

FROM dependencies AS builder
COPY automotive-event-api/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
EXPOSE 8080
COPY --from=builder /build/automotive-event-api/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]