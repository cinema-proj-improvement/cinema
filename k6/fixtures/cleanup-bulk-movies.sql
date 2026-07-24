-- seed-bulk-movies.sql로 넣은 데이터 정리용. 시나리오 C(대규모) 실행이 끝나면 바로 실행한다.
-- movie_genres/movie_screening_types에는 애초에 안 넣었으므로 movies만 지우면 된다.
-- 이 시드 데이터를 대상으로 한 screenings/reservations도 만들지 않았으므로 FK 문제 없음.

-- 삭제 전 확인
SELECT COUNT(*) AS rows_to_delete FROM movies WHERE title LIKE 'K6-PERF-MOVIE-%';

DELETE FROM movies WHERE title LIKE 'K6-PERF-MOVIE-%';

-- 삭제 후 확인 (0이어야 정상)
SELECT COUNT(*) AS remaining_rows FROM movies WHERE title LIKE 'K6-PERF-MOVIE-%';
