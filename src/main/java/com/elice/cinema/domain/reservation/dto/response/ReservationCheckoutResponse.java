package com.elice.cinema.domain.reservation.dto.response;

import com.elice.cinema.domain.screening.dto.response.ReservationCheckoutScreeningResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReservationCheckoutResponse {
    private Long id;
    private Integer totalPrice;
    private ReservationCheckoutScreeningResponse screening;
    private String movieTitle;
    private String screenName;

    private String movieThumbnail;
    private List<String> seatCodes;
}
