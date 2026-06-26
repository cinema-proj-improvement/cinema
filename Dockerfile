# =============================================================
# Stage 1: dependencies
# build.gradle / settings.gradle 가 바뀌지 않으면 레이어 재사용
# =============================================================
FROM eclipse-temurin:21-jdk-alpine AS dependencies
WORKDIR /app
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && \
    ./gradlew dependencies --no-daemon

# =============================================================
# Stage 2: build
# 소스만 바뀌어도 의존성 레이어는 캐시 유지
# =============================================================
FROM dependencies AS build
COPY src/ src/
RUN ./gradlew clean build -x test --no-daemon

# =============================================================
# Stage 3: production
# 앱 실행에 필요한 최소 요소만 포함
# =============================================================
FROM eclipse-temurin:21-jre-alpine AS production

# entrypoint.sh의 Secrets Manager JSON 파싱에 필요
RUN apk add --no-cache jq && \
    addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build --chown=appuser:appgroup /app/build/libs/app.jar app.jar
COPY --chown=appuser:appgroup entrypoint.sh entrypoint.sh

RUN chmod +x entrypoint.sh

USER appuser

EXPOSE 8080

# SPRING_PROFILES_ACTIVE — Task Definition 환경변수로 주입
# APP_SECRETS          — Task Definition secrets(valueFrom)로 주입 후 entrypoint.sh가 파싱
ENTRYPOINT ["./entrypoint.sh", "java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
