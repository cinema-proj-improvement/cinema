package com.elice.cinema.domain.screen.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatUpdateRequest {
    @NotNull(message = "운영 여부는 필수입니다.")
    private Boolean active;
}
