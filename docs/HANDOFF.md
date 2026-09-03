# HANDOFF

**마지막 갱신**: 2026-07-07
**dev 최신 커밋**: `01fe1ed`
**현재 브랜치**: `refactor/seat-lock-redis` (커밋 `0e8a4b8`, push 완료 / **dev 미병합, PR 필요**)

이 문서는 프로젝트의 "지금 상태"를 담는 스냅샷이다. 매 작업 후 이 파일을 최신 상태로 다시 써서 유지한다 — 지난 작업의 시간순 기록이 아니라 "지금 뭐가 어떻게 되어 있고, 뭐가 남았는지"만 담는다. 커밋 단위 상세 이력은 `git log`로 확인한다.

---

## 진행 중 / dev 병합 대기

### 좌석 선점(Redis lock) 트랜잭션 버그 수정 — `refactor/seat-lock-redis`

**아직 PR 생성 전. dev에 병합 안 됨.**

- **문제였던 것**: `holdSeats()`가 `@Transactional` 메서드의 `finally`에서 redis lock을 해제했는데, 이 시점은 DB 커밋보다 먼저 실행됨. 그 틈에 동시 요청이 같은 좌석에 DB insert를 시도할 수 있었음. `ReservedSeat`의 `(screening_id, seat_id)` UNIQUE 제약이 데이터 정합성은 지켜주지만, 경쟁에서 진 요청은 깔끔한 `SEAT_ALREADY_HELD` 대신 일반 500 에러를 받았음.
- **지금 구조**: Redis lock은 "5분간 선점 유지"가 아니라 DB 가용성 체크+INSERT라는 짧은 임계구역만 보호(TTL 15초, 크래시 안전망 용도). 5분간의 실제 선점 상태는 DB(`ReservedSeat.status=HOLD` + UNIQUE 제약)만 담당. lock 해제는 `SeatHoldFacade`가 DB 커밋을 확인한 뒤에만 수행(성공/실패 양쪽 다).
- **핵심 파일**: `ReservationLockRepository`(Lua 스크립트 `scripts/seat-lock.lua`/`seat-unlock.lua` 기반 `lockAll()`/`unlockAllSafely()`), `SeatHoldFacade`(신규, 트랜잭션 밖 오케스트레이션), `ReservationService.createHoldReservation()`(DB 전용), `SeatHoldProperties.lockTtlSeconds`
- **검증**: 로컬 Docker Redis로 동시 요청 테스트 완료 — 하나는 302(성공), 하나는 `400 SEAT_ALREADY_HELD`(경합이 Redis 단계에서 걸러짐, DB까지 안 내려감). 처리 직후 redis 키 잔존 없음 확인.
- **남은 일**: PR 생성/리뷰. `ExpireHoldScheduler`가 ECS 멀티 task에서 각자 독립 실행되는 문제(ShedLock 등 분산락 없음)는 범위 밖으로 분리해둠 — 아래 "알려진 이슈" 참고.

---

## 완료된 주요 기능 (dev 반영됨)

### 영화 이미지 스토리지 (S3/CDN)

- `FileService` 인터페이스에 local/S3 구현체. DB(`movie_images.image_url`)엔 스토리지 key만 저장, 응답 시 `toImageUrl(key)`로 CDN URL 조합(도메인 변경 시 DB 마이그레이션 불필요).
- 영화 등록·수정 둘 다 "파일 업로드(트랜잭션 밖) → DB 저장(트랜잭션) → 실패 시 파일 보상 삭제 / 수정은 성공 시 기존 파일도 삭제" 패턴으로 통일 (`MovieRegistrationFacade`, `MovieUpdateFacade`).
- `OrphanImageCleanupBatchService`(매일 03:00 KST)가 DB에 참조 없는 스토리지 파일을 정리.
- **알려진 갭**:
  - 영화 삭제 API 자체가 없음 — 필요 시 soft delete로 구현해야 하는 이유가 `docs/movie-deletion-strategy.md`에 정리되어 있음(예매/환불 기록 보존, FK 무결성 때문에 하드 삭제 불가).
  - 이미지 URL 변환 누락 재발 방지용 타입 분리(`ImageKey`/`ResolvedImageUrl`) 설계는 `docs/image-url-resolution-improvement.md`에 정리만 해두고 보류 중(현재는 서비스 레이어에서 개별 처리).

### 인프라 / 배포

- ECS Fargate 배포. GitHub Actions CI/CD(`.github/workflows/dev-cicd.yml`): `dev` 브랜치 push → test → build → ECR → ECS 롤링 배포(AWS OIDC 인증), 동작 확인됨.
- Dockerfile 멀티스테이지 빌드, `entrypoint.sh`(AWS Secrets Manager JSON 파싱), health check(`GET /health`, 인증 불필요).
- Flyway(`V1__init.sql` 전체 DDL, `V2__seed_data.sql`) — dev는 `ddl-auto: none` + Flyway로 스키마 관리.
- Redis(세션 스토리지 + 좌석 선점 락)와 S3(이미지) 둘 다 dev 환경변수 구성 완료 — 아래 표 참고.
- MDC 로깅 필터(`traceId`/`method`/`uri` 전 로그 포함), 배치 서비스별 완료 로그.

#### ECS Task Definition 필수 환경변수

| 환경변수 | 비고 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | |
| `TOSS_CLIENT_KEY`, `TOSS_SECRET_KEY` | |
| `REDIS_HOST`, `REDIS_PASSWORD` | 폴백 없음 — 미설정 시 앱 기동 실패 |
| `S3_BUCKET_NAME`, `AWS_REGION`, `CDN_BASE_URL` | `file.storage.type=s3`용 |

---

## 알려진 이슈 / TODO

- **`ExpireHoldScheduler` ECS 멀티 task 중복 실행**: `@Scheduled(fixedDelay=60_000)`가 분산 스케줄 락(ShedLock 등) 없이 각 task에서 독립 실행됨. 벌크 UPDATE/DELETE라 멱등적이라 데이터는 안 깨지지만, task 수만큼 불필요하게 자주 실행됨.
- **Payment/Refund 플로우 로깅 미비**: `PaymentSuccessService`, `PaymentCancelService`, `PaymentTxService`, `RefundService`에 로깅 없음.
- **영화 삭제 기능 미구현**: 위 "영화 이미지 스토리지" 항목의 "알려진 갭" 참고.
- **좌석 선점 리팩토링 미병합**: 위 "진행 중" 항목 참고.
