FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw --batch-mode dependency:go-offline
COPY src src
RUN ./mvnw --batch-mode clean package -DskipTests

FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app
COPY --from=builder /app/target/view-gene-backend-0.1.0-SNAPSHOT.jar app.jar
RUN mkdir -p /app/uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
