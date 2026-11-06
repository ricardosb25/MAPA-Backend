FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system mapa && useradd --system --gid mapa mapa
COPY --from=build --chown=mapa:mapa /app/build/libs/mapa-api-*-SNAPSHOT.jar /app/app.jar

USER mapa
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
