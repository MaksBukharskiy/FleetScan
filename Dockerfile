FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-rus && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]