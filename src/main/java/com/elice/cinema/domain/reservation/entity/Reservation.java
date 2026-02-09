package com.elice.cinema.domain.reservation.entity;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.global.common.audit.BaseEntity;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "member_name", nullable = false)
    private String memberName;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservedSeat> reservedSeats = new ArrayList<>();

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

        // LAZY 방지를 위해 호출하는 위치에서 미리 fetch join으로 가져와야 합니다.
        reservation.movieTitle = screening.getMovie().getTitle();
        reservation.screenName = screening.getScreen().getName();
        reservation.startAt = screening.getStartAt();
        reservation.endAt = screening.getEndAt();
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

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
        this.reservedAt = LocalDateTime.now();
    }

    // 예매 취소
    public void cancel() {
        if (!isCancelable()) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_CANCELABLE);
        }
        this.status = ReservationStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.reservedSeats.clear();
    }
    // 예매 실패(현재는 취소로 나타냄)
    public void fail() {
        this.status = ReservationStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.reservedSeats.clear();
    }

    // 양방향 편의 메서드: ReservedSeat 추가
    public void addReservedSeat(ReservedSeat reservedSeat) {
        reservedSeats.add(reservedSeat);
        reservedSeat.setReservation(this);
    }

    // 양방향 편의 메서드: ReservedSeat 제거
    public void removeReservedSeat(ReservedSeat reservedSeat) {
        reservedSeats.remove(reservedSeat);
        reservedSeat.setReservation(null);
    }

    // 양방향 편의 메서드: 모든 ReservedSeat 제거
    public void clearReservedSeats() {
        reservedSeats.forEach(reservedSeat -> reservedSeat.setReservation(null));
        reservedSeats.clear();
    }
}
