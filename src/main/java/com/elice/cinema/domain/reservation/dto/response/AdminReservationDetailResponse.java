package com.elice.cinema.domain.reservation.dto.response;

import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminReservationDetailResponse {

    // === 엔티티 값 ===
    private Long id;
    private String reservationCode;
    private String memberName;
    private String movieTitle;
    private String screenName;
    private ReservationStatus status;
    private LocalDateTime reservedAt;
    private Integer totalPrice;

    // === 가공값 ===
    private List<String> seatCodes;   // ["C8", "J9"]
    private String paymentStatus;     // "PAID" (임시)
    private boolean cancelable;       // CONFIRMED일 때만 true
}
