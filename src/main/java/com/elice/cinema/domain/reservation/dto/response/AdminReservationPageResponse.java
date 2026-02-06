package com.elice.cinema.domain.reservation.dto.response;

import com.elice.cinema.domain.payment.entity.PaymentStatus;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminReservationPageResponse {
    private Long id;
    private String reservationCode;
    private String memberName;
    private ReservationStatus status;
    private String seatSummary;
    private PaymentStatus paymentStatus;
    private LocalDateTime reservedAt;
    private Integer totalPrice;
}
