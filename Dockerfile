FROM gradle:8.11.1-jdk21 AS build

WORKDIR /workspace
COPY . .
RUN ./gradlew bootJar -x test

FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update && \
    apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-rus && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
