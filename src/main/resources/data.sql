-- =========================================================
-- ✅ 테스트 멤버 생성 (그대로 유지)
-- =========================================================
INSERT INTO members (
    id,
    email,
    password,
    name,
    nickname,
    age,
    role,
    created_at,
    updated_at
) VALUES (
             3,
             'test3@cinema.com',
             '$2a$10$M6AGWhAhrcsIxezsyoMVuuK1DfQ4Y8pECszpHwAWv3D8ZePBUHYNW',
             '테스트회원',
             'testuser3',   -- ✅ UNIQUE + NOT NULL
             25,            -- ✅ NOT NULL
             'ADMIN',
             '2026-02-02 00:00:00',
             '2026-02-02 00:00:00'
         );

-- =========================================================
-- ⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇
-- members INSERT 바로 아래에 "통 복붙" (여기부터)
-- ⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇
-- =========================================================

-- =========================
-- ENVIRONMENT POLICY (id=1 고정 사용 시 필요)
-- =========================
INSERT INTO environment_policy (
    id,
    cinema_open_hour,
    cleaning_minutes,
    default_price,
    max_reservation_count,
    open_to_closed_minutes,
    refund_deadline_minutes,
    reservation_deadline_minutes,
    scheduled_to_open_days,
    created_at,
    updated_at
) VALUES (
             1,
             9,
             15,
             15000,
             4,
             10,
             30,
             10,
             7,
             '2026-01-01 00:00:00',
             '2026-01-01 00:00:00'
         );

-- =========================
-- MOVIES
-- =========================
INSERT INTO movies (
    title, running_time_minutes, release_date, end_date,
    age_rating, synopsis, avg_score, advance_reservation_rate,
    status, created_at, updated_at
) VALUES
      ('인터스텔라', 169, '2026-02-01', '2026-03-01', 'AGE_12',
       '우주와 시간, 그리고 인간에 대한 이야기', 0.0, 0.0, 'NOW_SHOWING',
       '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
      ('듄: 파트 2', 166, '2026-02-15', '2026-03-20', 'AGE_12',
       '사막 행성에서 펼쳐지는 권력과 운명의 서사', 0.0, 0.0, 'UPCOMING',
       '2026-01-02 00:00:00', '2026-01-02 00:00:00'),
      ('존 윅 4', 170, '2026-01-01', '2026-01-31', 'AGE_19',
       '끝없는 복수와 액션의 연속', 0.0, 0.0, 'ENDED',
       '2026-01-03 00:00:00', '2026-01-03 00:00:00'),
      ('라라랜드', 128, '2026-02-05', '2026-03-05', 'AGE_12',
       '꿈을 좇는 두 남녀의 사랑 이야기', 0.0, 0.0, 'NOW_SHOWING',
       '2026-01-04 00:00:00', '2026-01-04 00:00:00');

-- =========================
-- MOVIE GENRES
-- =========================
INSERT INTO movie_genres (movie_id, genre) VALUES
                                               (1, 'SCI_FI'), (1, 'ADVENTURE'),
                                               (2, 'SCI_FI'), (2, 'ADVENTURE'),
                                               (3, 'ACTION'), (3, 'CRIME'),
                                               (4, 'ROMANCE'), (4, 'MUSIC');

-- =========================
-- MOVIE SCREENING TYPES
-- =========================
INSERT INTO movie_screening_types (movie_id, screening_type) VALUES
                                                                 (1, 'TWO_D'), (1, 'IMAX'),
                                                                 (2, 'TWO_D'), (2, 'FOUR_D'),
                                                                 (3, 'TWO_D'), (3, 'FOUR_D'),
                                                                 (4, 'TWO_D');

-- =========================
-- SCREENS
-- =========================
INSERT INTO screens (
    name, screening_type, total_seats, is_operating, created_at, updated_at
) VALUES
      ('1관', 'IMAX', 5, true, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
      ('2관', 'FOUR_D', 180, true, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
      ('3관', 'TWO_D', 200, true, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
      ('4관', 'TWO_D', 120, false, '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- =========================
-- SEATS (1관 A1~A5)
-- =========================
INSERT INTO seats (
    screen_id, seat_code, is_active, row_no, col_no, created_at, updated_at
) VALUES
      (1, 'A1', true, 1, 1, '2026-01-05 00:00:00', '2026-01-05 00:00:00'),
      (1, 'A2', true, 1, 2, '2026-01-05 00:00:00', '2026-01-05 00:00:00'),
      (1, 'A3', true, 1, 3, '2026-01-05 00:00:00', '2026-01-05 00:00:00'),
      (1, 'A4', true, 1, 4, '2026-01-05 00:00:00', '2026-01-05 00:00:00'),
      (1, 'A5', true, 1, 5, '2026-01-05 00:00:00', '2026-01-05 00:00:00');

-- =========================
-- SCREENINGS (id = 1~6)
-- =========================
INSERT INTO screenings (
    movie_id, screen_id, screening_type,
    start_at, end_at, end_at_with_cleaning,
    screening_status, created_at, updated_at
) VALUES
      (1, 1, 'IMAX',
       '2026-01-30 09:00:00', '2026-01-30 11:49:00', '2026-01-30 12:04:00',
       'SCHEDULED', '2026-01-10 00:00:00', '2026-01-10 00:00:00'),
      (1, 1, 'IMAX',
       '2026-02-01 13:00:00', '2026-02-01 15:49:00', '2026-02-01 16:04:00',
       'SCHEDULED', '2026-01-10 00:00:00', '2026-01-10 00:00:00'),
      (1, 3, 'TWO_D',
       '2026-02-02 10:00:00', '2026-02-02 12:49:00', '2026-02-02 13:04:00',
       'SCHEDULED', '2026-01-10 00:00:00', '2026-01-10 00:00:00'),
      (4, 3, 'TWO_D',
       '2026-02-01 14:00:00', '2026-02-01 16:08:00', '2026-02-01 16:23:00',
       'SCHEDULED', '2026-01-10 00:00:00', '2026-01-10 00:00:00'),
      (4, 3, 'TWO_D',
       '2026-02-02 17:00:00', '2026-02-02 19:08:00', '2026-02-02 19:23:00',
       'SCHEDULED', '2026-01-10 00:00:00', '2026-01-10 00:00:00'),
      (4, 2, 'FOUR_D',
       '2026-02-03 18:00:00', '2026-02-03 20:08:00', '2026-02-03 20:23:00',
       'SCHEDULED', '2026-01-10 00:00:00', '2026-01-10 00:00:00');

-- =========================
-- RESERVATIONS (✅ member_id 전부 3으로 통일)
--  - reservation.id 는 insert 순서대로 1~4로 생성됨(초기 데이터 기준)
-- =========================
INSERT INTO reservations (
    reservation_code, status, reserved_at, hold_expires_at, canceled_at,
    total_price, screening_id, member_id,
    movie_title, screen_name, start_at, end_at,
    member_name, created_at, updated_at
) VALUES
      ('R-20260201-001', 'CONFIRMED',
       '2026-01-31 10:00:00', '2026-01-31 10:00:00', NULL,
       20000, 2, 3,
       '인터스텔라', '1관',
       '2026-02-01 13:00:00', '2026-02-01 15:49:00',
       '테스트회원', '2026-01-31 10:00:00', '2026-01-31 10:00:00'),

      ('R-20260201-002', 'CONFIRMED',
       '2026-01-31 10:05:00', '2026-01-31 10:05:00', NULL,
       30000, 2, 3,
       '인터스텔라', '1관',
       '2026-02-01 13:00:00', '2026-02-01 15:49:00',
       '테스트회원', '2026-01-31 10:05:00', '2026-01-31 10:05:00'),

      ('R-20260201-003', 'HOLD',
       '2026-01-31 10:10:00', '2026-01-31 11:10:00', NULL,
       15000, 2, 3,
       '인터스텔라', '1관',
       '2026-02-01 13:00:00', '2026-02-01 15:49:00',
       '테스트회원', '2026-01-31 10:10:00', '2026-01-31 10:10:00'),

      ('R-20260201-004', 'CANCELED',
       '2026-01-31 10:15:00', '2026-01-31 11:15:00', '2026-01-31 10:20:00',
       10000, 2, 3,
       '인터스텔라', '1관',
       '2026-02-01 13:00:00', '2026-02-01 15:49:00',
       '테스트회원', '2026-01-31 10:15:00', '2026-01-31 10:20:00');

-- =========================
-- RESERVED SEATS
--  - seat_id = 1~5 (A1~A5)
--  - reservation_id = 1~4 (위 reservations insert 순서 기준)
-- =========================
INSERT INTO reserved_seats (
    status, reservation_id, screening_id, seat_id, seat_code, created_at, updated_at
) VALUES
      ('CONFIRMED', 1, 2, 1, 'A1', '2026-01-31 10:00:00', '2026-01-31 10:00:00'),
      ('CONFIRMED', 1, 2, 2, 'A2', '2026-01-31 10:00:00', '2026-01-31 10:00:00'),
      ('CONFIRMED', 2, 2, 3, 'A3', '2026-01-31 10:05:00', '2026-01-31 10:05:00'),
      ('HOLD',      3, 2, 4, 'A4', '2026-01-31 10:10:00', '2026-01-31 10:10:00'),
      ('CANCELED',  4, 2, 5, 'A5', '2026-01-31 10:15:00', '2026-01-31 10:20:00');

-- =========================
-- PAYMENTS (✅ member_id 전부 3으로 통일)
--  - payments.reservation_id 는 reservations.id(1,2,4) 사용
--  - approved_at 은 timestamp with time zone
-- =========================
INSERT INTO payments (
    reservation_id, member_id, reservation_code,
    payment_key, amount, status, approved_at, method
) VALUES
      (1, 3, 'R-20260201-001', 'pay_test_0001', 20000, 'PAID',     '2026-02-01 10:30:00+09:00', 'CARD'),
      (2, 3, 'R-20260201-002', 'pay_test_0002', 30000, 'PAID',     '2026-02-01 11:00:00+09:00', 'CARD'),
      (4, 3, 'R-20260201-004', 'pay_test_0003', 10000, 'CANCELED', '2026-02-01 11:30:00+09:00', 'CARD');

INSERT INTO refund_policies (name, before_start_minutes, refund_rate, created_at, updated_at)
VALUES
    ('상영 1일 전', 1440, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('상영 1시간 전', 60, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('상영 10분 전', 20, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
