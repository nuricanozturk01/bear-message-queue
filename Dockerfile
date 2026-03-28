FROM node:22-alpine AS frontend
WORKDIR /w
COPY bearmq-frontend/package.json bearmq-frontend/package-lock.json ./
RUN npm ci
COPY bearmq-frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21-alpine AS backend
WORKDIR /b
COPY broker/pom.xml .
RUN mvn -q dependency:go-offline -DskipTests
COPY broker/src ./src
RUN mkdir -p src/main/resources/static && rm -rf src/main/resources/static/*
COPY --from=frontend /w/dist/bearmq-frontend/browser/ ./src/main/resources/static/
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S bearmq && adduser -S bearmq -G bearmq \
  && mkdir -p /app/data \
  && chown -R bearmq:bearmq /app
USER bearmq
COPY --from=backend /b/target/broker-*.jar /app/app.jar
EXPOSE 3333 6667 9092
ENV DB_URL=jdbc:h2:file:/app/data/bearmq;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE;DATABASE_TO_LOWER=TRUE
ENV DB_USERNAME=sa
ENV DB_PASSWORD=
ENV SERVER_PORT=3333
ENV BEARMQ_BROKER_TCP_PORT=6667
ENV BEARMQ_STORAGE_DIR=/app/data/queues
ENV BEARMQ_DOMAIN=localhost:6667
VOLUME /app/data
ENTRYPOINT ["java", \
  "--add-opens=java.base/java.lang=ALL-UNNAMED", \
  "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED", \
  "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED", \
  "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED", \
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED", \
  "--add-opens=java.base/java.io=ALL-UNNAMED", \
  "--add-opens=java.base/java.nio=ALL-UNNAMED", \
  "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED", \
  "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED", \
  "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED", \
  "-jar", "/app/app.jar"]
