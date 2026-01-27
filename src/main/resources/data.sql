INSERT INTO movies (
    title,
    running_time_minutes,
    release_date,
    end_date,
    age_rating,
    synopsis,
    thumbnail_image_url,
    avg_score,
    advance_reservation_rate,
    status,
    created_at,
    updated_at
) VALUES
      (
          '인터스텔라',
          169,
          '2026-02-01',
          '2026-03-01',
          'AGE_12',
          '우주와 시간, 그리고 인간에 대한 이야기',
          'https://example.com/interstellar.jpg',
          0.0,
          0.0,
          'NOW_SHOWING',
          '2026-01-01 00:00:00',
          '2026-01-01 00:00:00'
      ),
      (
          '듄: 파트 2',
          166,
          '2026-02-15',
          '2026-03-20',
          'AGE_12',
          '사막 행성에서 펼쳐지는 권력과 운명의 서사',
          'https://example.com/dune2.jpg',
          0.0,
          0.0,
          'UPCOMING',
          '2026-01-02 00:00:00',
          '2026-01-02 00:00:00'
      ),
      (
          '존 윅 4',
          170,
          '2026-01-01',
          '2026-01-31',
          'AGE_19',
          '끝없는 복수와 액션의 연속',
          'https://example.com/johnwick4.jpg',
          0.0,
          0.0,
          'ENDED',
          '2026-01-03 00:00:00',
          '2026-01-03 00:00:00'
      ),
      (
          '라라랜드',
          128,
          '2026-02-05',
          '2026-03-05',
          'AGE_12',
          '꿈을 좇는 두 남녀의 사랑 이야기',
          'https://example.com/lalaland.jpg',
          0.0,
          0.0,
          'NOW_SHOWING',
          '2026-01-04 00:00:00',
          '2026-01-04 00:00:00'
      );

INSERT INTO movie_genres (movie_id, genre) VALUES
(1, 'SCI_FI'),
(1, 'ADVENTURE'),

(2, 'SCI_FI'),
(2, 'ADVENTURE'),

(3, 'ACTION'),
(3, 'CRIME'),

(4, 'ROMANCE'),
(4, 'MUSIC');

INSERT INTO movie_screening_types (movie_id, screening_type) VALUES
(1, 'TWO_D'),
(1, 'IMAX'),

(2, 'TWO_D'),
(2, 'FOUR_D'),

(3, 'TWO_D'),
(3, 'FOUR_D'),

(4, 'TWO_D');


INSERT INTO screens (
    name,
    screening_type,
    total_seats,
    is_operating,
    created_at,
    updated_at
) VALUES
      (
          '1관',
          'IMAX',
          250,
          true,
          '2026-01-01 00:00:00',
          '2026-01-01 00:00:00'
      ),
      (
          '2관',
          'FOUR_D',
          180,
          true,
          '2026-01-01 00:00:00',
          '2026-01-01 00:00:00'
      ),
      (
          '3관',
          'TWO_D',
          200,
          true,
          '2026-01-01 00:00:00',
          '2026-01-01 00:00:00'
      ),
      (
          '4관',
          'TWO_D',
          120,
          false,
          '2026-01-01 00:00:00',
          '2026-01-01 00:00:00'
      );
