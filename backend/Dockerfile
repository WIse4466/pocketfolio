FROM openjdk:21-jdk-slim

WORKDIR /app

COPY . /app

RUN ./gradlew build -x test

EXPOSE 8080

CMD ["java", "-jar", "build/libs/pocketfolio-0.0.1-SNAPSHOT.jar"]
