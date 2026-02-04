package com.elice.cinema.domain.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminPaymentReservationDetailResponse {

    private Long reservationId;
    private String reservationCode;
    private String movieTitle;
    private String screenName;
    private LocalDateTime startAt;

    private Long screeningId;

    // TODO:  좌석 정보(seatCodes, seatCount) 포함 여부 검토
}