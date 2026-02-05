package com.elice.cinema.domain.reservation.dto.response;

import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class MypageDetailReservationResponse {
    private Long id;
    private String reservationCode;
    private ReservationStatus status;
    private String movieTitle;
    private String screenName;
    private LocalDateTime reservedAt;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private List<String> seatCodes;
}
