package com.elice.cinema.domain.screen.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationScreenSelectResponse {
    private String name;
    private String screeningTypeDisplayName;
    private Integer totalSeats;
}
