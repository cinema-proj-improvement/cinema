# =============================================================
# ECS Fargate 앱 task의 사이드카 컨테이너로 뜨는 OTel Collector 이미지
# 앱 컨테이너의 otel-javaagent -> (localhost, 같은 task 내 네트워크 네임스페이스 공유) -> 이 collector
#   -> EC2의 Prometheus로 remote_write push
# prometheusremotewrite exporter는 contrib 배포판에만 있어서 core 이미지가 아닌 contrib 사용
# =============================================================
ARG OTEL_COLLECTOR_VERSION=0.110.0
FROM otel/opentelemetry-collector-contrib:${OTEL_COLLECTOR_VERSION}

COPY otel-collector-config.yml /etc/otel-collector-config.yml

ENTRYPOINT ["/otelcol-contrib", "--config=/etc/otel-collector-config.yml"]
