-- =====================================================================
-- V3: 영화 제목 검색 함수 인덱스 추가
--
-- 배경(k6 시나리오 C, k6/load-test-results.md 참고): MovieRepositoryImpl.
-- titleContains()가 LOWER(title) LIKE LOWER('%keyword%')로 변환되는 선행
-- 와일드카드 검색이라 인덱스를 못 타는 풀스캔이었다. 30만 건 시드 데이터로
-- 실측한 결과 쿼리 1회 311ms, 실제 화면은 이걸 2번(목록+COUNT) 호출하고
-- DB 커넥션을 오래 붙잡아 HikariCP 풀 고갈(커넥션 획득 30초 타임아웃)까지
-- 유발했다.
--
-- 조치: titleContains()를 startsWithIgnoreCase(LIKE 'keyword%', 선행
-- 와일드카드 제거)로 좁히고, LOWER(title)에 대한 함수 인덱스를 추가해 이
-- 쿼리가 인덱스를 타게 한다. "제목 중간 포함 검색"은 더 이상 안 되는
-- 트레이드오프를 감수한 결정 - 대안(MySQL FULLTEXT+ngram, 별도 검색 인프라)
-- 대비 구현/운영 비용이 가장 낮다는 점을 근거로 선택했다.
--
-- MySQL 8.0.13+ 함수 기반 인덱스 문법 필요 (RDS 엔진 버전 확인 필요).
-- =====================================================================

ALTER TABLE movies ADD INDEX idx_movie_title_lower ((LOWER(title)));
