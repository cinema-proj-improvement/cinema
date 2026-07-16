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

# 버전 고정 - "latest"로 받으면 재배포 시점마다 agent 버전이 달라져 재현이 안 됨
ARG OTEL_AGENT_VERSION=2.9.0

WORKDIR /app

# jq: entrypoint.sh의 Secrets Manager JSON 파싱용
# ca-certificates: wget으로 github release(https) 받으려면 필요
RUN apk add --no-cache jq ca-certificates && \
    addgroup -S appgroup && adduser -S appuser -G appgroup && \
    wget -q -O otel-javaagent.jar \
      "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar" && \
    chown appuser:appgroup otel-javaagent.jar

COPY --from=build --chown=appuser:appgroup /app/build/libs/app.jar app.jar
COPY --chown=appuser:appgroup entrypoint.sh entrypoint.sh

RUN chmod +x entrypoint.sh

USER appuser

EXPOSE 8080

# SPRING_PROFILES_ACTIVE — Task Definition 환경변수로 주입
# APP_SECRETS          — Task Definition secrets(valueFrom)로 주입 후 entrypoint.sh가 파싱
# OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_SERVICE_NAME 등 OTel 설정 — Task Definition 환경변수로 주입
#   (미설정 시에도 agent 자체는 기동에 영향 없음 - exporter 연결 실패는 로그만 남기고 앱은 정상 동작)
ENTRYPOINT ["./entrypoint.sh", "java", "-javaagent:/app/otel-javaagent.jar", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
