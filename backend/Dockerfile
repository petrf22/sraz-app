# Stage 1: Příprava Node.js pro Gradle
FROM node:24-alpine AS frontend-builder

WORKDIR /frontend

#COPY prani-fe/package*.json ./
COPY prani-fe .
RUN npm install
RUN npm run build

# Debug - zjistíme strukturu adresářů
#RUN echo "=== Obsah /frontend ===" && ls -la /frontend
#RUN echo "=== Obsah /frontend/dist ===" && ls -la /frontend/dist || echo "dist neexistuje"
#RUN echo "=== Obsah /frontend/dist/browser ===" && ls -la /frontend/dist/browser || echo "dist/browser neexistuje"
#RUN find /frontend -name "index.html" -type f

# Stage 2: Build backend
FROM eclipse-temurin:25-jdk-alpine AS backend-builder

WORKDIR /build/sraz-be

COPY prani-be/gradlew .
COPY prani-be/gradle gradle
COPY prani-be/build.gradle prani-be/settings.gradle ./

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY prani-be/src src
# Zkopírování Angular buildu do static adresáře
COPY --from=frontend-builder /frontend/dist/sraz-fe/browser ./src/main/resources/static

# Build aplikace - backend
RUN ./gradlew build -i --no-daemon -x test

# Stage 3: Runtime
FROM eclipse-temurin:25-jre-alpine

RUN apk add --no-cache curl
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

COPY --from=backend-builder --chown=spring:spring /build/sraz-be/build/libs/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]