package com.elice.cinema.domain.reservation.entity;

import com.elice.cinema.domain.screen.entity.Seat;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.global.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "reserved_seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_reserved_seat_screening_seat",
                        columnNames = {"screening_id", "seat_id"}
                )
        },
        indexes = {
                @Index(
                        name = "IX_reserved_seat_reservation_status",
                        columnList = "reservation_id, status"
                ),
                @Index(
                        name = "IX_reserved_seat_screening_status",
                        columnList = "screening_id, status"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ReservedSeat extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id",  nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "seat_code", nullable = false)
    private String seatCode;

    public static ReservedSeat createHoldReservedSeat(Screening screening,
                                                      Seat seat) {
        ReservedSeat reservedSeat = new ReservedSeat();
        reservedSeat.status = ReservationStatus.HOLD;

        reservedSeat.screening = screening;
        reservedSeat.seat = seat;

        reservedSeat.seatCode = seat.getSeatCode();

        return reservedSeat;
    }

    //TODO: 나중에  paymentStatus의 canChangeTo 만들어서 유효성 검사하기
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    // 양방향 편의 메서드: Reservation 설정
    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }
}
