-- =====================================================================
-- V4: 영화 제목 검색 인덱스를 함수 인덱스 -> 평범한 컬럼 인덱스로 교체
--
-- V3에서 추가한 LOWER(title) 함수 인덱스는 실측 결과 옵티마이저가 전혀
-- 사용하지 않았다(EXPLAIN ANALYZE, USE INDEX 힌트로도 확인). MySQL 공식
-- 문서 확인 결과 생성 컬럼(함수) 인덱스는 =, <, <=, >, >=, BETWEEN, IN()
-- 연산자에만 적용되고 LIKE는 지원 대상이 아니라, 애초에 이 조합 자체가
-- 불가능했다. (k6/load-test-results.md 참고)
--
-- 대신 MovieRepositoryImpl.titleContains()를 LOWER() 없이 title LIKE
-- 'keyword%'로 바꾸고, title 컬럼에 평범한 인덱스를 건다. 대소문자
-- 무관 검색은 movies 테이블의 콜레이션(utf8mb4_unicode_ci)에 위임한다 -
-- LOWER()를 안 쓰므로 이 콜레이션 특성이 그대로 살아서 index range scan이
-- 정상 동작한다.
-- =====================================================================

ALTER TABLE movies DROP INDEX idx_movie_title_lower;
ALTER TABLE movies ADD INDEX idx_movie_title (title);
