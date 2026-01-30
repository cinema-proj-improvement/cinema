package com.elice.cinema.domain.screening.dto.request;

import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreeningUpdateRequest {
    @NotNull(message = "상영 상태를 선택해주세요.")
    private ScreeningStatus screeningStatus;
}
