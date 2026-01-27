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
-- ===============================
-- Movie - Genre Mapping
-- ===============================

INSERT INTO movie_genres (movie_id, genre) VALUES
                                               (1, 'SCI_FI'),
                                               (1, 'DRAMA'),
                                               (1, 'ADVENTURE'),

                                               (2, 'SCI_FI'),
                                               (2, 'ACTION'),
                                               (2, 'ADVENTURE'),

                                               (3, 'ACTION'),
                                               (3, 'CRIME'),
                                               (3, 'THRILLER'),

                                               (4, 'ROMANCE'),
                                               (4, 'DRAMA'),
                                               (4, 'MUSIC');
