package com.elice.cinema.domain.reservation.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class MypageHomeReservationResponse {
    private String reservationCode;
    private String movieTitle;
    private String screenName;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private List<String> seatCodes;
}
