package com.elice.cinema.domain.reservation.dto.response.seatselection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenInfo {
    private Long screenId;
    private String screenName;
    private String screeningType;
}
