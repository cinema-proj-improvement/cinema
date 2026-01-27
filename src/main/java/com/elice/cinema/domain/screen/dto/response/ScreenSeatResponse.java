package com.elice.cinema.domain.screen.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScreenSeatResponse {
    private Long id;
    private String seatCode;
    private boolean active;
    private Integer rowNo;
    private Integer colNo;
}
