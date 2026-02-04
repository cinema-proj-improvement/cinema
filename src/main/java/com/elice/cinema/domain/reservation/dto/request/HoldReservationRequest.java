package com.elice.cinema.domain.reservation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class HoldReservationRequest {
    @NotNull
    private Long screeningId;

    @NotEmpty
    private List<Long> seatIds;
}
