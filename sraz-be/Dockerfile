# Produkční obraz backendu (stejný setup jako kvalita-cena). Testy se tu záměrně nepouštějí –
# integrační testy potřebují Docker (Testcontainers), patří do CI, ne do buildu image.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Commit, ze kterého se obraz staví – compose.prod.yaml ho předává jako build arg, buildInfo
# v build.gradle ho promítne do /actuator/info (ops/deploy.sh ho po nasazení ověřuje).
ARG GIT_SHA=""
ENV GIT_SHA=${GIT_SHA}

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --create-home --home-dir /app appuser \
    && chown -R appuser:appuser /app

COPY --from=build /app/build/libs/*.jar app.jar

USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
