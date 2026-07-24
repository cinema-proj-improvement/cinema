# 조회 트래픽 Load 테스트 결과 (시나리오 C)

- 작성일: 2026-07-24
- 스크립트: `k6/scenarios/c-browse-load.js` · 시드: `k6/fixtures/seed-bulk-movies.sql`/`cleanup-bulk-movies.sql`
- 상태: **원인 확정, 수정 전** — 대량 시드는 재검증용으로 아직 안 지움

## 배경

`MovieRepositoryImpl.titleContains()`가 `LOWER(title) LIKE LOWER('%keyword%')`로
변환되는 선행 와일드카드 쿼리라 인덱스를 못 탄다. dev DB에 영화가 3건뿐이라
안 드러나서, 가짜 영화를 늘려가며 언제부터 문제가 되는지 확인했다.

## 1단계 — 규모별 쿼리 비용 (`EXPLAIN ANALYZE`)

| 영화 수 | 필터(LIKE) 포함 쿼리 시간 |
|---|---|
| 1만 | 19.7ms |
| 10만 | 125ms |
| 30만 | **311ms** (실제 화면은 이 쿼리를 2번 호출) |

10만→30만 구간 증가율이 더 가팔라져(버퍼풀 압박 추정) 30만을 최종 규모로 확정.

## 2단계 — k6 (VU=100, 3m, 30만 건 상태)

```
http_req_failed 2.17%(40/1838) · http_req_duration p95 30.02s(임계값 500ms의 60배)
컨테이너 재기동 없음
```

500 에러가 검색과 무관한 4개 엔드포인트에서 골고루 발생했고, 실패 건 전부 응답
시간이 **정확히 30011~30014ms** — HikariCP `connectionTimeout` 기본값(30000ms,
미설정이라 기본값 적용)에 걸린 신호.

## 원인 — DB 커넥션 풀 고갈 (CPU 아님)

- CPU 최대 13.3% — CPU는 무관.
- DB Active가 Max(10)에 붙고 Pending 최대 87건, Acquisition Time도 같은 구간에 급등.
- Tomcat Busy는 최대 ~98(max 400에 여유) — 스레드가 바쁜 건 CPU 작업이 아니라
  `getConnection()` 대기 때문.

**결론**: 검색어 걸린 요청이 LIKE 풀스캔으로 커넥션을 오래 붙잡음 → 풀(10개) 고갈
→ 무관한 요청까지 전부 대기 → 30초 후 일괄 500. 순수 DB 풀 고갈이 원인.

**B와 차이**: B는 `/health`가 스레드풀 공유로 굶어서 컨테이너가 재기동됐지만,
`/health`는 DB를 안 건드려서 이번엔 계속 정상 응답 → 서버는 멀쩡해 보이는데 실제
요청만 30초씩 500 나는, 더 조용한 장애.

## 다음 액션

- [ ] `titleContains()` 쿼리 자체 수정 방향 결정
- [ ] 수정 후 같은 시드(30만 건)로 재실행해 개선폭 확인
- [ ] 재검증 끝나면 `cleanup-bulk-movies.sql`로 시드 정리
