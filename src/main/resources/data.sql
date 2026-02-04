-- =========================================================
-- 테스트 멤버 생성 (Member 엔티티 기준, id = 3)
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
             'USER',
             '2026-02-02 00:00:00',
             '2026-02-02 00:00:00'
         );


INSERT INTO movies (
    title,
    running_time_minutes,
    release_date,
    end_date,
    age_rating,
    synopsis,
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
          5,
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

INSERT INTO seats (
    screen_id,
    seat_code,
    is_active,
    row_no,
    col_no,
    created_at,
    updated_at
) VALUES
      (1, 'A1', true, 1, 1, '2026-01-05 00:00:00', '2026-01-05 00:00:00'),
      (1, 'A2', true, 1, 2, '2026-01-05 00:00:00', '2026-01-05 00:00:00'),
      (1, 'A3', true, 1, 3, '2026-01-05 00:00:00', '2026-01-05 00:00:00'),
      (1, 'A4', true, 1, 4, '2026-01-05 00:00:00', '2026-01-05 00:00:00'),
      (1, 'A5', true, 1, 5, '2026-01-05 00:00:00', '2026-01-05 00:00:00');

INSERT INTO screenings (
    movie_id,
    screen_id,
    screening_type,
    start_at,
    end_at,
    end_at_with_cleaning,
    screening_status,
    created_at,
    updated_at
) VALUES

-- ===============================
-- 🎬 인터스텔라 (NOW_SHOWING)
-- ===============================

-- 1️⃣ 인터스텔라 / IMAX / 1관 / 2월 1일
(
    1,
    1,
    'IMAX',
    '2026-01-30 09:00:00',
    '2026-01-30 11:49:00',
    '2026-02-01 12:04:00',
    'SCHEDULED',
    '2026-01-10 00:00:00',
    '2026-01-10 00:00:00'
),

-- 2️⃣ 인터스텔라 / IMAX / 1관 / 2월 1일 (2회차)
(
    1,
    1,
    'IMAX',
    '2026-02-01 13:00:00',
    '2026-02-01 15:49:00',
    '2026-02-01 16:04:00',
    'SCHEDULED',
    '2026-01-10 00:00:00',
    '2026-01-10 00:00:00'
),

-- 3️⃣ 인터스텔라 / 2D / 3관 / 2월 2일
(
    1,
    3,
    'TWO_D',
    '2026-02-02 10:00:00',
    '2026-02-02 12:49:00',
    '2026-02-02 13:04:00',
    'SCHEDULED',
    '2026-01-10 00:00:00',
    '2026-01-10 00:00:00'
),

-- ===============================
-- 🎬 라라랜드 (NOW_SHOWING)
-- ===============================

-- 4️⃣ 라라랜드 / 2D / 3관 / 2월 1일
(
    4,
    3,
    'TWO_D',
    '2026-02-01 14:00:00',
    '2026-02-01 16:08:00',
    '2026-02-01 16:23:00',
    'SCHEDULED',
    '2026-01-10 00:00:00',
    '2026-01-10 00:00:00'
),

-- 5️⃣ 라라랜드 / 2D / 3관 / 2월 2일 (저녁 회차)
(
    4,
    3,
    'TWO_D',
    '2026-02-02 17:00:00',
    '2026-02-02 19:08:00',
    '2026-02-02 19:23:00',
    'SCHEDULED',
    '2026-01-10 00:00:00',
    '2026-01-10 00:00:00'
),

-- ===============================
-- 🎬 라라랜드 / 4D / 2관 (필터용 핵심)
-- ===============================

-- 6️⃣ 라라랜드 / 4D / 2관 / 2월 3일
(
    4,
    2,
    'FOUR_D',
    '2026-02-03 18:00:00',
    '2026-02-03 20:08:00',
    '2026-02-03 20:23:00',
    'SCHEDULED',
    '2026-01-10 00:00:00',
    '2026-01-10 00:00:00'
);

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
) VALUES
      (
          1,
          'hong@test.com',
          'password',
          '홍길동',
          'hong',
          30,
          'USER',
          '2026-01-01 00:00:00',
          '2026-01-01 00:00:00'
      ),
      (
          2,
          'kim@test.com',
          'password',
          '김영희',
          'kim',
          25,
          'USER',
          '2026-01-01 00:00:00',
          '2026-01-01 00:00:00'
      );


INSERT INTO reservations ( id, reservation_code, member_id, member_name, movie_title, screen_name, screening_id, status, reserved_at, total_price, created_at, updated_at )
VALUES
    ( 1, 'R-20260201-001', 1, '홍길동', '인터스텔라', '1관', 2, 'CONFIRMED', '2026-01-31 10:00:00', 20000, NOW(), NOW() ),
    ( 2, 'R-20260201-002', 2, '김영희', '인터스텔라', '1관', 2, 'CONFIRMED', '2026-01-31 10:05:00', 30000, NOW(), NOW() ),
    ( 3, 'R-20260201-003', 1, '홍길동', '인터스텔라', '1관', 2, 'HOLD', '2026-01-31 10:10:00', 15000, NOW(), NOW() ),
    ( 4, 'R-20260201-004', 2, '김영희', '인터스텔라', '1관', 2, 'CANCELED', '2026-01-31 10:15:00', 10000, NOW(), NOW() );

INSERT INTO reserved_seats (
    reservation_id,
    screening_id,
    seat_id,
    seat_code,
    status
) VALUES
      (1, 2, 1, 'A1', 'CONFIRMED'),
      (1, 2, 2, 'A2', 'CONFIRMED'),
      (2, 2, 3, 'A3', 'CONFIRMED'),
      (3, 2, 4, 'A4', 'HOLD'),
      (4, 2, 5, 'A5', 'CANCELED');

INSERT INTO screenings (
    movie_id,
    screen_id,
    screening_type,
    start_at,
    end_at,
    end_at_with_cleaning,
    screening_status,
    created_at,
    updated_at
) VALUES

-- ===============================
-- ✅ OPEN: 인터스텔라 (1관 IMAX) - 오늘(1/30) 바로 보이게
-- ===============================
(
    1,
    1,
    'IMAX',
    '2026-01-30 13:00:00',
    '2026-01-30 15:49:00',
    '2026-01-30 16:04:00',
    'OPEN',
    '2026-01-20 00:00:00',
    '2026-01-20 00:00:00'
),

-- ===============================
-- ✅ OPEN: 라라랜드 (3관 2D) - 2/1, 2/2 보이게
-- ===============================
(
    4,
    3,
    'TWO_D',
    '2026-02-01 10:00:00',
    '2026-02-01 12:08:00',
    '2026-02-01 12:23:00',
    'OPEN',
    '2026-01-20 00:00:00',
    '2026-01-20 00:00:00'
),
(
    4,
    3,
    'TWO_D',
    '2026-02-02 14:00:00',
    '2026-02-02 16:08:00',
    '2026-02-02 16:23:00',
    'OPEN',
    '2026-01-20 00:00:00',
    '2026-01-20 00:00:00'
),

-- ===============================
-- ✅ OPEN: 듄 파트2 (2관 4D) - 2/4 보이게 (UPCOMING이어도 OPEN 상영 있으면 필터에 걸릴 수 있음)
-- ===============================
(
    2,
    2,
    'FOUR_D',
    '2026-02-04 19:00:00',
    '2026-02-04 21:46:00',
    '2026-02-04 22:01:00',
    'OPEN',
    '2026-01-20 00:00:00',
    '2026-01-20 00:00:00'
);

-- =========================================================
-- 1) (선택) 9번 상영의 상영관에 A1~A3 좌석이 없다면 먼저 만들어두기
--    * 이미 seats가 충분히 들어가 있으면 이 블록은 빼도 됨
--    * screen_id는 9번 상영의 screen_id를 따라감
-- =========================================================
INSERT INTO seats (screen_id, seat_code, is_active, row_no, col_no, created_at, updated_at)
SELECT sc.screen_id, 'A1', true, 1, 1, '2026-02-02 00:00:00', '2026-02-02 00:00:00'
FROM screenings sc
WHERE sc.id = 9;

INSERT INTO seats (screen_id, seat_code, is_active, row_no, col_no, created_at, updated_at)
SELECT sc.screen_id, 'A2', true, 1, 2, '2026-02-02 00:00:00', '2026-02-02 00:00:00'
FROM screenings sc
WHERE sc.id = 9;

INSERT INTO seats (screen_id, seat_code, is_active, row_no, col_no, created_at, updated_at)
SELECT sc.screen_id, 'A3', true, 1, 3, '2026-02-02 00:00:00', '2026-02-02 00:00:00'
FROM screenings sc
WHERE sc.id = 9;


-- =========================================================
-- 2) 9번 상영 예약 1건 생성 (Reservation 엔티티 컬럼 맞춤)
--    reservation_code 는 유니크여야 하니 충돌 안 나게 고정값 추천
-- =========================================================
INSERT INTO reservations (
    reservation_code,
    status,
    reserved_at,
    total_price,
    screening_id,
    member_id,
    movie_title,
    screen_name,
    member_name,
    created_at,
    updated_at
) VALUES (
             'RES-000009-01',          -- 유니크 코드 (원하는 규칙으로 바꿔도 됨)
             'CONFIRMED',
             '2026-02-02 13:10:00',
             45000,                    -- 예: 15000 * 3좌석
             9,
             3,                        -- member_id=1 이 DB에 있어야 함
             (SELECT m.title
              FROM screenings sc
                       JOIN movies m ON m.id = sc.movie_id
              WHERE sc.id = 9),
             (SELECT s.name
              FROM screenings sc
                       JOIN screens s ON s.id = sc.screen_id
              WHERE sc.id = 9),
             '테스트회원',               -- member_name 컬럼이 NOT NULL이라 임시값 필수
             '2026-02-02 13:10:00',
             '2026-02-02 13:10:00'
         );

-- =========================================================
-- 3) 예약 좌석 3개 생성 (ReservedSeat 엔티티 컬럼 맞춤)
--    ReservedSeat.status 는 @Enumerated가 없어서 ORDINAL(TINYINT)로 저장됨
--    ※ ReservationStatus enum 순서 기준으로 CONFIRMED 값(보통 1) 넣기
-- =========================================================
INSERT INTO reserved_seats (
    status,
    reservation_id,
    screening_id,
    seat_id,
    seat_code
)
SELECT
    1            AS status,          -- ✅ CONFIRMED (ORDINAL 가정: 0=HOLD, 1=CONFIRMED, 2=CANCELED)
    r.id         AS reservation_id,
    9            AS screening_id,
    st.id        AS seat_id,
    st.seat_code AS seat_code
FROM reservations r
         JOIN screenings sc ON sc.id = 9
         JOIN seats st ON st.screen_id = sc.screen_id
WHERE r.reservation_code = 'RES-000009-01'
  AND st.seat_code IN ('A1', 'A2', 'A3');


-- =========================================================
-- 테스트 결제 데이터 (Payment 엔티티 필수 컬럼 모두 포함)
-- =========================================================
INSERT INTO payments (
    reservation_id,
    member_id,
    reservation_code,
    payment_key,
    amount,
    status,
    approved_at,
    method
) VALUES
      (
          1,
          1,
          'R-20260201-001',
          'pay_test_0001',
          20000,
          'PAID',
          '2026-02-01 10:30:00',
          'CARD'
      ),
      (
          2,
          2,
          'R-20260201-002',
          'pay_test_0002',
          30000,
          'PAID',
          '2026-02-01 11:00:00',
          'CARD'
      ),
      (
          4,
          2,
          'R-20260201-004',
          'pay_test_0003',
          10000,
          'CANCELED',
          '2026-02-01 11:30:00',
          'CARD'
      );
