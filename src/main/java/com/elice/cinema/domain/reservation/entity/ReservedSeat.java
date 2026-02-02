package com.elice.cinema.domain.reservation.entity;

import com.elice.cinema.domain.screen.entity.Seat;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reserved_seats")  // UK_reserved_seat_unique(screening_id, seat_id) -> 하나의 상영 안에서 좌석 중복 X 제약조건 필수
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

    // TODO: HOLD 상태의 ReservedSeat 생성하는 static factory method 만들어야 함
    public static ReservedSeat createHoldReservedSeat(Reservation reservation,
                                                      Screening screening,
                                                      Seat seat) {
        ReservedSeat reservedSeat = new ReservedSeat();
        reservedSeat.status = ReservationStatus.HOLD;

        reservedSeat.reservation = reservation;
        reservedSeat.screening = screening;
        reservedSeat.seat = seat;

        reservedSeat.seatCode = seat.getSeatCode();

        return reservedSeat;
    }

    public void expire() {
        if(status == ReservationStatus.HOLD) {
            status = ReservationStatus.EXPIRED;
        }
    }
}
