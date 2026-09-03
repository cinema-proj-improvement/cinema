# Docker 배포 가이드

## 사전 요구사항

- Java 21
- Docker

## 빌드 및 실행

### 1. JAR 빌드

```bash
./gradlew build -x test
```

`build/libs/cinema-0.0.1-SNAPSHOT.jar` 생성됨.

### 2. Docker 이미지 빌드

```bash
docker build -t cinema .
```

### 3. 컨테이너 실행

```bash
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://<host>:3306/cinema?serverTimezone=Asia/Seoul&characterEncoding=UTF-8 \
  -e DB_USERNAME=<username> \
  -e DB_PASSWORD=<password> \
  -e TOSS_CLIENT_KEY=<key> \
  -e TOSS_SECRET_KEY=<secret> \
  cinema
```

## 환경변수

`dev` 프로파일 기준 필수 환경변수.

| 변수 | 설명 |
|------|------|
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` | DB 계정 |
| `DB_PASSWORD` | DB 비밀번호 |
| `TOSS_CLIENT_KEY` | Toss Payments 클라이언트 키 |
| `TOSS_SECRET_KEY` | Toss Payments 시크릿 키 |
| `SPRING_PROFILES_ACTIVE` | 기본값 `dev`, 필요 시 재정의 |

Redis는 `application-dev.yml`에 `localhost:6379`로 고정되어 있으므로, 컨테이너 실행 시 Redis 호스트를 별도로 지정해야 한다면 `SPRING_DATA_REDIS_HOST` 환경변수로 재정의.

## Dockerfile 설계 근거

| 항목 | 선택 | 이유 |
|------|------|------|
| 베이스 이미지 | `eclipse-temurin:21-jre-alpine` | JDK 불필요, Alpine으로 이미지 경량화 |
| 빌드 방식 | JAR 복사 (방식 A) | GitHub Actions에서 빌드·테스트 후 JAR만 전달, Gradle 빌드 중복 방지 |
| 비 root 실행 | `appuser` | 컨테이너 탈출 시 호스트 피해 최소화 |
| `-XX:MaxRAMPercentage=75.0` | JVM 힙을 컨테이너 메모리의 75%로 제한 | ECS 태스크 메모리 설정과 연동, OOM 방지 |

## GitHub Actions + ECR + ECS 파이프라인 (예정)

향후 파이프라인 흐름:

```
dev branch merge
  → Actions: ./gradlew test
  → Actions: ./gradlew build -x test
  → Actions: docker build & push to ECR
  → Actions: ECS 서비스 업데이트 (새 태스크 배포)
```
