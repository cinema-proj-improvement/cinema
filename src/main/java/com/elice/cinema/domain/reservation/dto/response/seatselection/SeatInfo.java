package com.elice.cinema.domain.reservation.dto.response.seatselection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatInfo {
    private Long seatId;
    private String seatCode;
    private int rowNo;
    private int colNo;
    private boolean selectable;  // inactive, HOLD, CONFIRMED -> false
}
