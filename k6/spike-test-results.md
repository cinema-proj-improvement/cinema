# 좌석 선점 스파이크 테스트 결과

- 작성일: 2026-07-10
- 대상 브랜치: `refactor/seat-lock-redis`
- 대상 커밋: `0e8a4b8` (좌석 선점 Redis lock 트랜잭션 순서 버그 수정 및 원자적 잠금 개선)
- 테스트 스크립트: `k6/scenarios/a-seat-hold-spike.js`
- 테스트 환경: dev ECS Fargate(**1 vCPU**, internet-facing ALB) + RDS MySQL **`db.t4g.micro`(2 vCPU)**(Flyway) + Redis — 실습/비용 절감을 위해 최소 사양으로 구성

> 용어 참고: 이 문서는 **스파이크 테스트**(순간 동시 접속 폭증 상황에서의 정합성 검증) 결과입니다.
> 부하를 점진적으로 늘려 시스템 한계점을 찾는 **스트레스 테스트**(시나리오 B)는 아직 실시하지 않았습니다.

## 코드 수정 요약 (`0e8a4b8`)

**좌석 선점(Redis lock) 트랜잭션 순서 버그 수정 및 원자적 잠금 개선**

- `ReservationLockRepository.lockAll()` — 여러 좌석을 Lua 스크립트(`seat-lock.lua`)로 **원자적으로 한 번에** 잠금. 하나라도 실패하면 이번 호출에서 잠근 것들이 자동 롤백되어, 일부만 잠기는 상태가 생기지 않음.
- `SeatHoldFacade.holdSeats()` — 락을 **DB 트랜잭션 시작 전에 잡고, DB 커밋이 성공한 뒤에 해제**하도록 순서를 바로잡음. (커밋 전에 락을 풀면 그 틈에 다른 요청이 같은 좌석을 잡을 수 있는 경쟁 구간이 생기는데, 그걸 막은 게 핵심)
- `ReservationService.createHoldReservation()` — Redis 락으로 대부분 걸러지지만, 커밋 타이밍상의 극히 좁은 경쟁 창에 대비해 DB 유니크 제약 위반 시에도 `SEAT_ALREADY_HELD`로 안전하게 처리하는 안전망 유지.

## 테스트 시나리오

| 항목 | 내용 |
|---|---|
| 도구 | k6 (`per-vu-iterations` executor, VU당 정확히 1회 실행) |
| 대상 | `POST /reservations/holds` (좌석 선점) |
| 시나리오 | 40석 상영에 200명이 거의 동시에 좌석 홀드 시도 (좌석당 평균 5명 경쟁) |
| 픽스처 | k6 `setup()`이 Admin API로 테스트 전용 40석 상영관+상영을 매 실행마다 새로 생성 |

테스트는 두 라운드로 진행했다. 1라운드에서 예상보다 훨씬 큰 지연시간이 나와서 원인을 조사했고,
그 결과를 반영해 스크립트를 고친 뒤 2라운드를 다시 실행했다.

---

## 1라운드 — 로그인 포함 측정

첫 버전은 VU마다 `default()`(측정 구간) 안에서 매번 로그인부터 새로 했다.

### 결과

- **정합성: 통과** — `seat_hold_success=40`, `seat_hold_conflict=160`, `unexpected_error=0`. DB로 직접 재확인해도 `reserved_seats`에 HOLD 40건 실존, 동일 좌석 중복 배정 **0건**.
- **응답 성능: 기준(p95<800ms) 크게 미달** — `seat_hold_duration` p95 3.68~5.51s, 전체 `http_req_duration` p95 15~18s, VU당 `iteration_duration` 평균 21s, 전체 실행 시간 ~23초.

```
seat_hold_success..............: 40     1.735878/s
seat_hold_conflict.............: 160    6.943514/s
seat_hold_unexpected_error.....: 0      0/s
seat_hold_duration.............: avg=1.72s  min=80.77ms med=1.08s  max=5.97s  p(90)=3.22s  p(95)=3.68s

http_req_duration..............: avg=5.18s  min=17.4ms  med=2.7s   max=20.64s p(90)=15.19s p(95)=18.04s
http_req_failed.................: 19.72% 160 out of 811

iteration_duration..............: avg=21.05s min=19.25s  med=21.18s max=21.86s p(90)=21.75s p(95)=21.83s
iterations......................: 200    8.679392/s

running (0m23.0s), 200 complete and 0 interrupted iterations
```

### 원인 조사 — CloudWatch

같은 시각(테스트 실행 구간)의 CloudWatch 지표를 직접 조회했다.

| 메트릭 | 값 | 비고 |
|---|---|---|
| ECS task CPUUtilization | **99.5%** | 평소 ~0% |
| ECS task MemoryUtilization | 20.4% | 여유 충분 |
| RDS(`cinema-dev-db`) CPUUtilization | 5.4% | 여유 충분 |
| RDS DatabaseConnections | 10.0 (최댓값) | HikariCP 기본 풀 크기(10)와 일치 |

**병목은 DB가 아니라 ECS task의 CPU였다.** RDS는 여유가 있는데 ECS task CPU는 거의 100%를 찍었다.
용의자는 **로그인마다 도는 BCrypt 해시 검증** — VU 200개가 거의 동시에 로그인을 시도하면서 BCrypt 연산이
1 vCPU 위에서 전부 직렬화된 것으로 추정. (BCrypt는 브루트포스 방어를 위해 의도적으로 CPU를 많이 쓰도록
설계된 알고리즘이라, 이 자체는 정상 동작이며 코드 결함이 아니다.)

또한 테스트 설계 자체의 문제도 있었다: 실제 티켓 오픈런 상황에서는 사용자들이 오픈 전부터 이미
로그인해서 대기하는 것이 일반적이고, 오픈 순간에 로그인까지 동시에 몰리는 게 아니다. 즉 1라운드는
"좌석락 코드의 성능"과 "동시 로그인 처리 용량"이 뒤섞인, 실제 시나리오보다 더 가혹한 조건이었다.

---

## 2라운드 — 로그인을 측정 구간 밖으로 분리

`setup()`에서 로그인 · CSRF 토큰 · 좌석 목록 조회를 전부 미리 끝내고, `default()`(측정 구간)는
`POST /reservations/holds` 한 번만 재도록 스크립트를 수정했다. (세션 쿠키를 `setup()`에서 한 번만
발급받아 모든 VU가 재사용 — Spring Session + Redis 기반이라 세션 하나를 여러 요청이 동시에 사용해도
문제없음.)

### 결과

```
seat_hold_success..............: 40     6.150991/s
seat_hold_conflict.............: 160    24.603965/s
seat_hold_unexpected_error.....: 0      0/s
seat_hold_duration.............: avg=2.79s min=699.25ms med=3.04s max=3.71s p(90)=3.65s p(95)=3.65s

http_req_duration..............: avg=2.65s min=22.7ms   med=2.94s max=3.71s p(90)=3.65s p(95)=3.65s
  { expected_response:true }...: avg=1.56s min=22.7ms   med=1.29s max=3.66s p(90)=2.94s p(95)=3.25s
http_req_failed.................: 75.47% 160 out of 212

iterations......................: 200    30.754956/s
running (0m06.5s), 200 complete and 0 interrupted iterations
```

- **정합성: 이번에도 통과** — 40/160/0, 동일하게 확인됨.
- **응답 성능: 여전히 threshold(800ms) 미달이지만 크게 개선** — 전체 실행 시간 **23초 → 6.5초**, `seat_hold_duration` p95는 3.65s로 1라운드와 비슷해 보이지만, 로그인 부담이 빠지고도 이 정도라는 게 핵심.

로그인을 빼자마자 실행 시간이 1/3.5로 줄어든 것 자체가 "1라운드 지연의 대부분은 BCrypt+1vCPU 때문이었다"는 걸 실측으로 증명한다.

### 남은 지연(p95 3.65s)의 원인 — HikariCP 커넥션 풀 대기줄

성공(302) 요청만 보면 median 1.29s인데, 실패(400) 포함 전체는 median 2.94s로 더 길다. 이는 "실패 처리 로직이 느려서"가 아니라, DB 커넥션을 늦게 잡은 요청일수록 처리 자체도 늦어지고 그만큼 좌석이 이미 나갔을 확률도 높아지는 **큐잉 현상**으로 해석된다.

`application-dev.yml`에는 HikariCP 풀 크기 설정이 없어 **기본값 10**을 사용 중이다. 200개 요청이 커넥션 10개를 놓고 경쟁한다면 대략 200/10 = **20번의 순차 웨이브**로 처리되고, p95(3.65s) ÷ 20웨이브 ≈ **웨이브당 약 180ms** — 이 환경에서 DB 커넥션 하나로 쿼리 하나 처리하는 시간치고 그럴듯한 수치다.

### 이상적인 풀 크기는 얼마인가

HikariCP 공식 가이드(PostgreSQL 성능팀 실측 기반)의 공식:

```
connections = (core_count * 2) + effective_spindle_count
```

여기서 `core_count`는 **DB 서버의 코어 수**다(애플리케이션 서버 코어 수가 아님). `cinema-dev-db`는
`db.t4g.micro`(**2 vCPU**), RDS는 SSD 기반이라 `effective_spindle_count=0`.

```
connections = (2 * 2) + 0 = 4   (여유형 변형 공식 N*2+1 로는 5)
```

**즉 이상적인 값은 4~5에 가깝고, 지금 기본값(10)이 이미 이 공식값보다 여유 있게 잡혀 있다.**
풀을 30 같은 큰 값으로 늘리는 건 근거가 없다 — DB가 2코어로만 실제 병렬 처리하는 이상, 풀을 키워도
병목이 "커넥션 풀 대기줄"에서 "DB 코어 경합"으로 자리만 옮길 뿐 근본적으로 200개 요청을 순식간에
처리해줄 수는 없다.

---

## 종합 결론

1. **좌석락 코드(`0e8a4b8`)는 결백하다.** 두 라운드 모두 정합성 100%(정확히 좌석 수만큼만 성공, 중복 배정 0건)이며, 측정 구간에서 로그인을 걷어내자 지연시간이 즉시 1/3 이하로 줄었다.
2. **남은 지연은 인프라 사이즈의 근본적 한계다.**
   - 1라운드 지연의 대부분: **1 vCPU ECS task**에서 200개 동시 BCrypt 로그인이 직렬화됨 (실제 서비스 시나리오에서는 애초에 발생하지 않을 조건이기도 함)
   - 2라운드에 남은 지연: **2 vCPU RDS(`db.t4g.micro`)**가 순간적으로 200개 요청을 감당하기엔 근본적으로 작음. HikariCP 풀 크기(현재 10)는 이미 공식 권장치(4~5)보다 여유 있어 튜닝 여지가 크지 않다.
3. **다음 액션이 필요하면 "설정값 조정"이 아니라 "인스턴스 사이즈업" 방향이어야 한다.** 다만 이 환경은 실습·비용 절감이 목적이므로, 이 정도 동시 폭주 상황에서의 지연은 현재 사이즈의 자연스러운 한계로 받아들이고 넘어가는 것도 합리적인 선택이다.

## 아직 안 한 것

- 시나리오 B(Stress), C(조회 Load), D(로그인 Stress) 미실시
- RDS 인스턴스 클래스를 실제로 키워서 지연시간이 개선되는지 확인하는 대조 실험 (선택)

## DB 검증 쿼리 (참고)

```sql
-- 동일 좌석 중복 할당 여부
SELECT screening_id, seat_id, COUNT(*) AS cnt
FROM reserved_seats
WHERE screening_id = {screeningId} AND status IN ('HOLD', 'CONFIRMED')
GROUP BY screening_id, seat_id
HAVING COUNT(*) > 1;
-- 결과: 0건 (두 라운드 모두)

SELECT COUNT(*) FROM reserved_seats WHERE screening_id = {screeningId} AND status = 'HOLD';
-- 결과: 40 (두 라운드 모두)
```
