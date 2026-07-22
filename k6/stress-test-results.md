# 좌석 선점 스트레스 테스트 결과 (시나리오 B)

- 작성일: 2026-07-22
- 대상 브랜치: `test/add-k6-performance-test`
- 테스트 스크립트: `k6/scenarios/b-seat-hold-stress.js`
- 관련 문서: [`spike-test-results.md`](./spike-test-results.md) (시나리오 A, 좌석 정합성 + 튜닝 기록)

> **이 문서는 이전 기록을 초기화하고 다시 작성한 것이다.** 기존 내용(`refactor/seat-lock-redis`
> 브랜치, 2026-07-10 실행분)은 모니터링 스택(OTel + Prometheus + Grafana) 도입 이전에 얻은
> 결과였고, 그때 발견한 "높은 rps에서 ECS task가 SIGTERM으로 교체됨" 현상은 실제 처리 용량
> 한계가 아니라 **헬스체크(`/health`)가 비즈니스 요청과 같은 Tomcat 스레드풀/큐를 공유해서
> 굶은 것**이 원인이었다(결론 자체는 유효). 이 문제는 "헬스체크를 별도 포트로 분리"하는 방식이
> 아니라 요청 처리 자체의 근본 원인을 고치는 방향으로 가기로 결정된 상태이며(관련: 시나리오 A
> 튜닝 기록), 그 결정 이후 모니터링이 갖춰진 지금 환경에서 시나리오 B를 처음부터 다시 측정한다.

## 시나리오 개요

시나리오 A(spike)가 "순간 폭증에도 정합성이 깨지지 않는가"를 봤다면, B는 "요청률을 계단식으로
올렸을 때 어디서부터, 어떻게 무너지는가"를 본다. 40석을 hold → 즉시 `cancel-hold`로 반납하는
루프로 지속적인 락 경합을 유지하면서, `ramping-arrival-rate`로 목표 rps를 1 → 100 → 200 → 400
→ 800 → 1500까지 계단식으로 올린다(각 단계 5~30초, 총 3분 20초). 로그인/CSRF는 시나리오 A와
동일하게 `setup()`에서 미리 끝내고, 측정 구간은 순수하게 홀드+취소 사이클만 반복한다.

시나리오 A의 튜닝(DBCP↑ → Thread↑ → Replica↑)으로 "순간 폭증"에는 강해졌다는 걸 확인했지만,
그 튜닝이 "지속적인 고부하"에도 그대로 유효한지는 별도로 검증이 필요해서 B를 이어서 실행한다.
앞으로 replica 수 / HikariCP 풀 크기 / Tomcat 스레드 수 조합을 바꿔가며 반복 실행하고, 조건별로
아래에 기록을 이어붙인다.

---

## 조건 1 — replica=1 / DBCP=10 / thread=400 (첫 실행)

### 설정

| 항목 | 값 |
|---|---|
| 앱 컨테이너 | 1.5 vCPU / 3GB (전 조건 공통 고정값) |
| OTel Collector 사이드카 | 0.5 vCPU / 1GB |
| ECS replica | **1개** |
| Tomcat | `threads.max=400`, `min-spare=10`, `accept-count=200` |
| HikariCP | `maximum-pool-size=10` |
| RDS | `db.t4g.micro` (2 vCPU / 1GiB, 버스터블) |

> HikariCP 10은 원래 시나리오 A 마지막 단계에서 "replica 2대로 나눠 쓰는 걸 가정하고" 20에서
> 줄여둔 값이다. 이번 조건은 replica는 1대로 되돌렸지만 DBCP는 그 값을 그대로 둔 상태 —
> 즉 시나리오 A가 끝난 시점의 `application-dev.yml`을 손대지 않고 그대로 실행한 결과다.

### 결과 (k6 클라이언트 기준)

```
checks_total.......................: 95775   475.450036/s
checks_succeeded...................: 100.00% 95775 out of 95775

stress_hold_success.................: 10476  52.005373/s
stress_hold_conflict................: 85299  423.444663/s
stress_hold_unexpected_error........: 0      0/s
stress_hold_duration.................: avg=2.44s min=18.84ms med=3.3s max=5.52s p(90)=3.9s p(95)=4.04s

http_req_duration....................: avg=2.24s med=3.2s max=5.52s p(90)=3.86s p(95)=4.03s
  { expected_response:true }.........: avg=489.08ms med=113.5ms max=5.52s p(90)=1.42s p(95)=3.43s
http_req_failed......................: 80.27% 85299 out of 106263
http_reqs.............................: 106263 527.51498/s

dropped_iterations....................: 31227  155.018306/s
iterations.............................: 95775  475.450036/s
vus_max................................: 3000

running (3m21.4s), 95775 complete and 0 interrupted iterations
```

- `unexpected_error=0` — 정합성/예외 측면은 끝까지 깨지지 않았고, `abortOnFail` 임계값에
  걸리지도 않았다(이전 실행과 달리 중도 중단 없이 완주).
- `stress_hold_conflict` 비율(80.3%)은 40석짜리 경합 설계상 정상 동작이지 오류가 아니다.
- **`dropped_iterations`가 31,227건(전체 요청 스케줄의 약 25%)** — k6가 목표 rps를 계속
  올리려 했지만 서버 응답이 너무 느려져서(이전 요청에 VU가 묶여) 다음 이터레이션을 아예
  시작도 못 한 것. 실제 도달한 rps는 목표(최대 1500)에 한참 못 미쳤다는 신호.

### 관찰 (대시보드)

- **HTTP Request Rate**: 실측 RPS 최대 **421 req/s**(평균 105) — 목표 상한 1500은커녕 800rps
  구간에도 제대로 못 미쳤을 가능성이 높다.
- **Tomcat Thread Pool (Idle/Busy/Total)**: 약 21:01:30~21:03:30 구간, **Busy=400(=설정
  max) / Idle=0**이 약 2분간 그대로 유지됨 — 워커 스레드가 완전히 바닥남. 시나리오 A(1000VU
  스파이크, 최대 사용량 225/400)에서는 없었던 현상.
- **DB Connection Pool Status**: 같은 시간대에 활성 커넥션이 max(10)까지 채워짐.
  **DB Connection Pending Requests**에 짧게 2건씩 대기가 몇 차례 발생.
  **DB Connection Acquisition Time**: p95 mean 80.7ms/max 533ms, p99 mean 330ms/**max 1.90s**.
- **CPU Utilization (JVM Process)**: 평소 ~0.2% → 부하 구간 **최대 92.9%**까지 상승. 시나리오
  A(스파이크, 최대 11%대)와 확연히 다르다 — 지속 부하가 걸리니 CPU도 실제 병목 후보로 등장.
- **Heap / Old Gen**: 안정적, IHOP 임계치(45%, 1.01GiB) 근처도 안 감 — 메모리 누수 신호 없음.
- **GC 빈도/일시정지**: Minor GC 빈도는 부하 구간에서 최대 0.5 ops/s로 증가했지만, 정지시간
  자체는 p99 기준 98~99ms대로 부하 전후 거의 변화 없음 — GC는 병목이 아님.
- **HTTP Latency (서버 사이드, p95/p99)**: p95 mean 309ms/max 845ms, p99 mean 467ms/max
  1.84s — **k6가 잰 `stress_hold_duration`(p95 4.04s)보다 훨씬 작다.** 시나리오 A에서 확인한
  "서버가 요청 처리를 시작한 이후만 잰 시간과 클라이언트 왕복 시간 사이의 큰 gap"이 이번에도
  재현됨 — 스레드 풀이 바닥난 상태에서 "연결은 됐지만 처리할 워커 스레드를 못 받고 대기"하는
  구간이 이 gap의 상당 부분을 차지하는 것으로 추정.
- **HTTP Error Rate (4xx)**: 평균 33.2%, 최대 96.9% — 좌석 경합 설계상 정상.

### 종합

이번 조건(replica=1, DBCP=10)에서는 **1500rps라는 목표 상한까지 갈 필요도 없이, 실측 ~400rps
근처에서 Tomcat 스레드 풀 · DB 커넥션 풀 · CPU가 거의 동시에 한계에 부딪히는 지점**을 확인했다.
DBCP=10은 애초에 "replica 2대 전제"로 줄여둔 값을 replica 1대에 그대로 적용한 상태라 이 결과가
어느 정도는 예견된 것이지만, 그걸 감안해도 **지속 부하에서는 CPU까지 실제로 밀린다**는 건 시나리오
A에서는 보지 못했던 새로운 발견이다.

## 핵심 수치 한눈에 보기

| 조건 | 설정 (replica / DBCP / thread max) | 실측 최대 RPS | p95 (`stress_hold_duration`) | dropped_iterations | Tomcat 스레드 풀 포화 | CPU 최대 |
|---|---|---|---|---|---|---|
| 1. 최초 실행 | 1 / 10 / 400 | 421 req/s | 4.04s | 31,227 (25%) | 예 (~2분 지속) | 92.9% |

## 다음 조건 후보 (미정 — 실행하면서 채워나감)

- [ ] DBCP를 20으로 복원 (replica=1 유지) — DB 풀이 진짜 병목이었는지 분리 검증
- [ ] Tomcat `threads.max`를 400보다 더 올려보기 — 지속 부하에서도 스레드 풀 확장이 유효한지
- [ ] replica 2대로 확장(시나리오 A 최종 조건과 동일) — 지속 부하에서도 수평 확장이 스파이크와
      비슷한 폭으로 개선을 주는지 확인
- [ ] 위 항목 중 CPU 92.9%가 실제 상한으로 작용하는 조건이 있는지 확인 (그렇다면 vCPU 증설이
      다음 후보)
