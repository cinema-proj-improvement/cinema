# HANDOFF

---

## feature/setup-logging

**기간**: 2026-06-22 ~

### 작업한 내용

#### 인프라 설정

- **`build.gradle`**: `logstash-logback-encoder:8.0` 의존성 추가
- **`src/main/resources/logback-spring.xml`** (신규):
  - `local`, `test` 프로파일: `%highlight`, `%cyan` 등 색상 패턴 + `%X{traceId:-}` 포함 콘솔 출력. Hibernate SQL/바인드 파라미터 DEBUG/TRACE 레벨.
  - `dev` 프로파일: `LogstashEncoder`로 JSON stdout 출력 → ECS awslogs → CloudWatch
- **`application.yml`**: `logging.level.root: INFO` 제거 (logback-spring.xml로 일원화)
- **`application-test.yml`**: `logging.level.root: WARN` 제거

#### HTTP 요청/응답 로깅 (MDC Filter)

- **`global/logging/filter/MdcLoggingFilter.java`** (신규):
  - `OncePerRequestFilter` 구현
  - 요청 진입 시 `traceId`(UUID), `method`, `uri`를 MDC에 세팅 → 해당 요청의 모든 로그에 자동 포함
  - 응답 시점 finally 블록에서 `"{method} {uri} {status} {duration}ms"` INFO 로그
  - `shouldNotFilter()`로 정적 리소스(`/css/`, `/js/`, `/images/`, `/static/`, `/uploads/`, `/favicon.ico`) 제외
  - `@Component` 없음 (FilterRegistrationBean 이중 등록 방지)
- **`global/logging/config/LoggingFilterConfig.java`** (신규):
  - `FilterRegistrationBean`으로 `MdcLoggingFilter` 등록, `Ordered.HIGHEST_PRECEDENCE`로 가장 바깥(첫 번째) 실행

#### 서비스 레이어 로깅

- **`MemberService.signup()`**: 이메일 중복 throw 전 WARN, 닉네임 중복 throw 전 WARN, 저장 성공 후 INFO (memberId, email)
- **`ReservationService`**:
  - `holdSeats()`: Redis 좌석 선점 실패 시 WARN (screeningId, seatId, memberId)
  - `holdSeats()`: 선점 성공 시 INFO (reservationId, screeningId, memberId, seatIds)
  - `cancelHoldReservation()`: 타인 예매 취소 시도 WARN, 취소 완료 INFO
  - `validateSeatCount()`: 최대 좌석 수 초과 시 WARN (요청 수, max)
  - `validateBookable()`: 이미 선점/예매된 좌석 포함 시 WARN (screeningId, 요청 seatIds, blockedSeatIds)

#### 배치 서비스 로깅

- **`MovieStatusBatchService`**: 배치 완료 후 INFO (today, UPCOMING→NOW_SHOWING 건수, →ENDED 건수)
- **`ScreeningStatusBatchService`**: 오픈 전환 완료 INFO (SCHEDULED→OPEN 건수, from/to), 종료 처리 완료 INFO (→FINISHED 건수, now)
- **`ExpireHoldBatchService`**: 배치별 처리 건수 > 0 인 경우에만 INFO (batch 번호, processed 건수). 만료 HOLD 없는 실행(= 대부분)에서는 로그 없음.
- **`MovieReservationRateBatchService`**: 갱신 완료 후 INFO (처리 영화 수)

### 미완료 (나중에 처리)

- **Payment/Refund 로깅**: `PaymentSuccessService`, `PaymentCancelService`, `PaymentTxService`, `RefundService` — 결제·환불 플로우 로깅 미추가
- **`MovieImageService` 로깅**: 해당 서비스 코드 수정 시 같이 추가 예정

---

## feature/create-dockerfile

**기간**: 2026-06-09 ~ 2026-06-18

### 작업한 내용

- **Dockerfile 멀티 스테이지 빌드** (eclipse-temurin:21-jre-alpine 기반, `app.jar` 고정)
- **`entrypoint.sh`**: AWS Secrets Manager JSON 시크릿 자동 파싱
- **Health check 엔드포인트** (`GET /health`): ECS ALB 타겟 그룹 대응, 인증 없이 접근 허용
- **Flyway 도입**: `V1__init.sql`(전체 DDL), `V2__seed_data.sql`(seed) 작성
- **GitHub Actions CI/CD** (`.github/workflows/dev-cicd.yml`): `dev` 브랜치 push 시 test → build → ECR 푸시 → ECS 롤링 배포 자동화. AWS OIDC 인증 사용. **동작 확인 완료.**
- **설정 정비**: `application-dev.yml` 환경변수화, `application-test.yml` CI 통과용 정비 (Redis 연결 없이 빈 생성만, Toss 더미 키 하드코딩)

### 현재 상태

- CI/CD 파이프라인 동작 중 (`dev` 브랜치 push → ECR → ECS 자동 배포)
- **Redis 미연동**: 세션은 JVM 인메모리, 좌석 선점 락 비동작
- **`application-dev.yml` 미커밋 변경사항 있음**: `REDIS_HOST` 폴백 제거, `REDIS_PASSWORD` 추가 → 커밋 필요
- 파일 업로드는 컨테이너 로컬 저장 (재시작 시 유실)

### ECS Task Definition 환경변수 목록

| 환경변수 | 필수 여부 | 비고 |
|---------|----------|------|
| `SPRING_PROFILES_ACTIVE` | 필수 | `dev` |
| `DB_URL` | 필수 | |
| `DB_USERNAME` | 필수 | |
| `DB_PASSWORD` | 필수 | |
| `TOSS_CLIENT_KEY` | 필수 | |
| `TOSS_SECRET_KEY` | 필수 | |
| `REDIS_HOST` | Redis 구성 후 필수 | 폴백 없음 — 미설정 시 앱 기동 실패 |
| `REDIS_PASSWORD` | Redis 구성 후 필수 | |
| `FILE_UPLOAD_BASE_PATH` | 선택 | 미설정 시 `/app/uploads` 폴백 |

### 나중에 처리할 것들

#### Redis 서버 구성 후
- `application-dev.yml`: `store-type: none` → `redis`
- ECS Task Definition에 `REDIS_HOST`, `REDIS_PASSWORD` 추가
- 정상화될 기능: 좌석 선점 락, 예약 충돌 방지, 세션 지속성

#### 파일 스토리지 S3 전환 시
- `application-dev.yml`: `file.storage.type: local` → `s3`
- S3 버킷, IAM 권한, 관련 환경변수 구성
