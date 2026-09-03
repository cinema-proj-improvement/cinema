-- 시나리오 C 최종 검증용 "현실적 규모" 시드 스크립트 (k6/load-test-results.md 참고)
--
-- 30만 건 테스트로 title 검색(LIKE '%keyword%' 풀스캔)의 근본 원인을 찾아
-- LIKE 'keyword%' + idx_movie_title 인덱스로 고쳤다. 그런데도 남아있던 지연은
-- "검색어 없는 목록 조회"의 COUNT(*)/정렬이 상영중+예정 영화 30만 건(=거의
-- 전체 테이블)을 다 훑어야 해서였다 - 이건 실제로는 있을 수 없는 규모다(상영중+
-- 예정 영화는 많아야 몇십 건). 그 가정을 실측으로 확인하기 위해, 30만 건 대신
-- 이번엔 30건만 시드해서 같은 k6 시나리오 C를 다시 돌린다.
--
-- 주의:
--   - cleanup은 기존 cleanup-bulk-movies.sql을 그대로 재사용한다(title 접두사
--     'K6-PERF-MOVIE-'가 동일).
--   - 이 스크립트를 돌리기 전에 반드시 cleanup-bulk-movies.sql로 기존 30만 건을
--     먼저 지운다 (안 그러면 30만 건 위에 30건이 추가될 뿐, 검증 의미가 없다).

-- 실행 전 확인 (0이어야 함 - 0이 아니면 cleanup-bulk-movies.sql 먼저 실행)
SELECT COUNT(*) AS existing_seed_rows FROM movies WHERE title LIKE 'K6-PERF-MOVIE-%';

INSERT INTO movies (title, running_time_minutes, release_date, end_date,
                    age_rating, synopsis, avg_score, advance_reservation_rate,
                    status, created_at, updated_at)
SELECT
    CONCAT(
        'K6-PERF-MOVIE-', LPAD(seq.n, 6, '0'),
        -- c-browse-load.js의 KEYWORDS('인터'/'라라')가 걸리는 영화를 몇 건 섞는다
        CASE
            WHEN seq.n % 10 = 0 THEN ' 인터 특별전'
            WHEN seq.n % 15 = 0 THEN ' 라라 리마스터'
            ELSE ''
        END
    ) AS title,
    80 + (seq.n % 100) AS running_time_minutes,
    DATE_ADD('2025-01-01', INTERVAL (seq.n % 730) DAY) AS release_date,
    DATE_ADD('2025-01-01', INTERVAL (seq.n % 730) + 30 DAY) AS end_date,
    ELT(1 + (seq.n % 4), 'ALL', 'AGE_12', 'AGE_15', 'AGE_19') AS age_rating,
    'k6 성능 테스트용 시드 데이터입니다. 실제 콘텐츠가 아닙니다.' AS synopsis,
    ROUND(RAND() * 5, 1) AS avg_score,
    ROUND(RAND() * 100, 1) AS advance_reservation_rate,
    IF(seq.n % 2 = 0, 'NOW_SHOWING', 'UPCOMING') AS status,
    NOW(6), NOW(6)
FROM (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14
    UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19
    UNION ALL SELECT 20 UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24
    UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29
) seq;

-- 실행 후 확인 (30이어야 정상)
SELECT COUNT(*) AS seeded_rows FROM movies WHERE title LIKE 'K6-PERF-MOVIE-%';
