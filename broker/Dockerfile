# JAR-only image (no UI). For the full stack (Angular + API + H2) build from the repository root:
#   docker build -t bearmq .
# See /Dockerfile in the parent directory.

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw package -q -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S bearmq && adduser -S bearmq -G bearmq
USER bearmq

COPY --from=build /app/target/broker-*.jar app.jar

EXPOSE 3333 6667

ENTRYPOINT ["java", "-jar", "app.jar"]
