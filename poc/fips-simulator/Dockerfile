# Stage 1 — build
FROM dhi.io/maven:3-jdk21-alpine3.22-dev AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn package -DskipTests -B

# Stage 2 — runtime
FROM dhi.io/eclipse-temurin:21-alpine3.23
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
