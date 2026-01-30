package com.elice.cinema.domain.reservation.entity;

import com.elice.cinema.domain.screen.entity.Seat;
import com.elice.cinema.domain.screening.entity.Screening;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reserved_seats")  // UK_reserved_seat_unique(screening_id, seat_id) -> 하나의 상영 안에서 좌석 중복 X 제약조건 필수
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ReservedSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

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
}
