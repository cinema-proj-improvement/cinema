package com.elice.cinema.domain.reservation.dto.response;

import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminReservationListResponse {
    private Long id;
    private String reservationCode;
    private String memberName;
    private String movieTitle;
    private String screenName;
    private ReservationStatus status;
    private LocalDateTime reservedAt;
    private Integer totalPrice;

    private String seatSummary;
    private String paymentStatus;
}