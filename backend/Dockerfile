FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY backend/ backend/
COPY frontend/ frontend/
WORKDIR /build/backend
RUN chmod +x mvnw && ./mvnw -B package -DskipTests
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd -r sentinel && useradd -r -g sentinel sentinel && mkdir -p /app/data && chown -R sentinel:sentinel /app
COPY --from=build /build/backend/target/backend-0.0.1-SNAPSHOT.jar app.jar
USER sentinel
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
