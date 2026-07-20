# 좌석 선점 스파이크 테스트 결과 (모니터링 재구축 후 재실행)

- 작성일: 2026-07-20
- 대상 브랜치: `test/add-k6-performance-test`
- 대상 커밋: `0893854`
- 테스트 스크립트: `k6/scenarios/a-seat-hold-spike.js`
- 테스트 환경: dev ECS Fargate + RDS MySQL `db.t4g.micro`(2 vCPU, 1GiB) + Redis + OTel Java agent/Collector 사이드카 → Prometheus → Grafana

> 이전 스파이크 결과(`refactor/seat-lock-redis` 브랜치, 2026-07-10 실시분)는 CloudWatch를 직접 조회하며 사후분석한 결과였다.
> 이번엔 OTel + Prometheus + Grafana 모니터링 스택이 갖춰진 상태에서 같은 스크립트를 재실행하며 진행 중이다.
> 아직 원인 조사 단계이며, 조치(HikariCP 풀 크기 조정 등) 후 재테스트 결과는 추후 이 문서에 트러블슈팅 형식으로 추가할 예정.

## 테스트 시나리오

| 항목 | 내용 |
|---|---|
| 도구 | k6 (`per-vu-iterations` executor, VU당 정확히 1회 실행) |
| 대상 | `POST /reservations/holds` (좌석 선점) |
| 시나리오 | 40석 상영에 **1000 VU**가 거의 동시에 좌석 홀드 시도 (좌석당 평균 25명 경쟁) |
| 측정 구간 | 로그인·CSRF·좌석 목록 조회는 `setup()`에서 미리 끝내고, `default()`는 홀드 POST 1회만 측정 |
| 픽스처 | k6 `setup()`이 Admin API로 테스트 전용 40석 상영관+상영을 매 실행마다 새로 생성 |

## 결과 (2026-07-20 13:25 실행)

```
checks_total.......: 1000    141.707611/s
checks_succeeded...: 100.00% 1000 out of 1000
checks_failed......: 0.00%   0 out of 1000

seat_hold_conflict.............: 960    136.039306/s
seat_hold_duration.............: avg=3.5s     min=325.85ms med=3.66s    max=5.76s p(90)=5.38s p(95)=5.59s
seat_hold_success..............: 40     5.668304/s
seat_hold_unexpected_error.....: 0      0/s

http_req_duration..............: avg=3.46s    min=17.2ms   med=3.65s    max=5.76s p(90)=5.37s p(95)=5.58s
  { expected_response:true }...: avg=790.29ms min=17.2ms   med=909.95ms max=2.05s p(90)=1.59s p(95)=1.66s
http_req_failed................: 94.86% 960 out of 1012
http_reqs......................: 1012   143.408102/s

iterations......................: 1000   141.707611/s
running (0m07.1s), 0000/1000 VUs, 1000 complete and 0 interrupted iterations
spike ✓ [===================] 1000 VUs  0m05.8s/2m0s  1000/1000 iters, 1 per VU
```

- **정합성: 통과** — `seat_hold_success=40`, `seat_hold_conflict=960`, `unexpected_error=0`. 1000 VU가 40석을 놓고 경쟁했는데 정확히 좌석 수만큼만 성공.
- **전체 실행 시간: 7.1초** (`per-vu-iterations`라 1000 VU가 병렬로 동시 실행되므로 총 소요시간 자체는 짧음). `seat_hold_duration` threshold(`p95<800ms`)는 미달.

## 원인 조사

### 1. 서버 자원(CPU/메모리/GC)은 문제 없음

같은 시각 Grafana 지표:

| 메트릭 | 값 | 비고 |
|---|---|---|
| CPU Utilization (JVM) | max 11.4%, mean 1.45% | 여유 충분 |
| Heap / Old Gen | Old Gen 최대 119MiB (IHOP 임계치 1.01GiB) | 누수 신호 없음 |
| HTTP Error Rate (4xx/5xx 중 5xx) | 0% | 애플리케이션 에러 없음 |
| DB Connection Pool Max Size | 10 (HikariCP 기본값, 커스텀 설정 없음) | |

CPU·힙·GC 전부 여유 있는 걸 보면 "처리 속도" 자체는 문제가 아니다.

### 2. 개별 요청 체감 지연은 심각 — 성공보다 실패가 더 느림

- 전체 `http_req_duration`: avg 3.46s, **p95 5.58s**
- 성공(302)만: avg 790ms, **p95 1.66s** — 훨씬 빠름

실패(400, `SEAT_ALREADY_HELD`)가 성공보다 느리다는 건 "실패 로직이 느려서"가 아니라, **대기열에서 늦게 처리된 요청일수록 이미 다른 요청이 좌석을 채간 뒤라 패배할 확률이 높다는 선택 편향**으로 해석된다. 즉 진짜 원인은 대기열 자체다.

`SeatHoldFacade.holdSeats()`(`SeatHoldFacade.java:24-38`)를 보면 Redis 락 승패가 갈리기 **전에** 모든 요청이 `validateBookable()`로 DB 읽기 트랜잭션을 한 번씩 거친다. 1000개 요청이 거의 동시에 몰리면:

1. Tomcat 워커 스레드(Spring Boot 기본 `max=200`)에서 1차 대기 — 200개까지만 앱 코드 진입, 나머지는 accept 큐 대기
2. 그 200개 중에서도 DB 커넥션(HikariCP 기본 `max=10`)에서 2차 대기

40석에 1000명(25배 오버서브스크립션)이 몰리는 구조라, 이 이중 대기열에서 수 초씩 지연이 발생하는 것으로 보인다.

### 3. (참고) Grafana 대시보드의 "약 4분 지속" 표시는 실제 서버 이벤트 길이와 다른 것으로 추정

이번 실행에서 `DB Connection Acquisition Time` 패널은 13:26:00~13:30:00 약 **4분간** 평평하게 상승한 뒤(p95 mean 316ms/max 723ms, p99 mean 659ms/max 1.56s) 뚝 떨어지는 사각형 형태로 나타났다. `HTTP Latency`, `GC Frequency` 패널도 같은 구간에서 유사하게 반응했다.

그런데 k6 자체 실행 시간은 7.1초였고, 과거 두 차례 재실행(2026-07-19 22:09, 23:44)에서도 k6는 매번 6~7초 만에 끝났는데 Grafana 상의 "영향 구간"은 매번 공교롭게 약 4분으로 나타났다. 서버 사이드 부작용의 실제 길이가 세 번 모두 우연히 4분으로 일치할 가능성은 낮다.

같은 화면의 `Tomcat Thread Pool` 패널(200→40→10으로 **약 90초에 걸쳐 삼각형으로 자연 감소** — Tomcat 기본 idle timeout과 부합하는 실제 물리적 감쇠 형태)과 모양이 다르다는 것도 방증이다. 같은 사건의 여파라면 감쇠 곡선 모양이 비슷해야 하는데, `DB Connection Acquisition Time` 쪽만 사각형(뚝 오르고 뚝 떨어짐)인 건 실제 현상이라기보단 **Grafana 패널 쿼리가 넓은 range window(추정 5분)로 스무딩하면서 수 초짜리 이벤트를 화면상 4분짜리로 "번짐" 처리하는 아티팩트**일 가능성이 높다.

`seat-hold.minutes=5`(DB hold 만료), `seat-hold.lock-ttl-seconds=15`(Redis 락 TTL, `application.yml:19-21`) 둘 다 4분과 맞아떨어지지 않아 "홀드 TTL 때문에 4분간 뭔가 계속 돈다"는 가설도 기각했다.

→ 애플리케이션 문제는 아니라고 보고 있으나, Grafana 쿼리의 range window를 스크레이프 주기에 맞게 좁혀서 별도로 검증 필요.

### 4. (참고) k6 지연시간과 Grafana 지연시간이 서로 다른 구간을 재고 있음 — 직접 비교 금지

- k6 `http_req_duration`(med 3.65s, p90 5.37s, p95 5.58s)은 **로컬 PC 기준 전체 왕복 시간**(인터넷 → ALB → OS accept 큐 → Tomcat 워커 배정 → 앱 로직/DB → 응답)이다.
- Grafana `HTTP Latency` 패널(p95 mean 38.5ms/max 90.3ms, p99 mean 51.9ms/max 157ms)은 **OTel 계측이 시작되는 시점(Tomcat이 요청을 이미 받아 처리를 시작한 순간)부터만** 잰다. 인터넷 왕복, ALB 처리, TCP 연결 수립, OS 소켓 accept 큐 대기 시간은 포함되지 않는다.
- 이번 시나리오는 `per-vu-iterations`라 1000개 VU가 매번 **새 TCP 연결**을 맺으므로, 1000개의 동시 신규 연결이 ALB/OS 단에 몰리는 구간이 존재하는데 이건 현재 어떤 패널에도 안 잡힌다. k6(5.5초대)와 Grafana 서버 사이드(백여 ms대) 사이의 큰 간극 상당 부분이 여기 있을 가능성이 높다.
- 추가로 `HTTP Latency` p99 최댓값(157ms)이 `DB Connection Acquisition Time` p99 최댓값(1.56s)보다 작다는 모순도 있다 — 커넥션 대기 시간은 논리적으로 전체 처리 시간의 일부여야 하므로, 두 패널이 서로 다른 요청 집합/윈도우를 집계하고 있을 가능성이 있다. 3번 항목의 range window 검증과 함께 확인 필요.
- **결론**: 실사용자 체감 지연은 k6 숫자(중앙값 3.65s, p90 5.37s)가 더 신뢰도 높은 근거다. Grafana 서버 사이드 패널들은 측정 구간이 좁고 패널 간 정합성도 아직 안 맞아서, 지금 시점에는 "심각한 지연이 있다"는 방향성만 보조 근거로 쓰고 절대값은 그대로 인용하지 않는다.

## 종합 결론 (조사 단계)

1. **좌석락 코드의 정합성은 문제 없음** — 매 실행마다 정확히 좌석 수만큼만 성공, 중복 배정 0건.
2. **서버 자원(CPU/메모리/GC)도 문제 없음** — 전부 여유 있게 관측됨.
3. **개별 요청 지연(p95 5.5초대)이 진짜 문제** — Tomcat 스레드 풀(max 200)과 HikariCP 커넥션 풀(max 10)의 이중 대기열이 유력한 원인. 1000 VU가 40석(25배 오버서브스크립션)에 몰리는 이 테스트 조건 자체가 가혹하긴 하지만, 그걸 감안해도 대기열 큐잉 시간이 크다.
4. Grafana 대시보드에 나타나는 "4분 지속" 표시는 별도의 모니터링 쿼리 이슈로 분리해서 다룬다 (애플리케이션 성능 문제와 무관).

## 다음 액션 (TODO)

- [ ] HikariCP `maximum-pool-size`를 기본값 10 → **20**으로 올려 재테스트 (`application-dev.yml`) — RDS `db.t4g.micro` 2vCPU/1GiB 스펙 대비 값은 실측 기반으로 단계적으로 늘려가며 CloudWatch RDS CPU/커넥션 지표와 함께 확인
- [ ] 재테스트 결과가 개선되면 이 문서에 트러블슈팅 형식(조치 → 재검증 결과)으로 추가
- [ ] Grafana `DB Connection Acquisition Time` 등 패널의 쿼리 range window 점검 (스파이크성 이벤트가 실제보다 길게 표시되는 문제)
- [ ] 시나리오 B(Stress), C(조회 Load) 재실행 및 결과 반영

## DB 검증 쿼리 (참고)

```sql
-- 동일 좌석 중복 할당 여부
SELECT screening_id, seat_id, COUNT(*) AS cnt
FROM reserved_seats
WHERE screening_id = {screeningId} AND status IN ('HOLD', 'CONFIRMED')
GROUP BY screening_id, seat_id
HAVING COUNT(*) > 1;
-- 결과: 0건

SELECT COUNT(*) FROM reserved_seats WHERE screening_id = {screeningId} AND status = 'HOLD';
-- 결과: 40
```
