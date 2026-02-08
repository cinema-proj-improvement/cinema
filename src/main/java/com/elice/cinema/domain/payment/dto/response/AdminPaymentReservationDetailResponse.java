package com.elice.cinema.domain.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AdminPaymentReservationDetailResponse {

    private Long reservationId;
    private String reservationCode;
    private String movieTitle;
    private String screenName;
    private LocalDateTime startAt;

    private Long screeningId;

    private List<String> seatCodes;

    public AdminPaymentReservationDetailResponse(Long reservationId, String reservationCode,
                                                 String movieTitle, String screenName,
                                                 LocalDateTime startAt, Long screeningId) {
        this.reservationId = reservationId;
        this.reservationCode = reservationCode;
        this.movieTitle = movieTitle;
        this.screenName = screenName;
        this.startAt = startAt;
        this.screeningId = screeningId;
    }
}