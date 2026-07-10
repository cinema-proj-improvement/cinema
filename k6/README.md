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

## 실행

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

이후 200 VU가 5초 안에 램프업되며 각 VU가 정확히 1번씩 좌석 홀드를 시도한다
(좌석 40개 / VU 200개 → 좌석당 평균 5명 경쟁).

## 결과 해석

- `seat_hold_success` ≈ 40 이어야 함 (좌석 수와 정확히 일치해야 이상적)
- `seat_hold_conflict` ≈ 나머지 (SEAT_ALREADY_HELD, 정상적인 경합 실패)
- `seat_hold_unexpected_error` == 0 이어야 함 (302/400 외 응답 — 진짜 버그/장애 신호)
- `seat_hold_duration` p95 확인

콘솔 로그에 `[setup] 픽스처 생성 완료: screenId=..., screeningId=...`가 찍히니
DB 검증 시 이 값을 사용한다.

## 실행 후 DB 검증 (k6 메트릭만으로는 정합성을 확인할 수 없음)

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

## 정리 (cleanup)

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

시나리오 B(Stress), C(조회 Load), D(로그인 Stress)는 아직 스크립트화하지 않았다. A가 실제
환경에서 기대대로 동작하는 것을 확인한 뒤 이어서 작성하는 걸 권장한다 (B는 A의 fixture를 재사용해서
"hold 성공 시 즉시 cancel-hold로 반납"하는 루프로 지속적인 락 경합을 만드는 방식이 적합).
