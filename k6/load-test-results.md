# 조회 트래픽 Load 테스트 결과 (시나리오 C)

- 작성일: 2026-07-24
- 스크립트: `k6/scenarios/c-browse-load.js` · 시드: `k6/fixtures/seed-bulk-movies.sql`(30만 건, 원인 확정용) / `seed-realistic-movies.sql`(30건, 최종 검증용) / `cleanup-bulk-movies.sql`
- 상태: **완료** — 원인 확정 → 수정 → 현실 규모 재검증까지 마무리

## 배경

`MovieRepositoryImpl.titleContains()`가 `LOWER(title) LIKE LOWER('%keyword%')`로 변환되는 선행
와일드카드 쿼리라 인덱스를 못 탄다. dev DB에 영화가 3건뿐이라 안 드러나서, 가짜 영화를
늘려가며 언제부터 문제가 되는지 확인했다.

## 1단계 — 규모별 쿼리 비용 (`EXPLAIN ANALYZE`)

| 영화 수 | 필터(LIKE) 포함 쿼리 시간 |
|---|---|
| 1만 | 19.7ms |
| 10만 | 125ms |
| 30만 | **311ms** (실제 화면은 이 쿼리를 2번 호출) |

30만을 최종 검증 규모로 확정.

## 2단계 — k6(수정 전, 30만 건, VU=100·3m)

```
http_req_failed 2.17%(40/1838) · http_req_duration p95 30.02s(임계값 500ms의 60배)
```

500 에러가 검색과 무관한 4개 엔드포인트에서 골고루 발생, 실패 건 응답 시간이 전부
**정확히 30011~30014ms** — HikariCP `connectionTimeout` 기본값(30000ms) 타임아웃 신호.

**원인**: 검색어 걸린 요청이 `LIKE '%kw%'` 풀스캔으로 DB 커넥션을 오래 붙잡음 → 풀(10개)
고갈 → 무관한 요청까지 대기 → 30초 후 일괄 500. CPU는 최대 13%로 무관했고, Tomcat
스레드는 커넥션을 기다리며 블로킹된 것뿐이었다(스레드 풀 자체는 안 막힘). `/health`는
DB를 안 건드려서 헬스체크는 계속 통과 — 시나리오 B(스레드 풀 공유로 헬스체크까지
굶어 컨테이너 재기동)와 달리 겉보기엔 멀쩡한데 실제 요청만 500 나는, 더 조용한 장애.

## 3단계 — 수정 시도 1(실패): `LOWER(title)` 함수 인덱스

`LIKE 'keyword%'`(prefix만)로 좁히고 `LOWER(title)`에 함수 인덱스를 걸었으나, `EXPLAIN
ANALYZE`·`USE INDEX` 힌트로도 계속 풀스캔이 나왔다. MySQL 공식 문서 확인 결과 생성
컬럼(함수) 인덱스는 `=`,`<`,`<=`,`>`,`>=`,`BETWEEN`,`IN()`에만 적용되고 **`LIKE`는 지원
대상이 아니라서** 애초에 불가능한 조합이었다.

## 4단계 — 수정 시도 2(성공): 평범한 `title` 컬럼 인덱스

`LOWER()` 없이 `title LIKE 'keyword%'` + `title` 컬럼에 평범한 인덱스(`idx_movie_title`).
대소문자 무관 검색은 MySQL 컬럼 콜레이션(`utf8mb4_unicode_ci`)에 위임(dev/운영은 기존과
동일 동작, H2(local/test)만 대소문자 구분 — 실사용자 영향 없음). MySQL 공식 문서(Range
Optimization)에서 명시적으로 지원하는 패턴(BTREE 인덱스 + 와일드카드로 시작 안 하는
LIKE)이라 실측으로도 확인됨 — `EXPLAIN ANALYZE` 결과 `Index range scan`, **1.18ms**.

이 방식은 `LIKE 'keyword%'`(prefix)만 지원하므로 **"제목 중간 포함 검색"은 포기**한다는
트레이드오프가 있다. 이 UX를 유지하는 방법(MySQL FULLTEXT+ngram 파서, 또는 별도 검색
인프라 도입)도 검토했으나, 전자는 로컬(H2)·운영(MySQL) 간 동작 불일치와 구현 복잡도가,
후자는 별도 인프라 구축·운영 비용이 지금 규모에 비해 과하다고 판단해 제외하고, 구현이
가장 단순하고 로컬/운영 동작이 100% 일치하는 이 방식을 최종 선택했다.

## 5단계 — k6 재검증(30만 건, 수정 후)

- 1차 재실행: 테스트 도중 배포(태스크 교체)가 겹쳐 결과 오염, 재실행하기로 함(시나리오 B
  조건 4와 동일한 처리).
- 2차 재실행(깨끗한 상태): `http_req_failed 0%`, 500 에러 완전히 사라짐. 다만
  `http_req_duration p95 14.41s`로 여전히 느림.

**남은 지연의 원인**: 검색어 없는 목록 조회(6번 중 4번)는 `status IN ('UPCOMING',
'NOW_SHOWING')`만 조건인데, 시드 데이터가 이 두 상태로만 채워져(ENDED 없음) 30만 건
전부가 이 조건에 걸린다. `COUNT(*)`와 정렬이 사실상 전체 테이블을 훑어야 해서
(`EXPLAIN ANALYZE` 각각 205ms/386ms) 검색어 있는 경우보다 오히려 더 큰 병목이 됐다.

## 6단계 — 현실 규모 재검증 (최종)

"상영중+예정 영화가 실제로 30만 건일 리 없다(많아야 몇십 건)"는 판단 하에, 30만 건 시드를
지우고 **30건**(`seed-realistic-movies.sql`)으로 다시 시드 후 동일 k6 재실행.

```
checks_succeeded 100%(7718/7718) · http_req_failed 0%
http_req_duration p95 30.45ms · max 317.9ms
```

재실행(안정화 후) 기준 `HTTP Latency` 대시보드 스파이크도 사라짐. 현실 규모에서는
COUNT(*)/정렬 비용이 무시할 수준(기존 `idx_movie_status_release_date`
커버링 인덱스로 이미 충분)이라, `/movies`의 COUNT 쿼리는 **별도 최적화(캐싱 등) 없이 그대로
유지하기로 결정**했다.

## 핵심 수치 한눈에 보기

| 단계 | 데이터 규모 | http_req_duration p95 | http_req_failed |
|---|---|---|---|
| 수정 전 | 30만 건 | 30.02s | 2.17% |
| title 인덱스 수정 후 | 30만 건 | 14.41s(COUNT 병목 잔존) | 0% |
| 최종(현실 규모) | 30건 | **30.45ms** | 0% |
