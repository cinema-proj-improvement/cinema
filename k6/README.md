# k6 성능 테스트

## 왜 이런 구조인가

- `V2__seed_data.sql`(Flyway) 같은 영구 마이그레이션 파일에 부하테스트용 데이터를 넣지 않는다.
  Flyway 마이그레이션은 한 번 dev DB에 적용되면 체크섬이 고정되고, 모든 신규 환경(새 개발자, CI)에
  그대로 재현되는 "영구 기준 데이터"다. 부하테스트 픽스처(가짜 40석 상영관 등)는 이번 테스트
  세션에서만 필요한 임시 데이터이므로 여기 섞으면 안 된다.
- 대신 `setup()`이 이미 있는 관리자 화면(`/admin/screens/new`, `/admin/screenings/new`)을
  실제 브라우저가 하는 것과 동일한 폼 제출로 호출해서 픽스처를 만든다. DB에 직접 접근할 필요가
  없고, 테스트가 끝나면 아래 "정리" 섹션의 SQL로 지우면 dev DB에 흔적이 남지 않는다.

## 사전 준비

1. **local(H2) 프로필로 테스트하지 말 것.** `dev` 프로필(MySQL 실 인스턴스)로 뜬 앱을 대상으로 한다.
2. 관리자 계정: `admin@test.com` / `1234` (DataInitialize가 항상 생성).
3. **현재 `NOW_SHOWING` 상태이고 `TWO_D` 상영 타입을 지원하는 영화의 `movieId`**를 하나 확보한다.
   - Flyway 시드(`V2__seed_data.sql`)의 영화들은 `release_date`/`end_date`가 2026년 2~3월로 박혀 있어서,
     실제 실행 시점의 날짜에 따라 이미 `ENDED`로 전이됐을 수 있다. `/admin/movies`에서 상태를 직접
     확인하거나, 상태 무관하게 테스트용 영화를 하나 새로 등록해서 써도 된다.
4. k6 설치: `brew install k6`

## 시나리오 A — Spike Test (좌석 선점, 순간 폭증)

정확히 좌석 수만큼만 성공하고 중복 배정이 없는지, 즉 **정합성**을 확인하는 테스트.
40석에 200 VU가 거의 동시에 홀드를 시도한다 (부하량 고정, 순간을 본다).

```bash
k6 run k6/scenarios/a-seat-hold-spike.js \
  -e BASE_URL=https://<dev-서버-주소> \
  -e MOVIE_ID=1 \
  -e ADMIN_USERNAME=admin@test.com -e ADMIN_PASSWORD=1234 \
  -e TEST_USERNAME=user@test.com -e TEST_PASSWORD=1234
```

`setup()` 단계에서 자동으로:
1. 관리자로 로그인
2. `K6-PERF-SCREEN-<timestamp>` 이름의 40석(5행×8열) 상영관 생성
3. 그 상영관 + `MOVIE_ID` + 지금부터 3시간 뒤 시작으로 상영 1건 생성
4. 생성된 `screeningId`의 선점 가능 좌석 목록 조회

각 VU가 `setup()`에서 미리 발급받은 세션을 공유해서 정확히 1번씩 좌석 홀드를 시도한다
(좌석 40개 / VU 200개 → 좌석당 평균 5명 경쟁). 로그인은 측정 구간 밖(`setup()`)에서 한 번만
이뤄지므로, 측정되는 지연시간은 순수하게 홀드 요청 자체의 지연이다.

### 결과 해석 (A)

- `seat_hold_success` ≈ 40 이어야 함 (좌석 수와 정확히 일치해야 이상적)
- `seat_hold_conflict` ≈ 나머지 (SEAT_ALREADY_HELD, 정상적인 경합 실패)
- `seat_hold_unexpected_error` == 0 이어야 함 (302/400 외 응답 — 진짜 버그/장애 신호)
- `seat_hold_duration` p95 확인

콘솔 로그에 `[setup] 픽스처 생성 완료: screenId=..., screeningId=...`가 찍히니
DB 검증 시 이 값을 사용한다.

### 실행 후 DB 검증 (k6 메트릭만으로는 정합성을 확인할 수 없음)

```sql
-- 1) 동일 좌석이 2명 이상에게 동시 할당됐는지 (0건이어야 정상)
SELECT screening_id, seat_id, COUNT(*) AS cnt
FROM reserved_seats
WHERE screening_id = {screeningId} AND status IN ('HOLD', 'CONFIRMED')
GROUP BY screening_id, seat_id
HAVING COUNT(*) > 1;

-- 2) HOLD 성공 건수가 정확히 좌석 수(40)와 일치하는지
SELECT COUNT(*) FROM reserved_seats
WHERE screening_id = {screeningId} AND status = 'HOLD';
```

## 시나리오 B — Stress Test (좌석 선점, 지속 부하 램프업)

A가 "순간에 정합성이 깨지는가"를 봤다면, B는 "요청률을 계단식으로 올렸을 때 어디서부터
무너지는가(처리량 한계)"를 본다. 좌석이 40개뿐이라 그냥 부으면 금방 매진되어버리니,
**성공한 홀드는 바로 `cancel-hold`로 반납**해서 같은 40석으로 계속 경합 상태를 유지한다.

```bash
k6 run k6/scenarios/b-seat-hold-stress.js \
  -e BASE_URL=https://<dev-서버-주소> \
  -e MOVIE_ID=1
```

rps를 `100 → 200 → 400 → 800 → 1500`으로 단계별 30초씩 유지하며 올린다 (총 ~3분 20초).
A와 마찬가지로 로그인/CSRF/좌석목록은 `setup()`에서 미리 끝낸다.

> 처음엔 5~100rps로 낮게 시작했는데 100rps까지도 전혀 안 무너져서(p95 217ms, 에러 0건)
> 상한선을 1500rps까지 올렸다. 각 단계를 30초씩 유지하는 이유는 `db.t4g.micro`가 버스터블
> 인스턴스라 CPU 크레딧이 바닥나면 갑자기 성능이 꺾이는 특성이 있는데, 단계가 너무 짧으면
> 이런 "크레딧 고갈형 절벽"을 놓칠 수 있어서다.

### 결과 해석 (B)

- `stress_hold_success` / `stress_hold_conflict` — 정상적인 경합(거부)은 `stress_hold_conflict`로 잡힘
- `stress_hold_unexpected_error` — 5xx, 타임아웃 등 진짜 장애 신호. 한계점을 찾는 게 목적이라
  초반 에러 몇 건에 바로 멈추지 않도록 넉넉한 상한선(500건)에서만 `abortOnFail`로 중단된다
- `stress_cancel_failed` — 반납 실패 건수 (참고용, 홀드 처리량 판단에는 영향 안 줌)
- `stress_hold_duration` — 어느 rps 단계부터 급격히 늘어나는지가 핵심. k6 콘솔 로그는 시간순으로 나오니,
  `--out json=result.json`으로 저장해서 타임스탬프별로 쪼개보면 어느 단계(rps)에서 꺾였는지 더 정확히 볼 수 있다.
- 이 환경(1 vCPU ECS + `db.t4g.micro` RDS)은 시나리오 A 조사 결과 이미 저용량으로 확인됐으므로,
  꽤 낮은 rps 구간에서부터 한계 징후가 나타날 가능성이 높다 — 나쁜 결과가 아니라 이 사이즈의 실측 한계를
  숫자로 남기는 게 목적이다.

## 시나리오 C — Load Test (조회 트래픽, 평시 부하)

A/B(순간 폭증·지속 스트레스)와 달리 **정상 범위의 트래픽에서 SLA를 만족하는지**가 목적이다.
영화 목록/상세, 예매 화면 진입, 스케줄 조회처럼 상시 발생하는 조회 흐름을 재현한다.

```bash
k6 run k6/scenarios/c-browse-load.js \
  -e BASE_URL=https://<dev-서버-주소> \
  -e VU_COUNT=100 -e DURATION=3m
```

**"동시 사용자 수"를 그대로 rps로 착각하면 안 된다.** 실제 사용자는 페이지 사이사이 생각할
시간이 있으므로, 이 스크립트는 각 액션(목록→상세→예매화면→스케줄조회) 사이에 1~5초의
think-time(`sleep`)을 넣어서 VU 수를 올려도 실질 rps는 낮게 유지되도록 설계했다. think-time
없이 VU_COUNT만 높이면 "평시 트래픽 재현"이 아니라 또 다른 스트레스 테스트가 되어버려서
[`docs/healthcheck-actuator-port-separation.md`](../docs/healthcheck-actuator-port-separation.md)에서
다루는 헬스체크 기아 문제(시나리오 B에서 확인, 의도적으로 남겨둔 동작)를 다시 건드릴 수 있다.

대상 엔드포인트가 전부 GET이라 CSRF는 필요 없다. 로그인은 A/B와 동일하게 `setup()`에서
한 번만 하고 세션을 재사용한다. 영화 id는 하드코딩하지 않고 `/movies` 응답 HTML에서 그때그때
파싱해서 쓰므로, dev DB에 어떤 영화가 있든(테스트로 만든 것 포함) 알아서 탐색한다.

### 결과 해석 (C)

- `browse_unexpected_error` — 0이어야 함 (200 외 응답)
- `browse_movie_list_duration` / `browse_movie_detail_duration` / `browse_reservation_page_duration` /
  `browse_schedule_duration` — 엔드포인트별 지연시간. 어느 화면이 유독 느린지 구분 가능
- `http_req_duration` p95 < 500ms 기준으로 threshold 설정해둠

### 정리 (cleanup) — 필요 없음

A/B와 달리 상영관/상영 같은 픽스처를 만들지 않고 기존 DB 데이터를 조회만 한다. `setup()`에서
발급받는 로그인 세션 외에는 쓰기 작업이 없어 dev DB에 흔적이 남지 않는다.

## 정리 (cleanup, 시나리오 A/B)

관리자 화면에는 상영관 삭제 기능이 없다(물리적 자산으로 취급되어 등록/수정만 가능).
상영(screening)은 `SCHEDULED` 상태일 때만 관리자 화면에서 삭제 가능하고, 이번 테스트로 만든
상영은 이미 예약이 붙어 있어 그 경로로는 못 지운다. 테스트 후에는 SQL로 직접 정리한다:

```sql
DELETE FROM reserved_seats WHERE screening_id = {screeningId};
DELETE FROM reservations WHERE screening_id = {screeningId};
DELETE FROM screenings WHERE id = {screeningId};
DELETE FROM seats WHERE screen_id = {screenId};
DELETE FROM screens WHERE id = {screenId};
```

반복 테스트를 자주 돌린다면 이 정리 SQL도 스크립트로 만들어 둘 수 있다 (원하면 추가로 작성).

## 문제 해결

- `CSRF 토큰을 찾을 수 없습니다` 에러: 로그인이 실패했거나(비밀번호 오타), 권한이 없어 다른
  페이지(에러 페이지 등)가 내려왔을 가능성. `ADMIN_USERNAME`/`TEST_USERNAME` 값 확인.
- `상영 생성 실패`: `MOVIE_ID`가 `TWO_D`를 지원하지 않거나, 이미 `ENDED`거나, `startAt`이
  영화의 `release_date`~`end_date` 범위 밖일 수 있음. 다른 영화로 시도.
- `seat_hold_unexpected_error`가 0이 아님: 콘솔에 찍힌 status/body를 보고 원인 파악. 흔한 원인은
  세션 만료(테스트가 너무 오래 걸림) 또는 `EnvironmentPolicy.maxReservationCount` 관련 검증.

## 다음 단계

시나리오 D(로그인 Stress)는 아직 스크립트화하지 않았다.
