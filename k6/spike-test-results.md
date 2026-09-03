# 좌석 선점 스파이크 테스트 결과 — 원인 조사 및 튜닝 기록

- 작성일: 2026-07-20
- 대상 브랜치: `test/add-k6-performance-test`
- 테스트 스크립트: `k6/scenarios/a-seat-hold-spike.js`
- 테스트 환경: dev ECS Fargate(앱 컨테이너 **1.5 vCPU / 3GB**, OTel Collector 사이드카 0.5 vCPU / 1GB — 모든 단계에서 고정) + RDS MySQL `db.t4g.micro`(2 vCPU / 1GiB) + Redis + OTel Java agent/Collector → Prometheus → Grafana
- 시나리오: 40석 상영에 **1000 VU**가 동시에 좌석 홀드 시도(`per-vu-iterations`, VU당 정확히 1회, 좌석당 평균 25명 경쟁 = **25배 오버서브스크립션**)
- 임계값: `seat_hold_duration p(95)<800ms` — 모든 단계에서 미달했지만, 애초에 25배 오버서브스크립션이라는 극단적 조건이라 절대 통과보다는 **단계별 개선 폭**을 보는 데 의미가 있다.

## 조사 개요

"서버 자원(CPU/메모리/GC)이나 처리 속도 자체는 문제없어 보이는데, 개별 요청 체감 지연은 심각하다"는 첫 관찰에서 출발해서, 병목을 DB 커넥션 풀 → Tomcat 스레드 풀 → (요청을 받는) 인스턴스 자체 수 순서로 하나씩 좁혀가며 튜닝했다. 각 단계는 "이전 결과에서 발견한 단서 → 원인 가설 → 조치 → 재테스트로 검증" 순서로 진행됐다.

## 단계별 기록

| 단계 | 조치 전 관찰 / 원인 판단 경위 | 조치 | 결과 (k6 클라이언트 기준) | 다음 단계로 이어진 단서 |
|---|---|---|---|---|
| **1. 아무것도 안 건드리고 실행** | (기준선) 아무 설정도 바꾸지 않은 상태에서 1000 VU 스파이크를 쏴서 문제가 실재하는지부터 확인 | 없음 (HikariCP `maximum-pool-size` 기본값 10, Tomcat `threads.max` 기본값 200, replica 1대) | `http_req_duration` p95 **4.88s** (성공만: avg 623ms / p95 1.32s) · 전체 처리시간(wall time) 6.1s · CPU 최대 11%대, 힙/GC 정상 | CPU·힙·GC는 전부 여유 있는데 개별 요청은 초 단위로 밀림 → 컴퓨팅 자원이 아니라 **대기열**에서 문제 발생 중이라 판단. `SeatHoldFacade.holdSeats()`가 Redis 락 전에 `validateBookable()`로 DB를 먼저 치는 구조라, 1000개 요청 전부가 DB 커넥션을 최소 1번씩 요구함 → `DB Connection Acquisition Time` 패널이 부하 구간에서 크게 튀는 것 확인, HikariCP 풀(기본 10)이 유력 용의자로 지목됨 |
| **2. HikariCP 풀 크기 확대** | DB Connection Acquisition Time이 부하 순간 정상 대비 수배~수십배 튐. RDS가 2vCPU라는 점을 고려해도 기본값 10은 협소해 보임 | `application-dev.yml`에 `spring.datasource.hikari.maximum-pool-size: 20` 추가 | `http_req_duration` p95 **5.53s** (오히려 소폭 증가, 오차범위) · 성공만: avg 687ms / p95 1.59s · 전체 처리시간 7.9s · **DB Connection Acquisition Time은 극적으로 개선**(p99 mean 659ms→46ms, max 1.56s→78ms, 약 15~20배) | DB 쪽 지표는 확실히 좋아졌는데 **정작 사용자 체감 p95는 안 바뀜** → "DB 커넥션 대기가 병목의 일부일 뿐, 주범이 아니다"로 결론. 같은 화면의 `Tomcat Thread Pool` 그래프가 여전히 기본 max(200)에 딱 붙어 있는 게 눈에 띔 → 다음 용의자를 Tomcat 워커 스레드로 전환 |
| **3. Tomcat 스레드 풀 확대** | 이 작업은 I/O-bound(DB/Redis 대기가 대부분, CPU 11%로 여유)라 코어 수보다 스레드 수를 늘리는 게 정석이라는 논의 후 결정. 1000개 요청이 거의 동시에 도착하는데 워커 스레드가 200개뿐이라 나머지는 accept 큐에서 대기 중이라 판단 | `application-dev.yml`에 `server.tomcat.threads.max: 400`, `accept-count: 200` 추가 | `http_req_duration` p95 **4.84s** (재실행 시 3.19s, 실행 간 편차 있음) · 성공만 p95 1.74s · `Tomcat Thread Pool` 최대 사용량 225로, 새 천장(400)엔 안 닿음 → **Tomcat 워커 스레드 자체는 더 이상 병목 아님 확인** · 대신 **DB Acquisition Time이 다시 악화**(p99 mean 46→85ms) — 더 많은 요청이 동시에 앱까지 들어와서 DB 풀(20)로 몰린 것 | 세부 타이밍(`blocked`/`connecting`/`waiting`)을 쪼개 재보니 연결 수립(`blocked`+`connecting`)은 ~200ms로 작고 **`waiting`(TTFB)이 전체(3초대)의 97% 이상** 차지 → 네트워크 연결 문제는 기각. 근데 Grafana `HTTP Latency`(서버가 요청 처리를 시작한 뒤부터 잰 시간)는 항상 수십~수백ms대로 작았음 → "TCP 연결은 됐고 서버 처리 자체도 빠른데, 그 사이 어딘가에 숨은 지연"이 있다고 판단. 처음엔 "ECS task가 1vCPU를 collector와 공유해서"로 추정했으나, 실제 task definition 확인 결과 앱 컨테이너는 **1.5vCPU 전용**이라 이 가설은 기각. 대신 Tomcat의 연결 수락 담당 스레드(acceptor)가 기본 1개뿐이라, 코어 수와 무관하게 **1000개 연결을 한 인스턴스가 순차적으로 받아야 하는 구조적 한계**로 재추정 → 근본적으로 "1대가 25배 부하를 혼자 감당"하는 구조 자체가 문제라고 보고 **replica 확장**으로 방향 전환 |
| **4. ECS app 컨테이너 replica 1 → 2대 확장** | Tomcat accept 병목이 CPU/스레드 설정이 아니라 "인스턴스 1대가 모든 연결을 받아야 하는 구조" 때문이라면, 인스턴스를 늘려 요청을 물리적으로 나눠 받는 게 근본 해법이라 판단. DB는 replica들이 공유하는 자원이므로, 총 커넥션 상한은 기존에 검증된 수준(~20)을 유지하기 위해 인스턴스당 풀 크기를 절반으로 조정 | ECS 서비스 desired count 1 → 2, `maximum-pool-size` 20 → **10**(인스턴스당, 총합 ~20 유지), Tomcat 스레드 설정은 유지(인스턴스별 독립 자원이라 부하가 반으로 나뉘면 자연히 여유 생김) | (배포 전환 중 실행된 1차 시도는 두 인스턴스 트래픽 분배가 불안정해 결과 제외) 배포 안정화 후 재실행: `http_req_duration` p95 **2.16s** (직전 단계 대비 약 **55% 개선**, 최초 기준선 대비 약 **56% 개선**) · 성공만: avg 445ms / p95 **1.07s** · 전체 처리시간 3.7s (6.5s → 거의 절반) · DB Acquisition Time p99 mean 178ms/max 458ms(약간 상승했지만 여전히 건강한 수준) · Grafana `HTTP Latency`가 이전보다 커짐(p99 max 1.97s) — 예전에 "안 보이던" 지연 구간이 줄면서 그만큼이 계측 가능한 구간 안으로 들어온 것으로 해석 | 여기까지가 이번 조사에서 다룬 범위. 남은 지연은 이제 서버 사이드 계측(OTel span)으로 직접 추적 가능한 상태라, 더 파고들 경우 여기서부터 이어가면 됨 |

## 핵심 수치 한눈에 보기

| 단계 | 설정 (풀 크기 / 스레드 max / replica) | p95 (client, `http_req_duration`) | 성공(302)만 p95 | 전체 처리시간(wall time) |
|---|---|---|---|---|
| 1. 기준선 | 10 / 200 / 1 | 4.88s | 1.32s | 6.1s |
| 2. DBCP↑ | 20 / 200 / 1 | 5.53s | 1.59s | 7.9s |
| 3. Thread↑ | 20 / 400 / 1 | 4.84s (재실행 3.19s) | 1.74s (재실행 0.55s) | 6.5s (재실행 4.0s) |
| 4. Replica↑ | 10×2 / 400 / 2 | **2.16s** | **1.07s** | **3.7s** |

정합성(같은 좌석 중복 배정 여부)은 모든 단계에서 문제없었다 — 매 실행마다 정확히 좌석 수(40)만큼만 성공, `unexpected_error=0`.

## 조사 중 확인한 부수적 사실 (참고)

- **Grafana 대시보드의 "약 4분 지속" 표시는 실제 서버 이벤트 길이와 무관한 것으로 추정된다.** k6 자체 실행 시간은 매번 4~8초였는데, `DB Connection Acquisition Time` 등 패널은 매번 공교롭게 약 4분짜리 사각형 plateau로 나타났다. 이벤트의 실제 심각도·길이와 무관하게 매번 폭이 똑같다는 건 Grafana 쿼리가 넓은 range window(추정 4~5분)로 스무딩하면서 수 초짜리 이벤트를 화면상 늘려 보여주는 아티팩트일 가능성이 높다. 애플리케이션 문제는 아니라고 보고 있으나, 별도로 쿼리 window 점검이 필요하다.
- **k6 클라이언트 지연시간과 Grafana 서버 사이드 지연시간은 서로 다른 구간을 잰다.** k6 `http_req_duration`은 로컬 PC 기준 전체 왕복 시간(네트워크 → ALB → Tomcat accept → 앱 처리 → 응답)이고, Grafana `HTTP Latency`는 OTel 계측이 시작되는 시점(Tomcat이 요청 처리를 시작한 뒤)부터만 잰다. 3단계 이전까지는 이 둘의 격차가 매우 컸는데(서버 사이드는 항상 수십~수백ms, k6는 초 단위), 4단계(replica 확장) 이후 격차가 크게 줄었다.
- **HTTP Error Rate(4xx) 패널에 높은 수치가 찍히는 건 정상이다.** 1000개 중 960개가 의도된 `SEAT_ALREADY_HELD`(400) 응답이라, 4xx 비율이 90%를 넘게 찍히는 게 정상 동작이다. k6의 `checks_succeeded=100%`, `unexpected_error=0`으로 실제 오류가 아님을 매 단계 확인했다.

## 아직 안 한 것 / 다음 액션

- [ ] 시나리오 B(Stress), C(조회 Load) 재실행 및 결과 반영
- [ ] Grafana 쿼리 range window 점검 (스파이크성 이벤트가 실제보다 길게 표시되는 문제)
- [ ] (선택) 4단계 이후 계측 가능해진 서버 사이드 지연(OTel span, p99 최대 1.97s)을 트레이스 레벨에서 더 파고들지 여부 판단

## DB 검증 쿼리 (참고)

```sql
-- 동일 좌석 중복 할당 여부
SELECT screening_id, seat_id, COUNT(*) AS cnt
FROM reserved_seats
WHERE screening_id = {screeningId} AND status IN ('HOLD', 'CONFIRMED')
GROUP BY screening_id, seat_id
HAVING COUNT(*) > 1;
-- 결과: 0건 (모든 단계)

SELECT COUNT(*) FROM reserved_seats WHERE screening_id = {screeningId} AND status = 'HOLD';
-- 결과: 40 (모든 단계)
```
