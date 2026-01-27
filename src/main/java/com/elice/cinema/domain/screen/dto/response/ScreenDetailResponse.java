package com.elice.cinema.domain.screen.dto.response;

import com.elice.cinema.domain.common.ScreeningType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScreenDetailResponse {
    private String name;
    private ScreeningType screeningType;
    private Integer totalSeats;
    private boolean operating;

    private List<ScreenSeatResponse> seats;
}
