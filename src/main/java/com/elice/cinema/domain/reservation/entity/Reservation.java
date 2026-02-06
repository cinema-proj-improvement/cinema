package com.elice.cinema.domain.reservation.entity;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.global.common.entity.BaseEntity;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "reservations",
        indexes = {
                @Index(
                        name = "IX_reservation_status_holdExpiresAt",
                        columnList = "status, hold_expires_at"
                ),
                @Index(
                        name = "IX_reservation_screening",
                        columnList = "screening_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Reservation extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "reservation_code",  unique = true, nullable = false)
    private String reservationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "hold_expires_at", nullable = false)
    private LocalDateTime holdExpiresAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "movie_title", nullable = false)
    private String movieTitle;

    @Column(name = "screen_name", nullable = false)
    private String screenName;

    @Column(name = "member_name", nullable = false)
    private String memberName;

    public static Reservation createHoldReservation(Screening screening,
                                                    Member member,
                                                    int totalPrice,
                                                    Duration ttl) {
        Reservation reservation = new Reservation();
        reservation.reservationCode = generateCode();
        reservation.status = ReservationStatus.HOLD;

        reservation.holdExpiresAt = LocalDateTime.now().plus(ttl);

        reservation.totalPrice = totalPrice;

        reservation.screening = screening;
        reservation.member = member;

        // TODO: LAZY -> 서비스에서 미리 fetch join으로 가져와야 함
        reservation.movieTitle = screening.getMovie().getTitle();
        reservation.screenName = screening.getScreen().getName();
        reservation.memberName = member.getName();

        return reservation;
    }

    private static String generateCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    //예매가 확정(CONFIRMED) 상태인지 여부
    public boolean isCancelableStatus() {
        return this.status == ReservationStatus.CONFIRMED;
    }

    //현재 시점 기준으로 상영 시작 전인지 여부
    public boolean isBeforeScreening() {
        return this.screening.getStartAt().isAfter(LocalDateTime.now());
    }

    // 예매 취소 가능 여부 (화면 / API 공통 판단)
    public boolean isCancelable() {
        return isCancelableStatus() && isBeforeScreening();
    }

    // 예매 취소
    public void cancel() {
        if (!isCancelable()) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_CANCELABLE);
        }
        this.status = ReservationStatus.CANCELED;
    }
}
