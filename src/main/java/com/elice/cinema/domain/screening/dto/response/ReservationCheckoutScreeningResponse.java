package com.elice.cinema.domain.screening.dto.response;

import com.elice.cinema.domain.common.ScreeningType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReservationCheckoutScreeningResponse {
    private ScreeningType screeningType;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
