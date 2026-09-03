#!/bin/sh
set -e

# Secrets Manager에서 valueFrom으로 주입된 JSON 환경변수 파싱
# 태스크 정의에서 아래처럼 설정:
#   "secrets": [{ "name": "APP_SECRETS", "valueFrom": "arn:aws:secretsmanager:..." }]
# 그러면 APP_SECRETS={"KEY":"VALUE",...} 형태로 컨테이너에 주입됨

SECRET_ENV_VAR="${SECRET_ENV_VAR:-APP_SECRETS}"
SECRET_JSON=$(printenv "$SECRET_ENV_VAR")

if [ -n "$SECRET_JSON" ]; then
  echo "[entrypoint] Parsing secrets from: $SECRET_ENV_VAR"

  if ! export_vars=$(echo "$SECRET_JSON" | jq -r 'to_entries[] | "export \(.key)=\(.value | @sh)"'); then
    echo "[entrypoint] ERROR: Failed to parse JSON from $SECRET_ENV_VAR"
    exit 1
  fi

  eval "$export_vars"
  echo "[entrypoint] Secrets loaded successfully"
else
  echo "[entrypoint] WARNING: $SECRET_ENV_VAR is not set, skipping secret parsing"
fi

exec "$@"