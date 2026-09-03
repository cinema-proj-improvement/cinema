-- 시나리오 C 대규모 조회 성능 검증용 임시 시드 스크립트
--
-- 목적: MovieRepositoryImpl.titleContains()가 QueryDSL containsIgnoreCase -> SQL
-- `LOWER(title) LIKE LOWER('%keyword%')`로 변환되는데, 이 패턴은 선행 와일드카드라
-- 인덱스를 못 타는 풀스캔이다. 지금 dev DB에는 영화가 3건뿐이라 이 비용이 드러나지
-- 않으므로, 30만 건을 넣어 실제로 문제가 되는지 확인한다.
--
-- 단계별 실측(EXPLAIN ANALYZE):
--   1만 건  -> 테이블 스캔 13.1ms / 필터 포함 19.7ms
--   10만 건 -> 테이블 스캔 58.3ms / 필터 포함 125ms
-- 두 지점의 증가폭으로 계산한 행당 한계비용은 약 1.17us/행 (필터 함수 평가 비용이
-- 거의 정확히 선형으로 증가하는 게 실제 병목으로 확인됨). 이 추세로 30만 건을
-- 추정하면 쿼리 1회에 약 350~400ms - 이번에도 EXPLAIN ANALYZE로 재실측해서 확인한다.
--
-- 주의:
--   - Flyway 마이그레이션이 아니다. dev DB에 mysql 클라이언트로 수동 실행하고,
--     k6 시나리오 C 실행이 끝나면 반드시 cleanup-bulk-movies.sql로 지운다.
--   - title을 'K6-PERF-MOVIE-'로 시작하게 만들어서, 운영 데이터와 절대 안 섞이고
--     cleanup 시 이 접두사 하나로 정확히 걸러낼 수 있게 한다.
--   - status를 UPCOMING/NOW_SHOWING로만 넣는다 - MovieRepositoryImpl.userVisibleCondition()
--     이 이 두 상태만 사용자 목록(/movies)에 노출하기 때문에, 다른 값을 넣으면
--     쿼리 조건에서 걸러져서 풀스캔 대상에 안 잡히고 아무 의미가 없다.
--   - movie_genres/movie_screening_types에는 안 넣는다 - 사용자 목록/상세 조회
--     쿼리(findUserMovies, findUserMovieById)가 이 두 테이블을 참조하지 않으므로
--     시드할 필요가 없다 (필요해지면 이 파일에 INSERT 블록 추가).
--   - 실행 전 아래 "실행 전 확인"으로 중복 실행 여부를 꼭 확인한다 (재실행하면 30만 건이
--     더 늘어난다 - 이 스크립트에는 자체 중복 방지 로직이 없다).

-- 실행 전 확인 (0이 아니면 이미 시드된 상태 - 먼저 cleanup 후 재실행)
SELECT COUNT(*) AS existing_seed_rows FROM movies WHERE title LIKE 'K6-PERF-MOVIE-%';

-- 000000~299999(총 300,000개)를 생성한다. 10만 건짜리 스크립트와 똑같은 5자리
-- (00000~99999) CROSS JOIN에, 0/1/2 세 값짜리 "10만 단위" 선택자(h)를 하나 더
-- 곱해서 h*100000 + (00000~99999) 로 정확히 30만 개를 만든다 - 100만 개를 만들어놓고
-- 앞에서 30만 개만 자르는 것보다 낭비가 없다. 재귀 CTE 안 쓰는 이유는 MySQL 기본
-- cte_max_recursion_depth(1000)에 걸리지 않기 위함
INSERT INTO movies (title, running_time_minutes, release_date, end_date,
                    age_rating, synopsis, avg_score, advance_reservation_rate,
                    status, created_at, updated_at)
SELECT
    CONCAT(
        'K6-PERF-MOVIE-', LPAD(seq.n, 6, '0'),
        -- 검색어('인터'/'라라', c-browse-load.js의 KEYWORDS와 동일)가 가끔 걸리도록
        -- 일부 제목에만 섞어 넣는다 (전부 매칭되면 비현실적인 셀렉티비티가 됨)
        CASE
            WHEN seq.n % 500 = 0 THEN ' 인터 특별전'
            WHEN seq.n % 733 = 0 THEN ' 라라 리마스터'
            ELSE ''
        END
    ) AS title,
    80 + (seq.n % 100) AS running_time_minutes,
    DATE_ADD('2025-01-01', INTERVAL (seq.n % 730) DAY) AS release_date,
    DATE_ADD('2025-01-01', INTERVAL (seq.n % 730) + 30 DAY) AS end_date,
    ELT(1 + (seq.n % 4), 'ALL', 'AGE_12', 'AGE_15', 'AGE_19') AS age_rating,
    'k6 성능 테스트용 대량 시드 데이터입니다. 실제 콘텐츠가 아닙니다.' AS synopsis,
    ROUND(RAND() * 5, 1) AS avg_score,
    ROUND(RAND() * 100, 1) AS advance_reservation_rate,
    IF(seq.n % 2 = 0, 'NOW_SHOWING', 'UPCOMING') AS status,
    NOW(6), NOW(6)
FROM (
    SELECT h.n * 100000 + e.n * 10000 + d.n * 1000 + c.n * 100 + b.n * 10 + a.n AS n
    FROM (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2) h
    CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
    CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
    CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
    CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
    CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) e
) seq;

-- 실행 후 확인 (300000이어야 정상)
SELECT COUNT(*) AS seeded_rows FROM movies WHERE title LIKE 'K6-PERF-MOVIE-%';
