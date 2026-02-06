package com.elice.cinema.domain.policy.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "environment_policy")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnvironmentPolicy {
    @Id @Column(name = "id")
    private Long id = 1L;

    // 청소 시간 (분)
    @Column(name = "cleaning_minutes", nullable = false)
    private Integer cleaningMinutes;

    // 예매 마감 데드라인 (분)
    @Column(name = "reservation_deadline_minutes", nullable = false)
    private Integer reservationDeadlineMinutes;

    // 환불 데드라인 (분)
    @Column(name = "refund_deadline_minutes", nullable = false)
    private Integer refundDeadlineMinutes;

    // 1회 최대 예매 가능 좌석 수
    @Column(name = "max_reservation_count", nullable = false)
    private Integer maxReservationCount;

    // 상영 상태 SCHEDULED → OPEN 전환 기준 (일)
    @Column(name = "scheduled_to_open_days", nullable = false)
    private Integer scheduledToOpenDays;

    // 상영 상태 OPEN → CLOSED 전환 기준 (분)
    @Column(name = "open_to_closed_minutes", nullable = false)
    private Integer openToClosedMinutes;

    // 영화관 오픈 시간 (시, 0~23)
    @Column(name = "cinema_open_hour", nullable = false)
    private Integer cinemaOpenHour;

    // 영화표 기본 가격
    @Column(name = "default_price", nullable = false)
    private Integer defaultPrice;

    // 테이블 생성일
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 테이블 수정일
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.id == null) this.id = 1L;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public EnvironmentPolicy(
            Integer cleaningMinutes,
            Integer reservationDeadlineMinutes,
            Integer refundDeadlineMinutes,
            Integer maxReservationCount,
            Integer scheduledToOpenDays,
            Integer openToClosedMinutes,
            Integer cinemaOpenHour,
            Integer defaultPrice
    ) {
        this.id = 1L;
        this.cleaningMinutes = cleaningMinutes;
        this.reservationDeadlineMinutes = reservationDeadlineMinutes;
        this.refundDeadlineMinutes = refundDeadlineMinutes;
        this.maxReservationCount = maxReservationCount;
        this.scheduledToOpenDays = scheduledToOpenDays;
        this.openToClosedMinutes = openToClosedMinutes;
        this.cinemaOpenHour = cinemaOpenHour;
        this.defaultPrice = defaultPrice;
    }


    /**
     * 관리자 수정(값만 수정)용 도메인 메서드
     * - insert/delete 금지, update만 허용하는 정책에 맞춤
     */
    public void updateValues(
            Integer cleaningMinutes,
            Integer reservationDeadlineMinutes,
            Integer refundDeadlineMinutes,
            Integer maxReservationCount,
            Integer scheduledToOpenDays,
            Integer openToClosedMinutes,
            Integer cinemaOpenHour,
            Integer defaultPrice
    ) {
        this.cleaningMinutes = cleaningMinutes;
        this.reservationDeadlineMinutes = reservationDeadlineMinutes;
        this.refundDeadlineMinutes = refundDeadlineMinutes;
        this.maxReservationCount = maxReservationCount;
        this.scheduledToOpenDays = scheduledToOpenDays;
        this.openToClosedMinutes = openToClosedMinutes;
        this.cinemaOpenHour = cinemaOpenHour;
        this.defaultPrice = defaultPrice;
    }
}
