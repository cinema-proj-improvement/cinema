-- =====================================================================
-- V1: cinema 데이터베이스 초기 스키마
-- 전제: RDS 인스턴스 생성 시 'Initial database name = cinema' 으로 설정,
--       DB_URL = jdbc:mysql://<host>:3306/cinema?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
-- =====================================================================

CREATE TABLE members (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    nickname   VARCHAR(50)  NOT NULL,
    age        INT          NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_email    UNIQUE (email),
    CONSTRAINT uk_member_nickname UNIQUE (nickname)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 싱글턴 정책 테이블 (id 는 항상 1)
CREATE TABLE environment_policy (
    id                           BIGINT      NOT NULL,
    cleaning_minutes             INT         NOT NULL,
    reservation_deadline_minutes INT         NOT NULL,
    refund_deadline_minutes      INT         NOT NULL,
    max_reservation_count        INT         NOT NULL,
    scheduled_to_open_days       INT         NOT NULL,
    open_to_closed_minutes       INT         NOT NULL,
    cinema_open_hour             INT         NOT NULL,
    default_price                INT         NOT NULL,
    created_at                   DATETIME(6) NOT NULL,
    updated_at                   DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE movies (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    title                    VARCHAR(255) NOT NULL,
    running_time_minutes     INT          NOT NULL,
    release_date             DATE         NOT NULL,
    end_date                 DATE         NOT NULL,
    age_rating               VARCHAR(20)  NOT NULL,
    synopsis                 LONGTEXT     NOT NULL,
    avg_score                DOUBLE       NOT NULL,
    advance_reservation_rate DOUBLE       NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    created_at               DATETIME(6)  NOT NULL,
    updated_at               DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_movie_status_release_date (status, release_date),
    INDEX idx_movie_status_end_date     (status, end_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Movie.genres @ElementCollection
CREATE TABLE movie_genres (
    movie_id BIGINT      NOT NULL,
    genre    VARCHAR(30) NOT NULL,
    PRIMARY KEY (movie_id, genre),
    CONSTRAINT fk_movie_genres_movie FOREIGN KEY (movie_id) REFERENCES movies (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Movie.screeningTypes @ElementCollection
CREATE TABLE movie_screening_types (
    movie_id       BIGINT      NOT NULL,
    screening_type VARCHAR(30) NOT NULL,
    PRIMARY KEY (movie_id, screening_type),
    CONSTRAINT fk_movie_screening_types_movie FOREIGN KEY (movie_id) REFERENCES movies (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- MovieImage: BaseEntity 미상속 → created_at/updated_at 없음
CREATE TABLE movie_images (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    movie_id      BIGINT       NOT NULL,
    image_url     VARCHAR(500) NOT NULL,
    display_order INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK_movie_image_movie_display_order UNIQUE (movie_id, display_order),
    INDEX IX_movie_images_movie_id               (movie_id),
    INDEX IX_movie_images_movie_id_display_order (movie_id, display_order),
    CONSTRAINT fk_movie_images_movie FOREIGN KEY (movie_id) REFERENCES movies (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE screens (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    name           VARCHAR(50) NOT NULL,
    screening_type VARCHAR(30) NOT NULL,
    total_seats    INT         NOT NULL,
    is_operating   TINYINT(1)  NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_screen_name UNIQUE (name),
    INDEX ix_screening_type (screening_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE seats (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    screen_id  BIGINT      NOT NULL,
    seat_code  VARCHAR(10) NOT NULL,
    is_active  TINYINT(1)  NOT NULL,
    row_no     INT         NOT NULL,
    col_no     INT         NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_seat_screen_code    UNIQUE (screen_id, seat_code),
    CONSTRAINT uk_seat_screen_row_col UNIQUE (screen_id, row_no, col_no),
    INDEX ix_seat_screen (screen_id),
    CONSTRAINT fk_seats_screen FOREIGN KEY (screen_id) REFERENCES screens (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE screenings (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    movie_id             BIGINT      NOT NULL,
    screen_id            BIGINT      NOT NULL,
    screening_type       VARCHAR(30) NOT NULL,
    start_at             DATETIME(6) NOT NULL,
    end_at               DATETIME(6) NOT NULL,
    end_at_with_cleaning DATETIME(6) NOT NULL,
    screening_status     VARCHAR(20) NOT NULL,
    created_at           DATETIME(6) NOT NULL,
    updated_at           DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_screening_screen_start UNIQUE (screen_id, start_at),
    INDEX ix_screening_movie       (movie_id,  start_at),
    INDEX ix_screening_screen_time (screen_id, start_at, end_at_with_cleaning),
    CONSTRAINT fk_screenings_movie  FOREIGN KEY (movie_id)  REFERENCES movies  (id),
    CONSTRAINT fk_screenings_screen FOREIGN KEY (screen_id) REFERENCES screens (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE refund_policies (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    name                 VARCHAR(50) NOT NULL,
    before_start_minutes INT         NOT NULL,
    refund_rate          INT         NOT NULL,
    created_at           DATETIME(6) NOT NULL,
    updated_at           DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refund_policy_before_start_minutes UNIQUE (before_start_minutes)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE reservations (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    reservation_code VARCHAR(255) NOT NULL,
    status           VARCHAR(255) NOT NULL,
    reserved_at      DATETIME(6),
    hold_expires_at  DATETIME(6)  NOT NULL,
    canceled_at      DATETIME(6),
    total_price      INT          NOT NULL,
    screening_id     BIGINT       NOT NULL,
    member_id        BIGINT       NOT NULL,
    movie_title      VARCHAR(255) NOT NULL,
    screen_name      VARCHAR(255) NOT NULL,
    start_at         DATETIME(6)  NOT NULL,
    end_at           DATETIME(6)  NOT NULL,
    member_name      VARCHAR(255) NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_reservation_code UNIQUE (reservation_code),
    INDEX IX_reservation_status_holdExpiresAt (status, hold_expires_at),
    INDEX IX_reservation_screening            (screening_id),
    CONSTRAINT fk_reservations_screening FOREIGN KEY (screening_id) REFERENCES screenings (id),
    CONSTRAINT fk_reservations_member   FOREIGN KEY (member_id)    REFERENCES members    (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE reserved_seats (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    status         VARCHAR(255) NOT NULL,
    reservation_id BIGINT       NOT NULL,
    screening_id   BIGINT       NOT NULL,
    seat_id        BIGINT       NOT NULL,
    seat_code      VARCHAR(255) NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK_reserved_seat_screening_seat UNIQUE (screening_id, seat_id),
    INDEX IX_reserved_seat_reservation_status (reservation_id, status),
    INDEX IX_reserved_seat_screening_status   (screening_id,   status),
    CONSTRAINT fk_reserved_seats_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_reserved_seats_screening   FOREIGN KEY (screening_id)   REFERENCES screenings   (id),
    CONSTRAINT fk_reserved_seats_seat        FOREIGN KEY (seat_id)        REFERENCES seats        (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Payment: BaseEntity 미상속 → created_at/updated_at 없음
CREATE TABLE payments (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    reservation_id   BIGINT       NOT NULL,
    member_id        BIGINT       NOT NULL,
    reservation_code VARCHAR(255) NOT NULL,
    payment_key      VARCHAR(255) NOT NULL,
    amount           BIGINT       NOT NULL,
    status           VARCHAR(255) NOT NULL,
    approved_at      DATETIME(6)  NOT NULL,
    method           VARCHAR(255) NOT NULL,
    failure_message  VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_reservation      UNIQUE (reservation_id),
    CONSTRAINT uk_payment_key              UNIQUE (payment_key),
    CONSTRAINT uk_payment_reservation_code UNIQUE (reservation_code),
    CONSTRAINT fk_payments_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_payments_member      FOREIGN KEY (member_id)      REFERENCES members      (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE refunds (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    payment_id    BIGINT      NOT NULL,
    refund_amount BIGINT      NOT NULL,
    refunded_at   DATETIME(6) NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refund_payment  UNIQUE (payment_id),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE reviews (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    member_id      BIGINT      NOT NULL,
    movie_id       BIGINT      NOT NULL,
    reservation_id BIGINT      NOT NULL,
    score          INT         NOT NULL,
    content        TEXT        NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_review_reservation UNIQUE (reservation_id),
    INDEX ix_review_movie_created  (movie_id,  created_at),
    INDEX ix_review_member_created (member_id, created_at),
    CONSTRAINT fk_reviews_member      FOREIGN KEY (member_id)      REFERENCES members      (id),
    CONSTRAINT fk_reviews_movie       FOREIGN KEY (movie_id)       REFERENCES movies       (id),
    CONSTRAINT fk_reviews_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
