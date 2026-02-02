package com.elice.cinema.domain.reservation.entity;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservations")  // TODO: (status, hold_expires_at) index 잡아서 나중에 만료 상태로 돌리는 Batch 성능 나오도록 해줘야 함
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

    // TODO: CANCELED 상태로 바뀔 때 시간 기록할 필드 필요 (soft delete 하는 객체들엔 전부 다 필요함)
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

    // TODO: HOLD 상태의 Reservation 생성하는 static factory method 만들어야 함
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

    public boolean isExpired(LocalDateTime now) {
        return status == ReservationStatus.HOLD && holdExpiresAt.isBefore(now);
    }

    public void expire() {
        if(status == ReservationStatus.HOLD) {
            status = ReservationStatus.EXPIRED;
        }
    }

    private static String generateCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
