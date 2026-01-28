package com.elice.cinema.domain.screen.dto.response;

import com.elice.cinema.domain.common.ScreeningType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ScreenDetailResponse {
    private String name;
    private ScreeningType screeningType;
    private Integer totalSeats;
    private Boolean operating;

    private List<ScreenSeatResponse> seats;
}
